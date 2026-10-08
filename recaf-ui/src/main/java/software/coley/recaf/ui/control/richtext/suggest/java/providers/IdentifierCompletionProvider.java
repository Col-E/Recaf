package software.coley.recaf.ui.control.richtext.suggest.java.providers;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.ui.control.richtext.suggest.java.CompletionKind;
import software.coley.recaf.ui.control.richtext.suggest.java.JavaCompletion;
import software.coley.recaf.ui.control.richtext.suggest.java.JavaCompletionContext;
import software.coley.recaf.ui.control.richtext.suggest.java.JavaCompletionFactory;
import software.coley.recaf.ui.control.richtext.suggest.java.JavaCompletionSession;
import software.coley.recaf.ui.control.richtext.suggest.java.JavaKeywordCompletionRules;
import software.coley.recaf.ui.control.richtext.suggest.java.JavaLexicalContext;
import software.coley.recaf.ui.control.richtext.suggest.lookups.CompletionMatching;
import software.coley.recaf.ui.control.richtext.suggest.typeindex.TypeCandidate;
import software.coley.sourcesolver.model.CompilationUnitModel;
import software.coley.sourcesolver.model.MethodModel;
import software.coley.sourcesolver.model.ScopeLookup;
import software.coley.sourcesolver.model.VariableModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Provider for identifier completions. This includes local variables, fields, methods, and types.
 *
 * @author Matt Coley
 */
public final class IdentifierCompletionProvider implements JavaCompletionProvider {
	@Nonnull
	@Override
	public List<JavaCompletion> complete(@Nonnull JavaCompletionSession session, @Nonnull JavaLexicalContext context) {
		Map<String, JavaCompletion> completions = new LinkedHashMap<>(); // Linked to preserve declaration order.
		String partial = context.partialText();
		CompilationUnitModel unit = session.unit();
		int caret = session.caretPosition();
		int astPos = caret < 0 ? -1 : session.completionContext().mapCurrentPositionToAst(caret);

		// Check for local variables in scope.
		// This includes method parameters and local variables declared in the current method.
		if (unit != null && astPos >= 0) {
			for (VariableModel variable : ScopeLookup.collectVisibleVariables(unit, astPos)) {
				String variableName = variable.getName();
				boolean parameter = variable.getParent() instanceof MethodModel;
				if (!CompletionMatching.matchesPrefix(variableName, partial))
					continue;
				JavaCompletion.addOrReplace(completions, new JavaCompletion(
						CompletionKind.LOCAL,
						parameter ? variableName + " (param)" : variableName,
						variableName,
						parameter ? 0 : 1,
						null,
						variableName,
						0,
						""
				));
			}
		}

		// Check for fields and methods in the current class.
		ClassPathNode currentPath = session.currentPath();
		if (currentPath != null) {
			ClassInfo currentClass = currentPath.getValue();
			for (FieldMember field : currentClass.getFields()) {
				if (!CompletionMatching.matchesPrefix(field.getName(), partial))
					continue;
				JavaCompletion.addOrReplace(completions, new JavaCompletion(
						CompletionKind.FIELD,
						JavaCompletionFactory.displayField(field.getName(), field.getDescriptor()),
						field.getName(),
						10 + CompletionMatching.prefixPenalty(field.getName(), partial),
						currentPath.child(field),
						field.getName(),
						0,
						""
				));
			}
			for (MethodMember method : currentClass.getMethods()) {
				if (method.getName().startsWith("<"))
					continue;
				if (!CompletionMatching.matchesPrefix(method.getName(), partial))
					continue;
				JavaCompletion.addOrReplace(completions, JavaCompletionFactory.methodCompletion(
						method.getName(),
						method.getDescriptor(),
						11 + CompletionMatching.prefixPenalty(method.getName(), partial),
						currentPath.child(method),
						true
				));
			}
			for (InnerClassInfo innerClass : currentClass.getInnerClasses()) {
				String innerName = innerClass.getInnerName();
				if (innerName == null)
					continue;
				if (!Objects.equals(innerClass.getOuterClassName(), currentClass.getName()))
					continue;
				if (!CompletionMatching.matchesPrefix(innerName, partial))
					continue;
				ClassPathNode innerPath = session.workspace().findClass(innerClass.getInnerClassName());
				JavaCompletion.addOrReplace(completions, JavaCompletionFactory.typeCompletion(innerPath, innerName,
						20 + CompletionMatching.prefixPenalty(innerName, partial)));
			}
		} else {
			JavaCompletionContext.DeclaredClassInfo declaredClassInfo = session.declaredClassInfo();
			if (declaredClassInfo != null) {
				for (FieldMember field : declaredClassInfo.fields()) {
					if (!CompletionMatching.matchesPrefix(field.getName(), partial))
						continue;
					JavaCompletion.addOrReplace(completions, new JavaCompletion(
							CompletionKind.FIELD,
							JavaCompletionFactory.displayField(field.getName(), field.getDescriptor()),
							field.getName(),
							10 + CompletionMatching.prefixPenalty(field.getName(), partial),
							null,
							field.getName(),
							0,
							""
					));
				}
				for (MethodMember method : declaredClassInfo.methods()) {
					if (method.getName().startsWith("<"))
						continue;
					if (!CompletionMatching.matchesPrefix(method.getName(), partial))
						continue;
					JavaCompletion.addOrReplace(completions, JavaCompletionFactory.methodCompletion(
							method.getName(),
							method.getDescriptor(),
							11 + CompletionMatching.prefixPenalty(method.getName(), partial),
							null,
							true
					));
				}
				for (TypeCandidate innerType : declaredClassInfo.innerTypes()) {
					if (!CompletionMatching.matchesPrefix(innerType.simpleName(), partial))
						continue;
					JavaCompletion.addOrReplace(completions,
							JavaCompletionFactory.typeCompletion(innerType, 20 + CompletionMatching.prefixPenalty(innerType.simpleName(), partial)));
				}
			}
		}

		// Add visible types from the current context.
		// This includes types from the current package, imported types, and types from java.lang.
		for (TypeCandidate candidate : session.visibleTypes().values()) {
			if (!CompletionMatching.matchesPrefix(candidate.simpleName(), partial))
				continue;
			JavaCompletion.addOrReplace(completions, JavaCompletionFactory.typeCompletion(candidate,
					30 + CompletionMatching.prefixPenalty(candidate.simpleName(), partial)));
		}

		// Add keywords that match the current prefix.
		// This is a bit of a catch-all to ensure we have some suggestions even if we can't resolve any types or members.
		for (String keyword : JavaKeywordCompletionRules.getIdentifierKeywords(session, context)) {
			if (!CompletionMatching.matchesPrefix(keyword, partial))
				continue;
			JavaCompletion.addOrReplace(completions, new JavaCompletion(
					CompletionKind.KEYWORD,
					keyword,
					keyword,
					20 + CompletionMatching.prefixPenalty(keyword, partial),
					null,
					keyword,
					0,
					""
			));
		}

		return new ArrayList<>(completions.values());
	}
}
