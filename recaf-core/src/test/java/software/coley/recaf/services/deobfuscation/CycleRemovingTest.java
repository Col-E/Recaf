package software.coley.recaf.services.deobfuscation;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.services.deobfuscation.transform.generic.CycleClassRemovingTransformer;
import software.coley.recaf.services.transform.JvmTransformResult;
import software.coley.recaf.services.transform.TransformationApplier;
import software.coley.recaf.services.transform.TransformationApplierService;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.workspace.model.BasicWorkspace;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResourceBuilder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CycleRemovingTest extends TestBase {
	private static TransformationApplierService transformationApplierService;
	private static TransformationApplier transformationApplier;
	private static Workspace workspace;

	@BeforeAll
	static void setupServices() {
		transformationApplierService = recaf.get(TransformationApplierService.class);
	}

	@BeforeEach
	void setupWorkspace() {
		workspace = new BasicWorkspace(new WorkspaceResourceBuilder().build());
		workspaceManager.setCurrentIgnoringConditions(workspace);
		transformationApplier = transformationApplierService.newApplierForCurrentWorkspace();
	}

	@Test
	void testCycleViaExtends() {
		ClassWriter cw = new ClassWriter(0);
		cw.visit(Opcodes.V1_8, 0, "Loop", null, "Loop", null);
		cw.visitEnd();
		byte[] bytes = cw.toByteArray();

		assertCycleRemoved(bytes);
	}

	@Test
	void testCycleViaImplements() {
		ClassWriter cw = new ClassWriter(0);
		cw.visit(Opcodes.V1_8, Opcodes.ACC_INTERFACE, "Loop", null, "java/lang/Object", new String[]{"Loop"});
		cw.visitEnd();
		byte[] bytes = cw.toByteArray();

		assertCycleRemoved(bytes);
	}

	private static void assertCycleRemoved(byte[] bytes) {
		// Initial workspace state
		JvmClassBundle bundle = workspace.getPrimaryResource().getJvmClassBundle();
		bundle.put(new JvmClassInfoBuilder(bytes).build());
		assertEquals(1, bundle.size());

		// Use cycle removing transformer and observe the class being removed from the workspace
		JvmTransformResult result = assertDoesNotThrow(() -> transformationApplier.transformJvm(List.of(CycleClassRemovingTransformer.class)));
		assertEquals(1, result.getClassesToRemove().size());
		assertEquals(1, bundle.size());
		result.apply();
		assertEquals(0, bundle.size());
	}
}
