package tool.mapeditor;

import javax.swing.SwingUtilities;

public class MapEditorApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MapEditorFrame frame = new MapEditorFrame();
            frame.setVisible(true);
        });
    }
}
