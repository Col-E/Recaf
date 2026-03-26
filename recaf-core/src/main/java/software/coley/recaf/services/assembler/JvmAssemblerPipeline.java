package software.coley.recaf.services.assembler;

import jakarta.annotation.Nonnull;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.compile.JavaClassRepresentation;
import me.darknet.assembler.compile.JvmCompiler;
import me.darknet.assembler.compile.JvmCompilerOptions;
import me.darknet.assembler.compile.analysis.BasicFieldValueLookup;
import me.darknet.assembler.compile.analysis.BasicMethodValueLookup;
import me.darknet.assembler.compile.analysis.jvm.ValuedJvmAnalysisEngine;
import me.darknet.assembler.compile.visitor.JavaCompileResult;
import me.darknet.assembler.compiler.Compiler;
import me.darknet.assembler.compiler.CompilerOptions;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.parser.BytecodeFormat;
import me.darknet.assembler.parser.processor.ASTProcessor;
import me.darknet.assembler.printer.ClassPrinter;
import me.darknet.assembler.printer.JvmClassPrinter;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.util.JavaVersion;
import software.coley.recaf.workspace.model.Workspace;

import java.io.ByteArrayInputStream;
import java.util.List;

/**
 * JVM assembler pipeline implementation.
 *
 * @author Justus Garbe
 */
public class JvmAssemblerPipeline extends AbstractAssemblerPipeline<JvmClassInfo, JavaCompileResult, JavaClassRepresentation> {
	public static final String SERVICE_ID = "jvm-assembler";
	private static final Logger logger = Logging.get(JvmAssemblerPipeline.class);
	private final ASTProcessor processor = new ASTProcessor(BytecodeFormat.JVM);
	private final InheritanceGraph inheritanceGraph;
	private final Workspace workspace;

	public JvmAssemblerPipeline(@Nonnull Workspace workspace,
	                            @Nonnull InheritanceGraph inheritanceGraph,
	                            @Nonnull AssemblerPipelineGeneralConfig generalConfig,
	                            @Nonnull JvmAssemblerPipelineConfig jvmConfig) {
		super(generalConfig, jvmConfig, inheritanceGraph);
		this.workspace = workspace;
		this.inheritanceGraph = inheritanceGraph;
	}

	@Nonnull
	@Override
	public Result<List<ASTElement>> concreteParse(@Nonnull List<ASTElement> elements) {
		return processor.processAST(elements);
	}

	@Nonnull
	@Override
	public JavaClassRepresentation getRepresentation(@Nonnull JvmClassInfo info) {
		return new JavaClassRepresentation(info.getBytecode());
	}

	@Nonnull
	@Override
	protected CompilerOptions<? extends CompilerOptions<?>> getCompilerOptions() {
		JvmCompilerOptions options = new JvmCompilerOptions();
		if (pipelineConfig.isValueAnalysisEnabled())
			options.engineProvider(vars -> {
				ValuedJvmAnalysisEngine engine = new ValuedJvmAnalysisEngine(vars);
				if (pipelineConfig.isSimulatingCommonJvmCalls()) {
					engine.setFieldValueLookup(new WorkspaceFieldValueLookup(workspace, new BasicFieldValueLookup()));
					engine.setMethodValueLookup(new BasicMethodValueLookup());
				}
				return engine;
			});
		return options;
	}

	@Nonnull
	@Override
	protected Compiler getCompiler() {
		return new JvmCompiler();
	}

	@Override
	protected int getClassVersion(@Nonnull JvmClassInfo info) {
		return info.getVersion() - JavaVersion.VERSION_OFFSET;
	}

	@Nonnull
	@Override
	public JvmClassInfo getClassInfo(@Nonnull JavaClassRepresentation representation) {
		return new JvmClassInfoBuilder(representation.classFile()).build();
	}

	@Nonnull
	@Override
	protected Result<ClassPrinter> classPrinter(@Nonnull ClassPathNode path) {
		ClassInfo classInfo = path.getValue();
		try {
			return Result.ok(new JvmClassPrinter(new ByteArrayInputStream(classInfo.asJvmClass().getBytecode())));
		} catch (Throwable t) {
			logger.error("Uncaught error creating class printer for: {}", classInfo.getName(), t);
			return Result.exception(t);
		}
	}
}
