package software.coley.recaf.services.inheritance;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.MappingApplier;
import software.coley.recaf.services.mapping.MappingApplierService;
import software.coley.recaf.services.mapping.MappingResults;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.BasicJvmClassBundle;

import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.ACC_INTERFACE;
import static org.objectweb.asm.Opcodes.V1_8;

/**
 * Tests for {@link InheritanceGraph} and interactions with {@link MappingApplier}.
 */
class InheritanceAndRenamingTest extends TestBase {
	static Workspace workspace;
	static InheritanceGraph inheritanceGraph;
	static MappingApplierService mappingApplierService;
	static JvmClassInfo[] generatedClasses;

	@BeforeAll
	static void setup() {
		generatedClasses = IntStream.rangeClosed(1, 5).mapToObj(i -> {
			String[] interfaces = i == 1 ? null : new String[]{"I" + (i - 1)};
			ClassWriter cw = new ClassWriter(0);
			cw.visit(V1_8, ACC_INTERFACE, "I" + i, null, "java/lang/Object", interfaces);
			return new JvmClassInfoBuilder(cw.toByteArray()).build();
		}).toList().toArray(JvmClassInfo[]::new);

		// Create workspace with the inheritance classes
		BasicJvmClassBundle classes = TestClassUtils.fromClasses(generatedClasses);
		workspace = TestClassUtils.fromBundle(classes);
		workspaceManager.setCurrent(workspace);

		// Get graph
		inheritanceGraph = recaf.get(InheritanceGraphService.class).getCurrentWorkspaceInheritanceGraph();
		inheritanceGraph.toString(); // Force immediate init.

		// Get mapping applier
		mappingApplierService = recaf.get(MappingApplierService.class);
	}

	@Test
	void test() {
		// Verify initial state
		for (int i = 1; i <= 5; i++) {
			String name = "I" + i;
			InheritanceVertex vertex = inheritanceGraph.getVertex(name);
			assertNotNull(vertex, "Graph missing '" + name + "'");
		}

		// Remap classes
		IntermediateMappings mappings = new IntermediateMappings();
		for (int i = 1; i <= 5; i++)
			mappings.addClass("I" + i, "R" + i);
		MappingResults results = mappingApplierService.inCurrentWorkspace().applyToPrimaryResource(mappings);
		results.apply();

		// Very old classes are removed from the graph
		for (int i = 1; i <= 5; i++) {
			String name = "I" + i;
			InheritanceVertex vertex = inheritanceGraph.getVertex(name);
			assertNull(vertex, "Graph contains pre-mapped '" + name + "'");
		}

		// Verify the new classes are added to the graph
		InheritanceVertex objectVertex = inheritanceGraph.getVertex("java/lang/Object");
		for (int i = 1; i <= 5; i++) {
			String name = "R" + i;
			InheritanceVertex vertex = inheritanceGraph.getVertex(name);
			assertNotNull(vertex, "Graph missing post-mapped '" + name + "'");
			if (i > 1) {
				Set<InheritanceVertex> parents = vertex.getParents();
				Set<InheritanceVertex> allParents = vertex.getAllParents();
				int directParentCount = parents.size();
				int allParentCount = allParents.size();
				assertEquals(2, directParentCount, "Vertex R" + i + " should have 2 direct parents (extended interface R" + (i - 1) + " + java/lang/Object)");
				assertEquals(i, allParentCount, "Vertex R" + i + " should have " + i + " total (direct + transitive) parents");
			}
		}
	}
}