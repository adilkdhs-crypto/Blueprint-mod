package dev.blueprint.schematic;

import java.util.*;

public final class Schematic {
    public final String name;
    public final String author;
    public final int dataVersion;
    public final List<Region> regions;

    Schematic(String name, String author, int dataVersion, List<Region> regions) {
        this.name = name;
        this.author = author;
        this.dataVersion = dataVersion;
        this.regions = List.copyOf(regions);
    }

    /** Callback fuer jeden nicht-leeren Block in Schematic-Koordinaten. */
    public interface BlockVisitor {
        void visit(int x, int y, int z, BlockStateEntry state);
    }

    public void forEachBlock(BlockVisitor v) {
        for (Region r : regions) {
            for (int y = 0; y < r.sizeY; y++)
                for (int z = 0; z < r.sizeZ; z++)
                    for (int x = 0; x < r.sizeX; x++) {
                        BlockStateEntry s = r.get(x, y, z);
                        if (!s.isAir()) v.visit(r.minX + x, r.minY + y, r.minZ + z, s);
                    }
        }
    }

    /** Materialliste: Blockname -> Anzahl, absteigend sortiert. Luft wird ignoriert. */
    public LinkedHashMap<String, Integer> materialList() {
        Map<String, Integer> counts = new HashMap<>();
        forEachBlock((x, y, z, s) -> counts.merge(s.name(), 1, Integer::sum));
        LinkedHashMap<String, Integer> sorted = new LinkedHashMap<>();
        counts.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                .thenComparing(Map.Entry.comparingByKey()))
            .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        return sorted;
    }

    public int[] boundsSize() {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (Region r : regions) {
            minX = Math.min(minX, r.minX); maxX = Math.max(maxX, r.minX + r.sizeX);
            minY = Math.min(minY, r.minY); maxY = Math.max(maxY, r.minY + r.sizeY);
            minZ = Math.min(minZ, r.minZ); maxZ = Math.max(maxZ, r.minZ + r.sizeZ);
        }
        return regions.isEmpty() ? new int[]{0, 0, 0}
            : new int[]{maxX - minX, maxY - minY, maxZ - minZ};
    }
}
