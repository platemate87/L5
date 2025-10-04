# Windows map editor (C#)

This WinForms rewrite of the Java map editor loads the same `maps/<mapId>.txt`
CSV layout produced by `L1V1Map#toCsv()` and wraps the data with the familiar
`L1Map` metadata flags. Designers can edit maps on any Windows 11 machine
without installing Java.

## Features

- Load `maps/<mapId>.txt` directly from the repository.
- Palette panel that lists every tile present in the current map.
- Brush and rectangle paint tools that call into `EditableL1Map.SetOriginalTile`.
- Passability three-state toggle (`SetPassable`) and zone selector (`SetZone`).
- Overlays to visualize passability and zone coverage.
- Undo/redo stack, coordinate readout, and minimap preview.
- Batch export back to CSV via the built-in `EditableL1Map.ToCsv()` bridge.

## Requirements

- .NET 8 SDK (LTS) on Windows (download from https://dotnet.microsoft.com/).
- No Java runtime is required for the C# editor.

## Running the editor

From the repository root:

```powershell
# Restore dependencies and launch the WinForms client
cd tool/map-editor-cs/MapEditor
dotnet run
```

The client automatically locates the repository's `maps` directory by walking up
from the executable folder. When prompted, type the
map ID (e.g., `1`) to open `maps/1.txt`. The palette on the left exposes every
tile currently in the map. Select a tile, choose Brush or Rectangle, and click
or drag in the canvas to paint. Use the passability checkbox (three-state) and
the zone dropdown to toggle metadata during painting. Switch overlays with the
toolbar combo box to inspect passability or safety/combat zones, and rely on the
status bar for the current cursor coordinate. Undo/redo shortcuts (Ctrl+Z /
Ctrl+Y) track every paint operation.

The minimap panel continuously updates to give a whole-map preview. Use the
`File > Save` action to update `maps/<mapId>.txt` in place or `File > Batch
Export...` to write every loaded map to a different folder.

## Publishing a standalone build

To ship a self-contained Windows binary with no external dependencies, run:

```powershell
cd tool/map-editor-cs/MapEditor
dotnet publish -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true
```

The published executable appears under
`bin/Release/net8.0-windows10.0.19041.0/win-x64/publish/MapEditor.exe`. Copy the `maps`
folder alongside the executable so the editor can locate the CSV files.
