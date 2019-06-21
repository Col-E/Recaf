package me.coley.recaf;

import me.coley.event.Bus;
import me.coley.recaf.event.ClassRenameEvent;
import me.coley.recaf.event.MethodRenameEvent;
import org.junit.jupiter.api.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Input class.
 *
 * @author Matt
 */
public class InputTest {
	private final static int CLASS_COUNT = 9;
	private Input input;

	@BeforeEach
	public void setup() throws IOException {
		ClassLoader classLoader = InputTest.class.getClassLoader();
		File file = new File(classLoader.getResource("inherit.jar").getFile());
		input = new Input(file);
	}

	@Test
	public void testGetEquality() {
		// They should alias to the same call.
		// The first call will generate a ClassNode from the raw backing map.
		// The second call will load the cached ClassNode from the first call.
		ClassNode c1 = input.getClasses().get("test/Yoda");
		ClassNode c2 = input.getClass("test/Yoda");
		assertEquals(c1, c2);
	}

	@Test
	public void testCacheInvalidation() {
		// Backing raw map will generate a ClassNode for this call
		ClassNode c1 = input.getClass("test/Yoda");
		// Remove the cached ClassNode
		input.getClasses().remove("test/Yoda");
		// Backing raw map will generate a new ClassNode for this call.
		ClassNode c2 = input.getClass("test/Yoda");
		// ClassNode instances do not match
		assertNotEquals(c1, c2);
	}

	@Test
	public void testSize() {
		// Technically, the map size is 0, but the backing map size should be 9.
		// Since we want to treat the classes map values as a sort of cache to work off of,
		// we want to get the backing map's size and then any time we need a value that isn't
		// in the map it will be generated based off of the raw backing map.
		assertEquals(CLASS_COUNT, input.getClasses().size());
	}

	@Test
	public void testEntrySet() {
		AtomicInteger count = new AtomicInteger();
		input.getClasses().entrySet().stream().forEach(e -> {
			ClassNode actual = e.getValue();
			ClassNode fetched = input.getClass(e.getKey());
			// Entry values should not be null
			assertNotNull(actual);
			assertNotNull(fetched);
			// get calls and the entry values should reference the same cached ClassNodes
			assertEquals(actual, fetched);
			// increment count
			count.incrementAndGet();
		});
		// Every item in the jar should be visited
		assertEquals(CLASS_COUNT, count.intValue());
	}

	// TODO: This test is flaky, is threading in the renaming process the issue?
	@Disabled
	@Test
	public void testClassRename() {
		// We're going to rename the super-class.
		// This cached copy should be invalidated after the rename.
		ClassNode yoda = input.getClass("test/Yoda");
		assertEquals("test/Jedi", yoda.superName);
		//
		// Invoke rename
		ClassNode jedi = input.getClass("test/Jedi");
		Bus.post(new ClassRenameEvent(jedi, "test/Jedi", "test/GoodGuy"));
		//
		// The number of classes should stay the same
		assertEquals(CLASS_COUNT, input.getClasses().size());
		// The old class should no longer exist
		assertNull(input.getClass("test/Jedi"));
		// The new class should exist
		assertNotNull(input.getClass("test/GoodGuy"));
		// Yoda's superName should be updated now.
		yoda = input.getClass("test/Yoda");
		assertEquals("test/GoodGuy", yoda.superName);
	}

	@Test
	public void testMethodRename() {
		ClassNode yoda = input.getClass("test/Yoda");
		MethodNode say = yoda.methods.get(1);
		//
		// Invoke rename
		Bus.post(new MethodRenameEvent(yoda, say, "say", "tell"));
		// All of these usages should be renamed
		assertEquals("tell", input.getClass("test/Greetings").methods.get(0).name);
		assertEquals("tell", input.getClass("test/Person").methods.get(1).name);
		assertEquals("tell", input.getClass("test/Jedi").methods.get(1).name);
		assertEquals("tell", input.getClass("test/Yoda").methods.get(1).name);
		assertEquals("tell", input.getClass("test/Sith").methods.get(1).name);
		// This should not change since it is not linked
		assertEquals("say", input.getClass("test/Speech").methods.get(0).name);
	}
}
