package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.io.ByteSourceElement;
import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.util.BufferData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Java module container source.
 *
 * @author xDark
 */
public class JModContainerSource extends ArchiveFileContentSource {
	/**
	 * @param path
	 * 		Path to the JMod file.
	 */
	public JModContainerSource(Path path) {
		super(SourceType.JMOD, path);
	}

	@Override
	protected Stream<ByteSourceElement<LocalFileHeader>> stream() throws IOException {
		return super.stream()
				.peek(x -> {
					// Normalize file names from .jmod archive
					LocalFileHeader header = x.getElement();
					String name = header.getFileNameAsString();
					name = name.substring(name.indexOf('/', 1) + 1);
					BufferData newName = BufferData.wrap(name.getBytes(StandardCharsets.UTF_8));
					header.setFileName(newName);
					header.getLinkedDirectoryFileHeader().setFileName(newName);
				});
	}
}
