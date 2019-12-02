package me.coley.recaf;

import me.coley.recaf.workspace.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the listening map used in {@link me.coley.recaf.workspace.JavaResource}.
 *
 * @author Matt
 */
public class ResourceUpdateTest extends Base {
	private JavaResource resource;
	private Workspace workspace;

	@BeforeEach
	public void setup() {
		resource = new DummyResource();
		workspace = new Workspace(resource);
	}

	@Test
	public void testClassPut() {
		String valueToPut = "Test";
		// Record values put in the map
		Set<String> putted = new HashSet<>();
		resource.getClasses().getPutListeners().add((name, code) -> putted.add(name));
		// Put value in map
		resource.getClasses().put(valueToPut, new byte[0]);
		// Assert value listener has been fired
		assertTrue(putted.contains(valueToPut));
		assertTrue(resource.getDirtyClasses().contains(valueToPut));
	}

	@Test
	public void testResourcePut() {
		String valueToPut = "Test";
		// Record values put in the map
		Set<String> putted = new HashSet<>();
		resource.getFiles().getPutListeners().add((name, data) -> putted.add(name));
		// Put value in map
		resource.getFiles().put(valueToPut, new byte[0]);
		// Assert value listener has been fired
		assertTrue(putted.contains(valueToPut));
		assertTrue(resource.getDirtyFiles().contains(valueToPut));
	}

	@Test
	public void testClassPutAll() {
		String valueToPut1 = "Test1";
		String valueToPut2 = "Test2";
		Map<String, byte[]> map = new HashMap<>();
		map.put(valueToPut1, new byte[0]);
		map.put(valueToPut2, new byte[0]);
		// Record values put in the map
		Set<String> putted = new HashSet<>();
		resource.getClasses().getPutListeners().add((name, code) -> putted.add(name));
		// Put value in map
		resource.getClasses().putAll(map);
		// Assert value listener has been fired
		assertTrue(putted.contains(valueToPut1));
		assertTrue(putted.contains(valueToPut2));
		assertTrue(resource.getDirtyClasses().contains(valueToPut1));
		assertTrue(resource.getDirtyClasses().contains(valueToPut2));

	}

	@Test
	public void testResourcePutAll() {
		String valueToPut1 = "Test1";
		String valueToPut2 = "Test2";
		Map<String, byte[]> map = new HashMap<>();
		map.put(valueToPut1, new byte[0]);
		map.put(valueToPut2, new byte[0]);
		// Record values put in the map
		Set<String> putted = new HashSet<>();
		resource.getFiles().getPutListeners().add((name, data) -> putted.add(name));
		// Put value in map
		resource.getFiles().putAll(map);
		// Assert value listener has been fired
		assertTrue(putted.contains(valueToPut1));
		assertTrue(putted.contains(valueToPut2));
		assertTrue(resource.getDirtyFiles().contains(valueToPut1));
		assertTrue(resource.getDirtyFiles().contains(valueToPut2));
	}

	@Test
	public void testClassRemove() {
		String valueToRemove = "Test";
		// Record values removed from the map
		Set<Object> removed = new HashSet<>();
		resource.getClasses().getRemoveListeners().add(removed::add);
		// Remove value from map
		resource.getClasses().remove(valueToRemove);
		// Assert value listener has been fired
		assertTrue(removed.contains(valueToRemove));
	}

	@Test
	public void testResourceRemove() {
		String valueToRemove = "Test";
		// Record values removed from the map
		Set<Object> removed = new HashSet<>();
		resource.getFiles().getRemoveListeners().add(removed::add);
		// Remove value from map
		resource.getFiles().remove(valueToRemove);
		// Assert value listener has been fired
		assertTrue(removed.contains(valueToRemove));
	}

	/**
	 * Empty resource that allows items to be added.
	 */
	private static class DummyResource extends EmptyResource {
		@Override
		protected Map<String, byte[]> loadClasses() throws IOException {
			return new HashMap<>();
		}

		@Override
		protected Map<String, byte[]> loadFiles() throws IOException {
			return new HashMap<>();
		}
	}
}
