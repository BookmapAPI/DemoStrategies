package velox.api.layer1.simpledemo.agreement;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import velox.api.layer1.Layer1ApiInstrumentSpecificEnabledStateProvider;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.agreement.AgreementComponent;
import velox.api.layer1.agreement.AgreementHelper;
import velox.api.layer1.agreement.AgreementInfo;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1Attachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.annotations.Layer1TradingStrategy;

@Layer1Attachable
@Layer1StrategyName("Simple Agreement Demo")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
@Layer1TradingStrategy
public class SimpleAgreementDemo implements Layer1ApiInstrumentSpecificEnabledStateProvider {

    private boolean isAgreementAccepted = false;

    private Map<String, Runnable> recheckCallbacksByAlias = new HashMap<>();

    public SimpleAgreementDemo(Layer1ApiProvider provider) {
    }

    @Override
    public void onStrategyCheckboxEnabled(String alias, boolean isEnabled) {
        AgreementInfo agreementInfo = new AgreementInfo.Builder()
                .setAgreementId("simple-agreement-demo#simple-agreement-demo-v1")
                .setAgreementTitle("Simple Agreement Demo")
                // optional. "Accept" by default:
                .setAcceptButtonText("Demo Accept")
                // optional. "Accept" by default:
                .setDeclineButtonText("Demo Decline")
                // agreement will expire in 5minutes, and after that user will have to accept it again:
                .setExpirationDate(Instant.now().plusSeconds(5 * 60))
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
                    String userChoiceText = userChoices.entrySet().stream()
                            .map(entry -> entry.getKey() + "=" + entry.getValue())
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("No choices");
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
        return false;
    }

    @Override
    public void setStrategyEnabledRecheckCallback(String alias, Runnable recheckCallback) {
        SwingUtilities.invokeLater(() -> recheckCallbacksByAlias.put(alias, recheckCallback));
    }
}
