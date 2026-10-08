package software.coley.recaf.ui.control.popup;

import jakarta.annotation.Nonnull;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import software.coley.recaf.services.workspace.io.ApkSigningConfig;
import software.coley.recaf.services.workspace.io.ApkSigningOptions;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.window.RecafScene;
import software.coley.recaf.ui.window.RecafStage;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.android.ApkSigningCredentialCodec;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import static atlantafx.base.theme.Styles.*;

/**
 * Popup for configuring APK signing credentials.
 *
 * @author Matt Coley
 */
public class ApkSigningCredentialsPopup extends RecafStage {
	private static final PseudoClass INVALID_INPUT = PseudoClass.getPseudoClass(DANGER);

	private final ApkSigningConfig signingConfig;
	private final PasswordField storePasswordInput = new PasswordField();
	private final PasswordField keyPasswordInput = new PasswordField();
	private final PasswordField masterPasswordInput = new PasswordField();
	private final Label error = new Label();

	/**
	 * @param signingConfig
	 * 		APK signing configuration containing the keystore path, type, alias, and encrypted envelope observable.
	 */
	public ApkSigningCredentialsPopup(@Nonnull ApkSigningConfig signingConfig) {
		this.signingConfig = signingConfig;
		setTitle(Lang.get("service.io.apk-signing-config.credentials.configure-title"));
		hideOnEscape();
		setOnCloseRequest(event -> clear());
		setOnHidden(event -> clear());

		Label warning = new BoundLabel(Lang.getBinding("service.io.apk-signing-config.credentials.warning"));
		warning.setWrapText(true);
		warning.getStyleClass().add(WARNING);

		Label target = new Label(configuredTargetDescription());
		target.setWrapText(true);

		GridPane fields = new GridPane();
		fields.setHgap(10);
		fields.setVgap(8);
		fields.addRow(0, new BoundLabel(Lang.getBinding("service.io.apk-signing-config.credentials.keystore-password")), storePasswordInput);
		fields.addRow(1, new BoundLabel(Lang.getBinding("service.io.apk-signing-config.credentials.key-password")), keyPasswordInput);
		fields.addRow(2, new BoundLabel(Lang.getBinding("service.io.apk-signing-config.credentials.master-password")), masterPasswordInput);

		error.setWrapText(true);
		error.getStyleClass().add(DANGER);
		error.setVisible(false);
		error.setManaged(false);

		ActionButton save = new ActionButton(Lang.getBinding("dialog.confirm"), this::save);
		save.setDisable(true);
		masterPasswordInput.pseudoClassStateChanged(INVALID_INPUT, true);
		masterPasswordInput.textProperty().addListener((ob, old, cur) -> {
			boolean empty = cur.isEmpty();
			save.setDisable(empty);
			masterPasswordInput.pseudoClassStateChanged(INVALID_INPUT, empty);
		});
		ActionButton cancel = new ActionButton(Lang.getBinding("dialog.cancel"), this::cancel);
		save.getStyleClass().addAll(SUCCESS);
		cancel.getStyleClass().addAll(DANGER);
		HBox buttons = new HBox(10, save, cancel);
		buttons.setAlignment(Pos.CENTER_RIGHT);

		VBox layout = new VBox(12, warning, target, fields, error, buttons);
		layout.setPadding(new Insets(14));
		layout.setFillWidth(true);
		setScene(new RecafScene(layout, 640, 330));
	}

	/**
	 * @return Future containing the master password, or {@code null} for cancellation.
	 */
	@Nonnull
	public static CompletableFuture<char[]> promptMasterPassword() {
		CompletableFuture<char[]> result = new CompletableFuture<>();
		FxThreadUtil.run(() -> {
			RecafStage stage = new RecafStage();
			stage.setTitle(Lang.get("service.io.apk-signing-config.credentials.prompt-title"));
			stage.hideOnEscape();

			PasswordField input = new PasswordField();
			Label warning = new BoundLabel(Lang.getBinding("service.io.apk-signing-config.credentials.master-password"));
			warning.setWrapText(true);

			Runnable cancel = () -> {
				input.clear();
				if (!result.isDone())
					result.complete(null);
				stage.hide();
			};
			Runnable accept = () -> {
				String text = input.getText();
				if (text.isEmpty())
					return;
				char[] password = text.toCharArray();
				input.clear();
				if (!result.complete(password))
					Arrays.fill(password, '\0');
				stage.hide();
			};
			input.setOnAction(event -> accept.run());
			ActionButton confirm = new ActionButton(Lang.get("dialog.confirm"), accept);
			confirm.getStyleClass().addAll(SUCCESS);
			confirm.setDisable(true);
			input.pseudoClassStateChanged(INVALID_INPUT, true);
			input.textProperty().addListener((ob, old, cur) -> {
				boolean empty = cur.isEmpty();
				confirm.setDisable(empty);
				input.pseudoClassStateChanged(INVALID_INPUT, empty);
			});
			ActionButton cancelButton = new ActionButton(Lang.get("dialog.cancel"), cancel);
			cancelButton.getStyleClass().addAll(DANGER);
			HBox buttons = new HBox(10, confirm, cancelButton);
			buttons.setAlignment(Pos.CENTER_RIGHT);

			stage.setOnHidden(event -> {
				input.clear();
				if (!result.isDone())
					result.complete(null);
			});
			VBox layout = new VBox(12, warning, input, buttons);
			layout.setPadding(new Insets(14));
			stage.setScene(new RecafScene(layout, 460, 210));
			stage.setOnShown(event -> input.requestFocus());
			stage.showAndWait();
		});
		return result;
	}

	/**
	 * Saves the configured credentials.
	 *
	 * @see ApkSigningCredentialCodec
	 */
	private void save() {
		String masterText = masterPasswordInput.getText();
		if (masterText.isEmpty())
			return;
		char[] storePassword = storePasswordInput.getText().toCharArray();
		char[] keyPassword = keyPasswordInput.getText().toCharArray();
		char[] masterPassword = masterText.toCharArray();
		try {
			// Validate all configured keystore data before touching the existing envelope.
			validateKeyStore(storePassword, keyPassword);
			String envelope = ApkSigningCredentialCodec.encrypt(masterPassword, storePassword, keyPassword);

			// The only persistent secret is the newly-created encrypted envelope. This is intentionally the final step.
			signingConfig.getCredentials().setValue(envelope);
			clear();
			hide();
		} catch (IOException | RuntimeException ex) {
			// Keep the previous envelope and all other settings untouched on every failure.
			showError(Lang.get("service.io.apk-signing-config.credentials.configure-error").formatted(safeMessage(ex)));
		} finally {
			Arrays.fill(storePassword, '\0');
			Arrays.fill(keyPassword, '\0');
			Arrays.fill(masterPassword, '\0');
		}
	}

	/**
	 * Cancels configuration.
	 */
	private void cancel() {
		clear();
		hide();
	}

	/**
	 * Clears all fields.
	 */
	private void clear() {
		storePasswordInput.clear();
		keyPasswordInput.clear();
		masterPasswordInput.clear();
	}

	/**
	 * Validates that the configured keystore path, type, and alias are valid and that the keystore can be read.
	 *
	 * @param storePassword
	 * 		Keystore password to use for validation.
	 * @param keyPassword
	 * 		Key password to use for validation.
	 *
	 * @throws IOException
	 * 		When the keystore is not valid or cannot be read.
	 */
	private void validateKeyStore(@Nonnull char[] storePassword, @Nonnull char[] keyPassword) throws IOException {
		// Validate the keystore path, type, and alias before reading the keystore.
		Path path = signingConfig.requireKeystoreSettings();
		String alias = signingConfig.getKeyAlias().getValue();
		ApkSigningOptions.KeyStoreType configuredType = signingConfig.getKeystoreType().getValue();

		try {
			// Read the keystore and the key within it.
			KeyStore keyStore = KeyStore.getInstance(configuredType.name());
			try (InputStream input = Files.newInputStream(path)) {
				keyStore.load(input, storePassword);
			}
			Key key = keyStore.getKey(alias, keyPassword);
			if (!(key instanceof PrivateKey))
				throw new IOException("Configured alias does not contain a private key");

			// And the certificates for the alias must be valid X.509 certificates.
			Certificate[] chain = keyStore.getCertificateChain(alias);
			if (chain == null || chain.length == 0)
				throw new IOException("Configured alias does not contain a certificate chain");
			for (Certificate certificate : chain) {
				if (!(certificate instanceof X509Certificate))
					throw new IOException("Configured alias does not contain an X.509 certificate chain");
			}
		} catch (IOException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new IOException("Unable to load or validate configured keystore", ex);
		}
	}

	@Nonnull
	private String configuredTargetDescription() {
		String path = signingConfig.getKeystorePath().getValue();
		String alias = signingConfig.getKeyAlias().getValue();
		String notConfigured = Lang.get("service.io.apk-signing-config.credentials.not-configured");
		String pathValue = path == null || path.isBlank() ? notConfigured : path;
		String aliasValue = alias == null || alias.isBlank() ? notConfigured : alias;
		return Lang.get("service.io.apk-signing-config.credentials.path") + ": " + pathValue + '\n' +
				Lang.get("service.io.apk-signing-config.credentials.alias") + ": " + aliasValue;
	}

	@Nonnull
	private static String safeMessage(@Nonnull Throwable throwable) {
		String message = throwable.getMessage();
		return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
	}

	private void showError(@Nonnull String message) {
		error.setText(message);
		error.setVisible(true);
		error.setManaged(true);
	}
}
