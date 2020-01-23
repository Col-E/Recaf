package me.xdark.recaf.jvm;

import me.coley.recaf.util.InsnUtil;
import me.xdark.recaf.jvm.classloading.ClassLoader;
import me.xdark.recaf.jvm.classloading.SystemClassLoader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.util.List;

public final class VirtualMachine {
	private final VMInstructions instructions = new VMInstructions();
	private final Compiler compiler;
	private final ClassLoader scl;

	public VirtualMachine() {
		Compiler c = compiler = new StandardCompiler(this);
		scl = new SystemClassLoader(c);
	}

	public Compiler getCompiler() {
		return compiler;
	}

	public ClassLoader getSystemClassLoader() {
		return scl;
	}

	public <R> R execute(Object instance, Method method, Object... args) throws VMException {
		boolean nonStatic = method.nonStatic;
		if (nonStatic && instance == null) {
			throw new VMException("Attempted to invoke non-static method with null instance");
		}
		ExecutionContext<R> ctx = new ExecutionContext<>(method.maxStack, method.maxLocals, method, this);
		int local = 0;
		if (nonStatic) {
			ctx.store(local++, instance);
		}
		for (int i = 0, j = args.length; i < j; i++) {
			ctx.store(local++, args[i]);
		}
		VMInstructions handlers = this.instructions;
		InsnList instructions = method.instructions;
		loop:
		while (true) {
			int cursor = ctx.nextCursor();
			AbstractInsnNode node = instructions.get(cursor);
			int opcode = node.getOpcode();
			if (opcode == -1) {
				continue;
			}
			InstructionHandler handler = handlers.getHandlerForOpcode(opcode);
			if (handler == null) {
				throw new InvalidBytecodeException("No handler for opcode: " + opcode);
			}
			try {
				handler.process(node, ctx);
			} catch (ExecutionCancelSignal ex) {
				return ctx.isStackEmpty() ? null : ctx.pop();
			} catch (Throwable t) {
				List<TryCatchBlockNode> tryCatchBlockNodes = method.tryCatchBlockNodes;
				if (tryCatchBlockNodes == null) {
					throw new VMException(t);
				}
				for (TryCatchBlockNode tryCatchBlockNode : tryCatchBlockNodes) {
					if (cursor >= InsnUtil.getLabelOffset(tryCatchBlockNode.start) &&
							cursor <= InsnUtil.getLabelOffset(tryCatchBlockNode.end)) {
						String type = tryCatchBlockNode.type;
						// TODO type check
						if (true) {
							ctx.clearStack();
							ctx.push(t);
							ctx.setCursor(InsnUtil.index(tryCatchBlockNode.handler));
							continue loop;
						}
					}
				}
				throw new VMException(t);
			}
		}
	}
}
