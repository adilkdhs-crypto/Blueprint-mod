package dev.blueprint.schematic;

import java.util.HashMap;
import java.util.Map;

/**
 * Dreht und spiegelt Blockzustaende (facing, axis, rotation, Treppen-Form, Schienen, Seiten
 * von Zaeunen/Waenden/Scheiben, Tuer-Scharnier, Truhen-Typ). Reine Logik, ohne Minecraft.
 * Reihenfolge wie bei Placement: erst spiegeln, dann im Uhrzeigersinn drehen.
 */
public final class BlockTransform {
    private BlockTransform() {}

    private static final String[] CW = {"north", "east", "south", "west"};

    static String rotDir(String d, int steps) {
        for (int i = 0; i < CW.length; i++)
            if (CW[i].equals(d)) return CW[(i + steps) & 3];
        return d; // up/down/andere
    }

    static String mirDir(String d, boolean mx, boolean mz) {
        if (mx) { if (d.equals("east")) d = "west"; else if (d.equals("west")) d = "east"; }
        if (mz) { if (d.equals("north")) d = "south"; else if (d.equals("south")) d = "north"; }
        return d;
    }

    private static String swapLR(String v, String l, String r) {
        return v.equals(l) ? r : v.equals(r) ? l : v;
    }

    /** Zerlegt Schienenform in Richtungsteile, wendet f an und baut sie in kanonischer Form zusammen. */
    private static String rail(String shape, java.util.function.UnaryOperator<String> f) {
        String asc = "";
        String s = shape;
        if (s.startsWith("ascending_")) { asc = "ascending_"; s = s.substring(10); }
        String[] parts = s.split("_");
        String[] out = new String[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = f.apply(parts[i]);
        if (asc.isEmpty() && out.length == 2) {
            // north_south / east_west / Kurven: kanonische Reihenfolge der Spiel-Namen
            java.util.Set<String> set = new java.util.HashSet<>(java.util.List.of(out));
            if (set.equals(java.util.Set.of("north", "south"))) return "north_south";
            if (set.equals(java.util.Set.of("east", "west"))) return "east_west";
            String ns = set.contains("north") ? "north" : "south";
            String ew = set.contains("east") ? "east" : "west";
            return ns + "_" + ew;
        }
        return asc + String.join("_", out);
    }

    public static BlockStateEntry apply(BlockStateEntry st, int rotation, boolean mx, boolean mz) {
        if (st.properties().isEmpty()) return st;
        final int rot = rotation & 3;
        Map<String, String> in = st.properties();
        Map<String, String> p = new HashMap<>(in);
        boolean mirrored = mx ^ mz; // ungerade Zahl an Spiegelungen kehrt die Haendigkeit um

        // 1) Spiegeln
        if (mx || mz) {
            if (p.containsKey("facing")) p.put("facing", mirDir(p.get("facing"), mx, mz));
            if (p.containsKey("rotation")) {
                int r = parseInt(p.get("rotation"));
                if (mx) r = (16 - r) & 15;
                if (mz) r = (8 - r) & 15;
                p.put("rotation", String.valueOf(r));
            }
            if (p.containsKey("shape")) {
                String sh = p.get("shape");
                if (sh.startsWith("inner_") || sh.startsWith("outer_")) {
                    if (mirrored) p.put("shape", sh.endsWith("_left")
                        ? sh.substring(0, sh.length() - 4) + "right"
                        : sh.substring(0, sh.length() - 5) + "left");
                } else if (sh.equals("straight")) {
                    // bleibt
                } else {
                    p.put("shape", rail(sh, d -> mirDir(d, mx, mz)));
                }
            }
            if (mirrored) {
                if (p.containsKey("hinge")) p.put("hinge", swapLR(p.get("hinge"), "left", "right"));
                if (p.containsKey("type")) p.put("type", swapLR(p.get("type"), "left", "right"));
            }
            swapSides(p, mx, mz);
        }

        // 2) Drehen
        if (rot != 0) {
            if (p.containsKey("facing")) p.put("facing", rotDir(p.get("facing"), rot));
            if (p.containsKey("axis") && (rot & 1) == 1) {
                String a = p.get("axis");
                if (a.equals("x")) p.put("axis", "z"); else if (a.equals("z")) p.put("axis", "x");
            }
            if (p.containsKey("rotation")) p.put("rotation", String.valueOf((parseInt(p.get("rotation")) + 4 * rot) & 15));
            if (p.containsKey("shape")) {
                String sh = p.get("shape");
                if (!sh.startsWith("inner_") && !sh.startsWith("outer_") && !sh.equals("straight"))
                    p.put("shape", rail(sh, d -> rotDir(d, rot)));
            }
            rotateSides(p, rot);
        }
        return new BlockStateEntry(st.name(), p);
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    private static boolean hasSides(Map<String, String> p) {
        return p.containsKey("north") || p.containsKey("south") || p.containsKey("east") || p.containsKey("west");
    }

    private static void swapSides(Map<String, String> p, boolean mx, boolean mz) {
        if (!hasSides(p)) return;
        Map<String, String> old = new HashMap<>();
        for (String d : CW) if (p.containsKey(d)) old.put(d, p.get(d));
        for (var e : old.entrySet()) p.remove(e.getKey());
        for (var e : old.entrySet()) p.put(mirDir(e.getKey(), mx, mz), e.getValue());
    }

    private static void rotateSides(Map<String, String> p, int steps) {
        if (!hasSides(p)) return;
        Map<String, String> old = new HashMap<>();
        for (String d : CW) if (p.containsKey(d)) old.put(d, p.get(d));
        for (var e : old.entrySet()) p.remove(e.getKey());
        for (var e : old.entrySet()) p.put(rotDir(e.getKey(), steps), e.getValue());
    }
}
