package me.coley.recaf.workspace;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;

/**
 * History manager for files.
 *
 * @author Matt
 */
public class History implements InputFileSystem {
	private static final int HISTORY_LENGTH = 10;
	private static final String HIST_POSTFIX = ".hst";
	private final FileSystem system;
	/**
	 * File being tracked.
	 */
	public final String name;
	/**
	 * Number of elements.
	 */
	public int length;

	public History(FileSystem system, String name) {
		this.system = system;
		this.name = name;
	}

	/**
	 * Wipe all items from the history.
	 *
	 * @throws IOException
	 * 		Thrown if the history file could not be deleted.
	 */
	public void clear() throws IOException {
		for(int i = 0; i < length; i++) {
			Path cur = getPath(name + HIST_POSTFIX + i);
			Files.delete(cur);
		}
		length = 0;
	}

	/**
	 * Fetch the creation times of all files in the history.
	 *
	 * @return Creation times. Lower index = older files.
	 *
	 * @throws IOException
	 * 		Thrown if the history file could not have their
	 * 		attributes read.
	 */
	public Instant[] getFileTimes() throws IOException {
		Instant[] instants = new Instant[length];
		for(int i = 0; i < length; i++) {
			Path file = getPath(name + HIST_POSTFIX + i);
			BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
			instants[i] = attr.creationTime().toInstant();
		}
		return instants;
	}

	/**
	 * Gets most recent change, shifts all history up.
	 *
	 * @return Last value before change.
	 *
	 * @throws IOException
	 * 		Thrown if the history file could not be read, or if
	 * 		history files could not be re-arranged.
	 */
	public byte[] pop() throws IOException {
		// Path of most recent element
		Path lastPath = getPath(name + HIST_POSTFIX + "0");
		if(Files.exists(lastPath)) {
			byte[] last = getFile(lastPath);
			// Move histories up.
			// Acts like a history stack.
			for(int i = -1; i < length; i++) {
				Path cur = i == -1 ? getPath(name) : getPath(name + HIST_POSTFIX + i);
				Path newer = getPath(name + HIST_POSTFIX + (i + 1));
				if(Files.exists(newer)) {
					Files.move(newer, cur, StandardCopyOption.REPLACE_EXISTING);
				}
			}
			// Decrease history count, return last
			length--;
			return last;
		}
		// No history to be found
		return null;
	}

	/**
	 * Updates current value, pushing the latest value into the history
	 * stack.
	 *
	 * @param modified
	 * 		Changed value.
	 *
	 * @throws IOException
	 * 		thrown if the value failed to push onto the stack.
	 */
	public void push(byte[] modified) throws IOException {
		Path path = getPath(name);
		for(int i = Math.min(length, HISTORY_LENGTH) + 1; i > 0; i--) {
			Path cur = getPath(name + HIST_POSTFIX + i);
			Path newer = getPath(name + HIST_POSTFIX + (i - 1));
			// start of history
			if(Files.exists(newer)) {
				Files.move(newer, cur, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		// Update lastest history element.
		Path pathNewHistory = getPath(name + HIST_POSTFIX + "0");
		write(pathNewHistory, getFile(name));
		// Update current value.
		write(path, modified);
		length++;
		if(length > HISTORY_LENGTH) {
			length = HISTORY_LENGTH;
		}
	}

	@Override
	public FileSystem getFileSystem() {
		return system;
	}
}