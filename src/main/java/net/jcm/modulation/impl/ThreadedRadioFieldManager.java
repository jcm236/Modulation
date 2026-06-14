package net.jcm.modulation.impl;

import com.mojang.logging.LogUtils;
import net.jcm.modulation.api.IRadioFieldManager;
import net.jcm.modulation.attenuation.MaterialGrid;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ThreadedRadioFieldManager implements IRadioFieldManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final int ticksPerSecond;

    private final ConcurrentHashMap<ServerLevel, WorldRadioField> fields = new ConcurrentHashMap<>();

    private ScheduledExecutorService executor;
    private ScheduledFuture<?> tickTask;

    private final AtomicLong totalTicks = new AtomicLong(0);
    private volatile long lastTickNanos = 0;

    public ThreadedRadioFieldManager(int ticksPerSecond) {
        this.ticksPerSecond = ticksPerSecond;
    }

    @Override
    public WorldRadioField getOrCreateField(ServerLevel level) {
        return this.fields.computeIfAbsent(level, k -> new WorldRadioField(k.dimension()));
    }

    @Override
    public void removeField(ServerLevel level) {
        this.fields.remove(level);
    }

    @Override
    public void start() {
        if (executor != null && !executor.isShutdown()) {
            LOGGER.warn("[Modulation] ThreadedRadioFieldManager already running!");
            return;
        }

        long periodMicros = 1_000_000L / ticksPerSecond;

        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "modulation-radio-thread");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY + 1);
            return t;
        });

        tickTask = executor.scheduleAtFixedRate(
                this::radioTick,
                0,
                periodMicros,
                TimeUnit.MICROSECONDS
        );

        LOGGER.info("[Modulation] Radio thread started at {} ticks/sec ({} period)",
                ticksPerSecond, periodMicros);
    }

    @Override
    public void shutdown() {
        if (tickTask != null) tickTask.cancel(false);
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        this.fields.clear();
        LOGGER.info("[Modulation] Radio thread shut down after {} ticks", totalTicks.get());
    }

    private void radioTick() {
        long start = System.nanoTime();

        for (WorldRadioField field : this.fields.values()) {
            try {
                field.tick();
            } catch (Exception e) {
                LOGGER.error("[Modulation] Exception during radio tick", e);
            }
        }

        long elapsedUs = (System.nanoTime() - start) / 1000;
        long budgetUs = 1_000_000L / ticksPerSecond;
        if (elapsedUs > budgetUs) {
            LOGGER.debug("[Modulation] Radio tick took {}µs (budget {}µs)", elapsedUs, budgetUs);
        }

        totalTicks.incrementAndGet();
    }

    public int getTPS() {
        return ticksPerSecond;
    }

    public long getTotalTicks() {
        return totalTicks.get();
    }
}