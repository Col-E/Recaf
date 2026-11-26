package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import org.objectweb.asm.tree.AbstractInsnNode;
import software.coley.recaf.services.search.query.structure.InsnMatcher;

/**
 * Android instruction matcher.
 *
 * @author Matt Coley
 */
public sealed interface DexInsnMatcher extends InsnMatcher permits AnyDexInsnMatcher,
		DexArrayInstructionMatcher, DexArrayLengthInstructionMatcher, DexBinary2AddrInstructionMatcher,
		DexBinaryInstructionMatcher, DexBinaryLiteralInstructionMatcher, DexBranchInstructionMatcher,
		DexBranchZeroInstructionMatcher, DexCheckCastInstructionMatcher, DexCompareInstructionMatcher,
		DexConstInstructionMatcher, DexConstMethodHandleInstructionMatcher, DexConstMethodTypeInstructionMatcher,
		DexConstStringInstructionMatcher, DexConstTypeInstructionMatcher, DexConstWideInstructionMatcher,
		DexFillArrayDataInstructionMatcher, DexFilledNewArrayInstructionMatcher, DexGotoInstructionMatcher,
		DexInstanceFieldInstructionMatcher, DexInstanceOfInstructionMatcher, DexInvokeCustomInstructionMatcher,
		DexInvokeInstructionMatcher, DexLabelMatcher, DexMonitorInstructionMatcher, DexMoveExceptionInstructionMatcher,
		DexMoveInstructionMatcher, DexMoveObjectInstructionMatcher, DexMoveResultInstructionMatcher,
		DexMoveWideInstructionMatcher, DexNewArrayInstructionMatcher, DexNewInstanceInstructionMatcher,
		DexNopInstructionMatcher, DexOpcodeInsnMatcher, DexPackedSwitchInstructionMatcher,
		DexReturnInstructionMatcher, DexSparseSwitchInstructionMatcher, DexStaticFieldInstructionMatcher,
		DexThrowInstructionMatcher, DexUnaryInstructionMatcher {

	@Override
	default boolean matchesJvm(@Nonnull AbstractInsnNode node) {
		return false;
	}

	@Override
	boolean matchesAndroid(@Nonnull Instruction instruction);
}
