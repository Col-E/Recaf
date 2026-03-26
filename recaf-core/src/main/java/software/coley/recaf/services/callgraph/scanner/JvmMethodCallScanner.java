package software.coley.recaf.services.callgraph.scanner;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.analytics.logging.DebuggingLogger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.callgraph.CallSite;
import software.coley.recaf.services.callgraph.InvokeKind;

import static org.objectweb.asm.Opcodes.*;

/**
 * JVM bytecode scanner for method call sites.
 *
 * @author Matt Coley
 */
public class JvmMethodCallScanner implements MethodCallScanner {
	private static final DebuggingLogger logger = Logging.get(JvmMethodCallScanner.class);
	private static final String LAMBDA_METAFACTORY_OWNER = "java/lang/invoke/LambdaMetafactory";
	private static final String LAMBDA_METAFACTORY_NAME = "metafactory";
	private static final String LAMBDA_METAFACTORY_DESC =
			"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;" +
					"Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)" +
					"Ljava/lang/invoke/CallSite;";

	@Override
	public boolean supports(@Nonnull ClassInfo classInfo) {
		return classInfo.isJvmClass();
	}

	@Override
	public void scan(@Nonnull ClassInfo classInfo, @Nonnull CallSiteConsumer consumer) {
		JvmClassInfo jvmClass = classInfo.asJvmClass();
		jvmClass.getClassReader().accept(new ClassVisitor(RecafConstants.getAsmVersion()) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
				MethodMember methodMember = jvmClass.getDeclaredMethod(name, descriptor);
				if (methodMember == null) {
					logger.error("Method {}{} was visited, but not present in info for declaring class {}",
							name, descriptor, jvmClass.getName());
					return null;
				}

				return new MethodVisitor(RecafConstants.getAsmVersion()) {
					@Override
					public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
						consumer.accept(methodMember,
								new CallSite(owner, name, descriptor, InvokeKind.fromJvmOpcode(opcode), isInterface));
					}

					@Override
					public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
						if (!LAMBDA_METAFACTORY_OWNER.equals(bootstrapMethodHandle.getOwner())
								|| !LAMBDA_METAFACTORY_NAME.equals(bootstrapMethodHandle.getName())
								|| !LAMBDA_METAFACTORY_DESC.equals(bootstrapMethodHandle.getDesc())) {
							super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
							return;
						}

						Object handleObj = bootstrapMethodArguments.length == 3 ? bootstrapMethodArguments[1] : null;
						if (handleObj instanceof Handle handle) {
							switch (handle.getTag()) {
								case H_INVOKESPECIAL,
								     H_INVOKEVIRTUAL,
								     H_INVOKESTATIC,
								     H_INVOKEINTERFACE -> consumer.accept(methodMember,
										new CallSite(handle.getOwner(), handle.getName(), handle.getDesc(),
												InvokeKind.fromJvmOpcode(handle.getTag()), handle.isInterface()));
								default -> {
									// no-op
								}
							}
							return;
						}

						super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
					}
				};
			}
		}, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
	}
}
