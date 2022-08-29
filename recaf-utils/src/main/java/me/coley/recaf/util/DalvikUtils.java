package me.coley.recaf.util;

import org.jf.dexlib2.ValueType;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.AnnotationElement;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.value.*;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Utilities for dealing with dalvik format.
 *
 * @author Matt Coley
 */
public class DalvikUtils {
	public static final String INNER_CLASS = "Ldalvik/annotation/InnerClass;";
	public static final String INNER_CLASS_MEMBER = "Ldalvik/annotation/MemberClasses;";
	public static final String OUTER_CLASS = "Ldalvik/annotation/EnclosingClass;";
	public static final String OUTER_METHOD = "Ldalvik/annotation/EnclosingMethod;";
	public static final String SIGNATURE = "Ldalvik/annotation/Signature;";

	/**
	 * @param classDef
	 * 		Class to parse.
	 *
	 * @return Data derived from annotations.
	 */
	public static ClassData parseAnnotations(ClassDef classDef) {
		ClassData data = new ClassData();
		for (Annotation annotation : classDef.getAnnotations()) {
			String annotationType = annotation.getType();
			switch (annotationType) {
				case INNER_CLASS:
					handleInnerClass(annotation, info -> data.innerClassInfo = info);
					break;
				case INNER_CLASS_MEMBER:
					handleMemberClasses(annotation, data.innerClassNames::add);
					break;
				case OUTER_CLASS:
					break;
				case OUTER_METHOD:
					break;
				case SIGNATURE:
					handleSignature(annotation, sig -> data.signature = sig);
					break;
				default:
					System.out.println(annotationType);
					break;
			}
		}
		return data;
	}

	private static void handleSignature(Annotation annotation, Consumer<String> action) {
		// Some signatures aren't valid. For example, just two "Ljava/lang/Object;" next to each other.
		// I don't know why.
		String signature;
		StringBuilder sb = new StringBuilder();
		for (AnnotationElement element : annotation.getElements())
			sb.append(element.getValue());
		signature = sb.toString();
		// Validate
		try {
			new SignatureReader(signature).accept(new SignatureWriter());
			// Can be used
			action.accept(signature);
		} catch (Exception ignored) {
			// Cannot be used
		}
	}

	private static void handleInnerClass(Annotation annotation, Consumer<DalvikInnerClass> action) {
		// Used to describe self as an inner class of an outer class
		String name = null;
		int access = 0;
		for (AnnotationElement element : annotation.getElements()) {
			String key = element.getName();
			EncodedValue value = element.getValue();
			int valueType = value.getValueType();
			switch (key) {
				case "accessFlags":
					access = ((IntEncodedValue) value).getValue();
					break;
				case "name":
					// Name can be null for anonymous classes.
					// Due to obfuscation though we have no way to know locally what the name here.
					// If the class is "MyType$1" we cannot know if "1" is the inner "name".
					name = (valueType == ValueType.NULL) ? null : ((StringEncodedValue) value).getValue();
					break;
			}
		}
		action.accept(new DalvikInnerClass(name, access));
	}

	private static void handleMemberClasses(Annotation annotation, Consumer<String> action) {
		// Used to list the names of member classes (does not include access flag info)
		// That information is stored within the target class itself.
		for (AnnotationElement element : annotation.getElements()) {
			ArrayEncodedValue membersArray = (ArrayEncodedValue) element.getValue();
			for (EncodedValue item : membersArray.getValue()) {
				int itemValueType = item.getValueType();
				if (itemValueType == ValueType.TYPE) {
					String desc = ((TypeEncodedValue) item).getValue();
					action.accept(Type.getType(desc).getInternalName());
				}
			}
		}
	}

	/**
	 * Used to collect data read in {@link #parseAnnotations(ClassDef)}.
	 */
	public static class ClassData {
		public final List<String> innerClassNames = new ArrayList<>();
		public DalvikInnerClass innerClassInfo;
		public String signature;
	}

	/**
	 * Used to represent data from {@link #INNER_CLASS_MEMBER}.
	 */
	public static class DalvikInnerClass {
		public final String name;
		public final int access;

		private DalvikInnerClass(String name, int access) {
			this.name = name;
			this.access = access;
		}

		/**
		 * @return Class containing this attribute is an anonymous class.
		 */
		public boolean isAnonymous() {
			return name == null;
		}
	}
}
