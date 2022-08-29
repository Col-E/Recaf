package me.coley.recaf.workspace.resource;

import me.coley.recaf.util.ShortcutUtil;
import me.coley.recaf.workspace.resource.source.ContentSource;
import me.coley.recaf.workspace.resource.source.SourceType;
import me.coley.recaf.workspace.resource.source.UrlContentSource;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

/**
 * Default content source factory.
 *
 * @author xDark
 */
final class DefaultContentSourceFactory implements ContentSourceFactory {
	static final ContentSourceFactory INSTANCE = new DefaultContentSourceFactory();

	private DefaultContentSourceFactory() {
	}

	@Override
	public ContentSource create(Path path) throws IOException {
		path = ShortcutUtil.follow(path);
		return SourceType.fromPath(path).sourceFromPath(path);
	}

	@Override
	public ContentSource create(URL url) throws IOException {
		return new UrlContentSource(url);
	}
}
