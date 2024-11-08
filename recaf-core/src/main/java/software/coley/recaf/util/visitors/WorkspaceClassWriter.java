package software.coley.recaf.util.visitors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import software.coley.recaf.services.inheritance.InheritanceGraph;

/**
 * Class writer that pulls inheritance information from a workspace via {@link InheritanceGraph}.
 *
 * @author Matt Coley
 */
public class WorkspaceClassWriter extends ClassWriter {
	private final InheritanceGraph graph;

	/**
	 * @param graph
	 * 		Graph to pull inheritance relations from.
	 * @param flags
	 * 		Writer flags.
	 */
	public WorkspaceClassWriter(@Nonnull InheritanceGraph graph, int flags) {
		this(graph, null, flags);
	}

	/**
	 * @param graph
	 * 		Graph to pull inheritance relations from.
	 * @param reader
	 * 		Reader to pre-populate the constant pool with. Speeds up writing process a bit.
	 * @param flags
	 * 		Writer flags.
	 */
	public WorkspaceClassWriter(@Nonnull InheritanceGraph graph, @Nullable ClassReader reader, int flags) {
		super(reader, flags);
		this.graph = graph;
	}

	@Override
	protected String getCommonSuperClass(String type1, String type2) {
		// Default assumption if a type isn't given.
		if (type1 == null || type2 == null)
			return "java/lang/Object";

		// Find common parent in workspace
		return graph.getCommon(type1, type2);
	}
}
