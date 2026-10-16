package dev.blueprint.schematic;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Liest .litematic-Dateien (gzip-NBT, Format von Litematica). */
public final class LitematicReader {
    private LitematicReader() {}

    public static Schematic read(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            return read(in);
        }
    }

    public static Schematic read(InputStream in) throws IOException {
        Map<String, Object> root = Nbt.readGzipped(in);

        Map<String, Object> meta = Nbt.compound(root, "Metadata");
        String name = meta != null ? Nbt.stringOf(meta, "Name", "Unbenannt") : "Unbenannt";
        String author = meta != null ? Nbt.stringOf(meta, "Author", "") : "";
        int dataVersion = Nbt.intOf(root, "MinecraftDataVersion", 0);

        Map<String, Object> regionsTag = Nbt.compound(root, "Regions");
        if (regionsTag == null) throw new IOException("Keine Regions im Litematic gefunden");

        List<Region> regions = new ArrayList<>();
        for (var entry : regionsTag.entrySet()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> tag = (Map<String, Object>) entry.getValue();
            regions.add(readRegion(entry.getKey(), tag));
        }
        return new Schematic(name, author, dataVersion, regions);
    }

    private static Region readRegion(String name, Map<String, Object> tag) throws IOException {
        Map<String, Object> pos = Nbt.compound(tag, "Position");
        Map<String, Object> size = Nbt.compound(tag, "Size");
        if (pos == null || size == null) throw new IOException("Region " + name + ": Position/Size fehlt");

        int px = Nbt.intOf(pos, "x", 0), py = Nbt.intOf(pos, "y", 0), pz = Nbt.intOf(pos, "z", 0);
        int sx = Nbt.intOf(size, "x", 0), sy = Nbt.intOf(size, "y", 0), sz = Nbt.intOf(size, "z", 0);

        // Negative Groesse = Region reicht von der Position aus in negative Richtung.
        int minX = px + (sx < 0 ? sx + 1 : 0);
        int minY = py + (sy < 0 ? sy + 1 : 0);
        int minZ = pz + (sz < 0 ? sz + 1 : 0);
        int ax = Math.abs(sx), ay = Math.abs(sy), az = Math.abs(sz);

        List<BlockStateEntry> palette = new ArrayList<>();
        for (Map<String, Object> p : Nbt.compoundList(tag, "BlockStatePalette")) {
            Map<String, String> props = new HashMap<>();
            Map<String, Object> propTag = Nbt.compound(p, "Properties");
            if (propTag != null)
                propTag.forEach((k, v) -> props.put(k, String.valueOf(v)));
            palette.add(new BlockStateEntry(Nbt.stringOf(p, "Name", "minecraft:air"), props));
        }
        if (palette.isEmpty()) throw new IOException("Region " + name + ": leere Palette");

        Object bs = tag.get("BlockStates");
        if (!(bs instanceof long[] longs)) throw new IOException("Region " + name + ": BlockStates fehlt");

        long volumeL = (long) ax * ay * az;
        if (volumeL > Integer.MAX_VALUE) throw new IOException("Region " + name + " ist zu gross");
        int[] cells = unpack(longs, palette.size(), (int) volumeL);
        return new Region(name, minX, minY, minZ, ax, ay, az, palette, cells);
    }

    /**
     * Litematica packt die Palettenindizes lueckenlos in ein long[]; ein Eintrag darf die
     * Grenze zwischen zwei longs ueberspannen. Bits pro Eintrag: max(2, ceil(log2(Paletten))).
     */
    static int[] unpack(long[] data, int paletteSize, int count) throws IOException {
        int bits = Math.max(2, 32 - Integer.numberOfLeadingZeros(paletteSize - 1));
        long mask = (1L << bits) - 1;
        if ((long) count * bits > (long) data.length * 64)
            throw new IOException("BlockStates zu kurz (" + data.length + " longs fuer "
                + count + " Bloecke bei " + bits + " Bit)");
        int[] out = new int[count];
        for (int i = 0; i < count; i++) {
            long startBit = (long) i * bits;
            int idx = (int) (startBit >>> 6);
            int off = (int) (startBit & 63);
            long v = data[idx] >>> off;
            if (off + bits > 64) v |= data[idx + 1] << (64 - off);
            int id = (int) (v & mask);
            if (id >= paletteSize) throw new IOException("Ungueltiger Palettenindex " + id);
            out[i] = id;
        }
        return out;
    }
}
