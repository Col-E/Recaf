package software.coley.recaf.services.callgraph;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.MethodMember;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Representation of a {@link ClassInfo} for {@link CallGraph}.
 * All the {@link MethodMember} values of the {@link ClassInfo#getMethods()} can be mapped to vertices via
 * {@link #getVertex(MethodMember)}.
 *
 * @author Matt Coley
 */
public class ClassMethodsContainer {
	private final Map<MethodMember, MethodVertex> methodVertices = Collections.synchronizedMap(new IdentityHashMap<>());
	private final ClassInfo classInfo;

	/**
	 * @param classInfo
	 * 		Class to wrap.
	 */
	public ClassMethodsContainer(@Nonnull ClassInfo classInfo) {
		this.classInfo = classInfo;
	}

	/**
	 * @return Wrapped {@link ClassInfo}.
	 */
	@Nonnull
	public ClassInfo getClassInfo() {
		return classInfo;
	}

	/**
	 * @return Collection of method vertices within this class.
	 */
	@Nonnull
	public Collection<MethodVertex> getVertices() {
		return methodVertices.values();
	}

	/**
	 * @param name
	 * 		Method name.
	 * @param descriptor
	 * 		Method descriptor.
	 *
	 * @return Method vertex of the declared method.
	 * {@code null} when no method by the given name/desc exist in this class.
	 */
	@Nullable
	public MethodVertex getVertex(@Nonnull String name, @Nonnull String descriptor) {
		MethodMember member = classInfo.getDeclaredMethod(name, descriptor);
		if (member == null)
			return null;
		return getVertex(member);
	}

	/**
	 * @param member
	 * 		Member declaration from the associated {@link ClassInfo}.
	 *
	 * @return Method vertex of the declared method.
	 *
	 * @throws IllegalArgumentException
	 * 		When the member declaration does not belong to the associated {@link ClassInfo}.
	 */
	@Nonnull
	public MethodVertex getVertex(@Nonnull MethodMember member) throws IllegalArgumentException {
		if (member.getDeclaringClass() != classInfo)
			throw new IllegalArgumentException("Member does not belong to class from this vertex");
		return methodVertices.computeIfAbsent(member, m -> new MutableMethodVertex(
				new MethodRef(classInfo.getName(), member.getName(), member.getDescriptor()),
				member)
		);
	}
}
