package software.coley.recaf.services.analysis;

import me.darknet.dex.tree.definitions.instructions.Invoke;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.analysis.structure.AreaAnalysisResult;
import software.coley.recaf.services.analysis.structure.AreaAnalysisService;
import software.coley.recaf.services.analysis.structure.AreaFormationKind;
import software.coley.recaf.services.analysis.structure.AreaGroup;
import software.coley.recaf.test.CompilerTestBase;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.TestDexUtils;
import software.coley.recaf.workspace.model.BasicWorkspace;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResourceBuilder;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static software.coley.recaf.test.TestClassUtils.fromClasses;

/**
 * Tests for {@link AreaAnalysisService}.
 */
class AreaAnalysisServiceTest extends CompilerTestBase {
	private static AreaAnalysisService service;

	@BeforeAll
	static void setupService() {
		service = recaf.get(AreaAnalysisService.class);
	}

	@Test
	void groupsJvmAreasAndMergesSmallHelpers() {
		compileFull("a/A", """
				package a;

				import java.security.MessageDigest;
				
				public class A {
					private MessageDigest digest;

					public static void run() {
						B.run();
					}
				}
				class B {
					static void run() {
						A.run();
					}
				}
				""");
		compileFull("m/Main", """
				package m;

				import java.net.URI;
				
				public class Main {
					public static void run() {
						URI.create("https://recaf.coley.software");
						Helper.run();
					}
				}
				
				class Helper {
					static void run() {}
				}
				""");
		compileFull("s/Standalone", """
				package s;
				public class Standalone {}
				""");
		Workspace workspace = workspaceManager.getCurrent();

		AreaAnalysisResult result = service.analyze(workspace, workspace.getPrimaryResource());

		assertEquals(3, result.groupCount());
		assertFalse(result.spaghettiDetected());

		AreaGroup cycleGroup = groupContaining(result, "a/A");
		assertEquals(Set.of("a/A", "a/B"), names(cycleGroup.classes()));
		assertEquals(AreaFormationKind.SCC, cycleGroup.formationKind());
		assertEquals("SECURITY", cycleGroup.purpose());

		AreaGroup mergedGroup = groupContaining(result, "m/Main");
		assertEquals(Set.of("m/Helper", "m/Main"), names(mergedGroup.classes()));
		assertEquals(AreaFormationKind.MERGED, mergedGroup.formationKind());
		assertEquals("NETWORKING", mergedGroup.purpose());

		AreaGroup standaloneGroup = groupContaining(result, "s/Standalone");
		assertEquals(Set.of("s/Standalone"), names(standaloneGroup.classes()));
		assertEquals("MISC", standaloneGroup.purpose());
		assertTrue(cycleGroup.confidence() > mergedGroup.confidence());

		assertEquals(List.of(1, 2, 3), result.groups().stream().map(AreaGroup::id).toList());
	}

	@Test
	void groupsClassesUsingMetadataEdgesWithoutCreatingPhantomGroups() {
		compileFull("meta/A", """
				package meta;
				
				import java.util.List;
				
				@Tag
				public class A {
					private List<B> items;
					public List<B> use(B input) {
						return items;
					}
				}
				
				@interface Tag {}
				
				class B {
					private A owner;
				}
				""");
		Workspace workspace = workspaceManager.getCurrent();

		AreaAnalysisResult result = service.analyze(workspace, workspace.getPrimaryResource());

		AreaGroup ownerGroup = groupContaining(result, "meta/A");
		assertEquals(Set.of("meta/A", "meta/B"), names(ownerGroup.classes()));

		AreaGroup tagGroup = groupContaining(result, "meta/Tag");
		assertEquals(Set.of("meta/Tag"), names(tagGroup.classes()));
		assertEquals(1, tagGroup.inboundLinkCount());

		assertEquals(2, result.groupCount());
		assertEquals(1, result.linkCount());
		assertTrue(result.links().getFirst().weight() > 0);
	}

	@Test
	void estimatesAreaPurposeFromRecognizableApiReferences() {
		compileFull("purpose/NetA", """
				package purpose;

				import java.net.URI;

				public class NetA {
					public static void run() {
						URI.create("https://example.com");
						NetB.run();
					}
				}

				class NetB {
					static void run() {
						URI.create("https://recaf.coley.software");
						NetA.run();
					}
				}
				""");
		compileFull("purpose/Sec", """
				package purpose;

				import java.security.SecureRandom;

				public class Sec {
					private final SecureRandom random = new SecureRandom();
				}
				""");
		Workspace workspace = workspaceManager.getCurrent();

		AreaAnalysisResult result = service.analyze(workspace, workspace.getPrimaryResource());

		assertEquals("NETWORKING", groupContaining(result, "purpose/NetA").purpose());
		assertEquals("SECURITY", groupContaining(result, "purpose/Sec").purpose());
	}

	@Test
	void ignoresOutOfScopeSupportClassesWhenAnalyzingPrimaryResource() {
		compileFull("android/Caller", """
				package android;
				public class Caller {
					public static void run() {}
				}
				""");
		compileFull("support/Leak", """
				package support;
				public class Leak {
					public static void run() {
						android.Caller.run();
					}
				}
				""");
		JvmClassInfo leak = get("support/Leak");
		WorkspaceResource primary = new WorkspaceResourceBuilder()
				.withAndroidClassBundles(Map.of("classes.dex", fromClasses(
						newAndroidCaller("android/Caller", "android/Target"),
						newAndroidCaller("android/Target", "android/Caller")
				)))
				.build();
		WorkspaceResource supporting = new WorkspaceResourceBuilder()
				.withJvmClassBundle(fromClasses(leak))
				.build();
		Workspace workspace = new BasicWorkspace(primary, List.of(supporting));

		AreaAnalysisResult result = service.analyze(workspace, primary);

		assertEquals(1, result.groupCount());
		assertEquals(Set.of("android/Caller", "android/Target"), names(result.groups().getFirst().classes()));
		assertTrue(result.groups().stream()
				.flatMap(group -> group.classes().stream())
				.noneMatch(path -> path.getValue().getName().startsWith("support/")));
	}

	@Test
	void analyzesAndroidOnlyAreas() {
		Workspace workspace = TestClassUtils.fromBundle(fromClasses(
				newAndroidCaller("dex/A", "dex/B"),
				newAndroidCaller("dex/B", "dex/A")
		));

		AreaAnalysisResult result = service.analyze(workspace, workspace.getPrimaryResource());

		assertEquals(1, result.groupCount());
		assertEquals(Set.of("dex/A", "dex/B"), names(result.groups().getFirst().classes()));
		assertEquals(AreaFormationKind.SCC, result.groups().getFirst().formationKind());
	}

	@Test
	void flagsDominantSpaghettiComponent() {
		compileFull("ring/A", """
				package ring;
				public class A {
					public static void run() {
						B.run();
					}
				}
				class B {
					static void run() {
						C.run();
					}
				}
				class C {
					static void run() {
						D.run();
					}
				}
				class D {
					static void run() {
						A.run();
					}
				}
				""");
		Workspace workspace = workspaceManager.getCurrent();

		AreaAnalysisResult result = service.analyze(workspace, workspace.getPrimaryResource());

		assertTrue(result.spaghettiDetected());
		assertEquals(1, result.groupCount());
		assertTrue(result.groups().getFirst().confidence() < 0.80);
	}

	@Test
	void mergesHelperChainsAcrossMultiplePasses() {
		compileFull("chain/Main", """
				package chain;
				public class Main {
					public static void run() {
						HelperA.run();
					}
				}
				class HelperA {
					static void run() {
						HelperB.run();
					}
				}
				class HelperB {
					static void run() {
						HelperC.run();
					}
				}
				class HelperC {
					static void run() {}
				}
				""");
		Workspace workspace = workspaceManager.getCurrent();

		AreaAnalysisResult result = service.analyze(workspace, workspace.getPrimaryResource());

		assertEquals(1, result.groupCount());
		AreaGroup mergedGroup = groupContaining(result, "chain/Main");
		assertEquals(Set.of("chain/Main", "chain/HelperA", "chain/HelperB", "chain/HelperC"), names(mergedGroup.classes()));
		assertEquals(AreaFormationKind.MERGED, mergedGroup.formationKind());
	}

	@Test
	void updatesDominanceAfterIntermediateMerge() {
		compileFull("dominance/Main", """
				package dominance;
				public class Main {
					public static void run() {
						Bridge.run();
						Helper.run();
					}
				}
				class Bridge {
					static void run() {
						Helper.run();
					}
				}
				class Helper {
					static void run() {}
				}
				""");
		Workspace workspace = workspaceManager.getCurrent();

		AreaAnalysisResult result = service.analyze(workspace, workspace.getPrimaryResource());

		assertEquals(1, result.groupCount());
		assertEquals(Set.of("dominance/Main", "dominance/Bridge", "dominance/Helper"),
				names(result.groups().getFirst().classes()));
		assertEquals(AreaFormationKind.MERGED, result.groups().getFirst().formationKind());
	}

	@Test
	void analyzeResultsAreRepeatableAcrossRuns() {
		compileFull("repeat/A", """
				package repeat;
				public class A {
					public static void run() {
						B.run();
						C.run();
					}
				}
				class B {
					static void run() {
						C.run();
					}
				}
				class C {
					static void run() {}
				}
				class CycleOne {
					static void run() {
						CycleTwo.run();
					}
				}
				class CycleTwo {
					static void run() {
						CycleOne.run();
					}
				}
				""");
		Workspace workspace = workspaceManager.getCurrent();

		String expectedSignature = analysisSignature(service.analyze(workspace, workspace.getPrimaryResource()));
		for (int i = 0; i < 5; i++) {
			AreaAnalysisResult rerun = service.analyze(workspace, workspace.getPrimaryResource());
			assertEquals(expectedSignature, analysisSignature(rerun));
		}
	}

	private static AreaGroup groupContaining(AreaAnalysisResult result, String className) {
		return result.groups().stream()
				.filter(group -> names(group.classes()).contains(className))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Missing group containing " + className));
	}

	private static String analysisSignature(AreaAnalysisResult result) {
		String groups = result.groups().stream()
				.map(group -> group.id() + ":" +
						group.formationKind() + ":" +
						group.purpose() + ":" +
						group.inboundLinkCount() + ":" +
						group.outboundLinkCount() + ":" +
						group.containsEntryPoint() + ":" +
						group.classes().stream()
								.map(path -> path.getValue().getName())
								.sorted()
								.toList())
				.collect(Collectors.joining("|"));
		String links = result.links().stream()
				.map(link -> link.sourceGroupId() + ">" + link.targetGroupId() + ":" + link.weight() + ":" + link.edgeCount())
				.collect(Collectors.joining("|"));
		return result.spaghettiDetected() + "#" + result.groupCount() + "#" + result.linkCount() + "#" + groups + "#" + links;
	}

	private static Set<String> names(Collection<ClassPathNode> classes) {
		return classes.stream().map(path -> path.getValue().getName()).collect(Collectors.toSet());
	}

	private static AndroidClassInfo newAndroidCaller(String callerType, String calleeType) {
		return TestDexUtils.newAndroidClass(callerType, TestDexUtils.newDexMethod("run", "()V", org.objectweb.asm.Opcodes.ACC_PUBLIC | org.objectweb.asm.Opcodes.ACC_STATIC, code -> code
				.arguments(0, 0)
				.registers(0)
				.invoke(Invoke.STATIC, me.darknet.dex.tree.type.Types.instanceTypeFromInternalName(calleeType), "run",
						me.darknet.dex.tree.type.Types.methodTypeFromDescriptor("()V"))
				.return_void()
		));
	}
}
