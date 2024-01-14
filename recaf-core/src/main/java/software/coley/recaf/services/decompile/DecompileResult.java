package software.coley.recaf.services.decompile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.properties.builtin.CachedDecompileProperty;
import software.coley.recaf.util.StringUtil;

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
	 * Constructor for a successful decompilation.
	 *
	 * @param text
	 * 		Decompiled text.
	 * @param configHash
	 * 		Value of {@link DecompilerConfig#getHash()} of associated decompiler.
	 * 		Used to determine if cached value in {@link CachedDecompileProperty} is up-to-date with current config.
	 */
	public DecompileResult(@Nonnull String text, int configHash) {
		this.text = text;
		this.type = ResultType.SUCCESS;
		this.configHash = configHash;
		this.exception = null;
	}

	/**
	 * Constructor for a failed decompilation.
	 *
	 * @param exception
	 * 		Failure reason.
	 * @param configHash
	 * 		Value of {@link DecompilerConfig#getHash()} of associated decompiler.
	 * 		Used to determine if cached value in {@link CachedDecompileProperty} is up-to-date with current config.
	 */
	public DecompileResult(@Nonnull Throwable exception, int configHash) {
		this.text = "// " + StringUtil.traceToString(exception).replace("\n", "\n// ");
		this.type = ResultType.FAILURE;
		this.configHash = configHash;
		this.exception = exception;
	}

	/**
	 * Constructor for a skipped decompilation.
	 *
	 * @param configHash
	 * 		Value of {@link DecompilerConfig#getHash()} of associated decompiler.
	 * 		Used to determine if cached value in {@link CachedDecompileProperty} is up-to-date with current config.
	 */
	public DecompileResult(int configHash) {
		this.text = null;
		this.type = ResultType.SKIPPED;
		this.configHash = configHash;
		this.exception = null;
	}

	/**
	 * Constructor for a skipped decompilation, with pre-defined text.
	 * Typically used for displaying feedback if the decompiler had an issue or timed out.
	 *
	 * @param text
	 * 		Decompiled text.
	*/
	public DecompileResult(@Nonnull String text) {
		this.text = text;
		this.type = ResultType.SKIPPED;
		this.configHash = 0;
		this.exception = null;
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
	 * @return Value of {@link DecompilerConfig#getHash()} of associated decompiler.
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
