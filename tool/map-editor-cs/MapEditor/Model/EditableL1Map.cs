using System;
using System.Text;

namespace tool.mapeditor.model;

public class EditableL1Map
{
    private const byte BitImpassable = 0x80;
    private const byte MaskZone = 0x30;

    private readonly byte[,] _tiles;

    public EditableL1Map(int mapId, byte[,] tiles, int startX, int startY, MapAttributes? attributes = null)
    {
        MapId = mapId;
        _tiles = tiles ?? throw new ArgumentNullException(nameof(tiles));
        StartX = startX;
        StartY = startY;
        Attributes = attributes?.Clone() ?? new MapAttributes();
    }

    public int MapId { get; }

    public int StartX { get; }

    public int StartY { get; }

    public MapAttributes Attributes { get; }

    public int Width => _tiles.GetLength(0);

    public int Height => _tiles.GetLength(1);

    public EditableL1Map DeepCopy()
    {
        var copy = new byte[Width, Height];
        Array.Copy(_tiles, copy, _tiles.Length);
        return new EditableL1Map(MapId, copy, StartX, StartY, Attributes.Clone());
    }

    public byte GetRawTile(int x, int y)
    {
        return InBounds(x, y) ? _tiles[x, y] : (byte)0;
    }

    public short GetOriginalTile(int x, int y)
    {
        return (short)(GetRawTile(x, y) & ~BitImpassable);
    }

    public void SetOriginalTile(int x, int y, short tile)
    {
        if (!InBounds(x, y))
        {
            return;
        }

        var raw = _tiles[x, y];
        var passable = (raw & BitImpassable) != 0;
        byte clamped = (byte)(tile & 0xFF);
        if (passable)
        {
            clamped |= BitImpassable;
        }
        else
        {
            clamped &= unchecked((byte)~BitImpassable);
        }
        _tiles[x, y] = clamped;
    }

    public bool IsPassable(int x, int y)
    {
        return (GetRawTile(x, y) & BitImpassable) != 0;
    }

    public void SetPassable(int x, int y, bool passable)
    {
        if (!InBounds(x, y))
        {
            return;
        }

        if (passable)
        {
            _tiles[x, y] = (byte)(GetOriginalTile(x, y) | BitImpassable);
        }
        else
        {
            _tiles[x, y] = (byte)(GetOriginalTile(x, y) & ~BitImpassable);
        }
    }

    public ZoneType GetZone(int x, int y)
    {
        var raw = GetRawTile(x, y);
        return (raw & MaskZone) switch
        {
            0x10 => ZoneType.Safety,
            0x20 => ZoneType.Combat,
            _ => ZoneType.None
        };
    }

    public void SetZone(int x, int y, ZoneType zone)
    {
        if (!InBounds(x, y))
        {
            return;
        }

        var raw = GetRawTile(x, y);
        raw = (byte)(raw & ~MaskZone);
        raw = zone switch
        {
            ZoneType.Safety => (byte)(raw | 0x10),
            ZoneType.Combat => (byte)(raw | 0x20),
            _ => raw
        };
        _tiles[x, y] = raw;
    }

    public bool InBounds(int x, int y)
    {
        return x >= 0 && x < Width && y >= 0 && y < Height;
    }

    public string ToCsv()
    {
        var builder = new StringBuilder();
        for (var y = 0; y < Height; y++)
        {
            for (var x = 0; x < Width; x++)
            {
                builder.Append(_tiles[x, y]);
                if (x < Width - 1)
                {
                    builder.Append(',');
                }
            }
            builder.AppendLine();
        }
        return builder.ToString();
    }
}
