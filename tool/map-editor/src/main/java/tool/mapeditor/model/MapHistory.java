package tool.mapeditor.model;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Simple undo/redo stack for {@link EditableL1Map} instances.
 */
public class MapHistory {
    private final Deque<byte[][]> undoStack = new ArrayDeque<>();
    private final Deque<byte[][]> redoStack = new ArrayDeque<>();

    public void snapshot(EditableL1Map map) {
        undoStack.push(map.copyTiles());
        redoStack.clear();
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void undo(EditableL1Map map) {
        if (!canUndo()) {
            return;
        }
        redoStack.push(map.copyTiles());
        byte[][] previous = undoStack.pop();
        map.overwriteTiles(previous);
    }

    public void redo(EditableL1Map map) {
        if (!canRedo()) {
            return;
        }
        undoStack.push(map.copyTiles());
        byte[][] next = redoStack.pop();
        map.overwriteTiles(next);
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
