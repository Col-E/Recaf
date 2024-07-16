package software.coley.recaf.services.script;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import regexodus.Matcher;
import software.coley.recaf.analytics.logging.DebuggingLogger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.compile.*;
import software.coley.recaf.services.plugin.CdiClassAllocator;
import software.coley.recaf.util.ClassDefiner;
import software.coley.recaf.util.ReflectUtil;
import software.coley.recaf.util.RegexUtil;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.threading.ThreadPoolFactory;

import java.lang.reflect.Method;
import java.util.*;
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
	private static final String SCRIPT_PACKAGE_NAME = "software.coley.recaf.generated";
	private static final String PATTERN_PACKAGE = "package ([\\w\\.\\*]+);?";
	private static final String PATTERN_IMPORT = "import ([\\w\\.\\*]+);?";
	private static final String PATTERN_CLASS_NAME = "(?<=class)\\s+(\\w+)\\s+(?:implements|extends|\\{)";
	private static final List<String> DEFAULT_IMPORTS = Arrays.asList(
			"java.io.*",
			"java.nio.file.*",
			"java.util.*",
			"software.coley.recaf.*",
			"software.coley.recaf.analytics.logging.*",
			"software.coley.recaf.info.*",
			"software.coley.recaf.info.annotation.*",
			"software.coley.recaf.info.builder.*",
			"software.coley.recaf.info.member.*",
			"software.coley.recaf.info.properties.*",
			"software.coley.recaf.services.*",
			// "software.coley.recaf.services.assemble.*",
			"software.coley.recaf.services.attach.*",
			"software.coley.recaf.services.callgraph.*",
			"software.coley.recaf.services.compile.*",
			"software.coley.recaf.services.config.*",
			"software.coley.recaf.services.decompile.*",
			"software.coley.recaf.services.file.*",
			"software.coley.recaf.services.inheritance.*",
			"software.coley.recaf.services.mapping.*",
			"software.coley.recaf.services.plugin.*",
			"software.coley.recaf.services.script.*",
			"software.coley.recaf.services.search.*",
			"software.coley.recaf.services.workspace.*",
			"software.coley.recaf.services.workspace.io.*",
			"software.coley.recaf.workspace.model.*",
			"software.coley.recaf.workspace.model.bundle.*",
			// "software.coley.recaf.services.ssvm.*",
			"software.coley.recaf.util.*",
			"software.coley.recaf.util.android.*",
			"software.coley.recaf.util.io.*",
			"software.coley.recaf.util.threading.*",
			"software.coley.recaf.util.visitors.*",
			"org.objectweb.asm.*",
			"org.objectweb.asm.tree.*",
			"jakarta.annotation.*",
			"jakarta.enterprise.context.*",
			"jakarta.inject.*",
			"org.slf4j.Logger"
	);
	private final Map<Integer, GenerateResult> generateResultMap = new HashMap<>();
	private final ExecutorService compileAndRunPool = ThreadPoolFactory.newSingleThreadExecutor("script-loader");
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
		return CompletableFuture.supplyAsync(() -> handleExecute(script), compileAndRunPool);
	}

	@Nonnull
	@Override
	public CompletableFuture<GenerateResult> compile(@Nonnull String scriptSource) {
		return CompletableFuture.supplyAsync(() -> generate(scriptSource), compileAndRunPool);
	}

	/**
	 * Compiles and executes the script.
	 * If the same script has already been compiled previously, the prior class reference will be used
	 * to reduce duplicate compilations.
	 *
	 * @param script
	 * 		Script to execute.
	 *
	 * @return Result of script execution.
	 */
	@Nonnull
	private ScriptResult handleExecute(@Nonnull String script) {
		GenerateResult result = generate(script);
		if (result.cls() != null) {
			try {
				logger.debugging(l -> l.info("Allocating script instance"));
				Object instance = allocator.instance(result.cls());
				Method run = ReflectUtil.getDeclaredMethod(instance.getClass(), "run");
				run.setAccessible(true);
				run.invoke(instance);
				logger.debugging(l -> l.info("Successfully ran script"));
				return new ScriptResult(result.diagnostics());
			} catch (Exception ex) {
				logger.error("Failed to execute script", ex);
				return new ScriptResult(result.diagnostics(), ex);
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
	private GenerateResult generate(@Nonnull String script) {
		int hash = script.hashCode();
		GenerateResult result;
		if (RegexUtil.matchesAny(PATTERN_CLASS_NAME, script)) {
			logger.debugging(l -> l.info("Compiling script as class"));
			result = generateResultMap.computeIfAbsent(hash, n -> generateStandardClass(script));
		} else {
			logger.debugging(l -> l.info("Compiling script as function"));
			String className = "Script" + Math.abs(hash);
			result = generateResultMap.computeIfAbsent(hash, n -> generateScriptClass(className, script));
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
	private GenerateResult generateStandardClass(@Nonnull String source) {
		String originalSource = source;

		// Extract package name
		String packageName = SCRIPT_PACKAGE_NAME;
		Matcher matcher = RegexUtil.getMatcher(PATTERN_PACKAGE, source);
		if (matcher.find())
			packageName = matcher.group(1);
		else
			source = "package " + packageName + ";\n" + source;

		// Add default imports
		String imports = "\nimport " + String.join(";\nimport ", DEFAULT_IMPORTS) + ";\n";
		source = StringUtil.insert(source, source.indexOf(packageName + ";") + packageName.length() + 1, imports);

		// Normalize package name
		packageName = packageName.replace('.', '/');

		// Extract class name
		String className;
		matcher = RegexUtil.getMatcher(PATTERN_CLASS_NAME, source);
		if (matcher.find()) {
			String originalName = matcher.group(1);
			String modifiedName = originalName + Math.abs(source.hashCode());
			className = packageName + "/" + modifiedName;

			// Replace name in script
			//  - Class definition
			//  - Constructors
			source = StringUtil.replaceRange(source, matcher.start(1), matcher.end(1), modifiedName);
			source = source.replace(" " + originalName + "(", " " + modifiedName + "(");
			source = source.replace("\t" + originalName + "(", "\t" + modifiedName + "(");
		} else {
			return new GenerateResult(null, List.of(
					new CompilerDiagnostic(-1, -1, 0,
							"Could not determine name of class", CompilerDiagnostic.Level.ERROR)));
		}

		// Compile the class
		return generate(className, originalSource, source);
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
	 * @return Compiler result wrapper containing the loaded class reference.
	 */
	@Nonnull
	private GenerateResult generateScriptClass(@Nonnull String className, @Nonnull String script) {
		String originalSource = script;
		Set<String> imports = new HashSet<>(DEFAULT_IMPORTS);
		Matcher matcher = RegexUtil.getMatcher(PATTERN_IMPORT, script);
		while (matcher.find()) {
			// Record import statement
			String importIdentifier = matcher.group(1);
			imports.add(importIdentifier);

			// Replace text with spaces to maintain script character offsets
			String importMatch = script.substring(matcher.start(), matcher.end());
			script = script.replace(importMatch, " ".repeat(importMatch.length()));
		}

		// Create code (just a basic class with a static 'run' method)
		StringBuilder code = new StringBuilder(
				"@Dependent public class " + className + " implements Runnable, Opcodes { " +
						"private static final Logger log = Logging.get(\"script\"); " +
						"Workspace workspace; " +
						"@Inject " + className +"(Workspace workspace) { this.workspace = workspace; } " +
						"public void run() {\n" + script + "\n" + "}" +
						"}");
		for (String imp : imports)
			code.insert(0, "import " + imp + "; ");
		code.insert(0, "package " + SCRIPT_PACKAGE_NAME + "; ");
		className = SCRIPT_PACKAGE_NAME.replace('.', '/') + "/" + className;

		// Compile the class
		return generate(className, originalSource, code.toString());
	}

	/**
	 * @param className
	 * 		Name of the script class.
	 * @param originalSource
	 * 		Original source provided by the user.
	 * @param compileSource
	 * 		Full source of the script to pass to the compiler.
	 *
	 * @return Compiler result wrapper containing the loaded class reference.
	 */
	@Nonnull
	private GenerateResult generate(@Nonnull String className,
									@Nonnull String originalSource,
									@Nonnull String compileSource) {
		JavacArguments args = new JavacArgumentsBuilder()
				.withClassName(className)
				.withClassSource(compileSource)
				.build();
		CompilerResult result = compiler.compile(args, null, null);
		if (result.wasSuccess()) {
			try {
				Map<String, byte[]> classes = result.getCompilations().entrySet().stream()
						.collect(Collectors.toMap(e -> e.getKey().replace('/', '.'), Map.Entry::getValue));
				ClassDefiner definer = new ClassDefiner(classes);
				Class<?> cls = definer.findClass(className.replace('/', '.'));
				return new GenerateResult(cls, mapDiagnostics(originalSource, compileSource, result.getDiagnostics()));
			} catch (Exception ex) {
				logger.error("Failed to define generated script class", ex);
			}
		}
		return new GenerateResult(null, mapDiagnostics(originalSource, compileSource, result.getDiagnostics()));
	}

	/**
	 * @param originalSource
	 * 		Original source provided by the user.
	 * @param compileSource
	 * 		Full source of the script to pass to the compiler.
	 * @param diagnostics
	 * 		Diagnostics to map position of.
	 *
	 * @return List of updated diagnostics.
	 */
	private List<CompilerDiagnostic> mapDiagnostics(@Nonnull String originalSource,
													@Nonnull String compileSource,
													@Nonnull List<CompilerDiagnostic> diagnostics) {

		int syntheticLineCount = StringUtil.count("\n", StringUtil.cutOffAtFirst(compileSource, originalSource));
		return diagnostics.stream()
				.map(d -> d.withLine(d.line() - syntheticLineCount))
				.toList();
	}
}
