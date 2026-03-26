package software.coley.recaf.services.callgraph.scanner;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.dex.tree.definitions.MethodMember;
import me.darknet.dex.tree.definitions.code.Code;
import me.darknet.dex.tree.definitions.instructions.Invoke;
import me.darknet.dex.tree.definitions.instructions.InvokeCustomInstruction;
import me.darknet.dex.tree.definitions.instructions.InvokeInstruction;
import me.darknet.dex.tree.visitor.DexClassVisitor;
import me.darknet.dex.tree.visitor.DexCodeVisitor;
import me.darknet.dex.tree.visitor.DexMethodVisitor;
import me.darknet.dex.tree.visitor.DexTreeWalker;
import software.coley.recaf.analytics.logging.DebuggingLogger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.services.callgraph.CallSite;
import software.coley.recaf.services.callgraph.InvokeKind;

/**
 * Android dex scanner for method call sites.
 *
 * @author Matt Coley
 */
public class AndroidMethodCallScanner implements MethodCallScanner {
	private static final DebuggingLogger logger = Logging.get(AndroidMethodCallScanner.class);

	@Override
	public boolean supports(@Nonnull ClassInfo classInfo) {
		return classInfo.isAndroidClass();
	}

	@Override
	public void scan(@Nonnull ClassInfo classInfo, @Nonnull CallSiteConsumer consumer) {
		AndroidClassInfo androidClass = classInfo.asAndroidClass();
		DexTreeWalker.accept(androidClass.getBackingDefinition(), new DexClassVisitor() {
			@Nullable
			@Override
			public DexMethodVisitor visitMethod(@Nonnull MethodMember method) {
				software.coley.recaf.info.member.MethodMember methodMember =
						androidClass.getDeclaredMethod(method.getName(), method.getType().descriptor());
				if (methodMember == null) {
					logger.error("Method {}{} was visited, but not present in info for declaring class {}",
							method.getName(), method.getType().descriptor(), androidClass.getName());
					return null;
				}

				return new DexMethodVisitor() {
					@Override
					public DexCodeVisitor visitCode(@Nonnull Code code) {
						return new DexCodeVisitor() {
							@Override
							public void visitInvokeInstruction(@Nonnull InvokeInstruction instruction) {
								consumer.accept(methodMember, new CallSite(
										instruction.owner().internalName().replace('.', '/'),
										instruction.name(),
										instruction.type().descriptor(),
										InvokeKind.fromDexOpcode(instruction.opcode()),
										instruction.opcode() == Invoke.INTERFACE));
							}

							@Override
							public void visitInvokeCustomInstruction(@Nonnull InvokeCustomInstruction instruction) {
								onInvokeCustomInstruction(methodMember, instruction, consumer);
							}
						};
					}
				};
			}
		});
	}

	/**
	 * Local extension point for dex invoke-custom handling.
	 *
	 * @param callingMethod
	 * 		Method containing the custom invoke.
	 * @param instruction
	 * 		Observed invoke-custom instruction.
	 * @param consumer
	 * 		Call-site consumer.
	 */
	protected void onInvokeCustomInstruction(@Nonnull software.coley.recaf.info.member.MethodMember callingMethod,
	                                         @Nonnull InvokeCustomInstruction instruction,
	                                         @Nonnull CallSiteConsumer consumer) {
		// TODO: Look into usage of invoke-custom in real-world samples, implement based on findings.
	}
}
