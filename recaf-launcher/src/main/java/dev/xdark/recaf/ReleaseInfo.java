package dev.xdark.recaf;

import com.google.gson.annotations.SerializedName;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Information about GitHub release.
 *
 * @author xDark
 */
public final class ReleaseInfo {
	/**
	 * Name of the release.
	 */
	@SerializedName("tag_name")
	private final String name;

	/**
	 * Release changelog.
	 * May be {@code null}.
	 */
	@SerializedName("body")
	private final String changelog;

	/**
	 * Assets of the release.
	 */
	private final List<Asset> assets;

	/**
	 * Publication date.
	 * May be {@code null}.
	 */
	@SerializedName("published_at")
	private final Instant timestamp;

	/**
	 * @param name
	 * 		Name of the release.
	 * @param changelog
	 * 		Release changelog.
	 * @param assets
	 * 		Assets of the release.
	 * @param timestamp
	 * 		Publication date.
	 */
	public ReleaseInfo(String name, String changelog,
					   List<Asset> assets, Instant timestamp) {
		this.name = Objects.requireNonNull(name, "name");
		this.changelog = changelog;
		this.assets = assets;
		this.timestamp = timestamp;
	}

	/**
	 * @return Name of the release.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Changelog of the release.
	 * May be {@code null}.
	 */
	public String getChangelog() {
		return changelog;
	}

	/**
	 * @return Assets of the release.
	 */
	public List<Asset> getAssets() {
		return assets;
	}

	/**
	 * @return Publication date.
	 * May be {@code null}.
	 */
	public Instant getTimestamp() {
		return timestamp;
	}

	@Override
	public String toString() {
		return "ReleaseInfo{" +
				"name='" + name + '\'' +
				", changelog='" + changelog + '\'' +
				", assets=" + assets +
				", timestamp=" + timestamp +
				'}';
	}
}
