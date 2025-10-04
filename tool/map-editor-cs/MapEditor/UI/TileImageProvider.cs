using System;
using System.Collections.Generic;
using System.Drawing;
using System.IO;
using System.Linq;

namespace tool.mapeditor.ui;

public sealed class TileImageProvider : IDisposable
{
    private static readonly HashSet<string> SupportedExtensions = new(StringComparer.OrdinalIgnoreCase)
    {
        ".png",
        ".bmp",
        ".jpg",
        ".jpeg"
    };

    private readonly Dictionary<short, string> _tilePaths = new();
    private readonly Dictionary<short, Image?> _cache = new();
    private bool _disposed;

    public TileImageProvider(string directory)
    {
        BaseDirectory = directory;
        if (!System.IO.Directory.Exists(directory))
        {
            return;
        }

        foreach (var file in System.IO.Directory.EnumerateFiles(directory, "*.*", SearchOption.AllDirectories))
        {
            if (!SupportedExtensions.Contains(Path.GetExtension(file)))
            {
                continue;
            }

            var name = Path.GetFileNameWithoutExtension(file);
            if (TryParseTileId(name, out var tile) && tile >= 0 && tile <= short.MaxValue)
            {
                _tilePaths[(short)tile] = file;
            }
        }
    }

    public string BaseDirectory { get; }

    public bool HasAnyTiles => _tilePaths.Count > 0;

    public Image? GetTile(short tile)
    {
        if (_disposed)
        {
            throw new ObjectDisposedException(nameof(TileImageProvider));
        }

        if (_cache.TryGetValue(tile, out var cached))
        {
            return cached;
        }

        if (!_tilePaths.TryGetValue(tile, out var path))
        {
            _cache[tile] = null;
            return null;
        }

        try
        {
            using var stream = File.OpenRead(path);
            var image = Image.FromStream(stream);
            _cache[tile] = image;
            return image;
        }
        catch
        {
            _cache[tile] = null;
            return null;
        }
    }

    private static bool TryParseTileId(string name, out int tile)
    {
        var digits = new string(name.Where(char.IsDigit).ToArray());
        if (digits.Length == 0)
        {
            tile = 0;
            return false;
        }

        return int.TryParse(digits, out tile);
    }

    public void Dispose()
    {
        if (_disposed)
        {
            return;
        }

        foreach (var image in _cache.Values)
        {
            image?.Dispose();
        }

        _cache.Clear();
        _tilePaths.Clear();
        _disposed = true;
    }
}
