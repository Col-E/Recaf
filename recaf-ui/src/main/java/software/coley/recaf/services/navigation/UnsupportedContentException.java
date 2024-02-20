package software.coley.recaf.services.navigation;

import software.coley.recaf.info.Info;

/**
 * Exception used to denote a {@link Info} type couldn't be shown in the UI due to lack of support.
 *
 * @author Matt Coley
 */
public class UnsupportedContentException extends RuntimeException {
	/**
	 * @param message
	 * 		Exception message.
	 */
	public UnsupportedContentException(String message) {
		super(message);
	}
}
