package me.coley.recaf.util;

import java.net.*;

/**
 * Networking utilities.
 *
 * @author Matt
 */
public class NetworkUtil {
	/**
	 * Timeout for URL verification. One second should be enough to do a simple length check.
	 */
	private static final int TIMEOUT = 1000;

	/**
	 * Verify that the URL points to a valid file.
	 *
	 * @param url
	 * 		The URL to verify.
	 *
	 * @throws IllegalArgumentException
	 * 		Thrown if the url times out or there is no content at the URL.
	 * @throws MalformedURLException
	 * 		Thrown if the url given is not formatted properly.
	 */
	public static void verifyUrlContent(String url) throws MalformedURLException {
		verifyUrlContent(new URL(url));
	}

	/**
	 * Verify that the URL points to a valid file.
	 *
	 * @param url
	 * 		The URL to verify.
	 *
	 * @throws IllegalArgumentException
	 * 		Thrown if the url times out or there is no content at the URL.
	 */
	public static void verifyUrlContent(URL url) {
		try {
			URLConnection conn = url.openConnection();
			conn.setReadTimeout(TIMEOUT);
			conn.setConnectTimeout(TIMEOUT);
			// Online check
			if(url.toString().startsWith("http")) {
				HttpURLConnection hconn = (HttpURLConnection) conn;
				hconn.setRequestMethod("GET");
				hconn.connect();
				// Request must be a "200 OK"
				int response = hconn.getResponseCode();
				if(response != 200)
					throw new IllegalArgumentException("File at URL \"" + url +
							"\" could not be loaded, gave response code: " + response);
			}
			// Local url check fallback
			else if (conn.getContentLength() == -1)
				throw new IllegalArgumentException("File at URL \"" + url + "\" does not exist!");

		} catch(Exception ex) {
			throw new IllegalArgumentException("File at URL \"" + url + "\" could not be reached!");
		}
	}
}
