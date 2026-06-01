package software.coley.recaf.services.script;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import software.coley.recaf.analytics.logging.DebuggingLogger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.compile.CompilerDiagnostic;
import software.coley.recaf.services.compile.CompilerResult;
import software.coley.recaf.services.compile.JavacArguments;
import software.coley.recaf.services.compile.JavacArgumentsBuilder;
import software.coley.recaf.services.compile.JavacCompiler;
import software.coley.recaf.services.plugin.CdiClassAllocator;
import software.coley.recaf.util.CancelSignal;
import software.coley.recaf.util.ReflectUtil;
import software.coley.recaf.util.threading.ThreadPoolFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Basic implementation of {@link ScriptEngine} using {@link JavacCompiler}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class JavacScriptEngine implements ScriptEngine {
	private static final DebuggingLogger logger = Logging.get(JavacScriptEngine.class);
	private final Map<Integer, GenerateResult> scriptResultMap = new HashMap<>();
	private final ExecutorService compilePool = ThreadPoolFactory.newSingleThreadExecutor("script-compiler");
	private final ExecutorService runPool = ThreadPoolFactory.newCachedThreadPool("script-runner");
	private final JavacCompiler compiler;
	private final CdiClassAllocator allocator;
	private final ScriptEngineConfig config;

	@Inject
	public JavacScriptEngine(JavacCompiler compiler, CdiClassAllocator allocator, ScriptEngineConfig config) {
		this.compiler = compiler;
		this.allocator = allocator;
		this.config = config;
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public ScriptEngineConfig getServiceConfig() {
		return config;
	}

	@Nonnull
	@Override
	public CompletableFuture<ScriptResult> run(@Nonnull String script) {
		return compile(script).thenCompose(this::run);
	}

	@Nonnull
	@Override
	public CompletableFuture<ScriptResult> run(@Nonnull GenerateResult result) {
		return CompletableFuture.supplyAsync(() -> handleExecute(result), runPool);
	}

	@Nonnull
	@Override
	public CompletableFuture<GenerateResult> compile(@Nonnull String scriptSource) {
		return CompletableFuture.supplyAsync(() -> generate(scriptSource), compilePool);
	}

	/**
	 * Executes the generated script.
	 *
	 * @param result
	 * 		Compiled script to execute.
	 *
	 * @return Result of script execution.
	 */
	@Nonnull
	private ScriptResult handleExecute(@Nonnull GenerateResult result) {
		if (result.cls() != null) {
			try {
				logger.debugging(l -> l.info("Allocating script instance"));
				Object instance = allocator.instance(result.cls());
				Method run = ReflectUtil.getDeclaredMethod(instance.getClass(), "run");
				run.setAccessible(true);
				run.invoke(instance);
				logger.debugging(l -> l.info("Successfully ran script"));
				return new ScriptResult(result.diagnostics());
			} catch (InvocationTargetException ex) {
				Throwable target = ex.getTargetException();
				if (target instanceof CancelSignal) {
					logger.debugging(l -> l.info("Cancelled script"));
					return ScriptResult.cancelled(result.diagnostics());
				}
				logger.error("Failed to execute script", target);
				return new ScriptResult(result.diagnostics(), target);
			} catch (Exception ex) {
				logger.error("Failed to execute script", ex);
				return new ScriptResult(result.diagnostics(), ex);
			} finally {
				try {
					result.resetStop();
				} catch (IllegalStateException ex) {
					logger.error("Failed to reset script cancellation state", ex);
				}
			}
		} else {
			logger.error("Failed to compile script");
			return new ScriptResult(result.diagnostics());
		}
	}

	/**
	 * Maps an input script to a full Java source file, and compiles it.
	 * Delegates to either:
	 * <ul>
	 *     <li>{@link #generateScriptClass(String, String)}</li>
	 *     <li>{@link #generateStandardClass(String)}</li>
	 * </ul>
	 *
	 * @param script
	 * 		Initial source of the script.
	 *
	 * @return Compiler result wrapper containing the loaded class reference.
	 */
	@Nonnull
	private GenerateResult generate(@Nonnull String script) {
		int hash = script.hashCode();
		GenerateResult result;
		if (ScriptSourceAugmentation.isClassScript(script)) {
			logger.debugging(l -> l.info("Compiling script as class"));
			result = scriptResultMap.computeIfAbsent(hash, n -> generateStandardClass(script).generateResult());
		} else {
			logger.debugging(l -> l.info("Compiling script as function"));
			String className = "Script" + Math.abs(hash);
			result = scriptResultMap.computeIfAbsent(hash, n -> generateScriptClass(className, script).generateResult());
		}
		return result;
	}

	/**
	 * Used when the script contains a class definition in itself.
	 * Adds the default script package name, if no package is defined.
	 *
	 * @param source
	 * 		Initial source of the script.
	 *
	 * @return Compiler result wrapper containing the loaded class reference.
	 */
	@Nonnull
	private ScriptTemplate generateStandardClass(@Nonnull String source) {
		AugmentedSourceUnit prepared = ScriptSourceAugmentation.augmentStandardClass(source);
		if (prepared == null)
			return new ScriptTemplate.Failed(List.of(
					new CompilerDiagnostic(-1, -1, 0, "Could not determine name of class", CompilerDiagnostic.Level.ERROR)));
		return generate(prepared);
	}

	/**
	 * Used when the script immediately starts with the code.
	 * This will wrap that content in a basic class.
	 *
	 * @param className
	 * 		Name of the script class.
	 * @param script
	 * 		Initial source of the script.
	 *
	 * @return Script wrapper containing the loaded class reference.
	 */
	@Nonnull
	private ScriptTemplate generateScriptClass(@Nonnull String className, @Nonnull String script) {
		return generate(ScriptSourceAugmentation.augmentSnippetClass(className, script));
	}

	@Nonnull
	private ScriptTemplate generate(@Nonnull AugmentedSourceUnit src) {
		String className = src.className();
		String classSource = src.source().augmentedSource();
		JavacArguments args = new JavacArgumentsBuilder()
				.withClassName(className)
				.withClassSource(classSource)
				.build();
		CompilerResult result = compiler.compile(args, null, null);
		List<CompilerDiagnostic> diagnostics = mapDiagnostics(src.source(), result.getDiagnostics());
		if (result.wasSuccess()) {
			Map<String, byte[]> classes = result.getCompilations().entrySet().stream()
					.collect(Collectors.toMap(e -> e.getKey().replace('/', '.'), e -> postProcessClass(e.getValue())));
			injectClasses(classes);
			return new ScriptTemplate.Generated(className.replace('/', '.'), Map.copyOf(classes), diagnostics);
		}
		return new ScriptTemplate.Failed(diagnostics);
	}

	@Nonnull
	private byte[] postProcessClass(@Nonnull byte[] classBytes) {
		ClassReader cr = new ClassReader(classBytes);
		ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		cr.accept(new InsertCancelSignalVisitor(cw), ClassReader.EXPAND_FRAMES);
		return cw.toByteArray();
	}

	private void injectClasses(@Nonnull Map<String, byte[]> classMap) {
		for (Class<?> c : List.of(CancellationSingleton.class)) {
			try (InputStream in = c.getClassLoader().getResourceAsStream(Type.getInternalName(c) + ".class")) {
				classMap.put(c.getName(), in.readAllBytes());
			} catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}
	}

	/**
	 * @param augmentedSource
	 * 		Augmented source mapping for the original class-based script.
	 * @param diagnostics
	 * 		Diagnostics to map position of.
	 *
	 * @return List of updated diagnostics.
	 */
	private List<CompilerDiagnostic> mapDiagnostics(@Nonnull AugmentedSource augmentedSource,
	                                                @Nonnull List<CompilerDiagnostic> diagnostics) {
		return diagnostics.stream()
				.map(d -> d.withLine(augmentedSource.mapAugmentedLineToOriginal(d.line())))
				.toList();
	}
}
