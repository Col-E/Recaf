package me.coley.recaf.workspace;

import me.coley.recaf.util.struct.ListeningMap;

import java.time.Instant;
import java.util.Stack;

import static me.coley.recaf.util.Log.*;

/**
 * History manager for files.
 *
 * @author Matt
 */
public class History {
	// TODO: For large inputs it would make sense to offload this to the file system.
	//  - But only for large inputs. In-memory is much faster and should be the default.
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
	/**
	 * Flag for if bottom is reached.
	 */
	private boolean atInitial = true;

	/**
	 * Constructs a history for an item of the given name in the given map.
	 *
	 * @param map
	 * 		Map containing the item.
	 * @param name
	 * 		Item's key.
	 */
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
	 * @return {@code true} if the top of the stack is the initial state of the item.
	 */
	public boolean isAtInitial() {
		return atInitial;
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
	 *
	 * @return Array of timestamps of each tracked change.
	 */
	public Instant[] getFileTimes() {
		return times.toArray(new Instant[0]);
	}

	/**
	 * @return Instant of most recent change.
	 */
	public Instant getMostRecentUpdate() {
		return times.peek();
	}

	/**
	 * Gets most recent change, deleting it in the process.
	 *
	 * @return Most recent version of the tracked file.
	 */
	public byte[] pop() {
		Instant time = times.pop();
		byte[] content = stack.pop();
		if (content != null) {
			map.put(name, content);
			// If the size is now 0, we just pop'd the initial state.
			// Since we ALWAYS want to keep the initial state we will push it back.
			if (size() == 0) {
				times.push(time);
				stack.push(content);
				atInitial = true;
				info("Reverted '{}' - initial state", name);
			} else {
				info("Reverted '{}' - {} total", name, stack.size());
			}
		} else {
			throw new IllegalStateException("No history to revert to!");
		}
		return content;
	}

	/**
	 * @return Most recent version of the tracked file.
	 */
	public byte[] peek() {
		return stack.peek();
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
		// Don't log the initial push
		if(stack.size() > 1) {
			info("Saved '{}' - {} total", name, stack.size());
			atInitial = false;
		}
	}
}