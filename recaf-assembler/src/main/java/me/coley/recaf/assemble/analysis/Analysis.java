package me.coley.recaf.assemble.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Wrapper for method analysis.
 *
 * @author Matt Coley
 * @see Analyzer
 */
public class Analysis {
	private final TreeMap<Integer, Block> blocks = new TreeMap<>();
	private final List<Frame> frames;

	/**
	 * @param codeSize
	 * 		Number of instructions in the method.
	 */
	public Analysis(int codeSize) {
		frames = newFrames(codeSize);
	}

	/**
	 * @param insnIndex
	 * 		Block's starting position in instruction count.
	 * @param block
	 * 		Block instance to add.
	 */
	public void addBlock(int insnIndex, Block block) {
		blocks.put(insnIndex, block);
	}

	/**
	 * @param insnIndex
	 * 		Instruction index in a method.
	 *
	 * @return {@code true} if a block starts at the given index.
	 */
	public boolean isBlockStart(int insnIndex) {
		return blocks.containsKey(insnIndex);
	}

	/**
	 * @param index
	 * 		Instruction index in a method.
	 *
	 * @return The first block that defined at or before the index.
	 */
	public Block blockFloor(int index) {
		return blocks.floorEntry(index).getValue();
	}

	/**
	 * @param index
	 * 		Instruction index in a method.
	 *
	 * @return Block that starts at the given index.
	 * If a block does not start there, then {@code null}.
	 */
	public Block block(int index) {
		return blocks.get(index);
	}

	/**
	 * @param index
	 * 		Instruction index in a method.
	 *
	 * @return Frame of the instruction.
	 */
	public Frame frame(int index) {
		return frames.get(index);
	}

	/**
	 * @return Sorted map of blocks.
	 */
	public TreeMap<Integer, Block> getBlocks() {
		return blocks;
	}

	/**
	 * @return List of frames.
	 */
	public List<Frame> getFrames() {
		return frames;
	}

	private static List<Frame> newFrames(int size) {
		List<Frame> list = new ArrayList<>(size);
		for (int i = 0; i < size; i++)
			list.add(new Frame());
		return list;
	}
}
