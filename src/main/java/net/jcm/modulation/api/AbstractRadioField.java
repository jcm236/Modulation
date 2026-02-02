package net.jcm.modulation.api;

import net.jcm.modulation.impl.WorldRadioField;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractRadioField extends SavedData {

    protected final AtomicInteger time = new AtomicInteger(0);

    protected final Map<Entity, IEntityRadioSubscriber> entityRadioSubscribers = new HashMap<>();
    protected final Map<Vec3i, IBlockRadioTickSubscriber> blockRadioSubscribers = new HashMap<>();
    protected final Set<IRadioTickSubscriber> tickSubscribers = new HashSet<>();


//    private final Map<SignalEmission, Stream<SectionPos>> emissionField = new HashMap<>();

    public static WorldRadioField getInstance(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(WorldRadioField::new, WorldRadioField::new, "modulation_radios");
    }

    public abstract void queueEmit(SignalEmission emission);

    public void addTickSubscriber(IRadioTickSubscriber subscriber) {
        this.tickSubscribers.add(subscriber);
    }

    public void removeTickSubscriber(IRadioTickSubscriber subscriber) {
        this.tickSubscribers.remove(subscriber);
    }


    public void addBlockSubscriber(Vec3i vec3, IBlockRadioTickSubscriber subscriber) {
        this.blockRadioSubscribers.put(vec3, subscriber);
    }

    public void removeBlockSubscriber(Vec3i vec3) {
        this.blockRadioSubscribers.remove(vec3);
    }

    public void addEntitySubscriber(Entity entity, IEntityRadioSubscriber subscriber) {
        this.entityRadioSubscribers.put(entity, subscriber);
    }

    public void removeEntitySubscriber(Entity entity) {
        this.entityRadioSubscribers.remove(entity);
    }

    public abstract void emit(SignalEmission emission); //{
//        this.emissionField.computeIfAbsent(emission, (e) -> {
//            float maxDist = Mth.sqrt(e.power() / MIN_SIGNAL);
//            return SectionPos.cube(SectionPos.of(e.position()), Mth.ceil(maxDist/16)+1);
//        });
//    }

    public abstract SignalSample sample(Vec3 position, int frequency, float bandwidth, Vector3f direction); //{
//        List<SignalEmission> emissionsInRange = new  ArrayList<>();
//        SectionPos receiverSection = SectionPos.of(position);
//
//
//        SectionPos.cube(receiverSection, MAX_CHUNK_RADIUS).filter(emissionField::containsKey).forEach((sectionPos) -> {
//            Map<SignalEmission, Stream<SectionPos>> emissions = emissionField.getOrDefault(sectionPos, new HashMap<>());
//            for (SignalEmission e : emissions) {
//                if (Math.abs(e.frequency() - frequency) > bandwidth)
//                    continue;
//
//                double distSq = Math.max(
//                        e.position().distanceToSqr(position),
//                        0.25 // half-block minimum
//                );
//                float strength = e.power() / (float) distSq;
//                if (strength > MIN_SIGNAL)
//                    emissionsInRange.add(e);
//            }
//        });
//
//        Map<SignalEmission, Float> emissionsAndStrength = new HashMap<>();
//
//        for (SignalEmission e : emissionsInRange) {
//            double distSq = Math.max(
//                    e.position().distanceToSqr(position),
//                    0.25 // half-block minimum
//            );
//            float strength = e.power() / (float) distSq;
//            if (e.direction() == null) {
//                emissionsAndStrength.put(e, strength);
//                continue;
//            }
//            Vector3f dirToReceiver = position.subtract(e.position()).normalize().toVector3f();
//            float alignment = Math.max(e.direction().dot(dirToReceiver), 0.0f);
//            float dirGain = alignment * alignment;
//
//            float directionalStrength = strength * dirGain;
//
//            if (directionalStrength > MIN_SIGNAL)
//                emissionsAndStrength.put(e, directionalStrength);
//
//        }
//        return Utils.mix(emissionsAndStrength, position, frequency);
//    }

    public int getTicks() {
        return time.get();
    }

    protected void preTick(ServerLevel level) {
        for (IRadioTickSubscriber subscriber : tickSubscribers) {
            subscriber.preTick(this, level);
        }

        for (Map.Entry<Vec3i, IBlockRadioTickSubscriber> entry : blockRadioSubscribers.entrySet()) {
            entry.getValue().tick(entry.getKey(),  level, this);
        }

        for (Map.Entry<Entity, IEntityRadioSubscriber> entry : entityRadioSubscribers.entrySet()) {
            entry.getValue().tick(entry.getKey(), level, this);
        }
    }

    protected void postTick(ServerLevel level) {
        for (IRadioTickSubscriber subscriber : tickSubscribers) {
            subscriber.postTick(this, level);
        }
    }

    protected abstract void reset();


    public void tick(ServerLevel level){
        System.out.println("rf tick fr");
//        if ((time.getAndIncrement()%20) == 0) {
//            System.out.println(time.get());
        this.preTick(level);
        this.reset();
        this.postTick(level);
//        }
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag) {
        return compoundTag;
    }

    @Mod.EventBusSubscriber
    public static class EventHandler {
        @SubscribeEvent
        public static void serverTick(TickEvent.LevelTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                if(event.level instanceof ServerLevel serverLevel) {
                    AbstractRadioField.getInstance(serverLevel).tick(serverLevel);
                }
            }
        }

        @SubscribeEvent
        public static void playerTick(TickEvent.PlayerTickEvent event) {
            if(event.player instanceof ServerPlayer serverPlayer) {
                AbstractRadioField.getInstance(serverPlayer.serverLevel()).addEntitySubscriber(serverPlayer, (pos, level, field) -> {
                    SignalSample sample =  field.sample(event.player.getPosition(1), 50, 50, null);
                    System.out.println("rec");
                    if (sample != null) {
                        System.out.println(new String(sample.data(), StandardCharsets.UTF_8));
                        event.player.displayClientMessage(Component.literal(new String(sample.data(), StandardCharsets.UTF_8)), false);
                    }
                });

            }
        }

    }
}
