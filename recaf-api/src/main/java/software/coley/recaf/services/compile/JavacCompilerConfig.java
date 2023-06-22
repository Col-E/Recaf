package software.coley.recaf.services.compile;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableBoolean;
import software.coley.observables.ObservableInteger;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;
import software.coley.recaf.workspace.model.Workspace;

import java.util.List;

/**
 * Config for {@link JavacCompiler}.
 * <br>
 * Not to be confused with {@link JavacArguments individual arguments} to be passed when invoking the compiler.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class JavacCompilerConfig extends BasicConfigContainer implements ServiceConfig {
	private final ObservableBoolean generatePhantoms = new ObservableBoolean(true);
	private final ObservableBoolean defaultEmitDebug = new ObservableBoolean(true);
	private final ObservableInteger defaultTargetVersion = new ObservableInteger(-1);

	@Inject
	public JavacCompilerConfig() {
		super(ConfigGroups.SERVICE_COMPILE, JavacCompiler.SERVICE_ID + CONFIG_SUFFIX);
		addValue(new BasicConfigValue<>("generate-phantoms", Boolean.class, generatePhantoms));
		addValue(new BasicConfigValue<>("default-emit-debug", Boolean.class, defaultEmitDebug));
		addValue(new BasicConfigValue<>("default-target-version", Integer.class, defaultTargetVersion));
	}

	/**
	 * Not enforced internally by {@link JavacCompiler}.
	 * Callers should check this value and ensure to call
	 * {@link JavacCompiler#compile(JavacArguments, Workspace, List, JavacListener)} with the list populated.
	 *
	 * @return {@code true} to enable phantom generation when calling {@code javac}.
	 */
	@Nonnull
	public ObservableBoolean getGeneratePhantoms() {
		return generatePhantoms;
	}

	/**
	 * Not enforced internally by {@link JavacCompiler}.
	 * Callers should check this value and ensure to call {@link JavacArgumentsBuilder#withDebugVariables(boolean)}
	 * and other debug methods.
	 *
	 * @return {@code true} to enable debug info by default.
	 */
	@Nonnull
	public ObservableBoolean getDefaultEmitDebug() {
		return defaultEmitDebug;
	}

	/**
	 * Not enforced internally by {@link JavacCompiler}.
	 * Callers should check this value and ensure to call {@link JavacArgumentsBuilder#withVersionTarget(int)}.
	 *
	 * @return Negative to match the input version of the class, otherwise target version
	 * <i>(In class file version format)</i>
	 */
	@Nonnull
	public ObservableInteger getDefaultTargetVersion() {
		return defaultTargetVersion;
	}
}
