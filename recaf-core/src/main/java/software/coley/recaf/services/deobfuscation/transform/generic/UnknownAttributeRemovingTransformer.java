package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.RecordComponentNode;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.util.visitors.UnknownAttributeRemovingVisitor;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * A transformer that removes unknown attributes.
 *
 * @author Matt Coley
 */
@Dependent
public class UnknownAttributeRemovingTransformer implements JvmClassTransformer {
	@Override
	public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	                      @Nonnull JvmClassInfo initialClassState) throws TransformationException {
		if (context.isNode(bundle, initialClassState)) {
			ClassNode node = context.getNode(bundle, initialClassState);
			if (node.attrs != null)
				node.attrs.clear();
			for (FieldNode field : node.fields)
				if (field.attrs != null)
					field.attrs.clear();
			for (MethodNode method : node.methods)
				if (method.attrs != null)
					method.attrs.clear();
			if (node.recordComponents != null)
				for (RecordComponentNode recordComponent : node.recordComponents)
					if (recordComponent.attrs != null)
						recordComponent.attrs.clear();
		} else {
			ClassReader reader = new ClassReader(context.getBytecode(bundle, initialClassState));
			ClassWriter writer = new ClassWriter(reader, 0);
			reader.accept(new UnknownAttributeRemovingVisitor(writer), 0);
			context.setBytecode(bundle, initialClassState, writer.toByteArray());
		}
	}

	@Nonnull
	@Override
	public String name() {
		return "Unknown attribute removal";
	}
}
