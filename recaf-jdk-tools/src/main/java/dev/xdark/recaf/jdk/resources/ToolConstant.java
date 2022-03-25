package dev.xdark.recaf.jdk.resources;

/**
 * Tool constants.
 *
 * @author xDark
 */
public class ToolConstant {

	/**
	 * Exit command.
	 */
	public static final int EXIT = 0;
	/**
	 * Resource request command.
	 */
	public static final int RESOURCE_REQUEST = 1;

	/**
	 * Block size.
	 */
	public static final int TRANSFER_BUFFER = 4096;
	/**
	 * Indicates that block was successfully transferred.
	 */
	public static final int TRANSFER_BLOCK_OK = 0;
	/**
	 * Indicates start of the first block,
	 * transfer block size is followed after that.
	 */
	public static final int TRANSFER_BLOCK_START = 1;
	/**
	 * Indicates that remote JVM failed to read resource,
	 * UTF-8 encoded error is followed after that
	 */
	public static final int TRANSFER_BLOCK_ERROR = 2;
	/**
	 * Indicates that transfer was complete.
	 */
	public static final int TRANSFER_BLOCK_COMPLETE = 3;
	/**
	 * Indicates that transfer was complete, but with
	 * some extra redundant data at the end.
	 */
	public static final int TRANSFER_BLOCK_PAD = 4;
	/**
	 * Indicates that resource was not found.
	 */
	public static final int TRANSFER_RESOURCE_NOT_FOUND = 5;
}
