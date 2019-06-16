package me.coley.recaf.parse.assembly.util;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import me.coley.recaf.Input;
import me.coley.recaf.util.Classpath;

/**
 * Autocomplete utility.
 *
 * @author Matt
 */
public class AutoComplete {
	/**
	 * Last input (for caching purpose).
	 * Uses {@link WeakReference} to allow GC after
	 * {@linkplain Input#input the original reference} becomes unreachable.
	 */
	private static WeakReference<Optional<Input>> lastInput = new WeakReference<>(null);

	/**
	 * A sorted list of class names for auto-completion,
	 * including {@linkplain Classpath#getSystemClassNames() the system's} and {@linkplain Input#classes the current input's}.
	 */
	private static List<String> cachedClassNames;

	/**
	 * Computes and/or returns a sorted list of class names available for completion.
	 */
	private static Stream<String> classNames() {
		Optional<Input> cachedInput = lastInput.get();
		Optional<Input> currentInput = Input.getOptional();
		if (cachedInput == null || !currentInput.equals(cachedInput)) {
			lastInput = new WeakReference<>(currentInput);
			List<String> systemClassNames = Classpath.getSystemClassNames();
			if (currentInput.isPresent()) {
				Set<String> inputClasses = currentInput.get().classes;
				int totalSize = inputClasses.size() + systemClassNames.size();
				cachedClassNames = Collections.unmodifiableList(Stream
						.concat(inputClasses.stream(), systemClassNames.stream())
						.distinct()
						.sorted(Comparator.naturalOrder())  // Pre-sort to save some time
						.collect(Collectors.toCollection(() -> new ArrayList<>(totalSize))));
			} else {
				cachedClassNames = systemClassNames;
			}
		}
		return cachedClassNames.stream();
	}

	// =================================================================== //

	/**
	 * Completes internal names.
	 *
	 * @param part current token to complete on
	 * @return a list of internal name completions, ordered alphabetically
	 */
	public static List<String> internalName(String part) {
		String key = part.trim();
		if (part.isEmpty()) return Collections.emptyList();
		return classNames()
				.filter(name -> name.startsWith(key))
				// .sorted()    // Input stream is already sorted
				.collect(Collectors.toList());
	}

	/**
	 * Completes internal descriptors.
	 *
	 * @param part current token to complete on
	 * @return a list of internal descriptor completions, ordered alphabetically
	 */
	public static List<String> descriptorName(String part) {
		part = part.trim();
		if (part.isEmpty()) return Collections.emptyList();
		StringBuilder prefixBuilder = new StringBuilder(1);
		StringBuilder keyBuilder = new StringBuilder(part.length() - 1);
		for (char c : part.toCharArray()) {
			if (keyBuilder.length() == 0) {
				if (c == 'L' || c == '[') {
					prefixBuilder.append(c);
					continue;    // Separate the prefix (`L` or `[L` etc) from the actual token for matching
				}
			} else if (c == ';') {
				// Already completed, don't bother
				return Collections.emptyList();
			}

			keyBuilder.append(c);
		}

		// No tokens to complete or no valid prefix found.
		if (prefixBuilder.length() == 0 || prefixBuilder.indexOf("L") == -1 || keyBuilder.length() == 0) {
			return Collections.emptyList();
		}

		String prefix = prefixBuilder.toString();
		String key = keyBuilder.toString();
		return classNames()
				.filter(name -> name.startsWith(key))
				// .sorted()                       // Input stream is already sorted
				.map(name -> prefix + name + ";")  // Re-adds the prefix and the suffix to the suggestions
				.collect(Collectors.toList());
	}

	// =================================================================== //

	/**
	 * Completes methods.
	 *
	 * @param token tokenizer root node
	 * @param part current token to complete on
	 * @return a list of method completions, ordered alphabetically
	 */
	public static List<String> method(RegexToken token, String part) {
		return matchSignatures(token, part,
				c -> Arrays.stream(c.getDeclaredMethods()).map(md -> md.getName().concat(Type.getMethodDescriptor(md))),
				cn -> cn.methods.stream().map(mn -> mn.name.concat(mn.desc)));
	}

	/**
	 * Completes fields.
	 *
	 * @param token tokenizer root node
	 * @param part current token to complete on
	 * @return a list of field completions, ordered alphabetically
	 */
	public static List<String> field(RegexToken token, String part) {
		return matchSignatures(token, part,
				c -> Arrays.stream(c.getDeclaredFields()).map(fd -> fd.getName() + " " + Type.getType(fd.getType())),
				cn -> cn.fields.stream().map(fn -> fn.name + " " + fn.desc));
	}

	/**
	 * Completes signatures.
	 *
	 * @param token tokenizer root node
	 * @param part current token to complete on
	 * @param signaturesFromClass the function used to map {@linkplain Class classes} to signatures
	 * @param signaturesFromNode the function used to map {@link ClassNode}s to signatures
	 * @return a list of signature completions, ordered alphabetically
	 */
	private static List<String> matchSignatures(RegexToken token, String part,
	                                            Function<Class<?>, Stream<String>> signaturesFromClass,
	                                            Function<ClassNode, Stream<String>> signaturesFromNode) {
		// Part string should have a "." in it, get rid of that.
		String key = part.charAt(0) == '.' ? part.substring(1) : part;

		String owner = token.get("OWNER");
		Stream<String> signatures = null;

		if (owner != null) {
			owner = owner.trim();
			Input in = Input.get();
			if (in != null && in.classes.contains(owner)) {
				// Check classes loaded in the input jar
				ClassNode cn = in.getClass(owner);
				signatures = signaturesFromNode.apply(cn);
			} else {
				// Check runtime
				Optional<Class<?>> c = Classpath.getSystemClassIfExists(owner.replace('/', '.'));
				signatures = c.map(signaturesFromClass).orElse(null);
			}
		}

		if (signatures != null) {
			return signatures
					.filter(s -> s.startsWith(key))
					.sorted(Comparator.naturalOrder())
					.collect(Collectors.toList());
		} else {
			return Collections.emptyList();
		}
	}
}
