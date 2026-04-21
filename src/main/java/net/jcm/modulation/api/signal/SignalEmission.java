package net.jcm.modulation.api.signal;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public record SignalEmission(Vec3 position, int frequency, byte[] data, float power, @Nullable Vector3f direction) {}