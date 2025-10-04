using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Windows.Forms;
using tool.mapeditor.model;

namespace tool.mapeditor.ui;

public class MapCanvasControl : Control
{
    public const int TileSize = 24;

    private readonly Pen _gridPen = new(Color.FromArgb(40, Color.Black));
    private readonly Brush _selectionBrush = new SolidBrush(Color.FromArgb(80, Color.CornflowerBlue));

    private Point? _dragStart;
    private Rectangle? _selection;
    private Point? _lastPainted;

    public MapCanvasControl()
    {
        DoubleBuffered = true;
        SetStyle(ControlStyles.ResizeRedraw, true);
    }

    public EditableL1Map? Map { get; set; }

    public OverlayMode Overlay { get; set; } = OverlayMode.None;

    public ToolMode Tool { get; set; } = ToolMode.Brush;

    public bool ShowTileImages { get; set; }

    public TileImageProvider? TileImages { get; set; }

    public event EventHandler<Point>? TileHovered;
    public event EventHandler<TileDrawEventArgs>? TilePaintRequested;
    public event EventHandler<Rectangle>? SelectionFinished;

    public void ClearSelection()
    {
        _selection = null;
        Invalidate();
    }

    protected override void OnPaint(PaintEventArgs e)
    {
        base.OnPaint(e);
        e.Graphics.Clear(Color.Black);

        if (Map == null)
        {
            return;
        }

        if (ShowTileImages && TileImages != null)
        {
            e.Graphics.InterpolationMode = InterpolationMode.NearestNeighbor;
            e.Graphics.PixelOffsetMode = PixelOffsetMode.Half;
        }

        for (var y = 0; y < Map.Height; y++)
        {
            for (var x = 0; x < Map.Width; x++)
            {
                var rect = new Rectangle(x * TileSize, y * TileSize, TileSize, TileSize);
                var tileId = Map.GetOriginalTile(x, y);
                var tileImage = ShowTileImages ? TileImages?.GetTile(tileId) : null;
                if (tileImage != null)
                {
                    e.Graphics.DrawImage(tileImage, rect);
                }
                else
                {
                    using var brush = new SolidBrush(TileColor(tileId));
                    e.Graphics.FillRectangle(brush, rect);
                }

                if (Overlay == OverlayMode.Passability)
                {
                    if (!Map.IsPassable(x, y))
                    {
                        using var overlay = new SolidBrush(Color.FromArgb(140, Color.Crimson));
                        e.Graphics.FillRectangle(overlay, rect);
                    }
                }
                else if (Overlay == OverlayMode.Zones)
                {
                    var zone = Map.GetZone(x, y);
                    if (zone == ZoneType.Safety)
                    {
                        using var overlay = new SolidBrush(Color.FromArgb(120, Color.MediumSeaGreen));
                        e.Graphics.FillRectangle(overlay, rect);
                    }
                    else if (zone == ZoneType.Combat)
                    {
                        using var overlay = new SolidBrush(Color.FromArgb(120, Color.SaddleBrown));
                        e.Graphics.FillRectangle(overlay, rect);
                    }
                }

                e.Graphics.DrawRectangle(_gridPen, rect);
            }
        }

        if (_selection.HasValue)
        {
            e.Graphics.FillRectangle(_selectionBrush, _selection.Value);
        }
    }

    protected override void OnMouseMove(MouseEventArgs e)
    {
        base.OnMouseMove(e);
        var tile = ToTile(e.Location);
        if (tile.HasValue)
        {
            TileHovered?.Invoke(this, tile.Value);
            if (Tool == ToolMode.Brush && e.Button.HasFlag(MouseButtons.Left))
            {
                if (!_lastPainted.HasValue || !_lastPainted.Value.Equals(tile.Value))
                {
                    _lastPainted = tile.Value;
                    TilePaintRequested?.Invoke(this, new TileDrawEventArgs(tile.Value, tile.Value));
                }
            }
        }

        if (_dragStart.HasValue && Map != null)
        {
            var start = _dragStart.Value;
            var end = tile ?? start;
            var rect = NormalizeSelection(start, end);
            _selection = rect;
            Invalidate();
        }
    }

    protected override void OnMouseLeave(EventArgs e)
    {
        base.OnMouseLeave(e);
        TileHovered?.Invoke(this, new Point(-1, -1));
        _lastPainted = null;
    }

    protected override void OnMouseDown(MouseEventArgs e)
    {
        base.OnMouseDown(e);
        if (Map == null)
        {
            return;
        }

        var tile = ToTile(e.Location);
        if (!tile.HasValue)
        {
            return;
        }

        if (e.Button == MouseButtons.Left)
        {
            if (Tool == ToolMode.Rectangle)
            {
                _dragStart = tile.Value;
                _selection = new Rectangle(tile.Value.X * TileSize, tile.Value.Y * TileSize, TileSize, TileSize);
            }
            else
            {
                _dragStart = null;
                _lastPainted = tile.Value;
                TilePaintRequested?.Invoke(this, new TileDrawEventArgs(tile.Value, tile.Value));
            }
        }
    }

    protected override void OnMouseUp(MouseEventArgs e)
    {
        base.OnMouseUp(e);
        if (Map == null)
        {
            return;
        }

        var tile = ToTile(e.Location);
        if (Tool == ToolMode.Rectangle && _dragStart.HasValue && tile.HasValue)
        {
            var rect = NormalizeSelection(_dragStart.Value, tile.Value);
            _selection = null;
            _dragStart = null;
            Invalidate();
            SelectionFinished?.Invoke(this, rect);
        }

        if (e.Button == MouseButtons.Left)
        {
            _lastPainted = null;
        }
    }

    public void RefreshSize()
    {
        if (Map == null)
        {
            Size = new Size(Width, Height);
            return;
        }

        Size = new Size(Map.Width * TileSize, Map.Height * TileSize);
        Location = new Point(0, 0);
        Invalidate();
    }

    private static Rectangle NormalizeSelection(Point start, Point end)
    {
        var x = Math.Min(start.X, end.X);
        var y = Math.Min(start.Y, end.Y);
        var width = Math.Abs(start.X - end.X) + 1;
        var height = Math.Abs(start.Y - end.Y) + 1;
        return new Rectangle(x * TileSize, y * TileSize, width * TileSize, height * TileSize);
    }

    private Point? ToTile(Point location)
    {
        if (Map == null)
        {
            return null;
        }

        var x = location.X / TileSize;
        var y = location.Y / TileSize;
        if (!Map.InBounds(x, y))
        {
            return null;
        }
        return new Point(x, y);
    }

    internal static Color TileColor(short tile)
    {
        unchecked
        {
            var hash = (int)(tile * 2654435761u);
            var r = (byte)(hash & 0x7F);
            var g = (byte)((hash >> 7) & 0x7F);
            var b = (byte)((hash >> 14) & 0x7F);
            return Color.FromArgb(255, 100 + r, 100 + g, 100 + b);
        }
    }

    protected override void Dispose(bool disposing)
    {
        if (disposing)
        {
            _gridPen.Dispose();
            _selectionBrush.Dispose();
        }
        base.Dispose(disposing);
    }
}

public enum OverlayMode
{
    None,
    Passability,
    Zones
}

public enum ToolMode
{
    Brush,
    Rectangle
}

public class TileDrawEventArgs : EventArgs
{
    public TileDrawEventArgs(Point start, Point end)
    {
        Start = start;
        End = end;
    }

    public Point Start { get; }

    public Point End { get; }

    public IEnumerable<Point> Tiles
    {
        get
        {
            var minX = Math.Min(Start.X, End.X);
            var maxX = Math.Max(Start.X, End.X);
            var minY = Math.Min(Start.Y, End.Y);
            var maxY = Math.Max(Start.Y, End.Y);
            for (var y = minY; y <= maxY; y++)
            {
                for (var x = minX; x <= maxX; x++)
                {
                    yield return new Point(x, y);
                }
            }
        }
    }
}
