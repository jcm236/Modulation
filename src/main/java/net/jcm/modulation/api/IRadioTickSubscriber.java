package net.jcm.modulation.api;

import net.minecraft.server.level.ServerLevel;

public interface IRadioTickSubscriber {
    void preTick(AbstractRadioField radioField, ServerLevel level);
    void postTick(AbstractRadioField radioField, ServerLevel level);
}
