package velox.api.layer1.simpledemo.agreement;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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

    private static final int AGREEMENTS_COUNT = 4;

    private boolean isStrategyEnabled;
    private boolean isAgreementAccepted;

    private final Map<String, Runnable> recheckCallbacksByAlias = new HashMap<>();

    public SimpleAgreementDemo(Layer1ApiProvider provider) {}

    @Override
    public void onStrategyCheckboxEnabled(String alias, boolean isEnabled) {
        isStrategyEnabled = isEnabled;

        if (isEnabled && !isAgreementAccepted) {
            requestAgreements();
        }
    }

    private void requestAgreements() {
        List<CompletableFuture<AgreementResult>> results = new ArrayList<>(AGREEMENTS_COUNT);
        for (int agreementNumber = 1; agreementNumber <= AGREEMENTS_COUNT; agreementNumber++) {
            results.add(requestAgreement(agreementNumber));
        }

        CompletableFuture.allOf(results.toArray(CompletableFuture<?>[]::new))
                .thenRun(() -> showAgreementResults(results));
    }

    private void showAgreementResults(List<CompletableFuture<AgreementResult>> results) {
        List<AgreementResult> agreementResults = results.stream().map(CompletableFuture::join).toList();
        SwingUtilities.invokeLater(() -> {
            isAgreementAccepted = isStrategyEnabled
                    && agreementResults.stream().allMatch(AgreementResult::accepted);
            JOptionPane.showMessageDialog(
                    null,
                    String.join("\n", agreementResults.stream().map(AgreementResult::message).toList()));
            recheckCallbacksByAlias.values().forEach(Runnable::run);
        });
    }

    private CompletableFuture<AgreementResult> requestAgreement(int agreementNumber) {
        CompletableFuture<AgreementResult> result = new CompletableFuture<>();
        AgreementInfo agreementInfo = buildAgreementInfo(agreementNumber);
        AgreementHelper.requestAgreement(
                agreementInfo,
                (agreementStatus, userChoices) -> result.complete(new AgreementResult(
                        "\"" + agreementInfo.agreementTitle + "\" agreement status: " + agreementStatus
                                + ", user choices: " + getUserChoiceText(userChoices),
                        agreementStatus.isAccepted)));
        return result;
    }

    private String getUserChoiceText(Map<String, String> userChoices) {
        if (userChoices == null) {
            return null;
        }
        String choices = userChoices.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));
        return choices.isEmpty() ? "No choices" : choices;
    }

    private AgreementInfo buildAgreementInfo(int agreementNumber) {
        return new AgreementInfo.Builder()
                .setAgreementId("simple-agreement-demo#simple-agreement-demo-" + agreementNumber + "-v1")
                .setAgreementText("""
                        <html>
                            This is simple agreement demo %d of %d. By accepting this agreement, you agree that you<br>
                            understand that this is just a demo, and <b>it doesn't do anything.</b><br>
                            Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
                        </html>
                        """
                        .formatted(agreementNumber, AGREEMENTS_COUNT))
                .setAgreementTitle("Simple Agreement Demo " + agreementNumber + "/" + AGREEMENTS_COUNT)
                // optional. "Accept" by default:
                .setAcceptButtonText("Demo Accept")
                // optional. "Decline" by default:
                .setDeclineButtonText("Demo Decline")
                // agreement will expire in 2 days, and after that user will have to accept it again:
                .setExpirationDate(LocalDate.now(ZoneOffset.UTC).plusDays(2))
                .addRadioButtonGroup(
                        "demo-radio-group-" + agreementNumber, "Choose one of the options:",
                        new AgreementComponent.RadioButtonGroup.Option[] {
                                new AgreementComponent.RadioButtonGroup.Option("option1", "Option 1"),
                                new AgreementComponent.RadioButtonGroup.Option("option2", "Option 2"),
                                new AgreementComponent.RadioButtonGroup.Option("option3", "Option 3")
                        })
                .setDeclineConfirmation(component -> {
                    int result = JOptionPane.showConfirmDialog(
                            component,
                            "Are you sure you want to decline this agreement?",
                            "Decline confirmation",
                            JOptionPane.YES_NO_OPTION);
                    return result == JOptionPane.YES_OPTION;
                })
                .build();
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

    private record AgreementResult(String message, boolean accepted) {}
}
