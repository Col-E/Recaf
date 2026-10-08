package software.coley.recaf.services.workspace.io;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableBoolean;
import software.coley.observables.ObservableObject;
import software.coley.observables.ObservableString;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration for signing APK exports.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class ApkSigningConfig extends BasicConfigContainer {
	public static final String ID = "apk-signing";
	public static final String KEY_ENABLED = "signing-enabled";
	public static final String KEY_KEYSTORE_PATH = "keystore-path";
	public static final String KEY_KEYSTORE_TYPE = "keystore-type";
	public static final String KEY_KEY_ALIAS = "key-alias";
	public static final String KEY_CREDENTIALS = "signing-credentials";

	private final ObservableBoolean signingEnabled = new ObservableBoolean(false);
	private final ObservableString keystorePath = new ObservableString("");
	private final ObservableObject<ApkSigningOptions.KeyStoreType> keystoreType = new ObservableObject<>(ApkSigningOptions.KeyStoreType.PKCS12);
	private final ObservableString keyAlias = new ObservableString("");
	private final ObservableString credentials = new ObservableString("");

	@Inject
	public ApkSigningConfig() {
		super(ConfigGroups.SERVICE_IO, ID + CONFIG_SUFFIX);
		addValue(new BasicConfigValue<>(KEY_ENABLED, boolean.class, signingEnabled));
		addValue(new BasicConfigValue<>(KEY_KEYSTORE_PATH, String.class, keystorePath));
		addValue(new BasicConfigValue<>(KEY_KEYSTORE_TYPE, ApkSigningOptions.KeyStoreType.class, keystoreType));
		addValue(new BasicConfigValue<>(KEY_KEY_ALIAS, String.class, keyAlias));
		addValue(new BasicConfigValue<>(KEY_CREDENTIALS, String.class, credentials));
	}

	/**
	 * @return {@code true} to sign APK exports using the configured keystore.
	 */
	@Nonnull
	public ObservableBoolean getSigningEnabled() {
		return signingEnabled;
	}

	/**
	 * @return Path to the JKS or PKCS12 keystore used for APK signing.
	 */
	@Nonnull
	public ObservableString getKeystorePath() {
		return keystorePath;
	}

	/**
	 * @return Keystore format used for APK signing.
	 */
	@Nonnull
	public ObservableObject<ApkSigningOptions.KeyStoreType> getKeystoreType() {
		return keystoreType;
	}

	/**
	 * @return Alias of the private key used for APK signing.
	 */
	@Nonnull
	public ObservableString getKeyAlias() {
		return keyAlias;
	}

	/**
	 * @return Encrypted APK signing credential envelope, or an empty string when not configured.
	 */
	@Nonnull
	public ObservableString getCredentials() {
		return credentials;
	}

	/**
	 * @return {@code true} when a keystore path is configured.
	 */
	public boolean hasKeystorePath() {
		return isConfigured(keystorePath.getValue());
	}

	/**
	 * Validates the keystore settings required to sign APK exports.
	 *
	 * @return Path to the existing keystore file.
	 *
	 * @throws IOException
	 * 		When the keystore path, type, or alias is missing or invalid, or the keystore file does not exist.
	 */
	@Nonnull
	public Path requireKeystoreSettings() throws IOException {
		String configuredPath = keystorePath.getValue();
		if (!isConfigured(configuredPath))
			throw new IOException("Keystore path is not configured");

		Path path;
		try {
			path = Paths.get(configuredPath);
		} catch (InvalidPathException ex) {
			throw new IOException("Keystore path is invalid", ex);
		}
		if (!Files.isRegularFile(path))
			throw new IOException("Configured keystore file does not exist");

		if (keystoreType.getValue() == null)
			throw new IOException("Keystore type is not configured");

		if (!isConfigured(keyAlias.getValue()))
			throw new IOException("Keystore key alias is not configured");

		return path;
	}

	/**
	 * @return Encrypted credential envelope.
	 *
	 * @throws IOException
	 * 		When no credentials are configured.
	 */
	@Nonnull
	public String requireCredentialEnvelope() throws IOException {
		String envelope = credentials.getValue();
		if (!isConfigured(envelope))
			throw new IOException("APK signing credentials are not configured");
		return envelope;
	}

	private static boolean isConfigured(@Nullable String value) {
		return value != null && !value.isBlank();
	}
}
