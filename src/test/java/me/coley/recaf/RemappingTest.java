package me.coley.recaf;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import me.coley.recaf.mapping.*;
import me.coley.recaf.workspace.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.ClassReader.*;

/**
 * Remapping tests.
 *
 * @author Matt
 */
public class RemappingTest extends Base {
	private JavaResource resource;
	private Workspace workspace;
	private File classMapFile;
	private File methodMapFile;
	private File methodEnigmaMapFile;

	@BeforeEach
	public void setup() {
		try {
			resource = new JarResource(getClasspathFile("inherit.jar"));
			workspace = new Workspace(resource);
			classMapFile = getClasspathFile("inherit-class-map.txt");
			/*
			test/Jedi 				-> 		rename/GoodGuy
			test/Sith				->		rename/BadGuy
			test/Greetings			->		rename/Hello
			 */
			methodMapFile = getClasspathFile("inherit-method-map.txt");
			methodEnigmaMapFile = getClasspathFile("inherit-method-map-enigma.txt");
			/*
			test/Greetings			->		rename/Hello
			test/Greetings.say()V	->		rename/Hello.speak()
			 */
		} catch(IOException ex) {
			fail(ex);
		}
	}

	@Test
	public void testRenamedClasses() {
		try {
			Mappings mappings = new SimpleMappings(classMapFile);
			mappings.accept(resource).entrySet().forEach(e -> {
				ClassReader reader = new ClassReader(e.getValue());
				String old = e.getKey();
				String rename = reader.getClassName();
				String renameSuper = reader.getSuperName();
				switch(old) {
					case "test/Jedi":
						assertEquals("rename/GoodGuy", rename);
						assertEquals("test/Person", renameSuper);
						break;
					case "test/Sith":
						assertEquals("rename/BadGuy", rename);
						assertEquals("test/Person", renameSuper);
						break;
					case "test/Greetings":
						assertEquals("rename/Hello", rename);
						break;
					case "test/Yoda":
						// Class not explicitly renamed, but has renamed references
						assertEquals("test/Yoda", rename);
						assertEquals("rename/GoodGuy", renameSuper);
						break;
					case "test/Person":
						// Class not explicitly renamed, but has renamed references
						String[] interfaces = reader.getInterfaces();
						assertTrue(interfaces.length == 1);
						assertEquals("rename/Hello", interfaces[0]);
						assertEquals("test/Person", rename);
						break;
					default:
						// No other values should have been updated
						fail("No other values should have been updated by renaming: " + old +
								" -> " + rename);
				}
			});
		} catch(IOException ex) {
			fail(ex);
		}
	}

	@Test
	public void testRenamedMethod() {
		try {
			Mappings mappings = new SimpleMappings(methodMapFile);
			mappings.accept(resource).entrySet().forEach(e -> {
				ClassReader reader = new ClassReader(e.getValue());
				ClassNode node = new ClassNode();
				reader.accept(node, SKIP_DEBUG | SKIP_CODE);
				String old = e.getKey();
				String rename = reader.getClassName();
				switch(old) {
					case "test/Jedi":
					case "test/Sith":
					case "test/Yoda":
						// Method should be renamed from "say" to "speak"
						assertEquals("speak", node.methods.get(1).name);
						break;
					case "test/Greetings":
						// Class was renamed
						assertEquals("rename/Hello", rename);
						// Method should be renamed from "say" to "speak"
						assertEquals("speak", node.methods.get(0).name);
						break;
					case "test/Person":
						// Class not explicitly renamed, but has renamed references
						String[] interfaces = reader.getInterfaces();
						assertTrue(interfaces.length == 1);
						assertEquals("rename/Hello", interfaces[0]);
						assertEquals("test/Person", rename);
						break;
					default:
						// No other values should have been updated
						fail("No other values should have been updated by renaming: " + old +
								" -> " + rename);
				}
			});
		} catch(IOException ex) {
			fail(ex);
		}
	}

	@Test
	public void testResourceKeys() {
		try {
			Map<String, byte[]> classes = resource.getClasses();
			// Before
			assertFalse(classes.containsKey("rename/GoodGuy"));
			assertFalse(classes.containsKey("rename/BadGuy"));
			assertFalse(classes.containsKey("rename/Hello"));
			assertTrue(classes.containsKey("test/Jedi"));
			assertTrue(classes.containsKey("test/Sith"));
			assertTrue(classes.containsKey("test/Greetings"));
			// After
			Mappings mappings = new SimpleMappings(classMapFile);
			mappings.accept(resource);
			assertTrue(classes.containsKey("rename/GoodGuy"));
			assertTrue(classes.containsKey("rename/BadGuy"));
			assertTrue(classes.containsKey("rename/Hello"));
			assertFalse(classes.containsKey("test/Jedi"));
			assertFalse(classes.containsKey("test/Sith"));
			assertFalse(classes.containsKey("test/Greetings"));
		} catch(IOException ex) {
			fail(ex);
		}
	}

	@Test
	public void testEngimaMappings() {
		try {
			// Both of these files outline the same data, just in different formats
			Mappings mappingsSimple = new SimpleMappings(methodMapFile);
			Mappings mappingsEnigma = new EnigmaMappings(methodEnigmaMapFile);
			// So their parsed values should be the same.
			MapDifference<String, String> difference =
					Maps.difference(mappingsSimple.getMappings(), mappingsEnigma.getMappings());
			assertTrue(difference.areEqual());
		} catch(IOException ex) {
			fail(ex);
		}
	}
}
