package software.coley.recaf.services.script;

import jakarta.annotation.Nonnull;
import software.coley.recaf.services.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Outline for script execution and compilation.
 *
 * @author Matt Coley
 */
public interface ScriptEngine extends Service {
	String SERVICE_ID = "script-engine";

	/**
	 * @param scriptSource
	 * 		Script source to execute.
	 *
	 * @return Future of script execution.
	 */
	@Nonnull
	CompletableFuture<ScriptResult> run(String scriptSource);

	/**
	 * @param scriptSource
	 * 		Script source to compile.
	 *
	 * @return Future of script compilation.
	 */
	@Nonnull
	CompletableFuture<GenerateResult> compile(String scriptSource);
}
