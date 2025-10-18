package software.coley.recaf.services.compile;

import jakarta.annotation.Nonnull;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.util.Iterator;
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
	private final List<WorkspaceResource> virtualClasspath;

	/**
	 * @param unitMap
	 * 		Class input map.
	 * @param virtualClasspath
	 * 		In-memory classpath.
	 * @param fallback
	 * 		Fallback manager.
	 */
	public VirtualFileManager(@Nonnull VirtualUnitMap unitMap, @Nonnull List<WorkspaceResource> virtualClasspath, @Nonnull JavaFileManager fallback) {
		super(fallback);
		this.virtualClasspath = virtualClasspath;
		this.unitMap = unitMap;
	}

	@Override
	public Iterable<JavaFileObject> list(@Nonnull Location location, @Nonnull String packageName,
	                                     @Nonnull Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException {
		Iterable<JavaFileObject> list = super.list(location, packageName, kinds, recurse);
		if (StandardLocation.CLASS_PATH.equals(location) && kinds.contains(JavaFileObject.Kind.CLASS)) {
			String formatted = packageName.isEmpty() ? "" : packageName.replace('.', '/') + '/';
			Predicate<String> check;
			if (recurse) {
				check = name -> name.startsWith(formatted);
			} else {
				check = name -> name.startsWith(formatted) &&
						name.indexOf('/', formatted.length()) == -1;
			}
			return () -> new ClassPathIterator(list.iterator(), virtualClasspath.stream()
					.flatMap(resource -> resource.jvmClassBundleStream().flatMap(b -> b.entrySet().stream()))
					.filter(entry -> check.test(entry.getKey()))
					.<JavaFileObject>map(entry -> new ResourceVirtualJavaFileObject(entry.getKey(),
							entry.getValue().getBytecode(), JavaFileObject.Kind.CLASS))
					.iterator());
		}
		return list;
	}

	@Override
	public String inferBinaryName(@Nonnull Location location, @Nonnull JavaFileObject file) {
		if (file instanceof ResourceVirtualJavaFileObject virtualObject && file.getKind() == JavaFileObject.Kind.CLASS) {
			return virtualObject.getResourceName().replace('/', '.');
		}
		return super.inferBinaryName(location, file);
	}

	@Override
	public JavaFileObject getJavaFileForOutput(@Nonnull JavaFileManager.Location location, @Nonnull String name, @Nonnull JavaFileObject.Kind
			kind, FileObject sibling) {
		// Name should be like "com.example.MyClass$MyInner"
		String internal = name.replace('.', '/');
		VirtualJavaFileObject obj = unitMap.getFile(internal);

		// Unknown class, assumed to be an inner class.
		// Add it to the unit map, so it can be fetched.
		if (obj == null) {
			obj = new VirtualJavaFileObject(internal, null);
			unitMap.addFile(internal, obj);
		}
		return obj;
	}

	private static final class ClassPathIterator implements Iterator<JavaFileObject> {
		private final Iterator<JavaFileObject> first, second;

		ClassPathIterator(@Nonnull Iterator<JavaFileObject> first, @Nonnull Iterator<JavaFileObject> second) {
			this.first = first;
			this.second = second;
		}

		@Override
		public boolean hasNext() {
			return first.hasNext() || second.hasNext();
		}

		@Override
		public JavaFileObject next() {
			if (first.hasNext())
				return first.next();
			return second.next();
		}
	}
}