package dev.blueprint;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Method;

/**
 * Sendet Nachrichten an den Spieler. Die Methodennamen dafuer haben sich zwischen
 * Minecraft-Versionen geaendert (in 26.2 gibt es LocalPlayer.displayClientMessage nicht mehr),
 * deshalb werden mehrere bekannte Varianten der Reihe nach probiert.
 */
public final class Msg {
    private Msg() {}

    /** overlay = true: Actionbar; false: Chat. */
    public static void send(Minecraft client, Component text, boolean overlay) {
        if (client == null) return;
        Object gui = field(client, "gui");
        if (overlay && tryInvoke(gui, "setOverlayMessage", text, false)) return;
        if (tryInvoke(client.player, "sendSystemMessage", text)) return;
        Object chat = call(gui, "getChat");
        if (tryInvoke(chat, "addClientSystemMessage", text)) return;
        if (tryInvoke(chat, "addMessage", text)) return;
        if (tryInvoke(client.player, "displayClientMessage", text, overlay)) return;
        System.err.println("[Blueprint] Konnte Nachricht nicht senden: " + text.getString());
    }

    private static Object field(Object target, String name) {
        try { return target.getClass().getField(name).get(target); }
        catch (ReflectiveOperationException | RuntimeException e) { return null; }
    }

    private static Object call(Object target, String name) {
        if (target == null) return null;
        for (Method m : target.getClass().getMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == 0) {
                try { return m.invoke(target); } catch (ReflectiveOperationException | RuntimeException e) { return null; }
            }
        }
        return null;
    }

    private static boolean tryInvoke(Object target, String name, Object... args) {
        if (target == null) return false;
        for (Method m : target.getClass().getMethods()) {
            if (!m.getName().equals(name) || m.getParameterCount() != args.length) continue;
            Class<?>[] p = m.getParameterTypes();
            boolean ok = true;
            for (int i = 0; i < p.length && ok; i++) ok = accepts(p[i], args[i]);
            if (!ok) continue;
            try { m.invoke(target, args); return true; }
            catch (ReflectiveOperationException | RuntimeException e) { /* naechste Variante */ }
        }
        return false;
    }

    private static boolean accepts(Class<?> type, Object arg) {
        if (type == boolean.class) return arg instanceof Boolean;
        return arg != null && type.isInstance(arg);
    }
}
