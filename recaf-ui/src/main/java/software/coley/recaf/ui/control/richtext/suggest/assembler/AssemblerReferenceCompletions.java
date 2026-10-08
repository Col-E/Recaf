package software.coley.recaf.ui.control.richtext.suggest.assembler;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.assembler.backend.dalvik.DalvikTargetContext;
import me.darknet.assembler.helper.Handle;
import me.darknet.assembler.target.TargetContext;
import me.darknet.assembler.util.EscapeUtil;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.inheritance.InheritanceVertex;
import software.coley.recaf.ui.control.richtext.suggest.assembler.AssemblerOperandLocator.Site;
import software.coley.recaf.ui.control.richtext.suggest.lookups.MemberVisibility;
import software.coley.recaf.ui.control.richtext.suggest.lookups.TypeVisibility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Completions for reference operands: owner types, then the member names and descriptors that follow an owner.
 * <p>
 * Each entry point returns a {@link Result}, which the tab completer turns into its operand context.
 *
 * @author Matt Coley
 */
final class AssemblerReferenceCompletions {
	private final TargetContext targetContext;

	/**
	 * @param targetContext
	 * 		Instruction language of the edited class. Decides which member kinds an instruction can reference.
	 */
	AssemblerReferenceCompletions(@Nonnull TargetContext targetContext) {
		this.targetContext = targetContext;
	}

	/**
	 * Completes the name or descriptor of a field declared with {@code .enum} or {@code .member}.
	 *
	 * @param scope
	 * 		Completion scope for the request.
	 * @param site
	 * 		Operand under the caret.
	 * @param prefix
	 * 		Typed text.
	 * @param memberReference
	 * 		Declared owner, or {@code owner.name} when completing the descriptor.
	 * @param descriptorOnly
	 *        {@code true} if the completion is only for the descriptor.
	 *        {@code false} if the completion is for the name and descriptor.
	 *
	 * @return Field completions for the position.
	 */
	@Nonnull
	Result declarationField(@Nonnull AssemblerCompletionScope scope, @Nonnull Site site, @Nonnull String prefix,
	                        @Nullable String memberReference,
	                        boolean descriptorOnly) {
		String owner;
		String name;
		String descriptor;
		String matchedText;
		String partialInput;
		if (descriptorOnly) {
			if (memberReference == null)
				return new Result(prefix, List.of());
			int separator = memberReference.lastIndexOf('.');
			if (separator <= 0 || separator == memberReference.length() - 1)
				return new Result(prefix, List.of());
			owner = memberReference.substring(0, separator);
			name = memberReference.substring(separator + 1);
			descriptor = prefix;
			matchedText = prefix;
			partialInput = site.linePrefix().trim();
		} else {
			owner = memberReference;
			if (owner == null)
				return new Result(prefix, List.of());
			name = prefix.isEmpty() ? null : prefix;
			descriptor = null;
			matchedText = prefix;
			partialInput = name == null ? owner + "." : site.linePrefix().trim();
		}

		ReferenceContext reference = new CodeFieldContext(
				scope, partialInput, matchedText, owner, name, descriptor, descriptorOnly, false);
		return new Result(matchedText, reference.complete());
	}

	/**
	 * Completes the operand of a handle, where the handle kind decides between a field and a method.
	 *
	 * @param site
	 * 		Operand under the caret.
	 * @param tokenPrefix
	 * 		Typed text.
	 * @param memberReference
	 * 		Owner and name of the handle, when completing its descriptor. {@code null} otherwise.
	 * @param kind
	 * 		Handle kind selected earlier in the operand, or {@code null} if none has been selected.
	 * @param descriptorOnly
	 *        {@code true} if the completion is only for the descriptor.
	 *        {@code false} if the completion is for the name and descriptor.
	 *
	 * @return Member completions for the position.
	 */
	@Nonnull
	Result handleReference(@Nonnull AssemblerCompletionScope scope, @Nonnull Site site, @Nonnull String tokenPrefix,
	                       @Nullable String memberReference,
	                       @Nullable Handle.Kind kind, boolean descriptorOnly) {
		if (kind == null)
			return new Result(tokenPrefix, List.of());

		String owner;
		String name;
		String descriptor;
		String matchedText;
		if (descriptorOnly) {
			if (memberReference == null)
				return new Result(tokenPrefix, List.of());
			int separator = memberReference.lastIndexOf('.');
			if (separator <= 0 || separator == memberReference.length() - 1)
				return new Result(tokenPrefix, List.of());
			owner = memberReference.substring(0, separator);
			name = memberReference.substring(separator + 1);
			descriptor = tokenPrefix;
			matchedText = tokenPrefix;
		} else {
			ReferenceToken token = ReferenceToken.split(tokenPrefix);
			owner = token.owner();
			name = token.name();
			matchedText = token.matchedText();
			descriptor = null;
		}

		String linePrefix = site.linePrefix().trim();
		ReferenceContext reference = kind.isField()
				? new CodeFieldContext(scope, linePrefix, matchedText, owner, name, descriptor, descriptorOnly)
				: new CodeMethodContext(scope, linePrefix, matchedText, owner, name, descriptor, descriptorOnly);
		return new Result(matchedText, reference.complete());
	}

	/**
	 * Completes a member reference argument, such as the owner and name of a {@code getfield} or {@code invokevirtual},
	 * or the descriptor that follows an owner and name written in an earlier operand.
	 *
	 * @param site
	 * 		Operand under the caret.
	 * @param memberIndex
	 * 		Operand index holding the member reference.
	 * @param isMethod
	 *        {@code true} if the member is a method, {@code false} if it is a field.
	 * @param tokenPrefix
	 * 		Typed text.
	 *
	 * @return Member completions for the position.
	 */
	@Nonnull
	Result memberReference(@Nonnull AssemblerCompletionScope scope, @Nonnull Site site, int memberIndex,
	                       boolean isMethod, @Nonnull String tokenPrefix) {
		String owner;
		String name;
		String descriptor;
		String matchedText;
		boolean descriptorOnly;
		if (site.argumentIndex() == memberIndex) {
			ReferenceToken token = ReferenceToken.split(tokenPrefix);
			owner = token.owner();
			name = token.name();
			matchedText = token.matchedText();
			descriptor = null;
			descriptorOnly = false;
		} else {
			String memberPath = site.argumentText(memberIndex);
			if (memberPath == null)
				return new Result(tokenPrefix, List.of());
			int separator = memberPath.lastIndexOf('.');
			if (separator <= 0 || separator == memberPath.length() - 1)
				return new Result(tokenPrefix, List.of());
			owner = memberPath.substring(0, separator);
			name = memberPath.substring(separator + 1);
			descriptor = tokenPrefix;
			matchedText = tokenPrefix;
			descriptorOnly = true;
		}

		String linePrefix = site.linePrefix().trim();
		ReferenceContext reference = isMethod
				? new CodeMethodContext(scope, linePrefix, matchedText, owner, name, descriptor, descriptorOnly)
				: new CodeFieldContext(scope, linePrefix, matchedText, owner, name, descriptor, descriptorOnly);
		return new Result(matchedText, reference.complete());
	}

	/**
	 * Completions for a reference operand, and the text the popup replaces at the caret.
	 *
	 * @param matchedText
	 * 		Text replaced by the completion.
	 * @param candidates
	 * 		Completions to offer.
	 */
	record Result(@Nonnull String matchedText, @Nonnull List<AssemblerCompletion> candidates) {}

	/**
	 * A reference token split at its last {@code .}, such as {@code java/lang/String.len}.
	 *
	 * @param owner
	 * 		Text before the separator. The whole token when there is no separator.
	 * @param name
	 * 		Text after the separator, or {@code null} if that text is empty or absent.
	 * @param matchedText
	 * 		Text the popup replaces: the whole token without a separator, otherwise the text after it.
	 */
	private record ReferenceToken(@Nonnull String owner, @Nullable String name, @Nonnull String matchedText) {
		@Nonnull
		static ReferenceToken split(@Nonnull String token) {
			int separator = token.lastIndexOf('.');
			if (separator < 0)
				return new ReferenceToken(token, null, token);
			String namePrefix = token.substring(separator + 1);
			return new ReferenceToken(token.substring(0, separator), namePrefix.isEmpty() ? null : namePrefix, namePrefix);
		}
	}

	/**
	 * The user is typing a field reference. Offers the parts of the reference that the text so far allows.
	 */
	private class CodeFieldContext extends ReferenceContext {
		private final boolean filterStatic;

		CodeFieldContext(@Nonnull AssemblerCompletionScope scope, @Nonnull String partialInput,
		                 @Nonnull String matchedText, @Nullable String owner,
		                 @Nullable String name, @Nullable String desc, boolean descriptorOnly) {
			this(scope, partialInput, matchedText, owner, name, desc, descriptorOnly, true);
		}

		CodeFieldContext(@Nonnull AssemblerCompletionScope scope, @Nonnull String partialInput,
		                 @Nonnull String matchedText, @Nullable String owner,
		                 @Nullable String name, @Nullable String desc, boolean descriptorOnly, boolean filterStatic) {
			super(scope, partialInput, matchedText, owner, name, desc, descriptorOnly);
			this.filterStatic = filterStatic;
		}

		@Nonnull
		@Override
		public List<AssemblerCompletion> complete() {
			boolean isDalvik = targetContext == DalvikTargetContext.INSTANCE;
			boolean isStatic = isDalvik
					? partialInput.contains("sget") || partialInput.contains("sput")
					|| partialInput.contains("getstatic") || partialInput.contains("putstatic")
					: partialInput.contains("getstatic") || partialInput.contains("putstatic");
			return complete(c -> filterStatic
					? c.fieldStream().filter(member -> isStatic == member.hasStaticModifier())
					: c.fieldStream());
		}
	}

	/**
	 * The user is typing a method reference. Offers the parts of the reference that the text so far allows.
	 */
	private class CodeMethodContext extends ReferenceContext {
		CodeMethodContext(@Nonnull AssemblerCompletionScope scope, @Nonnull String partialInput,
		                  @Nonnull String matchedText, @Nullable String owner,
		                  @Nullable String name, @Nullable String desc, boolean descriptorOnly) {
			super(scope, partialInput, matchedText, owner, name, desc, descriptorOnly);
		}

		@Nonnull
		@Override
		public List<AssemblerCompletion> complete() {
			boolean isDalvik = targetContext == DalvikTargetContext.INSTANCE;
			boolean isStatic = isDalvik
					? partialInput.contains("invoke-static") || partialInput.contains("invokestatic")
					: partialInput.contains("invokestatic");
			boolean isSpecial = isDalvik
					? partialInput.contains("invoke-direct") || partialInput.contains("invokespecial")
					: partialInput.contains("invokespecial");
			return complete(c -> c.methodStream().filter(m -> {
				// Only invokespecial/invoke-direct can be used to call constructors.
				if (m.getName().startsWith("<") && !isSpecial)
					return false;

				// Limit by static matching
				return isStatic == m.hasStaticModifier();
			}));
		}
	}

	/**
	 * Base for reference completion. Holds the reference text typed so far, split into owner, name, and descriptor.
	 */
	private abstract static class ReferenceContext {
		protected final AssemblerCompletionScope scope;
		protected final String partialInput;
		protected final String matchedText;
		protected final String owner;
		protected final String name;
		protected final String desc;
		protected final boolean descriptorOnly;

		/**
		 * @param partialInput
		 * 		Trimmed text of the instruction up to the caret. Used to infer the instruction, such as
		 *        {@code getstatic} or {@code invokespecial}, which narrows the members offered.
		 * @param matchedText
		 * 		Text the completion popup replaces at the caret.
		 * @param owner
		 * 		Owner type text. May be partial. {@code null} if there is no owner to complete from.
		 * @param name
		 * 		Member name text. May be partial. {@code null} if no name has been typed after the owner.
		 * @param desc
		 * 		Member descriptor text. May be partial. {@code null} if the descriptor is not being completed.
		 * @param descriptorOnly
		 *        {@code true} if the completion is only for the descriptor, {@code false} if the completion is for the name and descriptor.
		 * 		Generally {@code true} when the caret is in a later operand, and {@code false} when the caret is in the same operand as the owner and name.
		 */
		ReferenceContext(@Nonnull AssemblerCompletionScope scope, @Nonnull String partialInput,
		                 @Nonnull String matchedText, @Nullable String owner,
		                 @Nullable String name, @Nullable String desc, boolean descriptorOnly) {
			this.scope = scope;
			this.partialInput = partialInput;
			this.matchedText = matchedText;
			this.owner = owner;
			this.name = name;
			this.desc = desc;
			this.descriptorOnly = descriptorOnly;
		}

		/**
		 * @return Completions for the reference at its current step.
		 */
		@Nonnull
		abstract List<AssemblerCompletion> complete();

		/**
		 * Resolves completions for the reference, choosing the step based on which parts have been typed:
		 * <ul>
		 *     <li>{@code owner} alone: class names starting with the owner text.</li>
		 *     <li>{@code owner.}: every member of the owner, including inherited members.</li>
		 *     <li>{@code owner.name}: members whose name starts with {@code name}.</li>
		 *     <li>{@code owner.name desc}: members whose name is {@code name} and descriptor starts with {@code desc}.</li>
		 * </ul>
		 *
		 * @param classMemberLookup
		 * 		Selects the members to offer from a class, such as fields or methods.
		 *
		 * @return Completions for the current step, or an empty list if the owner type is unknown.
		 */
		@Nonnull
		protected List<AssemblerCompletion> complete(@Nonnull Function<ClassInfo, Stream<? extends ClassMember>> classMemberLookup) {
			if (owner == null)
				return Collections.emptyList();

			if (name == null && desc == null) {
				// TODO: startsWith is nice, but mirroring IntelliJ would be nicer
				//  - Should be able to complete "new Strin" to "new java/lang/String"
				//    - But out completion system just appends text so we'd need to do some more work there.
				//  - Also like IntelliJ we should prioritize common completions over niche ones
				//    - Also not something that happens right here
				if (partialInput.endsWith(".")) {
					ClassPathNode ownerPath = scope.workspace().findClass(owner);
					if (ownerPath != null)
						return memberCompletions(ownerPath, classMemberLookup, member -> true);
				} else {
					// No separator, user is still typing out a class name.
					return scope.typesWithPrefix(owner).stream()
							.filter(c -> TypeVisibility.isAccessibleType(c, scope.currentPackage()))
							.map(c -> new AssemblerCompletion.Type(Objects.requireNonNull(c.path()), EscapeUtil.escapeLiteral(
									AssemblerTypeCompletions.classTypeText(c.internalName(), owner, false, false))))
							.filter(type -> type.text().startsWith(matchedText) && !type.text().equals(matchedText))
							.map(AssemblerCompletion.class::cast)
							.toList();
				}
			}

			if (name != null && desc == null) {
				ClassPathNode ownerPath = scope.workspace().findClass(owner);
				if (ownerPath != null)
					return memberCompletions(ownerPath, classMemberLookup, member -> member.getName().startsWith(name));
			}

			if (desc != null) {
				ClassPathNode ownerPath = scope.workspace().findClass(owner);
				if (ownerPath != null)
					return memberCompletions(ownerPath, classMemberLookup,
							member -> member.getName().equals(name) && member.getDescriptor().startsWith(desc));
			}

			return Collections.emptyList();
		}

		/**
		 * @param ownerPath
		 * 		Path to the owner class of the reference.
		 * @param classMemberLookup
		 * 		Selects the members to consider from a class.
		 * @param filter
		 * 		Predicate members must satisfy to be offered.
		 *
		 * @return Completions for matching members declared by the owner or any of its parents.
		 */
		@Nonnull
		private List<AssemblerCompletion> memberCompletions(@Nonnull ClassPathNode ownerPath,
		                                                    @Nonnull Function<ClassInfo, Stream<? extends ClassMember>> classMemberLookup,
		                                                    @Nonnull Predicate<ClassMember> filter) {
			if (owner == null)
				return Collections.emptyList();

			// Gather all classes to consider for members, starting with the owner and including all parents.
			List<ClassInfo> classes = new ArrayList<>();
			classes.add(ownerPath.getValue());
			InheritanceVertex vertex = scope.graph().getVertex(owner);
			if (vertex != null)
				for (InheritanceVertex parent : vertex.getAllParents())
					classes.add(parent.getValue());

			// Gather all members from the classes, filtering out duplicates and those that are not visible from the edited class.
			List<ClassMember> items = new ArrayList<>();
			Set<String> seen = new HashSet<>();
			for (ClassInfo info : classes) {
				boolean declared = info.getName().equals(scope.className());
				boolean samePackage = Objects.equals(scope.currentPackage(),
						AssemblerCompletionScope.packageOfInternalName(info.getName()));
				classMemberLookup.apply(info).filter(filter).forEach(member -> {
					MemberVisibility.Access access = MemberVisibility.Access.fromFlags(member.getAccess());
					boolean visible = MemberVisibility.isVisible(access, declared, samePackage,
							() -> scope.className() != null
									&& scope.isSameOrSubclass(scope.className(), info.getName())
									&& scope.isSameOrSubclass(owner, scope.className()));
					if (visible && seen.add(member.getName() + member.getDescriptor()))
						items.add(member);
				});
			}
			return items.stream()
					.map(ownerPath::child)
					.map(this::memberCompletion)
					.map(AssemblerCompletion.class::cast)
					.toList();
		}

		/**
		 * @param path
		 * 		Path to the member to complete.
		 *
		 * @return Completion for the member, as a bare descriptor when only a descriptor is being completed,
		 * otherwise as {@code name descriptor}.
		 */
		@Nonnull
		private AssemblerCompletion.Member memberCompletion(@Nonnull ClassMemberPathNode path) {
			ClassMember member = path.getValue();
			String text = descriptorOnly
					? EscapeUtil.escapeLiteral(member.getDescriptor())
					: EscapeUtil.escapeLiteral(member.getName()) + " " + EscapeUtil.escapeLiteral(member.getDescriptor());
			return new AssemblerCompletion.Member(path, text);
		}
	}
}
