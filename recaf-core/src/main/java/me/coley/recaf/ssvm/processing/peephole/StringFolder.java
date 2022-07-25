package me.coley.recaf.ssvm.processing.peephole;

import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.api.VMInterface;
import dev.xdark.ssvm.execution.ExecutionContext;
import dev.xdark.ssvm.execution.Locals;
import dev.xdark.ssvm.mirror.InstanceJavaClass;
import dev.xdark.ssvm.thread.Backtrace;
import dev.xdark.ssvm.util.AsmUtil;
import dev.xdark.ssvm.value.Value;
import me.coley.recaf.ssvm.value.TrackedArrayValue;
import me.coley.recaf.ssvm.value.TrackedInstanceValue;
import me.coley.recaf.ssvm.value.TrackedValue;
import me.coley.recaf.util.Streams;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

import java.util.function.Predicate;

/**
 * Processors for folding creation of {@code String} values.
 *
 * @author xDark
 */
public class StringFolder {
	/**
	 * Installs peephole optimizations that inline some {@code String} constructor calls.
	 *
	 * @param vm
	 * 		Virtual machine to install into.
	 * @param whitelist
	 * 		Filter on which contexts to apply to.
	 */
	public static void installStringFolding(VirtualMachine vm, Predicate<ExecutionContext> whitelist) {
		VMInterface vmi = vm.getInterface();
		InstanceJavaClass jcString = vm.getSymbols().java_lang_String();
		vmi.registerMethodEnter(jcString, "<init>", "([B)V", ctx -> {
			if (!whitelist.test(ctx)) {
				return;
			}
			Locals locals = ctx.getLocals();
			Value _this = locals.load(0);
			if (!(_this instanceof TrackedInstanceValue)) {
				return;
			}
			Value bytes = locals.load(1);
			if (!(bytes instanceof TrackedArrayValue)) {
				return;
			}
			TrackedArrayValue array = (TrackedArrayValue) bytes;
			if (array.areAllValuesConstant()) {
				byte[] raw = ctx.getHelper().toJavaBytes(array);
				String str = new String(raw);
				Backtrace backtrace = ctx.getVM().currentThread().getBacktrace();
				// Get context of a method that invoked this <init> method,
				// usually its the 1 before this one, so we subtract 2.
				ExecutionContext caller = backtrace.get(backtrace.count() - 2).getExecutionContext();
				InsnList instructions = caller.getMethod().getNode().instructions;
				AbstractInsnNode callerNode = instructions.get(caller.getInsnPosition() - 1);
				AbstractInsnNode nextNode = callerNode.getNext();
				TrackedInstanceValue tracked = (TrackedInstanceValue) _this;
				// Find NEW instruction that allocates string value, we will
				// replace it with LDC
				Streams.<TrackedValue>recurse(tracked, value -> value.getParentValues().stream())
						.flatMap(x -> x.getContributingInstructions().stream())
						.filter(x -> x.getOpcode() == Opcodes.NEW)
						.findFirst()
						.ifPresent(x -> {
							instructions.set(x, new LdcInsnNode(str));
							// Because we invoke <init>([B)V of a string, we need
							// to POP value off the stack
							instructions.insertBefore(callerNode, new InsnNode(Opcodes.POP));
							instructions.remove(callerNode);
							// Remove all byte array instructions that contributed to this string
							Streams.<TrackedValue>recurse(array, value -> value.getClonedValues().stream())
									.flatMap(y -> y.getContributingInstructions().stream())
									.forEach(y -> {
										if (instructions.contains(y))
											instructions.remove(y);
									});
							// TODO: make ssvm use instructions instead of indices
							instructions.toArray();
							caller.setInsnPosition(AsmUtil.getIndex(nextNode));
						});
			}
		});
	}
}
