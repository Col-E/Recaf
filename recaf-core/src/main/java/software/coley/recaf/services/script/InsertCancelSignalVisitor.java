package software.coley.recaf.services.script;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.util.ReflectUtil;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

import static org.objectweb.asm.Opcodes.*;

final class InsertCancelSignalVisitor extends ClassVisitor {
	private static final MethodHandle GET_OFFSET;
	private static final String SINGLETON_TYPE = Type.getInternalName(CancellationSingleton.class);
	private static final String POLL = "poll";
	private static final String POLL_DESC = "()V";

	static {
		try {
			GET_OFFSET = ReflectUtil.lookup().findVirtual(Label.class, "getOffset", MethodType.methodType(int.class));
		} catch (ReflectiveOperationException ex) {
			throw new ExceptionInInitializerError(ex);
		}
	}

	InsertCancelSignalVisitor(ClassVisitor cv) {
		super(RecafConstants.getAsmVersion(), cv);
	}

	private static boolean isLoopback(Label label) {
		try {
			int ignored = (int) GET_OFFSET.invokeExact(label);
			return true;
		} catch (IllegalStateException ex) {
			return false;
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
			String[] exceptions) {
		return new MethodVisitor(api, super.visitMethod(access, name, descriptor, signature, exceptions)) {

			private boolean isAnyLoopback(Label[] labels) {
				for (Label label : labels) {
					if (isLoopback(label)) {
						return true;
					}
				}
				return false;
			}

			private void insertPoll(Label first, Label... labels) {
				if (!(isLoopback(first) || isAnyLoopback(labels)))
					return;
				super.visitMethodInsn(INVOKESTATIC, SINGLETON_TYPE, POLL, POLL_DESC, false);
			}

			@Override
			public void visitJumpInsn(int opcode, Label label) {
				insertPoll(label);
				super.visitJumpInsn(opcode, label);
			}

			@Override
			public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
				insertPoll(dflt, labels);
				super.visitLookupSwitchInsn(dflt, keys, labels);
			}

			@Override
			public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
				insertPoll(dflt, labels);
				super.visitTableSwitchInsn(min, max, dflt, labels);
			}
		};
	}
}
