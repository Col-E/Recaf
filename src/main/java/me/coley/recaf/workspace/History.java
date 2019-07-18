package me.coley.recaf.workspace;

import me.coley.recaf.util.struct.ListeningMap;
import org.pmw.tinylog.Logger;

import java.time.Instant;
import java.util.Stack;

/**
 * History manager for files.
 *
 * @author Matt
 */
public class History {
	/**
	 * Stack of changed content.
	 */
	private final Stack<byte[]> stack = new Stack<>();
	/**
	 * Stack of when the content was changed.
	 */
	private final Stack<Instant> times = new Stack<>();
	/**
	 * File map to update when the history is rolled back.
	 */
	private final ListeningMap<String, byte[]> map;
	/**
	 * File being tracked.
	 */
	public final String name;

	public History(ListeningMap<String, byte[]> map, String name) {
		this.map = map;
		this.name = name;
	}

	/**
	 * @return Size of history for the current file.
	 */
	public int size() {
		return stack.size();
	}

	/**
	 * Wipe all items from the history.
	 */
	public void clear() {
		stack.clear();
		times.clear();
	}

	/**
	 * Fetch the creation times of all save states.
	 */
	public Instant[] getFileTimes()  {
		return times.toArray(new Instant[0]);
	}

	/**
	 * Gets most recent change, deleting it in the process.
	 */
	public byte[] pop() {
		times.pop();
		byte[] content = stack.pop();
		if (content != null) {
			map.remove(name);
			map.put(name, content);
			Logger.info("Reverted '" + name + "'");
		} else {
			Logger.info("No history for '" + name + "' to revert back to");
		}
		return content;
	}

	/**
	 * Updates current value, pushing the latest value into the history
	 * stack.
	 *
	 * @param modified
	 * 		Changed value.
	 */
	public void push(byte[] modified) {
		stack.push(modified);
		times.push(Instant.now());
	}
}