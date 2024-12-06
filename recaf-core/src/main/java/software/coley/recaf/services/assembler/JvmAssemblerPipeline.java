package software.coley.recaf.services.assembler;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
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
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.parser.BytecodeFormat;
import me.darknet.assembler.parser.processor.ASTProcessor;
import me.darknet.assembler.printer.ClassPrinter;
import me.darknet.assembler.printer.JvmClassPrinter;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.cdi.WorkspaceScoped;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.path.AnnotationPathNode;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceGraphService;
import software.coley.recaf.services.inheritance.InheritanceVertex;
import software.coley.recaf.util.JavaVersion;
import software.coley.recaf.workspace.model.Workspace;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Objects;

/**
 * JVM assembler pipeline implementation.
 *
 * @author Justus Garbe
 */
@WorkspaceScoped
public class JvmAssemblerPipeline extends AbstractAssemblerPipeline<JvmClassInfo, JavaCompileResult, JavaClassRepresentation> {
	public static final String SERVICE_ID = "jvm-assembler";
	private static final Logger logger = Logging.get(JvmAssemblerPipeline.class);
	private final ASTProcessor processor = new ASTProcessor(BytecodeFormat.JVM);
	private final InheritanceGraph inheritanceGraph;
	private final Workspace workspace;

	@Inject
	public JvmAssemblerPipeline(@Nonnull Workspace workspace,
	                            @Nonnull InheritanceGraphService graphService,
	                            @Nonnull AssemblerPipelineGeneralConfig generalConfig,
	                            @Nonnull JvmAssemblerPipelineConfig config) {
		super(generalConfig, config);
		this.workspace = workspace;
		this.inheritanceGraph = Objects.requireNonNull(graphService.getCurrentWorkspaceInheritanceGraph(), "Graph not created");
	}

	@Nonnull
	@Override
	public Result<List<ASTElement>> concreteParse(@Nonnull List<ASTElement> elements) {
		return processor.processAST(elements);
	}

	@Nonnull
	@Override
	public Result<JavaCompileResult> assemble(@Nonnull List<ASTElement> elements, @Nonnull PathNode<?> path) {
		return compile(elements, path);
	}

	@Nonnull
	@Override
	public Result<String> disassemble(@Nonnull ClassPathNode path) {
		return classPrinter(path).map(this::print);
	}

	@Nonnull
	@Override
	public Result<String> disassemble(@Nonnull ClassMemberPathNode path) {
		return memberPrinter(path).map(this::print);
	}

	@Nonnull
	@Override
	public Result<String> disassemble(@Nonnull AnnotationPathNode path) {
		return annotationPrinter(path).map(this::print);
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

	@Nonnull
	@Override
	protected InheritanceChecker getInheritanceChecker() {
		return new InheritanceChecker() {
			@Override
			public boolean isSubclassOf(String child, String parent) {
				InheritanceVertex childVertex = inheritanceGraph.getVertex(child);
				InheritanceVertex parentVertex = inheritanceGraph.getVertex(parent);

				if (childVertex == null || parentVertex == null) {
					return false;
				}

				return childVertex.isChildOf(parentVertex);
			}

			@Override
			public String getCommonSuperclass(String type1, String type2) {
				return inheritanceGraph.getCommon(type1, type2);
			}
		};
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
