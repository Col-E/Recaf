package software.coley.recaf.services.deobfuscation.transform.specific.dasho;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.transform.CollectionTransformer;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.List;

import static org.objectweb.asm.Opcodes.ICONST_5;
import static org.objectweb.asm.Opcodes.IRETURN;

/**
 * A transformer that replaces DashO decryption seed value functions with a constant value.
 * <p>
 * In practice the values are always positive, so we can just return a positive constant.
 * This solves the issue of the unseeded use of {@code Random} being non-deterministic
 * making it impossible for transformers to reliably decrypt strings and other obfuscated values.
 *
 * @author Matt Coley
 */
@Dependent
public class DashOpaqueSeedFoldingTransformer implements JvmClassTransformer, CollectionTransformer {
	public static final String IDENTIFIER = "specific.dasho.opaqueseed";

	@Override
	public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	                      @Nonnull JvmClassInfo initialClassState) throws TransformationException {
		// The generated DashO class has two methods:
		// - Seed supplier
		// - String decryptor
		List<MethodMember> methods = initialClassState.getMethods();
		if (methods.size() != 2)
			return;

		// Must have one method that is the seed getter.
		if (methods.stream().noneMatch(m -> m.hasStaticModifier() && m.hasPublicModifier() && m.getDescriptor().equals("()I")))
			return;

		// Must have one method that is the string decryptor.
		if (methods.stream().noneMatch(m -> m.hasStaticModifier() && m.hasPublicModifier() && m.getDescriptor().matches("\\(.+\\)Ljava/lang/String;")))
			return;

		// Take the seed supplier and fold its return value.
		ClassNode node = context.getNode(bundle, initialClassState);
		for (MethodNode method : node.methods) {
			if (!method.desc.equals("()I"))
				continue;

			// Skip if abstract.
			InsnList instructions = method.instructions;
			if (instructions == null)
				continue;

			// Sanity check existence of initial pattern.
			//  new Random().nextInt(X) + 1
			boolean found = false;
			for (AbstractInsnNode insn : instructions) {
				if (insn instanceof MethodInsnNode min) {
					if (min.owner.equals("java/util/Random") && min.name.equals("nextInt") && min.desc.equals("(I)I")) {
						found = true;
						break;
					}
				}
			}
			if (!found)
				continue;

			// Just make it return a positive number.
			// All observed usages assume the value is positive and use opaque predicates such as:
			//  n * X % n != 0
			// Where X is some constant and n is the seed value.
			if (method.tryCatchBlocks != null)
				method.tryCatchBlocks.clear();
			instructions.clear();
			instructions.add(new InsnNode(ICONST_5));
			instructions.add(new InsnNode(IRETURN));

			// Update once we're done with this method.
			context.setNode(bundle, initialClassState, node);
			return;
		}
	}

	@Nonnull
	@Override
	public String identifier() {
		return IDENTIFIER;
	}

	@Nonnull
	@Override
	public String beforeExample() {
		return """
				// The obfuscator generates methods which generate a positive number >= 1.
				// Later, these int values are used as opaque predicates to determine if a string should be decrypted or not.
				static int seed() {
				    try {
				        return new Random().nextInt(100) + 1;
				    } catch (Exception e) {
				        return -1;
				    }
				}
				""";
	}

	@Nonnull
	@Override
	public String afterExample() {
		return """
				// The opaque predicate only relies on the number being positive, so we can just return a constant value.
				// This makes the other transformers see the seed value as constant rather than non-deterministic.
				static int seed() {
				    return 5;
				}
				""";
	}
}
