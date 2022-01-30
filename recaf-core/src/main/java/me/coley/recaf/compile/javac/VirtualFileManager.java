package me.coley.recaf.compile.javac;

import com.google.common.collect.Sets;
import me.coley.recaf.workspace.resource.Resource;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * File manager extension for handling updates to java file object's output stream.
 * Additionally, registers inner classes as new files.
 *
 * @author Matt Coley
 */
public class VirtualFileManager extends ForwardingJavaFileManager<JavaFileManager> {
	private final VirtualUnitMap unitMap;
	private final List<Resource> classpath;

	/**
	 * @param unitMap
	 * 		Class input map.
	 * @param classpath
	 * 		In-memory classpath.
	 * @param fallback
	 * 		Fallback manager.
	 */
	public VirtualFileManager(VirtualUnitMap unitMap, List<Resource> classpath, JavaFileManager fallback) {
		super(fallback);
		this.classpath = classpath;
		this.unitMap = unitMap;
	}

	@Override
	public Iterable<JavaFileObject> list(Location location, String packageName,
										 Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException {
		Iterable<JavaFileObject> list = super.list(location, packageName, kinds, recurse);
		if ("CLASS_PATH".equals(location.getName()) && kinds.contains(JavaFileObject.Kind.CLASS)) {
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
							x.getValue().getValue(), JavaFileObject.Kind.CLASS)));
			return result;
		}
		return list;
	}

	@Override
	public String inferBinaryName(Location location, JavaFileObject file) {
		if (file instanceof ResourceVirtualJavaFileObject && file.getKind() == JavaFileObject.Kind.CLASS) {
			return ((ResourceVirtualJavaFileObject) file).getResourceName().replace('/', '.');
		}
		return super.inferBinaryName(location, file);
	}

	@Override
	public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String name, JavaFileObject.Kind
			kind, FileObject sibling) throws IOException {
		// Name should be like "com.example.MyClass$MyInner"
		String internal = name.replace('.', '/');
		VirtualJavaFileObject obj = unitMap.getFile(internal);
		// Unknown class, assumed to be an inner class.
		// add to the unit map so it can be fetched.
		if (obj == null) {
			obj = new VirtualJavaFileObject(internal, null);
			unitMap.addFile(internal, obj);
		}
		return obj;
	}
}