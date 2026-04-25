package software.coley.recaf.services.search.similarity;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.dex.tree.definitions.code.Code;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.MethodMember;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Builds method fingerprints for JVM and Android classes.
 *
 * @author Matt Coley
 */
public final class MethodFingerprinting {
	private MethodFingerprinting() {}

	/**
	 * @param classInfo
	 * 		Class to make factory for.
	 *
	 * @return Class lookup method fingerprints.
	 */
	@Nonnull
	public static Lookup lookupFor(@Nonnull ClassInfo classInfo) {
		if (classInfo.isJvmClass())
			return new JvmLookup(classInfo.asJvmClass());
		if (classInfo.isAndroidClass())
			return new AndroidLookup(classInfo.asAndroidClass());
		throw new IllegalArgumentException("Unsupported class type: " + classInfo.getClass().getName());
	}

	@Nonnull
	private static Map<String, MethodNode> extractMethodNodes(@Nonnull JvmClassInfo classInfo) {
		// Read node structure (skipping debug info) for method instruction analysis.
		int flags = classInfo.getClassReaderFlags() | ClassReader.SKIP_DEBUG;
		ClassNode node = new ClassNode();
		classInfo.getClassReader().accept(node, flags);

		// Build map of methods by key.
		Map<String, MethodNode> methods = new HashMap<>(node.methods.size());
		for (MethodNode methodNode : node.methods)
			methods.put(methodKey(methodNode.name, methodNode.desc), methodNode);
		return methods;
	}

	@Nonnull
	private static String methodKey(@Nonnull MethodMember method) {
		return methodKey(method.getName(), method.getDescriptor());
	}

	@Nonnull
	private static String methodKey(@Nonnull String name, @Nonnull String descriptor) {
		return name + descriptor;
	}

	/**
	 * Class fingerprint lookup.
	 */
	public interface Lookup {
		/**
		 * @param method
		 * 		Method to fingerprint.
		 *
		 * @return Fingerprint of the method, or {@code null} when it has no executable body.
		 */
		@Nullable
		MethodFingerprint fingerprint(@Nonnull MethodMember method);
	}

	private static final class JvmLookup implements Lookup {
		private final Map<String, MethodNode> methodNodes;

		private JvmLookup(@Nonnull JvmClassInfo classInfo) {
			methodNodes = extractMethodNodes(classInfo);
		}

		@Override
		public MethodFingerprint fingerprint(@Nonnull MethodMember method) {
			MethodNode node = methodNodes.get(methodKey(method));
			if (node == null || node.instructions == null || node.instructions.size() == 0)
				return null;

			Type methodType = Type.getMethodType(method.getDescriptor());
			List<String> tokens = MethodInstructionNormalizer.normalizeInstructions(node);
			List<String> trigrams = MethodInstructionNormalizer.toTrigrams(tokens);
			return new MethodFingerprint(
					methodType.getArgumentTypes(),
					methodType.getReturnType(),
					new HashSet<>(method.getThrownTypes()),
					MethodInstructionNormalizer.multiset(trigrams),
					MethodInstructionNormalizer.computeControlFlowVector(node)
			);
		}
	}

	private static final class AndroidLookup implements Lookup {
		private final AndroidClassInfo classInfo;

		private AndroidLookup(@Nonnull AndroidClassInfo classInfo) {
			this.classInfo = classInfo;
		}

		@Override
		public MethodFingerprint fingerprint(@Nonnull MethodMember method) {
			me.darknet.dex.tree.definitions.MethodMember dexMethod =
					classInfo.getBackingDefinition().getMethod(method.getName(), method.getDescriptor());
			if (dexMethod == null)
				return null;

			Code code = dexMethod.getCode();
			if (!MethodInstructionNormalizer.hasExecutableInstructions(code))
				return null;

			Type methodType = Type.getMethodType(method.getDescriptor());
			List<String> tokens = MethodInstructionNormalizer.normalizeInstructions(code);
			List<String> trigrams = MethodInstructionNormalizer.toTrigrams(tokens);
			return new MethodFingerprint(
					methodType.getArgumentTypes(),
					methodType.getReturnType(),
					new HashSet<>(method.getThrownTypes()),
					MethodInstructionNormalizer.multiset(trigrams),
					MethodInstructionNormalizer.computeControlFlowVector(code)
			);
		}
	}
}
