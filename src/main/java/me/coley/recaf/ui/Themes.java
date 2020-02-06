package me.coley.recaf.ui;

import me.coley.recaf.Recaf;
import me.coley.recaf.util.Resource;
import me.coley.recaf.util.SelfReferenceUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Theme manager.
 *
 * @author Matt
 */
public class Themes {
	// TODO: Add UI for live theme editor

	/**
	 * @return List of application-wide styles.
	 */
	public static List<Resource> getStyles() {
		List<Resource> resources =  SelfReferenceUtil.get().getStyles();
		resources.addAll(get("ui-", ".css"));
		return resources;
	}

	/**
	 * @return List of text-editor styles.
	 */
	public static List<Resource> getTextThemes() {
		List<Resource> resources =  SelfReferenceUtil.get().getTextThemes();
		resources.addAll(get("text-", ".css"));
		return resources;
	}

	private static Collection<Resource> get(String prefix, String suffix) {
		List<Resource> resources = new ArrayList<>();
		File styleDirectory = Recaf.getDirectory("style").toFile();
		if (!styleDirectory.exists())
			styleDirectory.mkdir();
		for (File file : styleDirectory.listFiles()) {
			String name = file.getName();
			if (name.startsWith(prefix) && name.endsWith(suffix))
				resources.add(Resource.external(file.getPath().replace('\\', '/')));
		}
		return resources;
	}
}
