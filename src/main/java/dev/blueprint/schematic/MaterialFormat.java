package dev.blueprint.schematic;

public final class MaterialFormat {
    private MaterialFormat() {}

    /** 150 -> "2 Stacks + 22". stackSize ist meist 64. */
    public static String stacks(int count, int stackSize) {
        int full = count / stackSize, rest = count % stackSize;
        if (full == 0) return String.valueOf(rest);
        return rest == 0 ? full + " Stacks" : full + " Stacks + " + rest;
    }

    /** "minecraft:oak_planks" -> "Oak Planks" (Platzhalter, bis echte Uebersetzung angebunden ist). */
    public static String prettyName(String id) {
        String s = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        StringBuilder sb = new StringBuilder();
        for (String part : s.split("_")) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}
