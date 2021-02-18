package me.coley.recaf.workspace.resource;

import me.coley.recaf.RecafConstants;
import org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class info for resource. Provides some basic information about the class.
 *
 * @author Matt Coley
 */
public class ClassInfo extends LiteralInfo implements CommonClassInfo{
	private final String superName;
	private final List<String> interfaces;
	private final int access;
	private final List<MemberInfo> fields;
	private final List<MemberInfo> methods;

	private ClassInfo(String name, String superName, List<String> interfaces, int access,
					  List<MemberInfo> fields, List<MemberInfo> methods, byte[] value) {
		super(name, value);
		this.superName = superName;
		this.interfaces = interfaces;
		this.access = access;
		this.fields = fields;
		this.methods = methods;
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
	 * Create a class info unit from the given class bytecode.
	 *
	 * @param value
	 * 		Class bytecode.
	 *
	 * @return Parsed class information unit.
	 */
	public static ClassInfo read(byte[] value) {
		ClassReader reader = new ClassReader(value);
		String name = reader.getClassName();
		String superName = reader.getSuperName();
		List<String> interfaces = Arrays.asList(reader.getInterfaces());
		int access = reader.getAccess();
		List<MemberInfo> fields = new ArrayList<>();
		List<MemberInfo> methods = new ArrayList<>();
		reader.accept(new ClassVisitor(RecafConstants.ASM_VERSION) {
			@Override
			public FieldVisitor visitField(int access, String name, String descriptor, String sig, Object value) {
				fields.add(new MemberInfo(name, descriptor, access));
				return null;
			}

			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String sig, String[] ex) {
				methods.add(new MemberInfo(name, descriptor, access));
				return null;
			}
		}, ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE);
		return new ClassInfo(
				name,
				superName,
				interfaces,
				access,
				fields,
				methods,
				value);
	}
}
