package velox.api.layer1.simpledemo.agreement;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiInstrumentSpecificEnabledStateProvider;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.agreement.AgreementComponent;
import velox.api.layer1.agreement.AgreementHelper;
import velox.api.layer1.agreement.AgreementInfo;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1Attachable;
import velox.api.layer1.annotations.Layer1StrategyName;
@Layer1Attachable
@Layer1StrategyName("Simple Agreement Demo")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class SimpleAgreementDemo implements
        Layer1ApiFinishable,
        Layer1ApiInstrumentSpecificEnabledStateProvider {

    private boolean isStrategyEnabled = false;
    private boolean isAgreementAccepted = false;

    private final Map<String, Runnable> recheckCallbacksByAlias = new HashMap<>();

    public SimpleAgreementDemo(Layer1ApiProvider provider) {}

    @Override
    public void onStrategyCheckboxEnabled(String alias, boolean isEnabled) {
        isStrategyEnabled = isEnabled;

        if (!isEnabled) {
            return;
        }

        AgreementInfo agreementInfo = new AgreementInfo.Builder()
                .setAgreementId("simple-agreement-demo#simple-agreement-demo-v1")
                .setAgreementText("""
                        <html>
                            This is a simple agreement demo. By accepting this agreement, you agree that you<br>
                            understand that this is just a demo, and <b>it doesn't do anything.</b><br>
                            Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
                        </html>
                        """)
                .setAgreementTitle("Simple Agreement Demo")
                // optional. "Accept" by default:
                .setAcceptButtonText("Demo Accept")
                // optional. "Accept" by default:
                .setDeclineButtonText("Demo Decline")
                // agreement will expire in 2 days, and after that user will have to accept it again:
                .setExpirationDate(LocalDate.now(ZoneOffset.UTC).plusDays(2))
                .addRadioButtonGroup(
                        "demo-radio-group", "Choose one of the options:",
                        new AgreementComponent.RadioButtonGroup.Option[] {
                                new AgreementComponent.RadioButtonGroup.Option("option1", "Option 1"),
                                new AgreementComponent.RadioButtonGroup.Option("option2", "Option 2"),
                                new AgreementComponent.RadioButtonGroup.Option("option3", "Option 3")
                        })
                .build();

        AgreementHelper.requestAgreement(
                agreementInfo, (agreementStatus, userChoices) -> {
                    String userChoiceText = userChoices != null ? userChoices.entrySet().stream()
                            .map(entry -> entry.getKey() + "=" + entry.getValue())
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("No choices") : null;
                    JOptionPane.showMessageDialog(
                            null,
                            "\"" + agreementInfo.agreementTitle + "\" agreement status: " + agreementStatus
                                    + ", user choices: " + userChoiceText);
                    isAgreementAccepted = agreementStatus.isAccepted;
                    for (Runnable recheckCallback : recheckCallbacksByAlias.values()) {
                        recheckCallback.run();
                    }
                });
    }

    @Override
    public boolean isStrategyEnabled(String alias) {
        return isAgreementAccepted && isStrategyEnabled;
    }

    @Override
    public void setStrategyEnabledRecheckCallback(String alias, Runnable recheckCallback) {
        SwingUtilities.invokeLater(() -> recheckCallbacksByAlias.put(alias, recheckCallback));
    }

    @Override
    public void finish() {}
}
