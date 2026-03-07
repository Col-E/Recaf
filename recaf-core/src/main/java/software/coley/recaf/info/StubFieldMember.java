package software.coley.recaf.info;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.util.ExcludeFromJacocoGeneratedReport;

/**
 * Stub implementation of {@link FieldMember}.
 *
 * @author Matt Coley
 */
@ExcludeFromJacocoGeneratedReport(justification = "Stub/placeholder type")
public class StubFieldMember extends StubMember implements FieldMember {
	/**
	 * @param name
	 * 		Field name.
	 * @param desc
	 * 		Field descriptor.
	 * @param access
	 * 		Field access flags.
	 */
	public StubFieldMember(@Nonnull String name, @Nonnull String desc, int access) {
		super(name, desc, access);
	}

	@Nullable
	@Override
	public Object getDefaultValue() {
		return null;
	}

	@Override
	public String toString() {
		return getDescriptor() + ' ' + getName();
	}
}
