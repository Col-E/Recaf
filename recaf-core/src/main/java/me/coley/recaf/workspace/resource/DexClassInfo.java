package me.coley.recaf.workspace.resource;

import me.coley.recaf.android.cf.MutableClassDef;
import org.jf.dexlib2.dexbacked.DexBackedClassDef;
import org.jf.dexlib2.dexbacked.DexBackedMethod;
import org.jf.dexlib2.iface.ClassDef;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class info for Android dex classes.
 *
 * @author Matt Coley
 */
public class DexClassInfo extends ItemInfo implements CommonClassInfo {
	private final MutableClassDef def;
	private final String superName;
	private final List<String> interfaces;
	private final int access;
	private final List<MemberInfo> fields;
	private final List<MemberInfo> methods;

	private DexClassInfo(MutableClassDef def, String name, String superName, List<String> interfaces, int access,
						 List<MemberInfo> fields, List<MemberInfo> methods) {
		super(name);
		this.def = def;
		this.superName = superName;
		this.interfaces = interfaces;
		this.access = access;
		this.fields = fields;
		this.methods = methods;
	}

	/**
	 * @return Class definition.
	 */
	public ClassDef getClassDef() {
		return def;
	}

	@Override
	public String getSuperName() {
		return superName;
	}

	@Override
	public List<String> getInterfaces() {
		return interfaces;
	}

	@Override
	public int getAccess() {
		return access;
	}

	@Override
	public List<MemberInfo> getFields() {
		return fields;
	}

	@Override
	public List<MemberInfo> getMethods() {
		return methods;
	}


	/**
	 * Create a dex class info unit from the given class def.
	 *
	 * @param classDef
	 * 		Dex class accessor.
	 *
	 * @return Parsed class information unit.
	 */
	public static DexClassInfo parse(DexBackedClassDef classDef) {
		// Android internal types still hold the "L;" pattern in dexlib.
		// Need to strip that out.
		String name = Type.getType(classDef.getType()).getInternalName();
		// Supertype can be null, map it to object
		String superName = Type.getType(classDef.getSuperclass() == null ?
				"java/lang/Object" : classDef.getSuperclass()).getInternalName();
		List<String> interfaces = classDef.getInterfaces().stream()
				.map(itf -> Type.getType(itf).getInternalName())
				.collect(Collectors.toList());
		int access = classDef.getAccessFlags();
		List<MemberInfo> fields = new ArrayList<>();
		classDef.getFields().forEach(field -> {
			fields.add(new MemberInfo(field.getName(), field.getType(), field.getAccessFlags()));
		});
		List<MemberInfo> methods = new ArrayList<>();
		classDef.getMethods().forEach(method -> {
			methods.add(new MemberInfo(method.getName(), buildMethodType(method), method.getAccessFlags()));
		});
		return new DexClassInfo(
				new MutableClassDef(classDef),
				name,
				superName,
				interfaces,
				access,
				fields,
				methods
		);
	}

	private static String buildMethodType(DexBackedMethod method) {
		// TODO: This may already exist in dexlib, if not make a util class for this sorta stuff?
		StringBuilder sb = new StringBuilder("(");
		for (String type : method.getParameterTypes()) {
			sb.append(type);
		}
		sb.append(")").append(method.getReturnType());
		return sb.toString();
	}
}
