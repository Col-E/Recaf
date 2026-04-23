package software.coley.recaf.services.script;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.services.compile.CompilerDiagnostic;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Wrapper for results of {@link ScriptEngine#compile(String)} calls.
 *
 * @param cls
 * 		Compiled class reference. May be {@code null} when compilation failed.
 * @param diagnostics
 * 		Compiler diagnostic messages.
 *
 * @author Matt Coley
 */
public record GenerateResult(@Nullable Class<?> cls, @Nonnull List<CompilerDiagnostic> diagnostics) {
	/**
	 * @return {@code true} when compilation was a success.
	 */
	public boolean wasSuccess() {
		return cls != null;
	}

	/**
	 * Attempts to stop the script. If the generation failed, this method will do nothing.
	 * <p>
	 * Cancellation is scoped to the generated class loader that owns {@link #cls()}.
	 * Callers that wish to stop running scripts must use {@link ScriptEngine#run(GenerateResult)}
	 * and track the returned instance for stopping.
	 *
	 * @throws IllegalStateException
	 * 		If something went wrong.
	 */
	public void requestStop() {
		invokeCancellationSingleton("stop");
	}

	/**
	 * Clears any prior request to stop the script. If the generation failed, this method will do nothing.
	 *
	 * @throws IllegalStateException
	 * 		If something went wrong.
	 */
	public void resetStop() {
		invokeCancellationSingleton("reset");
	}

	private void invokeCancellationSingleton(@Nonnull String methodName) {
		Class<?> cls = this.cls;
		if (cls == null)
			return;
		try {
			Class<?> cancellationSingleton = cls.getClassLoader().loadClass(CancellationSingleton.class.getName());
			cancellationSingleton.getDeclaredMethod(methodName).invoke(null);
		} catch (InvocationTargetException ex) {
			throw new IllegalStateException(ex.getTargetException());
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}
}
