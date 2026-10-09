package software.coley.recaf.test;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.backend.jvm.compile.JavaClassRepresentation;
import me.darknet.assembler.backend.jvm.compile.JavaCompileResult;
import me.darknet.assembler.error.Diagnostic;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.parser.Token;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.objectweb.asm.tree.ClassNode;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.StubClassInfo;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.services.assembler.AssemblerPipelineManager;
import software.coley.recaf.services.assembler.JvmAssemblerPipeline;
import software.coley.recaf.services.compile.CompilerDiagnostic;
import software.coley.recaf.services.compile.CompilerResult;
import software.coley.recaf.services.compile.JavacArguments;
import software.coley.recaf.services.compile.JavacArgumentsBuilder;
import software.coley.recaf.services.compile.JavacCompiler;
import software.coley.recaf.services.decompile.DecompileResult;
import software.coley.recaf.services.decompile.JvmDecompiler;
import software.coley.recaf.services.decompile.cfr.CfrConfig;
import software.coley.recaf.services.decompile.cfr.CfrDecompiler;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.workspace.model.BasicWorkspace;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResourceBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Common base for tests that need to compile, assemble, or decompile JVM bytecode.
 */
public class CompilerTestBase extends TestBase {
	protected static final String CLASS_NAME = "Example";

	private static JvmAssemblerPipeline assembler;
	private static JavacCompiler javac;
	private static JvmDecompiler decompiler;
	private Workspace workspace;

	@BeforeAll
	static void setupServices() {
		javac = recaf.get(JavacCompiler.class);
		decompiler = new CfrDecompiler(recaf.get(WorkspaceManager.class), new CfrConfig());
	}

	@BeforeEach
	void setupWorkspace() {
		workspace = new BasicWorkspace(new WorkspaceResourceBuilder().build());
		workspaceManager.setCurrentIgnoringConditions(workspace);
		assembler = recaf.get(AssemblerPipelineManager.class).newJvmAssemblerPipeline(workspace);
	}

	protected void putClass(@Nullable String name, @Nullable Consumer<ClassNode> consumer) {
		workspace.getPrimaryResource().getJvmClassBundle()
				.put(TestClassUtils.createClass(name, consumer));
	}

	protected void clearClasses() {
		workspace.getPrimaryResource().getJvmClassBundle().clear();
	}

	@Nonnull
	protected ClassPathNode classPath(@Nonnull String name) {
		JvmClassBundle bundle = workspace.getPrimaryResource().getJvmClassBundle();
		return PathNodes.classPath(workspace, workspace.getPrimaryResource(), bundle, get(name));
	}

	@Nonnull
	protected ClassMemberPathNode memberPath(@Nonnull String owner, @Nonnull String name, @Nonnull String desc) {
		ClassPathNode classPath = classPath(owner);
		ClassMemberPathNode memberPath = classPath.child(name, desc);
		if (memberPath == null)
			fail("No member in class: " + owner + "." + name + desc);
		return memberPath;
	}

	@Nonnull
	protected JvmClassInfo get(@Nonnull String className) {
		JvmClassBundle bundle = workspace.getPrimaryResource().getJvmClassBundle();
		JvmClassInfo cls = bundle.get(className);
		if (cls == null)
			fail("No class: " + className);
		return cls;
	}

	@Nonnull
	protected String decompile(@Nonnull JvmClassInfo cls) {
		DecompileResult result = decompiler.decompile(workspace, cls);
		if (result.getText() == null)
			fail("Missing decompilation result");
		return result.getText();
	}

	@Nonnull
	protected String disassemble(boolean isFullBody) {
		JvmClassBundle bundle = workspace.getPrimaryResource().getJvmClassBundle();
		JvmClassInfo cls = bundle.get(CLASS_NAME);
		return disassemble(cls, isFullBody);
	}

	@Nonnull
	protected String disassemble(@Nonnull JvmClassInfo cls, boolean isFullBody) {
		JvmClassBundle bundle = workspace.getPrimaryResource().getJvmClassBundle();
		WorkspaceResource resource = workspace.getPrimaryResource();
		Outcome<String> disassembly;
		if (isFullBody) {
			ClassPathNode path = PathNodes.classPath(workspace, resource, bundle, cls);
			disassembly = assembler.disassemble(path);
		} else {
			MethodMember method = cls.getFirstDeclaredMethodByName("example");
			if (method == null)
				fail("Failed to find 'example' method, cannot disassemble");
			ClassMemberPathNode path = PathNodes.memberPath(workspace, resource, bundle, cls, method);
			disassembly = assembler.disassemble(path);
		}

		return requireSuccess(disassembly, "", "\n");
	}

	@Nonnull
	protected String compile(@Nonnull String src, Class<?>... importedType) {
		JavacArguments args = new JavacArgumentsBuilder()
				.withClassName(CLASS_NAME)
				.withClassSource("%IMPORTS%\nclass %NAME% {\n%SRC%\n}"
						.replace("%IMPORTS%", Arrays.stream(importedType).map(c -> "import " + c.getName() + ";").collect(Collectors.joining("\n")))
						.replace("%NAME%", CLASS_NAME)
						.replace("%SRC%", src))
				.build();
		CompilerResult result = javac.compile(args, workspace, null);
		WorkspaceResource resource = workspace.getPrimaryResource();
		JvmClassBundle bundle = resource.getJvmClassBundle();
		if (result.wasSuccess())
			result.getCompilations().forEach((name, code) -> bundle.put(name, new JvmClassInfoBuilder(code).build()));
		else
			fail("Failed to compile test input");
		JvmClassInfo cls = bundle.get(CLASS_NAME);
		return disassemble(cls, true);
	}

	@Nonnull
	protected String compileFull(@Nonnull String type, @Nonnull String src) {
		return compileFull(type, src, true);
	}

	@Nonnull
	protected String compileFull(@Nonnull String type, @Nonnull String src, boolean debugInfo) {
		JavacArguments args = new JavacArgumentsBuilder()
				.withClassName(type)
				.withClassSource(src)
				.withDebugVariables(debugInfo)
				.withDebugLineNumbers(debugInfo)
				.withDebugSourceName(debugInfo)
				.build();
		CompilerResult result = javac.compile(args, workspace, null);
		WorkspaceResource resource = workspace.getPrimaryResource();
		JvmClassBundle bundle = resource.getJvmClassBundle();
		if (result.wasSuccess())
			result.getCompilations().forEach((name, code) -> bundle.put(name, new JvmClassInfoBuilder(code).build()));
		else
			fail("Failed to compile test input '" + type + "': " +
					result.getDiagnostics().stream()
							.map(CompilerDiagnostic::toString)
							.collect(Collectors.joining("\n")));
		JvmClassInfo cls = bundle.get(type);
		return disassemble(cls, true);
	}

	@Nonnull
	protected JvmClassInfo assemble(@Nonnull String body, boolean isFullBody) {
		String assembly = fillAssemblyTemplate(body, isFullBody);
		WorkspaceResource resource = workspace.getPrimaryResource();
		JvmClassBundle bundle = resource.getJvmClassBundle();
		ClassPathNode path = PathNodes.classPath(workspace, resource, bundle, new StubClassInfo(CLASS_NAME).asJvmClass());
		JavaCompileResult result = requireSuccess(assembler.assembleSource(assembly, "<assembly>", path), "Errors assembling test input:\n - ", "\n - ");
		JavaClassRepresentation representation = result.representation();
		if (representation == null)
			fail("No assembler output for test case");
		JvmClassInfo cls = new JvmClassInfoBuilder(representation.classFile()).build();
		bundle.put(cls);
		return cls;
	}

	@Nonnull
	protected List<ASTElement> assembleAst(@Nonnull String body, boolean isFullBody) {
		String assembly = fillAssemblyTemplate(body, isFullBody);
		List<Token> tokens = requireSuccess(assembler.tokenize(assembly, "<assembly>"), "Errors tokenizing test input:\n - ", "\n - ");
		return requireSuccess(assembler.fullParse(tokens), "Errors parsing test input:\n - ", "\n - ");
	}

	@Nonnull
	private static <T> T requireSuccess(@Nonnull Outcome<T> outcome, @Nonnull String errorPrefix,
	                                    @Nonnull String diagnosticSeparator) {
		outcome.onPartial(partial -> fail(errorPrefix + formatDiagnostics(partial.diagnostics(), diagnosticSeparator)))
				.onFailure(failure -> fail(errorPrefix + formatDiagnostics(failure.diagnostics(), diagnosticSeparator)));
		return outcome.requireValue();
	}

	@Nonnull
	private static String formatDiagnostics(@Nonnull List<Diagnostic> diagnostics, @Nonnull String separator) {
		return diagnostics.stream().map(Diagnostic::toString).collect(Collectors.joining(separator));
	}

	@Nonnull
	private static String fillAssemblyTemplate(@Nonnull String body, boolean isFullBody) {
		return isFullBody ? body : """
				.super java/lang/Object
				.class public super %NAME% {
				%CODE%
				}
				""".replace("%NAME%", CLASS_NAME).replace("%CODE%", body);
	}
}
