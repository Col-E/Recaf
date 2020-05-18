package me.coley.recaf.compiler;

import me.coley.recaf.Recaf;

import javax.tools.*;
import javax.tools.JavaFileObject.Kind;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * In memory java-to-bytecode compiler.
 *
 * @author Matt
 */
public class JavacCompiler {
	private List<String> pathItems;
	private final Map<String, VirtualJavaFileObject> unitMap = new HashMap<>();
	private final Options options = new Options();
	private DiagnosticListener<VirtualJavaFileObject> listener;

	/**
	 * @return Success of compilation. Use {@link #setCompileListener(DiagnosticListener)}
	 * to receive information about failures.
	 */
	@SuppressWarnings("unchecked")
	public boolean compile() {
		JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
		if (javac == null)
			throw new IllegalStateException("No Java compiler is installed. Please use a JDK context when running.");
		// file manager, used so that the unit map can have their definitions updated
		// after compilation.
		DiagnosticListener<? super JavaFileObject> lll = (DiagnosticListener<? super JavaFileObject>) (Object) listener;
		JavaFileManager fmFallback = javac.getStandardFileManager(lll, Locale.getDefault(), UTF_8);
		JavaFileManager fm = new VirtualFileManager(fmFallback);
		// Add options
		List<String> args = new ArrayList<>();
		if(pathItems != null && pathItems.size() > 0)
			args.addAll(Arrays.asList("-classpath", getClassPathText()));
		args.addAll(Arrays.asList("-source", this.options.getTarget().toString()));
		args.addAll(Arrays.asList("-target", this.options.getTarget().toString()));
		args.add(this.options.toOption());
		// create task
		try {
			JavaCompiler.CompilationTask task = javac.getTask(null, fm, lll, args, null, unitMap.values());
			return task.call();
		} catch(RuntimeException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * @return Generated classpath.
	 */
	private String getClassPathText() {
		// ensure the default path is included
		String pathDefault = System.getProperty("java.class.path");
		StringBuilder sb = new StringBuilder(pathDefault);
		// add extra dependencies
		for(String path : pathItems)
			sb.append(";").append(path);
		for (Path path : getCompilerClassspathDirectory())
			sb.append(";").append(path.toFile().getAbsolutePath());
		return sb.toString();
	}

	/**
	 * @return Directory to contain additional classpath items.
	 */
	public static Path getCompilerClassspathDirectory() {
		return Recaf.getDirectory("classpath").resolve("compiler");
	}

	/**
	 * Add class to compilation process.
	 *
	 * @param className
	 * 		Name of class to compile.
	 * @param content
	 * 		Source code of class.
	 */
	public void addUnit(String className, String content) {
		unitMap.put(className, new VirtualJavaFileObject(className, content));
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return Bytecode of class.
	 */
	public byte[] getUnitCode(String name) {
		VirtualJavaFileObject file = unitMap.get(name);
		if(file == null)
			return null;
		return file.getBytecode();
	}

	/**
	 * @return Map of class names to bytecode.
	 */
	public Map<String, byte[]> getUnits() {
		return unitMap.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().getBytecode()));
	}

	/**
	 * @return Compiler options.
	 */
	public Options options() {
		return options;
	}

	/**
	 * @param pathItems
	 * 		Items to use for the classpath in compilation.
	 */
	public void setClassPath(List<String> pathItems) {
		this.pathItems = pathItems;
	}

	/**
	 * @param listener
	 * 		Listener that receives compiler error information.
	 */
	public void setCompileListener(DiagnosticListener<VirtualJavaFileObject> listener) {
		this.listener = listener;
	}

	/**
	 * File manager extension for handling updates to java file object's output stream.
	 * Additionally, registers inner classes as new files.
	 */
	private final class VirtualFileManager extends ForwardingJavaFileManager<JavaFileManager> {
		private VirtualFileManager(JavaFileManager fallback) {
			super(fallback);
		}

		@Override
		public JavaFileObject getJavaFileForOutput(Location location, String name, Kind
				kind, FileObject sibling) throws IOException {
			String internal = name.replace('.', '/');
			VirtualJavaFileObject obj = unitMap.get(internal);
			// Unknown class, assumed to be an inner class.
			// add to the unit map so it can be fetched.
			if(obj == null) {
				obj = new VirtualJavaFileObject(internal, null);
				unitMap.put(internal, obj);
			}
			return obj;
		}
	}
}