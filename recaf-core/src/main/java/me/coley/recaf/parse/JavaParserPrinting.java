package me.coley.recaf.parse;

import com.github.javaparser.symbolsolver.javassistmodel.*;
import com.github.javaparser.symbolsolver.reflectionmodel.*;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.bytecode.Descriptor;
import me.coley.recaf.util.logging.Logging;
import org.objectweb.asm.Type;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static me.coley.recaf.util.ReflectUtil.getDeclaredField;

/**
 * Utility for printing internal types/descriptors of items since these libraries tend to hide internal details to be
 * more source-level friendly and presentable.
 *
 * @author Matt Coley
 */
public class JavaParserPrinting {
	private static final Logger logger = Logging.get(JavaParserPrinting.class);
	private static Field javassistCtClass;
	private static Field javassistCtClassInterface;
	private static Field javassistCtClassAnnotation;
	private static Field javassistCtClassEnum;
	private static Field javassistCtField;
	private static Field javassistCtFieldEnum;
	private static Field javassistCtMethod;
	private static Field javassistCtMethodAnno;
	private static Field javassistCtMethodCtor;
	private static Field reflectionClass;
	private static Field reflectionClassInterface;
	private static Field reflectionClassAnnotation;
	private static Field reflectionClassEnum;
	private static Field reflectionField;
	private static Field reflectionFieldEnum;
	private static Field reflectionMethod;
	private static Field reflectionMethodAnno;
	private static Field reflectionMethodCtor;

	static {
		try {
			javassistCtClass = getDeclaredField(JavassistClassDeclaration.class, "ctClass");
			javassistCtClassInterface = getDeclaredField(JavassistInterfaceDeclaration.class, "ctClass");
			javassistCtClassAnnotation = getDeclaredField(JavassistAnnotationDeclaration.class, "ctClass");
			javassistCtClassEnum = getDeclaredField(JavassistEnumDeclaration.class, "ctClass");
			javassistCtField = getDeclaredField(JavassistFieldDeclaration.class, "ctField");
			javassistCtFieldEnum = getDeclaredField(JavassistEnumConstantDeclaration.class, "ctField");
			javassistCtMethod = getDeclaredField(JavassistMethodDeclaration.class, "ctMethod");
			javassistCtMethodAnno = getDeclaredField(JavassistAnnotationMemberDeclaration.class, "annotationMember");
			javassistCtMethodCtor = getDeclaredField(JavassistConstructorDeclaration.class, "ctConstructor");
			reflectionClass = getDeclaredField(ReflectionClassDeclaration.class, "clazz");
			reflectionClassInterface = getDeclaredField(ReflectionInterfaceDeclaration.class, "clazz");
			reflectionClassAnnotation = getDeclaredField(ReflectionAnnotationDeclaration.class, "clazz");
			reflectionClassEnum = getDeclaredField(ReflectionEnumDeclaration.class, "clazz");
			reflectionField = getDeclaredField(ReflectionFieldDeclaration.class, "field");
			reflectionFieldEnum = getDeclaredField(ReflectionEnumConstantDeclaration.class, "enumConstant");
			reflectionMethod = getDeclaredField(ReflectionMethodDeclaration.class, "method");
			reflectionMethodAnno = getDeclaredField(ReflectionAnnotationMemberDeclaration.class, "annotationMember");
			reflectionMethodCtor = getDeclaredField(ReflectionConstructorDeclaration.class, "constructor");
		} catch (ReflectiveOperationException ex) {
			// Should not occur unless API internals change in JavaParser
			logger.error("Failed to get internal name/descriptor accessors! Internal JavaParser API changed?", ex);
		}
	}

	/**
	 * @param clazz
	 * 		Type declaration.
	 *
	 * @return Internal type.
	 */
	public static String getType(JavassistClassDeclaration clazz) {
		try {
			String name = ((CtClass) javassistCtClass.get(clazz)).getName();
			return Descriptor.toJvmName(name);
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException("Failed to get internal type", ex);
		}
	}

	/**
	 * @param clazz
	 * 		Type declaration.
	 *
	 * @return Internal type.
	 */
	public static String getType(JavassistInterfaceDeclaration clazz) {
		try {
			String name = ((CtClass) javassistCtClassInterface.get(clazz)).getName();
			return Descriptor.toJvmName(name);
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException("Failed to get internal type", ex);
		}
	}

	/**
	 * @param clazz
	 * 		Type declaration.
	 *
	 * @return Internal type.
	 */
	public static String getType(JavassistAnnotationDeclaration clazz) {
		try {
			String name = ((CtClass) javassistCtClassAnnotation.get(clazz)).getName();
			return Descriptor.toJvmName(name);
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException("Failed to get internal type", ex);
		}
	}

	/**
	 * @param clazz
	 * 		Type declaration.
	 *
	 * @return Internal type.
	 */
	public static String getType(JavassistEnumDeclaration clazz) {
		try {
			String name = ((CtClass) javassistCtClassEnum.get(clazz)).getName();
			return Descriptor.toJvmName(name);
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException("Failed to get internal type", ex);
		}
	}

	/**
	 * @param clazz
	 * 		Type declaration.
	 *
	 * @return Internal type.
	 */
	public static String getType(ReflectionClassDeclaration clazz) {
		try {
			return Type.getInternalName((Class<?>) reflectionClass.get(clazz));
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException("Failed to get internal type", ex);
		}
	}

	/**
	 * @param clazz
	 * 		Type declaration.
	 *
	 * @return Internal type.
	 */
	public static String getType(ReflectionInterfaceDeclaration clazz) {
		try {
			return Type.getInternalName((Class<?>) reflectionClassInterface.get(clazz));
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException("Failed to get internal type", ex);
		}
	}

	/**
	 * @param clazz
	 * 		Type declaration.
	 *
	 * @return Internal type.
	 */
	public static String getType(ReflectionAnnotationDeclaration clazz) {
		try {
			return Type.getInternalName((Class<?>) reflectionClassAnnotation.get(clazz));
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException("Failed to get internal type", ex);
		}
	}

	/**
	 * @param clazz
	 * 		Type declaration.
	 *
	 * @return Internal type.
	 */
	public static String getType(ReflectionEnumDeclaration clazz) {
		try {
			return Type.getInternalName((Class<?>) reflectionClassEnum.get(clazz));
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException("Failed to get internal type", ex);
		}
	}

	/**
	 * @param field
	 * 		Field declaration.
	 *
	 * @return Field descriptor.
	 */
	public static String getFieldDesc(JavassistFieldDeclaration field) {
		try {
			return ((CtField) javassistCtField.get(field)).getSignature();
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException("Failed to get field descriptor", ex);
		}
	}

	/**
	 * @param field
	 * 		Field declaration.
	 *
	 * @return Field descriptor.
	 */
	public static String getFieldDesc(JavassistEnumConstantDeclaration field) {
		try {
			return ((CtField) javassistCtFieldEnum.get(field)).getSignature();
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException("Failed to get field descriptor", ex);
		}
	}

	/**
	 * @param field
	 * 		Field declaration.
	 *
	 * @return Field descriptor.
	 */
	public static String getFieldDesc(ReflectionFieldDeclaration field) {
		try {
			return Type.getType(((Field) reflectionField.get(field)).getType()).getDescriptor();
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException("Failed to get field descriptor", ex);
		}
	}

	/**
	 * @param field
	 * 		Field declaration.
	 *
	 * @return Field descriptor.
	 */
	public static String getFieldDesc(ReflectionEnumConstantDeclaration field) {
		try {
			return Type.getType(((Field) reflectionFieldEnum.get(field)).getType()).getDescriptor();
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException("Failed to get field descriptor", ex);
		}
	}

	/**
	 * @param method
	 * 		Method declaration.
	 *
	 * @return Method descriptor.
	 */
	public static String getMethodDesc(JavassistMethodDeclaration method) {
		try {
			return ((CtMethod) javassistCtMethod.get(method)).getSignature();
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException("Failed to get method descriptor", ex);
		}
	}

	/**
	 * @param method
	 * 		Method declaration.
	 *
	 * @return Method descriptor.
	 */
	public static String getMethodDesc(JavassistAnnotationMemberDeclaration method) {
		try {
			return Type.getMethodDescriptor((Method) javassistCtMethodAnno.get(method));
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException("Failed to get method descriptor", ex);
		}
	}

	/**
	 * @param method
	 * 		Method declaration.
	 *
	 * @return Method descriptor.
	 */
	public static String getMethodDesc(ReflectionMethodDeclaration method) {
		try {
			return Type.getMethodDescriptor((Method) reflectionMethod.get(method));
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException("Failed to get method descriptor", ex);
		}
	}

	/**
	 * @param method
	 * 		Method declaration.
	 *
	 * @return Method descriptor.
	 */
	public static String getMethodDesc(ReflectionAnnotationMemberDeclaration method) {
		try {
			return Type.getMethodDescriptor((Method) reflectionMethodAnno.get(method));
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException("Failed to get method descriptor", ex);
		}
	}

	/**
	 * @param ctor
	 * 		Constructor declaration.
	 *
	 * @return Constructor descriptor.
	 */
	public static String getConstructorDesc(JavassistConstructorDeclaration ctor) {
		try {
			return ((CtConstructor) javassistCtMethodCtor.get(ctor)).getSignature();
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException("Failed to get constructor descriptor", ex);
		}
	}

	/**
	 * @param ctor
	 * 		Constructor declaration.
	 *
	 * @return Constructor descriptor.
	 */
	public static String getConstructorDesc(ReflectionConstructorDeclaration ctor) {
		try {
			return Type.getConstructorDescriptor((Constructor<?>) reflectionMethodCtor.get(ctor));
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException("Failed to get constructor descriptor", ex);
		}
	}
}
