package me.coley.recaf.parse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import me.coley.recaf.Controller;
import me.coley.recaf.presentation.EmptyPresentation;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resources;
import me.coley.recaf.workspace.resource.RuntimeResource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Tests for {@link JavaParserHelper}
 */
@Execution(ExecutionMode.SAME_THREAD)
public class JavaParserGenericFilterTests {

	private static Controller controller;
	private static Method filterGenerics;

	@BeforeAll
	static void setup() {
		Workspace workspace = new Workspace(new Resources(RuntimeResource.get()));
		controller = new Controller(new EmptyPresentation());
		controller.setWorkspace(workspace);
		try {
			filterGenerics = JavaParserHelper.class.getDeclaredMethod("filterGenerics", String.class);
			filterGenerics.setAccessible(true);
		} catch (Exception e) {
			fail("Failed to access private method 'filterGenerics'", e);
		}
	}
	
	private static String filterGeneric(String source) {
		try {
			return (String) filterGenerics.invoke(JavaParserHelper.create(controller), source);
		} catch (Exception e) {
			fail("Failed to invoke private method 'filterGenerics'", e);
			return null;
		}
	}


	@Test
	void removeGenericInitialisation() {
		String code = "class Clazz {\n"
						  + "    public void test() {\n"
						  + "        final Map<String, byte[]> content = new HashMap<String, byte[]>();\n"
						  + "    }\n"
						  + "}";
		var res = filterGeneric(code);
		assertEquals("class Clazz {\n"
						 + "    public void test() {\n"
						 + "        final Map                 content = new HashMap                ();\n"
						 + "    }\n"
						 + "}", res);
	}


	@Test
	void removeGenericMethodDeclaration() {
		String code = "<T extends EntityLivingBase<X> & Foo> foo() {}";
		var res = filterGeneric(code);
		assertEquals("                                      foo() {}", res);
	}

	@Test
	void removeGenericInitialisationNonAscii() {
		String code = "class Clazz {\n"
						  + "    public void test() {\n"
						  + "        final バグ<カッコウ, カッコウ[]> カッコウ = new バグ<カッコウ, カッコウ[]>();\n"
						  + "    }\n"
						  + "}";
		var res = filterGeneric(code);
		assertEquals("class Clazz {\n"
						 + "    public void test() {\n"
						 + "        final バグ               カッコウ = new バグ              ();\n"
						 + "    }\n"
						 + "}", res);
	}

	@Test
	void removeGenericShouldNotTouchComparison() {
		String code = "class Clazz {\n"
						  + "   public boolean test(int x) {\n"
						  + "      int a = 0, b = 0;\n"
						  + "      if (a < x && x > b) {\n"
						  + "          return false;\n"
						  + "      }\n"
						  + "      return true;\n"
						  + "   }\n"
						  + "}";
		var res = filterGeneric(code);
		assertEquals(code, res);
	}


	@Test
	void removeGenericShouldIgnoreChar() {
		String code = "class Clazz {\n"
						  + "    private static String trim(final String item) {\n"
						  + "        return ('<' > 0) ? \"false\" : \"true\";\n"
						  + "    }\n"
						  + "}";
		var res = filterGeneric(code);
		assertEquals("class Clazz {\n"
						 + "    private static String trim(final String item) {\n"
						 + "        return ('<' > 0) ? \"false\" : \"true\";\n"
						 + "    }\n"
						 + "}", res);
	}

	@Test
	void removeGenericShouldIgnoreString() {
		String code = "class Clazz {\n"
						  + "    private static String trim(final String item) {\n"
						  + "        return (\"'<'\".length() > 0) ? \"false\" : \"true\";\n"
						  + "    }\n"
						  + "}";
		var res = filterGeneric(code);
		assertEquals("class Clazz {\n"
						 + "    private static String trim(final String item) {\n"
						 + "        return (\"'<'\".length() > 0) ? \"false\" : \"true\";\n"
						 + "    }\n"
						 + "}", res);
	}

	@Test
	void removeGenericMultiline() {
		String code = "public class FilePane extends BorderPane\n"
						  + "{\n"
						  + "    private final TreeView<String> tree;\n"
						  + "    private Input input;\n"
						  + "\n"
						  + "    public FilePane() {\n"
						  + "        this.tree = new TreeView<String>();\n"
						  + "        Bus.subscribe(this);\n"
						  + "        this.setCenter(this.tree);\n"
						  + "        this.tree.setOnDragOver(this::lambda$new$0);\n"
						  + "        this.tree.setOnMouseClicked(this::lambda$new$1);\n"
						  + "        this.tree.setOnDragDropped(FilePane::lambda$new$2);\n"
						  + "        this.tree.setShowRoot(false);\n"
						  + "        this.tree.setCellFactory(this::lambda$new$3);\n"
						  + "        Bus.subscribe(this);\n"
						  + "        final TreeView<String> tree = this.tree;\n"
						  + "        Objects.requireNonNull(tree);\n"
						  + "        Threads.runFx(tree::requestFocus);\n"
						  + "    }\n"
						  + "}\n";
		var res = filterGeneric(code);
		assertEquals("public class FilePane extends BorderPane\n"
						 + "{\n"
						 + "    private final TreeView         tree;\n"
						 + "    private Input input;\n"
						 + "\n"
						 + "    public FilePane() {\n"
						 + "        this.tree = new TreeView        ();\n"
						 + "        Bus.subscribe(this);\n"
						 + "        this.setCenter(this.tree);\n"
						 + "        this.tree.setOnDragOver(this::lambda$new$0);\n"
						 + "        this.tree.setOnMouseClicked(this::lambda$new$1);\n"
						 + "        this.tree.setOnDragDropped(FilePane::lambda$new$2);\n"
						 + "        this.tree.setShowRoot(false);\n"
						 + "        this.tree.setCellFactory(this::lambda$new$3);\n"
						 + "        Bus.subscribe(this);\n"
						 + "        final TreeView         tree = this.tree;\n"
						 + "        Objects.requireNonNull(tree);\n"
						 + "        Threads.runFx(tree::requestFocus);\n"
						 + "    }\n"
						 + "}\n", res);
	}

	@Test
	void removeGenericClassDeclaration() {
		String code = "public class FileTreeItem extends TreeItem<String> implements Comparable<String> {}";
		var res = filterGeneric(code);
		assertEquals("public class FileTreeItem extends TreeItem         implements Comparable         {}", res);
	}

	@Test
	void removeGenericComplexClassDeclaration() {
		String code = "public class FileTreeItem<T super X> extends TreeItem<T> implements Comparable<T> {}";
		var res = filterGeneric(code);
		assertEquals("public class FileTreeItem            extends TreeItem    implements Comparable    {}", res);
	}

	@Test
	void removeGenericAmbiguousBooleanExpr() {
		String code = "    public static void main(String[] args) {\n"
						  + "        int a = 0, b = 0, c = 0, d = 0;\n"
						  + "        boolean x = a < b & c > d;\n"
						  + "    }";
		var res = filterGeneric(code);
		assertEquals(code, res);
	}

	@Test
	void dontRemoveGenericInString() {
		String code = "String x = \"Foo<X>\";";
		var res = filterGeneric(code);
		assertEquals(code, res);
	}
}
