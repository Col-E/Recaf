package dev.xdark.recaf;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

/**
 * GitHub asset.
 *
 * @author xDark
 */
public final class Asset {
	/**
	 * Name of the asset.
	 */
	private final String name;

	/**
	 * Size of the artifact.
	 */
	private final int size;

	/**
	 * Download URL.
	 */
	@SerializedName("browser_download_url")
	private final String url;

	/**
	 * @param name
	 * 		Name of the asset.
	 * @param size
	 * 		Size of the artifact.
	 * @param url
	 * 		Download URL.
	 */
	public Asset(String name, int size, String url) {
		this.name = Objects.requireNonNull(name, "name");
		this.size = size;
		this.url = Objects.requireNonNull(url, "url");
	}

	/**
	 * @return Asset's name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Asset's size.
	 */
	public int getSize() {
		return size;
	}

	/**
	 * @return Asset's download URL.
	 */
	public String getUrl() {
		return url;
	}

	@Override
	public String toString() {
		return "Asset{" +
				"name='" + name + '\'' +
				", size=" + size +
				", url='" + url + '\'' +
				'}';
	}
}
