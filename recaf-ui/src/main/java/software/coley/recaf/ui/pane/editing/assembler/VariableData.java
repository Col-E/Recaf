package software.coley.recaf.ui.pane.editing.assembler;

import dev.xdark.blw.type.ClassType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.assembler.compile.analysis.Local;

/**
 * Models a variable.
 *
 * @param name
 * 		Name of variable.
 * @param type
 * 		Type of variable.
 * @param usage
 * 		Usages of the variable in the AST.
 */
public record VariableData(@Nonnull String name, @Nonnull ClassType type, @Nonnull AstUsages usage) {
	/**
	 * @param local
	 * 		blw variable declaration.
	 * @param usage
	 * 		AST usage.
	 *
	 * @return Data from a blw variable, plus AST usage.
	 */
	@Nonnull
	public static VariableData adaptFrom(@Nonnull Local local, @Nonnull AstUsages usage) {
		return new VariableData(local.name(), local.safeType(), usage);
	}

	/**
	 * @param other
	 * 		Other variable data to check against.
	 *
	 * @return {@code true} if the variable held by this data is the same as the other.
	 */
	public boolean matchesNameType(@Nullable VariableData other) {
		if (other == null) return false;
		return name.equals(other.name) && type.equals(other.type);
	}
}
