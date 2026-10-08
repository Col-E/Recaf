package software.coley.recaf.ui.control.richtext.suggest.assembler;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTDeclaration;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTObject;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.backend.dalvik.DalvikTargetContext;
import me.darknet.assembler.backend.dalvik.instructions.DalvikOperands;
import me.darknet.assembler.backend.jvm.JvmTargetContext;
import me.darknet.assembler.backend.jvm.instructions.JvmOperands;
import me.darknet.assembler.helper.Handle;
import me.darknet.assembler.instructions.OperandRole;
import me.darknet.assembler.instructions.SwitchShape;
import me.darknet.assembler.target.TargetContext;
import software.coley.recaf.ui.control.richtext.suggest.assembler.AssemblerOperandLocator.Site;

import java.util.List;

import static software.coley.recaf.ui.control.richtext.suggest.assembler.AssemblerOperandLocator.contains;

/**
 * Resolves which structured position the caret is in within an operand. Structured operands are JVM and Dalvik
 * {@code handle}, {@code constant}, and call-site argument values, plus Dalvik and JVM declarations.
 * <p>
 * Also decides whether a caret on a switch payload is on a label value, which is the only position that offers labels.
 *
 * @author Matt Coley
 */
final class AssemblerStructuredOperands {
	private AssemblerStructuredOperands() {}

	/**
	 * @param site
	 * 		Operand under the caret.
	 *
	 * @return {@code true} if the caret is at a label value of a switch payload, rather than at a case key or the
	 * payload's own keyword. Only operands with a switch payload role and a known switch shape qualify.
	 */
	static boolean isSwitchLabelPosition(@Nonnull Site site) {
		if (site.argumentIndex() < 0 || site.argumentIndex() >= site.definition().operandCount())
			return false;

		// Check that the operand is a switch payload and has a known shape.
		OperandRole role = site.definition().role(site.argumentIndex());
		SwitchShape shape = site.definition().switchShape();
		if (role == null || role.kind() != OperandRole.RoleKind.SWITCH_PAYLOAD || shape == null)
			return false;

		// Check if the caret is on a case key or default key.
		if (site.argument() instanceof ASTObject object) {
			for (var pair : object.values().pairs()) {
				if (pair.first() != null && contains(pair.first().range(), site.caret()))
					return false;
				ASTElement value = pair.second();
				if (value == null || !contains(value.range(), site.caret()))
					continue;
				String key = pair.first() == null ? null : pair.first().literal();
				if (key != null && key.equals(shape.defaultKey()))
					return true;
				if (key != null && key.equals(shape.casesKey()))
					return true;
				return shape == SwitchShape.LOOKUP || shape == SwitchShape.SPARSE;
			}
		}

		// Check if the caret is on a case key or default key.
		String key = site.nestedKey();
		if (key == null)
			return false;
		if (key.equals(shape.defaultKey()) || key.equals(shape.casesKey()))
			return true;
		return shape == SwitchShape.LOOKUP || shape == SwitchShape.SPARSE;
	}

	/**
	 * Resolves where the caret sits inside a structured operand.
	 *
	 * @param site
	 * 		Operand under the caret.
	 * @param targetContext
	 * 		Instruction language of the edited class.
	 *
	 * @return The structured position, or {@link StructuredOperand#NONE} if the operand has no structured schema.
	 */
	@Nonnull
	static StructuredOperand structuredOperand(@Nonnull Site site, @Nonnull TargetContext targetContext) {
		if (site.argumentIndex() < 0 || site.argumentIndex() >= site.definition().operandCount())
			return StructuredOperand.NONE;

		// Both JVM and Dalvik have basically the same resolution logic,
		// but with their respective 'XOperands' supplying the schema for each operand.
		//  - Handles
		//  - Constants
		//  - Call-site arguments
		var schema = site.definition().operand(site.argumentIndex());
		List<Integer> path = site.nestedIndices();
		if (targetContext == JvmTargetContext.INSTANCE) {
			if (schema == JvmOperands.HANDLE.getOperand()) {
				if (path.isEmpty() && !(site.argument() instanceof ASTArray))
					return new StructuredOperand(StructuredKind.HANDLE_SHORTCUT, null, null);
				return handlePosition(site, site.argument(), path, 0);
			}
			if (schema == JvmOperands.CONSTANT.getOperand())
				return constantPosition(site, site.argument(), path, 0);
			if (schema == JvmOperands.ARGS.getOperand()) {
				if (path.isEmpty())
					return StructuredOperand.NONE;
				ASTElement constant = arrayValue(site.argument(), path.getFirst());
				return constantPosition(site, constant, path.subList(1, path.size()), 1);
			}
			return StructuredOperand.NONE;
		}
		if (targetContext == DalvikTargetContext.INSTANCE) {
			StructuredOperand declaration = declarationConstantPosition(site);
			if (declaration.kind() != StructuredKind.NONE)
				return declaration;
			if (schema == DalvikOperands.HANDLE.getOperand()) {
				if (path.isEmpty() && !(site.argument() instanceof ASTArray))
					return new StructuredOperand(StructuredKind.HANDLE_SHORTCUT, null, null);
				return handlePosition(site, site.argument(), path, 0);
			}
			if (schema == DalvikOperands.CONSTANT.getOperand())
				return dalvikConstantPosition(site, site.argument(), path, 0);
			if (schema == DalvikOperands.ARGS_ARRAY.getOperand()) {
				if (path.isEmpty())
					return StructuredOperand.NONE;
				ASTElement constant = arrayValue(site.argument(), path.getFirst());
				return dalvikConstantPosition(site, constant, path.subList(1, path.size()), 1);
			}
		}
		return StructuredOperand.NONE;
	}

	/**
	 * @param site
	 * 		Operand under the caret.
	 * @param constant
	 * 		Constant value under the caret, or {@code null} if the operand is empty.
	 * @param path
	 * 		Path of indices to the constant within the operand.
	 * @param containerDepth
	 * 		Nesting depth of arrays containing the constant. Starts at 0 for the top-level operand array.
	 *
	 * @return The structured position, or {@link StructuredOperand#NONE} if the operand has no structured schema.
	 */
	@Nonnull
	private static StructuredOperand dalvikConstantPosition(@Nonnull Site site, @Nullable ASTElement constant,
	                                                        @Nonnull List<Integer> path, int containerDepth) {
		if (path.isEmpty()) {
			if (site.selectedElement() instanceof ASTString || site.selectedElement() instanceof ASTNumber)
				return StructuredOperand.NONE;
			return constant instanceof ASTArray
					? new StructuredOperand(StructuredKind.HANDLE_KIND, null, null)
					: new StructuredOperand(StructuredKind.CONSTANT_IDENTIFIER, null, null);
		}
		return handlePosition(site, constant, path, containerDepth);
	}

	/**
	 * @param site
	 * 		Operand under the caret.
	 *
	 * @return The structured position, or {@link StructuredKind#NONE} if the operand has no structured schema.
	 */
	@Nonnull
	private static StructuredKind declarationPosition(@Nonnull Site site) {
		ASTDeclaration declaration = findDeclaration(site.selectedElement());
		if (declaration == null && site.argument() instanceof ASTDeclaration direct)
			declaration = direct;

		// Check that the declaration is a valid declaration type.
		// Only enum and member declarations have structured operands.
		if (declaration == null
				|| declaration.keyword() == null
				|| (!".enum".equals(declaration.keyword().content()) && !".member".equals(declaration.keyword().content())))
			return StructuredKind.NONE;

		// Check which element of the declaration the caret is on.
		for (int i = 0; i < declaration.elements().size(); i++) {
			ASTElement element = declaration.elements().get(i);
			if (element != null && contains(element.range(), site.caret())) {
				if (i == 0)
					return StructuredKind.DECLARATION_OWNER;
				if (i == 1)
					return StructuredKind.DECLARATION_MEMBER;
				if (i == 2 && declaration.elements().size() == 3)
					return StructuredKind.DECLARATION_DESCRIPTOR;
			}
		}
		return StructuredKind.NONE;
	}

	/**
	 * @param site
	 * 		Operand under the caret.
	 *
	 * @return The structured position, or {@link StructuredOperand#NONE} if the operand has no structured schema.
	 */
	@Nonnull
	private static StructuredOperand declarationConstantPosition(@Nonnull Site site) {
		StructuredKind kind = declarationPosition(site);
		if (kind == StructuredKind.NONE)
			return StructuredOperand.NONE;

		// Check that the declaration has enough elements to resolve the structured operand.
		ASTDeclaration declaration = findDeclaration(site.selectedElement());
		if (declaration == null && site.argument() instanceof ASTDeclaration direct)
			declaration = direct;
		if (declaration == null || declaration.elements().size() < 2)
			return StructuredOperand.NONE;

		// Resolve the owner and member elements of the declaration.
		ASTElement ownerElement = declaration.elements().getFirst();
		ASTElement memberElement = declaration.elements().get(1);
		if (ownerElement == null || memberElement == null)
			return StructuredOperand.NONE;
		String owner = ownerElement.content();
		String member = memberElement.content();
		if (owner == null)
			return StructuredOperand.NONE;

		String reference = kind == StructuredKind.DECLARATION_DESCRIPTOR ? owner + "." + member : owner;
		return new StructuredOperand(kind, reference, null);
	}

	/**
	 * @param element
	 * 		AST element to check.
	 *
	 * @return The declaration the element is part of, or {@code null} if the element is not part of a declaration.
	 */
	@Nullable
	private static ASTDeclaration findDeclaration(@Nullable ASTElement element) {
		while (element != null) {
			if (element instanceof ASTDeclaration declaration)
				return declaration;
			element = element.parent();
		}
		return null;
	}

	/**
	 * @param site
	 * 		Operand under the caret.
	 * @param constant
	 * 		Constant value under the caret, or {@code null} if the operand is empty.
	 * @param path
	 * 		Path of indices to the constant within the operand.
	 * @param containerDepth
	 * 		Nesting depth of arrays containing the constant. Starts at 0 for the top-level operand array.
	 *
	 * @return The structured position, or {@link StructuredOperand#NONE} if the operand has no structured schema.
	 */
	@Nonnull
	private static StructuredOperand constantPosition(@Nonnull Site site, @Nullable ASTElement constant,
	                                                  @Nonnull List<Integer> path, int containerDepth) {
		// If the path is empty, the caret is on the constant itself, which is a constant identifier.
		if (path.isEmpty()) {
			if (site.selectedElement() instanceof ASTString || site.selectedElement() instanceof ASTNumber)
				return StructuredOperand.NONE;
			return new StructuredOperand(StructuredKind.CONSTANT_IDENTIFIER, null, null);
		}

		int index = path.getFirst();

		// Check for a handle array, which is a constant that is an array with a handle kind prefix.
		ASTArray array = constant instanceof ASTArray candidate ? candidate : null;
		boolean handleShape = possibleHandleArray(site, array, index, containerDepth);
		boolean dynamicShape = dynamicArray(site, array, index, containerDepth);
		if (handleShape)
			return handlePosition(site, array, path, containerDepth);
		if (!dynamicShape)
			return StructuredOperand.NONE;

		// A dynamic constant array is laid out as { name, type, handle, args }. For example, in JVM:
		//
		//   ldc { value, Ljava/lang/String;, { invokestatic, java/lang/invoke/ConstantBootstraps.nullConstant, ()V }, { } }
		//         ^0     ^1                  ^2 (handle, see below)                                              ^3
		if (index == 1)
			return new StructuredOperand(StructuredKind.DYNAMIC_TYPE, null, null);
		if (index == 2) {
			ASTElement handle = arrayValue(array, 2);
			if (path.size() == 1) {
				String handleText = handle == null ? site.arrayElementPrefix(containerDepth, 2) : handle.content();
				return handle instanceof ASTArray || handleText != null && handleText.startsWith("{")
						? new StructuredOperand(StructuredKind.HANDLE_KIND, null, null)
						: new StructuredOperand(StructuredKind.HANDLE_SHORTCUT, null, null);
			}
			if (handle instanceof ASTArray || handle == null)
				return handlePosition(site, handle, path.subList(1, path.size()), containerDepth + 1);
			return StructuredOperand.NONE;
		}
		if (index == 3) {
			if (path.size() == 1)
				return new StructuredOperand(StructuredKind.CONSTANT_IDENTIFIER, null, null);
			ASTElement arguments = arrayValue(array, 3);
			ASTElement nestedConstant = arrayValue(arguments, path.get(1));
			return constantPosition(site, nestedConstant, path.subList(2, path.size()), containerDepth + 2);
		}
		return StructuredOperand.NONE;
	}

	/**
	 * @param site
	 * 		Operand under the caret.
	 * @param handle
	 * 		Handle value under the caret, or {@code null} if the operand is empty.
	 * @param path
	 * 		Path of indices to the handle within the operand.
	 * @param containerDepth
	 * 		Nesting depth of arrays containing the handle. Starts at 0 for the top-level operand array.
	 *
	 * @return The structured position, or {@link StructuredOperand#NONE} if the operand has no structured schema.
	 */
	@Nonnull
	private static StructuredOperand handlePosition(@Nonnull Site site, @Nullable ASTElement handle,
	                                                @Nonnull List<Integer> path, int containerDepth) {
		// If the path is empty, the caret is on the handle itself, which is a handle kind.
		if (path.isEmpty())
			return new StructuredOperand(StructuredKind.HANDLE_KIND, null, null);

		int index = path.getFirst();

		// A handle is laid out as { kind, owner.name, descriptor }. For example, in JVM:
		//
		//   { invokestatic, java/lang/invoke/StringConcatFactory.makeConcatWithConstants, (Ljava/lang/String;)Ljava/lang/String; }
		//     ^0 kind       ^1 owner and member name                                       ^2 descriptor
		if (index == 0)
			return new StructuredOperand(StructuredKind.HANDLE_KIND, null, null);
		if (index == 1)
			return new StructuredOperand(StructuredKind.HANDLE_MEMBER, null, handleKind(site, handle, containerDepth));
		if (index == 2) {
			ASTElement member = arrayValue(handle, 1);
			String memberReference = member == null ? site.arrayElementPrefix(containerDepth, 1) : member.content();
			return new StructuredOperand(StructuredKind.HANDLE_DESCRIPTOR, memberReference,
					handleKind(site, handle, containerDepth));
		}
		return StructuredOperand.NONE;
	}

	/**
	 * @param site
	 * 		Operand under the caret.
	 * @param handle
	 * 		Handle value under the caret, or {@code null} if the operand is empty.
	 * @param containerDepth
	 * 		Nesting depth of arrays containing the handle. Starts at 0 for the top-level operand array.
	 *
	 * @return Handle kind selected by the operand, when one has been typed.
	 */
	@Nullable
	private static Handle.Kind handleKind(@Nonnull Site site, @Nullable ASTElement handle, int containerDepth) {
		ASTElement element = arrayValue(handle, 0);
		String content = element == null ? site.arrayElementPrefix(containerDepth, 0) : element.content();
		return content == null ? null : Handle.Kind.from(content);
	}

	/**
	 * @param site
	 * 		Operand under the caret.
	 * @param array
	 * 		Array value under the caret, or {@code null} if the operand is empty.
	 * @param containerDepth
	 * 		Nesting depth of arrays containing the handle. Starts at 0 for the top-level operand array.
	 *
	 * @return Text of the first element of an array operand, or {@code null} if the array is empty and no text has been typed.
	 */
	@Nullable
	private static String firstElementText(@Nonnull Site site, @Nullable ASTArray array, int containerDepth) {
		ASTElement first = arrayValue(array, 0);
		return first == null ? site.arrayElementPrefix(containerDepth, 0) : first.content();
	}

	/**
	 * @param site
	 * 		Operand under the caret.
	 * @param array
	 * 		Array value under the caret, or {@code null} if the operand is empty.
	 * @param index
	 * 		Index of the element under the caret within the array.
	 * @param containerDepth
	 * 		Nesting depth of arrays containing the handle. Starts at 0 for the top-level operand array.
	 *
	 * @return {@code true} if the array could still be a {@code handle} value, which is an array whose first element
	 * is a handle kind prefix.
	 */
	private static boolean possibleHandleArray(@Nonnull Site site, @Nullable ASTArray array, int index, int containerDepth) {
		if (array != null && array.values().size() >= 4)
			return false;

		String firstText = firstElementText(site, array, containerDepth);
		if (firstText == null || firstText.isEmpty())
			return index == 0;

		return Handle.KINDS.keySet().stream().anyMatch(kind -> kind.startsWith(firstText));
	}

	/**
	 * @param site
	 * 		Operand under the caret.
	 * @param array
	 * 		Array value under the caret, or {@code null} if the operand is empty.
	 * @param index
	 * 		Index of the element under the caret within the array.
	 * @param containerDepth
	 * 		Nesting depth of arrays containing the handle. Starts at 0 for the top-level operand array.
	 *
	 * @return {@code true} if the array could still be a {@code handle} value, which is an array whose first element
	 * is a handle kind prefix.
	 */
	private static boolean dynamicArray(@Nonnull Site site, @Nullable ASTArray array, int index, int containerDepth) {
		if (array != null && array.values().size() >= 4)
			return true;

		String firstText = firstElementText(site, array, containerDepth);
		return firstText != null
				&& !firstText.isEmpty()
				&& index > 0
				&& Handle.KINDS.keySet().stream().noneMatch(kind -> kind.startsWith(firstText));
	}

	/**
	 * @param element
	 * 		Array to get the value from.
	 * @param index
	 * 		Index of the value to get.
	 *
	 * @return Value at the index, or {@code null} if the element is not an array or the index is out of bounds.
	 */
	@Nullable
	private static ASTElement arrayValue(@Nullable ASTElement element, int index) {
		if (element instanceof ASTArray array && index >= 0 && index < array.values().size())
			return array.values().get(index);
		return null;
	}

	/**
	 * The kind of structured operand position under the caret, which selects the candidates to offer.
	 */
	enum StructuredKind {
		NONE,
		CONSTANT_IDENTIFIER,
		HANDLE_SHORTCUT,
		HANDLE_KIND,
		HANDLE_MEMBER,
		HANDLE_DESCRIPTOR,
		DYNAMIC_TYPE,
		DECLARATION_OWNER,
		DECLARATION_MEMBER,
		DECLARATION_DESCRIPTOR
	}

	/**
	 * @param kind
	 * 		Position within the structured operand.
	 * @param memberReference
	 * 		Owner or {@code owner.name} text the position refers to, when one has been typed.
	 * @param handleKind
	 * 		Handle kind selected by the operand, when one has been typed.
	 */
	record StructuredOperand(@Nonnull StructuredKind kind, @Nullable String memberReference,
	                         @Nullable Handle.Kind handleKind) {
		static final StructuredOperand NONE = new StructuredOperand(StructuredKind.NONE, null, null);
	}
}
