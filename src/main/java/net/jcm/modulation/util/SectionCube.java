package net.jcm.modulation.util;

import net.minecraft.core.SectionPos;
import org.joml.primitives.AABBi;

public class SectionCube {

    private final AABBi aabb;
    private final SectionPos center;

    public SectionCube(SectionPos section, int radius) {
        this.center = section;
        this.aabb = new AABBi(section.x()-radius, section.y()-radius, section.z()-radius, section.x()+radius, section.y()+radius, section.z()+radius);
    }

    public SectionPos getCenter() {
        return this.center;
    }

    public boolean coversSection(SectionPos section) {
        return this.aabb.containsPoint(section.x(), section.y(), section.z());
    }
//    public SectionCube inflate() {
//
//    }
}
