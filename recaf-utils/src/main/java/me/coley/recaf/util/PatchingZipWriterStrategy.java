package me.coley.recaf.util;

import me.coley.cafedude.InvalidClassException;
import me.coley.cafedude.classfile.ClassFile;
import me.coley.cafedude.io.ClassFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.coley.llzip.ZipArchive;
import software.coley.llzip.ZipCompressions;
import software.coley.llzip.part.CentralDirectoryFileHeader;
import software.coley.llzip.part.LocalFileHeader;
import software.coley.llzip.strategy.ZipWriterStrategy;
import software.coley.llzip.util.ByteData;
import software.coley.llzip.util.ByteDataUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Used to strip excess/red-herring data from jar files.
 * Typically, used in combination with {@link software.coley.llzip.ZipIO#readJvm(byte[])} for jar/zip files
 * manually crafted to deceive tools that do not parse zips the same way the JVM does.
 *
 * @author Matt Coley
 */
public class PatchingZipWriterStrategy implements ZipWriterStrategy {
	private static final Logger logger = LoggerFactory.getLogger(PatchingZipWriterStrategy.class);

	@Override
	public void write(ZipArchive archive, OutputStream os) throws IOException {
		Set<String> usedNames = new HashSet<>();
		try (ZipOutputStream zos = new ZipOutputStream(os)) {
			for (LocalFileHeader fileHeader : archive.getLocalFiles()) {
				CentralDirectoryFileHeader linked = fileHeader.getLinkedDirectoryFileHeader();
				if (linked == null)
					continue;
				if (fileHeader.getCrc32() == 0)
					continue;
				String name = linked.getFileNameAsString();
				// We only care about file entries
				if (fileHeader.getFileData().length() > 0) {
					// Remove trailing ".class/"
					if (name.contains(".class/")) {
						name = name.substring(0, name.lastIndexOf('/'));
						logger.info("Removing trailing '/' for class '{}'", name);
					}
					// No duplicate entries allowed
					if (!usedNames.contains(name)) {
						// The data MUST be decompressible in order to pass to the zip output stream.
						ByteData data;
						try {
							data = ZipCompressions.decompress(fileHeader);
						} catch (Throwable t) {
							logger.info("Dropping invalid compressed file '{}'", name);
							continue;
						}
						// If the data contains a class, the file path MUST match the file path.
						if (name.endsWith(".class")) {
							try {
								if (ByteDataUtil.startsWith(data, 0L, ByteHeaderUtil.CLASS)) {
									String className = name.substring(0, name.lastIndexOf(".class"));
									ClassFile cf = new ClassFileReader().read(ByteDataUtil.toByteArray(data));
									if (!cf.getName().equals(className)) {
										logger.info("Dropping duplicate class '{}'", name);
										continue;
									}
								}
							} catch (InvalidClassException e) {
								// Probably some packed class file format and loaded via a classloader.
							}
							// Remove impossibly small files that clearly aren't even packed classloaded classes.
							if (data.length() <= 40) {
								logger.info("Dropping junk class '{}'", name);
								continue;
							}
						}
						// Add to output
						zos.putNextEntry(new ZipEntry(name));
						zos.write(ByteDataUtil.toByteArray(data));
						zos.closeEntry();
						usedNames.add(name);
					} else {
						logger.info("Dropping duplicate named entry '{}'", name);
					}
				}
			}
		}
	}
}
