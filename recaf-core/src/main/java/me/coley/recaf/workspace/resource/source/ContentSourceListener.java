package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.workspace.resource.ClassInfo;
import me.coley.recaf.workspace.resource.DexClassInfo;
import me.coley.recaf.workspace.resource.FileInfo;
import me.coley.recaf.workspace.resource.Resource;

import java.nio.file.Path;

/**
 * Listener for read/write operations of a {@link ContentSource}.
 *
 * @author Matt Coley
 */
public interface ContentSourceListener {
	/**
	 * Called before {@link ContentSource#onRead(Resource)} is invoked.
	 * Any pre-processing steps can be done here.
	 *
	 * @param resource
	 * 		Destination.
	 */
	void onPreRead(Resource resource);

	/**
	 * Called after {@link ContentSource#onRead(Resource)} completes.
	 * Any cleanup steps can be done here.
	 *
	 * @param resource
	 * 		Destination.
	 */
	void onFinishRead(Resource resource);

	/**
	 * Called before {@link ContentSource#onWrite(Resource, Path)} is invoked.
	 * Any pre-processing steps can be done here.
	 *
	 * @param resource
	 * 		Content.
	 * @param path
	 * 		Destination.
	 */
	void onPreWrite(Resource resource, Path path);

	/**
	 * Called after {@link ContentSource#onWrite(Resource, Path)}} completes.
	 * Any cleanup steps can be done here.
	 *
	 * @param resource
	 * 		Content.
	 * @param path
	 * 		Destination
	 */
	void onFinishWrite(Resource resource, Path path);

	/**
	 * Called right before the given class is loaded into the associated {@link Resource}.
	 *
	 * @param clazz
	 * 		Class loaded.
	 */
	void onClassEntry(ClassInfo clazz);

	/**
	 * Called right before the given android class is loaded into the associated {@link Resource}.
	 *
	 * @param clazz
	 * 		Class loaded.
	 */
	void onDexClassEntry(DexClassInfo clazz);

	/**
	 * Called right before the given class is loaded into the associated {@link Resource} as a file.
	 * This indicates the class likely is obfuscated with an ASM crasher,
	 * or the file extension says class but the file content is not actually a valid class.
	 *
	 * @param clazz
	 * 		Class loaded.
	 */
	void onInvalidClassEntry(FileInfo clazz);

	/**
	 * Called right before the given file is loaded into the associated {@link Resource}.
	 *
	 * @param file
	 * 		File loaded.
	 */
	void onFileEntry(FileInfo file);
}
