using System.Collections.Generic;
using System.IO;
using System.Text;
using tool.mapeditor.model;

namespace tool.mapeditor.io;

public static class CsvMapWriter
{
    public static void Save(EditableL1Map map, string path)
    {
        var directory = Path.GetDirectoryName(path);
        if (!string.IsNullOrEmpty(directory))
        {
            Directory.CreateDirectory(directory);
        }

        var bytes = Encoding.UTF8.GetBytes(map.ToCsv());
        File.WriteAllBytes(path, bytes);
    }

    public static void ExportAll(IEnumerable<EditableL1Map> maps, string directory)
    {
        Directory.CreateDirectory(directory);
        foreach (var map in maps)
        {
            var path = Path.Combine(directory, $"{map.MapId}.txt");
            Save(map, path);
        }
    }
}
