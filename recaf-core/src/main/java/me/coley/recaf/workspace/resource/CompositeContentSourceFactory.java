package me.coley.recaf.workspace.resource;

import me.coley.recaf.util.UncheckedBiFunction;
import me.coley.recaf.workspace.resource.source.ContentSource;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

/**
 * Composite factory.
 *
 * @author xDark
 */
public final class CompositeContentSourceFactory implements ContentSourceFactory {
	private final List<ContentSourceFactory> factories;

	/**
	 * @param factories
	 * 		List of factories.
	 */
	public CompositeContentSourceFactory(List<ContentSourceFactory> factories) {
		this.factories = factories;
	}

	@Override
	public ContentSource create(Path path) throws IOException {
		return create(path, ContentSourceFactory::create);
	}

	@Override
	public ContentSource create(URL url) throws IOException {
		return create(url, ContentSourceFactory::create);
	}

	private <T> ContentSource create(T input, UncheckedBiFunction<ContentSourceFactory, T, ContentSource> fn) {
		for (ContentSourceFactory factory : factories) {
			ContentSource source = fn.apply(factory, input);
			if (source != null) {
				return source;
			}
		}
		return null;
	}
}
