package net.jcm.modulation.util;

import net.jcm.modulation.api.signal.SignalSample;
import net.jcm.modulation.impl.WorldRadioField;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Random;

public class Utils {
    private static final float NOISE_FIGURE_DB = 6f;

    public static float thermalNoise(int bandwidthHz) {
        return -174f + (float)(10 * Math.log10(bandwidthHz));
    }

    public static SignalSample mix(List<WorldRadioField.EmissionMetadata> emissions, long gameTime, int bandwidth) {
        if (emissions.isEmpty()) return null;

        // Capture effect: strongest signal wins
        WorldRadioField.EmissionMetadata dominant = emissions.get(0);
        for (WorldRadioField.EmissionMetadata e : emissions)
            if (e.getStrengthDbm() > dominant.getStrengthDbm()) dominant = e;

        float rxDbm = dominant.getStrengthDbm();

        float noiseFloor = thermalNoise(bandwidth) + NOISE_FIGURE_DB;

        if (rxDbm < noiseFloor) return null;

        float snrDb = rxDbm - noiseFloor;
        ByteArrayOutputStream out = noisify(gameTime, snrDb, dominant);

        return new SignalSample(out.toByteArray(), rxDbm);
    }

    private static @NotNull ByteArrayOutputStream noisify(long gameTime, float snrDb, WorldRadioField.EmissionMetadata dominant) {
        float noiseScale = Mth.clamp(1.0f - (snrDb / 20f), 0f, 1f);

        long seed = gameTime * 31L
                ^ dominant.getEmission().hashCode()
                ^ Double.doubleToLongBits(dominant.getDistMetres());
        Random rng = new Random(seed);

        byte[] raw = dominant.getEmission().data();
        ByteArrayOutputStream out = new ByteArrayOutputStream(raw.length);

        for (byte b : raw) {
            if (noiseScale > 0f && rng.nextFloat() < noiseScale) {
                int noise = (int) ((rng.nextFloat() - 0.5f) * 255f * noiseScale);
                out.write(Mth.clamp(b + noise, -128, 127));
            } else {
                out.write(b);
            }
        }
        return out;
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
