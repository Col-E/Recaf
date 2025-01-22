package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.annotation.Annotated;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.util.visitors.IllegalAnnotationRemovingVisitor;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * A transformer that removes any invalid annotations from classes and any of their declared members.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class IllegalAnnotationRemovingTransformer implements JvmClassTransformer {
	@Override
	public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	                      @Nonnull JvmClassInfo classInfo) throws TransformationException {
		// Only visit/transform if the class has annotations.
		if (hasAnnotations(classInfo)) {
			// Adapt the class bytes by removing any illegal annotation.
			ClassReader reader = classInfo.getClassReader();
			ClassWriter writer = new ClassWriter(reader, 0);
			IllegalAnnotationRemovingVisitor remover = new IllegalAnnotationRemovingVisitor(writer);
			reader.accept(remover, 0);
			if (remover.hasDetectedIllegalAnnotations()) // Should always occur given the circumstances.
				context.setBytecode(bundle, classInfo, writer.toByteArray());
		}
	}

	@Nonnull
	@Override
	public String name() {
		return "Illegal annotation removal";
	}

	private static boolean hasAnnotations(@Nonnull ClassInfo cls) {
		if (cls.allAnnotationsStream().findAny().isPresent())
			return true;
		return cls.fieldAndMethodStream()
				.flatMap(Annotated::allAnnotationsStream)
				.findAny()
				.isPresent();
	}
}
