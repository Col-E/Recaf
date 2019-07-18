package me.coley.recaf;

import me.coley.recaf.workspace.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for resource history.
 *
 * @author Matt
 */
public class HistoryTest extends Base {
	private JavaResource resource;

	@BeforeEach
	public void setup() {
		try {
			resource = new JarResource(getClasspathFile("inherit.jar"));
			resource.getClasses();
			resource.getResources();
		} catch(IOException ex) {
			fail(ex);
		}
	}

	@Test
	public void testClassCreateSave(){
		String key = "test/Yoda";
		resource.createClassSave(key);
		assertEquals(1, resource.getClassHistory(key).size());
		resource.createClassSave(key);
		assertEquals(2, resource.getClassHistory(key).size());
	}

	@Test
	public void testClassRollback(){
		String key = "test/Yoda";
		byte[] initial = resource.getClasses().get(key);
		// Clean save-state
		resource.createClassSave(key);
		// Set class value to null
		resource.getClasses().put(key, null);
		// Rollback
		byte[] rolled = resource.getClassHistory(key).pop();
		// Assert pop'd value and value in class map are present and the same
		assertEquals(initial, rolled);
		assertEquals(initial, resource.getClasses().get(key));
	}

	@Test
	public void testResourceCreateSave(){
		String key = "file";
		byte[] initial = new byte[] {1, 2, 3, 4};
		resource.getResources().put(key, initial);
		resource.createResourceSave(key);
		assertEquals(1, resource.getResourceHistory(key).size());
		resource.createResourceSave(key);
		assertEquals(2, resource.getResourceHistory(key).size());
	}

	@Test
	public void testResourceRollback(){
		String key = "file";
		byte[] initial = new byte[] {1, 2, 3, 4};
		resource.getResources().put(key, initial);
		// Clean save-state
		resource.createResourceSave(key);
		// Set resource value to null
		resource.getResources().put(key, null);
		// Rollback
		byte[] rolled = resource.getResourceHistory(key).pop();
		// Assert pop'd value and value in class map are present and the same
		assertEquals(initial, rolled);
		assertEquals(initial, resource.getResources().get(key));
	}
}
