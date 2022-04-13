package me.coley.recaf.ssvm;

import dev.xdark.ssvm.fs.SimpleFileDescriptorManager;

/**
 * File descriptor manager that denies access to any file path.
 *
 * @author Matt Coley
 */
public class DenyingFileDescriptorManager extends SimpleFileDescriptorManager {
	@Override
	public long open(String path, int mode) {
		// Deny any file IO
		throw new IllegalStateException("Denied IO access[" + describe(mode) + "] to: " + path);
	}

	private static String describe(int mode) {
		switch (mode) {
			case READ:
				return "READ";
			case WRITE:
				return "WRITE";
			case APPEND:
				return "APPEND";
			default:
				return "?";
		}
	}
}
