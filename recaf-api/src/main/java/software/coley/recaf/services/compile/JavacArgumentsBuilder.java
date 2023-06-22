package software.coley.recaf.services.compile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.util.JavaVersion;

/**
 * Builder for {@link JavacArguments}.
 *
 * @author Matt Coley
 */
public final class JavacArgumentsBuilder {
	private String className;
	private String classSource;
	private String classPath = System.getProperty("java.class.path");
	private int versionTarget = JavaVersion.get();
	private boolean debugVariables = true;
	private boolean debugLineNumbers = true;
	private boolean debugSourceName = true;

	/**
	 * @param className
	 * 		Internal name of the class being compiled.
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
	 * 		Source of the class.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public JavacArgumentsBuilder withClassSource(@Nonnull String classSource) {
		this.classSource = classSource;
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
	 * @param versionTarget
	 * 		Java bytecode version to target.
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
			throw new IllegalArgumentException("Class name must not be null");
		if (classSource == null)
			throw new IllegalArgumentException("Class source must not be null");

		return new JavacArguments(className, classSource,
				classPath, versionTarget,
				debugVariables, debugLineNumbers, debugSourceName);
	}
}
