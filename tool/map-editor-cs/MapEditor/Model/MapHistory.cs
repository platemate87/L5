using System.Collections.Generic;

namespace tool.mapeditor.model;

public class MapHistory
{
    private readonly Stack<EditableL1Map> _undo = new();
    private readonly Stack<EditableL1Map> _redo = new();

    public void PushSnapshot(EditableL1Map map)
    {
        _undo.Push(map.DeepCopy());
        _redo.Clear();
    }

    public EditableL1Map? Undo(EditableL1Map current)
    {
        if (_undo.Count == 0)
        {
            return null;
        }

        _redo.Push(current.DeepCopy());
        return _undo.Pop();
    }

    public EditableL1Map? Redo(EditableL1Map current)
    {
        if (_redo.Count == 0)
        {
            return null;
        }

        _undo.Push(current.DeepCopy());
        return _redo.Pop();
    }

    public void Clear()
    {
        _undo.Clear();
        _redo.Clear();
    }
}
