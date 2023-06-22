package software.coley.recaf.services.mapping.gen.generator;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.mapping.gen.NameGenerator;
import software.coley.recaf.workspace.model.Workspace;

/**
 * Basic name generator using a given alphabet of characters to generate pseudo-random names with.
 * Names will always yield the same value for the same input.
 *
 * @author Matt Coley
 */
public class IncrementingNameGenerator implements NameGenerator {
	private Workspace workspace;
	private long classIndex = 1;
	private long fieldIndex = 1;
	private long methodIndex = 1;

	/**
	 * Enables name deconfliction.
	 *
	 * @param workspace
	 * 		Workspace to assign, to deconflict names.
	 */
	public void setWorkspace(@Nullable Workspace workspace) {
		this.workspace = workspace;
	}

	@Nonnull
	private String nextClassName() {
		return "mapped/Class" + classIndex++;
	}

	@Nonnull
	private String nextFieldName() {
		return "field" + fieldIndex++;
	}

	@Nonnull
	private String nextMethodName() {
		return "method" + methodIndex++;
	}

	@Nonnull
	@Override
	public String mapClass(@Nonnull ClassInfo info) {
		String name = nextClassName();
		if (workspace != null) {
			while (workspace.findClass(name) != null)
				name = nextClassName();
		}
		return name;
	}

	@Nonnull
	@Override
	public String mapField(@Nonnull ClassInfo owner, @Nonnull FieldMember field) {
		String name = nextFieldName();
		String descriptor = field.getDescriptor();
		while (owner.getDeclaredField(name, descriptor) != null)
			name = nextFieldName();
		return name;
	}

	@Nonnull
	@Override
	public String mapMethod(@Nonnull ClassInfo owner, @Nonnull MethodMember method) {
		String name = nextMethodName();
		String descriptor = method.getDescriptor();
		while (owner.getDeclaredMethod(name, descriptor) != null)
			name = nextFieldName();
		return name;
	}
}
