package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.properties.builtin.ThrowableProperty;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceGraphService;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.util.visitors.SkippingClassVisitor;
import software.coley.recaf.util.visitors.SkippingMethodVisitor;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.HashSet;
import java.util.Set;

/**
 * A transformer that collects information about exceptions in the workspace.
 *
 * @author Matt Coley
 */
@Dependent
public class ExceptionCollectionTransformer implements JvmClassTransformer, Opcodes {
	private final Set<String> thrownExceptions = new HashSet<>();
	private final InheritanceGraphService graphService;
	private InheritanceGraph inheritanceGraph;

	@Inject
	public ExceptionCollectionTransformer(@Nonnull InheritanceGraphService graphService) {
		this.graphService = graphService;

		thrownExceptions.add("java/lang/Throwable");
		thrownExceptions.add("java/lang/Exception");
	}

	@Override
	public void setup(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace) {
		inheritanceGraph = graphService.getOrCreateInheritanceGraph(workspace);
	}

	@Override
	public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	                      @Nonnull JvmClassInfo initialClassState) throws TransformationException {
		initialClassState.getClassReader().accept(new SkippingClassVisitor() {
			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
				return new SkippingMethodVisitor() {
					@Override
					public void visitTypeInsn(int opcode, String type) {
						// Collect "new T" where "T" is a throwable type.
						if (opcode != NEW)
							return;
						ClassPathNode path = workspace.findClass(false, type);
						if (path == null)
							return;
						if (ThrowableProperty.get(path.getValue()) || inheritanceGraph.isAssignableFrom("java/lang/Throwable", type)) {
							// The constructed type is an exception type,
							// so we should add it and all parents to the known thrown types.
							ClassInfo exInfo = path.getValue();
							while (thrownExceptions.add(exInfo.getName()) && exInfo.getSuperName() != null) {
								path = workspace.findClass(false, exInfo.getSuperName());
								if (path == null)
									break;
							}
						}
					}
				};
			}
		}, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
	}

	@Nonnull
	@Override
	public String name() {
		return "Exception metadata collection";
	}

	/**
	 * @return Set of all exception types thrown in code defined in the workspace.
	 */
	@Nonnull
	public Set<String> getThrownExceptions() {
		return thrownExceptions;
	}
}
