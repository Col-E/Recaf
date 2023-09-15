package software.coley.recaf.test;

import org.objectweb.asm.ClassReader;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.workspace.model.BasicWorkspace;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.BasicFileBundle;
import software.coley.recaf.workspace.model.bundle.BasicJvmClassBundle;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResourceBuilder;

import java.io.IOException;

/**
 * Various test utils for {@link Class} and {@link ClassInfo} usage.
 *
 * @author Matt Coley
 */
public class TestClassUtils {
	/**
	 * @param c
	 * 		Class ref.
	 *
	 * @return Info of class.
	 *
	 * @throws IOException
	 * 		When the class cannot be found at runtime.
	 */
	public static JvmClassInfo fromRuntimeClass(Class<?> c) throws IOException {
		return new JvmClassInfoBuilder(new ClassReader(c.getName())).build();
	}

	/**
	 * @param classes
	 * 		Classes to put into the bundle.
	 *
	 * @return Class bundle of classes.
	 *
	 * @throws IOException
	 * 		When a class could not be found at runtime.
	 */
	public static BasicJvmClassBundle fromClasses(Class<?>... classes) throws IOException {
		BasicJvmClassBundle bundle = new BasicJvmClassBundle();
		for (Class<?> cls : classes)
			bundle.initialPut(fromRuntimeClass(cls));
		return bundle;
	}

	/**
	 * @param classes
	 * 		Classes to put into the bundle.
	 *
	 * @return Class bundle of classes.
	 */
	public static BasicJvmClassBundle fromClasses(JvmClassInfo... classes) {
		BasicJvmClassBundle bundle = new BasicJvmClassBundle();
		for (JvmClassInfo cls : classes)
			bundle.initialPut(cls);
		return bundle;
	}

	/**
	 * @param files
	 * 		Files to put into the bundle.
	 *
	 * @return File bundle contianing the files.
	 */
	public static BasicFileBundle fromFiles(FileInfo... files) {
		BasicFileBundle bundle = new BasicFileBundle();
		for (FileInfo file : files)
			bundle.initialPut(file);
		return bundle;
	}

	/**
	 * @param classes
	 * 		Classes to put into the workspace.
	 *
	 * @return Workspace containing classes in single resource.
	 */
	public static Workspace fromBundle(JvmClassBundle classes) {
		WorkspaceResource resource = new WorkspaceResourceBuilder()
				.withJvmClassBundle(classes)
				.build();
		return new BasicWorkspace(resource);
	}

	/**
	 * @param files
	 * 		Files to put into the workspace.
	 *
	 * @return Workspace containing files in single resource.
	 */
	public static Workspace fromBundle(FileBundle files) {
		WorkspaceResource resource = new WorkspaceResourceBuilder()
				.withFileBundle(files)
				.build();
		return new BasicWorkspace(resource);
	}

	/**
	 * @param classes
	 * 		Classes to put into the workspace.
	 * @param files
	 * 		Files to put into the workspace.
	 *
	 * @return Workspace containing classes and files in single resource.
	 */
	public static Workspace fromBundles(JvmClassBundle classes, FileBundle files) {
		WorkspaceResource resource = new WorkspaceResourceBuilder()
				.withJvmClassBundle(classes)
				.withFileBundle(files)
				.build();
		return new BasicWorkspace(resource);
	}
}
