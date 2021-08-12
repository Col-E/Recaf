package me.coley.recaf.workspace.resource;

import me.coley.cafedude.ClassFile;
import me.coley.cafedude.InvalidClassException;
import me.coley.cafedude.io.ClassFileReader;
import me.coley.cafedude.io.ClassFileWriter;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.util.ByteHeaderUtil;
import me.coley.recaf.util.ValidationVisitor;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.resource.source.ContentSourceListener;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A content listener that uses CafeDude to patch an assortment of ASM crashing capabilities.
 *
 * @author Matt Coley
 */
public class ClassPatchingListener implements ContentSourceListener {
	private static final Logger logger = Logging.get(ClassPatchingListener.class);
	private final List<FileInfo> invalidClasses = new ArrayList<>();
	private final List<String> invalidEntryClassPaths = new ArrayList<>();
	private final List<String> invalidEntryFilePaths = new ArrayList<>();

	@Override
	public void onFinishRead(Resource resource) {
		if (invalidClasses.isEmpty())
			return;
		// Try to recover classes
		int recovered = 0;
		int count = invalidClasses.size();
		logger.info("Attempting to patch {} malformed classes", count);
		Iterator<FileInfo> iterator = invalidClasses.iterator();
		while (iterator.hasNext()) {
			FileInfo file = iterator.next();
			String fileName = file.getName();
			byte[] clazz = file.getValue();
			// The class should match the known file header.
			// If a class is packed or XOR'd we have no easy way to tell and automatically undo such an operation.
			if (ByteHeaderUtil.match(clazz, ByteHeaderUtil.CLASS)) {
				// Attempt to patch the class
				try {
					ClassFileReader reader = new ClassFileReader();
					ClassFile classFile = reader.read(clazz);
					clazz = new ClassFileWriter().write(classFile);
				} catch (InvalidClassException ex) {
					logger.error("CAFEDUDE failed to parse {}", fileName, ex);
					continue;
				}
				// Check if it can be read by ASM and update the resource
				try {
					new ClassReader(clazz).accept(new ValidationVisitor(), 0);
					// If we reach here it can be read.
					ClassInfo classInfo = ClassInfo.read(clazz);
					resource.getFiles().remove(fileName);
					resource.getClasses().put(classInfo.getName(), classInfo);
					iterator.remove();
					recovered++;
				} catch (Exception ex) {
					logger.error("ASM failed to parse patched class bytecode {}", fileName, ex);
				}
			} else {
				logger.warn("{} does not start with 0xCAFEBABE", fileName);
			}
		}
		String percent = String.format("%.2f", 100 * recovered / (double) count);
		logger.info("Recovered {}/{} ({}%) malformed classes", recovered, count, percent);

		if (!invalidEntryClassPaths.isEmpty() || !invalidEntryFilePaths.isEmpty()) {
			count = invalidEntryClassPaths.size() + invalidEntryFilePaths.size();
			logger.info("Pruning {} bogus entries ending in path separators", count);
			invalidEntryClassPaths.forEach(key -> resource.getClasses().remove(key));
			invalidEntryFilePaths.forEach(key -> resource.getFiles().remove(key));
		}
	}

	@Override
	public void onInvalidClassEntry(FileInfo clazz) {
		invalidClasses.add(clazz);
	}

	@Override
	public void onClassEntry(ClassInfo clazz) {
		if (clazz.getName().endsWith("/"))
			invalidEntryClassPaths.add(clazz.getName());
	}

	@Override
	public void onDexClassEntry(DexClassInfo clazz) {
		if (clazz.getName().endsWith("/"))
			invalidEntryClassPaths.add(clazz.getName());
	}

	@Override
	public void onFileEntry(FileInfo file) {
		if (file.getName().endsWith("/"))
			invalidEntryFilePaths.add(file.getName());
	}

	@Override
	public void onPreRead(Resource resource) {
		// no-op
	}

	@Override
	public void onPreWrite(Resource resource, Path path) {
		// no-op
	}

	@Override
	public void onFinishWrite(Resource resource, Path path) {
		// no-op
	}
}
