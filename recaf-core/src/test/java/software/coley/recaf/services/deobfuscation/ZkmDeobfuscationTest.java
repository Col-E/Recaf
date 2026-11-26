package software.coley.recaf.services.deobfuscation;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.CheckClassAdapter;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.deobfuscation.transform.generic.CallResultInliningTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.InvokeDynamicInliningTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.OpaqueConstantFoldingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.StaticValueCollectionTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.StaticValueInliningTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.VariableFoldingTransformer;
import software.coley.recaf.services.deobfuscation.transform.specific.zkm.ZkmDecryptionCleanupTransformer;
import software.coley.recaf.services.deobfuscation.transform.specific.zkm.ZkmInvokeDynamicResolver;
import software.coley.recaf.services.deobfuscation.transform.specific.zkm.ZkmParameterUnpackingTransformer;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.transform.ClassTransformer;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformResult;
import software.coley.recaf.services.transform.TransformationApplier;
import software.coley.recaf.services.transform.TransformationApplierService;
import software.coley.recaf.services.transform.TransformationFeedback;
import software.coley.recaf.services.transform.TransformationParameters;
import software.coley.recaf.services.workspace.io.PathWorkspaceExportConsumer;
import software.coley.recaf.services.workspace.io.ResourceImporter;
import software.coley.recaf.services.workspace.io.WorkspaceExportOptions;
import software.coley.recaf.services.workspace.io.WorkspaceOutputType;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.util.analysis.ReInterpreter;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.workspace.model.BasicWorkspace;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.BasicJvmClassBundle;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResourceBuilder;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ZKM deobfuscation transformers.
 */
class ZkmDeobfuscationTest extends TestBase {
	private static final String FIXTURE_DIRECTORY = "src/testFixtures/resources/samples";
	private static final String STRING_PACKAGE_PREFIX = "sample/string/";
	private static final String DECODER_DESCRIPTOR = "(III)Ljava/lang/String;";
	private static final String ZKM_BOOTSTRAP_DESCRIPTOR = ZkmInvokeDynamicResolver.BOOTSTRAP_DESCRIPTOR;
	private static final String STRING_ARRAY_DESCRIPTOR = "[Ljava/lang/String;";
	private static final List<String> STRING_CLASSES = List.of(
			"sample/string/StringsDuplicates",
			"sample/string/StringsLong",
			"sample/string/StringsDummyApp");

	/** Verifies direct ZKM decoder calls are recovered without changing deferred mechanisms or runtime behavior. */
	@Test
	void inlinesZkmStringDecoderCalls(@TempDir Path tempDirectory) throws Exception {
		// Load each artifact independently, retaining only its JVM classes in the analysis workspaces.
		Path baselineJar = Paths.get(FIXTURE_DIRECTORY, "obf-sample-reference.jar");
		Path sampleJar = Paths.get(FIXTURE_DIRECTORY, "obf-sample-zkm-26.jar");
		Workspace baselineWorkspace = loadClassOnlyWorkspace(baselineJar);
		Workspace transformedWorkspace = loadClassOnlyWorkspace(sampleJar);
		WorkspaceResource transformedResource = transformedWorkspace.getPrimaryResource();
		assertTrue(transformedResource.getFileBundle().isEmpty(), "Fixture source entries must not enter the workspace");
		assertTrue(transformedResource.getEmbeddedResources().isEmpty(), "Fixture embedded resources must not enter the workspace");
		assertTrue(baselineWorkspace.getPrimaryResource().getFileBundle().isEmpty(), "Baseline source entries must not enter the workspace");
		assertTrue(baselineWorkspace.getPrimaryResource().getEmbeddedResources().isEmpty(), "Baseline embedded resources must not enter the workspace");

		// Snapshot classes outside the selected package before any transformer can touch the workspace.
		JvmClassBundle transformedClasses = transformedResource.getJvmClassBundle();
		Map<String, byte[]> unchangedBytecode = snapshotNonStringBytecode(transformedClasses);
		int initialInvokeDynamicCount = countInvokeDynamics(transformedClasses);
		int initialDecoderCallCount = countDecoderCalls(transformedClasses);
		assertTrue(initialDecoderCallCount > 0, "Expected direct class-local decoder calls in the fixture");

		// Collect static pools first, then evaluate exact direct calls with initializer evaluation enabled.
		workspaceManager.setCurrentIgnoringConditions(transformedWorkspace);
		TransformationApplier applier = Objects.requireNonNull(recaf.get(TransformationApplierService.class).newApplierForCurrentWorkspace());
		applier.setMaxPasses(1);
		TransformationParameters parameters = new TransformationParameters(Map.of(
				StaticValueCollectionTransformer.KEY_MAX_STEPS, 100_000,
				CallResultInliningTransformer.KEY_MAX_STEPS, 1_000_000,
				CallResultInliningTransformer.KEY_EVALUATE_CLASS_INITIALIZERS, true));
		TransformationFeedback feedback = new TransformationFeedback() {
			@Override
			public boolean shouldTransform(@Nonnull Workspace workspace, @Nonnull WorkspaceResource resource, @Nonnull ClassBundle<?> bundle,
			                               @Nonnull ClassInfo classInfo, @Nonnull ClassTransformer transformer, int pass) {
				return classInfo.getName().startsWith(STRING_PACKAGE_PREFIX);
			}
		};
		JvmTransformResult result = applier.transformJvm(List.of(StaticValueCollectionTransformer.class, CallResultInliningTransformer.class), parameters, feedback);
		assertTrue(result.getTransformerFailures().isEmpty(), "There were transformation failures");
		assertFalse(result.getTransformedClasses().isEmpty(), "Expected direct decoder calls to be transformed");
		result.apply();

		// Confirm all exact direct calls changed while helpers, pools, and dynamic sites remain present.
		// One call has an unknown merged argument, so the generic safety contract leaves it untouched.
		assertEquals(1, countDecoderCalls(transformedClasses), "Only the decoder call with an unknown merged argument may remain");
		assertEquals(initialInvokeDynamicCount, countInvokeDynamics(transformedClasses), "InvokeDynamic instructions must remain unchanged");
		assertNonStringBytecodeUnchanged(unchangedBytecode, transformedClasses);
		for (String className : STRING_CLASSES) {
			ClassNode node = parseClassNode(findClass(transformedWorkspace, className));
			MethodNode decoder = findMethod(node, DECODER_DESCRIPTOR);
			assertTrue((decoder.access & Opcodes.ACC_PRIVATE) != 0, "Decoder must remain private: " + className);
			assertTrue((decoder.access & Opcodes.ACC_STATIC) != 0, "Decoder must remain static: " + className);
			assertTrue(node.fields.stream().anyMatch(field ->
							(field.access & Opcodes.ACC_PRIVATE) != 0
									&& (field.access & Opcodes.ACC_STATIC) != 0
									&& STRING_ARRAY_DESCRIPTOR.equals(field.desc)),
					"Generated String[] pool/cache fields must remain: " + className);
		}

		MethodNode duplicate = findMethod(parseClassNode(findClass(transformedWorkspace, "sample/string/StringsDuplicates")), "duplicate", "()Ljava/lang/String;");
		assertTrue(hasStringConstant(duplicate, "Hello this is a duplicate string"), "Duplicate string call must become an exact LDC constant");
		MethodNode long1 = findMethod(parseClassNode(findClass(transformedWorkspace, "sample/string/StringsLong")), "long1", "()V");
		assertTrue(hasStringConstantStartingWith(long1, "Lorem ipsum dolor sit amet"), "Long string call must become a plaintext constant");
		MethodNode dummyInitializer = findMethod(parseClassNode(findClass(transformedWorkspace, "sample/string/StringsDummyApp")), "<clinit>", "()V");
		assertTrue(hasStringConstantContaining(dummyInitializer, "===      WELCOME       ==="), "Welcome banner must be assembled from recovered constants");

		// Export only the transformed class bundle, then compare all selected entry points with the baseline JAR.
		Path transformedDirectory = tempDirectory.resolve("transformed");
		exportClassOnlyWorkspace(transformedWorkspace, transformedDirectory);
		assertEquals(runJava(baselineJar, "sample.string.StringsDuplicates", ""),
				runJava(transformedDirectory, "sample.string.StringsDuplicates", ""),
				"StringsDuplicates output changed");
		assertEquals(runJava(baselineJar, "sample.string.StringsLong", ""),
				runJava(transformedDirectory, "sample.string.StringsLong", ""),
				"StringsLong output changed");
		assertEquals(runJava(baselineJar, "sample.string.StringsDummyApp", "5\n"),
				runJava(transformedDirectory, "sample.string.StringsDummyApp", "5\n"),
				"StringsDummyApp output changed");
	}

	/** Verifies ZKM reference sites become direct calls and their unused bootstrap methods are removed. */
	@Test
	void rewritesZkmReferenceLookups(@TempDir Path tempDirectory) throws Exception {
		Path baselineJar = Paths.get(FIXTURE_DIRECTORY, "obf-sample-reference.jar");
		Path sampleJar = Paths.get(FIXTURE_DIRECTORY, "obf-sample-zkm-26.jar");
		Workspace workspace = loadClassOnlyWorkspace(sampleJar);
		workspaceManager.setCurrentIgnoringConditions(workspace);
		JvmClassBundle classes = workspace.getPrimaryResource().getJvmClassBundle();
		int initialDynamicCount = countZkmInvokeDynamics(classes);
		int initialLambdaCount = countLambdaInvokeDynamics(classes);
		int initialLookupCount = countZkmLookupMethods(classes);
		assertTrue(initialDynamicCount > 0, "Expected ZKM reference call sites in the fixture");
		assertTrue(initialLookupCount > 0, "Expected ZKM bootstrap lookup methods in the fixture");

		// Recover direct member references while all generated metadata is still available to the resolver.
		TransformationApplier resolverApplier = Objects.requireNonNull(recaf.get(TransformationApplierService.class).newApplierForCurrentWorkspace());
		resolverApplier.setMaxPasses(1);
		JvmTransformResult resolverResult = resolverApplier.transformJvm(
				List.of(InvokeDynamicInliningTransformer.class),
				new TransformationParameters(Map.of(InvokeDynamicInliningTransformer.KEY_MAX_STEPS, 100_000)));
		assertTrue(resolverResult.getTransformerFailures().isEmpty(), "Reference transformation failed");
		assertFalse(resolverResult.getTransformedClasses().isEmpty(), "Expected ZKM references to be transformed");
		resolverResult.apply();

		assertEquals(0, countZkmInvokeDynamics(classes), "All ZKM reference sites should be restored");
		assertEquals(initialLambdaCount, countLambdaInvokeDynamics(classes), "Standard LambdaMetafactory sites must remain dynamic");
		assertEquals(initialLookupCount, countZkmLookupMethods(classes), "Bootstrap methods remain until their references are checked");
		ClassNode stringsLong = parseClassNode(findClass(workspace, "sample/string/StringsLong"));
		MethodNode long5 = findMethod(stringsLong, "long5", "()V");
		assertTrue(StreamSupport.stream(long5.instructions.spliterator(), false).anyMatch(instruction -> instruction instanceof MethodInsnNode method
						&& method.getOpcode() == Opcodes.INVOKESTATIC
						&& stringsLong.name.equals(method.owner)
						&& "p".equals(method.name)
						&& "(Ljava/lang/String;)V".equals(method.desc)),
				"long5 should directly invoke p(String)");

		// Keep private lookup methods when the caller has not ruled out reflection.
		TransformationApplier conservativeCleanup = Objects.requireNonNull(recaf.get(TransformationApplierService.class).newApplierForCurrentWorkspace());
		conservativeCleanup.setMaxPasses(1);
		JvmTransformResult conservativeResult = conservativeCleanup.transformJvm(List.of(ZkmDecryptionCleanupTransformer.class));
		assertTrue(conservativeResult.getTransformerFailures().isEmpty(), "Conservative cleanup failed");
		assertTrue(conservativeResult.getTransformedClasses().isEmpty(), "Cleanup must remain opt-in when reflection may observe private helpers");
		assertEquals(initialLookupCount, countZkmLookupMethods(classes));

		// Keep the inliner in this phase so cleanup can verify that dynamic references were handled first.
		TransformationApplier cleanupApplier = Objects.requireNonNull(recaf.get(TransformationApplierService.class).newApplierForCurrentWorkspace());
		cleanupApplier.setMaxPasses(1);
		JvmTransformResult cleanupResult = cleanupApplier.transformJvm(List.of(InvokeDynamicInliningTransformer.class, ZkmDecryptionCleanupTransformer.class));
		assertTrue(cleanupResult.getTransformerFailures().isEmpty(), "Reference cleanup failed");
		cleanupResult.apply();
		assertEquals(0, countZkmLookupMethods(classes), "Unreferenced ZKM bootstrap methods should be removed");

		// The direct rewrite must preserve the original observable output even before string values are inlined.
		Path transformedDirectory = tempDirectory.resolve("transformed");
		exportClassOnlyWorkspace(workspace, transformedDirectory);
		assertEquals(runJava(baselineJar, "sample.string.StringsLong", ""),
				runJava(transformedDirectory, "sample.string.StringsLong", ""),
				"StringsLong output changed after reference restoration");
	}

	/** Verifies ZKM helper methods and storage fields can be removed after all useful values and calls are recovered. */
	@Test
	void cleansZkmHelperMembersAfterPipeline(@TempDir Path tempDirectory) throws Exception {
		Path baselineJar = Paths.get(FIXTURE_DIRECTORY, "obf-sample-reference.jar");
		Workspace workspace = loadClassOnlyWorkspace(Paths.get(FIXTURE_DIRECTORY, "obf-sample-zkm-26.jar"));
		Workspace baselineWorkspace = loadClassOnlyWorkspace(baselineJar);
		workspaceManager.setCurrentIgnoringConditions(workspace);

		TransformationApplier applier = Objects.requireNonNull(recaf.get(TransformationApplierService.class).newApplierForCurrentWorkspace());
		applier.setMaxPasses(1);
		TransformationParameters parameters = new TransformationParameters(Map.of(
				StaticValueCollectionTransformer.KEY_MAX_STEPS, 1_000_000,
				InvokeDynamicInliningTransformer.KEY_MAX_STEPS, 1_000_000,
				CallResultInliningTransformer.KEY_MAX_STEPS, 1_000_000,
				CallResultInliningTransformer.KEY_EVALUATE_CLASS_INITIALIZERS, true));
		List<Class<? extends JvmClassTransformer>> pipeline = List.of(
				StaticValueCollectionTransformer.class,
				InvokeDynamicInliningTransformer.class,
				CallResultInliningTransformer.class,
				StaticValueInliningTransformer.class,
				VariableFoldingTransformer.class,
				OpaqueConstantFoldingTransformer.class,
				ZkmDecryptionCleanupTransformer.class);
		JvmTransformResult result = applier.transformJvm(pipeline, parameters);
		assertTrue(result.getTransformerFailures().isEmpty(), "Pipeline transformation failed");
		result.apply();

		ClassNode transformed = parseClassNode(findClass(workspace, "sample/string/StringsLong"));
		ClassNode baseline = parseClassNode(findClass(baselineWorkspace, "sample/string/StringsLong"));
		assertEquals(memberKeys(baseline), memberKeys(transformed),
				"StringsLong should retain only the baseline members after cleanup");

		// Export the cleaned classes and exercise the transformed class whose helper component was removed.
		Path transformedDirectory = tempDirectory.resolve("transformed");
		exportClassOnlyWorkspace(workspace, transformedDirectory);
		assertEquals(runJava(baselineJar, "sample.string.StringsDuplicates", ""),
				runJava(transformedDirectory, "sample.string.StringsDuplicates", ""),
				"StringsDuplicates output changed after helper cleanup");
		assertEquals(runJava(baselineJar, "sample.string.StringsLong", ""),
				runJava(transformedDirectory, "sample.string.StringsLong", ""),
				"StringsLong output changed after helper cleanup");
	}

	/** Verifies slight ZKM config variations of reference encryption + strings through "Method Parameter changes" still get deobfuscated. */
	@Test
	void rewritesSharedZkmReferenceLookups(@TempDir Path tempDirectory) throws Exception {
		Path baselineJar = Paths.get(FIXTURE_DIRECTORY, "obf-sample-zkm-26-3e-original.jar");
		List<String> samples = List.of(
				"obf-sample-zkm-26-3e-shared-1.jar",
				"obf-sample-zkm-26-3e-shared-2.jar",
				"obf-sample-zkm-26-3e-shared-3.jar");
		for (String sample : samples) {
			Workspace workspace = loadClassOnlyWorkspace(Paths.get(FIXTURE_DIRECTORY, sample));
			workspaceManager.setCurrentIgnoringConditions(workspace);
			WorkspaceResource resource = workspace.getPrimaryResource();
			JvmClassBundle classes = resource.getJvmClassBundle();

			// Assert all samples have ZKM reference encryption calls + bootstrapping.
			int initialZkmCount = countZkmInvokeDynamics(classes);
			int initialLambdaCount = countLambdaInvokeDynamics(classes);
			int initialLookupCount = countZkmLookupMethods(classes);
			assertTrue(initialZkmCount > 0, "Expected shared ZKM sites in " + sample);
			assertTrue(initialLookupCount > 0, "Expected shared ZKM lookup methods in " + sample);

			// All samples covered in this test have a shared helper class that is used to bootstrap the ZKM reference sites.
			ClassNode helper = parseClassNode(findClass(workspace, "n"));
			String indexDescriptor = sample.endsWith("shared-3.jar") ? "(J)I" : "(JJ)I";
			assertTrue(describeZkmSites(classes).stream().anyMatch(site -> site.endsWith(" @n")), "Expected a ZKM site bootstrapped by n in " + sample);
			assertTrue(helper.methods.stream().anyMatch(method -> indexDescriptor.equals(method.desc)), "Missing " + indexDescriptor + " helper in " + sample);

			// Setup transformation.
			TransformationApplier applier = Objects.requireNonNull(recaf.get(TransformationApplierService.class).newApplierForCurrentWorkspace());
			applier.setMaxPasses(8);
			TransformationParameters parameters = new TransformationParameters(Map.of(InvokeDynamicInliningTransformer.KEY_MAX_STEPS, 250_000));
			List<Class<? extends JvmClassTransformer>> pipeline = List.of(
					ZkmParameterUnpackingTransformer.class,
					OpaqueConstantFoldingTransformer.class,
					VariableFoldingTransformer.class,
					InvokeDynamicInliningTransformer.class,
					ZkmDecryptionCleanupTransformer.class);

			// Apply transformation, validate we saw changes.
			JvmTransformResult result = applier.transformJvm(pipeline, parameters);
			assertTrue(result.getTransformerFailures().isEmpty(), "Shared reference transformation failed for " + sample);
			assertFalse(result.getTransformedClasses().isEmpty(), "Expected shared ZKM classes to be transformed: " + sample);

			// After applying there should be no reference encryption calls remaining.
			result.apply();
			assertEquals(0, countZkmInvokeDynamics(classes), "All shared ZKM sites should be restored: " + sample + " " + describeZkmSites(classes));
			assertEquals(initialLambdaCount, countLambdaInvokeDynamics(classes), "Lambda sites must remain dynamic: " + sample);

			// Bootstrapping methods should be removed if they are no longer referenced.
			assertEquals(0, countZkmLookupMethods(classes), "Unreferenced shared ZKM bootstrap methods should be removed: " + sample);
			ClassNode cleanedHelper = parseClassNode(findClass(workspace, "n"));
			assertEquals(Set.of("M <init>()V"), memberKeys(cleanedHelper), "Shared helper should retain only its constructor: " + sample);

			// If we run the original classes and the transformed classes, the output should be the same.
			Path transformedDirectory = tempDirectory.resolve(sample);
			exportClassOnlyWorkspace(workspace, transformedDirectory);
			verifyExportedClasses(classes, transformedDirectory);
			String baselineOutput = runJava(baselineJar, "dev.lvstrng.Main", "");
			assertTrue(baselineOutput.contains("Executed all tests!"), "Baseline did not complete: " + sample);
			assertEquals(baselineOutput, runJava(transformedDirectory, "a", ""), "Shared transformed output changed: " + sample);
		}
	}

	/** Ensures opaque constant folding reaches a fixed point when analyzing the obfuscated reflection helper. */
	@Test
	@Timeout(value = 10)
	void opaqueConstantFoldingAnalyzesBinarySearchMethod() throws Exception {
		// Keep the workspace limited to class files so unrelated archive entries cannot affect the graph.
		Workspace workspace = loadClassOnlyWorkspace(Paths.get(FIXTURE_DIRECTORY, "obf-sample-zkm-26.jar"));
		workspaceManager.setCurrentIgnoringConditions(workspace);

		// The two merge orders must agree, otherwise cyclic paths can keep changing a frame's reference type.
		ReInterpreter interpreter = new ReInterpreter(new InheritanceGraph(workspace));
		ReValue objectNull = ObjectValue.VAL_OBJECT_NULL;
		ReValue classNull = ObjectValue.VAL_CLASS_NULL;
		ReValue objectNotNull = ObjectValue.VAL_OBJECT;
		ReValue mergedFromClass = interpreter.merge(interpreter.merge(objectNull, classNull), objectNotNull);
		ReValue mergedFromObject = interpreter.merge(interpreter.merge(objectNull, objectNotNull), classNull);
		assertEquals(Type.getObjectType("java/lang/Object"), mergedFromClass.type());
		assertEquals(mergedFromClass, mergedFromObject, "Typed-null merges must be order-independent");

		TransformationApplier applier = Objects.requireNonNull(recaf.get(TransformationApplierService.class).newApplierForCurrentWorkspace());
		applier.setMaxPasses(1);
		TransformationFeedback feedback = new TransformationFeedback() {
			@Override
			public boolean shouldTransform(@Nonnull Workspace currentWorkspace, @Nonnull WorkspaceResource resource, @Nonnull ClassBundle<?> bundle,
			                               @Nonnull ClassInfo classInfo, @Nonnull ClassTransformer transformer, int pass) {
				return "sample/math/BinarySearch".equals(classInfo.getName())
						&& transformer instanceof OpaqueConstantFoldingTransformer;
			}
		};
		JvmTransformResult result = applier.transformJvm(List.of(OpaqueConstantFoldingTransformer.class), new TransformationParameters(Map.of()), feedback);
		assertTrue(result.getTransformerFailures().isEmpty(), "There were transformation failures");
	}

	private static Set<String> memberKeys(ClassNode node) {
		Set<String> keys = new LinkedHashSet<>(); // Keep insertion order
		node.fields.forEach(field -> keys.add("F " + field.name + field.desc));
		node.methods.forEach(method -> keys.add("M " + method.name + method.desc));
		return keys;
	}

	private static Workspace loadClassOnlyWorkspace(Path jarPath) throws IOException {
		ResourceImporter importer = recaf.get(ResourceImporter.class);
		WorkspaceResource imported = importer.importResource(jarPath);
		try {
			// Detach the class bundle so plaintext source entries and archive metadata cannot be analyzed or exported.
			BasicJvmClassBundle classes = new BasicJvmClassBundle();
			imported.getJvmClassBundle().forEach(classes::initialPut);
			return new BasicWorkspace(new WorkspaceResourceBuilder().withJvmClassBundle(classes).build());
		} finally {
			imported.close();
		}
	}

	private static ClassNode parseClassNode(JvmClassInfo classInfo) {
		ClassNode node = new ClassNode();
		int flags = classInfo.getClassReaderFlags() | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG;
		classInfo.getClassReader().accept(node, flags);
		return node;
	}

	private static int countDecoderCalls(JvmClassBundle classes) {
		int count = 0;
		for (JvmClassInfo classInfo : classes) {
			if (!classInfo.getName().startsWith(STRING_PACKAGE_PREFIX))
				continue;
			ClassNode node = parseClassNode(classInfo);
			for (MethodNode method : node.methods) {
				if (method.instructions == null)
					continue;
				for (AbstractInsnNode instruction : method.instructions) {
					if (instruction.getOpcode() == Opcodes.INVOKESTATIC
							&& instruction instanceof MethodInsnNode methodInsn
							&& node.name.equals(methodInsn.owner)
							&& DECODER_DESCRIPTOR.equals(methodInsn.desc))
						count++;
				}
			}
		}
		return count;
	}

	private static JvmClassInfo findClass(Workspace workspace, String className) {
		JvmClassInfo classInfo = workspace.getPrimaryResource().getJvmClassBundle().get(className);
		assertNotNull(classInfo, "Missing class: " + className);
		return classInfo;
	}

	private static MethodNode findMethod(ClassNode node, String descriptor) {
		return node.methods.stream()
				.filter(method -> descriptor.equals(method.desc))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Missing method with descriptor " + descriptor + " in " + node.name));
	}

	private static MethodNode findMethod(ClassNode node, String name, String descriptor) {
		return node.methods.stream()
				.filter(method -> name.equals(method.name) && descriptor.equals(method.desc))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Missing method " + node.name + "." + name + descriptor));
	}

	private static Map<String, byte[]> snapshotNonStringBytecode(JvmClassBundle classes) {
		Map<String, byte[]> snapshot = new HashMap<>();
		for (JvmClassInfo classInfo : classes) {
			if (!classInfo.getName().startsWith(STRING_PACKAGE_PREFIX))
				snapshot.put(classInfo.getName(), Arrays.copyOf(classInfo.getBytecode(), classInfo.getBytecode().length));
		}
		return snapshot;
	}

	private static void assertNonStringBytecodeUnchanged(Map<String, byte[]> snapshot, JvmClassBundle classes) {
		int nonStringClassCount = 0;
		for (JvmClassInfo classInfo : classes) {
			if (classInfo.getName().startsWith(STRING_PACKAGE_PREFIX))
				continue;
			nonStringClassCount++;
			byte[] before = snapshot.get(classInfo.getName());
			assertNotNull(before, "Unexpected non-string class: " + classInfo.getName());
			assertArrayEquals(before, classInfo.getBytecode(), "Non-string class changed: " + classInfo.getName());
		}
		assertEquals(snapshot.size(), nonStringClassCount, "Non-string class set changed");
	}

	private static int countZkmInvokeDynamics(JvmClassBundle classes) {
		int count = 0;
		for (JvmClassInfo classInfo : classes) {
			ClassNode node = parseClassNode(classInfo);
			for (MethodNode method : node.methods) {
				if (method.instructions == null)
					continue;
				for (AbstractInsnNode instruction : method.instructions)
					if (instruction instanceof InvokeDynamicInsnNode indy && ZkmInvokeDynamicResolver.isZkmSite(indy))
						count++;
			}
		}
		return count;
	}

	@Nonnull
	private static List<String> describeZkmSites(JvmClassBundle classes) {
		List<String> result = new ArrayList<>();
		for (JvmClassInfo classInfo : classes) {
			ClassNode node = parseClassNode(classInfo);
			for (MethodNode method : node.methods) {
				if (method.instructions == null)
					continue;
				for (AbstractInsnNode instruction : method.instructions)
					if (instruction instanceof InvokeDynamicInsnNode indy && ZkmInvokeDynamicResolver.isZkmSite(indy))
						result.add(node.name + "." + method.name + method.desc + " -> " + indy.name + indy.desc + " @" + indy.bsm.getOwner());
			}
		}
		return result;
	}

	private static int countLambdaInvokeDynamics(JvmClassBundle classes) {
		int count = 0;
		for (JvmClassInfo classInfo : classes) {
			ClassNode node = parseClassNode(classInfo);
			for (MethodNode method : node.methods) {
				if (method.instructions == null)
					continue;
				for (AbstractInsnNode instruction : method.instructions)
					if (instruction instanceof InvokeDynamicInsnNode indy
							&& indy.bsm != null
							&& "java/lang/invoke/LambdaMetafactory".equals(indy.bsm.getOwner()))
						count++;
			}
		}
		return count;
	}

	private static int countZkmLookupMethods(JvmClassBundle classes) {
		int count = 0;
		for (JvmClassInfo classInfo : classes) {
			ClassNode node = parseClassNode(classInfo);
			for (MethodNode method : node.methods)
				if (ZKM_BOOTSTRAP_DESCRIPTOR.equals(method.desc))
					count++;
		}
		return count;
	}

	private static int countInvokeDynamics(JvmClassBundle classes) {
		int count = 0;
		for (JvmClassInfo classInfo : classes) {
			ClassNode node = parseClassNode(classInfo);
			for (MethodNode method : node.methods) {
				if (method.instructions == null)
					continue;
				for (AbstractInsnNode instruction : method.instructions)
					if (instruction instanceof InvokeDynamicInsnNode)
						count++;
			}
		}
		return count;
	}

	private static boolean hasStringConstant(MethodNode method, String expected) {
		return stringConstants(method).stream().anyMatch(expected::equals);
	}

	private static boolean hasStringConstantStartingWith(MethodNode method, String prefix) {
		return stringConstants(method).stream().anyMatch(value -> value.startsWith(prefix));
	}

	private static boolean hasStringConstantContaining(MethodNode method, String fragment) {
		return stringConstants(method).stream().anyMatch(value -> value.contains(fragment));
	}

	private static List<String> stringConstants(MethodNode method) {
		return StreamSupport.stream(method.instructions.spliterator(), false)
				.filter(LdcInsnNode.class::isInstance)
				.map(LdcInsnNode.class::cast)
				.map(instruction -> instruction.cst)
				.filter(String.class::isInstance)
				.map(String.class::cast)
				.toList();
	}

	private static void exportClassOnlyWorkspace(Workspace workspace, Path outputDirectory) throws IOException {
		WorkspaceExportOptions options = new WorkspaceExportOptions(WorkspaceOutputType.DIRECTORY, new PathWorkspaceExportConsumer(outputDirectory));
		options.create().export(workspace);
	}

	private static void verifyExportedClasses(@Nonnull JvmClassBundle classes, Path outputDirectory) throws IOException {
		try (TestClassLoader loader = new TestClassLoader(outputDirectory)) {
			for (JvmClassInfo classInfo : classes) {
				StringWriter verificationErrors = new StringWriter();
				PrintWriter writer = new PrintWriter(verificationErrors);
				assertDoesNotThrow(() -> CheckClassAdapter.verify(new ClassReader(classInfo.getBytecode()), loader, false, writer),
						"ASM verification failed for " + classInfo.getName());
				writer.flush();
				assertTrue(verificationErrors.toString().isBlank(), "ASM verification reported errors for " + classInfo.getName() + ":\n" + verificationErrors);
			}
		}
	}

	private static final class TestClassLoader extends URLClassLoader {
		private TestClassLoader(@Nonnull Path directory) throws IOException {
			super(new URL[]{directory.toUri().toURL()}, TestClassLoader.class.getClassLoader());
		}
	}

	private static String runJava(Path classpath, String className, String input) throws Exception {
		String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
		Path java = Paths.get(System.getProperty("java.home"), "bin", executable);
		Path outputFile = Files.createTempFile("recaf-zkm-process", ".out");
		Process process = null;
		try {
			// Redirect output to a file so a verbose sample cannot block while the test waits for completion.
			process = new ProcessBuilder(java.toString(), "-cp", classpath.toString(), className)
					.redirectErrorStream(true)
					.redirectOutput(outputFile.toFile())
					.start();
			try (var output = process.getOutputStream()) {
				output.write(input.getBytes(StandardCharsets.UTF_8));
			}
			if (!process.waitFor(30, TimeUnit.SECONDS)) {
				process.destroyForcibly();
				process.waitFor(5, TimeUnit.SECONDS);
				throw new AssertionError("Timed out running " + className);
			}
			String output = Files.readString(outputFile, StandardCharsets.UTF_8);
			assertEquals(0, process.exitValue(), "Process failed for " + className + ":\n" + output);
			return output.replace("\r\n", "\n").replace('\r', '\n');
		} finally {
			if (process != null && process.isAlive())
				process.destroyForcibly();
			Files.deleteIfExists(outputFile);
		}
	}
}
