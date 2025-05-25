package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.util.visitors.IllegalSignatureRemovingVisitor;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * A transformer that removes any invalid signatures from classes and any of their declared members.
 *
 * @author Matt Coley
 */
@Dependent
public class IllegalSignatureRemovingTransformer implements JvmClassTransformer {
	@Override
	public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	                      @Nonnull JvmClassInfo initialClassState) throws TransformationException {
		// Adapt the class bytes by removing any illegal signature.
		ClassReader reader = new ClassReader(context.getBytecode(bundle, initialClassState));
		ClassWriter writer = new ClassWriter(reader, 0);

		IllegalSignatureRemovingVisitor remover = new IllegalSignatureRemovingVisitor(writer);
		reader.accept(remover, 0);

		// If the visitor did work, update the class.
		if (remover.hasDetectedIllegalSignatures())
			context.setBytecode(bundle, initialClassState, writer.toByteArray());
	}

	@Nonnull
	@Override
	public String name() {
		return "Illegal signature removal";
	}
}
