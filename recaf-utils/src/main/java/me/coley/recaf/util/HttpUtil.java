package me.coley.recaf.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Various HTTP request utilities.
 *
 * @author xtherk
 */
public class HttpUtil {
	private static final HttpClient httpClient;
	private static final int TIMEOUT_SECONDS = 10;

	static {
		httpClient = HttpClient.newBuilder()
				.version(HttpClient.Version.HTTP_1_1)
				.followRedirects(HttpClient.Redirect.NORMAL)
				.connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
				.build();
	}

	/**
	 * @param url
	 * 		URL to fetch content from.
	 *
	 * @return Response containing raw content from url.
	 *
	 * @throws IOException
	 * 		When an I/O error occurs when sending or receiving.
	 * @throws InterruptedException
	 * 		When the operation is interrupted.
	 */
	public static HttpResponse<byte[]> download(String url) throws IOException, InterruptedException {
		return download(URI.create(url));
	}

	/**
	 * @param uri
	 * 		URI to fetch content from.
	 *
	 * @return Response containing raw content from uri.
	 *
	 * @throws IOException
	 * 		When an I/O error occurs when sending or receiving.
	 * @throws InterruptedException
	 * 		When the operation is interrupted.
	 */
	public static HttpResponse<byte[]> download(URI uri) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder().uri(uri).GET().build();
		return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
	}

	/**
	 * @param url
	 * 		URL to fetch content from.
	 *
	 * @return Response containing text content from url.
	 *
	 * @throws IOException
	 * 		When an I/O error occurs when sending or receiving.
	 * @throws InterruptedException
	 * 		When the operation is interrupted.
	 */
	public static HttpResponse<String> get(String url) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
		return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
	}

	/**
	 * Async variant of {@link #get(String)}.
	 *
	 * @param url
	 * 		URL to fetch content from.
	 *
	 * @return Response containing text content from url.
	 */
	public static CompletableFuture<HttpResponse<String>> getAsync(String url) {
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
		return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
	}

	/**
	 * @param url
	 * 		URL to send request to.
	 * @param body
	 * 		Text of body to send.
	 *
	 * @return Response containing text reply from remote server.
	 *
	 * @throws IOException
	 * 		When an I/O error occurs when sending or receiving.
	 * @throws InterruptedException
	 * 		When the operation is interrupted.
	 */
	public static HttpResponse<String> post(String url, String body) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();
		return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
	}

	/**
	 * Async varianet of {@link #post(String, String)}.
	 *
	 * @param url
	 * 		URL to send request to.
	 * @param body
	 * 		Text of body to send.
	 *
	 * @return Response containing text reply from remote server.
	 */
	public static CompletableFuture<HttpResponse<String>> postAsync(String url, String body) {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();
		return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
	}
}
