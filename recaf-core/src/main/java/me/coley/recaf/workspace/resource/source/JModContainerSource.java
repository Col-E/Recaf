package me.coley.recaf.workspace.resource.source;

import java.nio.file.Path;

/**
 * Java module container source.
 *
 * @author xDark
 */
public class JModContainerSource extends ArchiveFileContentSource {

	public JModContainerSource(Path path) {
		super(SourceType.JMOD, path);
	}
}
