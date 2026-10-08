package software.coley.recaf.services.workspace.io;

import jakarta.annotation.Nonnull;

import java.nio.file.Path;
import java.util.Arrays;

/**
 * APK signing configuration with independently owned, closeable password arrays.
 *
 * @author Matt Coley
 */
public final class ApkSigningOptions implements AutoCloseable {
	private final Path keyStorePath;
	private final KeyStoreType keyStoreType;
	private final String keyAlias;
	private final char[] storePassword;
	private final char[] keyPassword;

	/**
	 * @param keyStorePath
	 * 		Path to the keystore file.
	 * @param keyStoreType
	 * 		Keystore type.
	 * @param keyAlias
	 * 		Alias of the key to use for signing.
	 * @param storePassword
	 * 		Keystore password.
	 * @param keyPassword
	 * 		Private-key password.
	 */
	public ApkSigningOptions(@Nonnull Path keyStorePath, @Nonnull KeyStoreType keyStoreType,
	                         @Nonnull String keyAlias, @Nonnull char[] storePassword, @Nonnull char[] keyPassword) {
		this.keyStorePath = keyStorePath;
		this.keyStoreType = keyStoreType;
		this.keyAlias = keyAlias;
		this.storePassword = storePassword.clone();
		this.keyPassword = keyPassword.clone();
	}

	/**
	 * @return Path to the keystore file.
	 */
	@Nonnull
	public Path keyStorePath() {
		return keyStorePath;
	}

	/**
	 * @return Keystore type.
	 */
	@Nonnull
	public KeyStoreType keyStoreType() {
		return keyStoreType;
	}

	/**
	 * @return Alias of the key to use for signing.
	 */
	@Nonnull
	public String keyAlias() {
		return keyAlias;
	}

	/**
	 * @return Keystore password.
	 */
	@Nonnull
	public char[] storePassword() {
		return storePassword;
	}

	/**
	 * @return Private-key password.
	 */
	@Nonnull
	public char[] keyPassword() {
		return keyPassword;
	}

	@Override
	public void close() {
		Arrays.fill(storePassword, '\0');
		Arrays.fill(keyPassword, '\0');
	}

	/**
	 * Keystore types supported by the APK signing process.
	 */
	public enum KeyStoreType {
		JKS,
		PKCS12
	}
}
