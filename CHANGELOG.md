# Changelog

## 1.2 - 2026-08-26

### Added

- Automatic poison, wither, and frozen visual treatment for custom health bars and Boss Bar health replacements.
- A distinct absorption segment that shares the configured fill texture and follows the bar's fill direction and layer transform.
- Segmented texture sizing with a direct-entry and scroll-adjustable maximum-per-segment option in the Style panel.
- Direct-entry and scroll-adjustable Width/Max and Height/Max controls for growing a bar as its semantic maximum increases.

### Changed

- Health-bar rendering and delayed trails now use effective health while keeping numeric text based on ordinary health and maximum health.
- Withered absorption uses the wither treatment, matching vanilla heart rendering; poison and frozen health retain a gold absorption segment.
- Segment count responds only to semantic maximum health; current health, absorption, and trails affect fill without creating or removing cells.
- Generalized the segmented Style label from Max HP/segment to Max/Seg so it applies to every semantic bar source.
- Health-state overlays now use opaque, high-saturation colors; Boss Bar replacements use matching green, blue, yellow, or tinted-white vanilla progress sprites instead of blending every state into the red sprite.

### Fixed

- Preserved full/empty endpoint trail samples while a value-hidden bar is suppressed, so its first visible change trails from the hidden endpoint instead of snapping to the new value.

## 1.1 - 2026-08-24

### Added

- Independent X/Y/width/height transforms for Background, Frame, Filled, Empty, and Trail bar layers.
- Full wrapped texture details when hovering entries in the texture browser.
- Temporary list-selection previews with persistent bounds/name fallback for elements that cannot render.
- In-memory restoration of the last selected element when reopening the HUD editor.
- Snapping against locked elements without enabling their renderer interception or editor bounds.

### Changed

- Replaced persistent selection boxes with translucent blue hover/drag feedback for visible elements.
- Made delayed bar trails advance exclusively on client ticks with render-only partial-tick interpolation.
- Made overall HUD scaling compose over every custom bar layer, border, and text part.

### Fixed

- Prevented healing from canceling or restarting an active Decrease Only trail below its retained high.
- Prevented damage from canceling or restarting an active Increase Only trail above its retained low.
- Preserved configured trail delay/catch-up timing through opposite-direction value updates.
