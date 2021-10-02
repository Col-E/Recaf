package me.coley.recaf.code;

import me.coley.recaf.RecafConstants;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Class info for resource. Provides some basic information about the class.
 *
 * @author Matt Coley
 */
public class ClassInfo implements ItemInfo, LiteralInfo, CommonClassInfo {
	private final byte[] value;
	private final String name;
	private final String superName;
	private final List<String> interfaces;
	private final int version;
	private final int access;
	private final List<FieldInfo> fields;
	private final List<MethodInfo> methods;

	private ClassInfo(String name, String superName, List<String> interfaces, int version, int access,
					  List<FieldInfo> fields, List<MethodInfo> methods, byte[] value) {
		this.value = value;
		this.name = name;
		this.superName = superName;
		this.interfaces = interfaces;
		this.version = version;
		this.access = access;
		this.fields = fields;
		this.methods = methods;
	}

	@Override
	public byte[] getValue() {
		return value;
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
	 * @return Class major version.
	 */
	public int getVersion() {
		return version;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ClassInfo info = (ClassInfo) o;
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
	 * Create a class info unit from the given class bytecode.
	 *
	 * @param value
	 * 		Class bytecode.
	 *
	 * @return Parsed class information unit.
	 */
	public static ClassInfo read(byte[] value) {
		ClassReader reader = new ClassReader(value);
		String className = reader.getClassName();
		String superName = reader.getSuperName();
		List<String> interfaces = Arrays.asList(reader.getInterfaces());
		int access = reader.getAccess();
		int[] versionWrapper = new int[1];
		List<FieldInfo> fields = new ArrayList<>();
		List<MethodInfo> methods = new ArrayList<>();
		reader.accept(new ClassVisitor(RecafConstants.ASM_VERSION) {
			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				versionWrapper[0] = version;
			}

			@Override
			public FieldVisitor visitField(int access, String name, String descriptor, String sig, Object value) {
				fields.add(new FieldInfo(className, name, descriptor, access));
				return null;
			}

			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String sig, String[] ex) {
				methods.add(new MethodInfo(className, name, descriptor, access));
				return null;
			}
		}, ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE);
		return new ClassInfo(
				className,
				superName,
				interfaces,
				versionWrapper[0],
				access,
				fields,
				methods,
				value);
	}
}
