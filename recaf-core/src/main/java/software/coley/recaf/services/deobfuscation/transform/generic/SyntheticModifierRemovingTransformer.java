package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import static org.objectweb.asm.Opcodes.ACC_BRIDGE;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;

/**
 * A transformer that removes synthetic and bridge access flags from classes and members.
 *
 * @author Matt Coley
 */
@Dependent
public class SyntheticModifierRemovingTransformer implements JvmClassTransformer {
	public static final String IDENTIFIER = "cleanup.syntheticstrip";

	@Override
	public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	                      @Nonnull JvmClassInfo initialClassState) throws TransformationException {
		ClassNode node = context.getNode(bundle, initialClassState);
		int classAccess = node.access;
		node.access &= ~ACC_SYNTHETIC;
		boolean dirty = classAccess != node.access;

		for (FieldNode field : node.fields) {
			int fieldAccess = field.access;
			field.access &= ~ACC_SYNTHETIC;
			dirty |= fieldAccess != field.access;
		}

		for (MethodNode method : node.methods) {
			int methodAccess = method.access;
			method.access &= ~(ACC_SYNTHETIC | ACC_BRIDGE);
			dirty |= methodAccess != method.access;
		}

		if (dirty)
			context.setNode(bundle, initialClassState, node);
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
				class Example {
				    // Empty class in decompiler.
				    // All members marked as synthetic by an obfuscator.
				}
				""";
	}

	@Nonnull
	@Override
	public String afterExample() {
		return """
				class Example {
					static final int hiddenField = 4;
				
				    static void hiddenMethod() {
				        // ...
				    }
				}
				""";
	}
}
