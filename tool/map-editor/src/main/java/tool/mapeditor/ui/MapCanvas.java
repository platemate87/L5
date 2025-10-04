package tool.mapeditor.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import javax.swing.JPanel;

import tool.mapeditor.model.EditableL1Map;

public class MapCanvas extends JPanel {
    public enum PaintMode {
        BRUSH,
        RECTANGLE
    }

    public static class PaintRequest {
        public final int startX;
        public final int startY;
        public final int endX;
        public final int endY;

        public PaintRequest(int startX, int startY, int endX, int endY) {
            this.startX = Math.min(startX, endX);
            this.startY = Math.min(startY, endY);
            this.endX = Math.max(startX, endX);
            this.endY = Math.max(startY, endY);
        }
    }

    private EditableL1Map map;
    private int tileSize = 16;
    private PaintMode paintMode = PaintMode.BRUSH;
    private Consumer<PaintRequest> paintListener;
    private Consumer<Point> hoverListener;
    private boolean showPassabilityOverlay = true;
    private boolean showZoneOverlay = false;

    private Point dragStart;
    private Point hoverTile;
    private Rectangle previewRectangle;

    public MapCanvas() {
        setBackground(Color.DARK_GRAY);
        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (map == null) {
                    return;
                }
                int tileX = clampTile(e.getX() / tileSize, map.getWidth());
                int tileY = clampTile(e.getY() / tileSize, map.getHeight());
                dragStart = new Point(tileX, tileY);
                if (paintMode == PaintMode.BRUSH) {
                    firePaint(new PaintRequest(tileX, tileY, tileX, tileY));
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (map == null) {
                    return;
                }
                int tileX = clampTile(e.getX() / tileSize, map.getWidth());
                int tileY = clampTile(e.getY() / tileSize, map.getHeight());
                hoverTile = new Point(tileX, tileY);
                notifyHover(tileX, tileY);
                if (paintMode == PaintMode.BRUSH && dragStart != null) {
                    firePaint(new PaintRequest(tileX, tileY, tileX, tileY));
                } else if (paintMode == PaintMode.RECTANGLE && dragStart != null) {
                    previewRectangle = createRectangle(dragStart.x, dragStart.y, tileX, tileY);
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (map == null || dragStart == null) {
                    return;
                }
                int tileX = clampTile(e.getX() / tileSize, map.getWidth());
                int tileY = clampTile(e.getY() / tileSize, map.getHeight());
                if (paintMode == PaintMode.RECTANGLE) {
                    firePaint(new PaintRequest(dragStart.x, dragStart.y, tileX, tileY));
                }
                dragStart = null;
                previewRectangle = null;
                repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (map == null) {
                    return;
                }
                int tileX = clampTile(e.getX() / tileSize, map.getWidth());
                int tileY = clampTile(e.getY() / tileSize, map.getHeight());
                hoverTile = new Point(tileX, tileY);
                notifyHover(tileX, tileY);
                repaint();
            }
        };
        addMouseListener(adapter);
        addMouseMotionListener(adapter);
    }

    private void notifyHover(int tileX, int tileY) {
        if (hoverListener != null) {
            hoverListener.accept(new Point(tileX, tileY));
        }
    }

    private Rectangle createRectangle(int x1, int y1, int x2, int y2) {
        int startX = Math.min(x1, x2);
        int startY = Math.min(y1, y2);
        int width = Math.abs(x1 - x2) + 1;
        int height = Math.abs(y1 - y2) + 1;
        return new Rectangle(startX, startY, width, height);
    }

    private int clampTile(int value, int max) {
        if (max <= 0) {
            return 0;
        }
        if (value < 0) {
            return 0;
        }
        if (value >= max) {
            return max - 1;
        }
        return value;
    }

    private void firePaint(PaintRequest request) {
        if (paintListener != null) {
            paintListener.accept(request);
        }
    }

    public void setMap(EditableL1Map map) {
        this.map = map;
        this.dragStart = null;
        this.previewRectangle = null;
        this.hoverTile = null;
        updatePreferredSize();
        repaint();
    }

    public EditableL1Map getMap() {
        return map;
    }

    public void setPaintMode(PaintMode mode) {
        this.paintMode = mode;
    }

    public void setTileSize(int tileSize) {
        this.tileSize = tileSize;
        updatePreferredSize();
        revalidate();
        repaint();
    }

    public void setShowPassabilityOverlay(boolean show) {
        this.showPassabilityOverlay = show;
        repaint();
    }

    public void setShowZoneOverlay(boolean show) {
        this.showZoneOverlay = show;
        repaint();
    }

    public void setPaintListener(Consumer<PaintRequest> listener) {
        this.paintListener = listener;
    }

    public void setHoverListener(Consumer<Point> listener) {
        this.hoverListener = listener;
    }

    private void updatePreferredSize() {
        if (map == null) {
            setPreferredSize(new Dimension(400, 400));
            return;
        }
        setPreferredSize(new Dimension(map.getWidth() * tileSize, map.getHeight() * tileSize));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (map == null) {
            return;
        }
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int width = map.getWidth();
        int height = map.getHeight();
        int worldStartX = map.getX();
        int worldStartY = map.getY();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int worldX = worldStartX + x;
                int worldY = worldStartY + y;
                int tile = map.getOriginalTile(worldX, worldY);
                Color color = colorForTile(tile);
                g2d.setColor(color);
                g2d.fillRect(x * tileSize, y * tileSize, tileSize, tileSize);

                if (showZoneOverlay) {
                    drawZoneOverlay(g2d, worldX, worldY, x, y);
                }
                if (showPassabilityOverlay) {
                    drawPassabilityOverlay(g2d, worldX, worldY, x, y);
                }
            }
        }

        g2d.setColor(new Color(0, 0, 0, 60));
        for (int x = 0; x <= width; x++) {
            g2d.drawLine(x * tileSize, 0, x * tileSize, height * tileSize);
        }
        for (int y = 0; y <= height; y++) {
            g2d.drawLine(0, y * tileSize, width * tileSize, y * tileSize);
        }

        if (previewRectangle != null) {
            g2d.setColor(new Color(255, 255, 0, 120));
            g2d.fillRect(previewRectangle.x * tileSize, previewRectangle.y * tileSize,
                    previewRectangle.width * tileSize, previewRectangle.height * tileSize);
        }

        if (hoverTile != null) {
            g2d.setColor(new Color(255, 255, 255, 150));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRect(hoverTile.x * tileSize, hoverTile.y * tileSize, tileSize, tileSize);
        }

        g2d.dispose();
    }

    private void drawPassabilityOverlay(Graphics2D g2d, int worldX, int worldY, int tileX, int tileY) {
        if (map.isImpassable(worldX, worldY)) {
            g2d.setColor(new Color(200, 0, 0, 120));
        } else {
            g2d.setColor(new Color(0, 200, 0, 80));
        }
        g2d.fillRect(tileX * tileSize, tileY * tileSize, tileSize, tileSize);
    }

    private void drawZoneOverlay(Graphics2D g2d, int worldX, int worldY, int tileX, int tileY) {
        Color overlay;
        if (map.isSafetyZone(worldX, worldY)) {
            overlay = new Color(0, 120, 255, 90);
        } else if (map.isCombatZone(worldX, worldY)) {
            overlay = new Color(255, 120, 0, 90);
        } else {
            overlay = new Color(255, 255, 255, 30);
        }
        g2d.setColor(overlay);
        g2d.fillRect(tileX * tileSize, tileY * tileSize, tileSize, tileSize);
    }

    private Color colorForTile(int tile) {
        float hue = (tile % 360) / 360f;
        float saturation = 0.4f + ((tile % 5) * 0.1f);
        float brightness = 0.6f + ((tile % 3) * 0.1f);
        return Color.getHSBColor(hue, Math.min(1f, saturation), Math.min(1f, brightness));
    }
}
