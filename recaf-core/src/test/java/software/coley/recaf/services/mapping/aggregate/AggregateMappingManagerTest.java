package software.coley.recaf.services.mapping.aggregate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.workspace.model.EmptyWorkspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for {@link AggregateMappingManager}
 *
 * @see AggregateMappingsTest
 */
public class AggregateMappingManagerTest extends TestBase {
	AggregateMappingManager aggregateMappingManager;

	@BeforeEach
	void setupPerTest() {
		workspaceManager.setCurrent(EmptyWorkspace.get());
		aggregateMappingManager = recaf.get(AggregateMappingManager.class);
	}

	@AfterEach
	void cleanupPerTest() {
		workspaceManager.closeCurrent();
	}

	@Test
	void testRefactorIntFieldWithManager() {
		// Setup renaming a class 3 times, and a simple int field
		IntermediateMappings mappings1 = new IntermediateMappings();
		IntermediateMappings mappings2 = new IntermediateMappings();
		IntermediateMappings mappings3 = new IntermediateMappings();

		// 'a' renamed to 'b'
		mappings1.addClass("a", "b");

		// 'b' renamed to 'c', so 'a' is now 'c'
		// but also rename the field 'oldName' to 'newName'
		mappings2.addClass("b", "c");
		mappings2.addField("b", "I", "oldName", "newName");

		// 'c' renamed to 'd'
		// but also rename the field from before to 'brandNewName'
		mappings3.addClass("c", "d");
		mappings3.addField("c", "I", "newName", "brandNewName");

		// Get aggregate instance from manager
		AggregatedMappings aggregated = aggregateMappingManager.getAggregatedMappings();
		assertNotNull(aggregated);

		// Validate after first mapping pass
		aggregateMappingManager.updateAggregateMappings(mappings1);
		assertEquals("b", aggregated.getMappedClassName("a"));

		// Validate after second mapping pass
		aggregateMappingManager.updateAggregateMappings(mappings2);
		assertEquals("c", aggregated.getMappedClassName("a"));
		assertEquals("newName", aggregated.getMappedFieldName("a", "oldName", "I"));

		// Validate after third mapping pass
		aggregateMappingManager.updateAggregateMappings(mappings3);
		assertEquals("d", aggregated.getMappedClassName("a"));
		assertEquals("brandNewName", aggregated.getMappedFieldName("a", "oldName", "I"));
	}

	@Test
	void testRefactorGetInstanceWithManager() {
		// Setup renaming a class 3 times, and a 'getInstance()' sort of method
		IntermediateMappings mappings1 = new IntermediateMappings();
		IntermediateMappings mappings2 = new IntermediateMappings();
		IntermediateMappings mappings3 = new IntermediateMappings();

		// 'a' renamed to 'b'
		mappings1.addClass("a", "b");

		// 'b' renamed to 'c', so 'a' is now 'c'
		// but also rename the method 'obf' to 'factory'
		mappings2.addClass("b", "c");
		mappings2.addMethod("b", "()Lb;", "obf", "factory");

		// 'c' renamed to 'd'
		// but also rename the method from before to 'getInstance'
		mappings3.addClass("c", "d");
		mappings3.addMethod("c", "()Lc;", "factory", "getInstance");

		// Get aggregate instance from manager
		AggregatedMappings aggregated = aggregateMappingManager.getAggregatedMappings();
		assertNotNull(aggregated);

		// Validate after first mapping pass
		aggregateMappingManager.updateAggregateMappings(mappings1);
		assertEquals("b", aggregated.getMappedClassName("a"));

		// Validate after second mapping pass
		aggregateMappingManager.updateAggregateMappings(mappings2);
		assertEquals("c", aggregated.getMappedClassName("a"));
		assertEquals("factory", aggregated.getMappedMethodName("a", "obf", "()La;"));

		// Validate after third mapping pass
		aggregateMappingManager.updateAggregateMappings(mappings3);
		assertEquals("d", aggregated.getMappedClassName("a"));
		assertEquals("getInstance", aggregated.getMappedMethodName("a", "obf", "()La;"));
	}
}
