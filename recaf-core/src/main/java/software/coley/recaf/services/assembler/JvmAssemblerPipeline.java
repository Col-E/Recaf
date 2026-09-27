package software.coley.recaf.services.assembler;

import jakarta.annotation.Nonnull;
import me.darknet.assembler.analysis.registry.BasicFieldValueLookup;
import me.darknet.assembler.analysis.registry.BasicMethodValueLookup;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.backend.jvm.JvmTargetContext;
import me.darknet.assembler.backend.jvm.compile.JavaClassRepresentation;
import me.darknet.assembler.backend.jvm.compile.JavaCompileResult;
import me.darknet.assembler.backend.jvm.compile.JvmCompiler;
import me.darknet.assembler.backend.jvm.compile.JvmCompilerOptions;
import me.darknet.assembler.backend.jvm.compile.analysis.jvm.ValuedJvmAnalysisEngine;
import me.darknet.assembler.backend.jvm.printer.JvmClassPrinter;
import me.darknet.assembler.compiler.Compiler;
import me.darknet.assembler.compiler.CompilerOptions;
import me.darknet.assembler.error.Diagnostic;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticPhase;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.parser.processor.ASTProcessor;
import me.darknet.assembler.printer.ClassPrinter;
import me.darknet.assembler.target.TargetContext;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.util.JavaVersion;
import software.coley.recaf.workspace.model.Workspace;

import java.util.List;

/**
 * JVM assembler pipeline implementation.
 *
 * @author Justus Garbe
 */
public class JvmAssemblerPipeline extends AbstractAssemblerPipeline<JvmClassInfo, JavaCompileResult, JavaClassRepresentation> {
	public static final String SERVICE_ID = "jvm-assembler";
	private static final Logger logger = Logging.get(JvmAssemblerPipeline.class);
	private final ASTProcessor processor = new ASTProcessor(JvmTargetContext.INSTANCE);
	private final Workspace workspace;

	public JvmAssemblerPipeline(@Nonnull Workspace workspace,
	                            @Nonnull InheritanceGraph inheritanceGraph,
	                            @Nonnull AssemblerPipelineGeneralConfig generalConfig,
	                            @Nonnull JvmAssemblerPipelineConfig jvmConfig) {
		super(generalConfig, jvmConfig, inheritanceGraph);
		this.workspace = workspace;
	}

	@Nonnull
	@Override
	public Outcome<List<ASTElement>> concreteParse(@Nonnull List<ASTElement> elements) {
		return processor.processAST(elements);
	}

	@Nonnull
	@Override
	public JavaClassRepresentation getRepresentation(@Nonnull JvmClassInfo info) {
		return new JavaClassRepresentation(info.getBytecode());
	}

	@Nonnull
	@Override
	protected CompilerOptions<?, JavaClassRepresentation> getCompilerOptions(@Nonnull JvmClassInfo info) {
		JvmCompilerOptions options = new JvmCompilerOptions()
				.withTypeAwareness(getTypeAwareness())
				.withInheritanceChecker(getInheritanceChecker())
				.withVariableTableMode(getConfig().getVariableTableMode())
				.withReuseOverlayPool(getConfig().isRecycleConstPool());
		if (pipelineConfig.isValueAnalysisEnabled())
			options.withEngineProvider(vars -> {
				ValuedJvmAnalysisEngine engine = new ValuedJvmAnalysisEngine(vars);
				if (pipelineConfig.isSimulatingCommonJvmCalls()) {
					engine.setFieldValueLookup(new WorkspaceFieldValueLookup(workspace, new BasicFieldValueLookup()));
					engine.setMethodValueLookup(new BasicMethodValueLookup());
				}
				return engine;
			});
		return options;
	}

	@Override
	protected void setAnnotationPath(@Nonnull CompilerOptions<?, JavaClassRepresentation> options, @Nonnull String path) {
		if (options instanceof JvmCompilerOptions jvmOpts)
			jvmOpts.withAnnotationPath(path);
	}

	@Nonnull
	@Override
	protected TargetContext getTargetContext() {
		return JvmTargetContext.INSTANCE;
	}

	@Nonnull
	@Override
	protected Compiler<?, ?, ?> getCompiler() {
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
	protected Outcome<ClassPrinter> classPrinter(@Nonnull ClassPathNode path) {
		ClassInfo classInfo = path.getValue();
		try {
			return Outcome.success(new JvmClassPrinter(classInfo.asJvmClass().getBytecode()));
		} catch (Throwable t) {
			logger.error("Uncaught error creating class printer for: {}", classInfo.getName(), t);
			return Outcome.failure(Diagnostic.error(DiagnosticPhase.BACKEND_EMISSION, DiagnosticCode.BACKEND_FAILURE,
					"Failed to create class printer for " + classInfo.getName() + ": " + t.getMessage(), null));
		}
	}

	@Nonnull
	@Override
	public JvmAssemblerPipelineConfig getConfig() {
		return (JvmAssemblerPipelineConfig) super.getConfig();
	}
}
