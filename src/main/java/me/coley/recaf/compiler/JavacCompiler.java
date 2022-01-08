package me.coley.recaf.compiler;

import com.google.common.collect.Sets;
import me.coley.recaf.Recaf;
import me.coley.recaf.util.IOUtil;
import me.coley.recaf.util.Log;
import me.coley.recaf.util.Resource;
import me.coley.recaf.util.VMUtil;
import me.coley.recaf.workspace.JavaResource;

import javax.tools.*;
import javax.tools.JavaFileObject.Kind;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * In memory java-to-bytecode compiler.
 *
 * @author Matt
 */
public class JavacCompiler {
	private List<String> pathItems;
	private final Map<String, VirtualJavaFileObject> unitMap = new HashMap<>();
	private final JavacOptions options = new JavacOptions();
	private final List<JavaResource> classpath = new ArrayList<>();
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
		args.addAll(Arrays.asList("-classpath", getClassPathText()));
		if (VMUtil.getVmVersion() >= 9) {
			// For Java 9 and later, use release instead of the source/target pair
			args.addAll(Arrays.asList("--release", String.valueOf(this.options.getTarget().version())));
		} else {
			args.addAll(Arrays.asList("-source", this.options.getTarget().toString()));
			args.addAll(Arrays.asList("-target", this.options.getTarget().toString()));
		}
		args.add(this.options.toOption());
		// create task
		try {
			JavaCompiler.CompilationTask task = javac.getTask(null, fm, lll, args, null, unitMap.values());
			Boolean b = task.call();
			return b != null && b;
		} catch (RuntimeException e) {
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
		char separator = File.pathSeparatorChar;
		// add extra dependencies
		if (pathItems != null) {
			for (String path : pathItems)
				sb.append(separator).append(path);
		}

		try {
			Stream<Path> paths = Files.walk(getCompilerClasspathDirectory());
			paths.filter(p -> p.toString().toLowerCase().endsWith(".jar"))
					.filter(p -> p.toFile().length() < 10_000_000)
					.forEach(p -> sb.append(separator).append(IOUtil.toString(p)));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	/**
	 * @return Directory to contain additional classpath items.
	 */
	public static Path getCompilerClasspathDirectory() {
		Path path = Recaf.getDirectory("classpath").resolve("compiler");
		if (!Files.exists(path)) {
			try {
				Files.createDirectories(path);
			} catch (IOException ex) {
				// Shouldn't occur
				Log.error("Failed to create compiler ext directory", ex);
				throw new IllegalStateException(ex);
			}
		}
		return path;
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
		if (file == null)
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
	public JavacOptions options() {
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
	 * Registers resources as a classpath items.
	 *
	 * @param resources
	 * 		Resources to include.
	 */
	public void addToClassPath(Iterable<? extends JavaResource> resources) {
		List<JavaResource> classpath = this.classpath;
		resources.forEach(classpath::add);
	}

	/**
	 * Registers {@link Resource} as a classpath item.
	 *
	 * @param resource
	 * 		Resource to include.
	 */
	public void addToClassPath(JavaResource resource) {
		classpath.add(resource);
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
		public Iterable<JavaFileObject> list(Location location, String packageName,
											 Set<Kind> kinds, boolean recurse) throws IOException {
			Iterable<JavaFileObject> list = super.list(location, packageName, kinds, recurse);
			if ("CLASS_PATH".equals(location.getName()) && kinds.contains(Kind.CLASS)) {
				String formatted = packageName.isEmpty() ? "" : packageName.replace('.', '/') + '/';
				Set<JavaFileObject> result = Sets.newHashSet(list);
				Predicate<String> check;
				if (recurse) {
					check = s -> s.startsWith(formatted);
				} else {
					check = s -> s.startsWith(formatted)
							&& s.indexOf('/', formatted.length()) == -1;
				}
				classpath.stream()
						.flatMap(x -> x.getClasses().entrySet().stream())
						.filter(e -> check.test(e.getKey()))
						.forEach(x -> result.add(new ResourceVirtualJavaFileObject(x.getKey(),
								x.getValue(), Kind.CLASS)));
				return result;
			}
			return list;
		}

		@Override
		public String inferBinaryName(Location location, JavaFileObject file) {
			if (file instanceof ResourceVirtualJavaFileObject && file.getKind() == Kind.CLASS) {
				return ((ResourceVirtualJavaFileObject) file).getResourceName().replace('/', '.');
			}
			return super.inferBinaryName(location, file);
		}

		@Override
		public JavaFileObject getJavaFileForOutput(Location location, String name, Kind
				kind, FileObject sibling) throws IOException {
			String internal = name.replace('.', '/');
			VirtualJavaFileObject obj = unitMap.get(internal);
			// Unknown class, assumed to be an inner class.
			// add to the unit map so it can be fetched.
			if (obj == null) {
				obj = new VirtualJavaFileObject(internal, null);
				unitMap.put(internal, obj);
			}
			return obj;
		}
	}
}