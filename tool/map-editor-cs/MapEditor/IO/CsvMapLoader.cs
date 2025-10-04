using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Linq;
using tool.mapeditor.model;

namespace tool.mapeditor.io;

public class CsvMapLoader
{
    private readonly string _mapsDirectory;

    public CsvMapLoader(string mapsDirectory)
    {
        _mapsDirectory = mapsDirectory;
    }

    public EditableL1Map Load(int mapId)
    {
        var path = Path.Combine(_mapsDirectory, $"{mapId}.txt");
        if (!File.Exists(path))
        {
            throw new FileNotFoundException($"Map file not found: {Path.GetFullPath(path)}");
        }

        var rows = new List<byte[]>();
        using var reader = new StreamReader(path);
        string? line;
        while ((line = reader.ReadLine()) != null)
        {
            line = line.Trim();
            if (line.Length == 0 || line.StartsWith("#", StringComparison.Ordinal))
            {
                continue;
            }

            var parts = line.Split(',');
            var row = new byte[parts.Length];
            for (var i = 0; i < parts.Length; i++)
            {
                if (!byte.TryParse(parts[i].Trim(), NumberStyles.Integer, CultureInfo.InvariantCulture, out row[i]))
                {
                    row[i] = (byte)int.Parse(parts[i].Trim(), CultureInfo.InvariantCulture);
                }
            }
            rows.Add(row);
        }

        if (rows.Count == 0)
        {
            throw new InvalidDataException($"Map file contains no tile data: {path}");
        }

        var width = rows[0].Length;
        if (rows.Any(r => r.Length != width))
        {
            throw new InvalidDataException($"Inconsistent row width in {path}");
        }

        var tiles = new byte[width, rows.Count];
        for (var y = 0; y < rows.Count; y++)
        {
            var row = rows[y];
            for (var x = 0; x < width; x++)
            {
                tiles[x, y] = row[x];
            }
        }

        return new EditableL1Map(mapId, tiles, 0, 0);
    }
}
