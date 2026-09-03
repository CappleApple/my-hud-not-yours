# Changelog

## 1.4.1 - 2026-09-03

### Changed

- Newly discovered HUD elements now start locked to default and must be explicitly unlocked before they can be modified.
- Existing untouched layouts migrate to the locked state while previously customized layouts retain their saved lock choice.

### Fixed

- Custom Iron's mana bars use the same integer maximum as Iron's renderer, so increased or fractional max-mana attributes hide correctly when full.

## 1.4 - 2026-08-31

### Added

- A per-bar Show on idle toggle that uses the existing Hide delay and Fade out timing after semantic data stops changing.
- Child of relationships with inherited visibility, translation, and scale while retaining independent child-local movement and scaling.
- Visible-only Stack On relationships that center an element above its target and expose independent Stack X and Stack Y offsets.

### Changed

- Relationship selectors reject direct and indirect parent/stack cycles and preserve an element's visible transform when parenting or unparenting.
- Existing configurations migrate with Show on idle enabled so 1.3 visibility behavior remains unchanged until explicitly configured.

## 1.3 - 2026-08-26

### Added

- Per-bar Hide delay and Fade out timing controls for full, empty, and creative-mode visibility rules.

### Changed

- Health bars remain visible whenever absorption is present, including when absorption makes the effective fill full.
- Conditional visibility timing advances exclusively on client ticks and bars reappear immediately when their hide condition clears.

### Fixed

- Iron's Spells mana replacements retain full-mana samples while the native contextual overlay is hidden, so the first reappearance trails from full mana.
- Hide delay and fade-out timing now work for contextually hidden Iron's mana bars instead of preventing their replacement from rendering.

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
