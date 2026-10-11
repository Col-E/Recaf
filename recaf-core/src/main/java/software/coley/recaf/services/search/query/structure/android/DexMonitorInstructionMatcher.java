package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.MonitorInstruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a DEX monitor-enter or monitor-exit instruction.
 *
 * @param opcode
 * 		Opcode matcher.
 * @param register
 * 		Monitor object register matcher.
 * @param exit
 * 		Whether the instruction exits the monitor.
 *
 * @author Matt Coley
 */
public record DexMonitorInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                           @Nonnull Matcher<Integer> register,
                                           @Nonnull Matcher<Boolean> exit) implements DexInsnMatcher {
	/**
	 * @param opcode
	 * 		Exact monitor opcode.
	 * @param register
	 * 		Exact monitor object register.
	 * @param exit
	 * 		Exact monitor direction.
	 *
	 * @return Matcher against the exact monitor instruction.
	 */
	@Nonnull
	public static DexMonitorInstructionMatcher exact(int opcode, int register, boolean exit) {
		return new DexMonitorInstructionMatcher(Matchers.exact(opcode), Matchers.exact(register), Matchers.exact(exit));
	}

	/**
	 * @param register
	 * 		Exact monitor object register.
	 * @param exit
	 * 		Exact monitor direction.
	 *
	 * @return Matcher against a monitor instruction with the given operands.
	 */
	@Nonnull
	public static DexMonitorInstructionMatcher exact(int register, boolean exit) {
		return new DexMonitorInstructionMatcher(Matchers.any(), Matchers.exact(register), Matchers.exact(exit));
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return instruction instanceof MonitorInstruction monitor
				&& opcode.matches(monitor.opcode())
				&& register.matches(monitor.register())
				&& exit.matches(monitor.exit());
	}
}
