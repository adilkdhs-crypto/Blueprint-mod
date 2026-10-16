package dev.blueprint.schematic;

import java.util.Map;
import java.util.TreeMap;

/** Ein Blockzustand aus der Palette, z. B. minecraft:oak_stairs[facing=north,half=bottom]. */
public record BlockStateEntry(String name, Map<String, String> properties) {

    public BlockStateEntry {
        properties = properties == null ? Map.of() : new TreeMap<>(properties);
    }

    public boolean isAir() {
        return name.equals("minecraft:air")
            || name.equals("minecraft:cave_air")
            || name.equals("minecraft:void_air");
    }

    /** Schreibweise wie im Spiel: name[key=value,...] */
    public String asString() {
        if (properties.isEmpty()) return name;
        StringBuilder sb = new StringBuilder(name).append('[');
        boolean first = true;
        for (var e : properties.entrySet()) {
            if (!first) sb.append(',');
            sb.append(e.getKey()).append('=').append(e.getValue());
            first = false;
        }
        return sb.append(']').toString();
    }
}
