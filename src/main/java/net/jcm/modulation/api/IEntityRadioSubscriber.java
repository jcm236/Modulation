package net.jcm.modulation.api;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public interface IEntityRadioSubscriber {
    void tick(Entity entity, ServerLevel level, AbstractRadioField field);
}
