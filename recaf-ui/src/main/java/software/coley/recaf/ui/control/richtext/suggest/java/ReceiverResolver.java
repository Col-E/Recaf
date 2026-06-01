package software.coley.recaf.ui.control.richtext.suggest.java;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.source.ResolverAdapter;
import software.coley.recaf.ui.control.richtext.suggest.java.lookups.LocalScopeLookup;
import software.coley.recaf.ui.control.richtext.suggest.java.lookups.MemberLookup;
import software.coley.recaf.ui.control.richtext.suggest.java.lookups.VisibleTypeLookup;
import software.coley.sourcesolver.model.CompilationUnitModel;
import software.coley.sourcesolver.model.VariableModel;
import software.coley.sourcesolver.resolve.entry.ArrayEntry;
import software.coley.sourcesolver.resolve.entry.ClassEntry;
import software.coley.sourcesolver.resolve.entry.DescribableEntry;
import software.coley.sourcesolver.resolve.entry.EntryPool;
import software.coley.sourcesolver.resolve.entry.FieldEntry;
import software.coley.sourcesolver.resolve.result.ClassResolution;
import software.coley.sourcesolver.resolve.result.DescribableResolution;
import software.coley.sourcesolver.resolve.result.FieldResolution;
import software.coley.sourcesolver.resolve.result.MethodResolution;
import software.coley.sourcesolver.resolve.result.Resolution;

/**
 * Resolver for the receiver of a member access expression.
 *
 * @author Matt Coley
 */
public final class ReceiverResolver {
	private final LocalScopeLookup localScopeLookup = new LocalScopeLookup();
	private final VisibleTypeLookup visibleTypeLookup = new VisibleTypeLookup();

	/**
	 * Attempt to resolve the receiver at the caret position.
	 *
	 * @param session
	 * 		Current completion session.
	 * @param resolver
	 * 		Resolver to use for any necessary resolution work.
	 * @param context
	 * 		Lexical context of the completion.
	 *
	 * @return Resolved receiver, or {@code null} if the receiver could not be resolved or there is no receiver at the caret position.
	 */
	@Nullable
	public ResolvedReceiver resolveReceiver(@Nonnull JavaCompletionSession session,
	                                        @Nonnull ResolverAdapter resolver,
	                                        @Nonnull JavaLexicalContext context) {
		EntryPool pool = resolver.getEntryPool();
		String receiverText = context.receiverText();

		// Check for instance methods on the current class via "this" or "super" keywords.
		if ("this".equals(receiverText)) {
			ClassEntry classEntry = session.currentClassEntry(pool);
			if (classEntry != null)
				return new ResolvedReceiver(classEntry, ReceiverMode.INSTANCE_ONLY);
		} else if ("super".equals(receiverText)) {
			ClassEntry classEntry = session.currentClassEntry(pool);
			if (classEntry != null && classEntry.getSuperEntry() != null)
				return new ResolvedReceiver(classEntry.getSuperEntry(), ReceiverMode.INSTANCE_ONLY);
		}

		// Check for string literals and class literals.
		else if (isStringLiteral(receiverText)) {
			ClassEntry stringEntry = pool.getClass("java/lang/String");
			if (stringEntry != null)
				return new ResolvedReceiver(stringEntry, ReceiverMode.INSTANCE_ONLY);
		} else if (receiverText.endsWith(".class")) {
			ClassEntry classLiteralEntry = pool.getClass("java/lang/Class");
			if (classLiteralEntry != null)
				return new ResolvedReceiver(classLiteralEntry, ReceiverMode.INSTANCE_ONLY);
		}

		// Check for simple identifiers that could be local variables or fields.
		CompilationUnitModel unit = session.unit();
		int astPos = context.receiverResolveOffset() >= 0 ? session.completionContext().mapCurrentPositionToAst(context.receiverResolveOffset()) : -1;
		if (unit != null && astPos >= 0 && !receiverText.isEmpty()) {
			VariableModel variable = localScopeLookup.findVisibleVariable(unit, astPos, receiverText);
			if (variable != null) {
				DescribableEntry variableType = resolveTypeFromResolution(pool, variable.getType().resolve(resolver));
				if (variableType != null)
					return new ResolvedReceiver(variableType, ReceiverMode.INSTANCE_ONLY);
			}
		}

		// Check for qualified identifiers that could be chained field accesses or static type references.
		if (!receiverText.isEmpty()) {
			ResolvedReceiver chainedReceiver = resolveQualifiedIdentifierReceiver(session, resolver, pool, context, receiverText);
			if (chainedReceiver != null)
				return chainedReceiver;

			DescribableEntry currentFieldType = resolveCurrentClassFieldType(session, pool, receiverText);
			if (currentFieldType != null)
				return new ResolvedReceiver(currentFieldType, ReceiverMode.INSTANCE_ONLY);

			TypeCandidate visibleType = visibleTypeLookup.resolveVisibleTypeReference(session, receiverText);
			if (visibleType != null) {
				ClassEntry classEntry = pool.getClass(visibleType.internalName());
				if (classEntry != null)
					return new ResolvedReceiver(classEntry, ReceiverMode.STATIC_ONLY);
			}
		}

		// As a last resort, attempt to resolve the receiver as an arbitrary expression and use its type.
		if (context.receiverResolveOffset() < 0)
			return null;
		Resolution resolution = session.completionContext().resolveRawPositionSilently(context.receiverResolveOffset());
		if (resolution == null || resolution.isUnknown())
			return null;
		DescribableEntry resolvedType = resolveTypeFromResolution(pool, resolution);
		if (resolvedType == null)
			return null;
		return new ResolvedReceiver(resolvedType, ReceiverMode.INSTANCE_ONLY);
	}

	/**
	 * Attempt to resolve a qualified identifier as a receiver, which may involve chained field accesses or static type references.
	 *
	 * @param session
	 * 		Current completion session.
	 * @param resolver
	 * 		Resolver to use for any necessary resolution work.
	 * @param pool
	 * 		Entry pool to use for looking up entries.
	 * @param context
	 * 		Lexical context of the completion.
	 * @param receiverText
	 * 		Text of the receiver to resolve, which should be a qualified identifier.
	 *
	 * @return Resolved receiver, or {@code null} if the qualified identifier could not be resolved as a receiver.
	 */
	@Nullable
	private ResolvedReceiver resolveQualifiedIdentifierReceiver(@Nonnull JavaCompletionSession session,
	                                                            @Nonnull ResolverAdapter resolver,
	                                                            @Nonnull EntryPool pool,
	                                                            @Nonnull JavaLexicalContext context,
	                                                            @Nonnull String receiverText) {
		// Skip if the receiver text doesn't look like a qualified identifier.
		if (receiverText.indexOf('.') < 0 || !isQualifiedIdentifierExpression(receiverText))
			return null;

		// Skip if the receiver text doesn't have at least two segments.
		// We want "foo.bar" for any portion of "bar". Just "foo" is not enough to know if it's a variable or type reference.
		String[] segments = receiverText.split("\\.");
		if (segments.length < 2)
			return null;

		// Resolve the first segment as a simple identifier, which could be a local variable, field, or type reference.
		ResolvedReceiver receiver = resolveSimpleIdentifierReceiver(session, resolver, pool, context, segments[0]);
		if (receiver == null)
			return null;

		// Resolve each subsequent segment as a member of the previous segment's type.
		for (int i = 1; i < segments.length; i++) {
			receiver = resolveNamedMemberReceiver(session, pool, receiver, segments[i]);
			if (receiver == null)
				return null;
		}

		return receiver;
	}

	/**
	 * Attempt to resolve a simple identifier as a receiver, which could be a local variable, field, or type reference.
	 *
	 * @param session
	 * 		Current completion session.
	 * @param resolver
	 * 		Resolver to use for any necessary resolution work.
	 * @param pool
	 * 		Entry pool to use for looking up entries.
	 * @param context
	 * 		Lexical context of the completion.
	 * @param receiverText
	 * 		Text of the receiver to resolve, which should be a simple identifier.
	 *
	 * @return Resolved receiver, or {@code null} if the simple identifier could not be resolved as a receiver.
	 */
	@Nullable
	private ResolvedReceiver resolveSimpleIdentifierReceiver(@Nonnull JavaCompletionSession session,
	                                                         @Nonnull ResolverAdapter resolver,
	                                                         @Nonnull EntryPool pool,
	                                                         @Nonnull JavaLexicalContext context,
	                                                         @Nonnull String receiverText) {
		// Check if the identifier is a visible local variable.
		CompilationUnitModel unit = session.unit();
		int astPos = context.receiverResolveOffset() >= 0 ? session.completionContext().mapCurrentPositionToAst(context.receiverResolveOffset()) : -1;
		if (unit != null && astPos >= 0 && !receiverText.isEmpty()) {
			VariableModel variable = localScopeLookup.findVisibleVariable(unit, astPos, receiverText);
			if (variable != null) {
				DescribableEntry variableType = resolveTypeFromResolution(pool, variable.getType().resolve(resolver));
				if (variableType != null)
					return new ResolvedReceiver(variableType, ReceiverMode.INSTANCE_ONLY);
			}
		}

		// Check if the identifier is a field of the current class.
		DescribableEntry currentFieldType = resolveCurrentClassFieldType(session, pool, receiverText);
		if (currentFieldType != null)
			return new ResolvedReceiver(currentFieldType, ReceiverMode.INSTANCE_ONLY);

		// Check if the identifier is a visible type reference.
		TypeCandidate visibleType = visibleTypeLookup.resolveVisibleTypeReference(session, receiverText);
		if (visibleType != null) {
			ClassEntry classEntry = pool.getClass(visibleType.internalName());
			if (classEntry != null)
				return new ResolvedReceiver(classEntry, ReceiverMode.STATIC_ONLY);
		}

		return null;
	}

	/**
	 * Attempt to resolve a named member of a receiver, which could be a field or method.
	 * This is used for resolving chained member accesses in qualified identifiers.
	 *
	 * @param session
	 * 		Current completion session.
	 * @param pool
	 * 		Entry pool to use for looking up entries.
	 * @param receiver
	 * 		Receiver whose member is being accessed.
	 * @param memberName
	 * 		Name of the member being accessed.
	 *
	 * @return Resolved receiver for the member, or {@code null} if the member could not be resolved as a receiver.
	 */
	@Nullable
	private ResolvedReceiver resolveNamedMemberReceiver(@Nonnull JavaCompletionSession session,
	                                                    @Nonnull EntryPool pool,
	                                                    @Nonnull ResolvedReceiver receiver,
	                                                    @Nonnull String memberName) {
		// Handle array length as a special case since it's not a real member.
		if (receiver.type() instanceof ArrayEntry) {
			if ("length".equals(memberName) && receiver.mode() != ReceiverMode.STATIC_ONLY) {
				DescribableEntry intEntry = pool.getDescribable("I");
				if (intEntry != null)
					return new ResolvedReceiver(intEntry, ReceiverMode.INSTANCE_ONLY);
			}
			return null;
		}

		// Skip if the receiver type does not have a backing class entry, since only class types can have members.
		if (!(receiver.type() instanceof ClassEntry classEntry))
			return null;

		// Look for a visible field in the received class with the given name.
		FieldEntry field = MemberLookup.findVisibleField(classEntry, session.currentClassEntry(pool), memberName,
				session.currentPackageName(), receiver.mode());
		if (field == null)
			return null;

		// Resolve the field's type and return it as the new receiver.
		DescribableEntry fieldType = pool.getDescribable(field.getDescriptor());
		if (fieldType == null)
			return null;
		return new ResolvedReceiver(fieldType, ReceiverMode.INSTANCE_ONLY);
	}

	/**
	 * Resolve the type of a resolution result, which may be a method return type, field type, class type, or describable type.
	 *
	 * @param pool
	 * 		Entry pool to use for looking up entries.
	 * @param resolution
	 * 		Resolution result to resolve the type of.
	 *
	 * @return Describable entry representing the type of the resolution, or {@code null} if the type could not be resolved.
	 */
	@Nullable
	private static DescribableEntry resolveTypeFromResolution(@Nonnull EntryPool pool, @Nonnull Resolution resolution) {
		return switch (resolution) {
			case MethodResolution methodResolution ->
					pool.getDescribable(methodResolution.getMethodEntry().getReturnDescriptor());
			case FieldResolution fieldResolution ->
					pool.getDescribable(fieldResolution.getFieldEntry().getDescriptor());
			case ClassResolution classResolution -> classResolution.getClassEntry();
			case DescribableResolution describableResolution -> describableResolution.getDescribableEntry();
			default -> null;
		};
	}

	/**
	 * Attempt to resolve a simple identifier as a field of the current class, which may be accessible via the current path or declared class info.
	 *
	 * @param session
	 * 		Current completion session.
	 * @param pool
	 * 		Entry pool to use for looking up entries.
	 * @param name
	 * 		Name of the field to resolve.
	 *
	 * @return Describable entry representing the type of the field, or {@code null} if no such field could be found in the current class.
	 */
	@Nullable
	private static DescribableEntry resolveCurrentClassFieldType(@Nonnull JavaCompletionSession session,
	                                                             @Nonnull EntryPool pool,
	                                                             @Nonnull String name) {
		// Look in the current class via the path for matching fields.
		ClassPathNode currentPath = session.currentPath();
		if (currentPath != null) {
			FieldMember field = currentPath.getValue().getFirstDeclaredFieldByName(name);
			if (field == null)
				return null;
			return pool.getDescribable(field.getDescriptor());
		}

		// Try again with the declared class info model if that is present.
		JavaCompletionContext.DeclaredClassInfo declaredClassInfo = session.declaredClassInfo();
		if (declaredClassInfo == null)
			return null;
		for (FieldMember field : declaredClassInfo.fields())
			if (field.getName().equals(name))
				return pool.getDescribable(field.getDescriptor());
		return null;
	}

	/**
	 * @param receiverText
	 * 		Text to check, which should be the receiver portion of a member access expression.
	 *
	 * @return {@code true} if the text looks like a qualified identifier expression, or {@code false} if it doesn't.
	 */
	private static boolean isQualifiedIdentifierExpression(@Nonnull String receiverText) {
		// The receiver text must be non-empty and cannot start or end with a dot.
		if (receiverText.isEmpty() || receiverText.startsWith(".") || receiverText.endsWith("."))
			return false;

		// The receiver text must consist of valid Java identifier parts and dots, and cannot have consecutive dots.
		boolean expectIdentifier = true;
		for (int i = 0; i < receiverText.length(); i++) {
			char c = receiverText.charAt(i);
			if (c == '.') {
				if (expectIdentifier)
					return false;
				expectIdentifier = true;
			} else if (Character.isJavaIdentifierPart(c) || c == '$') {
				expectIdentifier = false;
			} else {
				return false;
			}
		}
		return !expectIdentifier;
	}

	/**
	 * @param receiverText
	 * 		Text to check, which should be the receiver portion of a member access expression.
	 *
	 * @return {@code true} if the text looks like a string literal, or {@code false} if it doesn't.
	 */
	private static boolean isStringLiteral(@Nonnull String receiverText) {
		return receiverText.length() >= 2 &&
				receiverText.charAt(0) == '"' &&
				receiverText.charAt(receiverText.length() - 1) == '"';
	}
}
