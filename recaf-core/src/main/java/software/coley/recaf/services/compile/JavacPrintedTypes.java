package software.coley.recaf.services.compile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.phantom.GeneratedPhantomWorkspaceResource;
import software.coley.recaf.util.Types;
import software.coley.recaf.workspace.model.Workspace;

import java.util.HashMap;
import java.util.Map;

/**
 * Resolves types as javac prints them in diagnostics to internal names and descriptors.
 *
 * @author jus7n
 * @see JavacDiagnosticHints
 */
final class JavacPrintedTypes {
	private static final String OBJECT_DESCRIPTOR = "Ljava/lang/Object;";
	private final Map<String, JvmClassInfo> phantomsByName = new HashMap<>();
	private final Map<String, String> phantomNameByKey = new HashMap<>();
	private final Workspace workspace;
	private Map<String, JvmClassInfo> knownByKey;

	/**
	 * @param phantoms
	 * 		Phantoms generated for the compilation.
	 * @param workspace
	 * 		Workspace the compilation ran against, or {@code null} when there is none.
	 */
	JavacPrintedTypes(@Nonnull GeneratedPhantomWorkspaceResource phantoms, @Nullable Workspace workspace) {
		this.workspace = workspace;

		phantoms.getJvmClassBundle().stream().forEach(info -> {
			phantomsByName.put(info.getName(), info);
			phantomNameByKey.put(keyOf(info.getName()), info.getName());
		});
	}

	/**
	 * @param key
	 * 		Printed name of a class.
	 *
	 * @return Internal name of the generated phantom with that printed name, or {@code null} when there is none.
	 */
	@Nullable
	public String phantomName(@Nonnull String key) {
		return phantomNameByKey.get(key);
	}

	/**
	 * @param internalName
	 * 		Internal name of a generated phantom.
	 *
	 * @return The generated phantom, or {@code null} when no phantom has that name.
	 */
	@Nullable
	public JvmClassInfo phantom(@Nonnull String internalName) {
		return phantomsByName.get(internalName);
	}

	/**
	 * @param key
	 * 		Printed name of a class.
	 *
	 * @return Workspace class with that printed name, or {@code null} when there is none.
	 */
	@Nullable
	public JvmClassInfo known(@Nonnull String key) {
		return knownByKey().get(key);
	}

	/**
	 * @param internalName
	 * 		Internal name of a class.
	 *
	 * @return {@code true} when the workspace has a class with that name.
	 */
	public boolean isKnown(@Nonnull String internalName) {
		return knownByKey().containsKey(keyOf(internalName));
	}

	/**
	 * @param key
	 * 		Printed name of a class.
	 *
	 * @return Internal name of the phantom or workspace class with that printed name, or {@code null} when there is
	 * none. Phantoms take precedence.
	 */
	@Nullable
	public String resolve(@Nonnull String key) {
		String internal = phantomNameByKey.get(key);
		if (internal == null) {
			JvmClassInfo known = known(key);
			if (known != null)
				internal = known.getName();
		}
		return internal;
	}

	/**
	 * @param key
	 * 		Printed name of a class.
	 *
	 * @return Internal name of the class, resolved when possible. Otherwise every dot is assumed to separate packages.
	 */
	@Nonnull
	public String internalNameOf(@Nonnull String key) {
		String internal = resolve(key);
		return internal != null ? internal : key.replace('.', '/');
	}

	/**
	 * @param internalPackage
	 * 		Internal name of a package, without a trailing slash.
	 *
	 * @return Number of workspace classes directly inside the package. Nested classes are not counted separately.
	 */
	public int packageMemberCount(@Nonnull String internalPackage) {
		String prefix = internalPackage + "/";
		int count = 0;
		for (JvmClassInfo info : knownByKey().values()) {
			String name = info.getName();
			if (name.startsWith(prefix) && name.indexOf('/', prefix.length()) < 0)
				count++;
		}
		return count;
	}

	/**
	 * @param printedType
	 * 		Type as javac printed it as the target of a conversion. May be generic or an array.
	 *
	 * @return Descriptor of the type's erasure, or {@code null} when the name cannot be resolved to a class. Simple
	 * names that are neither known nor generated are likely type variables.
	 */
	@Nullable
	public String descriptorOf(@Nonnull String printedType) {
		String type = erase(printedType);
		int dims = 0;
		while (type.endsWith("[]")) {
			dims++;
			type = type.substring(0, type.length() - 2);
		}
		String primitive = Types.sourceToPrimitive(type);
		if (primitive != null)
			return "[".repeat(dims) + primitive;
		if (!type.contains(".") && resolve(type) == null)
			return null;
		return "[".repeat(dims) + "L" + internalNameOf(type) + ";";
	}

	/**
	 * @param printedArguments
	 * 		Argument types as javac printed them, or {@code null} when there are none to print.
	 *
	 * @return Method descriptor returning {@code Object}, or {@code null} when the argument list cannot be read.
	 * javac does not report the return type of a missing method, and {@code Object} accepts any reference use.
	 */
	@Nullable
	public String methodDescriptor(@Nullable String printedArguments) {
		String parameters = parameterDescriptor(printedArguments);
		return parameters == null ? null : parameters + OBJECT_DESCRIPTOR;
	}

	/**
	 * @param printedArguments
	 * 		Argument types as javac printed them, or {@code null} when there are none.
	 *
	 * @return Parameter part of a method descriptor, including both parentheses,
	 * or {@code null} when the list cannot be read.
	 */
	@Nullable
	public String parameterDescriptor(@Nullable String printedArguments) {
		StringBuilder params = new StringBuilder("(");
		if (printedArguments != null && !printedArguments.isBlank()) {
			// Generic arguments do not change the erasure that the descriptor needs.
			for (String argument : erase(printedArguments).split(",")) {
				String type = argument.trim();
				if (type.isEmpty())
					return null;
				int dims = 0;
				if (type.endsWith("...")) {
					dims = 1;
					type = type.substring(0, type.length() - 3);
				}
				while (type.endsWith("[]")) {
					dims++;
					type = type.substring(0, type.length() - 2);
				}
				params.append("[".repeat(dims)).append(argumentDescriptor(type.trim()));
			}
		}
		return params.append(')').toString();
	}

	/**
	 * @param internalName
	 * 		Internal name of a class.
	 *
	 * @return Name as javac prints it in diagnostics: packages and nested classes are separated by dots.
	 */
	@Nonnull
	public static String keyOf(@Nonnull String internalName) {
		return internalName.replace('/', '.').replace('$', '.');
	}

	/**
	 * @param type
	 * 		One printed argument type, without array dimensions.
	 *
	 * @return Descriptor of the type.
	 */
	@Nonnull
	private String argumentDescriptor(@Nonnull String type) {
		String primitive = Types.sourceToPrimitive(type);
		if (primitive != null)
			return primitive;

		// javac prints '<null>' and similar placeholders for types it could not name. Object is always valid there.
		if (type.isEmpty() || type.startsWith("<"))
			return OBJECT_DESCRIPTOR;
		return "L" + internalNameOf(type) + ";";
	}

	/**
	 * @param printed
	 * 		Printed type or list of types.
	 *
	 * @return The text with every generic argument list removed, innermost first.
	 */
	@Nonnull
	private static String erase(@Nonnull String printed) {
		String erased = printed;
		for (String previous = null; !erased.equals(previous); ) {
			previous = erased;
			erased = erased.replaceAll("<[^<>]*>", "");
		}
		return erased;
	}

	/**
	 * @return Workspace classes keyed by their printed name.
	 * Built on first use, since most diagnostics only need the phantoms.
	 */
	@Nonnull
	private Map<String, JvmClassInfo> knownByKey() {
		if (knownByKey == null) {
			knownByKey = new HashMap<>();
			if (workspace != null)
				workspace.findJvmClasses(info -> true).forEach(path -> {
					JvmClassInfo info = path.getValue().asJvmClass();
					knownByKey.putIfAbsent(keyOf(info.getName()), info);
				});
		}
		return knownByKey;
	}
}
