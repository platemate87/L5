package tool.mapeditor.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import tool.mapeditor.model.EditableL1Map;

public class MinimapPanel extends JPanel {
    private BufferedImage minimapImage;

    public MinimapPanel() {
        setPreferredSize(new Dimension(180, 180));
    }

    public void setMap(EditableL1Map map) {
        if (map == null) {
            minimapImage = null;
            repaint();
            return;
        }
        int width = Math.max(1, map.getWidth());
        int height = Math.max(1, map.getHeight());
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int worldX = map.getX() + x;
                int worldY = map.getY() + y;
                int tile = map.getOriginalTile(worldX, worldY);
                int color = java.awt.Color.getHSBColor((tile % 360) / 360f, 0.6f, 0.8f).getRGB();
                if (map.isImpassable(worldX, worldY)) {
                    color = 0xAAFF0000;
                } else if (map.isSafetyZone(worldX, worldY)) {
                    color = 0x8800AAFF;
                } else if (map.isCombatZone(worldX, worldY)) {
                    color = 0x88FF8800;
                }
                image.setRGB(x, y, color);
            }
        }
        minimapImage = image;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (minimapImage == null) {
            return;
        }
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        Image scaled = minimapImage.getScaledInstance(getWidth(), getHeight(), Image.SCALE_SMOOTH);
        g2d.drawImage(scaled, 0, 0, getWidth(), getHeight(), null);
        g2d.dispose();
    }
}
