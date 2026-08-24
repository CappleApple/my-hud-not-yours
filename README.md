# My HUD Not Yours

My HUD Not Yours is a client-side NeoForge 1.21.1 HUD editor. Bind **Unlock HUD** in Minecraft's Controls menu, open it in a world, click an element, and manipulate it directly. The key is unbound by default. Layouts save automatically to `config/myhudnotyours/hud-layout.json`. Existing `config/elementsnotscreens` layouts and imported textures are copied forward automatically when the new location does not yet contain a layout; the legacy folder is retained as a backup.

## Player workflow

- Left-drag a visible element to move it.
- Use the mouse wheel to change its independent scale.
- Hold Ctrl while dragging to bypass snapping; hold Shift for fine movement.
- Middle-click an element, or use **Reset this element**, to restore its original placement and rendering.
- Use the element list to select overlapping, conditionally visible, or hidden elements.
- Selecting a currently hidden or conditional element shows its last-known/configured bounds, which remain directly draggable and middle-click resettable.
- Use the search field above the element list to filter by name, layer ID, namespace, or element type.
- Modified elements appear first with a yellow outline in the element list; resetting one removes the outline, restores strict vanilla passthrough, and returns it to the untouched group.
- **Lock to default** elements appear last with a solid cyan outline and barrier icon. The selected locked element exposes only its unlock control, has no world bounding box, and completely bypasses renderer interception.
- Change the anchor to any screen corner, edge center, or center. Offsets remain relative to that anchor across window sizes, aspect ratios, fullscreen changes, and GUI scales.
- Use **Original** or **Hidden** for every intercepted named layer.
- Data-linked bars additionally provide **Custom** and **Boss Bar** modes.
- Every element can be shown or suppressed in creative mode. Data-linked bars can also hide themselves when empty or full.
- Click any numeric value field to type an exact value. Enter, Tab, or clicking away commits it; Escape cancels it. Scrolling over a numeric field changes that field using the same increment as its minus/plus buttons.
- Click a layer's hex color button to open the color picker. It provides a color wheel, hue and brightness sliders, an opacity slider, and direct `#RRGGBB`/`#RRGGBBAA` entry.
- The nine quick-color swatches begin with the built-in presets. Each bar persists its own most-recent colors, which appear first and progressively replace older preset slots.

The property panel is intentionally compact:

- **Basic** controls mode, anchor, position, scale, source, dimensions, direction, and text.
- **Style** controls each procedural/texture layer, colors (wheel, sliders, hex, and per-bar recents), opacity, border, and texture sizing.
- **Texture** assigns Background, Frame, Filled, Empty, and Trail independently.
- **Trail** controls decrease/increase tracking, delay, catch-up duration, color, opacity, and texture.

Custom mode starts as a complete procedural solid-color bar. No texture is required.

## Texture browser and imports

The texture browser enumerates the active resource view only when opened and caches the result. Its tabs distinguish Minecraft resources, mod resources, overridden/pack resources, and imports. Search matches namespaces, paths, and source-pack names. Visible previews show dimensions and animation state.

**Import PNG** opens the platform Java file picker. The selected PNG is copied into `config/myhudnotyours/textures/`; a neighboring `name.png.mcmeta` is copied too. Imported files appear immediately without a game restart or resource-pack authoring.

Selected resource and imported textures are loaded through the same managed texture path. Standard Minecraft `animation` metadata is supported, including default frame strips, explicit frame indices, per-frame durations, and custom frame dimensions. Texture layers support:

- Stretch
- Tile
- 9-slice with editable left/top/right/bottom margins

If a configured texture disappears, that layer is skipped safely and the rest of the bar continues rendering.

## Semantic bar sources

Built-in client sources are:

- health
- absorption
- armor
- hunger
- air
- experience progress
- mount health
- mount jump charge

Vanilla health, armor, hunger, air, experience, mount health, and mount jump named layers are linked automatically. A bar's source can also be changed in the editor. Inactive conditional sources, such as air while the player is breathing normally or mount health while dismounted, do not draw a replacement.

Iron's Spells 'n Spellbooks' `irons_spellbooks:mana_overlay` has a full optional semantic integration. Current mana comes from its synchronized `ClientMagicData`, maximum mana comes from its syncable `max_mana` player attribute, and visibility follows its own `ManaBarOverlay.shouldShowManaBar` rule. The integration is loaded only when Iron's Spells is present and enables Original, Custom, Boss Bar, and Hidden modes with the `irons_spellbooks:mana` source.

The delayed-value trail supports Off, Decrease Only, Increase Only, and Both. A qualifying change updates the primary value immediately, holds the previous trail value for the configured delay, and then catches up with smoothstep easing. Repeated qualifying changes preserve the readable outer value and restart the delay.

## Discovery and compatibility model

NeoForge 1.21.1 gives every registered GUI layer a stable `ResourceLocation` and fires cancellable pre/post events around that layer. My HUD Not Yours uses that layer ID as its primary stable identity. Freshly discovered and reset elements are strict vanilla passthroughs: the mod observes their bounds for editor selection but does not cancel, replace, tint, or transform their renderers. A deliberate editor change sets the persisted `customized` flag. Modpack-authored layouts can set the same flag directly, and authored placement/scale/mode differences are detected automatically. Customized original renderers use a scoped pose transform, keeping their textures, animation, and mod updates intact. `lockedToDefault` is a stronger per-element bypass: after discovery keeps the element selectable in the list, the interceptor returns before bounds tracking, transformations, cancellation, replacement rendering, or stacking shims.

The vanilla `camera_overlays` layer is deliberately excluded before discovery or interception. Full-screen vignette, portal, spyglass, frozen-screen, and helmet effects are render effects rather than placeable HUD elements, so the mod never exposes or touches them.

While a layer renders, a small client mixin observes common `GuiGraphics` primitives—fills, texture blits, text, and items—and collects their transformed union. Unknown mod layers therefore remain selectable as opaque regions without per-mod integration. Debug details in the editor show the stable ID, namespace, render path, bounds, classification, source, render order, and observed texture atlases.

Vanilla numeric layers share `Gui.leftHeight` and `Gui.rightHeight` bookkeeping. When one is hidden or replaced, its renderer is executed under an empty scissor far offscreen before cancellation. This preserves non-layout side effects without leaking the original visuals. Hidden or value-suppressed bars have their stack counters restored so they consume no row; visible replacement bars preserve one row for later vanilla layers.

When vanilla Health is Custom or Boss Bar, its preserved `leftHeight` contribution is normalized to one row. Armor and later overlays such as the selected-item name therefore stack as though health occupied one vanilla row, regardless of boosted maximum health.

### Honest technical limits

- A mod that registers several inseparable widgets as one NeoForge layer is exposed as one composite region. Splitting it would require explicit integration.
- Rendering performed directly through OpenGL, a custom framebuffer, or a post-processing pass can bypass `GuiGraphics` bounds collection. Its named layer can still be hidden and transformed if it respects the active pose, but an automatic click region may be unavailable.
- HUD rendering performed only from a broad event and not registered as a named GUI layer has no safe independent cancellation boundary. The mod leaves it alone rather than risk corrupting unrelated rendering.
- Generic opacity is not applied to original third-party rendering because no scoped global alpha is safe for every render path. Custom bar layers have independent opacity.
- Resource enumeration is deliberately on-demand. The browser can contain many entries in large packs, but image decoding is limited to visible/cached previews.

These boundaries are extension points, not claims of semantic integration. `BarSourceRegistry` accepts deeper numeric sources, while the layer registry remains independent of future hotbar, selector, text, composite-child, or profile work.

## Configuration resilience

The pretty-printed JSON keeps settings for absent layers and missing mods. Discovery adds new elements without deleting unknown configuration. Saves use a temporary file and atomic replacement where the filesystem supports it. Stored data includes stable identity, type, the explicit `customized` passthrough flag, hard-lock and visibility flags, anchor-relative offsets, scale, mode, last native bounds, bar source, dimensions, direction, text, every layer style/texture, per-bar recent colors, 9-slice margins, and trail settings. Version-1 layouts migrate moved, scaled, hidden, or replaced elements as customized while leaving untouched entries inactive.

## Development

Requirements: Java 21 and NeoForge 21.1.244 for Minecraft 1.21.1.

```powershell
$env:JAVA_HOME='C:\path\to\jdk-21'
.\gradlew.bat test build
.\gradlew.bat runClient
```

The unit suite covers anchor persistence across resolutions, strict-passthrough migration/reset behavior, screen/element snapping, decrease and increase trails, repeated damage delay resets, and standard animated texture metadata. `check` also inspects the release JAR for required metadata, mixin configuration, and translations.
