package dev.blueprint.schematic;

import java.util.ArrayList;
import java.util.List;

public class GhostModelTest {
    public static void main(String[] a) {
        BlockStateEntry stone = new BlockStateEntry("minecraft:stone", null);
        List<Placement.PlacedBlock> cube = new ArrayList<>();
        for (int x = 0; x < 3; x++) for (int y = 0; y < 3; y++) for (int z = 0; z < 3; z++)
            cube.add(new Placement.PlacedBlock(x, 60 + y, z, stone));
        // 3x3x3: nur der Mittelblock ist komplett umschlossen
        int n = GhostModel.build(cube).size();
        if (n != 26) throw new AssertionError("erwartet 26, war " + n);
        // negative Koordinaten duerfen keine Kollisionen erzeugen
        List<Placement.PlacedBlock> neg = List.of(
            new Placement.PlacedBlock(-1, 5, -1, stone), new Placement.PlacedBlock(1, 5, 1, stone));
        if (GhostModel.build(neg).size() != 2) throw new AssertionError("negativ");
        var g = GhostModel.build(cube).get(0);
        if (g.r() < 0 || g.r() > 1 || g.g() < 0 || g.g() > 1 || g.b() < 0 || g.b() > 1) throw new AssertionError("farbe");
        System.out.println("GHOSTMODEL TESTS OK");
    }
}
