# L1J Map Editor

The map editor is a Swing-based utility designed to visualize and edit the tile
CSV files found in `maps/*.txt`. It wraps the same `L1Map` APIs that the server
uses so designers can tweak tiles, passability and zone data with immediate
feedback.

## Features

- Load any `maps/<mapId>.txt` file produced by `L1V1Map#toCsv()`.
- Brush and rectangle painting modes that call `setOriginalTile()` behind the
  scenes.
- Per-tile passability toggles backed by `setPassable()`.
- Zone painting for normal, safety and combat regions using the existing zone
  bit conventions.
- Visual overlays that highlight passability and zone data.
- Palette panel with tool selection, tile ID picker and overlay toggles.
- Undo/redo history for quick iteration.
- Coordinate read-out that mirrors `L1Map#toString(Point)`.
- Live minimap preview that reflects the currently loaded map.
- Batch export that writes all loaded maps back to CSV using
  `L1Map.toCsv()`.

## Running the editor

1. Build the server jar (provides the shared `L1Map` classes) from the project
   root:

   ```sh
   ant jar
   ```

2. Launch the editor with the dedicated Ant build file:

   ```sh
   ant -f tool/map-editor/build.xml run
   ```

   The task compiles the Swing client and runs `tool.mapeditor.MapEditorApp`.

## Usage overview

- Click **Load Map** and enter a map ID (the filename without `.txt`). The map
  list tracks every loaded map.
- Choose a tile ID, zone type and passability state from the palette and paint
  with either the brush (single-tile, drag-to-paint) or rectangle tool
  (click-and-drag selection).
- Toggle the passability/zone overlays to inspect map attributes visually.
- The minimap panel mirrors the main canvas and updates after each edit.
- Use **Undo**/**Redo** to walk the edit history for the selected map.
- **Save Map** writes the currently selected map, while **Export All** writes
  every loaded map back to `maps/` with the current edits applied.

Maps are stored in-place, so consider keeping version control handy when
iterating on production data.
