package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.util.visitors.WorkspaceClassWriter;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.cli.Flags;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

/**
 * Utility for wrapping {@link ClassDowngrader}.
 *
 * @author Matt Coley
 */
public class JavaDowngraderUtil {
	private static final Logger logger = Logging.get(JavaDowngraderUtil.class);

	/**
	 * Downgrade the provided classes.
	 *
	 * @param targetJavaVersion
	 * 		Target Java version to downgrade to.
	 * @param classes
	 * 		Map of classes to downgrade.
	 * @param transformConsumer
	 * 		Consumer to receive downgraded classes.
	 * 		Additional classes may be provided for cases where the downgrader creates its own backported library code.,
	 *
	 * @throws IOException
	 * 		Thrown when the downgrader instance cannot be constructed.
	 */
	public static void downgrade(int targetJavaVersion,
	                             @Nonnull Map<String, byte[]> classes,
	                             @Nonnull BiConsumer<String, byte[]> transformConsumer) throws IOException {
		downgrade(targetJavaVersion, null, classes, transformConsumer);
	}

	/**
	 * Downgrade the provided classes.
	 *
	 * @param targetJavaVersion
	 * 		Target Java version to downgrade to.
	 * @param inheritanceGraph
	 * 		Inheritance graph of the workspace containing the given classes.
	 * @param classes
	 * 		Map of classes to downgrade.
	 * @param transformConsumer
	 * 		Consumer to receive downgraded classes.
	 * 		Additional classes may be provided for cases where the downgrader creates its own backported library code.,
	 *
	 * @throws IOException
	 * 		Thrown when the downgrader instance cannot be constructed.
	 */
	public static void downgrade(int targetJavaVersion,
	                             @Nullable InheritanceGraph inheritanceGraph,
	                             @Nonnull Map<String, byte[]> classes,
	                             @Nonnull BiConsumer<String, byte[]> transformConsumer) throws IOException {
		int targetClassVersion = JavaVersion.VERSION_OFFSET + targetJavaVersion;

		Flags flags = new Flags();
		flags.classVersion = targetClassVersion;

		try (ClassDowngrader downgrader = new RecafClassDowngrader(flags, inheritanceGraph)) {
			classes.forEach((className, classBytes) -> {
				// Convert class file version to Java version.
				int classJavaVersion = classBytes[7] - JavaVersion.VERSION_OFFSET;

				// Hack to ensure downgrader will run on bleeding edge versions of Java.
				// This will cause brand-new features to not be properly downgraded, but
				// not all bleeding edge classes use all the brand-new features. At least
				// trying is better than failing outright for most use cases.
				if (classJavaVersion > downgrader.maxVersion()) {
					classBytes = Arrays.copyOf(classBytes, classBytes.length);
					classBytes[7] = (byte) (downgrader.maxVersion() + JavaVersion.VERSION_OFFSET);
				}

				// Transform if class version is greater than the target.
				if (classJavaVersion > targetJavaVersion) {
					try {
						Map<String, byte[]> downgraded = downgrader.downgrade(new AtomicReference<>(className), classBytes, false, classes::get);
						if (downgraded != null)
							downgraded.forEach(transformConsumer);
					} catch (Throwable t) {
						logger.error("Failed down sampling '{}' to version {}", className, targetJavaVersion, t);
					}
				}
			});
		}
	}

	private static class RecafClassDowngrader extends ClassDowngrader {
		private final InheritanceGraph inheritanceGraph;

		public RecafClassDowngrader(@Nonnull Flags flags, @Nullable InheritanceGraph inheritanceGraph) {
			super(flags);

			this.inheritanceGraph = inheritanceGraph;
		}

		@Override
		public byte[] classNodeToBytes(@Nonnull ClassNode node) {
			ClassWriter cw;

			// Use our class writer which can pull workspace type information.
			if (inheritanceGraph != null) {
				cw = new WorkspaceClassWriter(inheritanceGraph, ClassWriter.COMPUTE_MAXS);
			} else {
				cw = new ClassWriter(ClassWriter.COMPUTE_MAXS) {
					@Override
					protected String getCommonSuperClass(String type1, String type2) {
						// Shouldn't be called for computing maxes, but we just want to be sure no
						// redundant classloading is attempted by the default implementation.
						return "java/lang/Object";
					}
				};
			}

			// Fallback to base impl
			node.accept(cw);
			return cw.toByteArray();
		}
	}
}
