package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.util.visitors.LongExceptionRemovingVisitor;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * A transformer that removes any long/annoying exceptions from methods.
 *
 * @author Matt Coley
 */
@Dependent
public class LongExceptionRemovingTransformer implements JvmClassTransformer {
	private static final int LONG_EXCEPTION = 150;

	@Override
	public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	                      @Nonnull JvmClassInfo initialClassState) throws TransformationException {
		// Adapt the class bytes by removing any stupidly long annotation.
		// - Do not pass the reader as a writer parameter, MethodWriter.canCopyMethodAttributes breaks this
		ClassReader reader = new ClassReader(context.getBytecode(bundle, initialClassState));
		ClassWriter writer = new ClassWriter(0);

		LongExceptionRemovingVisitor remover = new LongExceptionRemovingVisitor(writer, LONG_EXCEPTION);
		reader.accept(remover, initialClassState.getClassReaderFlags());

		// If the visitor did work, update the class.
		if (remover.hasDetectedLongExceptions())
			context.setBytecode(bundle, initialClassState, writer.toByteArray());
	}

	@Nonnull
	@Override
	public String name() {
		return "Long exception removal";
	}

	@Override
	public boolean pruneAfterNoWork() {
		// Other transformers should not introduce junk/long exceptions,
		// so once the work is done there is no need to re-process classes on following passes.
		return true;
	}
}
