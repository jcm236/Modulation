package net.jcm.modulation;

import com.mojang.logging.LogUtils;
import net.jcm.modulation.api.AbstractRadioField;
import net.jcm.modulation.api.signal.SignalSample;
import net.jcm.modulation.api.tick.IEntityRadioSubscriber;
import net.jcm.modulation.impl.RadioManager;
import net.jcm.modulation.impl.ThreadedRadioFieldManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Modulation.MODID)
public class ServerEvents {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final int RADIO_TPS = 100;

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        RadioManager.init(new ThreadedRadioFieldManager(RADIO_TPS));
        LOGGER.info("[Modulation] RadioManager initialized with ThreadedRadioFieldManager at {} ticks/sec", RADIO_TPS);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        RadioManager.getInstance().shutdown();
        LOGGER.info("[Modulation] RadioManager shut down");
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            RadioManager.getInstance().getOrCreateField(level);
            LOGGER.info("[Modulation] Radio field created for level: {}", level.dimension().location());
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            RadioManager.getInstance().removeField(level);
            LOGGER.info("[Modulation] Radio field removed for level: {}", level.dimension().location());
        }
    }

    // for testing
    // TODO: remove when actual radio stuff added
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

        RadioManager.getInstance()
                .getOrCreateField(serverPlayer.serverLevel())
                .addEntitySubscriber(serverPlayer.getUUID(), new IEntityRadioSubscriber() {
                    @Override
                    public void preTick(UUID uuid, AbstractRadioField field) {
                        Vec3 pos = serverPlayer.position();
                        SignalSample sample = field.sample(pos, 50, 50, null);
                        if (sample == null) return;

                        String text = new String(
                                net.jcm.modulation.util.Utils.hammingDecodeBytes(sample.data()),
                                java.nio.charset.StandardCharsets.UTF_8
                        );

                        Objects.requireNonNull(serverPlayer.getServer()).execute(() ->
                                serverPlayer.displayClientMessage(
                                        net.minecraft.network.chat.Component.literal(text), false)
                        );
                    }

                    @Override
                    public void postTick(UUID uuid, AbstractRadioField field) {}
                });
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

        RadioManager.getInstance()
                .getOrCreateField(serverPlayer.serverLevel())
                .removeEntitySubscriber(serverPlayer.getUUID());
    }

}


