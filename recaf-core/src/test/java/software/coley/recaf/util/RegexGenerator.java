package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import me.darknet.assembler.backend.dalvik.DalvikTargetContext;
import me.darknet.assembler.backend.jvm.JvmTargetContext;
import me.darknet.assembler.parser.processor.DeclarationRegistry;
import me.darknet.assembler.parser.processor.ProcessorKeywords;
import me.darknet.assembler.target.TargetContext;
import me.darknet.assembler.util.KeywordRegexUtil;
import me.darknet.assembler.visitor.Modifiers;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Utility to generate keyword regex patterns.
 */
public class RegexGenerator {
	public static void main(String[] args) {
		print("java", collectJava());
		print("jasm", collectJasm());
	}

	@Nonnull
	private static Set<String> collectJava() {
		return new TreeSet<>(Keywords.getKeywords());
	}

	@Nonnull
	private static Set<String> collectJasm() {
		// Common keywords across all targets
		Set<String> keywords = new HashSet<>(DeclarationRegistry.createDefault().getKeywords());
		keywords.addAll(Modifiers.getValidModifiers());
		keywords.addAll(ProcessorKeywords.getCommonMethodBodyKeywords());

		// Target-specific keywords
		TargetContext[] contexts = new TargetContext[]{
				JvmTargetContext.INSTANCE,
				DalvikTargetContext.INSTANCE
		};
		for (TargetContext context : contexts)
			keywords.addAll(context.keywordNames());
		return keywords;
	}

	private static void print(@Nonnull String set, @Nonnull Set<String> keywords) {
		System.out.println("\n## Regex: " + set + " keywords: " + keywords.size() + "\n");

		// Basic regex
		System.out.println("\\b(?:" + String.join("|", keywords) + ")\\b\n");

		// Packed regex, with special boundary handling for cases where '\b' isn't enough.
		String keywordRegex = KeywordRegexUtil.getKeywordRegex(keywords);
		System.out.println(keywordRegex);
	}
}
