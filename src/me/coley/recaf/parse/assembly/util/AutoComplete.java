package me.coley.recaf.parse.assembly.util;

import com.google.common.reflect.ClassPath;
import me.coley.recaf.Input;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Autocomplete utility.
 *
 * @author Matt
 */
public class AutoComplete {
	/**
	 * System classloader
	 */
	private final static ClassLoader scl = ClassLoader.getSystemClassLoader();
	/**
	 * Last input name.
	 */
	private static String lastInput;
	/**
	 * Classpath class names.
	 */
	private static Collection<String> masterClassPath;
	/**
	 * All class names.
	 */
	private static Collection<String> master;


	/**
	 * @return cached {@link #master} set of names.
	 */
	private static Collection<String> names() {
		// TODO: There can probably be some more optimization to have this load/iterate faster
		// - By iterate, I mean in usage below in other methods.
		if(masterClassPath == null) {
			try {
				masterClassPath = ClassPath.from(scl).getAllClasses().stream()
						.map(info -> info.getName().replace(".", "/"))
						.collect(Collectors.toList());
			} catch(Exception e) {
				masterClassPath = new ArrayList<>();
			}
		}
		Input in = Input.get();
		String currentInput = in == null ? null : in.toString();
		if (currentInput != null && !currentInput.equals(lastInput)) {
			master = new ArrayList<>(masterClassPath);
			master.addAll(in.classes);
			lastInput = currentInput;
		} else if (master == null) {
			master = new ArrayList<>(masterClassPath);
		}
		return master;
	}

	// =================================================================== //

	/**
	 * @param part
	 * 		Current token to complete on.
	 *
	 * @return List of internal name completions.
	 */
	public static List<String> internalName(String part) {
		return names().stream()
				.filter(name -> name.startsWith(part))
				.sorted(Comparator.naturalOrder())
				.collect(Collectors.toList());
	}

	/**
	 * @param part
	 * 		Current token to complete on.
	 * @return List of internal descriptor completions.
	 */
	public static List<String> descriptorName(String part) {
		String key = part.startsWith(" ") ? part.substring(1) : part;
		return  names().stream()
				.map(s -> "L" + s + ";")
				.filter(name -> name.startsWith(key))
				.sorted(Comparator.naturalOrder())
				.collect(Collectors.toList());
	}

	// =================================================================== //

	/**
	 * @param token
	 * 		Tokenizer root node, useful for fetching prior tokens.
	 * @param part
	 * 		Current token to complete on.
	 *
	 * @return List of method completions.
	 */
	public static List<String> method(RegexToken token, String part) {
		String owner = token.get("OWNER").trim();
		try {
			if(owner != null) {
				// Part string should have a "." in it, get rid of that.
				String key = part.startsWith(".") ? part.substring(1) : part;
				Input in = Input.get();
				if(in != null && in.classes.contains(owner)) {
					// Check classes loaded in the input jar
					ClassNode cn = in.getClass(owner);
					return cn.methods.stream()
							.map(mn -> mn.name + mn.desc)
							.filter(s -> s.startsWith(key))
							.sorted(Comparator.naturalOrder())
							.collect(Collectors.toList());
				} else {
					// Check runtime
					Class<?> c = Class.forName(owner.replace("/", "."), false, scl);
					if (c == null)
						return Collections.emptyList();
					// Iterate over runtime visible methods
					return Arrays.asList(c.getDeclaredMethods()).stream()
							.map(md -> md.getName() + Type.getType(md))
							.filter(s -> s.startsWith(key))
							.sorted(Comparator.naturalOrder())
							.collect(Collectors.toList());
				}
			}
		} catch(Exception e) {}
		return Collections.emptyList();
	}

	/**
	 * @param token
	 * 		Tokenizer root node, useful for fetching prior tokens.
	 * @param part
	 * 		Current token to complete on.
	 *
	 * @return List of field completions.
	 */
	public static List<String> field(RegexToken token, String part) {
		String owner = token.get("OWNER").trim();
		try {
			if(owner != null) {
				// Part string should have a "." in it, get rid of that.
				String key = part.startsWith(".") ? part.substring(1) : part;
				Input in = Input.get();
				if(in != null && in.classes.contains(owner)) {
					// Check classes loaded in the input jar
					ClassNode cn = in.getClass(owner);
					return cn.fields.stream()
							.map(fn -> fn.name + " " + fn.desc)
							.filter(s -> s.startsWith(key))
							.sorted(Comparator.naturalOrder())
							.collect(Collectors.toList());
				} else {
					// Check runtime
					Class<?> c = Class.forName(owner.replace("/", "."), false, scl);
					if (c == null)
						return Collections.emptyList();
					// Iterate over runtime visible fields
					return Arrays.asList(c.getDeclaredFields()).stream()
							.map(fd -> fd.getName() + " " + Type.getType(fd.getType()))
							.filter(s -> s.startsWith(key))
							.sorted(Comparator.naturalOrder())
							.collect(Collectors.toList());
				}
			}
		} catch(Exception e) {}
		return Collections.emptyList();
	}
}
