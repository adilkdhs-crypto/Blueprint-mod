# Blueprint – Fabric-Projekt für Minecraft 26.x

## Was jetzt drin ist (getestet)
- `.litematic` (GZIP-NBT) einlesen: Regionen, negative Größen, Paletten-Bitpacking.
- Materialliste ohne Luft, Anzahl und 64er-Stacks.
- **Neu:** `BlockTransform` dreht/spiegelt Blockzustände mit
  (`facing`, `axis`, `rotation`, Treppenform, Schienen, Zaun-/Wand-Seiten, Tür-Scharnier, Truhen-Typ).
- **Neu:** `Placement.place(...)` liefert alle Blöcke in Weltkoordinaten mit passendem Zustand;
  `Placement.worldBounds(...)` den belegten Welt-Bereich.
- Tests (ohne JUnit, per `main`): `ParserSmokeTest`, `PlacementTest`, `BlockTransformTest`, `GhostModelTest`.
  Alle drei liefen erfolgreich (Java 21, reine Logik-Klassen).

## Tasten
`F8` laden · `F7` Materialliste · `F6` drehen · `F5`/`F4` spiegeln X/Z · `F9` Anker auf eigene Position · `F10` Anzeige an/aus.
Drehen/Spiegeln/Anker zeigen jetzt den Welt-Bereich in der Actionbar.

## Geisteranzeige (neu, ungetestet im Spiel)
`GhostRenderer` zeichnet die Schematic als halbtransparente, farbige Würfel (nur Oberfläche,
max. 1500 Stück im Umkreis von 48 Blöcken). Aufbau nach dem offiziellen Fabric-Beispiel
„Rendering in the World“ (26.2): `LevelExtractionEvents.END_EXTRACTION` +
`LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN` mit `RenderPipelines.DEBUG_FILLED_BOX`.
`GhostModel` (Oberfläche + Farbe) ist getestet. `F10` blendet die Anzeige ein/aus.

## Was NICHT fertig ist
- Keine echten Block-Texturen/-Modelle, nur einfarbige Würfel.
- Keine grafische Materialliste, kein Abgleich mit der echten Welt.
- **Nichts davon wurde gegen Minecraft kompiliert** (kein Gradle/Netz in meiner Umgebung).
  `BlueprintClient` (Mojang-Namen) und `GhostRenderer` können beim ersten Build kleine
  Namens-/Signaturfehler haben. Das Beispiel in der Doku gilt für 26.2.
- Der Vertex-Puffer wird beim Beenden nicht freigegeben (die Doku nutzt dafür ein Mixin
  auf `GameRenderer#close`; hier weggelassen).

## Bauen
**Ohne Installation (GitHub):** Projektordner als neues GitHub-Repository hochladen. Unter „Actions“ →
„build“ → „Run workflow“ starten; danach unter „Artifacts“ `blueprint-jar` herunterladen und die ZIP entpacken.
Die JAR `blueprint-0.1.0.jar` (nicht `-sources`) kommt nach `.minecraft/mods`.

**Lokal:** Java 25 + Gradle 9.5.1, dann `gradle build` → `build/libs/`.

Ziel ist Minecraft 26.2 (Loader 0.19.5, Loom 1.17-SNAPSHOT, Fabric API 0.161.0+26.2).
Die Versionen sind aus Doku/Drittquellen übernommen, nicht selbst geprüft: Falls Gradle eine Version
nicht findet, die aktuellen Werte auf https://fabricmc.net/develop/ nachsehen und in `gradle.properties` eintragen.
Zusätzlich gehört die Fabric-API-JAR in `mods`.

## Nutzung
JAR nach `.minecraft/mods`, `.litematic` nach `.minecraft/blueprint/`, im Spiel `F8`.
