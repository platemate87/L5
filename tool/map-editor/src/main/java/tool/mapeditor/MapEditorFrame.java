package tool.mapeditor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

import tool.mapeditor.io.CsvMapLoader;
import tool.mapeditor.io.CsvMapWriter;
import tool.mapeditor.model.EditableL1Map;
import tool.mapeditor.model.MapContext;
import tool.mapeditor.model.ZoneType;
import tool.mapeditor.ui.MapCanvas;
import tool.mapeditor.ui.MapCanvas.PaintMode;
import tool.mapeditor.ui.PalettePanel;
import tool.mapeditor.ui.MinimapPanel;

public class MapEditorFrame extends JFrame {
    private final CsvMapLoader loader = CsvMapLoader.forProjectRoot();
    private final CsvMapWriter writer = CsvMapWriter.forProjectRoot();

    private final Map<Integer, MapContext> contexts = new LinkedHashMap<>();
    private final DefaultListModel<Integer> mapListModel = new DefaultListModel<>();
    private final JList<Integer> mapList = new JList<>(mapListModel);

    private final MapCanvas mapCanvas = new MapCanvas();
    private final PalettePanel palettePanel = new PalettePanel();
    private final MinimapPanel minimapPanel = new MinimapPanel();

    private final JLabel coordinateLabel = new JLabel("Load a map to begin editing.");
    private final JLabel statusLabel = new JLabel("Ready");

    private final JButton undoButton = new JButton("Undo");
    private final JButton redoButton = new JButton("Redo");

    private MapContext activeContext;
    private boolean paintingInProgress;

    public MapEditorFrame() {
        super("L1J Map Editor");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 800));

        setLayout(new BorderLayout());
        add(buildLeftPanel(), BorderLayout.WEST);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildRightPanel(), BorderLayout.EAST);
        add(buildStatusBar(), BorderLayout.SOUTH);

        mapCanvas.setPaintListener(request -> {
            if (activeContext == null) {
                return;
            }
            if (!paintingInProgress) {
                activeContext.getHistory().snapshot(activeContext.getMap());
                paintingInProgress = true;
            }
            applyPaint(request.startX, request.startY, request.endX, request.endY);
        });
        mapCanvas.setHoverListener(point -> updateCoordinateLabel(point.x, point.y));
        mapCanvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                paintingInProgress = false;
                updateUndoRedoButtons();
            }
        });

        palettePanel.getBrushToggle().addActionListener(e -> mapCanvas.setPaintMode(PaintMode.BRUSH));
        palettePanel.getRectangleToggle().addActionListener(e -> mapCanvas.setPaintMode(PaintMode.RECTANGLE));
        palettePanel.getPassabilityOverlayCheck().addActionListener(
                e -> mapCanvas.setShowPassabilityOverlay(palettePanel.getPassabilityOverlayCheck().isSelected()));
        palettePanel.getZoneOverlayCheck().addActionListener(
                e -> mapCanvas.setShowZoneOverlay(palettePanel.getZoneOverlayCheck().isSelected()));

        mapList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mapList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Integer mapId = mapList.getSelectedValue();
                if (mapId != null) {
                    activateMap(mapId);
                }
            }
        });

        undoButton.addActionListener(this::undoAction);
        redoButton.addActionListener(this::redoAction);

        pack();
        setLocationRelativeTo(null);
        updateUndoRedoButtons();
    }

    private JPanel buildLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(320, 800));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel buttonRow = new JPanel();
        JButton loadButton = new JButton("Load Map");
        loadButton.addActionListener(this::loadMap);
        JButton saveButton = new JButton("Save Map");
        saveButton.addActionListener(e -> saveActiveMap());
        JButton exportButton = new JButton("Export All");
        exportButton.addActionListener(e -> exportAll());
        buttonRow.add(loadButton);
        buttonRow.add(saveButton);
        buttonRow.add(exportButton);
        buttonRow.add(undoButton);
        buttonRow.add(redoButton);

        JScrollPane listScroll = new JScrollPane(mapList);
        listScroll.setPreferredSize(new Dimension(300, 300));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, listScroll, palettePanel);
        splitPane.setResizeWeight(0.4);
        splitPane.setBorder(null);

        panel.add(buttonRow, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(mapCanvas);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(220, 800));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));
        panel.add(new JLabel("Minimap"), BorderLayout.NORTH);
        panel.add(minimapPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(4, 8, 4, 8));
        panel.add(coordinateLabel, BorderLayout.WEST);
        panel.add(statusLabel, BorderLayout.EAST);
        return panel;
    }

    private void loadMap(ActionEvent event) {
        String input = JOptionPane.showInputDialog(this, "Enter map ID to load", "Load Map",
                JOptionPane.QUESTION_MESSAGE);
        if (input == null) {
            return;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        try {
            int mapId = Integer.parseInt(trimmed);
            EditableL1Map map = loader.load(mapId);
            MapContext context = new MapContext(map);
            contexts.put(mapId, context);
            if (!mapListModel.contains(mapId)) {
                mapListModel.addElement(mapId);
            }
            mapList.setSelectedValue(mapId, true);
            statusLabel.setText("Loaded map " + mapId);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid map ID: " + input, "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Load Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void activateMap(int mapId) {
        activeContext = contexts.get(mapId);
        if (activeContext == null) {
            return;
        }
        mapCanvas.setMap(activeContext.getMap());
        minimapPanel.setMap(activeContext.getMap());
        paintingInProgress = false;
        updateCoordinateLabel(-1, -1);
        updateUndoRedoButtons();
    }

    private void applyPaint(int startX, int startY, int endX, int endY) {
        if (activeContext == null) {
            return;
        }
        EditableL1Map map = activeContext.getMap();
        ZoneType zone = palettePanel.getSelectedZone();
        boolean passable = palettePanel.isPassableSelected();
        int tileId = palettePanel.getSelectedTileId();
        int worldStartX = map.getX();
        int worldStartY = map.getY();
        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                int worldX = worldStartX + x;
                int worldY = worldStartY + y;
                map.setOriginalTile(worldX, worldY, (short) tileId);
                map.setPassable(worldX, worldY, passable);
                map.setZone(worldX, worldY, zone);
            }
        }
        minimapPanel.setMap(map);
        mapCanvas.repaint();
    }

    private void updateCoordinateLabel(int tileX, int tileY) {
        if (activeContext == null) {
            coordinateLabel.setText("No map selected");
            return;
        }
        if (tileX < 0 || tileY < 0) {
            coordinateLabel.setText("Map " + activeContext.getMap().getId() + ": hover tiles for details");
            return;
        }
        EditableL1Map map = activeContext.getMap();
        int worldX = map.getX() + tileX;
        int worldY = map.getY() + tileY;
        coordinateLabel.setText(map.toString(new l1j.server.server.types.Point(worldX, worldY)));
    }

    private void saveActiveMap() {
        if (activeContext == null) {
            JOptionPane.showMessageDialog(this, "No map selected", "Save Failed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            writer.write(activeContext.getMap());
            statusLabel.setText("Saved map " + activeContext.getMap().getId());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Save Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportAll() {
        int saved = 0;
        for (MapContext context : contexts.values()) {
            try {
                writer.write(context.getMap());
                saved++;
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Export Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        statusLabel.setText("Exported " + saved + " maps to " + Paths.get("maps").toAbsolutePath());
    }

    private void undoAction(ActionEvent event) {
        if (activeContext == null) {
            return;
        }
        activeContext.getHistory().undo(activeContext.getMap());
        mapCanvas.repaint();
        minimapPanel.setMap(activeContext.getMap());
        updateUndoRedoButtons();
    }

    private void redoAction(ActionEvent event) {
        if (activeContext == null) {
            return;
        }
        activeContext.getHistory().redo(activeContext.getMap());
        mapCanvas.repaint();
        minimapPanel.setMap(activeContext.getMap());
        updateUndoRedoButtons();
    }

    private void updateUndoRedoButtons() {
        if (activeContext == null) {
            undoButton.setEnabled(false);
            redoButton.setEnabled(false);
            return;
        }
        undoButton.setEnabled(activeContext.getHistory().canUndo());
        redoButton.setEnabled(activeContext.getHistory().canRedo());
    }
}
