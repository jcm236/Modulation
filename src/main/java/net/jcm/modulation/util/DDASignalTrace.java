package net.jcm.modulation.util;

import net.jcm.modulation.attenuation.MaterialGrid;
import net.minecraft.world.phys.Vec3;

public class DDASignalTrace {
    public static final float MAX_ATTENUATION_DB = 60f;

    public static float trace(Vec3 from, Vec3 to, int frequencyHz, MaterialGrid grid) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;

        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 0.5) return 0f;

        double invLen = 1.0 / length;
        double ndx = dx * invLen;
        double ndy = dy * invLen;
        double ndz = dz * invLen;

        int bx = (int) Math.floor(from.x);
        int by = (int) Math.floor(from.y);
        int bz = (int) Math.floor(from.z);

        int ex = (int) Math.floor(to.x);
        int ey = (int) Math.floor(to.y);
        int ez = (int) Math.floor(to.z);

        int stepX = ndx >= 0 ? 1 : -1;
        int stepY = ndy >= 0 ? 1 : -1;
        int stepZ = ndz >= 0 ? 1 : -1;

        double tDeltaX = Math.abs(ndx) > 1e-9 ? 1.0 / Math.abs(ndx) : Double.MAX_VALUE;
        double tDeltaY = Math.abs(ndy) > 1e-9 ? 1.0 / Math.abs(ndy) : Double.MAX_VALUE;
        double tDeltaZ = Math.abs(ndz) > 1e-9 ? 1.0 / Math.abs(ndz) : Double.MAX_VALUE;

        double tMaxX = tDeltaX == Double.MAX_VALUE ? Double.MAX_VALUE : (stepX > 0 ? Math.ceil(from.x) - from.x : from.x - Math.floor(from.x)) / Math.abs(ndx);
        double tMaxY = tDeltaY == Double.MAX_VALUE ? Double.MAX_VALUE : (stepY > 0 ? Math.ceil(from.y) - from.y : from.y - Math.floor(from.y)) / Math.abs(ndy);
        double tMaxZ = tDeltaZ == Double.MAX_VALUE ? Double.MAX_VALUE : (stepZ > 0 ? Math.ceil(from.z) - from.z : from.z - Math.floor(from.z)) / Math.abs(ndz);

        if (tMaxX == 0) tMaxX = tDeltaX;
        if (tMaxY == 0) tMaxY = tDeltaY;
        if (tMaxZ == 0) tMaxZ = tDeltaZ;

        float totalDb = 0f;
        int maxSteps = (int) (length + 4);

        for (int i = 0; i < maxSteps; i++) {
            totalDb += grid.getAttenuation(bx, by, bz, frequencyHz);

            if (totalDb >= MAX_ATTENUATION_DB) return MAX_ATTENUATION_DB;
            if (bx == ex && by == ey && bz == ez) break;

            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                bx += stepX;
                tMaxX += tDeltaX;
            } else if (tMaxY < tMaxZ) {
                by += stepY;
                tMaxY += tDeltaY;
            } else {
                bz += stepZ;
                tMaxZ += tDeltaZ;
            }
        }

        return totalDb;
    }
}
