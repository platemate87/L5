using System;
using System.Collections.Generic;
using System.Drawing;
using System.IO;
using System.Windows.Forms;
using tool.mapeditor.io;
using tool.mapeditor.model;
using tool.mapeditor.ui;

namespace tool.mapeditor.forms;

public class MapEditorForm : Form
{
    private readonly CsvMapLoader _loader;
    private readonly Dictionary<int, EditableL1Map> _maps = new();
    private readonly MapHistory _history = new();

    private readonly MapCanvasControl _canvas = new();
    private readonly MinimapControl _minimap = new();
    private readonly PaletteControl _palette = new();
    private readonly StatusStrip _statusStrip = new();
    private readonly ToolStripStatusLabel _coordinateLabel = new();
    private readonly ToolStripStatusLabel _mapLabel = new();
    private readonly ToolStrip _toolStrip = new();

    private readonly RadioButton _brushRadio = new() { Text = "Brush", Checked = true, Dock = DockStyle.Top };
    private readonly RadioButton _rectangleRadio = new() { Text = "Rectangle", Dock = DockStyle.Top };
    private readonly CheckBox _passableCheck = new() { Text = "Passable (3-state)", ThreeState = true, Dock = DockStyle.Top };
    private readonly ComboBox _zoneCombo = new() { Dock = DockStyle.Top, DropDownStyle = ComboBoxStyle.DropDownList };
    private readonly ComboBox _overlayCombo = new() { DropDownStyle = ComboBoxStyle.DropDownList, Width = 140 };
    private readonly ToolStripButton _tileArtToggle = new() { Text = "Tile Art", DisplayStyle = ToolStripItemDisplayStyle.Text, CheckOnClick = true };

    private EditableL1Map? _currentMap;
    private readonly string _mapsDirectory = ResolveMapsDirectory();
    private readonly TileImageProvider _tileImages;
    private short _selectedTile;
    private bool _snapshotCaptured;

    public MapEditorForm()
    {
        Text = "L1J Map Editor (C#)";
        Width = 1200;
        Height = 800;

        _loader = new CsvMapLoader(_mapsDirectory);
        _tileImages = new TileImageProvider(ResolveTileImageDirectory());
        UpdateTileArtAvailability();

        InitializeLayout();
        WireEvents();
        _canvas.TileImages = _tileImages;
        _palette.TileImages = _tileImages;
        UpdateTileArtVisibility();

        FormClosed += (_, _) => _tileImages.Dispose();
    }

    private void InitializeLayout()
    {
        var menuStrip = BuildMenu();
        Controls.Add(menuStrip);
        MainMenuStrip = menuStrip;

        _toolStrip.GripStyle = ToolStripGripStyle.Hidden;
        _toolStrip.RenderMode = ToolStripRenderMode.System;
        var loadButton = new ToolStripButton("Load") { DisplayStyle = ToolStripItemDisplayStyle.Text };
        loadButton.Click += (_, _) => LoadMapFromPrompt();
        var saveButton = new ToolStripButton("Save") { DisplayStyle = ToolStripItemDisplayStyle.Text };
        saveButton.Click += (_, _) => SaveCurrentMap();
        var undoButton = new ToolStripButton("Undo") { DisplayStyle = ToolStripItemDisplayStyle.Text };
        undoButton.Click += (_, _) => Undo();
        var redoButton = new ToolStripButton("Redo") { DisplayStyle = ToolStripItemDisplayStyle.Text };
        redoButton.Click += (_, _) => Redo();
        _overlayCombo.Items.AddRange(new object[] { "Tiles", "Passability", "Zones" });
        _overlayCombo.SelectedIndex = 0;
        _overlayCombo.SelectedIndexChanged += (_, _) => UpdateOverlay();
        var overlayHost = new ToolStripControlHost(_overlayCombo) { Margin = new Padding(6, 1, 0, 2) };

        _toolStrip.Items.Add(loadButton);
        _toolStrip.Items.Add(saveButton);
        _toolStrip.Items.Add(new ToolStripSeparator());
        _toolStrip.Items.Add(undoButton);
        _toolStrip.Items.Add(redoButton);
        _toolStrip.Items.Add(new ToolStripSeparator());
        _toolStrip.Items.Add(new ToolStripLabel("Overlay:"));
        _toolStrip.Items.Add(overlayHost);
        _toolStrip.Items.Add(new ToolStripSeparator());
        _toolStrip.Items.Add(_tileArtToggle);
        _toolStrip.Dock = DockStyle.Top;
        Controls.Add(_toolStrip);

        var split = new SplitContainer
        {
            Dock = DockStyle.Fill,
            SplitterDistance = 260,
            Panel1MinSize = 220
        };

        var paletteGroup = new GroupBox { Text = "Palette", Dock = DockStyle.Fill };
        _palette.Dock = DockStyle.Fill;
        paletteGroup.Controls.Add(_palette);

        var toolGroup = new GroupBox { Text = "Tools", Dock = DockStyle.Top, Height = 100 };
        toolGroup.Controls.Add(_rectangleRadio);
        toolGroup.Controls.Add(_brushRadio);

        var flagsGroup = new GroupBox { Text = "Flags", Dock = DockStyle.Top, Height = 150 };
        _passableCheck.Text = "Passable (leave unset for no change)";
        flagsGroup.Controls.Add(_zoneCombo);
        flagsGroup.Controls.Add(_passableCheck);

        _zoneCombo.Items.AddRange(new object[]
        {
            "Leave zone",
            "Normal",
            "Safety",
            "Combat"
        });
        _zoneCombo.SelectedIndex = 0;

        var leftLayout = new TableLayoutPanel
        {
            Dock = DockStyle.Fill,
            RowCount = 3,
            ColumnCount = 1
        };
        leftLayout.RowStyles.Add(new RowStyle(SizeType.Percent, 50));
        leftLayout.RowStyles.Add(new RowStyle(SizeType.AutoSize));
        leftLayout.RowStyles.Add(new RowStyle(SizeType.AutoSize));
        leftLayout.Controls.Add(paletteGroup, 0, 0);
        leftLayout.Controls.Add(toolGroup, 0, 1);
        leftLayout.Controls.Add(flagsGroup, 0, 2);

        split.Panel1.Controls.Add(leftLayout);

        var mapPanel = new Panel { Dock = DockStyle.Fill, AutoScroll = true, BackColor = Color.Black };
        _canvas.Dock = DockStyle.None;
        mapPanel.Controls.Add(_canvas);

        var minimapGroup = new GroupBox { Text = "Minimap", Dock = DockStyle.Bottom, Height = 200 };
        _minimap.Dock = DockStyle.Fill;
        minimapGroup.Controls.Add(_minimap);

        var rightLayout = new TableLayoutPanel { Dock = DockStyle.Fill, RowCount = 2 };
        rightLayout.RowStyles.Add(new RowStyle(SizeType.Percent, 100));
        rightLayout.RowStyles.Add(new RowStyle(SizeType.Absolute, 200));
        rightLayout.Controls.Add(mapPanel, 0, 0);
        rightLayout.Controls.Add(minimapGroup, 0, 1);

        split.Panel2.Controls.Add(rightLayout);

        Controls.Add(split);

        _statusStrip.Items.Add(_mapLabel);
        _statusStrip.Items.Add(new ToolStripStatusLabel { Spring = true });
        _statusStrip.Items.Add(_coordinateLabel);
        _statusStrip.Dock = DockStyle.Bottom;
        Controls.Add(_statusStrip);
    }

    private MenuStrip BuildMenu()
    {
        var menuStrip = new MenuStrip();
        var fileMenu = new ToolStripMenuItem("File");
        var loadItem = new ToolStripMenuItem("Load Map...", null, (_, _) => LoadMapFromPrompt()) { ShortcutKeys = Keys.Control | Keys.L };
        var saveItem = new ToolStripMenuItem("Save", null, (_, _) => SaveCurrentMap()) { ShortcutKeys = Keys.Control | Keys.S };
        var exportItem = new ToolStripMenuItem("Batch Export...", null, (_, _) => ExportAllMaps());
        var exitItem = new ToolStripMenuItem("Exit", null, (_, _) => Close());
        fileMenu.DropDownItems.Add(loadItem);
        fileMenu.DropDownItems.Add(saveItem);
        fileMenu.DropDownItems.Add(new ToolStripSeparator());
        fileMenu.DropDownItems.Add(exportItem);
        fileMenu.DropDownItems.Add(new ToolStripSeparator());
        fileMenu.DropDownItems.Add(exitItem);

        var editMenu = new ToolStripMenuItem("Edit");
        var undoItem = new ToolStripMenuItem("Undo", null, (_, _) => Undo()) { ShortcutKeys = Keys.Control | Keys.Z };
        var redoItem = new ToolStripMenuItem("Redo", null, (_, _) => Redo()) { ShortcutKeys = Keys.Control | Keys.Y };
        editMenu.DropDownItems.Add(undoItem);
        editMenu.DropDownItems.Add(redoItem);

        menuStrip.Items.Add(fileMenu);
        menuStrip.Items.Add(editMenu);
        return menuStrip;
    }

    private void WireEvents()
    {
        _palette.TileSelected += (_, tile) => _selectedTile = tile;
        _canvas.TileHovered += (_, point) => UpdateStatus(point);
        _canvas.TilePaintRequested += (_, args) => ApplyBrush(args);
        _canvas.SelectionFinished += (_, rect) => ApplyRectangle(rect);
        _canvas.MouseUp += (_, _) => _snapshotCaptured = false;
        _brushRadio.CheckedChanged += (_, _) => UpdateToolMode();
        _rectangleRadio.CheckedChanged += (_, _) => UpdateToolMode();
        _tileArtToggle.CheckedChanged += (_, _) => UpdateTileArtVisibility();
        KeyPreview = true;
        KeyDown += OnKeyDown;
    }

    private void UpdateToolMode()
    {
        _canvas.Tool = _rectangleRadio.Checked ? ToolMode.Rectangle : ToolMode.Brush;
    }

    private void UpdateOverlay()
    {
        _canvas.Overlay = _overlayCombo.SelectedIndex switch
        {
            1 => OverlayMode.Passability,
            2 => OverlayMode.Zones,
            _ => OverlayMode.None
        };
        _canvas.Invalidate();
    }

    private void UpdateStatus(Point point)
    {
        if (_currentMap == null || point.X < 0 || point.Y < 0)
        {
            _coordinateLabel.Text = string.Empty;
            return;
        }

        var tile = _currentMap.GetOriginalTile(point.X, point.Y);
        var passable = _currentMap.IsPassable(point.X, point.Y) ? "Passable" : "Blocked";
        var zone = _currentMap.GetZone(point.X, point.Y);
        _coordinateLabel.Text = $"({point.X}, {point.Y}) Tile {tile} {passable} Zone: {zone}";
    }

    private void LoadMapFromPrompt()
    {
        var id = PromptForMapId();
        if (!id.HasValue)
        {
            return;
        }

        try
        {
            LoadMap(id.Value);
        }
        catch (Exception ex)
        {
            MessageBox.Show(this, ex.Message, "Failed to load map", MessageBoxButtons.OK, MessageBoxIcon.Error);
        }
    }

    private void LoadMap(int mapId)
    {
        var map = _loader.Load(mapId);
        _maps[mapId] = map;
        SetCurrentMap(map);
        _history.Clear();
        _snapshotCaptured = false;
    }

    private void SetCurrentMap(EditableL1Map map)
    {
        _currentMap = map;
        _canvas.Map = map;
        _canvas.RefreshSize();
        _canvas.Invalidate();
        _minimap.Map = map;
        _minimap.Invalidate();
        _palette.SetPalette(AllTiles(map));
        _mapLabel.Text = $"Map {map.MapId} ({map.Width}x{map.Height})";
        _selectedTile = _palette.SelectedTile;
        _snapshotCaptured = false;
        UpdateTileArtVisibility();

    }

    private IEnumerable<short> AllTiles(EditableL1Map map)
    {
        for (var y = 0; y < map.Height; y++)
        {
            for (var x = 0; x < map.Width; x++)
            {
                yield return map.GetOriginalTile(x, y);
            }
        }
    }

    private void ApplyBrush(TileDrawEventArgs args)
    {
        if (_currentMap == null)
        {
            return;
        }

        if (!_snapshotCaptured)
        {
            _history.PushSnapshot(_currentMap);
            _snapshotCaptured = true;
        }
        foreach (var tile in args.Tiles)
        {
            if (!_currentMap.InBounds(tile.X, tile.Y))
            {
                continue;
            }

            _currentMap.SetOriginalTile(tile.X, tile.Y, _selectedTile);
            ApplyFlagUpdates(tile.X, tile.Y);
        }
        RefreshMapViews();
    }

    private void ApplyRectangle(Rectangle rect)
    {
        if (_currentMap == null)
        {
            return;
        }

        var start = new Point(rect.Left / MapCanvasControl.TileSize, rect.Top / MapCanvasControl.TileSize);
        var end = new Point((rect.Right - 1) / MapCanvasControl.TileSize, (rect.Bottom - 1) / MapCanvasControl.TileSize);
        ApplyBrush(new TileDrawEventArgs(start, end));
        _snapshotCaptured = false;
    }

    private void ApplyFlagUpdates(int x, int y)
    {
        if (_currentMap == null)
        {
            return;
        }

        if (_passableCheck.CheckState != CheckState.Indeterminate)
        {
            _currentMap.SetPassable(x, y, _passableCheck.CheckState == CheckState.Checked);
        }

        switch (_zoneCombo.SelectedIndex)
        {
            case 1:
                _currentMap.SetZone(x, y, ZoneType.None);
                break;
            case 2:
                _currentMap.SetZone(x, y, ZoneType.Safety);
                break;
            case 3:
                _currentMap.SetZone(x, y, ZoneType.Combat);
                break;
        }
    }

    private void RefreshMapViews()
    {
        if (_currentMap == null)
        {
            return;
        }

        _canvas.Invalidate();
        _minimap.Invalidate();
        if (_palette.ShowTileImages)
        {
            _palette.Invalidate();
        }
    }

    private void SaveCurrentMap()
    {
        if (_currentMap == null)
        {
            return;
        }

        var path = Path.Combine(_mapsDirectory, $"{_currentMap.MapId}.txt");
        var directory = Path.GetDirectoryName(path);
        if (!string.IsNullOrEmpty(directory))
        {
            Directory.CreateDirectory(directory);
        }
        try
        {
            CsvMapWriter.Save(_currentMap, path);
            MessageBox.Show(this, $"Saved to {path}", "Map Saved", MessageBoxButtons.OK, MessageBoxIcon.Information);
        }
        catch (Exception ex)
        {
            MessageBox.Show(this, ex.Message, "Save failed", MessageBoxButtons.OK, MessageBoxIcon.Error);
        }
    }

    private void ExportAllMaps()
    {
        if (_maps.Count == 0)
        {
            MessageBox.Show(this, "Load at least one map before exporting.", "Nothing to export", MessageBoxButtons.OK, MessageBoxIcon.Information);
            return;
        }

        using var dialog = new FolderBrowserDialog
        {
            Description = "Select export destination",
            UseDescriptionForTitle = true
        };
        if (dialog.ShowDialog(this) == DialogResult.OK)
        {
            try
            {
                CsvMapWriter.ExportAll(_maps.Values, dialog.SelectedPath);
                MessageBox.Show(this, $"Exported {_maps.Count} maps to {dialog.SelectedPath}", "Export complete", MessageBoxButtons.OK, MessageBoxIcon.Information);
            }
            catch (Exception ex)
            {
                MessageBox.Show(this, ex.Message, "Export failed", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
        }
    }

    private int? PromptForMapId()
    {
        using var prompt = new Form
        {
            Text = "Load Map",
            FormBorderStyle = FormBorderStyle.FixedDialog,
            StartPosition = FormStartPosition.CenterParent,
            ClientSize = new Size(280, 120),
            MinimizeBox = false,
            MaximizeBox = false
        };

        var label = new Label { Text = "Map ID:", Left = 12, Top = 18, AutoSize = true };
        var textBox = new TextBox { Left = 80, Top = 15, Width = 180 };
        var ok = new Button { Text = "OK", DialogResult = DialogResult.OK, Left = 80, Width = 80, Top = 60 };
        var cancel = new Button { Text = "Cancel", DialogResult = DialogResult.Cancel, Left = 180, Width = 80, Top = 60 };
        prompt.Controls.AddRange(new Control[] { label, textBox, ok, cancel });
        prompt.AcceptButton = ok;
        prompt.CancelButton = cancel;

        if (prompt.ShowDialog(this) == DialogResult.OK)
        {
            if (int.TryParse(textBox.Text.Trim(), out var mapId))
            {
                return mapId;
            }
            MessageBox.Show(this, "Enter a numeric map ID.", "Invalid ID", MessageBoxButtons.OK, MessageBoxIcon.Warning);
        }
        return null;
    }

    private void Undo()
    {
        if (_currentMap == null)
        {
            return;
        }

        var map = _history.Undo(_currentMap);
        if (map != null)
        {
            _maps[map.MapId] = map;
            SetCurrentMap(map);
            _snapshotCaptured = false;
        }
    }

    private void Redo()
    {
        if (_currentMap == null)
        {
            return;
        }

        var map = _history.Redo(_currentMap);
        if (map != null)
        {
            _maps[map.MapId] = map;
            SetCurrentMap(map);
            _snapshotCaptured = false;
        }
    }

    private void OnKeyDown(object? sender, KeyEventArgs e)
    {
        if (e.Control && e.KeyCode == Keys.Z)
        {
            Undo();
            e.Handled = true;
        }
        else if (e.Control && e.KeyCode == Keys.Y)
        {
            Redo();
            e.Handled = true;
        }
    }

    private void UpdateTileArtVisibility()
    {
        var show = _tileArtToggle.Checked && _tileImages.HasAnyTiles;
        _canvas.ShowTileImages = show;
        _palette.ShowTileImages = show;
        _canvas.Invalidate();
        _palette.Invalidate();
    }

    private void UpdateTileArtAvailability()
    {
        if (_tileImages.HasAnyTiles)
        {
            _tileArtToggle.Enabled = true;
            _tileArtToggle.ToolTipText = "Toggle tile art rendering (requires extracted tile images)";
        }
        else
        {
            _tileArtToggle.Enabled = false;
            _tileArtToggle.Checked = false;
            _tileArtToggle.ToolTipText = $"Place tile art images in {_tileImages.BaseDirectory} to enable";
        }
    }

    private static string ResolveMapsDirectory()
    {
        var current = AppContext.BaseDirectory;
        for (var i = 0; i < 6; i++)
        {
            var candidate = Path.Combine(current, "maps");
            if (Directory.Exists(candidate))
            {
                return candidate;
            }

            var parent = Path.GetFullPath(Path.Combine(current, ".."));
            if (parent == current)
            {
                break;
            }
            current = parent;
        }

        return Path.Combine(AppContext.BaseDirectory, "maps");
    }

    private static string ResolveTileImageDirectory()
    {
        var current = AppContext.BaseDirectory;
        for (var i = 0; i < 6; i++)
        {
            var candidate = Path.Combine(current, "tiles");
            if (Directory.Exists(candidate))
            {
                return candidate;
            }

            var parent = Path.GetFullPath(Path.Combine(current, ".."));
            if (parent == current)
            {
                break;
            }
            current = parent;
        }

        return Path.Combine(AppContext.BaseDirectory, "tiles");
    }
}
