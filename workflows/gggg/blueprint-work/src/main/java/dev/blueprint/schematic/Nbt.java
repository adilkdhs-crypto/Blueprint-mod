package dev.blueprint.schematic;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Minimaler NBT-Leser. Compound = Map, List = List, Arrays = byte[]/int[]/long[]. */
public final class Nbt {
    private Nbt() {}

    @SuppressWarnings("unchecked")
    public static Map<String, Object> readGzipped(InputStream in) throws IOException {
        try (DataInputStream data = new DataInputStream(
                new BufferedInputStream(new GZIPInputStream(in)))) {
            byte type = data.readByte();
            if (type != 10) throw new IOException("Wurzel ist kein Compound (Typ " + type + ")");
            data.readUTF(); // Wurzelname, unwichtig
            return (Map<String, Object>) readPayload(data, (byte) 10);
        }
    }

    private static Object readPayload(DataInputStream in, byte type) throws IOException {
        switch (type) {
            case 1: return in.readByte();
            case 2: return in.readShort();
            case 3: return in.readInt();
            case 4: return in.readLong();
            case 5: return in.readFloat();
            case 6: return in.readDouble();
            case 7: { byte[] a = new byte[in.readInt()]; in.readFully(a); return a; }
            case 8: return in.readUTF();
            case 9: {
                byte inner = in.readByte();
                int n = in.readInt();
                List<Object> list = new ArrayList<>(Math.max(0, n));
                for (int i = 0; i < n; i++) list.add(readPayload(in, inner));
                return list;
            }
            case 10: {
                Map<String, Object> map = new LinkedHashMap<>();
                while (true) {
                    byte t = in.readByte();
                    if (t == 0) break;
                    String name = in.readUTF();
                    map.put(name, readPayload(in, t));
                }
                return map;
            }
            case 11: { int[] a = new int[in.readInt()]; for (int i = 0; i < a.length; i++) a[i] = in.readInt(); return a; }
            case 12: { long[] a = new long[in.readInt()]; for (int i = 0; i < a.length; i++) a[i] = in.readLong(); return a; }
            default: throw new IOException("Unbekannter NBT-Typ " + type);
        }
    }

    // ---- Komfort-Zugriffe ----
    @SuppressWarnings("unchecked")
    public static Map<String, Object> compound(Map<String, Object> m, String key) {
        Object o = m.get(key);
        return o instanceof Map ? (Map<String, Object>) o : null;
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> compoundList(Map<String, Object> m, String key) {
        Object o = m.get(key);
        return o instanceof List ? (List<Map<String, Object>>) o : List.of();
    }

    public static int intOf(Map<String, Object> m, String key, int def) {
        Object o = m.get(key);
        return o instanceof Number n ? n.intValue() : def;
    }

    public static String stringOf(Map<String, Object> m, String key, String def) {
        Object o = m.get(key);
        return o instanceof String s ? s : def;
    }
}
