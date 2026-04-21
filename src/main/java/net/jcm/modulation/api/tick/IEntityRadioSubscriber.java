package net.jcm.modulation.api.tick;

import net.jcm.modulation.api.AbstractRadioField;

import java.util.UUID;

public interface IEntityRadioSubscriber {
    void preTick(UUID uuid, AbstractRadioField field);
    void postTick(UUID uuid, AbstractRadioField field);
}
