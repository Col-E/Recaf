package me.coley.recaf.workspace.resource;

import me.coley.cafedude.InvalidClassException;
import me.coley.cafedude.classfile.ClassFile;
import me.coley.cafedude.io.ClassFileReader;
import me.coley.cafedude.io.ClassFileWriter;
import me.coley.cafedude.transform.IllegalStrippingTransformer;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.visitor.IllegalSignatureRemovingVisitor;
import me.coley.recaf.workspace.resource.source.ContentCollection;
import me.coley.recaf.workspace.resource.source.ContentSourceListener;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.slf4j.Logger;

import java.util.*;

/**
 * A content listener that uses CafeDude to patch an assortment of ASM crashing capabilities.
 *
 * @author Matt Coley
 */
public class ClassPatchingListener implements ContentSourceListener {
	private static final Logger logger = Logging.get(ClassPatchingListener.class);

	@Override
	public void onFinishRead(ContentCollection collection) {
		// Try to recover classes
		int invalidClasses = collection.getPendingInvalidClasses().size();
		collection.getPendingInvalidClasses().entrySet().removeIf(entry -> {
			String path = entry.getKey();
			byte[] data = entry.getValue();
			try {
				// Patch via CAFEDUDE
				ClassFileReader reader = new ClassFileReader();
				ClassFile classFile = reader.read(data);
				new IllegalStrippingTransformer(classFile).transform();
				byte[] patched = new ClassFileWriter().write(classFile);
				// Attempt to load
				ClassInfo info = ClassInfo.read(patched);
				collection.addClass(info);
				return true;
			} catch (InvalidClassException ex) {
				logger.error("CAFEDUDE failed to parse '{}'", path, ex);
			} catch (Throwable t) {
				logger.error("CAFEDUDE failed to patch '{}'", path, t);
			}
			// We failed to patch it, add as a file instead
			collection.addFile(new FileInfo(path, data));
			return false;
		});
		int recoveredClasses = invalidClasses - clearAndCountRemaining(collection.getPendingInvalidClasses());
		if (invalidClasses > 0) {
			String patchPercent = String.format("%.2f", 100 * recoveredClasses / (double) invalidClasses);
			logger.info("Recovered {}/{} ({}%) malformed classes", recoveredClasses, invalidClasses, patchPercent);
		}
		// Handle name mismatched classes
		int mismatchedClasses = collection.getPendingNameMismatchedClasses().size();
		collection.getPendingNameMismatchedClasses().entrySet().removeIf(entry -> {
			ClassInfo info = entry.getValue();
			String name = info.getName();
			// If the name isn't already used we can just add it with the correct name.
			if (!collection.getClasses().containsKey(name)) {
				collection.addClass(info);
				return true;
			}
			return false;
		});
		recoveredClasses = mismatchedClasses - collection.getPendingNameMismatchedClasses().size();
		if (mismatchedClasses > 0) {
			String patchPercent = String.format("%.2f", 100 * recoveredClasses / (double) mismatchedClasses);
			logger.info("Recovered {}/{} ({}%) mismatched class names", recoveredClasses, mismatchedClasses, patchPercent);
		}
		// Handle files named '.class' that aren't actual classes. Just add them as files.
		collection.getPendingNonClassClasses().entrySet().removeIf(entry -> {
			collection.addFile(new FileInfo(entry.getKey(), entry.getValue()));
			return true;
		});
		// Handle stripping bogus signatures
		Map<String, ClassInfo> patched = new HashMap<>();
		collection.getClasses().forEach((name, info) -> {
			ClassWriter writer = new ClassWriter(0);
			IllegalSignatureRemovingVisitor remover = new IllegalSignatureRemovingVisitor(writer);
			info.getClassReader().accept(remover, 0);
			if (remover.hasDetectedIllegalSignatures()) {
				patched.put(name, ClassInfo.read(writer.toByteArray()));
			}
		});
		if (!patched.isEmpty()) {
			logger.info("Stripped malformed signature data from {} classes", patched.size());
			patched.forEach((name, info) -> collection.replaceClass(info));
		}
		// TODO: Other actionable items
		//   - collection.getPendingDuplicateFiles()
		//   - collection.getPendingDuplicateClasses()
		//   - collection.getPendingNameMismatchedClasses()
	}

	@Override
	public void onPreRead(ContentCollection collection) {
		// no-op
	}

	private static int clearAndCountRemaining(Map<?, ?> collection) {
		int count = collection.size();
		collection.clear();
		return count;
	}
}
