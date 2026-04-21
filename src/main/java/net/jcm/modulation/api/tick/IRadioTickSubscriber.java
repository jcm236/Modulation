package net.jcm.modulation.api.tick;

import net.jcm.modulation.api.AbstractRadioField;

public interface IRadioTickSubscriber {
    void preTick(AbstractRadioField radioField);
    void postTick(AbstractRadioField radioField);
}
