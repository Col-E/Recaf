package software.coley.recaf.services.compile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.workspace.model.Workspace;

import java.util.Objects;

/**
 * Arguments to pass to {@link JavacCompiler#compile(JavacArguments, Workspace, JavacListener)}.
 *
 * @author Matt Coley
 * @see JavacArgumentsBuilder
 */
public class JavacArguments {
	// Primary inputs
	private final String className;
	private final String classSource;
	// Options
	private final String classPath;
	private final int versionTarget;
	private final boolean debugVariables;
	private final boolean debugLineNumbers;
	private final boolean debugSourceName;

	/**
	 * @param className
	 * 		Internal name of the class being compiled.
	 * @param classSource
	 * 		Source of the class.
	 * @param classPath
	 * 		Classpath to use with compiler.
	 * @param versionTarget
	 * 		Java bytecode version to target.
	 * @param debugVariables
	 * 		Debug flag to include variable info.
	 * @param debugLineNumbers
	 * 		Debug flag to include line number info.
	 * @param debugSourceName
	 * 		Debug flag to include source file name.
	 */
	public JavacArguments(@Nonnull String className, @Nonnull String classSource,
						  @Nullable String classPath, int versionTarget,
						  boolean debugVariables, boolean debugLineNumbers, boolean debugSourceName) {
		this.className = className;
		this.classSource = classSource;
		this.classPath = classPath;
		this.versionTarget = versionTarget;
		this.debugVariables = debugVariables;
		this.debugLineNumbers = debugLineNumbers;
		this.debugSourceName = debugSourceName;
	}

	/**
	 * @return String representation of debug flags.
	 */
	@Nonnull
	public String createDebugValue() {
		StringBuilder s = new StringBuilder();
		if (debugVariables)
			s.append("vars,");
		if (debugLineNumbers)
			s.append("lines,");
		if (debugSourceName)
			s.append("source");

		// edge case
		if (s.length() == 0)
			return "-g:none";

		// Substring off dangling comma
		String value = s.toString();
		if (value.endsWith(","))
			value = s.substring(0, value.length() - 1);
		return "-g:" + value;
	}

	/**
	 * @return Internal name of the class being compiled.
	 */
	@Nonnull
	public String getClassName() {
		return className;
	}

	/**
	 * @return Source of the class.
	 */
	@Nonnull
	public String getClassSource() {
		return classSource;
	}

	/**
	 * @return Classpath to use with compiler.
	 */
	@Nullable
	public String getClassPath() {
		return classPath;
	}

	/**
	 * @return Java bytecode version to target.
	 */
	public int getVersionTarget() {
		return versionTarget;
	}

	/**
	 * @return Debug flag to include variable info.
	 */
	public boolean isDebugVariables() {
		return debugVariables;
	}

	/**
	 * @return Debug flag to include line number info.
	 */
	public boolean isDebugLineNumbers() {
		return debugLineNumbers;
	}

	/**
	 * @return Debug flag to include source file name.
	 */
	public boolean isDebugSourceName() {
		return debugSourceName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		JavacArguments other = (JavacArguments) o;

		if (versionTarget != other.versionTarget) return false;
		if (debugVariables != other.debugVariables) return false;
		if (debugLineNumbers != other.debugLineNumbers) return false;
		if (debugSourceName != other.debugSourceName) return false;
		if (!className.equals(other.className)) return false;
		if (!classSource.equals(other.classSource)) return false;
		return Objects.equals(classPath, other.classPath);
	}

	@Override
	public int hashCode() {
		int result = className.hashCode();
		result = 31 * result + classSource.hashCode();
		result = 31 * result + (classPath != null ? classPath.hashCode() : 0);
		result = 31 * result + versionTarget;
		result = 31 * result + (debugVariables ? 1 : 0);
		result = 31 * result + (debugLineNumbers ? 1 : 0);
		result = 31 * result + (debugSourceName ? 1 : 0);
		return result;
	}
}
