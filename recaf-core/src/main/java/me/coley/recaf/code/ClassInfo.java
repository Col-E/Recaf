package me.coley.recaf.code;

import me.coley.recaf.RecafConstants;
import me.coley.recaf.util.logging.Logging;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class info for resource. Provides some basic information about the class.
 *
 * @author Matt Coley
 */
public class ClassInfo implements ItemInfo, LiteralInfo, CommonClassInfo {
	private static final Logger LOGGER = Logging.get(ClassInfo.class);
	public static long maxOuterDepth = 20;
	private final byte[] value;
	private final String name;
	private final String superName;
	private final String signature;
	private final List<String> interfaces;
	private final List<InnerClassInfo> innerClasses;
	private final List<String> outerClassBreadcrumbs;
	private final int version;
	private final int access;
	private final OuterMethodInfo outerMethod;
	private final List<FieldInfo> fields;
	private final List<MethodInfo> methods;
	private ClassReader classReader;
	private int hashCode;

	private ClassInfo(String name, String superName, String signature, List<String> interfaces, int version, int access,
					  OuterMethodInfo outerMethod, List<FieldInfo> fields, List<MethodInfo> methods, byte[] value,
					  List<InnerClassInfo> innerClasses, List<String> outerClassBreadcrumbs) {
		this.value = value;
		this.name = name;
		this.signature = signature;
		this.superName = superName;
		this.interfaces = interfaces;
		this.version = version;
		this.access = access;
		this.outerMethod = outerMethod;
		this.fields = fields;
		this.methods = methods;
		this.innerClasses = innerClasses;
		this.outerClassBreadcrumbs = outerClassBreadcrumbs;
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

	@Override
	@Nullable
	public OuterMethodInfo getOuterMethod() {
		return outerMethod;
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
		return innerClasses;
	}

	@Override
	public List<String> getOuterClassBreadcrumbs() {
		return outerClassBreadcrumbs;
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
				name.equals(info.name) &&
				Objects.equals(superName, info.superName) &&
				Objects.equals(signature, info.signature) &&
				interfaces.equals(info.interfaces) &&
				fields.equals(info.fields) &&
				methods.equals(info.methods);
	}

	@Override
	public int hashCode() {
		int hashCode = this.hashCode;
		if (hashCode == 0) {
			hashCode = name.hashCode();
			hashCode = 31 * hashCode + Objects.hashCode(superName);
			hashCode = 31 * hashCode + Objects.hashCode(signature);
			hashCode = 31 * hashCode + interfaces.hashCode();
			hashCode = 31 * hashCode + access;
			hashCode = 31 * hashCode + fields.hashCode();
			hashCode = 31 * hashCode + methods.hashCode();
			this.hashCode = hashCode + 1;
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return "ClassInfo{'" + name + "'}";
	}

	/**
	 * @return ASM class reader.
	 */
	public ClassReader getClassReader() {
		ClassReader classReader = this.classReader;
		if (classReader == null) {
			return this.classReader = new ClassReader(value);
		}
		return classReader;
	}

	/**
	 * Create a class info unit from the given class bytecode.
	 *
	 * @param value
	 * 		Class bytecode.
	 *
	 * @return Parsed class information unit.
	 */
	@SuppressWarnings("unchecked")
	public static ClassInfo read(byte[] value) {
		ClassReader reader = new ClassReader(value);
		String className = reader.getClassName();
		String superName = reader.getSuperName();
		String[] signatureWrapper = new String[1];
		List<String>[] interfacesWrapper = new List[1];
		int access = reader.getAccess();
		int[] versionWrapper = new int[1];
		List<FieldInfo> fields = new ArrayList<>();
		List<MethodInfo> methods = new ArrayList<>();
		List<InnerClassInfo> innerClasses = new ArrayList<>();
		OuterMethodInfo[] outerMethod = new OuterMethodInfo[1];
		reader.accept(new ClassVisitor(RecafConstants.getAsmVersion()) {
			@Override
			public void visit(int version, int access, String name, String signature,
							  String superName, String[] interfaces) {
				versionWrapper[0] = version;
				interfacesWrapper[0] = Arrays.asList(interfaces);
				signatureWrapper[0] = signature;
			}

			@Override
			public FieldVisitor visitField(int access, String name, String descriptor, String sig, Object value) {
				fields.add(new FieldInfo(className, name, descriptor, sig, access, value));
				return null;
			}

			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String sig, String[] ex) {
				List<String> exceptions = ex == null ? Collections.emptyList() : Arrays.asList(ex);
				methods.add(new MethodInfo(className, name, descriptor, sig, access, exceptions));
				return null;
			}

			@Override
			public void visitInnerClass(String name, @Nullable String outerName, @Nullable String innerName, int access) {
				innerClasses.add(new InnerClassInfo(className, name, outerName, innerName, access));
			}

			@Override
			public void visitOuterClass(String owner, @Nullable String name, @Nullable String descriptor) {
				outerMethod[0] = new OuterMethodInfo(owner, name, descriptor);
			}
		}, ClassReader.SKIP_CODE);
		List<InnerClassInfo> directlyNested = // Getting all inner classes which are directly visible, no nested inside nested ones
				innerClasses.stream()
						.filter(innerClass -> (innerClass.getOuterName() == null || className.equals(innerClass.getOuterName()))
								&& !className.equals(innerClass.getName()))
						.collect(Collectors.toList());
		// Create outer class breadcrumbs
		// Alternatives are not great:
		// - Splitting by $ can go wrong with obfuscation.
		// - Searching for outer classes when opening the outline can be slow because if you have a big workspace/package,
		//   you need to go multiple times through all the entries for reconstruction.
		String outerClassName = outerClassOf(className, innerClasses);
		List<String> breadcrumbs = new ArrayList<>();
		int counter = 0; // if some obfuscators think they can trick this by adding many inner classes
		if (maxOuterDepth > 0) while (outerClassName != null) {
			if (++counter > maxOuterDepth) {
				LOGGER.info("Class {} has too long breadcrumb of outer classes (over {}), " +
						"cleared chain, as this assumed to be work from obfuscators.", className, maxOuterDepth);
				breadcrumbs.clear(); // assuming some obfuscator is at work, so breadcrumbs might be invalid.
				break;
			}
			breadcrumbs.add(0, outerClassName);
			outerClassName = outerClassOf(outerClassName, innerClasses);
		}
		return new ClassInfo(
				className,
				superName,
				signatureWrapper[0],
				interfacesWrapper[0],
				versionWrapper[0],
				access,
				outerMethod[0],
				fields,
				methods,
				value,
				directlyNested,
				breadcrumbs);
	}

	private static String outerClassOf(String name, List<InnerClassInfo> candidates) {
		if (name == null || name.isEmpty()) return null;
		for (InnerClassInfo innerClass : candidates) {
			if (name.equals(innerClass.getName())) {
				return innerClass.getOuterName();
			}
		}
		return null;
	}
}
