package velox.api.layer1.simpledemo.timezone;

import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.Layer1CustomPanelsGetter;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1Attachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.common.helper.TimeZoneHelper;
import velox.api.layer1.common.helper.TimeZoneHelper.TimeZoneListener;
import velox.gui.StrategyPanel;

@Layer1Attachable
@Layer1StrategyName("Time zone demo")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class TimeZoneDemo implements
        Layer1ApiFinishable,
        Layer1CustomPanelsGetter,
        TimeZoneListener {
    private final AtomicReference<TimeZonePanel> panel = new AtomicReference<>();
    private volatile TimeZone timeZone;

    public TimeZoneDemo(Layer1ApiProvider provider) {
        TimeZoneHelper.addListener(this);
        timeZone = TimeZoneHelper.getTimeZone();
    }

    @Override
    public void onTimeZoneChanged(TimeZone timeZone) {
        this.timeZone = (TimeZone) timeZone.clone();
        updatePanel();
    }

    @Override
    public StrategyPanel[] getCustomGuiFor(String alias, String indicatorName) {
        TimeZonePanel currentPanel = panel.updateAndGet(
                value -> Objects.requireNonNullElseGet(value, TimeZonePanel::new));
        currentPanel.update(timeZone);
        return new StrategyPanel[] {currentPanel};
    }

    @Override
    public void finish() {
        TimeZoneHelper.removeListener(this);
        panel.set(null);
    }

    private void updatePanel() {
        if (panel.get() != null) {
            SwingUtilities.invokeLater(() -> {
                TimeZonePanel currentPanel = panel.get();
                TimeZone currentTimeZone = timeZone;
                if (currentPanel != null && currentTimeZone != null) {
                    currentPanel.update(currentTimeZone);
                }
            });
        }
    }
}
