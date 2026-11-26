package software.coley.recaf.services.mapping.aggregate;

import org.junit.jupiter.api.Test;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.data.VariableMapping;
import software.coley.recaf.workspace.model.EmptyWorkspace;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link AggregatedMappings}
 *
 * @see AggregateMappingManagerTest
 */
public class AggregateMappingsTest {
	@Test
	void testRefactorIntField() {
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

		// Setup aggregation
		AggregatedMappings aggregated = new AggregatedMappings(EmptyWorkspace.get());
		// Validate after first mapping pass
		aggregated.update(mappings1);
		assertEquals("b", aggregated.getMappedClassName("a"));
		// Validate after second mapping pass
		aggregated.update(mappings2);
		assertEquals("c", aggregated.getMappedClassName("a"));
		assertEquals("newName", aggregated.getMappedFieldName("a", "oldName", "I"));
		// Validate after third mapping pass
		aggregated.update(mappings3);
		assertEquals("d", aggregated.getMappedClassName("a"));
		assertEquals("brandNewName", aggregated.getMappedFieldName("a", "oldName", "I"));
	}

	@Test
	void testRefactorGetInstance() {
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

		// Setup aggregation
		AggregatedMappings aggregated = new AggregatedMappings(EmptyWorkspace.get());
		// Validate after first mapping pass
		aggregated.update(mappings1);
		assertEquals("b", aggregated.getMappedClassName("a"));
		// Validate after second mapping pass
		aggregated.update(mappings2);
		assertEquals("c", aggregated.getMappedClassName("a"));
		assertEquals("factory", aggregated.getMappedMethodName("a", "obf", "()La;"));
		// Validate after third mapping pass
		aggregated.update(mappings3);
		assertEquals("d", aggregated.getMappedClassName("a"));
		assertEquals("getInstance", aggregated.getMappedMethodName("a", "obf", "()La;"));
	}

	@Test
	void testRefactorVariable() {
		IntermediateMappings mappings1 = new IntermediateMappings();
		IntermediateMappings mappings2 = new IntermediateMappings();

		// Create base mappings and then an update mappings.
		mappings1.addVariable("a", "foo", "(I)V", "I", "x", 1, "y");
		mappings1.addVariable("a", "bar", "(I)V", null, null, 1, "arg");
		mappings2.addVariable("a", "foo", "(I)V", "I", "y", 1, "z");
		mappings2.addVariable("a", "bar", "(I)V", null, null, 1, "renamedArg");

		// Aggregate baseline is the first mappings, then update with the second mappings.
		AggregatedMappings aggregated = new AggregatedMappings(EmptyWorkspace.get());
		aggregated.update(mappings1);

		// Validate that the first mappings are applied correctly.
		assertEquals("y", aggregated.getMappedVariableName("a", "foo", "(I)V", "x", "I", 1));
		assertEquals("arg", aggregated.getMappedVariableName("a", "bar", "(I)V", "anything", "I", 1));

		// validate that the second mappings are applied correctly.
		aggregated.update(mappings2);
		assertEquals("z", aggregated.getMappedVariableName("a", "foo", "(I)V", "x", "I", 1));
		assertEquals("renamedArg", aggregated.getMappedVariableName("a", "bar", "(I)V", "anything", "I", 1));
		assertEquals(1, aggregated.getMethodVariableMappings("a", "foo", "(I)V").size());
		assertEquals(1, aggregated.getMethodVariableMappings("a", "bar", "(I)V").size());
	}

	@Test
	void testRefactorVariableWithClassAndMethod() {
		IntermediateMappings mappings1 = new IntermediateMappings();
		IntermediateMappings mappings2 = new IntermediateMappings();

		// Same idea as the last test, but we also have mappings for the class and method they are defined in.
		mappings1.addClass("a", "b");
		mappings1.addMethod("a", "()La;", "foo", "bar");
		mappings1.addVariable("a", "foo", "()La;", "La;", "x", 1, "y");
		mappings2.addClass("b", "c");
		mappings2.addMethod("b", "()Lb;", "bar", "baz");
		mappings2.addVariable("b", "bar", "()Lb;", "Lb;", "y", 1, "z");

		// Aggregate baseline is the first mappings, then update with the second mappings.
		AggregatedMappings aggregated = new AggregatedMappings(EmptyWorkspace.get());
		aggregated.update(mappings1);
		assertEquals("b", aggregated.getMappedClassName("a"));
		assertEquals("bar", aggregated.getMappedMethodName("a", "foo", "()La;"));
		assertEquals("y", aggregated.getMappedVariableName("a", "foo", "()La;", "x", "La;", 1));

		// Validate that the second mappings are applied correctly.
		aggregated.update(mappings2);
		assertEquals("c", aggregated.getMappedClassName("a"));
		assertEquals("baz", aggregated.getMappedMethodName("a", "foo", "()La;"));
		assertEquals("z", aggregated.getMappedVariableName("a", "foo", "()La;", "x", "La;", 1));

		// Validate that the variable mapping is correct after the aggregation.
		VariableMapping variableMapping = aggregated.getMethodVariableMappings("a", "foo", "()La;").getFirst();
		assertEquals("x", variableMapping.getOldName());
		assertEquals("La;", variableMapping.getDesc());
		assertEquals(1, variableMapping.getIndex());
		assertEquals("z", variableMapping.getNewName());
	}
}
