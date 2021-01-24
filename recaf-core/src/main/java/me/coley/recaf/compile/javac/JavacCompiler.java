package me.coley.recaf.compile.javac;

import me.coley.recaf.compile.CompileOption;
import me.coley.recaf.compile.Compiler;
import me.coley.recaf.compile.CompilerDiagnostic;
import me.coley.recaf.compile.CompilerResult;
import me.coley.recaf.util.JavaVersion;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.Directories;
import org.slf4j.Logger;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Wrapper for {@link JavaCompiler}.
 *
 * @author Matt Coley
 */
public class JavacCompiler extends Compiler {
	private static final Logger logger = Logging.get(JavacCompiler.class);
	private static final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
	private JavacListener listener;

	/**
	 * New javac compiler wrapper.
	 */
	public JavacCompiler() {
		super("javac", System.getProperty("java.version"));
	}

	@Override
	public CompilerResult compile(String className, String classSource, Map<String, CompileOption<?>> options) {
		// Class input map
		VirtualUnitMap unitMap = new VirtualUnitMap();
		unitMap.addSource(className, classSource);
		// Create a file manager to track files in-memory rather than on-disk
		List<CompilerDiagnostic> errors = new ArrayList<>();
		JavacListener listenerWrapper = createRecordingListener(errors);
		JavaFileManager fmFallback = compiler.getStandardFileManager(listenerWrapper, Locale.getDefault(), UTF_8);
		JavaFileManager fm = new VirtualFileManager(unitMap, fmFallback);
		// Populate arguments
		//  - Classpath
		//  - Target bytecode level
		//  - Debug info
		List<String> args = new ArrayList<>();
		if (options.containsKey("classpath")) {
			String cp = options.get("classpath").getValue().toString();
			args.add("-classpath");
			args.add(cp);
			logger.debug("Compiler classpath: {}", cp);
		}
		if (options.containsKey("target")) {
			String target = options.get("target").getValue().toString();
			if (JavaVersion.get() > 8) {
				args.add("--release");
				args.add(target);
			} else {
				args.add("-source");
				args.add(target);
				args.add("-target");
				args.add(target);
			}
			logger.debug("Compiler target: {}", target);
		}
		if (options.containsKey("debug")) {
			String value = options.get("debug").getValue().toString();
			if (value.isEmpty())
				value = "none";
			args.add("-g:" + value);
			logger.debug("Compiler debug: {}", value);
		} else {
			args.add("-g:none");
			logger.debug("Compiler debug: none");
		}
		// Invoke compiler
		try {
			JavaCompiler.CompilationTask task =
					compiler.getTask(null, fm, listenerWrapper, args, null, unitMap.getFiles());
			if (task.call()) {
				logger.info("Compilation of '{}' finished", className);
				return new CompilerResult(this, unitMap.getCompilations());
			} else {
				logger.error("Compilation of '{}' failed", className);
				return new CompilerResult(this, errors);
			}
		} catch (RuntimeException ex) {
			logger.error("Compilation of '{}' crashed: {}", className, ex);
			return new CompilerResult(this, ex);
		}
	}

	@Override
	protected Map<String, CompileOption<?>> createDefaultOptions() {
		// TODO: Replace descriptions with translations
		//  - How do we want to handle translation usage in the core?
		//    Typically this would be a UI-only thing
		//  - Do we even want to support things like this?
		//    Would support here make the system less maintainable?
		Map<String, CompileOption<?>> options = new HashMap<>();
		options.put("target",
				new CompileOption<>(int.class, "--release", "target", "Target Java version", 8));
		options.put("debug",
				new CompileOption<>(String.class, "-g", "debug", "Debug information", "vars,lines,source"));
		options.put("classpath",
				new CompileOption<>(String.class, "-cp", "classpath", "Classpath", createClassPath()));
		return options;
	}

	/**
	 * @param errors
	 * 		List to add diagnostics to.
	 *
	 * @return Listener to encompass recording behavior and the user defined {@link #listener}.
	 */
	private JavacListener createRecordingListener(List<CompilerDiagnostic> errors) {
		return new ForwardingListener(listener) {
			@Override
			public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
				// pass to delegate
				super.report(diagnostic);
				// record
				if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
					errors.add(new CompilerDiagnostic((int) diagnostic.getLineNumber(),
							diagnostic.getMessage(Locale.getDefault())));
				}
			}
		};
	}

	/**
	 * @return Generated classpath.
	 */
	private String createClassPath() {
		// Ensure the default path is included
		String pathDefault = System.getProperty("java.class.path");
		StringBuilder sb = new StringBuilder(pathDefault);
		char separator = File.pathSeparatorChar;
		// Add classpath extension directory
		// Add phantoms directory
		try {
			Stream<Path> classpathJars = Files.walk(Directories.getClasspathDirectory());
			Stream<Path> phantomJars = Files.walk(Directories.getPhantomsDirectory());
			Stream.concat(classpathJars, phantomJars)
					.filter(p -> p.toString().toLowerCase().endsWith(".jar"))
					.forEach(p -> sb.append(separator).append(p.toAbsolutePath().toString()));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return sb.toString();
	}

	/**
	 * @param listener
	 * 		Listener that receives compiler error information.
	 */
	public void setCompileListener(JavacListener listener) {
		this.listener = listener;
	}
}
