package software.coley.recaf.info;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.ClassReader;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;

import java.util.Arrays;

/**
 * Basic JVM class info implementation.
 *
 * @author Matt Coley
 */
public class BasicJvmClassInfo extends BasicClassInfo implements JvmClassInfo {
	private final byte[] bytecode;
	private final int version;
	private ClassReader reader;

	/**
	 * @param builder
	 * 		Builder to pull info from.
	 */
	public BasicJvmClassInfo(@Nonnull JvmClassInfoBuilder builder) {
		super(builder);
		this.bytecode = builder.getBytecode();
		this.version = builder.getVersion();
	}

	@Nonnull
	@Override
	public byte[] getBytecode() {
		return bytecode;
	}

	@Nonnull
	@Override
	public ClassReader getClassReader() {
		if (reader == null)
			reader = new ClassReader(bytecode);
		return reader;
	}

	@Override
	public int getVersion() {
		return version;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		if (!super.equals(o)) return false;

		if (o instanceof JvmClassInfo other) {
			if (version != other.getVersion()) return false;
			return Arrays.equals(bytecode, other.getBytecode());
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + Arrays.hashCode(bytecode);
		result = 31 * result + version;
		return result;
	}

	@Override
	public String toString() {
		return "JVM class: " + getName();
	}
}
