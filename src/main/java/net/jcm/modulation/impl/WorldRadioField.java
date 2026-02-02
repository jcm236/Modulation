package net.jcm.modulation.impl;

import net.jcm.modulation.api.AbstractRadioField;
import net.jcm.modulation.util.SectionCube;
import net.jcm.modulation.api.SignalEmission;
import net.jcm.modulation.api.SignalSample;
import net.jcm.modulation.util.Utils;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class WorldRadioField extends AbstractRadioField {

    public static final float MIN_SIGNAL = 0.01f;
    protected Map<SignalEmission, SectionCube> emissions = new HashMap<>();

    protected Map<SignalEmission, SectionCube> queued = new HashMap<>();

    public WorldRadioField(CompoundTag tag) {}

    public WorldRadioField() {}

    @Override
    public void queueEmit(SignalEmission emission) {
        this.queued.put(emission, this.getCubeForEmission(emission));
    }

    @Override
    public void emit(SignalEmission emission) {
        this.emissions.put(emission, this.getCubeForEmission(emission));
    }

    @Override
    public SignalSample sample(Vec3 position, int frequency, float bandwidth, Vector3f direction) {
        SectionPos receiverPos = SectionPos.of(position);

        Stream.Builder<SignalEmission> emissionsInRange = Stream.builder();
        for (SignalEmission emission : this.emissions.keySet()) {
            if (emissions.get(emission).coversSection(receiverPos)) {
                    emissionsInRange.add(emission);
            }
        }

        Stream<EmissionMetadata> filtered = emissionsInRange
                .build()
                .filter((e) -> Math.abs(e.frequency() - frequency) <= bandwidth)
                .map((e) -> new EmissionMetadata(e, position))
                .map((e) -> {
                    if (e.emission.direction() == null) {
                        return e;
                    }

                    Vector3f dirToReceiver = position.subtract(e.emission.position()).normalize().toVector3f();
                    float alignment = Math.max(e.emission.direction().dot(dirToReceiver), 0.0f);
                    float dirGain = alignment * alignment;
                    e.applyStrengthGain(dirGain);
                    return e;
                })
                .map((e) -> {
                    if (direction == null) {
                        return e;
                    }

                    Vector3f dirFromReceiver = e.emission.position().subtract(position).normalize().toVector3f();
                    float alignment = Math.max(direction.dot(dirFromReceiver), 0.0f);
                    float dirGain = alignment * alignment;
                    e.applyStrengthGain(dirGain);
                    return e;
                })
                .filter((e) -> e.getStrength() > MIN_SIGNAL);
        List<EmissionMetadata> listified = filtered.toList();
        System.out.println(listified);
        return Utils.mix(listified, frequency);

    }

    @Override
    protected void reset() {
        this.emissions.clear();
    }

    @Override
    protected void postTick(ServerLevel level) {
        super.postTick(level);
        this.emissions.putAll(queued);
        this.queued.clear();
    }

    private SectionCube getCubeForEmission(SignalEmission emission){
        float maxDist = Mth.sqrt(emission.power() / MIN_SIGNAL);
        return new SectionCube(SectionPos.of(emission.position()), Mth.ceil(maxDist/16)+1);
    }

    public static class EmissionMetadata {
        private final SignalEmission emission;
        private double strength;
        private final double distanceSq;

        public EmissionMetadata(SignalEmission emission, Vec3 receiverPos) {
            this.emission =  emission;
            this.distanceSq = Math.max(
                    emission.position().distanceToSqr(receiverPos),
                    0.25 // half-block minimum
            );
            this.strength = emission.power() / (float) this.distanceSq;
        }

        public EmissionMetadata(SignalEmission emission, double strength, double distanceSq) {
            this.emission = emission;
            this.strength = strength;
            this.distanceSq = distanceSq;
        }

        public void applyStrengthGain(double gain) {
            this.strength = this.strength * gain;
        }

        public SignalEmission getEmission() {
            return emission;
        }

        public  double getStrength() {
            return strength;
        }

        public double getDistanceSq() {
            return distanceSq;
        }
    }
}
