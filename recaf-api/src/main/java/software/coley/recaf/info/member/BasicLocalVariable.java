package software.coley.recaf.info.member;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * Basic implementation of a local variable.
 *
 * @author Matt Coley
 */
public class BasicLocalVariable implements LocalVariable {
	private final int index;
	private final String name;
	private final String desc;
	private final String signature;

	/**
	 * @param index
	 * 		Variable index.
	 * @param name
	 * 		Variable name.
	 * @param desc
	 * 		Variable type.
	 * @param signature
	 * 		Variable generic type.
	 */
	public BasicLocalVariable(int index, @Nonnull String name, @Nonnull String desc, @Nullable String signature) {
		this.index = index;
		this.name = name;
		this.desc = desc;
		this.signature = signature;
	}

	@Override
	public int getIndex() {
		return index;
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
		return signature;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		BasicLocalVariable that = (BasicLocalVariable) o;

		if (index != that.index) return false;
		if (!name.equals(that.name)) return false;
		if (!desc.equals(that.desc)) return false;
		return Objects.equals(signature, that.signature);
	}

	@Override
	public int hashCode() {
		int result = index;
		result = 31 * result + name.hashCode();
		result = 31 * result + desc.hashCode();
		result = 31 * result + (signature != null ? signature.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return index + ": " + name;
	}
}
