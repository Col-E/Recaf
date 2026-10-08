package software.coley.recaf.ui.control.richtext.suggest.java;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.ui.control.richtext.suggest.TabCompletionConfig;
import software.coley.recaf.ui.control.richtext.suggest.ranking.CompletionRanker;
import software.coley.recaf.ui.control.richtext.suggest.ranking.RankingKeys;

import java.util.Map;
import java.util.TreeMap;

/**
 * Java-specific ranking keys and built-in baselines for {@link CompletionRanker}.
 *
 * @author Matt Coley
 */
public final class JavaCompletionRanking {
	private static final Map<String, Integer> TYPE_BASELINES = new TreeMap<>();
	private static final Map<String, Integer> OWNER_MEMBER_BASELINES = new TreeMap<>();

	static {
		// Populate baseline boosts for common types and members.
		registerType(6, "java.lang.String");
		registerType(5, "java.lang.Class");
		registerType(5, "java.lang.System");
		registerType(4, "java.lang.Integer");
		registerType(4, "java.lang.Double");
		registerType(4, "java.lang.Long");
		registerType(4, "java.lang.Boolean");
		registerType(4, "java.lang.StringBuilder");
		registerType(4, "java.util.Arrays");
		registerType(4, "java.util.List");
		registerType(4, "java.util.Map");
		registerType(4, "java.util.Set");
		registerType(4, "java.io.File");
		registerType(4, "java.nio.file.Paths");
		registerType(4, "java.nio.file.Path");
		registerType(4, "java.nio.file.Files");
		registerType(3, "java.util.Stream");
		registerType(3, "java.lang.Throwable");
		registerType(3, "java.lang.Exception");
		registerType(3, "java.util.ArrayList");
		registerType(3, "java.util.HashMap");
		registerType(3, "java.util.HashSet");
		registerType(3, "java.util.Optional");
		registerType(2, "java.lang.Object");
		registerType(2, "java.lang.StringBuffer");
		registerMember(6, "java.io.PrintStream", "println");
		registerMember(6, "java.io.PrintWriter", "println");
		registerMember(4, "java.io.PrintStream", "print");
		registerMember(4, "java.io.PrintWriter", "print");
		registerMember(3, "java.io.PrintStream", "printf");
		registerMember(3, "java.io.PrintWriter", "printf");
		registerMember(2, "java.io.PrintStream", "format");
		registerMember(2, "java.io.PrintWriter", "format");
		registerMember(5, "java.lang.StringBuilder", "append");
		registerMember(4, "java.lang.StringBuffer", "append");
	}

	private JavaCompletionRanking() {}

	/**
	 * @param config
	 * 		Config holding adaptive ranking settings and learned usage counts.
	 *
	 * @return Ranker for Java completions.
	 */
	@Nonnull
	public static CompletionRanker<JavaCompletion> create(@Nonnull TabCompletionConfig config) {
		return new CompletionRanker<>(config, new RankingKeys<>(
				JavaCompletion::rank,
				JavaCompletion::sortKey,
				JavaCompletion::displayText,
				JavaCompletionRanking::learningKey,
				JavaCompletionRanking::baselineBoost));
	}

	/**
	 * @param completion
	 * 		Completion to compute a learning key for.
	 *
	 * @return Stable key for the completion, or {@code null} for keyword, local, and package completions.
	 */
	@Nullable
	private static String learningKey(@Nonnull JavaCompletion completion) {
		return switch (completion.kind()) {
			case TYPE -> {
				String qualifiedName = qualifiedOwner(completion.path());
				yield "TYPE:" + (qualifiedName != null ? qualifiedName : completion.insertionText());
			}
			case FIELD, METHOD -> {
				if (completion.path() instanceof ClassMemberPathNode memberPath) {
					ClassMember member = memberPath.getValue();
					String owner = ownerName(memberPath);
					if (owner != null)
						yield completion.kind() + ":" + owner + "#" + member.getName() + member.getDescriptor();
				}
				yield completion.kind() + ":" + completion.displayText();
			}
			default -> null;
		};
	}

	/**
	 * @param completion
	 * 		Completion to compute a baseline boost for.
	 *
	 * @return Baseline boost for the completion.
	 */
	private static int baselineBoost(@Nonnull JavaCompletion completion) {
		return switch (completion.kind()) {
			case TYPE -> {
				String qualifiedName = qualifiedOwner(completion.path());
				int boost = TYPE_BASELINES.getOrDefault(completion.insertionText(), 0);
				if (qualifiedName != null)
					boost = Math.max(boost, TYPE_BASELINES.getOrDefault(qualifiedName, 0));
				yield boost;
			}
			case FIELD, METHOD -> {
				String owner = ownerName(completion.path());
				yield (owner == null) ? 0 : OWNER_MEMBER_BASELINES.getOrDefault(owner + "#" + completion.sortKey(), 0);
			}
			default -> 0;
		};
	}

	/**
	 * @param path
	 * 		Path to a member in the workspace.
	 *
	 * @return Owner name of the member, or {@code null} if the member is not backed by a workspace entry.
	 */
	@Nullable
	private static String ownerName(@Nullable PathNode<?> path) {
		if (path instanceof ClassMemberPathNode memberPath) {
			ClassPathNode parent = memberPath.getParent();
			if (parent != null)
				return qualifiedName(parent.getValue());
		}
		if (path instanceof ClassPathNode classPath)
			return qualifiedName(classPath.getValue());
		return null;
	}

	/**
	 * @param path
	 * 		Path to a type in the workspace.
	 *
	 * @return Qualified name of the type, or {@code null} if the type is not backed by a workspace entry.
	 */
	@Nullable
	private static String qualifiedOwner(@Nullable PathNode<?> path) {
		if (path instanceof ClassPathNode classPath)
			return qualifiedName(classPath.getValue());
		return null;
	}

	/**
	 * @param classInfo
	 * 		Class info to get a qualified name for.
	 *
	 * @return Qualified name of the class.
	 */
	@Nonnull
	private static String qualifiedName(@Nonnull ClassInfo classInfo) {
		return qualifiedName(classInfo.getName());
	}

	/**
	 * @param internalName
	 * 		Internal name to get a qualified name for.
	 *
	 * @return Qualified name of the class.
	 */
	@Nonnull
	private static String qualifiedName(@Nonnull String internalName) {
		return internalName.replace('/', '.').replace('$', '.');
	}

	/**
	 * Registers a type with a baseline boost for ranking.
	 *
	 * @param boost
	 * 		Baseline boost to apply to the type.
	 * @param qualifiedName
	 * 		Dot-separated qualified name of the type.
	 */
	private static void registerType(int boost, @Nonnull String qualifiedName) {
		String simpleName = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
		TYPE_BASELINES.put(qualifiedName, boost);
		TYPE_BASELINES.put(simpleName, boost);
	}

	/**
	 * Registers a member with a baseline boost for ranking.
	 *
	 * @param boost
	 * 		Baseline boost to apply to the member.
	 * @param ownerQualifiedName
	 * 		Dot-separated qualified name of the member's owner type.
	 * @param memberName
	 * 		Name of the member.
	 */
	private static void registerMember(int boost, @Nonnull String ownerQualifiedName, @Nonnull String memberName) {
		OWNER_MEMBER_BASELINES.put(ownerQualifiedName + "#" + memberName, boost);
	}
}
