package me.coley.recaf.workspace;

import me.coley.event.Bus;
import me.coley.recaf.Logging;
import me.coley.recaf.event.ClassReloadEvent;
import me.coley.recaf.event.HistoryRevertEvent;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Iterator;
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
	 * Classes map.
	 */
	private final ClassesMap classes;
	/**
	 * File being tracked.
	 */
	public final String name;

	public History(ClassesMap classes, String name) {
		this.classes = classes;
		this.name = name;
	}

	/**
	 * @return Size of history for the current class.
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
			classes.remove(name);
			classes.putRaw(name, content);
			Bus.post(new HistoryRevertEvent(name));
			Bus.post(new ClassReloadEvent(name));
			Logging.info("Reverted '" + name + "'");
		} else {
			Logging.info("No history for '" + name + "' to revert back to");
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