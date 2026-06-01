package software.coley.recaf.ui.control.richtext.suggest.java;

import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.ui.control.richtext.suggest.TabCompletionConfig;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Adaptive ranking for Java completions.
 *
 * @author Matt Coley
 */
public class JavaCompletionRanker {
	private static final int MAX_BASELINE_BOOST = 6;
	private static final int MAX_LEARNED_BOOST = 8;
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

	private final TabCompletionConfig config;
	private final Comparator<JavaCompletion> adaptiveOrder = Comparator
			.comparingInt(JavaCompletion::rank)
			.thenComparingInt(this::adaptiveRank)
			.thenComparing(JavaCompletion::sortKey, String.CASE_INSENSITIVE_ORDER)
			.thenComparing(JavaCompletion::displayText, String.CASE_INSENSITIVE_ORDER);

	public JavaCompletionRanker(@Nonnull TabCompletionConfig config) {
		this.config = config;
	}

	/**
	 * @param completions
	 * 		Raw completion results.
	 *
	 * @return Sorted completion results.
	 */
	@Nonnull
	public List<JavaCompletion> rank(@Nonnull List<JavaCompletion> completions) {
		Comparator<JavaCompletion> order = config.isAdaptiveRankingEnabled()
				? adaptiveOrder
				: JavaCompletionFactory.DEFAULT_COMPLETION_ORDER;
		return completions.stream().sorted(order).toList();
	}

	/**
	 * Track the selection of a completion for adaptive ranking purposes.
	 *
	 * @param completion
	 * 		Accepted completion.
	 */
	public void recordSelection(@Nonnull JavaCompletion completion) {
		if (!config.isAdaptiveRankingLearningEnabled() || !isAdaptiveKind(completion.kind()))
			return;
		String key = stableKey(completion);
		if (key != null)
			config.recordAdaptiveRankingSelection(key);
	}

	/**
	 * Clears all learned ranking data.
	 */
	public void reset() {
		config.clearAdaptiveRankingUsageCounts();
	}

	/**
	 * @param completion
	 * 		Completion to compute a stable key for.
	 *
	 * @return Stable key for the completion.
	 *
	 * @see TabCompletionConfig#getAdaptiveRankingUsageCount(String)
	 */
	@Nonnull
	@VisibleForTesting
	protected String stableKeyOrThrow(@Nonnull JavaCompletion completion) {
		String key = stableKey(completion);
		if (key == null)
			throw new IllegalStateException("Completion did not have a stable adaptive ranking key");
		return key;
	}

	private int adaptiveRank(@Nonnull JavaCompletion completion) {
		if (!isAdaptiveKind(completion.kind()))
			return 0;
		return -(baselineBoost(completion) + learnedBoost(completion));
	}

	private int baselineBoost(@Nonnull JavaCompletion completion) {
		return Math.min(MAX_BASELINE_BOOST, switch (completion.kind()) {
			case TYPE -> {
				String qualifiedName = qualifiedOwner(completion.path());
				int boost = TYPE_BASELINES.getOrDefault(completion.insertionText(), 0);
				if (qualifiedName != null)
					boost = Math.max(boost, TYPE_BASELINES.getOrDefault(qualifiedName, 0));
				yield boost;
			}
			case FIELD, METHOD -> {
				String name = completion.sortKey();
				String owner = ownerName(completion.path());
				yield (owner == null) ? 0 : OWNER_MEMBER_BASELINES.getOrDefault(owner + "#" + name, 0);
			}
			default -> 0;
		});
	}

	private int learnedBoost(@Nonnull JavaCompletion completion) {
		String key = stableKey(completion);
		if (key == null)
			return 0;
		int count = config.getAdaptiveRankingUsageCount(key);
		if (count <= 0)
			return 0;
		return Math.min(MAX_LEARNED_BOOST, 2 + (31 - Integer.numberOfLeadingZeros(count)));
	}

	private static boolean isAdaptiveKind(@Nonnull CompletionKind kind) {
		return kind == CompletionKind.TYPE || kind == CompletionKind.FIELD || kind == CompletionKind.METHOD;
	}

	/**
	 * @param completion
	 * 		Completion to compute a stable key for.
	 *
	 * @return Stable key for the completion, or {@code null} if a stable key cannot be computed.
	 *
	 * @see TabCompletionConfig#getAdaptiveRankingUsageCount(String)
	 */
	@Nullable
	private static String stableKey(@Nonnull JavaCompletion completion) {
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

	@Nullable
	private static String qualifiedOwner(@Nullable PathNode<?> path) {
		if (path instanceof ClassPathNode classPath)
			return qualifiedName(classPath.getValue());
		return null;
	}

	@Nonnull
	private static String qualifiedName(@Nonnull ClassInfo classInfo) {
		return qualifiedName(classInfo.getName());
	}

	@Nonnull
	private static String qualifiedName(@Nonnull String internalName) {
		return internalName.replace('/', '.').replace('$', '.');
	}

	private static void registerType(int boost, @Nonnull String qualifiedName) {
		String simpleName = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
		TYPE_BASELINES.put(qualifiedName, boost);
		TYPE_BASELINES.put(simpleName, boost);
	}

	private static void registerMember(int boost, @Nonnull String ownerQualifiedName, @Nonnull String memberName) {
		OWNER_MEMBER_BASELINES.put(ownerQualifiedName + "#" + memberName, boost);
	}
}
