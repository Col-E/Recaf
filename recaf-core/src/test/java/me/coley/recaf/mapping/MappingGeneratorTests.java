package me.coley.recaf.mapping;

import me.coley.recaf.TestUtils;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.graph.InheritanceGraph;
import me.coley.recaf.mapping.gen.MappingGenerator;
import me.coley.recaf.mapping.gen.NameGenerator;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for mapping generation.
 */
public class MappingGeneratorTests extends TestUtils {
	private static Workspace workspace;
	private static NameGenerator nameGenerator;

	@BeforeAll
	static void setup() throws IOException {
		nameGenerator = new NameGenerator() {
			@Override
			public String mapClass(CommonClassInfo info) {
				return "mapped/" + info.getName();
			}

			@Override
			public String mapField(CommonClassInfo owner, FieldInfo info) {
				return "mapped" + StringUtil.uppercaseFirstChar(info.getName());
			}

			@Override
			public String mapMethod(CommonClassInfo owner, MethodInfo info) {
				String name = info.getName();
				if (name.charAt(0) == '<')
					return name;
				return "mapped" + StringUtil.uppercaseFirstChar(name);
			}
		};
		workspace = createWorkspace(jarsDir.resolve("DemoGame.jar"));
		// Assert correct workspace contents
		Resource primary = workspace.getResources().getPrimary();
		assertNotNull(primary.getClasses().get("game/FxMain"));
	}

	@Test
	void testGeneral() {
		Resource primary = workspace.getResources().getPrimary();
		InheritanceGraph graph = new InheritanceGraph(workspace);
		MappingGenerator generator = new MappingGenerator(primary, graph);
		generator.setNameGenerator(nameGenerator);
		// Apply and assert no unexpected values exist
		Mappings mappings = generator.generate();
		if (mappings.getMappedClassName("java/lang/Object") != null ||
				mappings.getMappedClassName("java/lang/enum") != null)
			fail("Should not map core JVM class");
	}

	@Test
	@Disabled
	void testSkipsLibraryMembers() {
		// TODO: Use a sample that has a library dependency (not JavaFX)
		//  - assert that no 'inheritable' member has mapping from library classes
	}

	@Test
	@Disabled
	void testFilter() {
		// TODO: Use a filter to skip mapping in some cases
	}
}
