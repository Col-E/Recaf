package dev.xdark.recaf.plugin.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Wrapper of existing {@link PluginRepository} implementations.
 *
 * @author xtherk
 */
public class CommonPluginRepository implements PluginRepository {
	private static final List<PluginRepository> repositories = new ArrayList<>();

	static {
		repositories.add(new OfficialPluginRepository());
	}

	@Override
	public List<PluginRepositoryItem> pluginItems() {
		return repositories.stream()
				.flatMap(it -> it.pluginItems().stream())
				.collect(Collectors.toList());
	}
}
