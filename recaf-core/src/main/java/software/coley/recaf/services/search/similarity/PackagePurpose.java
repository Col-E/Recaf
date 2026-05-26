package software.coley.recaf.services.search.similarity;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.collections.tree.SortedTree;
import software.coley.collections.tree.SortedTreeImpl;
import software.coley.collections.tree.Tree;
import software.coley.recaf.util.StringUtil;

import java.util.List;

/**
 * Utility for determining the "purpose" of a package or class based on its name.
 *
 * @author Matt Coley
 */
public class PackagePurpose {
	private static final Tree<String, String> packageBucketTree = new SortedTreeImpl<>();
	private static final Tree<String, String> classBucketTree = new SortedTreeImpl<>();

	// Bucket groups.
	public static final String DEFAULT_BUCKET = "MISC";
	public static final String BUCKET_BYTECODE = "BYTECODE";
	public static final String BUCKET_ENTERPRISE = "ENTERPRISE";
	public static final String BUCKET_IO = "IO";
	public static final String BUCKET_NATIVE = "NATIVE";
	public static final String BUCKET_NETWORKING = "NETWORKING";
	public static final String BUCKET_REFLECTION = "REFLECTION";
	public static final String BUCKET_SECURITY = "SECURITY";
	public static final String BUCKET_UI = "UI";
	public static final String BUCKET_UTIL = "UTIL";

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
		addBucket(packageBucketTree, "com/carrotdata/cache", BUCKET_ENTERPRISE); // Newer caching library
		addBucket(packageBucketTree, "com/fasterxml", BUCKET_UTIL);
		addBucket(packageBucketTree, "com/github/benmanes/caffeine", BUCKET_ENTERPRISE); // Caching library often used in enterprise apps.
		addBucket(packageBucketTree, "com/google/common", BUCKET_UTIL);
		addBucket(packageBucketTree, "com/google/common/codec", BUCKET_IO);
		addBucket(packageBucketTree, "com/google/common/collections", BUCKET_UTIL);
		addBucket(packageBucketTree, "com/google/common/io", BUCKET_IO);
		addBucket(packageBucketTree, "com/google/common/net", BUCKET_NETWORKING);
		addBucket(packageBucketTree, "com/google/common/reflect", BUCKET_REFLECTION);
		addBucket(packageBucketTree, "com/google/gson", BUCKET_UTIL);
		addBucket(packageBucketTree, "com/google/protobuf", BUCKET_IO);
		addBucket(packageBucketTree, "com/ibatis", BUCKET_ENTERPRISE); // ORM
		addBucket(packageBucketTree, "com/mojang/authlib", BUCKET_NETWORKING);
		addBucket(packageBucketTree, "com/rabbitmq", BUCKET_NETWORKING); // Messaging library
		addBucket(packageBucketTree, "com/squareup/moshi", BUCKET_UTIL); // JSON library
		addBucket(packageBucketTree, "com/sun/javafx", BUCKET_UI);
		addBucket(packageBucketTree, "com/sun/jna", BUCKET_NATIVE);
		addBucket(packageBucketTree, "com/sun/security", BUCKET_SECURITY);
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
		addBucket(packageBucketTree, "javax/imageio", BUCKET_IO);
		addBucket(packageBucketTree, "javax/management", BUCKET_UTIL); // JMX for monitoring and management
		addBucket(packageBucketTree, "javax/naming", BUCKET_ENTERPRISE); // JNDI for directory and naming services, often used in enterprise environments.
		addBucket(packageBucketTree, "javax/persistence", BUCKET_ENTERPRISE);
		addBucket(packageBucketTree, "javax/rmi", BUCKET_NETWORKING); // Remote Method Invocation for distributed computing.
		addBucket(packageBucketTree, "javax/security", BUCKET_SECURITY);
		addBucket(packageBucketTree, "javax/servlet", BUCKET_ENTERPRISE);
		addBucket(packageBucketTree, "javax/sound", BUCKET_IO);
		addBucket(packageBucketTree, "javax/sql", BUCKET_ENTERPRISE); // JDBC for database access, often used in enterprise applications.
		addBucket(packageBucketTree, "javax/swing", BUCKET_UI);
		addBucket(packageBucketTree, "javax/xml", BUCKET_UTIL);
		addBucket(packageBucketTree, "kafka", BUCKET_ENTERPRISE); // Distributed event streaming platform.
		addBucket(packageBucketTree, "kotlin", BUCKET_UTIL);
		addBucket(packageBucketTree, "kotlin/io", BUCKET_IO);
		addBucket(packageBucketTree, "kotlin/reflect", BUCKET_REFLECTION);
		addBucket(packageBucketTree, "kotlinx/coroutines", BUCKET_UTIL);
		addBucket(packageBucketTree, "kotlinx/serialization", BUCKET_IO);
		addBucket(packageBucketTree, "net/bytebuddy", BUCKET_BYTECODE);
		addBucket(packageBucketTree, "net/objenesis", BUCKET_REFLECTION);
		addBucket(packageBucketTree, "net/sf/ehcache", BUCKET_ENTERPRISE); // Caching library often used in enterprise apps.
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
		addBucket(packageBucketTree, "org/apache/hadoop", BUCKET_ENTERPRISE); // Big-data processing
		addBucket(packageBucketTree, "org/apache/hc", BUCKET_NETWORKING); // Apache HttpClient
		addBucket(packageBucketTree, "org/apache/ibatis", BUCKET_ENTERPRISE); // ORM
		addBucket(packageBucketTree, "org/apache/kafka", BUCKET_ENTERPRISE); // Distributed event streaming platform.
		addBucket(packageBucketTree, "org/apache/kerby", BUCKET_SECURITY); // Kerberos authentication, often used in enterprise environments.
		addBucket(packageBucketTree, "org/apache/logging", BUCKET_UTIL);
		addBucket(packageBucketTree, "org/bouncycastle", BUCKET_SECURITY);
		addBucket(packageBucketTree, "org/ehcache", BUCKET_ENTERPRISE); // Caching library often used in enterprise apps.
		addBucket(packageBucketTree, "org/esotericsoftware/kryo", BUCKET_IO);
		addBucket(packageBucketTree, "org/esotericsoftware/reflectasm", BUCKET_REFLECTION);
		addBucket(packageBucketTree, "org/fxmisc", BUCKET_UI); // JavaFX misc libraries
		addBucket(packageBucketTree, "org/hibernate", BUCKET_ENTERPRISE);
		addBucket(packageBucketTree, "org/ietf/jgss", BUCKET_ENTERPRISE); // GSS-API for secure authentication, often used in enterprise environments.
		addBucket(packageBucketTree, "org/jboss", BUCKET_ENTERPRISE); // JBoss has lots of stuff, but this is an umbrella.
		addBucket(packageBucketTree, "org/joml", BUCKET_UTIL); // Math
		addBucket(packageBucketTree, "org/json", BUCKET_UTIL);
		addBucket(packageBucketTree, "org/lwjgl", BUCKET_UI); // OpenGL bindings
		addBucket(packageBucketTree, "org/objectweb/asm", BUCKET_BYTECODE);
		addBucket(packageBucketTree, "org/omg", BUCKET_ENTERPRISE); // CORBA + other OMG standards often seen in enterprise environments.
		addBucket(packageBucketTree, "org/reflections", BUCKET_REFLECTION);
		addBucket(packageBucketTree, "org/slf4j", BUCKET_UTIL);
		addBucket(packageBucketTree, "org/spongepowered/asm", BUCKET_BYTECODE);
		addBucket(packageBucketTree, "org/spongepowered/tools", BUCKET_BYTECODE);
		addBucket(packageBucketTree, "org/springframework", BUCKET_ENTERPRISE);
		addBucket(packageBucketTree, "org/yaml", BUCKET_UTIL);
		addBucket(packageBucketTree, "oshi", BUCKET_NATIVE); // OS and hardware information
		addBucket(packageBucketTree, "reactor", BUCKET_UTIL); // Reactive streams
		addBucket(packageBucketTree, "retrofit2", BUCKET_NETWORKING); // REST client

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

	/**
	 * @param bucket
	 * 		Bucket name.
	 *
	 * @return Human-friendly bucket name for display purposes.
	 */
	@Nonnull
	public static String toString(@Nonnull String bucket) {
		return switch (bucket) {
			case BUCKET_BYTECODE -> "Bytecode Manipulation";
			case BUCKET_ENTERPRISE -> "Enterprise";
			case BUCKET_IO -> "Input/Output";
			case BUCKET_NATIVE -> "Native/OS";
			case BUCKET_NETWORKING -> "Networking";
			case BUCKET_REFLECTION -> "Reflection/Introspection";
			case BUCKET_SECURITY -> "Security/Cryptography";
			case BUCKET_UI -> "User Interface";
			case BUCKET_UTIL -> "Utilities";
			case DEFAULT_BUCKET -> "Miscellaneous";
			default -> bucket;
		};
	}

	/**
	 * @return All known bucket names in display order.
	 */
	@Nonnull
	public static List<String> buckets() {
		return List.of(
				BUCKET_BYTECODE,
				BUCKET_ENTERPRISE,
				BUCKET_IO,
				BUCKET_NATIVE,
				BUCKET_NETWORKING,
				BUCKET_REFLECTION,
				BUCKET_SECURITY,
				BUCKET_UI,
				BUCKET_UTIL,
				DEFAULT_BUCKET
		);
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
