package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.util.visitors.WorkspaceClassWriter;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.RuntimeWorkspaceResource;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.util.Pair;
import xyz.wagyourtail.jvmdg.version.map.FullyQualifiedMemberNameAndDesc;
import xyz.wagyourtail.jvmdg.version.map.MemberNameAndDesc;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

/**
 * Utility for wrapping {@link ClassDowngrader}.
 *
 * @author Matt Coley
 */
public class JavaDowngraderUtil {
	private static final Logger logger = Logging.get(JavaDowngraderUtil.class);
	private static final String[] undesirableStubs = {
			// Array of stubs (owner;name;desc) that we do not want to pass back to callers.
			"Lxyz/wagyourtail/jvmdg/j18/stub/java_base/J_L_System;getProperty;(Ljava/lang/String;)Ljava/lang/String;",
			"Lxyz/wagyourtail/jvmdg/j18/stub/java_base/J_L_System;getProperties;()Ljava/util/Properties;"
	};

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
		for (String undesirableStub : undesirableStubs)
			flags.debugSkipStub.add(FullyQualifiedMemberNameAndDesc.of(undesirableStub));

		try (ClassDowngrader downgrader = new RecafClassDowngrader(flags, inheritanceGraph)) {
			int maxClassFileVersion = downgrader.maxVersion();
			classes.forEach((className, classBytes) -> {
				int classFileVersion = classBytes[7];

				// Hack to ensure downgrader will run on bleeding edge versions of Java.
				// This will cause brand-new features to not be properly downgraded, but
				// not all bleeding edge classes use all the brand-new features. At least
				// trying is better than failing outright for most use cases.
				if (classFileVersion > maxClassFileVersion) {
					classBytes = Arrays.copyOf(classBytes, classBytes.length);
					classBytes[7] = (byte) (maxClassFileVersion);
				}

				// Transform if current class's version is greater than the target.
				if (classFileVersion > targetClassVersion) {
					try {
						Map<String, byte[]> downgraded = downgrader.downgrade(new AtomicReference<>(className), classBytes, false,
								key -> getBytes(maxClassFileVersion, classes, key));
						fillStubClasses(downgraded);
						if (downgraded != null)
							downgraded.forEach(transformConsumer);
					} catch (Throwable t) {
						logger.error("Failed down sampling '{}' to version {}", className, targetJavaVersion, t);
					}
				}
			});
		}
	}

	@Nullable
	private static byte[] getBytes(int maxClassFileVersion, @Nonnull Map<String, byte[]> classes, @Nonnull String key) {
		byte[] classBytes = classes.get(key);
		if (classBytes != null) {
			// Same version hack as above.
			int classFileVersion = classBytes[7];
			if (classFileVersion > maxClassFileVersion) {
				classBytes = Arrays.copyOf(classBytes, classBytes.length);
				classBytes[7] = (byte) (maxClassFileVersion);
			}
		}
		return classBytes;
	}

	private static void fillStubClasses(@Nullable Map<String, byte[]> downgraded) {
		if (downgraded == null || downgraded.isEmpty())
			return;

		// Find all referenced stubs from the downgraded classes.
		Set<String> referencedStubs = new HashSet<>();
		for (byte[] classBytes : downgraded.values()) {
			ClassReader reader = new ClassReader(classBytes);
			reader.accept(new ClassVisitor(RecafConstants.getAsmVersion()) {
				public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
					return new MethodVisitor(RecafConstants.getAsmVersion()) {
						public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
							if (owner.startsWith("xyz/wagyourtail/jvmdg/") && owner.contains("/stub/"))
								referencedStubs.add(owner);
						}

						public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
							if (owner.startsWith("xyz/wagyourtail/jvmdg/") && owner.contains("/stub/"))
								referencedStubs.add(owner);
						}
					};
				}
			}, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
		}

		// Add all referenced stubs to the downgraded output.
		JvmClassBundle runtimeBundle = RuntimeWorkspaceResource.getInstance().getJvmClassBundle();
		for (String referencedStub : referencedStubs) {
			JvmClassInfo cls = runtimeBundle.get(referencedStub);
			if (cls != null)
				downgraded.put(cls.getName(), cls.getBytecode());
		}
	}

	private static class RecafClassDowngrader extends ClassDowngrader {
		private final InheritanceGraph inheritanceGraph;

		public RecafClassDowngrader(@Nonnull Flags flags, @Nullable InheritanceGraph inheritanceGraph) {
			super(flags);

			this.inheritanceGraph = inheritanceGraph;

			// Compute the max-version. For the override methods below there are some cases where the base
			// implementation does version checks on classes used only for computing basic structure data.
			// We want these to at least have the chance to run vs failing outright, so we will cap the passed
			// version to the max supported by this downgrader.
			maxVersion();
		}

		@Override
		public Set<MemberNameAndDesc> getMembers(int version, Type type, Set<String> warnings) throws IOException {
			return super.getMembers(Math.min(maxVersion, version), type, warnings);
		}

		@Override
		public List<Pair<Type, Boolean>> getSupertypes(int version, Type type, Set<String> warnings) throws IOException {
			return super.getSupertypes(Math.min(maxVersion, version), type, warnings);
		}

		@Override
		public Boolean isInterface(int version, Type type, Set<String> warnings) throws IOException {
			return super.isInterface(Math.min(maxVersion, version), type, warnings);
		}

		@Override
		public Type stubClass(int version, Type type, Set<String> warnings) {
			return super.stubClass(Math.min(maxVersion, version), type, warnings);
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
