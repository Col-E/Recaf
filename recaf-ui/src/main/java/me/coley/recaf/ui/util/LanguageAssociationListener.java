package me.coley.recaf.ui.util;

import me.coley.recaf.ui.control.code.Language;

/**
 * Listener to receive updates when an association between a file extension and a {@link Language} changes.
 *
 * @author yapht
 */
public interface LanguageAssociationListener {
	/**
	 * @param extension
	 * 		The file extension the language should apply to.
	 * @param newLanguage
	 * 		The new language associated with extension.
	 */
	void onAssociationChanged(String extension, Language newLanguage);
}
