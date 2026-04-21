package net.jcm.modulation.impl;

import net.jcm.modulation.api.AbstractRadioField;
import net.jcm.modulation.util.SectionCube;
import net.jcm.modulation.api.signal.SignalEmission;
import net.jcm.modulation.api.signal.SignalSample;
import net.jcm.modulation.util.Utils;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class WorldRadioField extends AbstractRadioField {

    public static final float MIN_SIGNAL = 0.01f;
    protected ConcurrentHashMap<SignalEmission, SectionCube> emissions = new ConcurrentHashMap<>();
    protected ConcurrentHashMap<SignalEmission, SectionCube> queued = new ConcurrentHashMap<>();

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
    public List<EmissionMetadata> sampleRaw(Vec3 position, int frequency, float bandwidth, Vector3f direction) {
        SectionPos receiverPos = SectionPos.of(position);
        List<EmissionMetadata> result = new ArrayList<>();

        for (var entry : this.emissions.entrySet()) {
            SignalEmission emission = entry.getKey();

            if (!entry.getValue().coversSection(receiverPos)) continue;
            if (Math.abs(emission.frequency() - frequency) > bandwidth) continue;

            EmissionMetadata meta = new EmissionMetadata(emission, position);

            // Apply transmitter directional gain
            if (emission.direction() != null) {
                Vector3f dirToReceiver = position.subtract(emission.position()).normalize().toVector3f();
                float alignment = Math.max(emission.direction().dot(dirToReceiver), 0.0f);
                meta.applyStrengthGain(alignment * alignment);
            }

            // Apply receiver directional gain
            if (direction != null) {
                Vector3f dirFromReceiver = emission.position().subtract(position).normalize().toVector3f();
                float alignment = Math.max(direction.dot(dirFromReceiver), 0.0f);
                meta.applyStrengthGain(alignment * alignment);
            }

            if (meta.getStrength() > MIN_SIGNAL) {
                result.add(meta);
            }
        }

        return result;
    }

    @Override
    public SignalSample sample(Vec3 position, int frequency, float bandwidth, Vector3f direction) {
        return Utils.mix(this.sampleRaw(position, frequency, bandwidth, direction), frequency, time.get());
    }

    @Override
    protected void reset() {
        this.emissions.clear();
    }

    @Override
    protected void postTick() {
        super.postTick();
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
