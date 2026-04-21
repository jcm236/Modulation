package net.jcm.modulation.api;

import net.jcm.modulation.impl.WorldRadioField;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public interface IRadioFieldManager {
    WorldRadioField getOrCreateField(ServerLevel level);
    void removeField(ServerLevel level);
    void start();
    void shutdown();
}
