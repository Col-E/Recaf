package me.coley.recaf.util;

import me.coley.recaf.Recaf;
import me.coley.recaf.workspace.Workspace;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Autocomplete utility.
 *
 * @author Matt
 * @author Andy Li
 */
public class AutoCompleteUtil {
	/**
	 * A sorted list of class names for auto-completion,
	 * including {@linkplain ClasspathUtil#getSystemClassNames() the system's} and
	 * {@linkplain Workspace#getClassNames()} the current input's}.
	 */
	private static Set<String> cachedClassNames;

	/**
	 * Computes and/or returns a sorted list of class names available for completion.
	 */
	private static Stream<String> classNames() {
		Set<String> systemClassNames = ClasspathUtil.getSystemClassNames();
		Optional<Workspace> opt = Optional.ofNullable(Recaf.getCurrentWorkspace());
		if (opt.isPresent()) {
			Set<String> inputClasses = opt.get().getClassNames();
			int totalSize = inputClasses.size() + systemClassNames.size();
			cachedClassNames = Collections.unmodifiableSet(Stream
					.concat(inputClasses.stream(), systemClassNames.stream())
					.distinct()
					.sorted(Comparator.naturalOrder())  // Pre-sort to save some time
					.collect(Collectors.toCollection(() -> new LinkedHashSet<>(totalSize))));
		} else {
			cachedClassNames = systemClassNames;
		}
		return cachedClassNames.stream();
	}

	// =================================================================== //

	/**
	 * Completes internal names.
	 *
	 * @param part
	 * 		current token to complete on
	 *
	 * @return a list of internal name completions, ordered alphabetically
	 */
	public static List<String> internalName(String part) {
		String key = part.trim();
		if (part.isEmpty())
			return Collections.emptyList();
		return classNames()
				.filter(name -> name.startsWith(key) && !name.equals(key))
				.collect(Collectors.toList());
	}

	/**
	 * Completes descriptors.
	 *
	 * @param part
	 * 		Current token to complete on
	 *
	 * @return List of descriptor completions, ordered alphabetically.
	 */
	public static List<String> descriptorName(String part) {
		part = part.trim();
		if (part.isEmpty())
			return Collections.emptyList();
		StringBuilder prefixBuilder = new StringBuilder(1);
		StringBuilder keyBuilder = new StringBuilder(part.length() - 1);
		for (char c : part.toCharArray()) {
			if (keyBuilder.length() == 0) {
				// Separate the prefix (`L` or `[L` etc) from the actual token for matching
				if(c == 'L' || c == '[') {
					prefixBuilder.append(c);
					continue;
				}
			} else if (c == ';') {
				// Already completed, don't bother
				return Collections.emptyList();
			}
			keyBuilder.append(c);
		}
		// No tokens to complete or no valid prefix found.
		if (prefixBuilder.length() == 0 || prefixBuilder.indexOf("L") == -1 || keyBuilder.length() == 0)
			return Collections.emptyList();
		//
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
	 * @param line
	 * 		Current line to complete.
	 *
	 * @return List of method completions, ordered alphabetically.
	 */
	public static List<String> method(String line) {
		return matchSignatures(line,
				c -> Arrays.stream(c.getDeclaredMethods())
						.map(md -> md.getName().concat(Type.getMethodDescriptor(md))),
				cr -> ClassUtil.getMethodDefs(cr).stream()
						.map(p -> p.getKey() + p.getValue()));
	}

	/**
	 * Completes fields.

	 * @param line
	 * 		Current line to complete.
	 *
	 * @return List of field completions, ordered alphabetically.
	 */
	public static List<String> field(String line) {
		return matchSignatures(line,
				c -> Arrays.stream(c.getDeclaredFields())
						.map(fd -> fd.getName() + " " + Type.getType(fd.getType())),
				cr -> ClassUtil.getFieldDefs(cr).stream()
						.map(p -> p.getKey()  + " " + p.getValue()));
	}

	/**
	 * Completes signatures.
	 *
	 * @param line
	 * 		Current line to complete.
	 * @param signaturesFromClass
	 * 		The function used to map {@link Class classes} to signatures.
	 * @param signaturesFromNode
	 * 		The function used to map {@link ClassReader}s to signatures.
	 *
	 * @return List of signature completions, ordered alphabetically.
	 */
	private static List<String> matchSignatures(String line,
	                                            Function<Class<?>, Stream<String>> signaturesFromClass,
	                                            Function<ClassReader, Stream<String>> signaturesFromNode) {
		int dot = line.indexOf('.');
		if (dot == -1)
			return Collections.emptyList();
		String owner = line.substring(0, dot).trim();
		String member = line.substring(dot + 1);
		// Assembler should have already run the parse chain, so we can fetch values
		Stream<String> signatures = null;
		// Attempt to check against workspace classes, fallback using runtime classes
		Optional<Workspace> opt = Optional.ofNullable(Recaf.getCurrentWorkspace());
		if (opt.isPresent()) {
			Workspace in = opt.get();
			ClassReader cr = in.getClassReader(owner);
			if (cr != null)
				signatures = signaturesFromNode.apply(cr);
		}
		if (signatures == null) {
			// Check runtime
			Optional<Class<?>> c = ClasspathUtil.getSystemClassIfExists(owner.replace('/', '.'));
			signatures = c.map(signaturesFromClass).orElse(null);
		}
		if (signatures != null) {
			return signatures
					.filter(s -> s.startsWith(member))
					.sorted(Comparator.naturalOrder())
					.collect(Collectors.toList());
		} else {
			return Collections.emptyList();
		}
	}
}