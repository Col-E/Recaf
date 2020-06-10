package me.coley.recaf.ui.controls.text.model;

import com.eclipsesource.json.*;
import me.coley.recaf.util.Log;
import org.apache.commons.io.IOUtils;

import java.net.URL;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Utility for loading language style rulesets.
 *
 * @author Matt
 */
public class Languages {
	private static final Language NONE = new Language("none", Collections.emptyList(), true);
	private static final Map<String, Language> CACHE = new HashMap<>();

	/**
	 * @param key
	 * 		Name of language
	 *
	 * @return Language ruleset for styling.
	 */
	public static Language find(String key) {
		key = key.toLowerCase();
		// Check if already fetched
		Language language = CACHE.get(key);
		if(language != null)
			return language;
		// Attempt to read language file
		try {
			String file = "languages/" + key + ".json";
			URL url = Thread.currentThread().getContextClassLoader().getResource(file);
			// If file found, parse
			if(url != null) {
				String jsStr = IOUtils.toString(url.openStream(), UTF_8);
				JsonObject json = Json.parse(jsStr).asObject();
				language = parse(json);
			}
		} catch(Exception ex) {
			Log.error(ex, "Failed parsing language json for type '{}'", key);
		}
		// Update cache and return
		if (language == null)
			language = NONE;
		CACHE.put(key, language);
		return language;
	}

	private static Language parse(JsonObject json) {
		String name = json.getString("name", null);
		if (name == null)
			throw new IllegalArgumentException("Language JSON missing name");
		boolean wrap = json.getBoolean("wrap", true);
		JsonArray jsonArray = json.get("rules").asArray();
		List<Rule> rules = new ArrayList<>();
		jsonArray.forEach(arr -> {
			String ruleName = arr.asObject().getString("name", null);
			String rulePattern = arr.asObject().getString("pattern", null);
			rules.add(new Rule(ruleName, rulePattern));
		});
		return new Language(name, rules, wrap);
	}
}
