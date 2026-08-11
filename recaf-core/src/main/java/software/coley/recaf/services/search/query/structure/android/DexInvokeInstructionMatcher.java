package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.InvokeInstruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.query.structure.ListMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches a native DEX method invocation.
 *
 * @param opcode
 * 		Opcode matcher.
 * @param owner
 * 		Method owner matcher.
 * @param name
 * 		Method name matcher.
 * @param descriptor
 * 		Method descriptor matcher.
 * @param arguments
 * 		Argument register matcher.
 *
 * @author Matt Coley
 */
public record DexInvokeInstructionMatcher(@Nonnull Matcher<Integer> opcode,
                                          @Nonnull Matcher<String> owner,
                                          @Nonnull Matcher<String> name,
                                          @Nonnull Matcher<String> descriptor,
                                          @Nonnull ListMatcher<Matcher<Integer>> arguments) implements DexInsnMatcher {
	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		if (!(instruction instanceof InvokeInstruction value)
				|| !opcode.matches(value.opcode())
				|| !owner.matches(value.owner().internalName())
				|| !name.matches(value.name())
				|| !descriptor.matches(value.type().descriptor()))
			return false;
		List<Integer> registers = new ArrayList<>();
		for (int register : value.arguments())
			registers.add(register);
		return arguments.matches(registers);
	}
}
