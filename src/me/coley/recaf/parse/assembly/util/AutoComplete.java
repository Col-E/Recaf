package me.coley.recaf.parse.assembly.util;

import com.google.common.reflect.ClassPath;
import me.coley.recaf.Input;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.util.*;
import java.util.stream.Collectors;

public class AutoComplete {
	private static Collection<String> master;

	private static Collection<String> names() {
		Input in = Input.get();
		if(in != null)
			return in.classes;
		else {
			try {
				// TODO: There can probably be some more optimization to have this load/iterate faster
				if(master == null)
					master = ClassPath.from(ClassLoader.getSystemClassLoader()).getAllClasses()
							.stream().map(info -> info.getName().replace(".", "/")).collect(Collectors.toList());
				return master;
			} catch(Exception e) {
				return master = Collections.emptyList();
			}
		}
	}

	// =================================================================== //

	public static List<String> internalName(String part) {
		return names().stream()
				.filter(name -> name.startsWith(part))
				.sorted(Comparator.naturalOrder())
				.collect(Collectors.toList());
	}

	public static List<String> descriptorName(String part) {
		return Collections.emptyList();
	}

	// =================================================================== //

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
					Class<?> c = Class.forName(owner.replace("/", "."), false, ClassLoader
							.getSystemClassLoader());
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
					Class<?> c = Class.forName(owner.replace("/", "."), false, ClassLoader
							.getSystemClassLoader());
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
