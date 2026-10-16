package dev.blueprint.schematic;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/** Laeuft ohne JUnit: einfach die main-Methode starten. */
public class ParserSmokeTest {

    public static void main(String[] args) throws Exception {
        Random rnd = new Random(42);
        // 3 Region-Faelle: positive Groesse, negative Groesse, grosse Palette (>16 -> 5 Bit)
        check(rnd, 3, 2, 4, 5, new int[]{10, 20, 30});
        check(rnd, -4, 3, -2, 7, new int[]{-5, 64, 8});
        check(rnd, 5, 5, 5, 40, new int[]{0, 0, 0});
        System.out.println("ALLE TESTS OK");
    }

    static void check(Random rnd, int sx, int sy, int sz, int paletteSize, int[] pos) throws Exception {
        int ax = Math.abs(sx), ay = Math.abs(sy), az = Math.abs(sz);
        int count = ax * ay * az;
        int[] expected = new int[count];
        for (int i = 0; i < count; i++) expected[i] = rnd.nextInt(paletteSize);

        // Packen wie Litematica
        int bits = Math.max(2, 32 - Integer.numberOfLeadingZeros(paletteSize - 1));
        long[] longs = new long[(int) (((long) count * bits + 63) / 64)];
        for (int i = 0; i < count; i++) {
            long start = (long) i * bits;
            int idx = (int) (start >>> 6), off = (int) (start & 63);
            longs[idx] |= ((long) expected[i]) << off;
            if (off + bits > 64) longs[idx + 1] |= ((long) expected[i]) >>> (64 - off);
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream o = new DataOutputStream(new GZIPOutputStream(bos))) {
            o.writeByte(10); o.writeUTF("");
            // Metadata
            o.writeByte(10); o.writeUTF("Metadata");
            str(o, "Name", "Test"); str(o, "Author", "ich");
            o.writeByte(0);
            o.writeByte(3); o.writeUTF("MinecraftDataVersion"); o.writeInt(4000);
            // Regions
            o.writeByte(10); o.writeUTF("Regions");
            o.writeByte(10); o.writeUTF("main");
            vec(o, "Position", pos[0], pos[1], pos[2]);
            vec(o, "Size", sx, sy, sz);
            o.writeByte(9); o.writeUTF("BlockStatePalette"); o.writeByte(10); o.writeInt(paletteSize);
            for (int i = 0; i < paletteSize; i++) {
                str(o, "Name", i == 0 ? "minecraft:air" : "minecraft:block_" + i);
                if (i == 1) {
                    o.writeByte(10); o.writeUTF("Properties");
                    str(o, "facing", "north"); o.writeByte(0);
                }
                o.writeByte(0);
            }
            o.writeByte(12); o.writeUTF("BlockStates"); o.writeInt(longs.length);
            for (long l : longs) o.writeLong(l);
            o.writeByte(0); // Ende main
            o.writeByte(0); // Ende Regions
            o.writeByte(0); // Ende Wurzel
        }

        Schematic s = LitematicReader.read(new ByteArrayInputStream(bos.toByteArray()));
        Region r = s.regions.get(0);
        int minX = pos[0] + (sx < 0 ? sx + 1 : 0);
        int minY = pos[1] + (sy < 0 ? sy + 1 : 0);
        int minZ = pos[2] + (sz < 0 ? sz + 1 : 0);
        assertEq(r.minX, minX); assertEq(r.minY, minY); assertEq(r.minZ, minZ);
        assertEq(r.sizeX, ax); assertEq(r.sizeY, ay); assertEq(r.sizeZ, az);

        int nonAir = 0;
        for (int y = 0; y < ay; y++) for (int z = 0; z < az; z++) for (int x = 0; x < ax; x++) {
            int exp = expected[(y * az + z) * ax + x];
            String want = exp == 0 ? "minecraft:air" : "minecraft:block_" + exp;
            if (!r.get(x, y, z).name().equals(want)) throw new AssertionError("Block bei " + x + "," + y + "," + z);
            if (exp != 0) nonAir++;
        }
        int total = s.materialList().values().stream().mapToInt(Integer::intValue).sum();
        assertEq(total, nonAir);
        assertEq(s.dataVersion, 4000);
        if (!s.name.equals("Test")) throw new AssertionError("Name");
        System.out.println("OK  size=" + sx + "x" + sy + "x" + sz + " palette=" + paletteSize
            + " bits=" + bits + " nichtLeer=" + nonAir);
    }

    static void assertEq(int a, int b) { if (a != b) throw new AssertionError(a + " != " + b); }

    static void str(DataOutputStream o, String k, String v) throws IOException {
        o.writeByte(8); o.writeUTF(k); o.writeUTF(v);
    }

    static void vec(DataOutputStream o, String k, int x, int y, int z) throws IOException {
        o.writeByte(10); o.writeUTF(k);
        for (var e : new Object[][]{{"x", x}, {"y", y}, {"z", z}}) {
            o.writeByte(3); o.writeUTF((String) e[0]); o.writeInt((Integer) e[1]);
        }
        o.writeByte(0);
    }
}
