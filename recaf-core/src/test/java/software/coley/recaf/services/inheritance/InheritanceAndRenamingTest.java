package software.coley.recaf.services.inheritance;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.MappingApplier;
import software.coley.recaf.services.mapping.MappingApplierService;
import software.coley.recaf.services.mapping.MappingListeners;
import software.coley.recaf.services.mapping.MappingResults;
import software.coley.recaf.services.mapping.MappingsAdapter;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.BasicJvmClassBundle;

import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static org.objectweb.asm.Opcodes.ACC_INTERFACE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.V1_8;

/**
 * Tests for {@link InheritanceGraph} and interactions with {@link MappingApplier}.
 */
class InheritanceAndRenamingTest extends TestBase {
	private static final int CLASS_COUNT = 10;
	static MappingApplierService mappingApplierService;
	static InheritanceGraphService inheritanceGraphService;
	static MappingListeners mappingListeners;

	@BeforeAll
	static void setup() {
		inheritanceGraphService = recaf.get(InheritanceGraphService.class);
		mappingApplierService = recaf.get(MappingApplierService.class);
		mappingListeners = recaf.get(MappingListeners.class);
	}

	@Test
	void methodFamilySetIsSharedAcrossMappedOwners() {
		// Generate a bunch of classes: [I1, I2, I3, ..., IN] where each class implements the previous class in the list.
		//
		// class IN implements I(N-1) {
		//   public abstract void run();
		// }
		int familySize = 32;
		JvmClassInfo[] generatedClasses = IntStream.rangeClosed(1, familySize).mapToObj(i -> {
			String[] interfaces = i == 1 ? null : new String[]{"I" + (i - 1)};
			ClassWriter cw = new ClassWriter(0);
			cw.visit(V1_8, ACC_INTERFACE | ACC_ABSTRACT, "I" + i, null, "java/lang/Object", interfaces);
			MethodVisitor method = cw.visitMethod(ACC_PUBLIC | ACC_ABSTRACT, "run", "()V", null, null);
			method.visitEnd();
			return new JvmClassInfoBuilder(cw.toByteArray()).build();
		}).toList().toArray(JvmClassInfo[]::new);
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(generatedClasses));

		// Create inheritance graph and add mappings for all 'run' methods in the family.
		CountingInheritanceGraph inheritanceGraph = new CountingInheritanceGraph(workspace);
		IntermediateMappings mappings = new IntermediateMappings();
		for (int i = 1; i <= familySize; i++)
			mappings.addMethod("I" + i, "()V", "run", "renamed");

		// When we fill in the family sets, all mapped owners should share the same materialized set
		// rather than each mapped owner creating their own set. Saves a lot of memory and time.
		MappingsAdapter adapter = new MappingsAdapter(true, true);
		adapter.infillMethodFamilies(mappings, inheritanceGraph);

		// Verify only one family was materialized, and all the methods have mapped names.
		assertEquals(1, inheritanceGraph.familyLookupCount, "All mapped owners in one inheritance family should share a single materialized family set");
		for (int i = 1; i <= familySize; i++)
			assertEquals("renamed", adapter.getMappedMethodName("I" + i, "run", "()V"));
	}

	@Test
	void verifyLinearInterfaces() {
		JvmClassInfo[] generatedClasses = IntStream.rangeClosed(1, CLASS_COUNT).mapToObj(i -> {
			String[] interfaces = i == 1 ? null : new String[]{"I" + (i - 1)};
			ClassWriter cw = new ClassWriter(0);
			cw.visit(V1_8, ACC_INTERFACE, "I" + i, null, "java/lang/Object", interfaces);
			return new JvmClassInfoBuilder(cw.toByteArray()).build();
		}).toList().toArray(JvmClassInfo[]::new);

		// Create workspace + graph with the generated interfaces
		BasicJvmClassBundle classes = TestClassUtils.fromClasses(generatedClasses);
		Workspace workspace = TestClassUtils.fromBundle(classes);
		InheritanceGraph inheritanceGraph = inheritanceGraphService.newInheritanceGraph(workspace);
		inheritanceGraph.installMappingListener(mappingListeners);

		// Verify initial state
		for (int i = 1; i <= CLASS_COUNT; i++) {
			String name = "I" + i;
			InheritanceVertex vertex = inheritanceGraph.getVertex(name);
			assertNotNull(vertex, "Graph missing '" + name + "'");
		}

		// Remap classes
		IntermediateMappings mappings = new IntermediateMappings();
		for (int i = 1; i <= CLASS_COUNT; i++)
			mappings.addClass("I" + i, "R" + i);
		MappingResults results = mappingApplierService.inWorkspace(workspace).applyToPrimaryResource(mappings);
		results.apply();

		// Verify old classes are removed from the graph
		for (int i = 1; i <= CLASS_COUNT; i++) {
			String name = "I" + i;
			InheritanceVertex vertex = inheritanceGraph.getVertex(name);
			assertNull(vertex, "Graph contains pre-mapped '" + name + "'");
		}

		// Verify the new classes are added to the graph
		InheritanceVertex objectVertex = inheritanceGraph.getVertex("java/lang/Object");
		for (int i = 1; i <= CLASS_COUNT; i++) {
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

	/** Small graph extension for counting calls to methods. */
	private static class CountingInheritanceGraph extends InheritanceGraph {
		private int familyLookupCount;

		private CountingInheritanceGraph(Workspace workspace) {
			super(workspace);
		}

		@Override
		public Set<InheritanceVertex> getVertexFamily(@Nonnull String name, boolean includeObject) {
			familyLookupCount++;
			return super.getVertexFamily(name, includeObject);
		}
	}
}