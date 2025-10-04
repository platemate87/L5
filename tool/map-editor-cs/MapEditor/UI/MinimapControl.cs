using System.Drawing;
using System.Windows.Forms;
using tool.mapeditor.model;

namespace tool.mapeditor.ui;

public class MinimapControl : Control
{
    public EditableL1Map? Map { get; set; }

    protected override void OnPaint(PaintEventArgs e)
    {
        base.OnPaint(e);
        e.Graphics.Clear(Color.Black);

        if (Map == null)
        {
            return;
        }

        var width = Map.Width;
        var height = Map.Height;
        if (width == 0 || height == 0)
        {
            return;
        }

        var scaleX = (float)ClientSize.Width / width;
        var scaleY = (float)ClientSize.Height / height;
        var scale = Math.Min(scaleX, scaleY);

        for (var y = 0; y < height; y++)
        {
            for (var x = 0; x < width; x++)
            {
                var color = MapCanvasControl.TileColor(Map.GetOriginalTile(x, y));
                using var brush = new SolidBrush(color);
                e.Graphics.FillRectangle(
                    brush,
                    x * scale,
                    y * scale,
                    scale + 1,
                    scale + 1);
            }
        }
    }
}
