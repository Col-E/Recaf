package software.coley.recaf.info;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.annotation.AnnotationElement;
import software.coley.recaf.info.member.LocalVariable;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.util.ExcludeFromJacocoGeneratedReport;

import java.util.Collections;
import java.util.List;

/**
 * Stub implementation of {@link MethodMember}.
 *
 * @author Matt Coley
 */
@ExcludeFromJacocoGeneratedReport(justification = "Stub/placeholder type")
public class StubMethodMember extends StubMember implements MethodMember {
	/**
	 * @param name
	 * 		Method name.
	 * @param desc
	 * 		Method descriptor.
	 * @param access
	 * 		Method access flags.
	 */
	public StubMethodMember(@Nonnull String name, @Nonnull String desc, int access) {
		super(name, desc, access);
	}

	@Nonnull
	@Override
	public List<String> getThrownTypes() {
		return  Collections.emptyList();
	}

	@Nonnull
	@Override
	public List<LocalVariable> getLocalVariables() {
		return Collections.emptyList();
	}

	@Nullable
	@Override
	public AnnotationElement getAnnotationDefault() {
		return null;
	}

	@Override
	public String toString() {
		return getName() + getDescriptor();
	}
}
