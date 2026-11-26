package software.coley.recaf.ui.control.richtext.suggest.assembler;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.ui.control.richtext.suggest.TabCompletionConfig;
import software.coley.recaf.ui.control.richtext.suggest.ranking.CompletionRanker;
import software.coley.recaf.ui.control.richtext.suggest.ranking.RankingKeys;

import java.util.Map;
import java.util.TreeMap;

/**
 * Ranking keys and built-in baselines for JASM completions.
 *
 * @author Matt Coley
 */
public final class AssemblerCompletionRanking {
	private static final Map<String, Integer> TYPE_BASELINES = new TreeMap<>();
	private static final Map<String, Integer> MEMBER_BASELINES = new TreeMap<>();

	static {
		registerType(6, "java/lang/String");
		registerType(5, "java/lang/Class");
		registerType(5, "java/lang/System");
		registerType(4, "java/lang/Integer");
		registerType(4, "java/lang/Double");
		registerType(4, "java/lang/Long");
		registerType(4, "java/lang/Boolean");
		registerType(4, "java/lang/StringBuilder");
		registerType(4, "java/util/Arrays");
		registerType(4, "java/util/List");
		registerType(4, "java/util/Map");
		registerType(4, "java/util/Set");
		registerType(4, "java/io/File");
		registerType(4, "java/nio/file/Paths");
		registerType(4, "java/nio/file/Path");
		registerType(4, "java/nio/file/Files");
		registerType(3, "java/util/stream/Stream");
		registerType(3, "java/lang/Throwable");
		registerType(3, "java/lang/Exception");
		registerType(3, "java/util/ArrayList");
		registerType(3, "java/util/HashMap");
		registerType(3, "java/util/HashSet");
		registerType(3, "java/util/Optional");
		registerType(2, "java/lang/Object");
		registerType(2, "java/lang/StringBuffer");
		registerMember(6, "java/io/PrintStream", "println");
		registerMember(6, "java/io/PrintWriter", "println");
		registerMember(4, "java/io/PrintStream", "print");
		registerMember(4, "java/io/PrintWriter", "print");
		registerMember(3, "java/io/PrintStream", "printf");
		registerMember(3, "java/io/PrintWriter", "printf");
		registerMember(2, "java/io/PrintStream", "format");
		registerMember(2, "java/io/PrintWriter", "format");
		registerMember(5, "java/lang/StringBuilder", "append");
		registerMember(4, "java/lang/StringBuffer", "append");
	}

	private AssemblerCompletionRanking() {}

	/**
	 * @param config
	 * 		Config holding adaptive ranking settings and learned usage counts.
	 *
	 * @return Ranker for assembler completions.
	 */
	@Nonnull
	public static CompletionRanker<AssemblerCompletion> create(@Nonnull TabCompletionConfig config) {
		RankingKeys<AssemblerCompletion> keys = new RankingKeys<>(
				completion -> completion instanceof AssemblerCompletion.Type type ? type.matchRank() : 0,
				AssemblerCompletion::text,
				AssemblerCompletion::text,
				AssemblerCompletionRanking::learningKey,
				AssemblerCompletionRanking::baselineBoost);
		return new CompletionRanker<>(config, keys);
	}

	/**
	 * @param completion
	 * 		Completion to compute a learning key for.
	 *
	 * @return Stable key for the completion, or {@code null} for operands, which are never learned.
	 */
	@Nullable
	private static String learningKey(@Nonnull AssemblerCompletion completion) {
		return switch (completion) {
			case AssemblerCompletion.Opcode opcode -> "JASM:OPCODE:" + opcode.text();
			case AssemblerCompletion.Type type -> "JASM:TYPE:" + type.path().getValue().getName();
			case AssemblerCompletion.Member member -> {
				String owner = ownerName(member);
				if (owner == null)
					yield null;
				yield "JASM:MEMBER:" + owner + "#" + member.path().getValue().getName()
						+ member.path().getValue().getDescriptor();
			}
			case AssemblerCompletion.Operand operand -> null;
		};
	}

	private static int baselineBoost(@Nonnull AssemblerCompletion completion) {
		return switch (completion) {
			case AssemblerCompletion.Type type -> TYPE_BASELINES.getOrDefault(type.path().getValue().getName(), 0);
			case AssemblerCompletion.Member member -> {
				String owner = ownerName(member);
				if (owner == null)
					yield 0;
				yield MEMBER_BASELINES.getOrDefault(owner + "#" + member.path().getValue().getName(), 0);
			}
			case AssemblerCompletion.Opcode opcode -> 0;
			case AssemblerCompletion.Operand operand -> 0;
		};
	}

	@Nullable
	private static String ownerName(@Nonnull AssemblerCompletion.Member member) {
		ClassPathNode parent = member.path().getParent();
		return parent == null ? null : parent.getValue().getName();
	}

	private static void registerType(int boost, @Nonnull String internalName) {
		TYPE_BASELINES.put(internalName, boost);
	}

	private static void registerMember(int boost, @Nonnull String ownerInternalName, @Nonnull String memberName) {
		MEMBER_BASELINES.put(ownerInternalName + "#" + memberName, boost);
	}
}
