package me.coley.recaf.code.parse;

import com.github.javaparser.symbolsolver.javassistmodel.JavassistClassDeclaration;
import com.github.javaparser.symbolsolver.javassistmodel.JavassistFieldDeclaration;
import com.github.javaparser.symbolsolver.javassistmodel.JavassistMethodDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionClassDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionFieldDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionMethodDeclaration;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link JavaParserPrinting}
 */
public class JavaParserPrintingTests {
	private static ClassPool pool = ClassPool.getDefault();

	@Test
	void testJavassist() {
		try {
			CtClass ctClass = pool.getCtClass("java.lang.String");
			CtField ctField = ctClass.getField("hash");
			CtMethod ctMethod = ctClass.getMethod("intern", "()Ljava/lang/String;");
			JavassistClassDeclaration classDeclaration = new JavassistClassDeclaration(ctClass, null);
			JavassistFieldDeclaration fieldDeclaration = new JavassistFieldDeclaration(ctField, null);
			JavassistMethodDeclaration methodDeclaration = new JavassistMethodDeclaration(ctMethod, null);
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
