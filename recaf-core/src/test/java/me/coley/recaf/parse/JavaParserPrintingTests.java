package me.coley.recaf.parse;

import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionClassDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionFieldDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionMethodDeclaration;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.parse.jpimpl.RecafResolvedClassDeclaration;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.Resources;
import me.coley.recaf.workspace.resource.source.EmptyContentSource;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link JavaParserPrinting}
 */
public class JavaParserPrintingTests {

	@Test
	void testRecafBacked() {
		try {
			Workspace workspace = new Workspace(new Resources(new Resource(new EmptyContentSource())));
			WorkspaceTypeSolver solver = new WorkspaceTypeSolver(workspace);
			ClassInfo classInfo = workspace.getResources().getClass("java/lang/String");
			RecafResolvedClassDeclaration classDeclaration = new RecafResolvedClassDeclaration(solver, classInfo);
			ResolvedFieldDeclaration fieldDeclaration = classDeclaration.getField("hash");
			ResolvedMethodDeclaration methodDeclaration = classDeclaration.getDeclaredMethods().stream()
					.filter(m -> m.getName().equals("intern"))
					.findFirst().get();


			assertEquals("java/lang/String", JavaParserPrinting.getType(classDeclaration));
			assertEquals("I", JavaParserPrinting.getFieldDesc(fieldDeclaration));
			assertEquals("()Ljava/lang/String;", JavaParserPrinting.getMethodDesc(methodDeclaration));
		} catch (Exception ex) {
			fail(ex);
		}
	}

	@Test
	void testReflection() {
		try {
			Class<?> clazz = String.class;
			Field field = clazz.getDeclaredField("hash");
			Method method = clazz.getDeclaredMethod("intern");
			ReflectionClassDeclaration classDeclaration = new ReflectionClassDeclaration(clazz, null);
			ReflectionFieldDeclaration fieldDeclaration = new ReflectionFieldDeclaration(field, null);
			ReflectionMethodDeclaration methodDeclaration = new ReflectionMethodDeclaration(method, null);
			assertEquals("java/lang/String", JavaParserPrinting.getType(classDeclaration));
			assertEquals("I", JavaParserPrinting.getFieldDesc(fieldDeclaration));
			assertEquals("()Ljava/lang/String;", JavaParserPrinting.getMethodDesc(methodDeclaration));
		} catch (Exception ex) {
			fail(ex);
		}
	}
}
