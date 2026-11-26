package software.coley.recaf.services.assembler;

import jakarta.annotation.Nonnull;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.backend.dalvik.DalvikClassRepresentation;
import me.darknet.assembler.backend.dalvik.DalvikTargetContext;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompileResult;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompiler;
import me.darknet.assembler.backend.dalvik.compile.DalvikCompilerOptions;
import me.darknet.assembler.backend.dalvik.printer.DalvikClassPrinter;
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
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.builder.AndroidClassInfoBuilder;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;

import java.util.List;

/**
 * Dalvik assembler pipeline implementation.
 *
 * @author Matt Coley
 */
public class AndroidAssemblerPipeline extends AbstractAssemblerPipeline<AndroidClassInfo, DalvikCompileResult, DalvikClassRepresentation> {
	public static final String SERVICE_ID = "dalvik-assembler";
	private static final Logger logger = Logging.get(AndroidAssemblerPipeline.class);
	private final ASTProcessor processor = new ASTProcessor(DalvikTargetContext.INSTANCE);

	public AndroidAssemblerPipeline(@Nonnull InheritanceGraph inheritanceGraph,
	                                @Nonnull AssemblerPipelineGeneralConfig generalConfig,
	                                @Nonnull AndroidAssemblerPipelineConfig androidConfig) {
		super(generalConfig, androidConfig, inheritanceGraph);
	}

	@Nonnull
	@Override
	public Outcome<List<ASTElement>> concreteParse(@Nonnull List<ASTElement> elements) {
		return processor.processAST(elements);
	}

	@Nonnull
	@Override
	public DalvikClassRepresentation getRepresentation(@Nonnull AndroidClassInfo info) {
		return new DalvikClassRepresentation(info.getBackingDefinition());
	}

	@Nonnull
	@Override
	protected CompilerOptions<?, DalvikClassRepresentation> getCompilerOptions(@Nonnull AndroidClassInfo info) {
		return new DalvikCompilerOptions()
				.withTypeAwareness(getTypeAwareness())
				.withInheritanceChecker(getInheritanceChecker());
	}

	@Override
	protected void configureCompilerOptions(@Nonnull CompilerOptions<?, DalvikClassRepresentation> options,
	                                        @Nonnull PathNode<?> path) {
		AndroidClassBundle bundle = path.getValueOfType(AndroidClassBundle.class);
		int version = bundle == null ? 0 : bundle.getVersion();
		if (options instanceof DalvikCompilerOptions dalvikOptions)
			dalvikOptions.withVersion(version);
	}

	@Override
	protected void setAnnotationPath(@Nonnull CompilerOptions<?, DalvikClassRepresentation> options, @Nonnull String path) {
		if (options instanceof DalvikCompilerOptions dakvikOptions)
			dakvikOptions.withAnnotationPath(path);
	}

	@Nonnull
	@Override
	protected TargetContext getTargetContext() {
		return DalvikTargetContext.INSTANCE;
	}

	@Nonnull
	@Override
	protected Compiler<?, ?, ?> getCompiler() {
		return new DalvikCompiler();
	}

	@Nonnull
	@Override
	public AndroidClassInfo getClassInfo(@Nonnull DalvikClassRepresentation representation) {
		return new AndroidClassInfoBuilder(representation.definition()).build();
	}

	@Nonnull
	@Override
	protected Outcome<ClassPrinter> classPrinter(@Nonnull ClassPathNode path) {
		ClassInfo classInfo = path.getValue();
		try {
			return Outcome.success(new DalvikClassPrinter(path.getValue().asAndroidClass().getBackingDefinition()));
		} catch (Throwable t) {
			logger.error("Uncaught error creating class printer for: {}", classInfo.getName(), t);
			return Outcome.failure(Diagnostic.error(DiagnosticPhase.BACKEND_EMISSION, DiagnosticCode.BACKEND_FAILURE,
					"Failed to create class printer for " + classInfo.getName() + ": " + t.getMessage(), null));
		}
	}
}
