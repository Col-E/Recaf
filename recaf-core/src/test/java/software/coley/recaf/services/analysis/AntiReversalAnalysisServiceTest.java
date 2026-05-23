package software.coley.recaf.services.analysis;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.analysis.antitamper.AntiReversalAnalysisService;
import software.coley.recaf.services.analysis.antitamper.IllegalNameAnalysis;
import software.coley.recaf.services.analysis.antitamper.IllegalNameAntiReversalAnalyzer;
import software.coley.recaf.services.analysis.antitamper.TransformerImpactAnalysis;
import software.coley.recaf.services.analysis.antitamper.TransformerImpactAntiReversalAnalyzer;
import software.coley.recaf.services.transform.JvmTransformResult;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static software.coley.recaf.test.TestClassUtils.*;

/**
 * Tests for {@link AntiReversalAnalysisService}.
 */
class AntiReversalAnalysisServiceTest extends TestBase {
	private static AntiReversalAnalysisService service;

	@BeforeAll
	static void setupService() {
		service = recaf.get(AntiReversalAnalysisService.class);
	}

	@Test
	void findsIllegalNames() {
		// Class names that are not valid should be reported.
		JvmClassBundle bundle = fromClasses(
				createEmptyClass("bad name/Test"),
				createEmptyClass("void/Test"),
				createEmptyClass("int"),
				createEmptyClass("\0Test"),
				createEmptyClass("Test\n"),
				createEmptyClass("com//Test")
		);
		Workspace workspace = fromBundle(bundle);
		IllegalNameAnalysis analysis = service.analyze(workspace, workspace.getPrimaryResource(), IllegalNameAntiReversalAnalyzer.class);
		List<ClassPathNode> classPathNodes = analysis.classesWithIllegalNames();
		assertEquals(bundle.size(), classPathNodes.size());
	}

	@Test
	void reportsJvmTransformerImpact() {
		// class Loop extends Loop {}
		ClassWriter cw = new ClassWriter(0);
		cw.visit(Opcodes.V1_8, 0, "Loop", null, "Loop", null);
		cw.visitEnd();
		byte[] cycleBytes = cw.toByteArray();

		// Class with invalid signature on a field.
		cw = new ClassWriter(0);
		cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "Signature", null, "java/lang/Object", null);
		cw.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, "foo", "Ljava/util/List;", "Ljava/util/List<I>;", null).visitEnd();
		cw.visitEnd();
		byte[] signatureBytes = cw.toByteArray();

		// Craft a workspace with:
		//  - Class with hierarchy cycle
		//  - Class with invalid signature
		JvmClassInfo cycle = new JvmClassInfoBuilder(cycleBytes).build();
		JvmClassInfo invalidSignature = new JvmClassInfoBuilder(signatureBytes).build();
		Workspace workspace = fromBundle(fromClasses(cycle, invalidSignature));

		// Result should show:
		// - Loop class to be removed due to the cycle.
		// - Broken class to be transformed due to the invalid signature.
		TransformerImpactAnalysis analysis = service.analyze(workspace, workspace.getPrimaryResource(), TransformerImpactAntiReversalAnalyzer.class);
		JvmTransformResult result = analysis.jvm().result();
		assertEquals(List.of("Signature"), result.getTransformedClasses().keySet().stream().map(p -> p.getValue().getName()).sorted().toList());
		assertEquals(List.of("Loop"), result.getClassesToRemove().stream().map(p -> p.getValue().getName()).sorted().toList());
	}
}
