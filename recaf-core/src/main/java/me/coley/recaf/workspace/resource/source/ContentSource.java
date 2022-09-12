package me.coley.recaf.workspace.resource.source;

import me.coley.cafedude.classfile.VersionConstants;
import me.coley.recaf.util.ByteHeaderUtil;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.visitor.CustomAttributeCollectingVisitor;
import me.coley.recaf.workspace.resource.Resource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Origin location information and loading for {@link Resource}s.
 *
 * @author Matt Coley
 */
public abstract class ContentSource {
	private static final Logger logger = Logging.get(ContentSource.class);
	private final Set<ContentSourceListener> listeners = new HashSet<>();
	private final SourceType type;

	protected ContentSource(SourceType type) {
		this.type = type;
	}

	/**
	 * @param listener
	 * 		Listener to remove.
	 */
	public void removeListener(ContentSourceListener listener) {
		listeners.remove(listener);
	}

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	public void addListener(ContentSourceListener listener) {
		listeners.add(listener);
	}

	/**
	 * @return Listeners.
	 */
	public Set<ContentSourceListener> getListeners() {
		return listeners;
	}

	/**
	 * Populates the given resource with classes and files from the current content source.
	 *
	 * @param resource
	 * 		Destination.
	 *
	 * @throws IOException
	 * 		When reading from the source encounters some error.
	 */
	public void readInto(Resource resource) throws IOException {
		ContentCollection collection = new ContentCollection(resource);
		getListeners().forEach(l -> l.onPreRead(collection));
		onRead(collection);
		getListeners().forEach(l -> l.onFinishRead(collection));
		// Log results / summarize what has been found
		int actionable = collection.getPendingCount();
		if (actionable > 0) {
			logger.info("Read {} classes, {} files, {} actionable items",
					collection.getClassCount() + collection.getDexClassCount(),
					collection.getFileCount(),
					collection.getPendingCount());
		} else {
			logger.info("Read {} classes, {} files",
					collection.getClassCount() + collection.getDexClassCount(),
					collection.getFileCount());
		}
		// Populate
		collection.getFiles().forEach((path, info) -> {
			resource.getFiles().initialPut(info);
		});
		collection.getClasses().forEach((path, info) -> {
			resource.getClasses().initialPut(info);
		});
		collection.getDexClasses().getBackingMap().forEach((path, map) -> {
			resource.getDexClasses().putDexMap(path, map);
		});
	}

	/**
	 * Reads classes and files from the source and deposits them into the given resource.
	 *
	 * @param collection
	 * 		Destination.
	 *
	 * @throws IOException
	 * 		When reading from the source encounters some error.
	 */
	protected abstract void onRead(ContentCollection collection) throws IOException;

	/**
	 * @return Content source type.
	 */
	public SourceType getType() {
		return type;
	}


	/**
	 * Check if the class can be parsed by ASM.
	 *
	 * @param content
	 * 		The class file content.
	 *
	 * @return {@code true} if ASM can parse the class.
	 */
	protected static boolean isParsableClass(byte[] content) {
		try {
			CustomAttributeCollectingVisitor customVisitor = new CustomAttributeCollectingVisitor(new ClassWriter(0));
			ClassReader reader = new ClassReader(content);
			reader.accept(customVisitor, 0);
			if (customVisitor.hasCustomAttributes()) {
				throw new IllegalStateException("Unknown attributes found in class: " + reader.getClassName() + "[" +
						String.join(", ", customVisitor.getCustomAttributeNames()) + "]");
			}
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	/**
	 * Check if the byte array is prefixed by the class file magic header.
	 *
	 * @param content
	 * 		File content.
	 *
	 * @return If the content seems to be a class at a first glance.
	 */
	protected static boolean matchesClass(byte[] content) {
		// Null and size check
		// The smallest valid class possible that is verifiable is 37 bytes AFAIK, but we'll be generous here.
		if (content == null || content.length <= 16)
			return false;
		// We want to make sure the 'magic' is correct.
		if (!ByteHeaderUtil.match(content, ByteHeaderUtil.CLASS))
			return false;
		// 'dylib' files can also have CAFEBABE as a magic header... Gee, thanks Apple :/
		// Because of this we'll employ some more sanity checks.
		// Version number must be non-zero
		int version = ((content[6] & 0xFF) << 8) + (content[7] & 0xFF);
		if (version < VersionConstants.JAVA1)
			return false;
		// Must include some constant pool entries.
		// The smallest number includes:
		//  - utf8  - name of current class
		//  - class - wrapper of prior
		//  - utf8  - name of object class
		//  - class - wrapper of prior`
		int cpSize = ((content[8] & 0xFF) << 8) + (content[9] & 0xFF);
		return cpSize >= 4;
	}
}
