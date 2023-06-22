package software.coley.recaf.services.navigation;

import software.coley.recaf.info.Info;

/**
 * Exception used to denote a {@link Info} type couldn't be shown in the UI due to lack of support.
 *
 * @author Matt Coley
 */
public class UnsupportedContent extends RuntimeException {
	/**
	 * @param message
	 * 		Exception message.
	 */
	public UnsupportedContent(String message) {
		super(message);
	}
}
