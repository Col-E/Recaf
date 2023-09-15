package software.coley.recaf.services.decompile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.properties.builtin.CachedDecompileProperty;

import java.util.Objects;

/**
 * Result for a {@link Decompiler} output.
 *
 * @author Matt Coley
 */
public class DecompileResult {
	private final String text;
	private final Throwable exception;
	private final ResultType type;
	private final int configHash;

	/**
	 * @param text
	 * 		Decompiled text.
	 * @param exception
	 * 		Failure reason.
	 * @param type
	 * 		Result type.
	 * @param configHash
	 * 		Value of {@link DecompilerConfig#getConfigHash()} of associated decompiler.
	 * 		Used to determine if cached value in {@link CachedDecompileProperty} is up-to-date with current config.
	 */
	public DecompileResult(@Nullable String text, @Nullable Throwable exception, @Nonnull ResultType type, int configHash) {
		this.text = text;
		this.exception = exception;
		this.type = type;
		this.configHash = configHash;
	}

	/**
	 * @return Decompiled text.
	 * May be {@code null} when {@link #getType()} is not {@link ResultType#SUCCESS}.
	 */
	@Nullable
	public String getText() {
		return text;
	}

	/**
	 * @return Failure reason.
	 * May be {@code null} when {@link #getType()} is not {@link ResultType#FAILURE}.
	 */
	@Nullable
	public Throwable getException() {
		return exception;
	}

	/**
	 * @return Result type.
	 */
	@Nonnull
	public ResultType getType() {
		return type;
	}

	/**
	 * @return Value of {@link DecompilerConfig#getConfigHash()} of associated decompiler.
	 * Used to determine if cached value in {@link CachedDecompileProperty} is up-to-date with current config.
	 */
	public int getConfigHash() {
		return configHash;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		DecompileResult that = (DecompileResult) o;

		if (configHash != that.configHash) return false;
		if (!Objects.equals(text, that.text)) return false;
		if (!Objects.equals(exception, that.exception)) return false;
		return type == that.type;
	}

	@Override
	public int hashCode() {
		int result = text != null ? text.hashCode() : 0;
		result = 31 * result + (exception != null ? exception.hashCode() : 0);
		result = 31 * result + type.hashCode();
		result = 31 * result + configHash;
		return result;
	}

	/**
	 * Type of result.
	 */
	public enum ResultType {
		/**
		 * Successful decompilation.
		 */
		SUCCESS,
		/**
		 * Decompilation skipped for some reason. Likely due to a thread being cancelled.
		 */
		SKIPPED,
		/**
		 * Decompilation failed to emit any output.
		 */
		FAILURE
	}
}
