package software.coley.recaf.ui.control.richtext.suggest.java.lookups;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.ui.control.richtext.suggest.java.CompletionKind;
import software.coley.recaf.ui.control.richtext.suggest.java.JavaCompletion;
import software.coley.recaf.ui.control.richtext.suggest.java.JavaCompletionFactory;
import software.coley.recaf.ui.control.richtext.suggest.java.JavaCompletionSession;
import software.coley.recaf.ui.control.richtext.suggest.java.ReceiverMode;
import software.coley.sourcesolver.resolve.entry.ClassEntry;
import software.coley.sourcesolver.resolve.entry.FieldEntry;
import software.coley.sourcesolver.resolve.entry.MemberEntry;
import software.coley.sourcesolver.resolve.entry.MethodEntry;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Lookup for class members.
 *
 * @author Matt Coley
 */
public final class MemberLookup {
	/**
	 * Collect member completions for the given class and its super types.
	 *
	 * @param session
	 * 		Session to pull data from, like the workspace and type index.
	 * @param completions
	 * 		Map to add completions to. Completions will be added or replaced based on their name.
	 * @param classEntry
	 * 		Class to collect members from.
	 * @param partial
	 * 		Partial text to match member names against.
	 * @param methodsOnly
	 *        {@code true} to only include methods, {@code false} to include fields as well.
	 * @param inheritedOnly
	 *        {@code true} to only include inherited members, {@code false} to include declared members as well.
	 * @param currentPackage
	 * 		Current package name, used to determine visibility of package-private members.
	 * @param receiverMode
	 * 		Receiver mode to filter members by static/instance.
	 * @param insertMethodCall
	 *        {@code true} to include method call syntax in method completions, {@code false} for simple method name.
	 * 		This is typically {@code false} when completing method references, and {@code true} for normal method calls.
	 */
	public void collectMemberCompletions(@Nonnull JavaCompletionSession session,
	                                     @Nonnull Map<String, JavaCompletion> completions,
	                                     @Nonnull ClassEntry classEntry,
	                                     @Nullable ClassEntry currentClassEntry,
	                                     @Nonnull String partial,
	                                     boolean methodsOnly,
	                                     boolean inheritedOnly,
	                                     @Nullable String currentPackage,
	                                     @Nonnull ReceiverMode receiverMode,
	                                     boolean insertMethodCall) {
		Set<String> visited = new HashSet<>();
		collectMemberCompletions(session, completions, classEntry, partial, methodsOnly, inheritedOnly, currentPackage,
				receiverMode, insertMethodCall, classEntry, currentClassEntry, visited, 0);
	}

	private void collectMemberCompletions(@Nonnull JavaCompletionSession session,
	                                      @Nonnull Map<String, JavaCompletion> completions,
	                                      @Nonnull ClassEntry classEntry,
	                                      @Nonnull String partial,
	                                      boolean methodsOnly,
	                                      boolean inheritedOnly,
	                                      @Nullable String currentPackage,
	                                      @Nonnull ReceiverMode receiverMode,
	                                      boolean insertMethodCall,
	                                      @Nonnull ClassEntry receiverClassEntry,
	                                      @Nullable ClassEntry currentClassEntry,
	                                      @Nonnull Set<String> visited,
	                                      int depth) {
		if (!visited.add(classEntry.getName()))
			return;

		boolean declared = depth == 0 && !inheritedOnly;
		ClassPathNode ownerPath = session.workspace().findClass(classEntry.getName());

		if (!methodsOnly) {
			for (FieldEntry field : classEntry.getDeclaredFields()) {
				if (!matchesReceiverMode(field, receiverMode))
					continue;
				if (!isVisibleMember(field, classEntry, receiverClassEntry, currentClassEntry, currentPackage))
					continue;
				String name = field.getName();
				if (!JavaCompletionFactory.matchesPrefix(name, partial))
					continue;
				ClassMemberPathNode memberPath = ownerPath == null ? null : ownerPath.child(name, field.getDescriptor());
				JavaCompletion.addOrReplace(completions, new JavaCompletion(
						CompletionKind.FIELD,
						JavaCompletionFactory.displayField(name, field.getDescriptor()),
						name,
						(declared ? 0 : 10) + JavaCompletionFactory.prefixPenalty(name, partial),
						memberPath,
						name,
						0,
						""
				));
			}
		}

		for (MethodEntry method : classEntry.getDeclaredMethods()) {
			String name = method.getName();
			if (name.startsWith("<"))
				continue;
			if (!matchesReceiverMode(method, receiverMode))
				continue;
			if (!isVisibleMember(method, classEntry, receiverClassEntry, currentClassEntry, currentPackage))
				continue;
			if (!JavaCompletionFactory.matchesPrefix(name, partial))
				continue;
			ClassMemberPathNode memberPath = ownerPath == null ? null : ownerPath.child(name, method.getDescriptor());
			JavaCompletion.addOrReplace(completions, JavaCompletionFactory.methodCompletion(
					name,
					method.getDescriptor(),
					(declared ? 1 : 11) + JavaCompletionFactory.prefixPenalty(name, partial),
					memberPath,
					insertMethodCall
			));
		}

		ClassEntry superEntry = classEntry.getSuperEntry();
		if (superEntry != null)
			collectMemberCompletions(session, completions, superEntry, partial, methodsOnly, false, currentPackage,
					receiverMode, insertMethodCall, receiverClassEntry, currentClassEntry, visited, depth + 1);
		for (ClassEntry implementedEntry : classEntry.getImplementedEntries())
			collectMemberCompletions(session, completions, implementedEntry, partial, methodsOnly, false, currentPackage,
					receiverMode, insertMethodCall, receiverClassEntry, currentClassEntry, visited, depth + 1);
	}

	/**
	 * Find a visible field with the given name in the class or its super types.
	 *
	 * @param classEntry
	 * 		Class to start the search from.
	 * @param fieldName
	 * 		Name of the field to find.
	 * @param currentPackage
	 * 		Current package name, used to determine visibility of package-private members.
	 * @param receiverMode
	 * 		Receiver mode to filter members by static/instance.
	 *
	 * @return A visible field entry with the given name, or {@code null} if no such field is found.
	 */
	@Nullable
	public static FieldEntry findVisibleField(@Nonnull ClassEntry classEntry,
	                                          @Nullable ClassEntry currentClassEntry,
	                                          @Nonnull String fieldName,
	                                          @Nullable String currentPackage,
	                                          @Nonnull ReceiverMode receiverMode) {
		return findVisibleField(classEntry, classEntry, currentClassEntry, fieldName, currentPackage,
				receiverMode, new HashSet<>());
	}

	@Nullable
	private static FieldEntry findVisibleField(@Nonnull ClassEntry ownerClassEntry,
	                                           @Nonnull ClassEntry receiverClassEntry,
	                                           @Nullable ClassEntry currentClassEntry,
	                                           @Nonnull String fieldName,
	                                           @Nullable String currentPackage,
	                                           @Nonnull ReceiverMode receiverMode,
	                                           @Nonnull Set<String> visited) {
		// Skip if we've already visited this class, to prevent infinite loops in case of junk classes with circular inheritance.
		if (!visited.add(ownerClassEntry.getName()))
			return null;

		// Check for any matching declared fields in this class.
		for (FieldEntry field : ownerClassEntry.getDeclaredFields()) {
			if (!fieldName.equals(field.getName()))
				continue;
			if (!matchesReceiverMode(field, receiverMode))
				continue;
			if (isVisibleMember(field, ownerClassEntry, receiverClassEntry, currentClassEntry, currentPackage))
				return field;
		}

		// If not found, check super types.
		ClassEntry superEntry = ownerClassEntry.getSuperEntry();
		if (superEntry != null) {
			FieldEntry inherited = findVisibleField(superEntry, receiverClassEntry, currentClassEntry, fieldName,
					currentPackage, receiverMode, visited);
			if (inherited != null)
				return inherited;
		}
		for (ClassEntry implementedEntry : ownerClassEntry.getImplementedEntries()) {
			FieldEntry inherited = findVisibleField(implementedEntry, receiverClassEntry, currentClassEntry, fieldName,
					currentPackage, receiverMode, visited);
			if (inherited != null)
				return inherited;
		}

		return null;
	}

	private static boolean matchesReceiverMode(@Nonnull MemberEntry member, @Nonnull ReceiverMode receiverMode) {
		return switch (receiverMode) {
			case ANY -> true;
			case STATIC_ONLY -> member.isStatic();
			case INSTANCE_ONLY -> !member.isStatic();
		};
	}

	private static boolean isVisibleMember(@Nonnull MemberEntry member,
	                                       @Nonnull ClassEntry ownerClassEntry,
	                                       @Nonnull ClassEntry receiverClassEntry,
	                                       @Nullable ClassEntry currentClassEntry,
	                                       @Nullable String currentPackage) {
		// Public members are always visible.
		if (member.isPublic())
			return true;

		// Declared members in the current class are always visible, even if they are private.
		if (currentClassEntry != null && ownerClassEntry.getName().equals(currentClassEntry.getName()))
			return true;

		// If it's not in the same class and private, not visible.
		if (member.isPrivate())
			return false;

		// If they're not in the same package, and it's package-private, not visible.
		if (Objects.equals(currentPackage, ownerClassEntry.getPackageName()))
			return true;

		// Protected members are visible to subclasses, but only if the receiver type is a subclass of the current class.
		if (!member.isProtected() || currentClassEntry == null)
			return false;
		return isSameOrSubclass(currentClassEntry, ownerClassEntry) &&
				isSameOrSubclass(receiverClassEntry, currentClassEntry);
	}

	private static boolean isSameOrSubclass(@Nonnull ClassEntry candidate, @Nonnull ClassEntry target) {
		return isSameOrSubclass(candidate, target, new HashSet<>());
	}

	private static boolean isSameOrSubclass(@Nonnull ClassEntry candidate,
	                                        @Nonnull ClassEntry target,
	                                        @Nonnull Set<String> visited) {
		// Skip if visited.
		if (!visited.add(candidate.getName()))
			return false;

		// Check for match.
		if (candidate.getName().equals(target.getName()))
			return true;

		// Check super types.
		ClassEntry superEntry = candidate.getSuperEntry();
		if (superEntry != null && isSameOrSubclass(superEntry, target, visited))
			return true;
		for (ClassEntry implementedEntry : candidate.getImplementedEntries())
			if (isSameOrSubclass(implementedEntry, target, visited))
				return true;
		return false;
	}
}
