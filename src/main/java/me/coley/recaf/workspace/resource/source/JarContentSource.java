package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.workspace.resource.ClassInfo;
import me.coley.recaf.workspace.resource.FileInfo;
import me.coley.recaf.workspace.resource.Resource;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Origin location information for jars.
 *
 * @author Matt Coley
 */
public class JarContentSource extends FileContentSource {
	protected JarContentSource(Path path) {
		super(SourceType.JAR, path);
	}

	@Override
	public void readInto(Resource resource) throws IOException {
		try (ZipFile zipFile = new ZipFile(getPath().toFile())) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while(entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				// TODO: This is just for quick demo purposes
				//  - Proper impl will look closer to 2.X code for reading jars
				if (entry.getName().endsWith(".class")) {
					String className = entry.getName().replace(".class", "");
					byte[] content = IOUtils.toByteArray(zipFile.getInputStream(entry));
					resource.getClasses().initialPut(new ClassInfo(className, content));
				} else if (!entry.isDirectory()) {
					String fileName = entry.getName();
					byte[] content = IOUtils.toByteArray(zipFile.getInputStream(entry));
					resource.getFiles().initialPut(new FileInfo(fileName, content));
				}
			}
		}
	}
}
