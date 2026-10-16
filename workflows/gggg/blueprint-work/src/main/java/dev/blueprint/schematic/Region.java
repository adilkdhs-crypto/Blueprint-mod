package dev.blueprint.schematic;

import java.util.List;

/**
 * Ein Bereich einer Schematic. (minX, minY, minZ) ist die kleinste Ecke relativ zum
 * Schematic-Ursprung; die Groesse ist immer positiv.
 */
public final class Region {
    public final String name;
    public final int minX, minY, minZ;
    public final int sizeX, sizeY, sizeZ;
    public final List<BlockStateEntry> palette;
    private final int[] cells; // Palettenindex je Zelle, Reihenfolge: (y * sizeZ + z) * sizeX + x

    Region(String name, int minX, int minY, int minZ, int sizeX, int sizeY, int sizeZ,
           List<BlockStateEntry> palette, int[] cells) {
        this.name = name;
        this.minX = minX; this.minY = minY; this.minZ = minZ;
        this.sizeX = sizeX; this.sizeY = sizeY; this.sizeZ = sizeZ;
        this.palette = palette;
        this.cells = cells;
    }

    /** Koordinaten lokal zur Region (0..size-1). */
    public BlockStateEntry get(int x, int y, int z) {
        return palette.get(cells[(y * sizeZ + z) * sizeX + x]);
    }

    public int volume() { return cells.length; }
}
