using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Windows.Forms;

namespace tool.mapeditor.ui;

public class PaletteControl : ListBox
{
    private readonly List<short> _tiles = new();

    public event EventHandler<short>? TileSelected;

    public PaletteControl()
    {
        DrawMode = DrawMode.OwnerDrawFixed;
        ItemHeight = 24;
        SelectedIndexChanged += (_, _) =>
        {
            if (SelectedIndex >= 0 && SelectedIndex < _tiles.Count)
            {
                TileSelected?.Invoke(this, _tiles[SelectedIndex]);
            }
        };
    }

    public void SetPalette(IEnumerable<short> tiles)
    {
        _tiles.Clear();
        _tiles.AddRange(tiles.Distinct().OrderBy(t => t));
        Items.Clear();
        foreach (var tile in _tiles)
        {
            Items.Add($"Tile {tile}");
        }
        if (_tiles.Count > 0)
        {
            SelectedIndex = 0;
        }
    }

    public short SelectedTile => SelectedIndex >= 0 && SelectedIndex < _tiles.Count ? _tiles[SelectedIndex] : (short)0;

    protected override void OnDrawItem(DrawItemEventArgs e)
    {
        e.DrawBackground();
        if (e.Index < 0 || e.Index >= _tiles.Count)
        {
            return;
        }

        var tile = _tiles[e.Index];
        var color = MapCanvasControl.TileColor(tile);
        using var brush = new SolidBrush(color);
        var colorRect = new Rectangle(e.Bounds.Left + 4, e.Bounds.Top + 4, 16, 16);
        e.Graphics.FillRectangle(brush, colorRect);
        e.Graphics.DrawRectangle(Pens.Black, colorRect);
        using var textBrush = new SolidBrush(e.ForeColor);
        e.Graphics.DrawString($"{tile}", e.Font!, textBrush, e.Bounds.Left + 28, e.Bounds.Top + 4);
        e.DrawFocusRectangle();
    }
}
