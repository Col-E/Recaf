package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.FilledNewArrayInstruction;
import me.darknet.dex.tree.definitions.instructions.InvokeCustomInstruction;
import me.darknet.dex.tree.definitions.instructions.InvokeInstruction;

import java.util.AbstractList;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;

/**
 * Read-only {@link List} view over DEX register operands without copying or boxing the whole register array.
 *
 * @author Matt Coley
 */
public final class DexRegisterList extends AbstractList<Integer> implements RandomAccess {
	private final int[] registers;
	private final int first;
	private final int size;

	private DexRegisterList(int[] registers, int first, int size) {
		this.registers = registers;
		this.first = first;
		this.size = size;
	}

	/**
	 * @param registers
	 * 		Explicit register array.
	 *
	 * @return Read-only view of the array.
	 */
	@Nonnull
	public static DexRegisterList of(int[] registers) {
		return new DexRegisterList(Objects.requireNonNull(registers), 0, registers.length);
	}

	/**
	 * @param first
	 * 		First register in a contiguous run.
	 * @param size
	 * 		Number of registers in the run.
	 *
	 * @return Read-only view of the register run.
	 */
	@Nonnull
	public static DexRegisterList range(int first, int size) {
		if (size < 0)
			throw new IllegalArgumentException("Register range size must not be negative");
		return new DexRegisterList(null, first, size);
	}

	/**
	 * @param instruction
	 * 		Instruction to read registers from.
	 *
	 * @return Read-only view of the instruction's register operands.
	 */
	@Nonnull
	public static DexRegisterList of(@Nonnull InvokeInstruction instruction) {
		int[] registers = instruction.arguments();
		if (registers != null)
			return of(registers);
		return range(instruction.first(), rangeSize(instruction.first(), instruction.last()));
	}

	/**
	 * @param instruction
	 * 		Instruction to read registers from.
	 *
	 * @return Read-only view of the instruction's register operands.
	 */
	@Nonnull
	public static DexRegisterList of(@Nonnull InvokeCustomInstruction instruction) {
		int[] registers = instruction.argumentRegisters();
		if (registers != null)
			return of(registers);
		return range(instruction.first(), rangeSize(instruction.first(), instruction.last()));
	}

	/**
	 * @param instruction
	 * 		Instruction to read registers from.
	 *
	 * @return Read-only view of the instruction's register operands.
	 */
	@Nonnull
	public static DexRegisterList of(@Nonnull FilledNewArrayInstruction instruction) {
		int[] registers = instruction.registers();
		if (registers != null)
			return of(registers);
		return range(instruction.first(), rangeSize(instruction.first(), instruction.last()));
	}

	private static int rangeSize(int first, int last) {
		long size = (long) last - first + 1;
		if (size < 0 || size > Integer.MAX_VALUE)
			throw new IllegalArgumentException("Invalid DEX register range: " + first + ".." + last);
		return (int) size;
	}

	@Override
	public Integer get(int index) {
		Objects.checkIndex(index, size);
		return registers == null ? first + index : registers[index];
	}

	@Override
	public int size() {
		return size;
	}
}
