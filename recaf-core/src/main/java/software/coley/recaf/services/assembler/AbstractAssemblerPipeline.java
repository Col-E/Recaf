package software.coley.recaf.services.assembler;

import jakarta.annotation.Nonnull;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.compiler.ClassResult;
import me.darknet.assembler.compiler.Compiler;
import me.darknet.assembler.compiler.CompilerOptions;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.compiler.TypeAwareness;
import me.darknet.assembler.error.Diagnostic;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.error.DiagnosticPhase;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.printer.AnnotationHolder;
import me.darknet.assembler.printer.AnnotationPrinter;
import me.darknet.assembler.printer.ClassPrinter;
import me.darknet.assembler.printer.PrintContext;
import me.darknet.assembler.printer.Printer;
import me.darknet.assembler.processing.SemanticProcessor;
import me.darknet.assembler.processing.ValidatedUnit;
import me.darknet.assembler.target.TargetContext;
import software.coley.observables.AbstractObservable;
import software.coley.observables.ChangeListener;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.annotation.Annotated;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.path.AnnotationPathNode;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.workspace.model.Workspace;

import java.util.ArrayList;
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
public abstract class AbstractAssemblerPipeline<C extends ClassInfo, R extends ClassResult<I>, I> implements AssemblerPipeline<C, R, I> {
	protected final AssemblerPipelineConfig pipelineConfig;
	private final AssemblerPipelineGeneralConfig generalConfig;
	private final InheritanceGraph inheritanceGraph;
	private final ListenerHost indentListener = new ListenerHost();
	protected PrintContext<?> context;

	public AbstractAssemblerPipeline(@Nonnull AssemblerPipelineGeneralConfig generalConfig,
	                                 @Nonnull AssemblerPipelineConfig pipelineConfig,
	                                 @Nonnull InheritanceGraph inheritanceGraph) {
		this.generalConfig = generalConfig;
		this.pipelineConfig = pipelineConfig;
		this.inheritanceGraph = inheritanceGraph;

		generalConfig.getDisassemblyIndent().addChangeListener(indentListener);
	}

	private void refreshContext() {
		context = new PrintContext<>(generalConfig.getDisassemblyIndent().getValue());

		// 10000000000 vs 1E10
		if (generalConfig.getUseWholeFloatingNumbers().getValue())
			context.setForceWholeNumberRepresentation(true);

		// Enable comments that outline where try-catch ranges begin/end.
		if (pipelineConfig instanceof JvmAssemblerPipelineConfig jvmConfig && jvmConfig.emitTryRangeComments())
			context.setDebugTryCatchRanges(true);
	}

	@Nonnull
	protected abstract Outcome<ClassPrinter> classPrinter(@Nonnull ClassPathNode path);

	@Nonnull
	protected abstract CompilerOptions<?, I> getCompilerOptions(@Nonnull C info);

	protected abstract void setAnnotationPath(@Nonnull CompilerOptions<?, I> options, @Nonnull String path);

	@Nonnull
	protected abstract TargetContext getTargetContext();

	@Nonnull
	protected abstract Compiler<?, ?, ?> getCompiler();

	@Nonnull
	protected TypeAwareness getTypeAwareness() {
		return new TypeAwareness() {
			@Override
			public boolean isAwareOf(String type) {
				return inheritanceGraph.getVertex(type) != null;
			}

			@Override
			public String notifyUnknownType(String type) {
				// TODO: We should have 'assembler.missingtypewarning' in the lang files, but we're not in the UI module
				//   so we can't reference it here...
				return "Type '" + type + "' is not present in the workspace. StackMapTable references to it will degrade to 'java/lang/Object'\n" +
						"Consider either:\n" +
						"- Adding a dependency containing the class to the workspace.\n" +
						"- Enabling workspace phantom generation under 'All Services > Analysis > Phantom generator'.";
			}
		};
	}

	@Nonnull
	protected InheritanceChecker getInheritanceChecker() {
		return new InheritanceChecker() {
			@Override
			public boolean isSubclassOf(String child, String parent) {
				return inheritanceGraph.isAssignableFrom(parent, child);
			}

			@Override
			public String getCommonSuperclass(String type1, String type2) {
				return inheritanceGraph.getCommon(type1, type2);
			}
		};
	}

	protected abstract int getClassVersion(@Nonnull C info);

	@Nonnull
	@Override
	public Outcome<R> assemble(@Nonnull List<ASTElement> elements, @Nonnull PathNode<?> path) {
		return compile(elements, path);
	}

	@Nonnull
	@Override
	public Outcome<String> disassemble(@Nonnull ClassPathNode path) {
		return printOutcome(classPrinter(path));
	}

	@Nonnull
	@Override
	public Outcome<String> disassemble(@Nonnull ClassMemberPathNode path) {
		return printOutcome(memberPrinter(path));
	}

	@Nonnull
	@Override
	public Outcome<String> disassemble(@Nonnull AnnotationPathNode path) {
		return printOutcome(annotationPrinter(path));
	}

	@Nonnull
	private Outcome<String> printOutcome(@Nonnull Outcome<? extends Printer> outcome) {
		if (!outcome.hasValue())
			return Outcome.of(null, outcome.diagnostics());
		return Outcome.of(print(outcome.requireValue()), outcome.diagnostics());
	}

	@Nonnull
	@SuppressWarnings({"unchecked", "rawtypes"})
	protected Outcome<R> compile(@Nonnull List<ASTElement> elements, @Nonnull PathNode<?> path) {
		if (elements.isEmpty()) {
			return Outcome.failure(Diagnostic.error(DiagnosticPhase.TARGET_VALIDATION,
					DiagnosticCode.MALFORMED_DECLARATION, "No elements to compile", null));
		}
		if (elements.size() != 1) {
			return Outcome.failure(Diagnostic.error(DiagnosticPhase.TARGET_VALIDATION,
					DiagnosticCode.MALFORMED_DECLARATION, "Multiple elements to compile", elements.get(1).location()));
		}
		ASTElement element = elements.get(0);

		if (element == null) {
			return Outcome.failure(Diagnostic.error(DiagnosticPhase.TARGET_VALIDATION,
					DiagnosticCode.MALFORMED_DECLARATION, "No element to compile", null));
		}

		ClassInfo classInfo = path.getValueOfType(ClassInfo.class);
		if (classInfo == null) {
			return Outcome.failure(Diagnostic.error(DiagnosticPhase.TARGET_VALIDATION,
					DiagnosticCode.UNCLASSIFIED, "Dangling member", null));
		}

		C info = (C) classInfo;
		CompilerOptions<?, I> options = getCompilerOptions(info);
		if (element.type() != ElementType.CLASS) {
			options.withOverlay(getRepresentation(info));

			if (element.type() == ElementType.ANNOTATION) {
				String annotationPath = "this";
				PathNode<?> parent = path.getParent();
				if (parent instanceof ClassMemberPathNode memberPathNode) {
					annotationPath += memberPathNode.isMethod() ? ".method." : ".field.";
					annotationPath += memberPathNode.getValue().getName() + ".";
					annotationPath += memberPathNode.getValue().getDescriptor();
				}

				Annotated annotated = path.getValueOfType(Annotated.class);
				if (annotated == null) {
					return Outcome.failure(Diagnostic.error(DiagnosticPhase.TARGET_VALIDATION,
							DiagnosticCode.UNCLASSIFIED, "Dangling annotation", null));
				}

				AnnotationInfo annotation = (AnnotationInfo) path.getValue();
				annotationPath += annotated.getAnnotations().indexOf(annotation);
				setAnnotationPath(options, annotationPath);
			}
		}

		Outcome<ValidatedUnit> processed = SemanticProcessor.process(elements, getTargetContext());
		if (processed.hasErrors())
			return Outcome.of(null, processed.diagnostics());

		Outcome<R> compiled = (Outcome<R>) ((Compiler) getCompiler()).compile(processed.requireValue(), options);
		if (processed.diagnostics().isEmpty())
			return compiled;

		List<Diagnostic> diagnostics = new ArrayList<>(processed.diagnostics());
		diagnostics.addAll(compiled.diagnostics());
		return Outcome.of(compiled.value(), diagnostics);
	}

	@Nonnull
	protected Outcome<Printer> memberPrinter(@Nonnull ClassMemberPathNode path) {
		ClassPathNode owner = path.getParent();
		if (owner == null)
			return failure("Dangling member");

		Outcome<ClassPrinter> classOutcome = classPrinter(owner);
		if (!classOutcome.hasValue())
			return Outcome.of(null, classOutcome.diagnostics());

		ClassMember member = path.getValue();
		ClassPrinter printer = classOutcome.requireValue();
		Printer memberPrinter = null;
		if (member.isMethod()) {
			memberPrinter = printer.method(member.getName(), member.getDescriptor());
		} else if (member.isField()) {
			memberPrinter = printer.field(member.getName(), member.getDescriptor());
		}

		if (memberPrinter == null)
			return failure(classOutcome, "Failed to find member");
		return Outcome.of(memberPrinter, classOutcome.diagnostics());
	}

	@Nonnull
	protected Outcome<AnnotationPrinter> annotationPrinter(@Nonnull AnnotationPathNode path) {
		if (path.getParent() == null)
			return failure("Dangling annotation");

		Object parent = path.getParent().getValue();
		Outcome<? extends Printer> parentOutcome;
		if (parent instanceof ClassPathNode classNode) {
			parentOutcome = classPrinter(classNode);
		} else if (parent instanceof ClassMemberPathNode classMember) {
			parentOutcome = memberPrinter(classMember);
		} else {
			return failure("Invalid parent type");
		}

		if (!parentOutcome.hasValue())
			return Outcome.of(null, parentOutcome.diagnostics());
		if (!(parent instanceof Annotated annotated))
			return failure(parentOutcome, "Parent cannot hold annotations");

		Printer printer = parentOutcome.requireValue();
		if (!(printer instanceof AnnotationHolder holder))
			return failure(parentOutcome, "Parent is not an annotation holder");

		AnnotationInfo annotation = path.getValue();
		AnnotationPrinter annotationPrinter = holder.annotation(annotated.getAnnotations().indexOf(annotation));
		if (annotationPrinter == null)
			return failure(parentOutcome, "Failed to find annotation");
		return Outcome.of(annotationPrinter, parentOutcome.diagnostics());
	}

	@Nonnull
	private static <T> Outcome<T> failure(@Nonnull String message) {
		return Outcome.failure(Diagnostic.error(DiagnosticPhase.BACKEND_EMISSION,
				DiagnosticCode.UNCLASSIFIED, message, null));
	}

	@Nonnull
	private static <T> Outcome<T> failure(@Nonnull Outcome<?> previous, @Nonnull String message) {
		List<Diagnostic> diagnostics = new ArrayList<>(previous.diagnostics());
		diagnostics.add(Diagnostic.error(DiagnosticPhase.BACKEND_EMISSION,
				DiagnosticCode.UNCLASSIFIED, message, null));
		return Outcome.of(null, diagnostics);
	}

	@Nonnull
	protected String print(@Nonnull Printer printer) {
		refreshContext(); // new context
		printer.print(context);
		return context.toString();
	}

	/**
	 * Called when the associated {@link Workspace} for this pipeline is closed.
	 */
	public void close() {
		generalConfig.getDisassemblyIndent().removeChangeListener(indentListener);
	}

	@Nonnull
	@Override
	public AssemblerPipelineConfig getConfig() {
		return pipelineConfig;
	}

	private class ListenerHost implements ChangeListener<String> {
		@Override
		public void changed(AbstractObservable<? extends String> abstractObservable, String s, String t1) {
			refreshContext();
		}
	}
}
