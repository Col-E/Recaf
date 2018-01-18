package me.coley.recaf.ui;

import java.util.HashMap;
import java.util.Map;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import me.coley.recaf.util.Streams;

public class Lang {
	private static final Map<String, String> map = new HashMap<>();

	public static String get(String key) {
		return map.getOrDefault(key, key);
	}

	public static void load(String lang) {
		// If this is crashing you, add **/*.json to your source filters.
		// The assets need to be on the class-path.
		// Refresh the project when that's done.
		try {
			String prefix = "/resources/lang/";
			String file = prefix + lang + ".json";
			String jsStr = Streams.toString(Lang.class.getResourceAsStream(file));
			JsonObject json = Json.parse(jsStr).asObject();
			json.forEach(v -> {
				map.put(v.getName(), v.getValue().asString());
			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
