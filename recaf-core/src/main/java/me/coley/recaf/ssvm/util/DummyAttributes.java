package me.coley.recaf.ssvm.util;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

/**
 * Dummy attributes to use for {@link me.coley.recaf.ssvm.WorkspaceZipFile}.
 *
 * @author Matt Coley
 */
public class DummyAttributes implements BasicFileAttributes {
	private final String path;

	/**
	 * @param path
	 * 		File path.
	 */
	public DummyAttributes(String path) {
		this.path = path;
	}

	@Override
	public FileTime lastModifiedTime() {
		return creationTime();
	}

	@Override
	public FileTime lastAccessTime() {
		return creationTime();
	}

	@Override
	public FileTime creationTime() {
		return FileTime.fromMillis(System.currentTimeMillis());
	}

	@Override
	public boolean isRegularFile() {
		return true;
	}

	@Override
	public boolean isDirectory() {
		return false;
	}

	@Override
	public boolean isSymbolicLink() {
		return false;
	}

	@Override
	public boolean isOther() {
		return false;
	}

	@Override
	public long size() {
		return 100L;
	}

	@Override
	public Object fileKey() {
		return path;
	}
}
