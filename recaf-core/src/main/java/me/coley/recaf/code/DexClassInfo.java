package me.coley.recaf.code;

import me.coley.recaf.android.cf.MutableClassDef;
import me.coley.recaf.util.DalvikUtils;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.objectweb.asm.Type;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Class info for Android dex classes.
 *
 * @author Matt Coley
 */
public class DexClassInfo implements ItemInfo, CommonClassInfo {
	private final String dexPath;
	private final Opcodes opcodes;
	private final MutableClassDef def;
	private final String name;
	private final String superName;
	private final String signature;
	private final List<String> interfaces;
	private final int access;
	private final List<FieldInfo> fields;
	private final List<MethodInfo> methods;

	private DexClassInfo(String dexPath, Opcodes opcodes, MutableClassDef def, String name, String superName,
						 String signature, List<String> interfaces, int access,
						 List<FieldInfo> fields, List<MethodInfo> methods) {
		this.dexPath = dexPath;
		this.opcodes = opcodes;
		this.def = def;
		this.name = name;
		this.superName = superName;
		this.signature = signature;
		this.interfaces = interfaces;
		this.access = access;
		this.fields = fields;
		this.methods = methods;
	}

	/**
	 * It is intended that our {@link me.coley.recaf.workspace.resource.Resource} is wrapping an APK file.
	 * These may contain multiple dex classes, so we need to know which one this belongs to.
	 *
	 * @return Internal path to dex file in an APK where the class is defined in.
	 */
	public String getDexPath() {
		return dexPath;
	}

	/**
	 * @return Instruction set for a given API version.
	 */
	public Opcodes getOpcodes() {
		return opcodes;
	}

	/**
	 * @return Class definition.
	 */
	public MutableClassDef getClassDef() {
		return def;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getSuperName() {
		return superName;
	}

	@Override
	public String getSignature() {
		return signature;
	}

	@Override
	public List<String> getInterfaces() {
		return interfaces;
	}

	@Override
	public int getAccess() {
		return access;
	}

	@Nullable
	@Override
	public OuterMethodInfo getOuterMethod() {
		// TODO: implement outer method if dex class is a method local class
		return null;
	}

	@Override
	public List<FieldInfo> getFields() {
		return fields;
	}

	@Override
	public List<MethodInfo> getMethods() {
		return methods;
	}

	@Override
	public List<InnerClassInfo> getInnerClasses() {
		// TODO: Implement dex inner classes
		return new ArrayList<>();
	}

	@Override
	public List<String> getOuterClassBreadcrumbs() {
		//TODO: Implement dex outer class breadcrumbs
		return new ArrayList<>();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DexClassInfo info = (DexClassInfo) o;
		return access == info.access &&
				Objects.equals(name, info.name) &&
				Objects.equals(superName, info.superName) &&
				Objects.equals(interfaces, info.interfaces) &&
				Objects.equals(fields, info.fields) &&
				Objects.equals(methods, info.methods);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, superName, interfaces, access, fields, methods);
	}

	/**
	 * Create a dex class info unit from the given class def.
	 *
	 * @param dexPath
	 * 		Internal path to dex file in an APK where the class is defined in.
	 * @param opcodes
	 * 		Instruction set for a given API version.
	 * @param classDef
	 * 		Dex class accessor.
	 *
	 * @return Parsed class information unit.
	 */
	public static DexClassInfo parse(String dexPath, Opcodes opcodes, ClassDef classDef) {
		// Android internal types still hold the "L;" pattern in dexlib.
		// Need to strip that out.
		String className = Type.getType(classDef.getType()).getInternalName();
		// Supertype can be null, map it to object
		String superName = Type.getType(classDef.getSuperclass() == null ?
				"java/lang/Object" : classDef.getSuperclass()).getInternalName();
		List<String> interfaces = classDef.getInterfaces().stream()
				.map(itf -> Type.getType(itf).getInternalName())
				.collect(Collectors.toList());
		int access = classDef.getAccessFlags();
		List<FieldInfo> fields = new ArrayList<>();
		classDef.getFields().forEach(field -> {
			// TODO: Extract signature from annotations
			String fieldSignature = null;
			// TODO: Extract value from encoded-value
			Object value = null;
			fields.add(new FieldInfo(className, field.getName(), field.getType(),
					fieldSignature, field.getAccessFlags(), value));
		});
		List<MethodInfo> methods = new ArrayList<>();
		classDef.getMethods().forEach(method -> {
			// TODO: Extract signature from annotations
			String methodSignature = null;
			// TODO: Extract exceptions from annotations
			List<String> exceptions = Collections.emptyList();
			methods.add(new MethodInfo(className, method.getName(), buildMethodType(method),
					methodSignature, method.getAccessFlags(), exceptions));
		});
		DalvikUtils.ClassData annotationData = DalvikUtils.parseAnnotations(classDef);
		String signature = annotationData.signature;
		return new DexClassInfo(
				dexPath,
				opcodes,
				new MutableClassDef(classDef),
				className,
				superName,
				signature,
				interfaces,
				access,
				fields,
				methods
		);
	}

	private static String buildMethodType(Method method) {
		StringBuilder sb = new StringBuilder("(");
		for (CharSequence type : method.getParameterTypes()) {
			sb.append(type);
		}
		sb.append(")").append(method.getReturnType());
		return sb.toString();
	}
}
