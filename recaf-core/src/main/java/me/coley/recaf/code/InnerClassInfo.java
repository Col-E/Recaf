package me.coley.recaf.code;

import org.objectweb.asm.Type;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * InnerClass Attribute, containing information about a used inner class.<br>
 * The root class (the most outer one) is not available in this attribute,
 * so no access flags can be retrieved this way.
 * <br>
 * From <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.6">4.7.6. The InnerClasses Attribute </a>:<br>
 * The InnerClasses attribute is a variable-length attribute in the attributes table of a ClassFile structure
 * ( <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.1">§4.1</a>).
 * <br>
 * If the constant pool of a class or interface C contains at least one CONSTANT_Class_info entry
 * (<a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4.1">§4.4.1</a>)
 * which represents a class or interface that is not a member of a package, then there must be exactly
 * one InnerClasses attribute in the attributes table of the ClassFile structure for C.
 * <br>
 * <b>Important note:</b> Oracle's Java Virtual Machine implementation
 * does not check the consistency of an InnerClasses attribute against a class
 * file representing a class or interface referenced by the attribute.
 *
 * @author Amejonah
 */
public class InnerClassInfo implements ItemInfo {
	private final String outerDeclaringClass;
	private final String name;
	private final String outerName;
	private final String innerName;
	private final int access;

	/**
	 * @param name
	 * 		The internal name of an inner class (see {@link Type#getInternalName()}).
	 * @param outerName
	 * 		The internal name of the class to which the inner class belongs (see {@link
	 *        Type#getInternalName()}). May be {@code null} for not member classes.
	 * @param innerName
	 * 		The (simple) name of the inner class inside its enclosing class. May be
	 *        {@code null} for anonymous inner classes.
	 * @param access
	 * 		The access flags of the inner class as originally declared in the enclosing
	 * 		class.
	 */
	public InnerClassInfo(String outerDeclaringClass, String name, @Nullable String outerName, @Nullable String innerName, int access) {
		this.outerDeclaringClass = outerDeclaringClass;
		this.name = name;
		this.outerName = outerName;
		this.innerName = innerName;
		this.access = access;
	}

	/**
	 * This is useful for backtracking to which class this attribute belongs to.
	 *
	 * @return Owner of this attribute.
	 */
	public String getOuterDeclaringClass() {
		return outerDeclaringClass;
	}

	/**
	 * @return Internal name of the inner class.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Internal name of the outer class in which this inner class is in. May be null for not member classes.
	 */
	@Nullable
	public String getOuterName() {
		return outerName;
	}

	/**
	 * @return Simple name of the inner class. May be null for anonymous inner classes.
	 */
	@Nullable
	public String getInnerName() {
		return innerName;
	}

	/**
	 * Extracted from
	 * <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.6">4.7.6. The InnerClasses Attribute</a>:<br>
	 * The value of the inner_class_access_flags item is a mask of flags used to denote access permissions
	 * to and properties of class or interface C [this inner class] as declared in the source code from which this class file
	 * was compiled. It is used by a compiler to recover the original information when source code is not available.
	 * The flags are specified in Table 4.7.6-A.
	 *
	 * @return Access flags for the inner class.
	 */
	public int getAccess() {
		return access;
	}

	/**
	 * @return Either {@link #getInnerName()} if not null, otherwise the last "part" (after last $ or /) of {@link #getName()}.
	 */
	@Nonnull
	public String getSimpleName() {
		if (innerName != null) return innerName;
		int lastIndex = 0;
		int endIndex = Math.min(outerDeclaringClass.length(), name.length());
		for (; lastIndex < endIndex; lastIndex++) {
			if (outerDeclaringClass.charAt(lastIndex) != name.charAt(lastIndex)) break;
		}
		if (lastIndex == 0)
			return name;
		else if (name.startsWith("$", lastIndex))
			lastIndex++;
		return name.substring(lastIndex);
	}
}
