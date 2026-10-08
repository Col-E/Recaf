package software.coley.recaf.util.android;

import com.android.apksig.ApkSigner;
import com.android.apksig.ApkVerifier;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.services.workspace.io.ApkSigningOptions;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Signs rebuilt APK archives with user-provided signing credentials.
 *
 * @author Matt Coley
 */
public final class ApkSigningUtil {
	private ApkSigningUtil() {}

	/**
	 * Signs an aligned APK with v1/v2/v3 enabled and verifies the resulting archive.
	 *
	 * @param alignedApk
	 * 		Unprefixed APK bytes, aligned before signing.
	 * @param options
	 * 		Keystore and credential options.
	 *
	 * @return Completed signed APK bytes.
	 *
	 * @throws IOException
	 * 		When the keystore is invalid, signing fails, verification fails, or temporary files cannot be removed.
	 */
	@Nonnull
	public static byte[] signAndVerify(@Nonnull byte[] alignedApk, @Nonnull ApkSigningOptions options) throws IOException {
		Path inputFile = null;
		Path outputFile = null;
		IOException failure = null;
		try {
			KeyStore keyStore = KeyStore.getInstance(options.keyStoreType().name());
			try (InputStream input = Files.newInputStream(options.keyStorePath())) {
				char[] password = options.storePassword();
				keyStore.load(input, password);
			}

			// Keystore must contain the alias provided by the user.
			if (!keyStore.containsAlias(options.keyAlias()))
				throw new IOException("APK signing key alias was not found in the keystore");

			// Keystore must contain a private key for the alias provided by the user.
			Key key = keyStore.getKey(options.keyAlias(), options.keyPassword());
			if (!(key instanceof PrivateKey privateKey))
				throw new IOException("APK signing key alias does not resolve to a private key");

			// The certificates for the alias must be valid X.509 certificates.
			Certificate[] certificates = keyStore.getCertificateChain(options.keyAlias());
			if (certificates == null || certificates.length == 0)
				throw new IOException("APK signing key alias has no certificate chain");
			List<X509Certificate> certificateChain = new ArrayList<>(certificates.length);
			for (Certificate certificate : certificates) {
				if (!(certificate instanceof X509Certificate x509Certificate))
					throw new IOException("APK signing key alias has a non-X.509 certificate chain");
				certificateChain.add(x509Certificate);
			}

			// Sign the APK with the provided credentials.
			ApkSigner.SignerConfig config = new ApkSigner.SignerConfig.Builder(options.keyAlias(), privateKey, certificateChain).build();
			inputFile = Files.createTempFile("recaf-apk-signing-input-", ".apk");
			outputFile = Files.createTempFile("recaf-apk-signing-output-", ".apk");
			Files.write(inputFile, alignedApk);
			new ApkSigner.Builder(List.of(config))
					.setInputApk(inputFile.toFile())
					.setOutputApk(outputFile.toFile())
					.setV1SigningEnabled(true)
					.setV2SigningEnabled(true)
					.setV3SigningEnabled(true)
					.setV4SigningEnabled(false)
					.setAlignmentPreserved(true)
					.build()
					.sign();

			// First verify the schemes the APK's declared minSdk can use.
			ApkVerifier.Result verification = new ApkVerifier.Builder(outputFile.toFile()).build().verify();
			if (!verification.isVerified())
				throw verificationFailure(verification);

			// The platform-equivalent verifier skips old schemes when minSdk is high (for example,
			// v1 and v2 are skipped for API 34). Verify those enabled schemes from API 18, the first
			// Android release supporting SHA-256 JAR signatures, as well.
			if (!verification.isVerifiedUsingV1Scheme() || !verification.isVerifiedUsingV2Scheme() ||
					!verification.isVerifiedUsingV3Scheme()) {
				verification = new ApkVerifier.Builder(outputFile.toFile())
						.setMinCheckedPlatformVersion(18)
						.build()
						.verify();
				if (!verification.isVerified() || !verification.isVerifiedUsingV1Scheme() ||
						!verification.isVerifiedUsingV2Scheme() || !verification.isVerifiedUsingV3Scheme())
					throw verificationFailure(verification);
			}

			return Files.readAllBytes(outputFile);
		} catch (IOException ex) {
			failure = ex;
			throw ex;
		} catch (Throwable t) {
			failure = new IOException("Failed to sign or verify APK output", t);
			throw failure;
		} finally {
			// Cleanup temporary files, collecting any exceptions that occur along the way.
			IOException cleanupFailure = deleteTemporaryFile(inputFile);
			IOException outputCleanupFailure = deleteTemporaryFile(outputFile);
			if (cleanupFailure == null)
				cleanupFailure = outputCleanupFailure;
			else if (outputCleanupFailure != null)
				cleanupFailure.addSuppressed(outputCleanupFailure);

			// Throw if cleanup failed and no other exception was already thrown.
			if (cleanupFailure != null) {
				if (failure != null)
					failure.addSuppressed(cleanupFailure);
				else
					throw cleanupFailure;
			}
		}
	}

	@Nonnull
	private static IOException verificationFailure(ApkVerifier.Result verification) {
		return new IOException("Signed APK failed verification for enabled v1/v2/v3 schemes: " +
				"verified=" + verification.isVerified() +
				", v1=" + verification.isVerifiedUsingV1Scheme() +
				", v2=" + verification.isVerifiedUsingV2Scheme() +
				", v3=" + verification.isVerifiedUsingV3Scheme() +
				", errors=" + verification.getAllErrors());
	}

	@Nullable
	private static IOException deleteTemporaryFile(@Nullable Path file) {
		if (file == null)
			return null;
		try {
			Files.deleteIfExists(file);
			return null;
		} catch (Exception ex) {
			return new IOException("Failed to remove temporary APK signing file: " + file, ex);
		}
	}
}
