package tool.mapeditor.model;

public class MapContext {
    private final EditableL1Map map;
    private final MapHistory history = new MapHistory();

    public MapContext(EditableL1Map map) {
        this.map = map;
    }

    public EditableL1Map getMap() {
        return map;
    }

    public MapHistory getHistory() {
        return history;
    }
}
