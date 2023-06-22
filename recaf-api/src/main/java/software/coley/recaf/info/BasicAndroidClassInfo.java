package software.coley.recaf.info;

import com.android.tools.r8.graph.DexProgramClass;
import jakarta.annotation.Nonnull;
import org.objectweb.asm.ClassReader;
import software.coley.dextranslator.Options;
import software.coley.dextranslator.ir.ConversionException;
import software.coley.dextranslator.model.ApplicationData;
import software.coley.recaf.info.builder.AndroidClassInfoBuilder;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;

import java.io.IOException;
import java.util.Collections;

/**
 * Basic Android class info implementation.
 *
 * @author Matt Coley
 */
public class BasicAndroidClassInfo extends BasicClassInfo implements AndroidClassInfo {
	private final DexProgramClass dexClass;
	private JvmClassInfo converted;

	/**
	 * @param builder
	 * 		Builder to pull info from.
	 */
	public BasicAndroidClassInfo(@Nonnull AndroidClassInfoBuilder builder) {
		super(builder);
		dexClass = builder.getDexClass();
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
					ApplicationData data = ApplicationData.fromProgramClasses(Collections.singleton(dexClass));
					data.setOperationOptionsProvider(() -> new Options()
							.enableLoadStoreOptimization()
							.setLenient(true)
							.setReplaceInvalidMethodBodies(true));
					byte[] convertedBytecode = data.exportToJvmClass(name);
					if (convertedBytecode == null)
						throw new IllegalStateException("Failed to convert Dalvik model of " + name + " to JVM bytecode, " +
								"conversion results did not include type name.");
					ClassReader reader = new ClassReader(convertedBytecode);
					converted = new JvmClassInfoBuilder(reader).build();
				} catch (ConversionException | IOException ex) {
					throw new IllegalStateException(ex);
				}
			}
		}
		return converted;
	}

	/**
	 * @return Backing program class node.
	 */
	@Nonnull
	public DexProgramClass getDexClass() {
		return dexClass;
	}

	@Override
	public String toString() {
		return "Android class: " + getName();
	}
}
