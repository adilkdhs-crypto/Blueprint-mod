package dev.blueprint.schematic;

/**
 * Wo und wie eine Schematic in der Welt steht: Ankerpunkt, Drehung (0-3 mal 90 Grad im
 * Uhrzeigersinn, von oben gesehen) und Spiegelung. Reine Mathematik, ohne Minecraft.
 * Reihenfolge wie bei Minecraft/Litematica: erst spiegeln, dann drehen.
 */
public final class Placement {
    public int anchorX, anchorY, anchorZ;
    public int rotation;          // 0..3
    public boolean mirrorX;       // spiegelt an der X-Achse (Minecraft: FRONT_BACK)
    public boolean mirrorZ;       // spiegelt an der Z-Achse (Minecraft: LEFT_RIGHT)

    public Placement(int x, int y, int z) {
        this.anchorX = x; this.anchorY = y; this.anchorZ = z;
    }

    public void rotateClockwise() { rotation = (rotation + 1) & 3; }
    public void rotateCounterClockwise() { rotation = (rotation + 3) & 3; }
    public void toggleMirrorX() { mirrorX = !mirrorX; }
    public void toggleMirrorZ() { mirrorZ = !mirrorZ; }
    public void move(int dx, int dy, int dz) { anchorX += dx; anchorY += dy; anchorZ += dz; }

    /** Rechnet eine Schematic-Koordinate in eine Weltkoordinate um. Ergebnis: {x, y, z}. */
    public int[] toWorld(int x, int y, int z) {
        if (mirrorX) x = -x;
        if (mirrorZ) z = -z;
        for (int i = 0; i < rotation; i++) {
            int nx = -z;   // CLOCKWISE_90: (x, z) -> (-z, x)
            int nz = x;
            x = nx; z = nz;
        }
        return new int[]{anchorX + x, anchorY + y, anchorZ + z};
    }

    /** Blockzustand passend zu Drehung/Spiegelung dieser Platzierung. */
    public BlockStateEntry transform(BlockStateEntry state) {
        return BlockTransform.apply(state, rotation, mirrorX, mirrorZ);
    }

    /** Ein Block, wie er in der Welt stehen soll. */
    public record PlacedBlock(int x, int y, int z, BlockStateEntry state) {}

    /** Alle nicht-leeren Bloecke der Schematic in Weltkoordinaten, mit angepasstem Zustand. */
    public java.util.List<PlacedBlock> place(Schematic schematic) {
        java.util.List<PlacedBlock> out = new java.util.ArrayList<>();
        schematic.forEachBlock((x, y, z, st) -> {
            int[] w = toWorld(x, y, z);
            out.add(new PlacedBlock(w[0], w[1], w[2], transform(st)));
        });
        return out;
    }

    /** Welt-Umriss der Schematic: {minX,minY,minZ,maxX,maxY,maxZ}, Maxima inklusive. */
    public int[] worldBounds(Schematic schematic) {
        int[] b = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE,
                   Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        for (Region r : schematic.regions) {
            for (int cx : new int[]{r.minX, r.minX + r.sizeX - 1})
                for (int cy : new int[]{r.minY, r.minY + r.sizeY - 1})
                    for (int cz : new int[]{r.minZ, r.minZ + r.sizeZ - 1}) {
                        int[] w = toWorld(cx, cy, cz);
                        b[0] = Math.min(b[0], w[0]); b[3] = Math.max(b[3], w[0]);
                        b[1] = Math.min(b[1], w[1]); b[4] = Math.max(b[4], w[1]);
                        b[2] = Math.min(b[2], w[2]); b[5] = Math.max(b[5], w[2]);
                    }
        }
        return b;
    }

    /** Beschreibt Anker, Drehung und Spiegelung fuer die Chat-Ausgabe. */
    public String describe() {
        return "Anker " + anchorX + "," + anchorY + "," + anchorZ
            + "  Drehung " + (rotation * 90) + "°"
            + (mirrorX ? "  Spiegel X" : "") + (mirrorZ ? "  Spiegel Z" : "");
    }
}
