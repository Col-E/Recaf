package me.coley.recaf.util;

import java.util.*;
import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;

/**
 * Assist in copying abstract data types to the clipboard... or at least the
 * illusion of doing so.
 * 
 * @author Matt
 */
public class Clipboard {
	private static final Map<String, Object> content = new HashMap<>();
	private static String lastKey = null;

	public static <T> void setContent(String key, T value) {
		// Update internal map
		lastKey = key.trim();
		content.put(key, value);
		// Update system keyboard
		StringSelection selection = new StringSelection(key);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
	}

	public static <T> T getRecent() {
		return getFromKey(lastKey);
	}

	@SuppressWarnings("unchecked")
	public static <T> T getFromKey(String key) {
		Object obj = content.get(key.trim());
		if (obj == null) return null;
		return (T) obj;
	}

	public static boolean isRecentType(Class<?> type) {
		Object obj = content.get(lastKey);
		if (obj == null) return false;
		return type.isAssignableFrom(obj.getClass());
	}
}
