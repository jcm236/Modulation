package net.jcm.modulation.impl;

import net.jcm.modulation.api.AbstractRadioField;
import net.jcm.modulation.attenuation.MaterialGrid;
import net.jcm.modulation.util.DDASignalTrace;
import net.jcm.modulation.util.SectionCube;
import net.jcm.modulation.api.signal.SignalEmission;
import net.jcm.modulation.api.signal.SignalSample;
import net.jcm.modulation.util.Utils;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class WorldRadioField extends AbstractRadioField {

    public static final float RECEIVER_SENSITIVITY_DBM = -100f;
    protected ConcurrentHashMap<SignalEmission, SectionCube> emissions = new ConcurrentHashMap<>();
    protected ConcurrentHashMap<SignalEmission, SectionCube> queued = new ConcurrentHashMap<>();

    public WorldRadioField(ResourceKey<Level> level) {
        super(level);
    }

    @Override
    public void queueEmit(SignalEmission emission) {
        this.queued.put(emission, this.getCubeForEmission(emission));
    }

    @Override
    public void emit(SignalEmission emission) {
        System.out.println("emitting at" + emission.frequency());
        this.emissions.put(emission, this.getCubeForEmission(emission));
    }

    @Override
    public List<EmissionMetadata> sampleRaw(Vec3 position, int frequency, int bandwidth, Vector3f direction) {
        SectionPos receiverPos = SectionPos.of(position);
        MaterialGrid grid = MaterialGrid.getInstance(this.level);
        List<EmissionMetadata> result = new ArrayList<>();

        for (var entry : this.emissions.entrySet()) {
            SignalEmission emission = entry.getKey();

            if (!entry.getValue().coversSection(receiverPos)) continue;

            // Bandpass filter
            float freqDelta = Math.abs(emission.frequency() - frequency);
            if (freqDelta > bandwidth) continue;
            float filterLossDb = (float) (10 * Math.log10(Math.exp(
                    -Math.log(2) * (freqDelta / bandwidth) * (freqDelta / bandwidth)
            )));

            // Friis path loss
            double distMetres = Math.max(Math.sqrt(emission.position().distanceToSqr(position)), 0.5);
            float fsplDb = friisPathLossDb(distMetres, emission.frequency());

            // Block attenuation
            float blockDb = grid != null ? DDASignalTrace.trace(emission.position(), position, frequency, grid) : 0f;

            // Directional tx gain
            float txGainDb = 0f;
            if (emission.direction() != null) {
                Vector3f dirToReceiver = position.subtract(emission.position()).normalize().toVector3f();
                float alignment = Math.max(emission.direction().dot(dirToReceiver), 0f);
                txGainDb = (float) (10 * Math.log10(Math.max(alignment * alignment, 1e-10)));
                txGainDb = Math.max(txGainDb, -30f);
            }

            // Directional rx gain
            float rxGainDb = 0f;
            if (direction != null) {
                Vector3f dirFromReceiver = emission.position().subtract(position).normalize().toVector3f();
                float alignment = Math.max(direction.dot(dirFromReceiver), 0f);
                rxGainDb = (float) (10 * Math.log10(Math.max(alignment * alignment, 1e-10)));
                rxGainDb = Math.max(rxGainDb, -30f);
            }

            float rxPowerDbm = emission.powerDbm() + txGainDb + rxGainDb - fsplDb - blockDb + filterLossDb;
            if (rxPowerDbm < RECEIVER_SENSITIVITY_DBM) continue;

            result.add(new EmissionMetadata(emission, rxPowerDbm, distMetres));
        }

        System.out.println(result);

        return result;
    }

    @Override
    public SignalSample sampleAndMix(Vec3 position, int frequency, int bandwidth, Vector3f direction) {
        return Utils.mix(this.sampleRaw(position, frequency, bandwidth, direction), time.get(), bandwidth);
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

    private SectionCube getCubeForEmission(SignalEmission emission) {
        double exponent = (emission.powerDbm() - RECEIVER_SENSITIVITY_DBM
                - 20 * Math.log10(emission.frequency()) + 147.55) / 20.0;
        float maxDistBlocks = (float) Math.pow(10, exponent) * 1.5f;
        return new SectionCube(SectionPos.of(emission.position()), Mth.ceil(maxDistBlocks / 16f) + 1);
    }

    public static float friisPathLossDb(double distMetres, int frequencyHz) {
        return (float) (20 * Math.log10(distMetres) + 20 * Math.log10(frequencyHz) - 147.55);
    }

    public static class EmissionMetadata {
        private final SignalEmission emission;
        private float strengthDbm;
        private final double distMetres;

        public EmissionMetadata(SignalEmission emission, float strengthDbm, double distMetres) {
            this.emission = emission;
            this.strengthDbm = strengthDbm;
            this.distMetres = distMetres;
        }

        public void applyGainDb(float gainDb) {
            this.strengthDbm += gainDb;
        }
        public SignalEmission getEmission() {
            return emission;
        }
        public float getStrengthDbm() {
            return strengthDbm;
        }
        public double getDistMetres() {
            return distMetres;
        }
    }
}
