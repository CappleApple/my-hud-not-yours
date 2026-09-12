# My HUD Not Yours

My HUD Not Yours is a client-side HUD editor for NeoForge 1.21.1.

It lets you move, resize, hide, restyle, and reconnect HUD elements directly in-game instead of editing coordinates in config files. Vanilla HUD pieces work out of the box, and many modded HUD layers can be discovered automatically.

Bind **Unlock HUD** in Minecraft's Controls menu to open the editor. The key is unbound by default.

Layouts are saved to:

```text
config/myhudnotyours/hud-layout.json
```

## Basic editing

- Drag an element to move it.
- Use the mouse wheel to change its scale.
- Hold Ctrl while dragging to ignore snapping.
- Hold Shift for finer movement.
- Middle-click an element to reset it.
- Use the element list when HUD pieces overlap or are normally hidden.
- Elements start locked and must be unlocked before editing.

Positions are anchor-based, so layouts can survive changes in resolution, aspect ratio, fullscreen mode, and GUI scale much better than raw screen coordinates.

Each supported element can be left **Original**, replaced with a custom version where available, or **Hidden**.

## Custom bars

Data-backed HUD bars such as health, armor, hunger, air, and experience can be rebuilt with the editor rather than only moved around.

Custom bars support:

- Adjustable width and height
- Horizontal or vertical fill directions
- Solid-color rendering without textures
- Background, frame, filled, empty, and trail texture layers
- Independent layer transforms
- Borders and opacity
- Dynamic sizing as a source's maximum value changes
- Delayed increase/decrease trails
- Hide-at-full, hide-at-empty, and idle hiding
- Fade timing
- Numeric text

A bar can also use another element as a parent or stack itself above another visible HUD element, which makes it possible to build groups without hard-coding all of their positions.

## Texture browser

The editor includes a texture browser for Minecraft, mods, active resource packs, and textures imported by the player.

PNG files can be imported directly into:

```text
config/myhudnotyours/textures/
```

A matching `.png.mcmeta` file is copied with the texture when present, so normal Minecraft animated textures work too.

Texture layers can be stretched, tiled, segmented, or rendered with 9-slice margins.

## Built-in data sources

My HUD Not Yours includes sources for:

- Health
- Absorption
- Armor
- Hunger
- Air
- Experience progress
- Mount health
- Mount jump charge

Vanilla bars are linked automatically. Replacements still respect relevant vanilla states such as poison, wither, freezing, and absorption instead of treating health as a plain number.

### Iron's Spells 'n Spellbooks

When Iron's Spells 'n Spellbooks is installed, its mana HUD can also be edited through the normal bar tools.

The source ID is:

```text
irons_spellbooks:mana
```

Iron's remains optional; there is no hard dependency.

## Element relationships

**Child of** makes one HUD element follow another element's position, scale, visibility, and fade while keeping its own local offset.

**Stack On** places an element above another element only while the target is visible. This is useful for bars or labels that should collapse naturally when another HUD piece disappears.

The editor prevents direct and indirect relationship loops.

## Modded HUD support

The mod watches NeoForge GUI layers and identifies elements by their layer IDs. This means many simple modded HUDs can be moved, scaled, or hidden without a dedicated compatibility module.

Some renderers are too unusual to manipulate safely, so automatic support is not guaranteed for every mod. Full-screen camera effects such as the vignette, portal overlay, spyglass mask, freezing overlay, and helmet overlays are intentionally not treated as movable HUD elements.

Debug information in the editor can help identify a troublesome layer by showing its ID, namespace, bounds, render path, data source, and observed textures.

## Notes for pack makers

The editor is designed so a pack can ship a layout while still letting players adjust it themselves. Elements that have not been customized can remain locked to their normal rendering, which keeps the mod out of the way when no edit is needed.

Imported texture support also makes it possible to distribute a custom HUD style without replacing Minecraft's global GUI textures.

## Requirements

- Minecraft 1.21.1
- NeoForge
- Java 21

This is a client-side mod.

## Building from source

```bash
./gradlew build
```

Windows:

```powershell
.\gradlew.bat build
```

The built jar is written to `build/libs/`.
