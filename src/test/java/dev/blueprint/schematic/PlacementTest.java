package dev.blueprint.schematic;

import java.util.Arrays;

public class PlacementTest {
    public static void main(String[] a) {
        Placement p = new Placement(100, 64, 200);
        eq(p.toWorld(1, 2, 3), 101, 66, 203);

        // Norden (-z) nach Osten (+x), wie bei Minecraft CLOCKWISE_90
        p.rotateClockwise();
        eq(p.toWorld(0, 0, -1), 101, 64, 200);
        // vier Drehungen = Ausgangslage
        p.rotateClockwise(); p.rotateClockwise(); p.rotateClockwise();
        eq(p.toWorld(1, 2, 3), 101, 66, 203);
        // links herum ist das Gegenteil
        p.rotateCounterClockwise();
        eq(p.toWorld(0, 0, -1), 99, 64, 200);

        Placement m = new Placement(0, 0, 0);
        m.toggleMirrorX();
        eq(m.toWorld(5, 1, 7), -5, 1, 7);
        m.toggleMirrorZ();
        eq(m.toWorld(5, 1, 7), -5, 1, -7);

        if (!MaterialFormat.stacks(150, 64).equals("2 Stacks + 22")) throw new AssertionError();
        if (!MaterialFormat.stacks(64, 64).equals("1 Stacks")) throw new AssertionError();
        if (!MaterialFormat.prettyName("minecraft:oak_planks").equals("Oak Planks")) throw new AssertionError();
        System.out.println("PLACEMENT TESTS OK");
    }

    static void eq(int[] got, int... want) {
        if (!Arrays.equals(got, want))
            throw new AssertionError(Arrays.toString(got) + " != " + Arrays.toString(want));
    }
}
