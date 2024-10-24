package software.coley.recaf.services.compile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.collections.Lists;
import software.coley.recaf.analytics.logging.DebuggingLogger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.Service;
import software.coley.recaf.util.LookupUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Wrapper for {@link JavaCompiler}.
 * <br>
 * Worth note, the minimum supported version is declared in {@code com.sun.tools.javac.jvm.Target} but is marked
 * as an unstable API subject to change without notice. As of Java 17, the minimum target version is Java 7.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class JavacCompiler implements Service {
	public static final String SERVICE_ID = "java-compiler";
	public static final int MIN_DOWNSAMPLE_VER = 8;
	private static final DebuggingLogger logger = Logging.get(JavacCompiler.class);
	private static final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
	private static int minTargetVersion = 7;
	private final JavacCompilerConfig config;

	@Inject
	public JavacCompiler(JavacCompilerConfig config) {
		this.config = config;
	}

	/**
	 * @param arguments
	 * 		Wrapper of all arguments.
	 * @param workspace
	 * 		Optional workspace to include for additional classpath support.
	 * @param listener
	 * 		Optional listener to handle feedback with,
	 * 		mirroring what is reported by {@link CompilerResult#getDiagnostics()}
	 *
	 * @return Compilation result wrapper.
	 */
	@Nonnull
	public CompilerResult compile(@Nonnull JavacArguments arguments,
	                              @Nullable Workspace workspace,
	                              @Nullable JavacListener listener) {
		return compile(arguments, workspace, null, listener);
	}

	/**
	 * @param arguments
	 * 		Wrapper of all arguments.
	 * @param workspace
	 * 		Optional workspace to include for additional classpath support.
	 * @param supplementaryResources
	 * 		Optional resources to further extend the compilation classpath with.
	 * @param listener
	 * 		Optional listener to handle feedback with,
	 * 		mirroring what is reported by {@link CompilerResult#getDiagnostics()}
	 *
	 * @return Compilation result wrapper.
	 */
	@Nonnull
	public CompilerResult compile(@Nonnull JavacArguments arguments,
	                              @Nullable Workspace workspace,
	                              @Nullable List<WorkspaceResource> supplementaryResources,
	                              @Nullable JavacListener listener) {
		if (compiler == null)
			return new CompilerResult(new IllegalStateException("Cannot load 'javac' compiler."));

		String className = arguments.getClassName();

		// Class input map
		VirtualUnitMap unitMap = new VirtualUnitMap();
		unitMap.addSource(className, arguments.getClassSource());

		// Create a file manager to track files in-memory rather than on-disk
		List<WorkspaceResource> virtualClassPath = workspace == null ?
				Collections.emptyList() : workspace.getAllResources(true);
		if (supplementaryResources != null)
			virtualClassPath = Lists.combine(virtualClassPath, supplementaryResources);
		List<CompilerDiagnostic> diagnostics = new ArrayList<>();
		JavacListener listenerWrapper = createRecordingListener(listener, diagnostics);
		JavaFileManager fmFallback = compiler.getStandardFileManager(listenerWrapper, Locale.getDefault(), UTF_8);
		JavaFileManager fm = new VirtualFileManager(unitMap, virtualClassPath, fmFallback);

		// Populate arguments
		List<String> args = new ArrayList<>();

		// Classpath
		String cp = arguments.getClassPath();
		if (cp != null) {
			args.add("-classpath");
			args.add(cp);
			logger.debugging(l -> l.info("Compiler classpath: {}", cp));
		}

		// Target version
		int target = arguments.getVersionTarget();
		args.add("--release");
		args.add(Integer.toString(target));
		logger.debugging(l -> l.info("Compiler target: {}", target));

		// Debug info
		String debugArg = arguments.createDebugValue();
		args.add(debugArg);
		logger.debugging(l -> l.info("Compiler debug: {}", debugArg));

		// Invoke compiler
		try {
			JavaCompiler.CompilationTask task =
					compiler.getTask(null, fm, listenerWrapper, args, null, unitMap.getFiles());
			if (task.call()) {
				logger.debugging(l -> l.info("Compilation of '{}' finished", className));
			} else {
				logger.debugging(l -> l.error("Compilation of '{}' failed", className));
			}
			CompileMap compilations = unitMap.getCompilations();
			int downsampleTarget = arguments.getDownsampleTarget();
			if (downsampleTarget >= MIN_DOWNSAMPLE_VER)
				compilations.downsample(downsampleTarget);
			else if (downsampleTarget >= 0)
				logger.warn("Cannot downsample beyond Java {}", JavacCompiler.MIN_DOWNSAMPLE_VER);
			return new CompilerResult(compilations, diagnostics);
		} catch (RuntimeException ex) {
			logger.debugging(l -> l.error("Compilation of '{}' crashed: {}", className, ex));
			return new CompilerResult(ex);
		}
	}

	/**
	 * @return {@code true} when the compiler can be invoked.
	 */
	public static boolean isAvailable() {
		return compiler != null;
	}

	/**
	 * @return Minimum target version supported by the compiler.
	 */
	public static int getMinTargetVersion() {
		return minTargetVersion;
	}

	/**
	 * @param listener
	 * 		Optional listener to wrap.
	 * @param diagnostics
	 * 		List to add diagnostics to.
	 *
	 * @return Listener to encompass recording behavior and the user defined listener.
	 */
	private JavacListener createRecordingListener(@Nullable JavacListener listener,
	                                              @Nonnull List<CompilerDiagnostic> diagnostics) {
		return new ForwardingListener(listener) {
			@Override
			public void report(@Nonnull Diagnostic<? extends JavaFileObject> diagnostic) {
				// Pass to user defined listener
				super.report(diagnostic);

				// Record the diagnostic to our output
				if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
					diagnostics.add(new CompilerDiagnostic(
							(int) diagnostic.getLineNumber(),
							(int) diagnostic.getColumnNumber(),
							(int) diagnostic.getEndPosition() - (int) diagnostic.getPosition(),
							diagnostic.getMessage(Locale.getDefault()),
							mapKind(diagnostic.getKind())
					));
				}
			}

			private CompilerDiagnostic.Level mapKind(Diagnostic.Kind kind) {
				switch (kind) {
					case ERROR:
						return CompilerDiagnostic.Level.ERROR;
					case WARNING:
					case MANDATORY_WARNING:
						return CompilerDiagnostic.Level.WARNING;
					case NOTE:
					case OTHER:
					default:
						return CompilerDiagnostic.Level.INFO;
				}
			}
		};
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public JavacCompilerConfig getServiceConfig() {
		return config;
	}

	static {
		// Lookup oldest supported version
		try {
			MethodHandles.Lookup lookup = LookupUtil.lookup();
			Class<?> target = Class.forName("com.sun.tools.javac.jvm.Target");
			MethodHandle min = lookup.findStaticGetter(target, "MIN", target);
			Object minTarget = min.invoke();
			Field majorVersion = minTarget.getClass().getDeclaredField("majorVersion");
			majorVersion.setAccessible(true);
			minTargetVersion = majorVersion.getInt(minTarget) - JvmClassInfo.BASE_VERSION;
		} catch (Throwable ignored) {
			// Oh well...
		}
	}
}
