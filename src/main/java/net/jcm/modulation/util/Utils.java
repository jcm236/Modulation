package net.jcm.modulation.util;

import net.jcm.modulation.api.SignalEmission;
import net.jcm.modulation.api.SignalSample;
import net.jcm.modulation.impl.WorldRadioField;
import net.minecraft.world.phys.Vec3;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {
    public static SignalSample mix(List<WorldRadioField.EmissionMetadata> emissions, int tunedFrequency) {

        ByteArrayOutputStream mixed = new ByteArrayOutputStream();
        AtomicReference<Float> totalStrength = new AtomicReference<>(0f);
        for (WorldRadioField.EmissionMetadata e : emissions) {
            double dist = Math.sqrt(e.getDistanceSq());

            float freqDelta = Math.abs(e.getEmission().frequency() - tunedFrequency);
            float frequencyPenalty = (float) (freqDelta * dist * 0.001f);

            double usableStrength = e.getStrength() - frequencyPenalty;
            if (usableStrength <= 0f) {
                continue;
            }

            byte[] data = e.getEmission().data();
            for (byte datum : data) {
                // noise grows with frequency + distance
                int noise = (int) (Math.random() * 255 * frequencyPenalty);

                int sample = (int) (datum * usableStrength) ^ noise;
                mixed.write(sample);
            }
            totalStrength.updateAndGet(v -> (float) (v + usableStrength));

        }

        if (totalStrength.get() <= 0f)
            return null;

        return new SignalSample(
                mixed.toByteArray(),
                totalStrength.get()
        );
    }
}
