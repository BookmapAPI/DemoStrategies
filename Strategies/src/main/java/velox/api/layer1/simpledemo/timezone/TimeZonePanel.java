package velox.api.layer1.simpledemo.timezone;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.TimeZone;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTextArea;

import velox.gui.StrategyPanel;

final class TimeZonePanel extends StrategyPanel {
    private final JLabel currentTimeZoneLabel = new JLabel();

    TimeZonePanel() {
        super("Time zone");
        GridBagLayout layout = new GridBagLayout();
        layout.columnWidths = new int[] {0, 0};
        layout.rowHeights = new int[] {0, 0, 0};
        layout.columnWeights = new double[] {1.0, Double.MIN_VALUE};
        layout.rowWeights = new double[] {0.0, 0.0, Double.MIN_VALUE};
        setLayout(layout);

        GridBagConstraints currentTimeZoneLabelConstraints = new GridBagConstraints();
        currentTimeZoneLabelConstraints.anchor = GridBagConstraints.WEST;
        currentTimeZoneLabelConstraints.gridx = 0;
        currentTimeZoneLabelConstraints.gridy = 1;
        add(currentTimeZoneLabel, currentTimeZoneLabelConstraints);

        JTextArea instructionText = new JTextArea(
                "To change the time zone, go to Settings > Time zone. "
                        + "The value above will update automatically.");
        instructionText.setEditable(false);
        instructionText.setFocusable(false);
        instructionText.setLineWrap(true);
        instructionText.setOpaque(false);
        instructionText.setWrapStyleWord(true);
        instructionText.setFont(
                instructionText.getFont().deriveFont(instructionText.getFont().getSize2D() - 2.0f));
        instructionText.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(5, 8, 5, 8)));

        GridBagConstraints instructionTextConstraints = new GridBagConstraints();
        instructionTextConstraints.anchor = GridBagConstraints.WEST;
        instructionTextConstraints.fill = GridBagConstraints.HORIZONTAL;
        instructionTextConstraints.insets = new Insets(0, 0, 5, 0);
        instructionTextConstraints.gridx = 0;
        instructionTextConstraints.gridy = 0;
        add(instructionText, instructionTextConstraints);
    }

    void update(TimeZone timeZone) {
        currentTimeZoneLabel.setText("Current time zone: " + timeZone.getID());
    }
}
