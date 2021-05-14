package me.coley.recaf.code;

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
	private final List<FieldInfo> fields;
	private final List<MethodInfo> methods;

	private ClassInfo(String name, String superName, List<String> interfaces, int access,
					  List<FieldInfo> fields, List<MethodInfo> methods, byte[] value) {
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
	public List<FieldInfo> getFields() {
		return fields;
	}

	@Override
	public List<MethodInfo> getMethods() {
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
		List<FieldInfo> fields = new ArrayList<>();
		List<MethodInfo> methods = new ArrayList<>();
		reader.accept(new ClassVisitor(RecafConstants.ASM_VERSION) {
			@Override
			public FieldVisitor visitField(int access, String name, String descriptor, String sig, Object value) {
				fields.add(new FieldInfo(name, descriptor, access));
				return null;
			}

			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String sig, String[] ex) {
				methods.add(new MethodInfo(name, descriptor, access));
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
