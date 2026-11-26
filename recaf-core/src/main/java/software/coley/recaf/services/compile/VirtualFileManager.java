package software.coley.recaf.services.compile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.bundle.VersionedJvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * File manager extension for handling updates to java file object's output stream.
 * Additionally, registers inner classes as new files.
 *
 * @author Matt Coley
 */
public class VirtualFileManager extends ForwardingJavaFileManager<JavaFileManager> {
	private final CompileClasspathCache classpathCache;
	private final VirtualUnitMap unitMap;
	private final List<WorkspaceResource> virtualClasspath;
	private final String classPath;
	private final int versionTarget;

	/**
	 * @param classpathCache
	 * 		Cache for fallback classpath listings.
	 * @param unitMap
	 * 		Class input map.
	 * @param virtualClasspath
	 * 		In-memory classpath.
	 * @param fallback
	 * 		Fallback manager.
	 * @param classPath
	 * 		Compiler classpath, used to distinguish cached fallback results from other compilations.
	 * @param versionTarget
	 * 		Compiler target release, used to select workspace multi-release classes.
	 */
	public VirtualFileManager(@Nonnull CompileClasspathCache classpathCache,
	                          @Nonnull VirtualUnitMap unitMap,
	                          @Nonnull List<WorkspaceResource> virtualClasspath,
	                          @Nonnull JavaFileManager fallback,
	                          @Nullable String classPath,
	                          int versionTarget) {
		super(fallback);
		this.classpathCache = classpathCache;
		this.virtualClasspath = virtualClasspath;
		this.unitMap = unitMap;
		this.classPath = classPath;
		this.versionTarget = versionTarget;
	}

	@Override
	public Iterable<JavaFileObject> list(@Nonnull Location location, @Nonnull String packageName,
	                                     @Nonnull Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException {
		if (StandardLocation.CLASS_PATH.equals(location) && kinds.contains(JavaFileObject.Kind.CLASS)) {
			// First get the fallback manager's classpath listing, so we can combine it with our virtual classpath.
			List<JavaFileObject> list = classpathCache.getFallbackClassPathList(fileManager, classPath, versionTarget,
					location, packageName, kinds, recurse);

			// Filter the virtual classpath for classes in the requested package.
			String formatted = packageName.isEmpty() ? "" : packageName.replace('.', '/') + '/';
			Predicate<String> check;
			if (recurse) {
				check = name -> name.startsWith(formatted);
			} else {
				check = name -> name.startsWith(formatted) &&
						name.indexOf('/', formatted.length()) == -1;
			}
			return () -> new ClassPathIterator(list.iterator(), virtualClasspath.stream()
					.flatMap(resource -> listResourceClasses(resource, check))
					.iterator());
		}

		// Defer to the fallback manager for other locations and kinds.
		return super.list(location, packageName, kinds, recurse);
	}

	@Override
	public String inferBinaryName(@Nonnull Location location, @Nonnull JavaFileObject file) {
		if (file instanceof ResourceVirtualJavaFileObject virtualObject && file.getKind() == JavaFileObject.Kind.CLASS)
			return virtualObject.getResourceName().replace('/', '.');
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

	/**
	 * @param resource
	 * 		Resource to scan for classes.
	 * @param check
	 * 		Predicate to check if a class name should be included.
	 *
	 * @return Stream of class file objects from the resource and its embedded resources.
	 */
	@Nonnull
	private Stream<JavaFileObject> listResourceClasses(@Nonnull WorkspaceResource resource,
	                                                   @Nonnull Predicate<String> check) {
		Map<String, JvmClassInfo> selectedClasses = new TreeMap<>();

		// Look for multi-release classes, and select the highest version that is less than or equal to the target.
		if (versionTarget >= 9) {
			NavigableMap<Integer, VersionedJvmClassBundle> versionedBundlesBelow =
					resource.getVersionedJvmClassBundles().headMap(versionTarget, true).descendingMap();
			for (VersionedJvmClassBundle bundle : versionedBundlesBelow.values()) {
				for (Map.Entry<String, JvmClassInfo> entry : bundle.entrySet())
					if (check.test(entry.getKey()))
						selectedClasses.putIfAbsent(entry.getKey(), entry.getValue());
			}
		}

		// Fill in any remaining classes from the non-versioned bundles.
		for (JvmClassBundle bundle : resource.jvmClassBundles()) {
			for (Map.Entry<String, JvmClassInfo> entry : bundle.entrySet())
				if (check.test(entry.getKey()))
					selectedClasses.putIfAbsent(entry.getKey(), entry.getValue());
		}

		// Collect classes from embedded resources.
		Stream<JavaFileObject> embeddedClasses = resource.getEmbeddedResources().values().stream()
				.flatMap(embedded -> listResourceClasses(embedded, check));

		// Convert the selected classes to JavaFileObjects.
		Stream<JavaFileObject> localClasses = selectedClasses.entrySet().stream()
				.map(entry -> new ResourceVirtualJavaFileObject(entry.getKey(), entry.getValue().getBytecode(), JavaFileObject.Kind.CLASS));
		return Stream.concat(localClasses, embeddedClasses);
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