package software.coley.recaf.services.compile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.util.JavaVersion;

import java.util.HashMap;
import java.util.Map;

/**
 * Builder for {@link JavacArguments}.
 *
 * @author Matt Coley
 */
public final class JavacArgumentsBuilder {
	// Primary class to compile.
	private String className;
	private String classSource;
	// All classes to compile, keyed by internal name.
	// Does not need to contain the primary class, that will be added automatically if provided.
	private Map<String, String> classSources;
	// Additional compiler arguments.
	private String classPath = System.getProperty("java.class.path");
	private int versionTarget = JavaVersion.get();
	private int downsampleTarget = -1;
	private boolean debugVariables = true;
	private boolean debugLineNumbers = true;
	private boolean debugSourceName = true;

	/**
	 * @param className
	 * 		Internal name of the primary class being compiled.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public JavacArgumentsBuilder withClassName(@Nonnull String className) {
		this.className = className;
		return this;
	}

	/**
	 * @param classSource
	 * 		Source of the primary class to compile.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public JavacArgumentsBuilder withClassSource(@Nonnull String classSource) {
		this.classSource = classSource;
		return this;
	}

	/**
	 * @param classSources
	 * 		Sources of additional classes to compile, keyed by internal name.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public JavacArgumentsBuilder withClassSources(@Nonnull Map<String, String> classSources) {
		// Skip if no additional sources are provided.
		if (classSources.isEmpty())
			return this;

		// Add additional sources to the builder.
		if (this.classSources == null)
			this.classSources = new HashMap<>();
		this.classSources.putAll(classSources);

		// If the primary class is not set pick the first entry in the map as the primary class.
		if (className == null) {
			Map.Entry<String, String> entry = classSources.entrySet().iterator().next();
			return withClassName(entry.getKey())
					.withClassSource(entry.getValue());
		}

		return this;
	}

	/**
	 * @param classPath
	 * 		Classpath to use with compiler.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public JavacArgumentsBuilder withClassPath(@Nullable String classPath) {
		this.classPath = classPath;
		return this;
	}

	/**
	 * @param downsampleTarget
	 * 		Java version to target via down sampling. Negative to disable downs sampling.
	 * 		See: {@link JavacCompiler#MIN_DOWNSAMPLE_VER} for lowest supported target.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public JavacArgumentsBuilder withDownsampleTarget(int downsampleTarget) {
		this.downsampleTarget = downsampleTarget;
		return this;
	}

	/**
	 * @param versionTarget
	 * 		Java version to target.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public JavacArgumentsBuilder withVersionTarget(int versionTarget) {
		this.versionTarget = versionTarget;
		return this;
	}

	/**
	 * @param debugVariables
	 * 		Debug flag to include variable info.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public JavacArgumentsBuilder withDebugVariables(boolean debugVariables) {
		this.debugVariables = debugVariables;
		return this;
	}

	/**
	 * @param debugLineNumbers
	 * 		Debug flag to include line number info.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public JavacArgumentsBuilder withDebugLineNumbers(boolean debugLineNumbers) {
		this.debugLineNumbers = debugLineNumbers;
		return this;
	}

	/**
	 * @param debugSourceName
	 * 		Debug flag to include source file name.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public JavacArgumentsBuilder withDebugSourceName(boolean debugSourceName) {
		this.debugSourceName = debugSourceName;
		return this;
	}

	/**
	 * @return Arguments instance.
	 */
	@Nonnull
	public JavacArguments build() {
		if (className == null)
			throw new IllegalArgumentException("Primary class name must not be null");

		// Combine primary input with any additional sources.
		Map<String, String> allClassSources = classSources == null ? new HashMap<>() : new HashMap<>(classSources);
		if (classSource != null)
			allClassSources.put(className, classSource);

		// Validate that the primary class is present in the sources.
		if (allClassSources.isEmpty())
			throw new IllegalArgumentException("Class sources must not be empty");
		if (!allClassSources.containsKey(className))
			throw new IllegalArgumentException("Class sources must contain the primary class");

		return new JavacArguments(className, allClassSources,
				classPath, versionTarget, downsampleTarget,
				debugVariables, debugLineNumbers, debugSourceName);
	}
}
