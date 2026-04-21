package net.jcm.modulation.util;

import net.jcm.modulation.api.signal.SignalSample;
import net.jcm.modulation.impl.WorldRadioField;
import net.minecraft.util.Mth;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Random;

public class Utils {
    public static SignalSample mix(
            List<WorldRadioField.EmissionMetadata> emissions,
            int tunedFrequency,
            long gameTime
    ) {
        ByteArrayOutputStream mixed = new ByteArrayOutputStream();
        float totalStrength = 0f;

        for (WorldRadioField.EmissionMetadata e : emissions) {
            double dist = Math.sqrt(e.getDistanceSq());

            float freqDelta = Math.abs(e.getEmission().frequency() - tunedFrequency);
            float frequencyPenalty = freqDelta * (float) dist * 0.001f;

            double usableStrength = e.getStrength() - frequencyPenalty;
            if (usableStrength <= 0.0)
                continue;

            // --- Noise model ---
            float distanceNoise = (float) Math.min(1.0, dist * 0.02f);
            float detuneNoise   = Math.min(1.0f, freqDelta * 0.002f);
            float noiseScale    = (distanceNoise + detuneNoise) * 0.5f;

            // Strong signal suppresses noise
            float snrSuppression = (float) Math.min(1.0, usableStrength);
            noiseScale *= (1.0f - snrSuppression);

            // Per-preTick deterministic randomness
            long seed =
                    gameTime * 31L ^
                            e.getEmission().hashCode() ^
                            Double.doubleToLongBits(e.getDistanceSq());

            Random rng = new Random(seed);

            byte[] data = e.getEmission().data();
            for (int i = 0; i < data.length; i++) {
                int clean = (int) (data[i] * usableStrength);
                int sample = clean;

                // Burst noise: not every byte
                if (rng.nextFloat() < noiseScale) {
                    int noise = (int) (
                            (rng.nextFloat() - 0.5f) * 16f * noiseScale
                    );
                    noise = Mth.clamp(noise, -6, 6);
                    sample += noise;
                }

                sample = Mth.clamp(sample, -128, 127);
                mixed.write(sample);
            }

            totalStrength += usableStrength;
        }

        if (totalStrength <= 0f)
            return null;

        return new SignalSample(
                mixed.toByteArray(),
                totalStrength
        );
    }

    public static byte hammingEncodeNibble(int data) {
        // data: lower 4 bits only
        int d1 = (data >> 0) & 1;
        int d2 = (data >> 1) & 1;
        int d3 = (data >> 2) & 1;
        int d4 = (data >> 3) & 1;

        int p1 = d1 ^ d2 ^ d4;
        int p2 = d1 ^ d3 ^ d4;
        int p4 = d2 ^ d3 ^ d4;

        int encoded = 0;
        encoded |= (p1 << 0); // bit 1
        encoded |= (p2 << 1); // bit 2
        encoded |= (d1 << 2); // bit 3
        encoded |= (p4 << 3); // bit 4
        encoded |= (d2 << 4); // bit 5
        encoded |= (d3 << 5); // bit 6
        encoded |= (d4 << 6); // bit 7

        return (byte) encoded;
    }


    public static int hammingDecodeNibble(byte encoded) {
        int b1 = (encoded >> 0) & 1;
        int b2 = (encoded >> 1) & 1;
        int b3 = (encoded >> 2) & 1;
        int b4 = (encoded >> 3) & 1;
        int b5 = (encoded >> 4) & 1;
        int b6 = (encoded >> 5) & 1;
        int b7 = (encoded >> 6) & 1;

        int s1 = b1 ^ b3 ^ b5 ^ b7;
        int s2 = b2 ^ b3 ^ b6 ^ b7;
        int s4 = b4 ^ b5 ^ b6 ^ b7;

        int syndrome = (s4 << 2) | (s2 << 1) | s1;

        // Correct single-bit error
        if (syndrome != 0 && syndrome <= 7) {
            encoded ^= (1 << (syndrome - 1));
        }

        // Extract corrected data bits
        int d1 = (encoded >> 2) & 1;
        int d2 = (encoded >> 4) & 1;
        int d3 = (encoded >> 5) & 1;
        int d4 = (encoded >> 6) & 1;

        return (d4 << 3) | (d3 << 2) | (d2 << 1) | d1;
    }


    public static byte[] hammingEncodeBytes(byte[] input) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        for (byte b : input) {
            int lo = b & 0x0F;
            int hi = (b >> 4) & 0x0F;

            out.write(hammingEncodeNibble(lo));
            out.write(hammingEncodeNibble(hi));
        }

        return out.toByteArray();
    }

    public static byte[] hammingDecodeBytes(byte[] encoded) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        for (int i = 0; i + 1 < encoded.length; i += 2) {
            int lo = hammingDecodeNibble(encoded[i]);
            int hi = hammingDecodeNibble(encoded[i + 1]);

            out.write((hi << 4) | lo);
        }

        return out.toByteArray();
    }

}
