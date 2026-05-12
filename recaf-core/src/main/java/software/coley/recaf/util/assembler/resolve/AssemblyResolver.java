package software.coley.recaf.util.assembler.resolve;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.ast.primitive.ASTObject;
import me.darknet.assembler.ast.specific.ASTAnnotation;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTException;
import me.darknet.assembler.ast.specific.ASTField;
import me.darknet.assembler.ast.specific.ASTInner;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.util.ElementMap;
import me.darknet.assembler.util.Range;
import software.coley.recaf.util.assembler.JasmUtils;

import java.util.List;

/**
 * Helper for determining what content is at a specific offset within an assembler's last parsed AST.
 *
 * @author Matt Coley
 */
public class AssemblyResolver {
	private List<ASTElement> ast;

	/**
	 * @param ast
	 * 		New AST to operate off of.
	 */
	public void setAst(@Nullable List<ASTElement> ast) {
		this.ast = ast;
	}

	/**
	 * @param position
	 * 		Offset in the assembler source/AST.
	 *
	 * @return Resolved content at the given position.
	 */
	@Nonnull
	public AssemblyResolution resolveAt(int position) {
		if (ast == null)
			return AssemblyResolution.EMPTY;

		AssemblyResolution resolution = resolveAt(position, null, ast);
		if (resolution != null)
			return resolution;

		return AssemblyResolution.EMPTY;
	}

	@Nullable
	private static AssemblyResolution resolveAt(int position, @Nullable ASTClass parentClassDec, @Nonnull List<ASTElement> ast) {
		// Example JASM snippet for reference:
        /*
        .super java/lang/Object
        .annotation TestAnnotation {
            number: 15,
            subAnnotation: .annotation org/jetbrains/annotations/NotNull {
                value: "Hello, world!"
            },
            stringArray: { "one", "two", "three" }
        }
        .inner private static {
            name: InnerClass,
            inner: Example$InnerClass,
            outer: Example
        }
        .class public super Example {
            .method public exampleMethod (LExample;)LExample; {
                parameters: { this, other },
                exceptions: { { A, A, B, * } },
                code: {
                A:
                    aload this
                    areturn
                B:
                }
            }
        }
        */
		for (ASTElement child : ast) {
			Range range = child.range();
			if (range != null && range.within(position)) {
				if (child instanceof ASTMethod method) {
					ASTElement selectedParameter = get(position, method.parameters());
					if (selectedParameter != null)
						return new VariableDeclarationResolution(parentClassDec, method, (ASTIdentifier) selectedParameter);

					for (ASTException exception : method.exceptions()) {
						ASTElement selectedLabel = get(position, List.of(exception.start(), exception.end(), exception.handler()));
						if (selectedLabel != null)
							return new LabelReferenceResolution(parentClassDec, method, (ASTIdentifier) selectedLabel);
						ASTElement selectedType = get(position, List.of(exception.exceptionType()));
						if (selectedType != null && selectedType.content().charAt(0) == '*')
							return new TypeReferenceResolution(parentClassDec, method, (ASTIdentifier) selectedType);
					}

					ASTElement selectedAnno = get(position, method.getVisibleAnnotations());
					if (selectedAnno != null)
						return new MethodAnnotationResolution(parentClassDec, method, (ASTAnnotation) selectedAnno);
					selectedAnno = get(position, method.getInvisibleAnnotations());
					if (selectedAnno != null)
						return new MethodAnnotationResolution(parentClassDec, method, (ASTAnnotation) selectedAnno);

					ASTElement selectedInstruction = get(position, method.code().instructions());
					if (selectedInstruction instanceof ASTLabel label)
						return new LabelDeclarationResolution(parentClassDec, method, label);
					else if (selectedInstruction != null) {
						ASTInstruction insn = (ASTInstruction) selectedInstruction;
						AssemblyResolution insnResolution = resolveInstructionSelection(position, parentClassDec, method, insn);
						if (insnResolution != null)
							return insnResolution;
						return new InstructionResolution(parentClassDec, method, insn);
					}

					return new MethodResolution(parentClassDec, method);
				} else if (child instanceof ASTField field) {
					ASTElement selectedAnno = get(position, field.getVisibleAnnotations());
					if (selectedAnno != null)
						return new FieldAnnotationResolution(parentClassDec, field, (ASTAnnotation) selectedAnno);
					selectedAnno = get(position, field.getInvisibleAnnotations());
					if (selectedAnno != null)
						return new FieldAnnotationResolution(parentClassDec, field, (ASTAnnotation) selectedAnno);

					return new FieldResolution(parentClassDec, field);
				} else if (child instanceof ASTClass klass) {
					ASTElement selectedInterface = get(position, klass.getInterfaces());
					if (selectedInterface != null)
						return new ClassImplements(klass, (ASTIdentifier) selectedInterface);

					ASTElement selectedInner = get(position, klass.getInners());
					if (selectedInner != null)
						return new InnerClassResolution(klass, (ASTInner) selectedInner);

					ASTElement selectedAnno = get(position, klass.getVisibleAnnotations());
					if (selectedAnno != null)
						return new ClassAnnotationResolution(klass, (ASTAnnotation) selectedAnno);
					selectedAnno = get(position, klass.getInvisibleAnnotations());
					if (selectedAnno != null)
						return new ClassAnnotationResolution(klass, (ASTAnnotation) selectedAnno);

					ASTIdentifier superName = klass.getSuperName();
					if (superName != null && superName.range().within(position))
						return new ClassExtends(klass, superName);

					// Recurse for declared fields/methods
					return resolveAt(position, klass, klass.contents());
				} else if (child instanceof ASTAnnotation anno) {
					return new IndependentAnnotationResolution(anno);
				}
			}
		}
		return null;
	}

	@Nullable
	private static AssemblyResolution resolveInstructionSelection(int position, @Nullable ASTClass parentClassDec,
	                                                             @Nonnull ASTMethod method, @Nonnull ASTInstruction instruction) {
		ASTElement selected = instruction.pick(position);
		if (!(selected instanceof ASTIdentifier identifier))
			return null;

		if (isLabelReference(instruction, identifier))
			return new LabelReferenceResolution(parentClassDec, method, identifier);
		if (isVariableReference(instruction, identifier))
			return new VariableDeclarationResolution(parentClassDec, method, identifier);
		if (isTypeReference(instruction, identifier))
			return new TypeReferenceResolution(parentClassDec, method, identifier);

		return null;
	}

	private static boolean isLabelReference(@Nonnull ASTInstruction instruction, @Nonnull ASTIdentifier selected) {
		String name = instruction.identifier().content();
		if (name == null)
			return false;
		if (JasmUtils.isFlowControlInstruction(name))
			return isMatchingIdentifierArgument(instruction, 0, selected);
		if ("tableswitch".equals(name))
			return isTableSwitchLabelReference(instruction, selected);
		if ("lookupswitch".equals(name))
			return isLookupSwitchLabelReference(instruction, selected);
		return false;
	}

	private static boolean isVariableReference(@Nonnull ASTInstruction instruction, @Nonnull ASTIdentifier selected) {
		String name = instruction.identifier().content();
		return JasmUtils.isVariableReferenceInstruction(name) && isMatchingIdentifierArgument(instruction, 0, selected);
	}

	private static boolean isTypeReference(@Nonnull ASTInstruction instruction, @Nonnull ASTIdentifier selected) {
		String name = instruction.identifier().content();
		return JasmUtils.isTypeReferenceInstruction(name) && isMatchingIdentifierArgument(instruction, 0, selected);
	}

	private static boolean isTableSwitchLabelReference(@Nonnull ASTInstruction instruction, @Nonnull ASTIdentifier selected) {
		if (instruction.arguments().isEmpty() || !(instruction.arguments().getFirst() instanceof ASTObject switchObj))
			return false;

		ASTElement defaultCase = switchObj.value("default");
		if (selected == defaultCase)
			return true;

		ASTArray cases = switchObj.value("cases");
		return cases != null && cases.values().contains(selected);
	}

	private static boolean isLookupSwitchLabelReference(@Nonnull ASTInstruction instruction, @Nonnull ASTIdentifier selected) {
		if (instruction.arguments().isEmpty() || !(instruction.arguments().getFirst() instanceof ASTObject switchObj))
			return false;

		ASTElement defaultCase = switchObj.value("default");
		if (selected == defaultCase)
			return true;

		ElementMap<ASTIdentifier, ASTElement> values = switchObj.values();
		for (int i = 0; i < values.size(); i++) {
			ASTIdentifier key = values.key(i);
			if ("default".equals(key.content()))
				continue;
			if (selected == values.get(i))
				return true;
		}
		return false;
	}

	private static boolean isMatchingIdentifierArgument(@Nonnull ASTInstruction instruction, int argumentIndex,
	                                                    @Nonnull ASTIdentifier selected) {
		List<ASTElement> arguments = instruction.arguments();
		return arguments.size() > argumentIndex && arguments.get(argumentIndex) == selected;
	}

	@Nullable
	private static ASTElement get(int position, @Nonnull List<? extends ASTElement> ast) {
		for (ASTElement child : ast) {
			Range range = child.range();
			if (range != null && range.within(position)) {
				return child;
			}
		}
		return null;
	}
}
