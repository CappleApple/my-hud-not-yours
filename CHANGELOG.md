# Changelog

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
