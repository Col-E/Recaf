package software.coley.recaf.info;

import jakarta.annotation.Nonnull;
import me.darknet.dex.convert.DexConversionIr;
import me.darknet.dex.convert.DexConversionSimple;
import me.darknet.dex.tree.definitions.ClassDefinition;
import org.objectweb.asm.ClassReader;
import software.coley.recaf.info.builder.AndroidClassInfoBuilder;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;

/**
 * Basic Android class info implementation.
 *
 * @author Matt Coley
 */
public class BasicAndroidClassInfo extends BasicClassInfo implements AndroidClassInfo {
	private final ClassDefinition def;
	private JvmClassInfo converted;

	/**
	 * @param builder
	 * 		Builder to pull info from.
	 */
	public BasicAndroidClassInfo(@Nonnull AndroidClassInfoBuilder builder) {
		super(builder);
		def = builder.getDef();
	}

	@Nonnull
	@Override
	public ClassDefinition getBackingDefinition() {
		return def;
	}

	@Override
	public boolean canMapToJvmClass() {
		// See below
		return true;
	}

	/**
	 * @return Translation into JVM class.
	 */
	@Nonnull
	@Override
	public JvmClassInfo asJvmClass() {
		if (converted == null) {
			// This is an expensive operation, so it's best to make other thread access wait until it is done and
			// then use the singular return value.
			synchronized (this) {
				// If there are 2+ requests here, and we let one through to completion, when the others are let in
				// this value should be computed then.
				if (converted != null)
					return converted;
				try {
					String name = getName();
					byte[] convertedBytecode = new DexConversionIr().toJavaClass(def);
					if (convertedBytecode == null)
						throw new IllegalStateException("Failed to convert Dalvik model of " + name + " to JVM bytecode, " +
								"conversion results did not include type name.");
					ClassReader reader = new ClassReader(convertedBytecode);
					converted = new JvmClassInfoBuilder(reader).build();
				} catch (IllegalStateException ex) {
					throw ex;
				} catch (Throwable ex) {
					throw new IllegalStateException(ex);
				}
			}
		}
		return converted;
	}

	@Override
	public String toString() {
		return "Android class: " + getName();
	}
}
