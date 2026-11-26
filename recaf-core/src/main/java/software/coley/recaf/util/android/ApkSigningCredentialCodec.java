package software.coley.recaf.util.android;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;

/**
 * Encrypts APK keystore credentials.
 *
 * @author Matt Coley
 */
public final class ApkSigningCredentialCodec {
	private static final String VERSION = "v1";
	private static final int SALT_LENGTH = 16;
	private static final int NONCE_LENGTH = 12;
	private static final int TAG_LENGTH_BITS = 128;
	private static final int PBKDF2_ITERATIONS = 600_000;
	private static final int KEY_LENGTH_BITS = 256;
	private static final byte[] AAD = ("recaf-apk-signing:" + VERSION).getBytes(StandardCharsets.UTF_8);
	private static final Random RANDOM = new SecureRandom();
	private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

	private ApkSigningCredentialCodec() {}

	/**
	 * Encrypts the keystore and private-key passwords using a user-provided master password.
	 *
	 * @param masterPassword
	 * 		Master password. Must not be blank.
	 * @param storePassword
	 * 		Keystore password.
	 * @param keyPassword
	 * 		Private-key password.
	 *
	 * @return Versioned Base64-URL envelope, with no padding.
	 *
	 * @throws IOException
	 * 		When the input is invalid or encryption is unavailable.
	 */
	@Nonnull
	public static String encrypt(@Nonnull char[] masterPassword, @Nonnull char[] storePassword,
	                             @Nonnull char[] keyPassword) throws IOException {
		if (isBlank(masterPassword))
			throw new IOException("Master password must not be blank");

		byte[] storeBytes = null;
		byte[] keyBytes = null;
		byte[] payload = null;
		byte[] derivedKey = null;
		PBEKeySpec keySpec = null;
		try {
			// Encode the passwords into bytes, and then create a payload that contains the lengths and bytes of each password.
			//  - 4: Length of the keystore password
			//  - N: Keystore password bytes
			//  - 4: Length of the private-key password
			//  - M: Private-key password bytes
			storeBytes = encode(storePassword);
			keyBytes = encode(keyPassword);
			payload = ByteBuffer.allocate(Math.addExact(4 + 4, Math.addExact(storeBytes.length, keyBytes.length)))
					.putInt(storeBytes.length).put(storeBytes)
					.putInt(keyBytes.length).put(keyBytes)
					.array();

			// Derive a key from the master password.
			byte[] salt = new byte[SALT_LENGTH];
			byte[] nonce = new byte[NONCE_LENGTH];
			RANDOM.nextBytes(salt);
			RANDOM.nextBytes(nonce);
			keySpec = new PBEKeySpec(masterPassword, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
			derivedKey = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
					.generateSecret(keySpec).getEncoded();

			// Encrypt the payload with AES-GCM using the derived key and nonce.
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(derivedKey, "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
			cipher.updateAAD(AAD);
			byte[] ciphertext = cipher.doFinal(payload);
			return VERSION + ":"
					+ ENCODER.encodeToString(salt) + ':'
					+ ENCODER.encodeToString(nonce) + ':'
					+ ENCODER.encodeToString(ciphertext);
		} catch (Exception ex) {
			throw new IOException("Failed to encrypt APK signing credentials", ex);
		} finally {
			if (keySpec != null)
				keySpec.clearPassword();
			clear(storeBytes);
			clear(keyBytes);
			clear(payload);
			clear(derivedKey);
		}
	}

	/**
	 * Decrypts a credential envelope created by {@link #encrypt(char[], char[], char[])}.
	 *
	 * @param masterPassword
	 * 		Master password. Must not be blank.
	 * @param envelope
	 * 		Versioned encrypted credential envelope.
	 *
	 * @return Decrypted passwords, owned by the returned object and cleared by {@link Credentials#close()}.
	 *
	 * @throws IOException
	 * 		When the envelope is invalid or authentication fails.
	 */
	@Nonnull
	public static Credentials decrypt(@Nonnull char[] masterPassword, @Nonnull String envelope) throws IOException {
		if (isBlank(masterPassword))
			throw new IOException("Master password must not be blank");

		byte[] salt = null;
		byte[] nonce = null;
		byte[] ciphertext = null;
		byte[] derivedKey = null;
		byte[] payload = null;
		char[] storePassword = null;
		char[] keyPassword = null;
		PBEKeySpec keySpec = null;
		try {
			// Split the envelope into its parts and validate the version.
			String[] parts = envelope.split(":", -1);
			if (parts.length != 4 || !VERSION.equals(parts[0]))
				throw new IOException("Malformed or unsupported APK signing credential envelope");

			// Decode the parts and validate their lengths.
			salt = decodePart(parts[1]);
			nonce = decodePart(parts[2]);
			ciphertext = decodePart(parts[3]);
			if (salt.length != SALT_LENGTH || nonce.length != NONCE_LENGTH || ciphertext.length < 24)
				throw new IOException("Malformed APK signing credential envelope");

			// Derive a key from the master password and decrypt the payload.
			keySpec = new PBEKeySpec(masterPassword, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
			derivedKey = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(keySpec).getEncoded();
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(derivedKey, "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
			cipher.updateAAD(AAD);
			payload = cipher.doFinal(ciphertext);
			if (payload.length < 8)
				throw new IOException("Malformed decrypted APK signing credentials");

			// Payload must be the right size.
			//  - 4: Length of the keystore password
			//  - N: Keystore password bytes
			//  - 4: Length of the private-key password
			//  - M: Private-key password bytes
			ByteBuffer buffer = ByteBuffer.wrap(payload);
			int storeLength = buffer.getInt();
			if (storeLength < 0 || storeLength > buffer.remaining() - 4)
				throw new IOException("Malformed decrypted APK signing credentials");

			// Read the store bytes.
			byte[] storeBytes = new byte[storeLength];
			buffer.get(storeBytes);
			int keyLength = buffer.getInt();
			if (keyLength < 0 || keyLength != buffer.remaining()) {
				clear(storeBytes);
				throw new IOException("Malformed decrypted APK signing credentials");
			}

			// Read the key bytes, and decode both the store and key passwords.
			byte[] keyBytes = new byte[keyLength];
			buffer.get(keyBytes);
			try {
				storePassword = decode(storeBytes);
				keyPassword = decode(keyBytes);
			} finally {
				clear(storeBytes);
				clear(keyBytes);
			}

			// Wrap up and clear.
			Credentials credentials = new Credentials(storePassword, keyPassword);
			clear(storePassword);
			clear(keyPassword);
			storePassword = null;
			keyPassword = null;
			return credentials;
		} catch (AEADBadTagException ex) {
			throw new IOException("APK signing credential authentication failed", ex);
		} catch (Exception ex) {
			throw new IOException("Malformed or invalid APK signing credential envelope", ex);
		} finally {
			if (keySpec != null)
				keySpec.clearPassword();
			clear(salt);
			clear(nonce);
			clear(ciphertext);
			clear(derivedKey);
			clear(payload);
			clear(storePassword);
			clear(keyPassword);
		}
	}

	/**
	 * Decodes a Base64-URL part and validates that it matches the original.
	 *
	 * @param part
	 * 		Base64-URL part to decode.
	 *
	 * @return Decoded bytes.
	 *
	 * @throws IOException
	 * 		When the part is invalid or does not match the original.
	 */
	@Nonnull
	private static byte[] decodePart(@Nonnull String part) throws IOException {
		try {
			byte[] decoded = DECODER.decode(part);
			if (!ENCODER.encodeToString(decoded).equals(part))
				throw new IOException("Decoded part does not match original");
			return decoded;
		} catch (IllegalArgumentException ex) {
			throw new IOException("Invalid Base64-URL part in APK signing credential envelope", ex);
		}
	}

	/**
	 * Encodes a character array into bytes using UTF-8.
	 *
	 * @param chars
	 * 		Characters to encode.
	 *
	 * @return Encoded bytes.
	 *
	 * @throws CharacterCodingException
	 * 		When the characters cannot be encoded.
	 */
	@Nonnull
	private static byte[] encode(@Nonnull char[] chars) throws CharacterCodingException {
		ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder()
				.onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT)
				.encode(CharBuffer.wrap(chars));
		byte[] bytes = new byte[encoded.remaining()];
		encoded.get(bytes);
		if (encoded.hasArray())
			Arrays.fill(encoded.array(), (byte) 0);
		return bytes;
	}

	/**
	 * Decodes bytes into a character array using UTF-8.
	 *
	 * @param bytes
	 * 		Bytes to decode.
	 *
	 * @return Decoded characters.
	 *
	 * @throws CharacterCodingException
	 * 		When the bytes cannot be decoded.
	 */
	@Nonnull
	private static char[] decode(@Nonnull byte[] bytes) throws CharacterCodingException {
		CharBuffer decoded = StandardCharsets.UTF_8.newDecoder()
				.onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT)
				.decode(ByteBuffer.wrap(bytes));
		char[] chars = new char[decoded.remaining()];
		decoded.get(chars);
		if (decoded.hasArray())
			Arrays.fill(decoded.array(), '\0');
		return chars;
	}

	private static boolean isBlank(@Nonnull char[] value) {
		for (char c : value) {
			if (!Character.isWhitespace(c))
				return false;
		}
		return true;
	}

	private static void clear(@Nullable byte[] value) {
		if (value != null)
			Arrays.fill(value, (byte) 0);
	}

	private static void clear(@Nullable char[] value) {
		if (value != null)
			Arrays.fill(value, '\0');
	}

	/**
	 * Decrypted keystore credentials.
	 */
	public static final class Credentials implements AutoCloseable {
		private final char[] storePassword;
		private final char[] keyPassword;
		private boolean closed;

		private Credentials(char[] storePassword, char[] keyPassword) {
			this.storePassword = storePassword.clone();
			this.keyPassword = keyPassword.clone();
		}

		/**
		 * @return Owned keystore password array, cleared when this object is closed.
		 */
		@Nonnull
		public char[] storePassword() {
			return storePassword;
		}

		/**
		 * @return Owned private-key password array, cleared when this object is closed.
		 */
		@Nonnull
		public char[] keyPassword() {
			return keyPassword;
		}

		@Override
		public void close() {
			if (!closed) {
				clear(storePassword);
				clear(keyPassword);
				closed = true;
			}
		}
	}
}
