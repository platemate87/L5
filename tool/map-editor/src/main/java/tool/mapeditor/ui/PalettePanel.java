package tool.mapeditor.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;

import tool.mapeditor.model.ZoneType;

public class PalettePanel extends JPanel {
    private final JSpinner tileSpinner;
    private final JCheckBox passableCheck;
    private final JComboBox<ZoneType> zoneCombo;
    private final JToggleButton brushToggle;
    private final JToggleButton rectangleToggle;
    private final JCheckBox passabilityOverlayCheck;
    private final JCheckBox zoneOverlayCheck;

    public PalettePanel() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;

        add(new JLabel("Tile ID"), gbc);
        gbc.gridx = 1;
        tileSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 255, 1));
        add(tileSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Zone"), gbc);
        gbc.gridx = 1;
        zoneCombo = new JComboBox<>(ZoneType.values());
        zoneCombo.setSelectedItem(ZoneType.NORMAL);
        add(zoneCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Passable"), gbc);
        gbc.gridx = 1;
        passableCheck = new JCheckBox();
        passableCheck.setSelected(true);
        add(passableCheck, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Tool"), gbc);
        gbc.gridx = 1;
        JPanel toolPanel = new JPanel();
        brushToggle = new JToggleButton("Brush");
        rectangleToggle = new JToggleButton("Rectangle");
        ButtonGroup group = new ButtonGroup();
        group.add(brushToggle);
        group.add(rectangleToggle);
        brushToggle.setSelected(true);
        toolPanel.add(brushToggle);
        toolPanel.add(rectangleToggle);
        add(toolPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Overlays"), gbc);
        gbc.gridx = 1;
        JPanel overlayPanel = new JPanel();
        passabilityOverlayCheck = new JCheckBox("Passability");
        passabilityOverlayCheck.setSelected(true);
        zoneOverlayCheck = new JCheckBox("Zones");
        overlayPanel.add(passabilityOverlayCheck);
        overlayPanel.add(zoneOverlayCheck);
        add(overlayPanel, gbc);
    }

    public int getSelectedTileId() {
        return (Integer) tileSpinner.getValue();
    }

    public void setSelectedTileId(int tileId) {
        tileSpinner.setValue(tileId);
    }

    public boolean isPassableSelected() {
        return passableCheck.isSelected();
    }

    public void setPassableSelected(boolean value) {
        passableCheck.setSelected(value);
    }

    public ZoneType getSelectedZone() {
        return (ZoneType) zoneCombo.getSelectedItem();
    }

    public void setSelectedZone(ZoneType zone) {
        zoneCombo.setSelectedItem(zone);
    }

    public JToggleButton getBrushToggle() {
        return brushToggle;
    }

    public JToggleButton getRectangleToggle() {
        return rectangleToggle;
    }

    public JCheckBox getPassabilityOverlayCheck() {
        return passabilityOverlayCheck;
    }

    public JCheckBox getZoneOverlayCheck() {
        return zoneOverlayCheck;
    }
}
