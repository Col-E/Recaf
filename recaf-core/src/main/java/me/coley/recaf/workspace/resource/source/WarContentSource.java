package me.coley.recaf.workspace.resource.source;

import java.nio.file.Path;

/**
 * Origin location information for wars.
 *
 * @author Matt Coley
 */
public class WarContentSource extends ArchiveFileContentSource {
	public static final String WAR_CLASS_PREFIX = "WEB-INF/classes/";

	/**
	 * @param path
	 * 		Path to war file.
	 */
	public WarContentSource(Path path) {
		super(SourceType.WAR, path);
		getListeners().add(new ContentSourceListener() {
			@Override
			public void onPreRead(ContentCollection collection) {
				// no-op
			}

			@Override
			public void onFinishRead(ContentCollection collection) {
				collection.getPendingNameMismatchedClasses().entrySet().removeIf(e -> {
					if (e.getKey().startsWith(WAR_CLASS_PREFIX)) {
						collection.addClass(e.getValue());
						return true;
					}
					return false;
				});
			}
		});
	}
}
