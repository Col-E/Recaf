package software.coley.recaf.info;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassReader;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.annotation.TypeAnnotationInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.info.properties.Property;
import software.coley.recaf.util.JavaVersion;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Stub implementation of {@link ClassInfo}.
 *
 * @author Matt Coley
 */
public class StubClassInfo implements ClassInfo {
	private final String name;

	/**
	 * @param name
	 * 		Class name.
	 */
	public StubClassInfo(@Nonnull String name) {
		this.name = name;
	}

	@Override
	public int getAccess() {
		return 0;
	}

	@Nullable
	@Override
	public String getSourceFileName() {
		return null;
	}

	@Nonnull
	@Override
	public List<String> getInterfaces() {
		return Collections.emptyList();
	}

	@Nullable
	@Override
	public String getSuperName() {
		return "java/lang/Object";
	}

	@Nullable
	@Override
	public String getSignature() {
		return null;
	}

	@Override
	public boolean hasValidSignatures() {
		return true;
	}

	@Nullable
	@Override
	public String getOuterClassName() {
		return null;
	}

	@Nullable
	@Override
	public String getOuterMethodName() {
		return null;
	}

	@Nullable
	@Override
	public String getOuterMethodDescriptor() {
		return null;
	}

	@Nonnull
	@Override
	public List<String> getOuterClassBreadcrumbs() {
		return Collections.emptyList();
	}

	@Nonnull
	@Override
	public List<InnerClassInfo> getInnerClasses() {
		return Collections.emptyList();
	}

	@Nonnull
	@Override
	public List<FieldMember> getFields() {
		return Collections.emptyList();
	}

	@Nonnull
	@Override
	public List<MethodMember> getMethods() {
		return Collections.emptyList();
	}

	@Override
	public void acceptIfJvmClass(@Nonnull Consumer<JvmClassInfo> action) {
		// no-op
	}

	@Override
	public void acceptIfAndroidClass(@Nonnull Consumer<AndroidClassInfo> action) {
		// no-op
	}

	@Override
	public boolean testIfJvmClass(@Nonnull Predicate<JvmClassInfo> predicate) {
		return false;
	}

	@Override
	public boolean testIfAndroidClass(@Nonnull Predicate<AndroidClassInfo> predicate) {
		return false;
	}

	@Override
	public boolean isJvmClass() {
		return false;
	}

	@Override
	public boolean isAndroidClass() {
		return false;
	}

	@Nonnull
	@Override
	public String getName() {
		return name;
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
	public <V> void setProperty(Property<V> property) {
		// no-op
	}

	@Override
	public void removeProperty(String key) {
		// no-op
	}

	@Nonnull
	@Override
	public Map<String, Property<?>> getProperties() {
		return Collections.emptyMap();
	}

	@Nonnull
	@Override
	public JvmClassInfo asJvmClass() {
		return new Jvm(name);
	}

	@Nonnull
	@Override
	public AndroidClassInfo asAndroidClass() {
		return new Android(name);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		StubClassInfo that = (StubClassInfo) o;

		return name.equals(that.name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	private static class Android extends StubClassInfo implements AndroidClassInfo {
		public Android(@Nonnull String name) {
			super(name);
		}

		@Nonnull
		@Override
		public AndroidClassInfo asAndroidClass() {
			return this;
		}
	}

	private static class Jvm extends StubClassInfo implements JvmClassInfo {
		public Jvm(@Nonnull String name) {
			super(name);
		}

		@Nonnull
		@Override
		public JvmClassInfo asJvmClass() {
			return this;
		}

		@Override
		public int getVersion() {
			return JavaVersion.VERSION_OFFSET + JavaVersion.get();
		}

		@Nonnull
		@Override
		public byte[] getBytecode() {
			return new byte[0];
		}

		@Nonnull
		@Override
		public ClassReader getClassReader() {
			throw new IllegalStateException("Cannot read from stub class!");
		}
	}
}
