package software.coley.recaf.services.search.similarity;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.collections.tree.SortedTree;
import software.coley.collections.tree.SortedTreeImpl;
import software.coley.collections.tree.Tree;
import software.coley.recaf.util.StringUtil;

/**
 * Utility for determining the "purpose" of a package or class based on its name.
 *
 * @author Matt Coley
 */
public class PackagePurpose {
	private static final Tree<String, String> packageBucketTree = new SortedTreeImpl<>();
	private static final Tree<String, String> classBucketTree = new SortedTreeImpl<>();
	private static final String DEFAULT_BUCKET = "MISC";
	private static final String BUCKET_BYTECODE = "BYTECODE";
	private static final String BUCKET_ENTERPRISE = "ENTERPRISE";
	private static final String BUCKET_IO = "IO";
	private static final String BUCKET_NATIVE = "NATIVE";
	private static final String BUCKET_NETWORKING = "NETWORKING";
	private static final String BUCKET_REFLECTION = "REFLECTION";
	private static final String BUCKET_SECURITY = "SECURITY";
	private static final String BUCKET_UI = "UI";
	private static final String BUCKET_UTIL = "UTIL";

	static {
		addBucket(packageBucketTree, "android/animation", BUCKET_UI);
		addBucket(packageBucketTree, "android/app", BUCKET_UI);
		addBucket(packageBucketTree, "android/appwidget", BUCKET_UI);
		addBucket(packageBucketTree, "android/bluetooth", BUCKET_NETWORKING);
		addBucket(packageBucketTree, "android/content/res", BUCKET_IO);
		addBucket(packageBucketTree, "android/database", BUCKET_IO);
		addBucket(packageBucketTree, "android/graphics", BUCKET_UI);
		addBucket(packageBucketTree, "android/graphics/pdf", BUCKET_IO);
		addBucket(packageBucketTree, "android/hardware", BUCKET_NATIVE);
		addBucket(packageBucketTree, "android/inputmethodservice", BUCKET_UI);
		addBucket(packageBucketTree, "android/media", BUCKET_IO);
		addBucket(packageBucketTree, "android/mtp", BUCKET_IO);
		addBucket(packageBucketTree, "android/net", BUCKET_NETWORKING);
		addBucket(packageBucketTree, "android/nfc", BUCKET_NETWORKING);
		addBucket(packageBucketTree, "android/opengl", BUCKET_NATIVE);
		addBucket(packageBucketTree, "android/os", BUCKET_NATIVE);
		addBucket(packageBucketTree, "android/preference", BUCKET_UI);
		addBucket(packageBucketTree, "android/print/pdf", BUCKET_IO);
		addBucket(packageBucketTree, "android/service", BUCKET_UI);
		addBucket(packageBucketTree, "android/system", BUCKET_NATIVE);
		addBucket(packageBucketTree, "android/telecom", BUCKET_NETWORKING);
		addBucket(packageBucketTree, "android/telephony", BUCKET_NETWORKING);
		addBucket(packageBucketTree, "android/transition", BUCKET_UI);
		addBucket(packageBucketTree, "android/view", BUCKET_UI);
		addBucket(packageBucketTree, "android/webkit", BUCKET_NETWORKING);
		addBucket(packageBucketTree, "android/widget", BUCKET_UI);
		addBucket(packageBucketTree, "android/window", BUCKET_UI);
		addBucket(packageBucketTree, "androidx/appcompat", BUCKET_UI);
		addBucket(packageBucketTree, "androidx/compose", BUCKET_UI);
		addBucket(packageBucketTree, "androidx/lifecycle", BUCKET_UI);
		addBucket(packageBucketTree, "ch/qos/logback", BUCKET_UTIL);
		addBucket(packageBucketTree, "com/alibaba/fastjson", BUCKET_UTIL);
		addBucket(packageBucketTree, "com/android/net", BUCKET_NETWORKING);
		addBucket(packageBucketTree, "com/android/okhttp", BUCKET_NETWORKING);
		addBucket(packageBucketTree, "com/fasterxml", BUCKET_UTIL);
		addBucket(packageBucketTree, "com/google/common", BUCKET_UTIL);
		addBucket(packageBucketTree, "com/google/common/codec", BUCKET_IO);
		addBucket(packageBucketTree, "com/google/common/collections", BUCKET_UTIL);
		addBucket(packageBucketTree, "com/google/common/io", BUCKET_IO);
		addBucket(packageBucketTree, "com/google/common/net", BUCKET_NETWORKING);
		addBucket(packageBucketTree, "com/google/common/reflect", BUCKET_REFLECTION);
		addBucket(packageBucketTree, "com/google/gson", BUCKET_UTIL);
		addBucket(packageBucketTree, "com/google/protobuf", BUCKET_IO);
		addBucket(packageBucketTree, "com/mojang/authlib", BUCKET_NETWORKING);
		addBucket(packageBucketTree, "com/squareup/moshi", BUCKET_UTIL);
		addBucket(packageBucketTree, "com/sun/javafx", BUCKET_UI);
		addBucket(packageBucketTree, "com/sun/jna", BUCKET_NATIVE);
		addBucket(packageBucketTree, "dalvik/system", BUCKET_REFLECTION); // Had a bunch of classloaders for dex files.
		addBucket(packageBucketTree, "io/netty", BUCKET_NETWORKING);
		addBucket(packageBucketTree, "it/unimi", BUCKET_UTIL); // Primitive collections.
		addBucket(packageBucketTree, "jakarta", BUCKET_ENTERPRISE);
		addBucket(packageBucketTree, "java/awt", BUCKET_UI);
		addBucket(packageBucketTree, "java/io", BUCKET_IO);
		addBucket(packageBucketTree, "java/lang/annotation", BUCKET_REFLECTION);
		addBucket(packageBucketTree, "java/lang/classfile", BUCKET_BYTECODE);
		addBucket(packageBucketTree, "java/lang/foreign", BUCKET_NATIVE);
		addBucket(packageBucketTree, "java/lang/instrument", BUCKET_BYTECODE);
		addBucket(packageBucketTree, "java/lang/invoke", BUCKET_REFLECTION);
		addBucket(packageBucketTree, "java/lang/reflect", BUCKET_REFLECTION);
		addBucket(packageBucketTree, "java/math", BUCKET_UTIL);
		addBucket(packageBucketTree, "java/net", BUCKET_NETWORKING);
		addBucket(packageBucketTree, "java/nio", BUCKET_IO);
		addBucket(packageBucketTree, "java/security", BUCKET_SECURITY);
		addBucket(packageBucketTree, "java/swing", BUCKET_UI);
		addBucket(packageBucketTree, "java/text", BUCKET_UTIL);
		addBucket(packageBucketTree, "java/time", BUCKET_UTIL);
		addBucket(packageBucketTree, "java/util", BUCKET_UTIL);
		addBucket(packageBucketTree, "java/util/jar", BUCKET_IO);
		addBucket(packageBucketTree, "java/util/zip", BUCKET_IO);
		addBucket(packageBucketTree, "java/xml", BUCKET_UTIL);
		addBucket(packageBucketTree, "javafx", BUCKET_UI);
		addBucket(packageBucketTree, "javax/crypto", BUCKET_SECURITY);
		addBucket(packageBucketTree, "javax/enterprise", BUCKET_ENTERPRISE);
		addBucket(packageBucketTree, "javax/persistence", BUCKET_ENTERPRISE);
		addBucket(packageBucketTree, "javax/security", BUCKET_SECURITY);
		addBucket(packageBucketTree, "javax/swing", BUCKET_UI);
		addBucket(packageBucketTree, "javax/xml", BUCKET_UTIL);
		addBucket(packageBucketTree, "kotlin", BUCKET_UTIL);
		addBucket(packageBucketTree, "kotlin/io", BUCKET_IO);
		addBucket(packageBucketTree, "kotlin/reflect", BUCKET_REFLECTION);
		addBucket(packageBucketTree, "kotlinx/coroutines", BUCKET_UTIL);
		addBucket(packageBucketTree, "kotlinx/serialization", BUCKET_IO);
		addBucket(packageBucketTree, "net/bytebuddy", BUCKET_BYTECODE);
		addBucket(packageBucketTree, "net/objenesis", BUCKET_REFLECTION);
		addBucket(packageBucketTree, "okhttp3", BUCKET_NETWORKING);
		addBucket(packageBucketTree, "okio", BUCKET_IO);
		addBucket(packageBucketTree, "org/apache/commons/codec", BUCKET_IO);
		addBucket(packageBucketTree, "org/apache/commons/collections", BUCKET_UTIL);
		addBucket(packageBucketTree, "org/apache/commons/collections4", BUCKET_UTIL);
		addBucket(packageBucketTree, "org/apache/commons/compress", BUCKET_IO);
		addBucket(packageBucketTree, "org/apache/commons/exec", BUCKET_NATIVE);
		addBucket(packageBucketTree, "org/apache/commons/io", BUCKET_IO);
		addBucket(packageBucketTree, "org/apache/commons/lang3", BUCKET_UTIL);
		addBucket(packageBucketTree, "org/apache/commons/lang3/reflect", BUCKET_REFLECTION);
		addBucket(packageBucketTree, "org/apache/hc", BUCKET_NETWORKING); // Apache HttpClient
		addBucket(packageBucketTree, "org/apache/logging", BUCKET_UTIL);
		addBucket(packageBucketTree, "org/bouncycastle", BUCKET_SECURITY);
		addBucket(packageBucketTree, "org/esotericsoftware/kryo", BUCKET_IO);
		addBucket(packageBucketTree, "org/esotericsoftware/reflectasm", BUCKET_REFLECTION);
		addBucket(packageBucketTree, "org/fxmisc", BUCKET_UI); // JavaFX misc libraries
		addBucket(packageBucketTree, "org/hibernate", BUCKET_ENTERPRISE);
		addBucket(packageBucketTree, "org/jboss", BUCKET_ENTERPRISE); // JBoss has lots of stuff, but this is an umbrella.
		addBucket(packageBucketTree, "org/joml", BUCKET_UTIL); // Math
		addBucket(packageBucketTree, "org/json", BUCKET_UTIL);
		addBucket(packageBucketTree, "org/lwjgl", BUCKET_UI); // OpenGL bindings
		addBucket(packageBucketTree, "org/objectweb/asm", BUCKET_BYTECODE);
		addBucket(packageBucketTree, "org/reflections", BUCKET_REFLECTION);
		addBucket(packageBucketTree, "org/slf4j", BUCKET_UTIL);
		addBucket(packageBucketTree, "org/spongepowered/asm", BUCKET_BYTECODE);
		addBucket(packageBucketTree, "org/spongepowered/tools", BUCKET_BYTECODE);
		addBucket(packageBucketTree, "org/springframework", BUCKET_ENTERPRISE);
		addBucket(packageBucketTree, "org/yaml", BUCKET_UTIL);
		addBucket(packageBucketTree, "oshi", BUCKET_NATIVE);
		addBucket(packageBucketTree, "retrofit2", BUCKET_NETWORKING);

		// Specific class overrides for classes that are in otherwise generic packages.
		addBucket(classBucketTree, "java/lang/Class", BUCKET_REFLECTION);
		addBucket(classBucketTree, "java/lang/ClassLoader", BUCKET_REFLECTION);
		addBucket(classBucketTree, "java/lang/Process", BUCKET_NATIVE);
		addBucket(classBucketTree, "java/lang/ProcessBuilder", BUCKET_NATIVE);
		addBucket(classBucketTree, "java/lang/ProcessEnvironment", BUCKET_NATIVE);
		addBucket(classBucketTree, "java/lang/ProcessHandle", BUCKET_NATIVE);
		addBucket(classBucketTree, "java/lang/Runtime", BUCKET_NATIVE);
		addBucket(classBucketTree, "jdk/internal/misc/Unsafe", BUCKET_NATIVE);
		addBucket(classBucketTree, "sun/misc/Unsafe", BUCKET_NATIVE);
	}

	/**
	 * @param internalName
	 * 		Internal type name.
	 *
	 * @return Bucket name for the type.
	 */
	@Nonnull
	public static String objectBucket(@Nonnull String internalName) {
		// Check for specific class buckets first.
		String classBucket = lookupBucket(classBucketTree, internalName);
		if (classBucket != null)
			return classBucket;

		// Check for package buckets.
		int lastSlash = internalName.lastIndexOf('/');
		if (lastSlash >= 0) {
			String packageBucket = lookupBucket(packageBucketTree, internalName.substring(0, lastSlash));
			if (packageBucket != null)
				return packageBucket;
		}

		// No bucket found, return default.
		return DEFAULT_BUCKET;
	}

	@Nullable
	private static String lookupBucket(@Nonnull Tree<String, String> tree, @Nonnull String pathName) {
		Tree<String, String> path = tree;
		String bucket = null;
		for (String section : StringUtil.fastSplit(pathName, false, '/')) {
			path = path.get(section);
			if (path == null)
				break;

			String value = path.getValue();
			if (value != null)
				bucket = value;
		}
		return bucket;
	}

	private static void addBucket(@Nonnull Tree<String, String> tree,
	                              @Nonnull String pathName,
	                              @Nonnull String bucket) {
		Tree<String, String> path = tree;
		var sections = StringUtil.fastSplit(pathName, false, '/');
		for (int i = 0; i < sections.size(); i++) {
			String section = sections.get(i);
			String value = i == sections.size() - 1 ? bucket : null;
			SortedTree<String, String> copy = (SortedTree<String, String>) path;
			path = path.computeIfAbsent(section, x -> new SortedTreeImpl<>(copy, value));
		}
	}
}
