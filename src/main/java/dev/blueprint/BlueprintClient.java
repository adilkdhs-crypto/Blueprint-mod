package dev.blueprint;

import dev.blueprint.schematic.GhostModel;
import dev.blueprint.schematic.LitematicReader;
import dev.blueprint.schematic.Placement;
import dev.blueprint.schematic.MaterialFormat;
import dev.blueprint.schematic.Region;
import dev.blueprint.schematic.Schematic;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class BlueprintClient implements ClientModInitializer {
    public static final String MOD_ID = "blueprint";
    private KeyMapping loadKey;
    private KeyMapping materialsKey;
    private KeyMapping rotateKey;
    private KeyMapping mirrorXKey;
    private KeyMapping mirrorZKey;
    private KeyMapping anchorKey;
    private KeyMapping ghostKey;
    private static Schematic currentSchematic;
    private static Placement placement = new Placement(0, 0, 0);

    @Override
    public void onInitializeClient() {
        loadKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.blueprint.load", GLFW.GLFW_KEY_F8, KeyMapping.Category.MISC));
        materialsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.blueprint.materials", GLFW.GLFW_KEY_F7, KeyMapping.Category.MISC));
        rotateKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.blueprint.rotate", GLFW.GLFW_KEY_F6, KeyMapping.Category.MISC));
        mirrorXKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.blueprint.mirror_x", GLFW.GLFW_KEY_F5, KeyMapping.Category.MISC));
        mirrorZKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.blueprint.mirror_z", GLFW.GLFW_KEY_F4, KeyMapping.Category.MISC));
        anchorKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.blueprint.anchor", GLFW.GLFW_KEY_F9, KeyMapping.Category.MISC));
        ghostKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.blueprint.ghost", GLFW.GLFW_KEY_F10, KeyMapping.Category.MISC));
        GhostRenderer.init();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (loadKey.consumeClick()) loadFirstSchematic(client);
            while (materialsKey.consumeClick()) showMaterials(client);
            while (rotateKey.consumeClick()) { placement.rotateClockwise(); refreshGhosts(); announcePlacement(client); }
            while (mirrorXKey.consumeClick()) { placement.toggleMirrorX(); refreshGhosts(); announcePlacement(client); }
            while (mirrorZKey.consumeClick()) { placement.toggleMirrorZ(); refreshGhosts(); announcePlacement(client); }
            while (anchorKey.consumeClick()) { moveAnchorToPlayer(client); refreshGhosts(); announcePlacement(client); }
            while (ghostKey.consumeClick()) {
                boolean on = GhostRenderer.toggleVisible();
                if (client.player != null) client.player.displayClientMessage(
                    Component.literal("[Blueprint] Geisteranzeige " + (on ? "an" : "aus")), true);
            }
        });
        System.out.println("[Blueprint] geladen – F8 laden, F8 laden, F7 Materialliste, F6 drehen, F5/F4 spiegeln, F9 Anker versetzen, F10 Anzeige an/aus");
    }

    private static void showMaterials(Minecraft client) {
        if (client.player == null) return;
        if (currentSchematic == null) {
            client.player.displayClientMessage(Component.literal("[Blueprint] Erst eine Datei mit F8 laden."), false);
            return;
        }
        var materials = currentSchematic.materialList();
        client.player.displayClientMessage(Component.literal("[Blueprint] Materialliste für „" + currentSchematic.name + "“ (ohne Luft):"), false);
        int shown = 0;
        for (var entry : materials.entrySet()) {
            if (shown++ >= 20) break;
            client.player.displayClientMessage(Component.literal(" • " + MaterialFormat.prettyName(entry.getKey()) + ": "
                + entry.getValue() + " (" + MaterialFormat.stacks(entry.getValue(), 64) + ")"), false);
        }
        if (materials.size() > 20) client.player.displayClientMessage(Component.literal("… und " + (materials.size() - 20) + " weitere Blockarten."), false);
        client.player.displayClientMessage(Component.literal("Gesamt: " + materials.values().stream().mapToInt(Integer::intValue).sum() + " Blöcke."), false);
    }

    /** Berechnet die Geisterbloecke neu (nach Laden, Drehen, Spiegeln, Verschieben). */
    private static void refreshGhosts() {
        if (currentSchematic == null) { GhostRenderer.setGhosts(java.util.List.of()); return; }
        GhostRenderer.setGhosts(GhostModel.build(placement.place(currentSchematic)));
    }

    private static void moveAnchorToPlayer(Minecraft client) {
        if (client.player == null) return;
        placement.anchorX = client.player.getBlockX();
        placement.anchorY = client.player.getBlockY();
        placement.anchorZ = client.player.getBlockZ();
    }

    /** Zeigt Platzierung und den Welt-Umriss, den die Schematic damit belegen wuerde. */
    private static void announcePlacement(Minecraft client) {
        if (client.player == null) return;
        String text = "[Blueprint] " + placement.describe();
        if (currentSchematic != null) {
            int[] b = placement.worldBounds(currentSchematic);
            text += " | Bereich " + b[0] + "," + b[1] + "," + b[2] + " bis " + b[3] + "," + b[4] + "," + b[5];
        }
        client.player.displayClientMessage(Component.literal(text), true);
    }

    private static void loadFirstSchematic(Minecraft client) {
        if (client.player == null) return;
        Path folder = client.gameDirectory.toPath().resolve("blueprint");
        try {
            Files.createDirectories(folder);
            List<Path> files;
            try (Stream<Path> paths = Files.list(folder)) {
                files = paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".litematic"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()))
                    .toList();
            }
            if (files.isEmpty()) {
                client.player.displayClientMessage(Component.literal("[Blueprint] Keine .litematic-Datei gefunden. Lege eine Datei in " + folder.toAbsolutePath()), false);
                return;
            }
            Schematic schematic = LitematicReader.read(files.get(0));
            currentSchematic = schematic;
            placement = new Placement(client.player.getBlockX(), client.player.getBlockY(), client.player.getBlockZ());
            refreshGhosts();
            int[] bounds = schematic.boundsSize();
            String author = schematic.author.isBlank() ? "unbekannt" : schematic.author;
            client.player.displayClientMessage(Component.literal("[Blueprint] Eingelesen: " + schematic.name
                + " | Autor: " + author + " | Regionen: " + schematic.regions.size()
                + " | Abmessungen: " + bounds[0] + "×" + bounds[1] + "×" + bounds[2]
                + " | Datei: " + files.get(0).getFileName()), false);
            client.player.displayClientMessage(Component.literal("[Blueprint] Anker auf deiner Position gesetzt. F7 Materialliste, F6 drehen, F5/F4 spiegeln."), false);
        } catch (IOException | RuntimeException exception) {
            String reason = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            client.player.displayClientMessage(Component.literal("[Blueprint] Fehler beim Einlesen: " + reason), false);
        }
    }
}
