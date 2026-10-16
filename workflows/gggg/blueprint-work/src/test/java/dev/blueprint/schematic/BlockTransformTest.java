package dev.blueprint.schematic;

import java.util.Map;

public class BlockTransformTest {
    public static void main(String[] a) {
        // facing: 4x CW = Ausgangslage, 1x CW: north -> east
        BlockStateEntry f = st("minecraft:furnace", "facing", "north");
        eq(BlockTransform.apply(f, 1, false, false), "facing", "east");
        eq(BlockTransform.apply(f, 2, false, false), "facing", "south");
        eq(BlockTransform.apply(f, 4, false, false), "facing", "north");
        eq(BlockTransform.apply(f, 0, false, true), "facing", "south");
        eq(BlockTransform.apply(f, 0, true, false), "facing", "north");
        eq(BlockTransform.apply(st("x:y", "facing", "up"), 3, true, true), "facing", "up");

        // axis
        eq(BlockTransform.apply(st("minecraft:oak_log", "axis", "x"), 1, false, false), "axis", "z");
        eq(BlockTransform.apply(st("minecraft:oak_log", "axis", "x"), 2, false, false), "axis", "x");
        eq(BlockTransform.apply(st("minecraft:oak_log", "axis", "y"), 1, false, false), "axis", "y");

        // Schilder rotation 0..15 (0 = Sueden)
        eq(BlockTransform.apply(st("minecraft:oak_sign", "rotation", "0"), 1, false, false), "rotation", "4");
        eq(BlockTransform.apply(st("minecraft:oak_sign", "rotation", "14"), 1, false, false), "rotation", "2");
        eq(BlockTransform.apply(st("minecraft:oak_sign", "rotation", "0"), 0, false, true), "rotation", "8");
        eq(BlockTransform.apply(st("minecraft:oak_sign", "rotation", "4"), 0, true, false), "rotation", "12");

        // Treppe: Spiegelung kehrt Ecke um, 180 Grad (beide Spiegel) nicht
        BlockStateEntry stair = new BlockStateEntry("minecraft:oak_stairs",
            Map.of("facing", "east", "shape", "inner_left", "half", "bottom"));
        BlockStateEntry m = BlockTransform.apply(stair, 0, false, true);
        eq(m, "facing", "east"); eq(m, "shape", "inner_right"); eq(m, "half", "bottom");
        eq(BlockTransform.apply(stair, 0, true, false), "facing", "west");
        eq(BlockTransform.apply(stair, 0, true, false), "shape", "inner_right");
        eq(BlockTransform.apply(stair, 0, true, true), "shape", "inner_left");
        eq(BlockTransform.apply(stair, 0, true, true), "facing", "west");
        eq(BlockTransform.apply(new BlockStateEntry("minecraft:oak_stairs",
            Map.of("facing", "north", "shape", "outer_right")), 0, false, true), "shape", "outer_left");

        // Schienen
        eq(BlockTransform.apply(st("minecraft:rail", "shape", "north_south"), 1, false, false), "shape", "east_west");
        eq(BlockTransform.apply(st("minecraft:rail", "shape", "north_east"), 1, false, false), "shape", "south_east");
        eq(BlockTransform.apply(st("minecraft:rail", "shape", "north_east"), 0, true, false), "shape", "north_west");
        eq(BlockTransform.apply(st("minecraft:rail", "shape", "north_east"), 0, false, true), "shape", "south_east");
        eq(BlockTransform.apply(st("minecraft:rail", "shape", "ascending_north"), 1, false, false), "shape", "ascending_east");
        eq(BlockTransform.apply(st("minecraft:rail", "shape", "ascending_east"), 0, true, false), "shape", "ascending_west");

        // Zaeune/Waende: Seiten wandern mit
        BlockStateEntry fence = new BlockStateEntry("minecraft:oak_fence",
            Map.of("north", "true", "south", "false", "east", "false", "west", "false"));
        BlockStateEntry fr = BlockTransform.apply(fence, 1, false, false);
        eq(fr, "east", "true"); eq(fr, "north", "false");
        BlockStateEntry fm = BlockTransform.apply(fence, 0, false, true);
        eq(fm, "south", "true"); eq(fm, "north", "false");

        // Tuer-Scharnier, Truhen-Typ
        eq(BlockTransform.apply(new BlockStateEntry("minecraft:oak_door",
            Map.of("facing", "north", "hinge", "left")), 0, false, true), "hinge", "right");
        eq(BlockTransform.apply(new BlockStateEntry("minecraft:oak_door",
            Map.of("facing", "north", "hinge", "left")), 2, false, false), "hinge", "left");
        eq(BlockTransform.apply(st("minecraft:chest", "type", "left"), 0, true, false), "type", "right");
        eq(BlockTransform.apply(st("minecraft:chest", "type", "single"), 0, true, false), "type", "single");

        // Bloecke ohne Eigenschaften bleiben identisch
        BlockStateEntry stone = new BlockStateEntry("minecraft:stone", null);
        if (BlockTransform.apply(stone, 3, true, true) != stone) throw new AssertionError("stone");

        // Placement.place: Weltposition und Zustand zusammen
        Placement p = new Placement(10, 64, 10);
        p.rotateClockwise();
        BlockStateEntry t = p.transform(f);
        eq(t, "facing", "east");

        System.out.println("BLOCKTRANSFORM TESTS OK");
    }

    static BlockStateEntry st(String n, String k, String v) {
        return new BlockStateEntry(n, Map.of(k, v));
    }

    static void eq(BlockStateEntry s, String k, String want) {
        String got = s.properties().get(k);
        if (!want.equals(got)) throw new AssertionError(s.asString() + ": " + k + "=" + got + ", erwartet " + want);
    }
}
