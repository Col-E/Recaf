package software.coley.recaf.services.decompile;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for maintainer obfuscation concerns (ZWSP, bidi, whitespace tricks vs readable Cyrillic).
 */
class DecompilerUnicodeDisplayTest extends TestBase {
	private static DecompilerManager decompilerManager;
	private static DecompilerManagerConfig decompilerManagerConfig;
	private static Workspace workspace;
	private static JvmClassInfo obfuscationSample;

	@BeforeAll
	static void setup() throws IOException {
		decompilerManager = recaf.get(DecompilerManager.class);
		obfuscationSample = TestClassUtils.createClass("ObfUnicodeSample", DecompilerUnicodeDisplayTest::appendPrintMethods);
		workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(obfuscationSample));
	}

	@BeforeEach
	void setupEach() {
		decompilerManagerConfig = new DecompilerManagerConfig();
		decompilerManagerConfig.getCacheDecompilations().setValue(false);
		assertDoesNotThrow(() -> software.coley.recaf.util.ReflectUtil.quietSet(
				unwrapProxy(decompilerManager),
				DecompilerManager.class.getDeclaredField("config"),
				decompilerManagerConfig));
		workspaceManager.setCurrent(workspace);
	}

	@Test
	void cfr_readableCyrillic_notZwspTricks() throws Exception {
		String text = decompile(CfrDecompiler.NAME);
		assertReadableCyrillic(text);
		assertObfuscationEscapesPreserved(text);
	}

	@Test
	void procyon_readableCyrillic_notZwspTricks() throws Exception {
		String text = decompile(ProcyonDecompiler.NAME);
		assertReadableCyrillic(text);
		assertObfuscationEscapesPreserved(text);
	}

	@Test
	void cfr_hideUtfTrue_skipsFilter_outputUnchangedForTricks() throws Exception {
		JvmDecompiler cfr = decompilerManager.getJvmDecompiler(CfrDecompiler.NAME);
		CfrConfig config = (CfrConfig) cfr.getConfig();
		config.getHideutf().setValue(CfrConfig.BooleanOption.TRUE);

		String withoutFilter = cfr.decompile(workspace, obfuscationSample).getText();
		String viaManager = decompilerManager.decompile(cfr, workspace, obfuscationSample)
				.get(60, TimeUnit.SECONDS).getText();

		assertObfuscationEscapesPreserved(viaManager);
		assertEquals(normalizeLineEndings(withoutFilter), normalizeLineEndings(viaManager),
				() -> "hideutf=true should skip unicode filter; manager output must match raw decompiler output");
	}

	@Test
	void procyon_unicodeOutputEnabled_skipsFilter_outputUnchanged() throws Exception {
		JvmDecompiler procyon = decompilerManager.getJvmDecompiler(ProcyonDecompiler.NAME);
		ProcyonConfig config = (ProcyonConfig) procyon.getConfig();
		config.getIsUnicodeOutputEnabled().setValue(true);

		String withoutFilter = procyon.decompile(workspace, obfuscationSample).getText();
		String viaManager = decompilerManager.decompile(procyon, workspace, obfuscationSample)
				.get(60, TimeUnit.SECONDS).getText();

		assertEquals(normalizeLineEndings(withoutFilter), normalizeLineEndings(viaManager),
				() -> "unicode output enabled should skip unicode filter");
	}

	@Nonnull
	private String decompile(@Nonnull String decompilerName) throws Exception {
		JvmDecompiler decompiler = decompilerManager.getJvmDecompiler(decompilerName);
		DecompileResult result = decompilerManager.decompile(decompiler, workspace, obfuscationSample)
				.get(60, TimeUnit.SECONDS);
		String text = result.getText();
		if (text == null)
			throw new AssertionError("Missing decompilation from " + decompilerName);
		return text;
	}

	private static void appendPrintMethods(@Nonnull ClassNode node) {
		MethodNode run = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "run", "()V", null, null);
		appendPrintln(run, "\u0414\u043b\u044f \u0440\u0430\u0431\u043e\u0442\u044b");
		appendPrintln(run, "\u200b\u200b\u200b");
		appendPrintln(run, "before\u202eRTL");
		run.instructions.add(new InsnNode(Opcodes.RETURN));
		node.methods.add(run);
	}

	private static void appendPrintln(@Nonnull MethodNode method, @Nonnull String value) {
		method.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
		method.instructions.add(new LdcInsnNode(value));
		method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
				"(Ljava/lang/String;)V", false));
	}

	private static void assertReadableCyrillic(@Nonnull String text) {
		boolean hasReadable = text.contains("\u0414\u043b\u044f") || text.contains("\u0434\u043b\u044f")
				|| text.contains("\u0414") && text.contains("\u044f");
		boolean onlyEscapes = text.contains("\\u0414") && !text.contains("\u0414\u043b");
		assertTrue(hasReadable || !onlyEscapes,
				"Expected readable Cyrillic in decompiler output, got:\n" + text);
	}

	private static void assertObfuscationEscapesPreserved(@Nonnull String text) {
		boolean hasZwspEscape = text.contains("\\u200b") || text.contains("\\u200B");
		boolean hasBidiEscape = text.contains("\\u202e") || text.contains("\\u202E");
		boolean hasRawZwsp = text.contains("\u200b");
		boolean hasRawBidi = text.contains("\u202e");
		assertTrue(hasZwspEscape || !hasRawZwsp,
				"ZWSP should remain escaped, not raw invisible char. Output:\n" + text);
		assertTrue(hasBidiEscape || !hasRawBidi,
				"Bidi override should remain escaped. Output:\n" + text);
	}

	@Nonnull
	private static String normalizeLineEndings(@Nonnull String text) {
		return text.replace("\r\n", "\n");
	}
}
