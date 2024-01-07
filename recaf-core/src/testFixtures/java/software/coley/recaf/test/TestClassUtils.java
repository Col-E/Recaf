package software.coley.recaf.test;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
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
import java.util.function.Consumer;

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
	@Nonnull
	public static JvmClassInfo fromRuntimeClass(@Nonnull Class<?> c) throws IOException {
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
	@Nonnull
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
	@Nonnull
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
	@Nonnull
	public static Workspace fromBundle(@Nonnull JvmClassBundle classes) {
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
	@Nonnull
	public static Workspace fromBundle(@Nonnull FileBundle files) {
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
	@Nonnull
	public static Workspace fromBundles(@Nonnull JvmClassBundle classes, @Nonnull FileBundle files) {
		WorkspaceResource resource = new WorkspaceResourceBuilder()
				.withJvmClassBundle(classes)
				.withFileBundle(files)
				.build();
		return new BasicWorkspace(resource);
	}

	/**
	 * @param name
	 * 		Name of class to create.
	 *
	 * @return Info of generated class.
	 */
	@Nonnull
	public static JvmClassInfo createEmptyClass(@Nonnull String name) {
		return createClass(name, null);
	}

	/**
	 * @param name
	 * 		Name of class to create.
	 * @param consumer
	 * 		Optional post-processing to add content to the generated class.
	 *
	 * @return Info of generated class.
	 */
	@Nonnull
	public static JvmClassInfo createClass(@Nonnull String name, @Nullable Consumer<ClassNode> consumer) {
		ClassWriter cw = new ClassWriter(0);
		ClassNode node = new ClassNode();
		node.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", null);
		if (consumer != null) consumer.accept(node);
		node.accept(cw);
		return new JvmClassInfoBuilder(cw.toByteArray()).build();
	}
}
