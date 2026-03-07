package software.coley.recaf.info;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.annotation.TypeAnnotationInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.properties.Property;
import software.coley.recaf.util.ExcludeFromJacocoGeneratedReport;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Stub implementation of {@link ClassMember}.
 *
 * @author Matt Coley
 */
@ExcludeFromJacocoGeneratedReport(justification = "Stub/placeholder type")
public abstract class StubMember implements ClassMember {
	private final String name;
	private final String desc;
	private final int access;

	/**
	 * @param name Member name.
	 * @param desc Member descriptor.
	 * @param access Member access flags.
	 */
	public StubMember(@Nonnull String name, @Nonnull String desc, int access) {
		this.name = name;
		this.desc = desc;
		this.access = access;
	}

	@Nonnull
	@Override
	public String getName() {
		return name;
	}

	@Nonnull
	@Override
	public String getDescriptor() {
		return desc;
	}

	@Nullable
	@Override
	public String getSignature() {
		return null;
	}

	@Override
	public int getAccess() {
		return access;
	}

	@Nonnull
	@Override
	public List<AnnotationInfo> getAnnotations() {
		return Collections.emptyList();
	}

	@Nonnull
	@Override
	public List<TypeAnnotationInfo> getTypeAnnotations() {
		return Collections.emptyList();
	}

	@Override
	public <V> void setProperty(Property<V> property) {}

	@Override
	public void removeProperty(String key) {}

	@Nonnull
	@Override
	public Map<String, Property<?>> getProperties() {
		return Collections.emptyMap();
	}

	@Override
	public final boolean equals(Object o) {
		if (!(o instanceof StubMember that)) return false;

		return access == that.access
				&& name.equals(that.name)
				&& desc.equals(that.desc);
	}

	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + desc.hashCode();
		result = 31 * result + access;
		return result;
	}
}
