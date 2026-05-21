package software.coley.recaf.services.decompile;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.decompile.cfr.CfrConfig;
import software.coley.recaf.services.decompile.cfr.CfrDecompiler;
import software.coley.recaf.services.decompile.procyon.ProcyonConfig;
import software.coley.recaf.services.decompile.procyon.ProcyonDecompiler;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.workspace.model.Workspace;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for obfuscation concerns (ZWSP, RTL, whitespace tricks) vs readable Cyrillic.
 */
@Execution(ExecutionMode.SAME_THREAD)
class DecompilerUnicodeDisplayTest extends TestBase {
	private static final String CYRILLIC_MESSAGE = "\u0414\u043b\u044f \u0440\u0430\u0431\u043e\u0442\u044b";

	private static DecompilerManager decompilerManager;
	private static DecompilerManagerConfig managerConfig;
	private static Workspace workspace;
	private static JvmClassInfo testClass;

	@BeforeAll
	static void setup() throws IOException {
		decompilerManager = recaf.get(DecompilerManager.class);
		managerConfig = unwrapProxy(decompilerManager).getServiceConfig();
		testClass = TestClassUtils.createClass("UnicodeTestClass", DecompilerUnicodeDisplayTest::appendTestMethods);
		workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(testClass));
	}

	@BeforeEach
	void setupEach() {
		managerConfig.getCacheDecompilations().setValue(false);
		workspaceManager.setCurrent(workspace);
	}

	@AfterEach
	void resetConfigs() {
		managerConfig.getCacheDecompilations().setValue(true);
		setCfrHideUtf(CfrConfig.BooleanOption.DEFAULT);
		setProcyonUnicodeOutput(false);
	}

	@Test
	void cfr_showsReadableCyrillic_andPreservesObfuscationEscapes() throws Exception {
		String output = decompileWithManager(CfrDecompiler.NAME);
		assertReadableCyrillic(output);
		assertObfuscationEscapesPreserved(output);
	}

	@Test
	void procyon_showsReadableCyrillic_andPreservesObfuscationEscapes() throws Exception {
		String output = decompileWithManager(ProcyonDecompiler.NAME);
		assertReadableCyrillic(output);
		assertObfuscationEscapesPreserved(output);
	}

	@Test
	void cfr_hideUtfTrue_skipsUnicodeFilter() throws Exception {
		setCfrHideUtf(CfrConfig.BooleanOption.TRUE);
		assertManagerOutputMatchesRaw(CfrDecompiler.NAME);
	}

	@Test
	void procyon_unicodeOutputEnabled_skipsUnicodeFilter() throws Exception {
		setProcyonUnicodeOutput(true);
		assertManagerOutputMatchesRaw(ProcyonDecompiler.NAME);
	}

	private static void setCfrHideUtf(@Nonnull CfrConfig.BooleanOption value) {
		((CfrConfig) decompilerManager.getJvmDecompiler(CfrDecompiler.NAME).getConfig()).getHideutf().setValue(value);
	}

	private static void setProcyonUnicodeOutput(boolean enabled) {
		((ProcyonConfig) decompilerManager.getJvmDecompiler(ProcyonDecompiler.NAME).getConfig())
				.getIsUnicodeOutputEnabled().setValue(enabled);
	}

	@Nonnull
	private String decompileWithManager(@Nonnull String decompilerName) throws Exception {
		JvmDecompiler decompiler = decompilerManager.getJvmDecompiler(decompilerName);
		assertNotNull(decompiler);
		DecompileResult result = decompilerManager.decompile(decompiler, workspace, testClass)
				.get(60, TimeUnit.SECONDS);
		String text = result.getText();
		assertNotNull(text, () -> "Missing decompilation from " + decompilerName);
		return text;
	}

	private void assertManagerOutputMatchesRaw(@Nonnull String decompilerName) throws Exception {
		JvmDecompiler decompiler = decompilerManager.getJvmDecompiler(decompilerName);
		assertNotNull(decompiler);
		String raw = decompiler.decompile(workspace, testClass).getText();
		String managed = decompileWithManager(decompilerName);
		assertNotNull(raw);
		assertEquals(normalizeLineEndings(raw), normalizeLineEndings(managed),
				() -> "Manager output should match raw decompiler when unicode filter is disabled");
	}

	private static void appendTestMethods(@Nonnull ClassNode node) {
		MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "test", "()V", null, null);
		appendPrintln(method, CYRILLIC_MESSAGE);
		appendPrintln(method, "\u200b\u200b\u200b");
		appendPrintln(method, "before\u202eRTL after");
		appendPrintln(method, "\u2004\u2005\uFEFF");
		method.instructions.add(new InsnNode(Opcodes.RETURN));
		node.methods.add(method);
	}

	private static void appendPrintln(@Nonnull MethodNode method, @Nonnull String value) {
		method.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
		method.instructions.add(new LdcInsnNode(value));
		method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
				"(Ljava/lang/String;)V", false));
	}

	private static void assertReadableCyrillic(@Nonnull String text) {
		assertTrue(text.contains(CYRILLIC_MESSAGE) || text.contains("\\u0414"),
				() -> "Expected readable Cyrillic. Got:\n" + text);
	}

	private static void assertObfuscationEscapesPreserved(@Nonnull String text) {
		assertFalse(text.contains("\u200b"),
				() -> "ZWSP must not appear as a raw invisible character. Got:\n" + text);
		assertFalse(text.contains("\u202e"),
				() -> "Bidi override must not appear as a raw control character. Got:\n" + text);
		assertTrue(text.contains("\\u200b") || text.contains("\\u200B"),
				() -> "ZWSP should remain visible as an escape. Got:\n" + text);
		assertTrue(text.contains("\\u202e") || text.contains("\\u202E"),
				() -> "RTL override should remain visible as an escape. Got:\n" + text);
	}

	@Nonnull
	private static String normalizeLineEndings(@Nonnull String text) {
		return text.replace("\r\n", "\n");
	}
}
