package software.coley.recaf.services.assembler;

import jakarta.annotation.Nonnull;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.compiler.*;
import me.darknet.assembler.compiler.Compiler;
import me.darknet.assembler.error.Error;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.printer.*;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.annotation.Annotated;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.path.AnnotationPathNode;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;

import java.util.List;

/**
 * Common pipeline implementation details for all class types.
 *
 * @param <C>
 * 		Class type which will be assembled.
 * @param <R>
 * 		Compile return value for JASM {@link Compiler}.
 * @param <I>
 * 		Class intermediate representation type.
 *
 * @author Justus Garbe
 */
public abstract class AbstractAssemblerPipeline<C extends ClassInfo, R extends ClassResult, I extends ClassRepresentation> implements AssemblerPipeline<C, R, I> {
	protected final AssemblerPipelineConfig pipelineConfig;
	private final AssemblerPipelineGeneralConfig generalConfig;
	protected PrintContext<?> context;

	public AbstractAssemblerPipeline(@Nonnull AssemblerPipelineGeneralConfig generalConfig,
									 @Nonnull AssemblerPipelineConfig pipelineConfig) {
		this.generalConfig = generalConfig;
		this.pipelineConfig = pipelineConfig;

		generalConfig.getDisassemblyIndent().addChangeListener((ob, old, current) -> refreshContext());
	}

	private void refreshContext() {
		context = new PrintContext<>(generalConfig.getDisassemblyIndent().getValue());

		// Enable comments that outline where try-catch ranges begin/end.
		if (pipelineConfig instanceof JvmAssemblerPipelineConfig jvmConfig && jvmConfig.emitTryRangeComments())
			context.setDebugTryCatchRanges(true);
	}

	@Nonnull
	protected abstract Result<ClassPrinter> classPrinter(@Nonnull ClassPathNode path);

	@Nonnull
	protected abstract CompilerOptions<? extends CompilerOptions<?>> getCompilerOptions();

	@Nonnull
	protected abstract Compiler getCompiler();

	@Nonnull
	protected abstract InheritanceChecker getInheritanceChecker();

	protected abstract int getClassVersion(@Nonnull C info);

	@Nonnull
	@SuppressWarnings("unchecked")
	protected Result<R> compile(@Nonnull List<ASTElement> elements, @Nonnull PathNode<?> path) {
		if (elements.isEmpty()) {
			return Result.err(Error.of("No elements to compile", null));
		}
		if (elements.size() != 1) {
			return Result.err(Error.of("Multiple elements to compile", elements.get(1).location()));
		}
		ASTElement element = elements.get(0);

		if (element == null) {
			return Result.err(Error.of("No element to compile", null));
		}

		ClassInfo classInfo = path.getValueOfType(ClassInfo.class);
		if (classInfo == null) {
			return Result.err(Error.of("Dangling member", null));
		}

		C info = (C) classInfo;

		CompilerOptions<? extends CompilerOptions<?>> options = getCompilerOptions();
		options.version(getClassVersion(info))
				.inheritanceChecker(getInheritanceChecker());

		if (element.type() != ElementType.CLASS) {
			options.overlay(getRepresentation(info));

			if (element.type() == ElementType.ANNOTATION) {
				// build annotation path
				String annoPath = "this";
				PathNode<?> parent = path.getParent();
				if (parent instanceof ClassMemberPathNode memberPathNode) {
					annoPath += memberPathNode.isMethod() ? ".method." : ".field.";
					annoPath += memberPathNode.getValue().getName() + ".";
					annoPath += memberPathNode.getValue().getDescriptor();
				}

				Annotated annotated = path.getValueOfType(Annotated.class);

				if (annotated == null) {
					return Result.err(Error.of("Dangling annotation", null));
				}

				AnnotationInfo annotation = (AnnotationInfo) path.getValue();

				annoPath += annotated.getAnnotations().indexOf(annotation);

				options.annotationPath(annoPath);
			}
		}

		Compiler compiler = getCompiler();

		return (Result<R>) compiler.compile(elements, options);
	}

	@Nonnull
	protected Result<Printer> memberPrinter(@Nonnull ClassMemberPathNode path) {
		ClassPathNode owner = path.getParent();
		if (owner == null)
			return Result.err(Error.of("Dangling member", null));

		ClassMember member = path.getValue();
		return classPrinter(owner).flatMap((printer) -> {
			Printer memberPrinter = null;

			if (member.isMethod()) {
				memberPrinter = printer.method(member.getName(), member.getDescriptor());
			} else if (member.isField()) {
				memberPrinter = printer.field(member.getName(), member.getDescriptor());
			}

			if (memberPrinter == null) {
				return Result.err(Error.of("Failed to find member", null));
			} else {
				return Result.ok(memberPrinter);
			}
		});
	}

	@Nonnull
	protected Result<AnnotationPrinter> annotationPrinter(@Nonnull AnnotationPathNode path) {
		if (path.getParent() == null) {
			return Result.err(Error.of("Dangling annotation", null));
		}

		Object parent = path.getParent().getValue();
		Result<? extends Printer> parentPrinter;
		if (parent instanceof ClassPathNode classNode) {
			parentPrinter = classPrinter(classNode);
		} else if (parent instanceof ClassMemberPathNode classMember) {
			parentPrinter = memberPrinter(classMember);
		} else {
			return Result.err(Error.of("Invalid parent type", null));
		}

		AnnotationInfo annotation = path.getValue();

		if (parent instanceof Annotated annotated) {

			return parentPrinter.flatMap((printer) -> {
				if (printer instanceof AnnotationHolder holder) {
					return Result.ok(holder.annotation(annotated.getAnnotations().indexOf(annotation)));
				} else {
					return Result.err(Error.of("Parent is not an annotation holder", null));
				}
			});

		} else {
			return Result.err(Error.of("Parent cannot hold annotations", null));
		}
	}

	@Nonnull
	protected String print(@Nonnull Printer printer) {
		refreshContext(); // new context
		printer.print(context);
		return context.toString();
	}

	@Nonnull
	@Override
	public AssemblerPipelineConfig getConfig() {
		return pipelineConfig;
	}
}
