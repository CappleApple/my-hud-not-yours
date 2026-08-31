# My HUD Not Yours

My HUD Not Yours is a client-side NeoForge 1.21.1 HUD editor. Bind **Unlock HUD** in Minecraft's Controls menu, open it in a world, select an element, and manipulate it directly.

The key is unbound by default.

Layouts save automatically to:

```text
config/myhudnotyours/hud-layout.json
```

## Player Workflow

* Left-drag an element to move it.
* Use the mouse wheel to change its independent scale.
* Hold Ctrl while dragging to bypass snapping.
* Hold Shift while dragging for fine movement.
* Middle-click an element, or use **Reset this element**, to restore its original placement and rendering.
* Use the element list to select overlapping, hidden, or conditionally visible elements.
* Selecting an element from the list requests a temporary unsuppressed preview without saving a configuration change. If its renderer still cannot produce pixels, its last-known/configured bounding box and name remain visible and draggable until selection changes.
* Closing and reopening the editor during the same game run restores the last selected element, its preview behavior, and its visible row in the list. This selection is never written to disk.
* Search elements by name, layer ID, namespace, or type.
* Modified elements appear first in the element list with a yellow outline.
* **Lock to default** elements appear last with a cyan outline and barrier icon. They completely bypass renderer interception, show no world marker, and remain available as passive snap targets.
* Anchor elements to any screen corner, edge center, or center.
* Anchor-relative offsets remain stable across resolution, aspect-ratio, fullscreen, and GUI-scale changes.
* Every supported element provides **Original** and **Hidden** modes.
* Data-linked bars additionally provide **Custom** and **Boss Bar** modes.
* Elements can be independently shown or suppressed in creative mode.
* Data-linked bars can hide themselves when empty or full, with configurable hide delay and fade-out duration.
* **Show on idle** can hide a bar after its semantic value/effects stop changing, using the same Hide delay and Fade out timing.
* **Child of** links an element to another element's visibility and overall transform while preserving independent child-local movement and scaling.
* **Stack On** conditionally centers an element above a visible target with editable Stack X and Stack Y offsets.
* Numeric fields support direct entry and mouse-wheel adjustment.
* Custom bars can grow in width and/or height as their semantic maximum increases.
* Color controls support a color wheel, hue and brightness adjustment, opacity, and direct `#RRGGBB` or `#RRGGBBAA` input.
* Each bar remembers its most recently used colors.
* Visible elements have no persistent selection box. Hovering an editable element or dragging the selected element temporarily shades its hitbox translucent blue and shows its name.

The property panel is divided into:

* **Basic** — mode, anchor, position, scale, source, dimensions, direction, and text.
* **Style** — procedural and texture layers, colors, opacity, borders, texture sizing, and independent X/Y/width/height transforms for each bar layer.
* **Texture** — Background, Frame, Filled, Empty, and Trail textures.
* **Trail** — increase/decrease tracking, delay, catch-up duration, color, opacity, and texture.

Element-level scaling composes over every layer-local transform, border, and text part. Custom bars work without textures and start as complete procedural solid-color bars.

## Texture Browser and Imports

The built-in texture browser can search the currently active Minecraft, mod, resource-pack, and imported textures.

Search matches namespaces, paths, and source-pack names. Texture previews include dimensions and animation state. Hovering a card shows its complete wrapped reference, namespace, path, source pack, category, resolved resource, texture-sheet and source-region dimensions, and animation state.

**Import PNG** opens the platform file picker and copies the selected image into:

```text
config/myhudnotyours/textures/
```

A neighboring `name.png.mcmeta` file is imported with it when present.

Imported textures become available immediately without restarting the game or creating a resource pack.

Standard Minecraft animation metadata is supported, including:

* Default frame strips
* Explicit frame indices
* Per-frame durations
* Custom frame dimensions

Texture layers support:

* Stretch
* Tile
* Segmented, with one repeated texture cell per configurable amount of the semantic maximum
* 9-slice with configurable left, top, right, and bottom margins

**Max/Seg** appears in the Style panel whenever the selected layer uses Segmented sizing. The default is `2.0`, producing ten cells for a source maximum of 20 and twenty cells for a source maximum of 40. Current value, health absorption, and delayed trails change how those cells are filled or overlaid; only the semantic maximum changes the number of cells.

If a configured texture disappears, only that texture layer is skipped.

## Semantic Bar Sources

Built-in sources include:

* Health
* Absorption
* Armor
* Hunger
* Air
* Experience progress
* Mount health
* Mount jump charge

Vanilla health, armor, hunger, air, experience, mount health, and mount jump layers are linked automatically.

Custom and Boss Bar health replacements preserve vanilla health presentation state. Poison, wither, and full freezing tint the ordinary-health portion using vanilla's precedence, while absorption occupies a distinct overlay in the configured fill direction. Any active absorption keeps the health bar visible regardless of its full/empty visibility rules. Withered absorption uses the wither treatment instead of gold, matching vanilla hearts. These overlays reuse the configured Filled layer texture, opacity, scale mode, and layer-local transform; numeric text continues to report ordinary health and maximum health.

A bar's data source can also be changed manually in the editor.

The Basic panel provides **Width/Max** and **Height/Max**. Each value is the number of unscaled pixels added per one point that the semantic maximum rises above the maximum observed when dynamic sizing was first enabled. Both default to `0`, so existing bars remain fixed-size. Returning both values to `0` clears the baseline; enabling either again captures the then-current maximum. Changing the bar source also captures a fresh baseline.

Conditional sources such as air or mount health do not render replacements while inactive, except for the temporary list-selection preview described above.

### Iron's Spells 'n Spellbooks

When Iron's Spells 'n Spellbooks is installed, its mana overlay receives full bar integration.

The mana layer supports:

* Original
* Custom
* Boss Bar
* Hidden

and exposes the source:

```text
irons_spellbooks:mana
```

Visibility follows Iron's own mana-bar visibility rules. Its synchronized full-mana sample remains tracked while Iron's contextual overlay is hidden, allowing Custom and Boss Bar replacements to honor Hide delay/Fade out and to trail from full mana on their first reappearance.

## Delayed Value Trail

Bars can display a delayed trail when their value changes.

Available modes are:

* Off
* Decrease Only
* Increase Only
* Both

The primary value updates immediately. Delay and catch-up timing advance exclusively on the 20 TPS client tick; configured milliseconds round up to whole 50 ms ticks, and render partial ticks only smooth between tick endpoints.

Repeated qualifying changes restart the delay while preserving the outer trail value. In Decrease Only, healing below the retained high updates the live destination without canceling, restarting, or pausing the trail. Increase Only applies the same rule in reverse for damage above the retained low.

When a bar is configured to hide at full or empty, that hidden endpoint remains a tick-sampled trail value. Its first visible change therefore trails from full or empty immediately instead of using the newly visible value as a fresh baseline.

The Basic panel's **Hide delay** waits before a full, empty, or creative-mode hide begins. **Fade out** controls the subsequent fade duration. Both timers run on the 20 TPS client tick, render partial ticks only smooth opacity between tick samples, and clearing the hide condition restores the bar immediately. A bar that is already hidden when a world loads does not flash during initialization.

When **Show on idle** is false, changes to the source's current value, range, absorption, or health-effect presentation reveal the bar immediately. The next unchanged client tick starts the same Hide delay/Fade out sequence. The first sample after loading establishes an idle baseline and does not flash the bar.

## Element Relationships

**Child of** stores the linked element's position and scale as a local transform beneath its parent. Linking and unlinking preserve the child's current on-screen bounds. Moving or scaling the parent composes through every descendant, while dragging, scaling, or directly editing the child changes only its own local transform. Parent visibility and fade opacity multiply through the chain.

**Stack On** is a separate conditional placement rule. While its target is visible, the element is horizontally centered immediately above the target, plus its Stack X and Stack Y offsets. When the target is not visible, the element returns to its ordinary anchored or parent-local position. Dragging while the stack rule is active edits the stack offsets. Self-links and indirect cycles across either relationship are excluded.

## HUD Discovery and Compatibility

My HUD Not Yours automatically discovers named NeoForge GUI layers and uses their stable layer IDs to identify HUD elements.

Unmodified and reset elements remain visually untouched until explicitly changed in the editor. Locked elements use an even stronger bypass that returns before renderer tracking, transforms, cancellation, replacement rendering, or stacking shims.

Unknown mod HUD layers can often still be moved, scaled, hidden, and selected without dedicated compatibility code.

The full-screen vanilla `camera_overlays` layer is intentionally excluded. Effects such as the vignette, portal overlay, spyglass mask, freezing overlay, and helmet overlays are not treated as movable HUD elements.

The editor's debug information can show:

* Layer ID
* Namespace
* Render path
* Bounds
* Classification
* Data source
* Render order
* Observed texture atlases

Vanilla numeric layers share HUD stacking counters. Hidden or value-suppressed bars consume no row, while visible replacements preserve one row. A Custom or Boss Bar health replacement is normalized to one row regardless of boosted maximum health so armor and later overlays stack correctly.

### Technical Limits

* Mods that combine several widgets into one inseparable GUI layer are exposed as one composite element unless explicit integration is added.
* Rendering performed outside normal GUI rendering paths may not provide an automatically detectable click region.
* HUD rendering that is not exposed as an independently cancellable GUI layer cannot safely be controlled individually.
* Generic opacity is not applied to arbitrary third-party original rendering. Custom bar layers support independent opacity.
* Texture enumeration is performed only when the browser is opened, and large packs may contain many results.

These limitations do not prevent explicit integrations or additional semantic sources from being added later.

## Configuration Resilience

Layouts preserve settings for temporarily missing mods, layers, and textures.

Newly discovered HUD elements are added without deleting unknown configuration.

Existing version-1 layouts are migrated automatically.

Stored layouts include element identity, placement, scale, mode, visibility, bar source and dimensions, styles, textures, layer-local transforms, colors, 9-slice settings, and trail configuration.

## Development

Requires Java 21 and NeoForge 21.1.244 for Minecraft 1.21.1.

```powershell
$env:JAVA_HOME='C:\path\to\jdk-21'
.\gradlew.bat test build
.\gradlew.bat runClient
```
