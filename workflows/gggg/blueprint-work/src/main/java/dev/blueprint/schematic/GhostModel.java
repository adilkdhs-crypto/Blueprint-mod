package dev.blueprint.schematic;

import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

/**
 * Bereitet platzierte Bloecke fuer die Geisteranzeige auf: nur Oberflaechenbloecke
 * (mindestens ein leerer Nachbar), je Blockart eine feste Farbe. Reine Logik.
 */
public final class GhostModel {
    private GhostModel() {}

    public record Ghost(int x, int y, int z, float r, float g, float b) {}

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    public static List<Ghost> build(List<Placement.PlacedBlock> blocks) {
        Set<Long> solid = new HashSet<>(blocks.size() * 2);
        for (var b : blocks) solid.add(key(b.x(), b.y(), b.z()));
        int[][] n = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
        List<Ghost> out = new ArrayList<>();
        for (var b : blocks) {
            boolean surface = false;
            for (int[] d : n)
                if (!solid.contains(key(b.x() + d[0], b.y() + d[1], b.z() + d[2]))) { surface = true; break; }
            if (!surface) continue;
            int h = b.state().name().hashCode();
            float r = 0.25f + ((h >>> 16) & 0xFF) / 255f * 0.75f;
            float g = 0.25f + ((h >>> 8) & 0xFF) / 255f * 0.75f;
            float bl = 0.25f + (h & 0xFF) / 255f * 0.75f;
            out.add(new Ghost(b.x(), b.y(), b.z(), r, g, bl));
        }
        return out;
    }
}
