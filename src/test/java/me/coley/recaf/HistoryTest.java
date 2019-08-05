package me.coley.recaf;

import me.coley.recaf.workspace.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for resource history.
 *
 * @author Matt
 */
public class HistoryTest extends Base {
	private final static byte[] DUMMY = new byte[] { 1, 2, 3, 4};
	private JavaResource resource;

	@BeforeEach
	public void setup() {
		try {
			resource = new JarResource(getClasspathFile("calc.jar"));
			resource.getClasses();
			resource.getResources();
		} catch(IOException ex) {
			fail(ex);
		}
	}

	@Test
	public void testClassHasInitialState(){
		String key = "Start";
		assertEquals(1, resource.getClassHistory(key).size());
	}

	@Test
	public void testResourceHasInitialState(){
		String key = "src/Start.java";
		assertEquals(1, resource.getResourceHistory(key).size());
	}

	@Test
	public void testClassCanAlwaysCanRollbackToInitial(){
		String key = "Start";
		for (int i = 0; i < 5; i++) {
			assertEquals(1, resource.getClassHistory(key).size());
			resource.getClassHistory(key).pop();
		}
	}

	@Test
	public void testResourceCanAlwaysCanRollbackToInitial(){
		String key = "src/Start.java";
		for (int i = 0; i < 5; i++) {
			assertEquals(1, resource.getResourceHistory(key).size());
			resource.getResourceHistory(key).pop();
		}
	}

	@Test
	public void testNewClassHasInitialState(){
		// Inserting a class not already in the workspace should create an initial state
		String key = "NewClass";
		resource.getClasses().put(key, DUMMY);
		assertEquals(1, resource.getClassHistory(key).size());
	}

	@Test
	public void testNewResourceHasInitialState(){
		// Inserting a class not already in the workspace should create an initial state
		String key = "src/NewClass.java";
		resource.getClasses().put(key, DUMMY);
		assertEquals(1, resource.getClassHistory(key).size());
	}

	@Test
	public void testClassCreateSave(){
		String key = "Start";
		// Create additional history entries
		resource.createClassSave(key);
		assertEquals(2, resource.getClassHistory(key).size());
		resource.createClassSave(key);
		assertEquals(3, resource.getClassHistory(key).size());
	}

	@Test
	public void testResourceCreateSave(){
		String key = "src/Start.java";
		// Create additional history entries
		resource.createResourceSave(key);
		assertEquals(2, resource.getResourceHistory(key).size());
		resource.createResourceSave(key);
		assertEquals(3, resource.getResourceHistory(key).size());
	}

	@Test
	public void testClassRollback(){
		String key = "Start";
		byte[] initial = resource.getClassHistory(key).peek();
		resource.getClasses().put(key, DUMMY);
		resource.createClassSave(key);
		// Rollback and assert pop'd value and value in class map are present and the same
		assertArrayEquals(DUMMY, resource.getClassHistory(key).pop());
		assertArrayEquals(initial, resource.getClassHistory(key).pop());
	}

	@Test
	public void testResourceRollback(){
		String key = "src/Start.java";
		byte[] initial = resource.getResourceHistory(key).peek();
		resource.getResources().put(key, DUMMY);
		resource.createResourceSave(key);
		// Rollback and assert pop'd value and value in class map are present and the same
		assertArrayEquals(DUMMY, resource.getResourceHistory(key).pop());
		assertArrayEquals(initial, resource.getResourceHistory(key).pop());
	}
}
