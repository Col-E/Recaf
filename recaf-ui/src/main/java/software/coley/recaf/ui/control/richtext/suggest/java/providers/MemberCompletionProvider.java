package software.coley.recaf.ui.control.richtext.suggest.java.providers;

import jakarta.annotation.Nonnull;
import software.coley.recaf.services.source.ResolverAdapter;
import software.coley.recaf.ui.control.richtext.suggest.java.CompletionKind;
import software.coley.recaf.ui.control.richtext.suggest.java.ContextKind;
import software.coley.recaf.ui.control.richtext.suggest.java.JavaCompletion;
import software.coley.recaf.ui.control.richtext.suggest.java.JavaCompletionFactory;
import software.coley.recaf.ui.control.richtext.suggest.java.JavaCompletionSession;
import software.coley.recaf.ui.control.richtext.suggest.java.JavaLexicalContext;
import software.coley.recaf.ui.control.richtext.suggest.java.ReceiverMode;
import software.coley.recaf.ui.control.richtext.suggest.java.ReceiverResolver;
import software.coley.recaf.ui.control.richtext.suggest.java.ResolvedReceiver;
import software.coley.recaf.ui.control.richtext.suggest.java.lookups.MemberLookup;
import software.coley.sourcesolver.resolve.entry.ArrayEntry;
import software.coley.sourcesolver.resolve.entry.ClassEntry;
import software.coley.sourcesolver.resolve.entry.EntryPool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Provider for member completions.
 *
 * @author Matt Coley
 */
public final class MemberCompletionProvider implements JavaCompletionProvider {
	private final ReceiverResolver receiverResolver = new ReceiverResolver();
	private final MemberLookup memberLookup = new MemberLookup();

	@Nonnull
	@Override
	public List<JavaCompletion> complete(@Nonnull JavaCompletionSession session, @Nonnull JavaLexicalContext context) {
		ResolverAdapter resolver = session.completionContext().getResolver();
		if (resolver == null)
			return List.of();

		ResolvedReceiver receiver = receiverResolver.resolveReceiver(session, resolver, context);
		if (receiver == null)
			return List.of();

		Map<String, JavaCompletion> completions = new LinkedHashMap<>();
		String partial = context.partialText();
		String currentPackage = session.currentPackageName();
		ClassEntry currentClassEntry = session.currentClassEntry(resolver.getEntryPool());
		boolean insertMethodCall = context.kind() != ContextKind.METHOD_REFERENCE;
		boolean methodsOnly = context.kind() == ContextKind.METHOD_REFERENCE;

		if (receiver.type() instanceof ArrayEntry) {
			if (receiver.mode() != ReceiverMode.STATIC_ONLY && !methodsOnly && JavaCompletionFactory.matchesPrefix("length", partial)) {
				JavaCompletion.addOrReplace(completions, new JavaCompletion(
						CompletionKind.FIELD,
						"length : int",
						"length",
						0,
						null,
						"length",
						0,
						""
				));
			}
			if (receiver.mode() != ReceiverMode.STATIC_ONLY && JavaCompletionFactory.matchesPrefix("clone", partial)) {
				JavaCompletion.addOrReplace(completions, JavaCompletionFactory.methodCompletion(
						"clone",
						"()Ljava/lang/Object;",
						0,
						null,
						insertMethodCall
				));
			}
			EntryPool pool = resolver.getEntryPool();
			ClassEntry objectEntry = pool.getClass("java/lang/Object");
			if (objectEntry != null)
				memberLookup.collectMemberCompletions(session, completions, objectEntry, currentClassEntry, partial, true, true,
						currentPackage, ReceiverMode.INSTANCE_ONLY, insertMethodCall);
			return new ArrayList<>(completions.values());
		}

		if (!(receiver.type() instanceof ClassEntry classEntry))
			return List.of();

		memberLookup.collectMemberCompletions(session, completions, classEntry, currentClassEntry, partial, methodsOnly, false,
				currentPackage, receiver.mode(), insertMethodCall);
		return new ArrayList<>(completions.values());
	}
}
