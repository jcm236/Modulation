package net.jcm.modulation.api.tick;

import net.jcm.modulation.api.AbstractRadioField;
import net.minecraft.core.Vec3i;

public interface IBlockRadioTickSubscriber {
    void preTick(Vec3i pos, AbstractRadioField field);
    void postTick(Vec3i pos, AbstractRadioField field);
}
