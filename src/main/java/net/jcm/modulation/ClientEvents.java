package net.jcm.modulation;

import com.mojang.blaze3d.platform.InputConstants;
import net.jcm.modulation.util.ImGuiManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientEvents {
    public static final KeyMapping OPEN_RADIO_GUI =
            new KeyMapping(
                    "key.modulation.open_gui",
                    InputConstants.KEY_P,
                    "key.categories.modulation"
            );

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (OPEN_RADIO_GUI.consumeClick()) {
            Minecraft.getInstance().setScreen(new NodeScreen(Component.literal("meower 2000 pro max")));
        }
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        ImGuiManager.init();
    }

    @SubscribeEvent
    public void registerBindings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_RADIO_GUI);
    }
}

