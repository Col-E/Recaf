package software.coley.recaf.services.deobfuscation;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.deobfuscation.transform.generic.InvokeDynamicInliningTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.InvokeDynamicResolver;
import software.coley.recaf.services.inheritance.InheritanceGraphService;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.util.Handles;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link InvokeDynamicInliningTransformer}.
 */
class InvokeDynamicInliningTest extends TransformerTestBase {
	@Test
	void rewritesResolvedMembersAndPreservesUnsupportedIndys() throws Exception {
		// Build a class containing fields, method calls, a malformed resolution, and a standard lambda site.
		putClass(CLASS_NAME, this::populateDynamicFixture);
		Workspace workspace = workspaceManager.getCurrent();
		WorkspaceResource resource = workspace.getPrimaryResource();
		JvmClassBundle bundle = resource.getJvmClassBundle();
		JvmClassInfo initialClassState = get(CLASS_NAME);

		// Create a inlining transformer with a custom resolver that recognizes the dynamic call sites in the fixture.
		InvokeDynamicResolver resolver = (context, currentWorkspace, classNode, method, instruction, frame) -> switch (instruction.name) {
			case "getStatic" -> resolved(Opcodes.H_GETSTATIC, CLASS_NAME, "answer", "I", false, 0);
			case "putStatic" -> resolved(Opcodes.H_PUTSTATIC, CLASS_NAME, "answer", "I", false, 0);
			case "getField" -> resolved(Opcodes.H_GETFIELD, CLASS_NAME, "value", "I", false, 1);
			case "putField" -> resolved(Opcodes.H_PUTFIELD, CLASS_NAME, "value", "I", false, 0);
			case "staticReference" -> resolved(Opcodes.H_INVOKESTATIC, CLASS_NAME, "targetReference",
					"(Ljava/lang/String;Ljava/lang/Integer;)Ljava/lang/String;", false, 0);
			case "virtualMethod" -> resolved(Opcodes.H_INVOKEVIRTUAL, CLASS_NAME, "targetVirtual", "(I)I", false, 1);
			case "unsupported" -> resolved(Opcodes.H_GETSTATIC, CLASS_NAME, "answer", "I", false, 2);
			default -> null;
		};
		InvokeDynamicInliningTransformer transformer = new InvokeDynamicInliningTransformer(recaf.get(InheritanceGraphService.class), List.of(resolver));

		// Set up the transformer and run it on the fixture class.
		JvmTransformerContext context = new JvmTransformerContext(workspace, resource, List.of(transformer));
		transformer.setup(context, workspace);
		transformer.transform(context, workspace, resource, bundle, initialClassState);

		// Get the transformed class and its example method, which contains all of the dynamic call sites.
		ClassNode transformed = context.getNode(bundle, initialClassState);
		MethodNode method = transformed.methods.stream()
				.filter(candidate -> candidate.name.equals("example"))
				.findFirst()
				.orElseThrow();

		// All recognized dynamic calls are gone, while the malformed resolution and standard lambda remain dynamic.
		List<InvokeDynamicInsnNode> remainingDynamic = instructionsOfType(method, InvokeDynamicInsnNode.class);
		assertEquals(2, remainingDynamic.size());
		assertTrue(remainingDynamic.stream().anyMatch(indy -> indy.name.equals("unsupported")));
		assertTrue(remainingDynamic.stream().anyMatch(indy -> indy.name.equals("lambda")));

		// Field and method handles become their corresponding direct JVM instructions.
		List<FieldInsnNode> fields = instructionsOfType(method, FieldInsnNode.class);
		assertTrue(fields.stream().anyMatch(field -> field.getOpcode() == Opcodes.GETSTATIC && field.name.equals("answer")));
		assertTrue(fields.stream().anyMatch(field -> field.getOpcode() == Opcodes.PUTSTATIC && field.name.equals("answer")));
		assertTrue(fields.stream().anyMatch(field -> field.getOpcode() == Opcodes.GETFIELD && field.name.equals("value")));
		assertTrue(fields.stream().anyMatch(field -> field.getOpcode() == Opcodes.PUTFIELD && field.name.equals("value")));
		List<MethodInsnNode> methods = instructionsOfType(method, MethodInsnNode.class);
		assertTrue(methods.stream().anyMatch(call -> call.name.equals("targetReference") && call.getOpcode() == Opcodes.INVOKESTATIC));
		assertTrue(methods.stream().anyMatch(call -> call.name.equals("targetVirtual") && call.getOpcode() == Opcodes.INVOKEVIRTUAL));

		// The reference argument is cast and the primitive argument is boxed before the static call.
		assertTrue(instructionsOfType(method, TypeInsnNode.class).stream()
				.anyMatch(cast -> cast.getOpcode() == Opcodes.CHECKCAST && cast.desc.equals("java/lang/String")));
		assertTrue(methods.stream().anyMatch(call -> call.owner.equals("java/lang/Integer")
				&& call.name.equals("valueOf")
				&& call.desc.equals("(I)Ljava/lang/Integer;")));

		// Every original argument, including the wide suffix, was stored in fresh locals.
		assertTrue(instructionsOfType(method, VarInsnNode.class).stream()
				.anyMatch(variable -> variable.getOpcode() == Opcodes.LSTORE));
		assertTrue(method.maxLocals > 0);
	}

	private void populateDynamicFixture(@Nonnull ClassNode node) {
		node.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "answer", "I", null, 7).visitEnd();
		node.visitField(Opcodes.ACC_PUBLIC, "value", "I", null, null).visitEnd();
		MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "example", "()V", null, null);

		// Static field get.
		method.instructions.add(new InvokeDynamicInsnNode("getStatic", "()I", customBootstrap()));
		method.instructions.add(new InsnNode(Opcodes.POP));

		// Static field put with a non-void call-site result, showing void-to-default handling.
		method.instructions.add(new IntInsnNode(Opcodes.BIPUSH, 1));
		method.instructions.add(new InvokeDynamicInsnNode("putStatic", "(I)I", customBootstrap()));
		method.instructions.add(new InsnNode(Opcodes.POP));

		// Instance field get with an ignored integer suffix.
		method.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
		method.instructions.add(new IntInsnNode(Opcodes.BIPUSH, 2));
		method.instructions.add(new InvokeDynamicInsnNode("getField", "(LExample;I)I", customBootstrap()));
		method.instructions.add(new InsnNode(Opcodes.POP));

		// Instance field put with a non-void call-site result.
		method.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
		method.instructions.add(new IntInsnNode(Opcodes.BIPUSH, 3));
		method.instructions.add(new InvokeDynamicInsnNode("putField", "(LExample;I)I", customBootstrap()));
		method.instructions.add(new InsnNode(Opcodes.POP));

		// Object-to-reference cast and primitive-to-wrapper boxing.
		method.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
		method.instructions.add(new IntInsnNode(Opcodes.BIPUSH, 4));
		method.instructions.add(new InvokeDynamicInsnNode("staticReference", "(Ljava/lang/Object;I)Ljava/lang/String;", customBootstrap()));
		method.instructions.add(new InsnNode(Opcodes.POP));

		// A wide suffix is stored and discarded while the leading receiver and argument are reloaded.
		method.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
		method.instructions.add(new IntInsnNode(Opcodes.BIPUSH, 5));
		method.instructions.add(new InsnNode(Opcodes.LCONST_0));
		method.instructions.add(new InvokeDynamicInsnNode("virtualMethod", "(LExample;IJ)I", customBootstrap()));
		method.instructions.add(new InsnNode(Opcodes.POP));

		// This site intentionally remains unresolved because its suffix count is invalid.
		method.instructions.add(new IntInsnNode(Opcodes.BIPUSH, 6));
		method.instructions.add(new InvokeDynamicInsnNode("unsupported", "(I)I", customBootstrap()));
		method.instructions.add(new InsnNode(Opcodes.POP));

		// Standard lambda like LambdaMetafactory sites are not owned by an obfuscator resolver and must remain dynamic.
		method.instructions.add(new InvokeDynamicInsnNode("lambda", "()Ljava/lang/Runnable;", Handles.META_FACTORY,
				Type.getMethodType("()V"), new Handle(Opcodes.H_INVOKESTATIC, CLASS_NAME, "lambdaBody", "()V", false),
				Type.getMethodType("()V")));
		method.instructions.add(new InsnNode(Opcodes.POP));
		method.instructions.add(new InsnNode(Opcodes.RETURN));
		method.maxStack = 5;
		method.maxLocals = 0;
		node.methods.add(method);

		// Emit the direct targets of the resolved call sites.
		MethodNode targetReference = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "targetReference",
				"(Ljava/lang/String;Ljava/lang/Integer;)Ljava/lang/String;", null, null);
		targetReference.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
		targetReference.instructions.add(new InsnNode(Opcodes.ARETURN));
		targetReference.maxStack = 1;
		node.methods.add(targetReference);

		MethodNode targetVirtual = new MethodNode(Opcodes.ACC_PUBLIC, "targetVirtual", "(I)I", null, null);
		targetVirtual.instructions.add(new InsnNode(Opcodes.ICONST_0));
		targetVirtual.instructions.add(new InsnNode(Opcodes.IRETURN));
		targetVirtual.maxStack = 1;
		node.methods.add(targetVirtual);

		MethodNode lambdaBody = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, "lambdaBody", "()V", null, null);
		lambdaBody.instructions.add(new InsnNode(Opcodes.RETURN));
		node.methods.add(lambdaBody);
	}

	@Nonnull
	private static InvokeDynamicResolver.ResolvedInvokeDynamic resolved(int tag, @Nonnull String owner,
	                                                                    @Nonnull String name, @Nonnull String desc,
	                                                                    boolean isInterface, int suffixCount) {
		return new InvokeDynamicResolver.ResolvedInvokeDynamic(new Handle(tag, owner, name, desc, isInterface), suffixCount);
	}

	@Nonnull
	private static Handle customBootstrap() {
		return new Handle(Opcodes.H_INVOKESTATIC, CLASS_NAME, "bootstrap",
				"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)"
						+ "Ljava/lang/invoke/CallSite;", false);
	}

	@Nonnull
	private static <T extends AbstractInsnNode> List<T> instructionsOfType(@Nonnull MethodNode method, @Nonnull Class<T> type) {
		List<T> result = new ArrayList<>();
		for (AbstractInsnNode instruction : method.instructions)
			if (type.isInstance(instruction))
				result.add(type.cast(instruction));
		return result;
	}
}
