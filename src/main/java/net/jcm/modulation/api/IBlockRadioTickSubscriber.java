package net.jcm.modulation.api;

import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;

public interface IBlockRadioTickSubscriber {
    void tick(Vec3i pos, ServerLevel level, AbstractRadioField field);
}
