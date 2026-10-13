package software.coley.recaf.services.compile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.phantom.GeneratedPhantomWorkspaceResource;
import software.coley.recaf.services.phantom.PhantomHint;
import software.coley.recaf.workspace.model.Workspace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static software.coley.recaf.services.compile.JavacPrintedTypes.keyOf;

/**
 * Translates the errors javac reports against generated phantoms into {@link PhantomHint hints}.
 *
 * @author Matt Coley
 * @see JavacPhantomRefiner
 */
final class JavacDiagnosticHints {
	/** javac's diagnostic for an import or qualified reference to a package with no classes. */
	private static final Pattern MISSING_PACKAGE = Pattern.compile("package ([\\w.]+) does not exist");
	/** javac's diagnostic for a member class that its outer class declares but whose class file is missing. */
	private static final Pattern CLASS_FILE_MISSING = Pattern.compile("class file for ([\\w$.]+) not found");
	/** javac's diagnostic for a constructor call whose arguments match no constructor, with the arguments it was given. */
	private static final Pattern CONSTRUCTOR_NOT_APPLICABLE = Pattern.compile("constructor [\\w$.]+ in (?:class|enum) ([\\w$.]+) cannot be applied to given types[\\s\\S]*?found:\\s*([^\\r\\n]*)");
	/** javac's diagnostic for a constructor call when the class has other constructors, with the arguments it was given. */
	private static final Pattern NO_SUITABLE_CONSTRUCTOR = Pattern.compile("no suitable constructor found for ([\\w$.]+)\\(([^)]*)\\)");
	/** javac's "{@code cannot find symbol}" diagnostic for a method or field, with its printed signature when it is a method. */
	private static final Pattern MISSING_MEMBER = Pattern.compile("cannot find symbol[\\s\\S]*?symbol:\\s+(method|variable)\\s+([\\w$]+)(?:\\(([^)]*)\\))?");
	/** Where javac was looking for a member. A variable location means an instance member, and a class location a static one. */
	private static final Pattern MEMBER_LOCATION = Pattern.compile("location:\\s+(variable\\s+[\\w$]+\\s+of\\s+type|class|interface)\\s+([\\w$.]+)");
	/** javac's "{@code cannot find symbol}" diagnostic for a missing class, with the simple name it was looking for. */
	private static final Pattern MISSING_CLASS = Pattern.compile("cannot find symbol[\\s\\S]*?symbol:\\s+class\\s+([\\w$.]+)");
	/** Where javac was looking for a missing class (package, or a class). */
	private static final Pattern MISSING_LOCATION = Pattern.compile("location:\\s+(class|interface|package)\\s+([\\w$.]+)");
	/** javac's diagnostic for a checked exception thrown by a call that the code does not handle. */
	private static final Pattern UNREPORTED_EXCEPTION = Pattern.compile("unreported exception ([\\w$.]+); must be caught or declared to be thrown");
	/** javac's diagnostic for a catch clause naming an exception that its try block cannot throw. */
	private static final Pattern NEVER_THROWN = Pattern.compile("exception ([\\w$.]+) is never thrown in body of corresponding try statement");
	/** javac's diagnostic for a value of a phantom member's {@code Object} type used as another type. */
	private static final Pattern OBJECT_TO_TYPE = Pattern.compile("incompatible types: java\\.lang\\.Object cannot be converted to ([\\w$.\\[\\]<>]+)");
	/** javac's "{@code A cannot be converted to B}" diagnostic. Names are printed with dots for packages and nesting. */
	private static final Pattern INCOMPATIBLE_TYPES = Pattern.compile("incompatible types: ([\\w$.]+) cannot be converted to ([\\w$.]+)");
	/** How many lines above a catch clause to search for the call in its try block. */
	private static final int TRY_BLOCK_SEARCH_LINES = 8;
	private static final String OBJECT_DESCRIPTOR = "Ljava/lang/Object;";
	private final Map<String, String> sources;
	private final Collection<String> sourceTexts;
	private final Map<String, String[]> sourceLines;
	private final Set<String> starPackages;
	private final JavacPrintedTypes types;
	private final List<PhantomHint> existing;
	private final Set<PhantomHint> found = new LinkedHashSet<>();

	/**
	 * @param sources
	 * 		Sources used for type and package lookups, keyed by internal class name.
	 * @param lineSources
	 * 		Source text used by the compilation that produced the diagnostics, keyed by internal class name.
	 * @param types
	 * 		Resolver for the type names javac prints.
	 * @param existing
	 * 		Hints already applied.
	 */
	private JavacDiagnosticHints(@Nonnull Map<String, String> sources, @Nonnull Map<String, String> lineSources,
	                             @Nonnull JavacPrintedTypes types, @Nonnull List<PhantomHint> existing) {
		this.sources = sources;
		this.types = types;
		this.existing = existing;


		Map<String, String[]> lines = new HashMap<>(lineSources.size());
		lineSources.forEach((name, source) -> lines.put(name, JavaSourceScan.lines(source)));
		sourceTexts = sources.values();
		sourceLines = lines;
		starPackages = JavaSourceScan.starImportPackages(sourceTexts);
	}

	/**
	 * @param diagnostics
	 * 		Diagnostics of one compilation against generated phantoms.
	 * @param phantoms
	 * 		Phantoms that compilation was given.
	 * @param sources
	 * 		Sources used for type and package lookups, keyed by internal class name.
	 * @param lineSources
	 * 		Source text used by the compilation that produced the diagnostics, keyed by internal class name.
	 * @param workspace
	 * 		Workspace the compilation ran against.
	 * @param existing
	 * 		Hints already applied, which are not repeated.
	 * 		Member hints among them are the ones that later hints refine.
	 *
	 * @return New hints for what javac reported, in diagnostic order and without duplicates.
	 */
	@Nonnull
	public static List<PhantomHint> collect(@Nonnull List<CompilerDiagnostic> diagnostics,
	                                        @Nonnull GeneratedPhantomWorkspaceResource phantoms,
	                                        @Nonnull Map<String, String> sources,
	                                        @Nonnull Map<String, String> lineSources,
	                                        @Nullable Workspace workspace,
	                                        @Nonnull List<PhantomHint> existing) {
		JavacDiagnosticHints collector = new JavacDiagnosticHints(sources, lineSources, new JavacPrintedTypes(phantoms, workspace), existing);
		for (CompilerDiagnostic diagnostic : diagnostics)
			if (diagnostic.level() == CompilerDiagnostic.Level.ERROR)
				collector.handle(diagnostic);
		return new ArrayList<>(collector.found);
	}

	/**
	 * javac stops at an on-demand import of a package with no classes. We get nothing useful when this happens.
	 * Collects the packages of such imports, so they can be removed and the compilation retried to reveal which types are missing.
	 *
	 * @param diagnostics
	 * 		Diagnostics of one compilation.
	 * @param sources
	 * 		Sources of that compilation.
	 *
	 * @return Dotted packages of on-demand imports that javac reported as having no classes.
	 */
	@Nonnull
	public static Set<String> absentStarPackages(@Nonnull List<CompilerDiagnostic> diagnostics,
	                                             @Nonnull Collection<String> sources) {
		Set<String> imported = JavaSourceScan.starImportPackages(sources);
		Set<String> absent = new TreeSet<>();
		for (CompilerDiagnostic diagnostic : diagnostics) {
			if (diagnostic.level() != CompilerDiagnostic.Level.ERROR)
				continue;
			Matcher missing = MISSING_PACKAGE.matcher(diagnostic.message());
			if (missing.find() && imported.contains(missing.group(1)))
				absent.add(missing.group(1));
		}
		return absent;
	}

	/**
	 * Passes the diagnostic to the first handler that recognizes it.
	 *
	 * @param diagnostic
	 * 		Error diagnostic to handle.
	 *
	 * @return {@code true} when a handler recognized the diagnostic.
	 */
	private boolean handle(@Nonnull CompilerDiagnostic diagnostic) {
		String message = diagnostic.message();
		return missingPackage(diagnostic, message)
				|| missingClassFile(message)
				|| missingConstructor(message)
				|| missingMember(message)
				|| missingClass(message)
				|| unreportedException(diagnostic, message)
				|| neverThrownException(diagnostic, message)
				|| objectConversion(diagnostic, message)
				|| incompatibleTypes(message);
	}

	/**
	 * A qualified reference into a package with no classes. javac names the package only, so the class name is read
	 * from the reported line. On-demand imports of such packages are handled by {@link JavacPhantomRefiner}, since
	 * their names come from usages.
	 *
	 * @return {@code true} when the diagnostic was recognized.
	 */
	private boolean missingPackage(@Nonnull CompilerDiagnostic diagnostic, @Nonnull String message) {
		// Can't guess the class name when the package is imported with a star import.
		Matcher absentPackage = MISSING_PACKAGE.matcher(message);
		if (!absentPackage.find() || starPackages.contains(absentPackage.group(1)))
			return false;

		// Extract the class name from the qualified reference on the reported line, and create a phantom for it
		// if the reference isn't already satisfied by a source or known type.
		String pkg = absentPackage.group(1);
		Pattern qualified = Pattern.compile("\\b" + Pattern.quote(pkg) + "\\.([A-Za-z_$][\\w$]*)");
		Matcher reference = qualified.matcher(lineAt(diagnostic, diagnostic.line()));
		while (reference.find()) {
			String name = pkg.replace('.', '/') + "/" + reference.group(1);
			if (!sources.containsKey(name) && !types.isKnown(name))
				add(new PhantomHint.TypeOnly(name));
		}
		return true;
	}

	/**
	 * A nested class that javac can see in its outer class but cannot load. The binary name is in the message.
	 *
	 * @return {@code true} when the diagnostic was recognized.
	 */
	private boolean missingClassFile(@Nonnull String message) {
		Matcher classFile = CLASS_FILE_MISSING.matcher(message);
		if (!classFile.find())
			return false;

		String name = classFile.group(1).replace('.', '/');
		if (!sources.containsKey(name) && !types.isKnown(name))
			add(new PhantomHint.TypeOnly(name));
		return true;
	}

	/**
	 * A constructor call that matches no constructor of a generated phantom. Its printed arguments give the descriptor.
	 *
	 * @return {@code true} when the diagnostic was recognized.
	 */
	private boolean missingConstructor(@Nonnull String message) {
		// The two patterns are for different diagnostics, but they both report
		// that the constructor is missing and give the arguments it was called with.
		Matcher applied = CONSTRUCTOR_NOT_APPLICABLE.matcher(message);
		Matcher suitable = NO_SUITABLE_CONSTRUCTOR.matcher(message);
		Matcher constructor = applied.find() ? applied : suitable.find() ? suitable : null;
		if (constructor == null)
			return false;

		// So we have the printed arguments, but we need the owner to make a hint.
		// The owner is the class whose constructor was called.
		String owner = types.phantomName(constructor.group(1));
		if (owner == null || sources.containsKey(owner) || isInterfacePhantom(owner))
			return true;

		// Map the args to a descriptor, and add a constructor hint for the phantom.
		String printed = constructor.group(2);
		String parameters = types.parameterDescriptor(printed.equals("no arguments") ? "" : printed);
		if (parameters != null)
			add(new PhantomHint.Member(owner, "<init>", parameters + "V", false, false));
		return true;
	}

	/**
	 * A member javac cannot find on a generated phantom. The member is declared with its printed argument types. Its
	 * return and field types are not reported by javac, so they are left as {@code Object} until
	 * {@link #objectConversion(CompilerDiagnostic, String) a use} says otherwise.
	 *
	 * @return {@code true} when the diagnostic was recognized.
	 */
	private boolean missingMember(@Nonnull String message) {
		// Need to know what kind of member it is, and what its name is.
		Matcher member = MISSING_MEMBER.matcher(message);
		if (!member.find())
			return false;

		// And where javac was looking for it, to know what class to add the member to.
		Matcher where = MEMBER_LOCATION.matcher(message);
		String owner = where.find() ? types.phantomName(where.group(2)) : null;
		if (owner == null || sources.containsKey(owner))
			return true;

		// Determine what kind of member it is from the diagnostic, and add a hint for it.
		boolean field = member.group(1).equals("variable");
		boolean isStatic = !where.group(1).startsWith("variable");
		String descriptor = field ? OBJECT_DESCRIPTOR : types.methodDescriptor(member.group(3));
		if (descriptor != null)
			add(new PhantomHint.Member(owner, member.group(2), descriptor, field, isStatic));
		return true;
	}

	/**
	 * A class javac cannot find. Creating the phantom is the whole fix, so its relations are left to the generator.
	 *
	 * @return {@code true} when the diagnostic was recognized.
	 */
	private boolean missingClass(@Nonnull String message) {
		Matcher missing = MISSING_CLASS.matcher(message);
		if (!missing.find())
			return false;

		String name = missingClassName(missing.group(1), message);
		if (name != null && !sources.containsKey(name) && !types.isKnown(name))
			add(new PhantomHint.TypeOnly(name));
		return true;
	}

	/**
	 * A checked exception that a phantom method's call throws, which the code does not handle.
	 * The method named on the reported line declares it.
	 *
	 * @return {@code true} when the diagnostic was recognized.
	 */
	private boolean unreportedException(@Nonnull CompilerDiagnostic diagnostic, @Nonnull String message) {
		Matcher unreported = UNREPORTED_EXCEPTION.matcher(message);
		if (!unreported.find())
			return false;

		PhantomHint declared = declaringException(lineAt(diagnostic, diagnostic.line()), unreported.group(1));
		if (declared != null)
			add(declared);
		return true;
	}

	/**
	 * A catch clause for an exception that its try block cannot throw. javac reports the catch clause and not the
	 * call, so the call is found by searching upward from the catch for a line that names a phantom method.
	 *
	 * @return {@code true} when the diagnostic was recognized.
	 */
	private boolean neverThrownException(@Nonnull CompilerDiagnostic diagnostic, @Nonnull String message) {
		Matcher neverThrown = NEVER_THROWN.matcher(message);
		if (!neverThrown.find())
			return false;

		// Look back through the try block for a call to a phantom method that can throw the exception.
		int lowest = Math.max(1, diagnostic.line() - TRY_BLOCK_SEARCH_LINES);
		for (int line = diagnostic.line() - 1; line >= lowest; line--) {
			// Skip if the line does not name a phantom method.
			String text = lineAt(diagnostic, line);
			if (memberOnLine(text, candidate -> !candidate.field()) == null)
				continue;

			// The line names a phantom method, so declare the exception on it.
			PhantomHint declared = declaringException(text, neverThrown.group(1));
			if (declared != null)
				add(declared);
			break;
		}
		return true;
	}

	/**
	 * A value of a phantom member's {@code Object} type that javac needs as another type.
	 * The member named on the reported line is retyped.
	 *
	 * @return {@code true} when the diagnostic was recognized.
	 */
	private boolean objectConversion(@Nonnull CompilerDiagnostic diagnostic, @Nonnull String message) {
		Matcher objectTarget = OBJECT_TO_TYPE.matcher(message);
		if (!objectTarget.find())
			return false;

		PhantomHint refined = refinedMember(diagnostic, objectTarget.group(1));
		if (refined != null)
			add(refined);
		return true;
	}

	/**
	 * A phantom that javac cannot convert to another type. The phantom is made a subtype of the target.
	 *
	 * @return {@code true} when the diagnostic was recognized.
	 */
	private boolean incompatibleTypes(@Nonnull String message) {
		Matcher matcher = INCOMPATIBLE_TYPES.matcher(message);
		if (!matcher.find())
			return false;

		// The subtype is the phantom, and the supertype is the type javac needs it to be.
		// If the subtype is a non-phantom, we can't do anything about it.
		String subtype = types.phantomName(matcher.group(1));
		if (subtype == null || sources.containsKey(subtype))
			return true;

		// However if it is a phantom, then we can add a hint that it is a subtype of the supertype.
		// We need to know if the relation is via extension or implementation, so we check if the supertype is an interface.
		String supertype = types.phantomName(matcher.group(2));
		boolean supertypeIsInterface;
		if (supertype != null) {
			supertypeIsInterface = isInterfacePhantom(supertype);
		} else {
			JvmClassInfo known = types.known(matcher.group(2));
			if (known == null)
				return true;
			supertype = known.getName();
			supertypeIsInterface = known.hasInterfaceModifier();
		}

		// A superclass must be a class, and an interface cannot have one. Interfaces may be implemented by either.
		if (sources.containsKey(supertype) || (!supertypeIsInterface && isInterfacePhantom(subtype)))
			return true;
		add(new PhantomHint.Supertype(subtype, supertype));
		return true;
	}

	/**
	 * @param simpleName
	 * 		Name javac was looking for, as written in the source.
	 * @param message
	 * 		Diagnostic message, which says where javac was looking.
	 *
	 * @return Internal name javac was looking for, or {@code null} when the diagnostic's location does not determine it.
	 */
	@Nullable
	private String missingClassName(@Nonnull String simpleName, @Nonnull String message) {
		Matcher location = MISSING_LOCATION.matcher(message);
		if (!location.find())
			return null;
		String kind = location.group(1);
		String where = location.group(2);
		if (kind.equals("package"))
			return where.replace('.', '/') + "/" + simpleName;

		// A simple name inside a class being compiled resolves in its package, or through its on-demand imports.
		for (Map.Entry<String, String> source : sources.entrySet()) {
			if (!keyOf(source.getKey()).equals(where))
				continue;
			List<String> sourceStarPackages = JavaSourceScan.starImportPackages(source.getValue());
			if (sourceStarPackages.isEmpty())
				return JavaSourceScan.packagePrefix(source.getValue()) + simpleName;

			// One on-demand import is the strongest guess. Two or more are ambiguous, and hinting several would make
			// javac report the name as ambiguous rather than missing. The package the workspace already has classes in
			// is the likelier home, with import order breaking ties.
			String chosen = sourceStarPackages.getFirst();
			if (sourceStarPackages.size() > 1) {
				int best = -1;
				for (String candidate : sourceStarPackages) {
					int count = types.packageMemberCount(candidate.replace('.', '/'));
					if (count > best) {
						best = count;
						chosen = candidate;
					}
				}
			}
			return chosen.replace('.', '/') + "/" + simpleName;
		}

		// Otherwise the name is a member of a class that already exists.
		String owner = types.resolve(where);
		return owner == null ? null : owner + "$" + simpleName;
	}

	/**
	 * Retypes the one phantom member named on a diagnostic's line that still returns or holds {@code Object}.
	 * If more than one such member is named there, the line does not say which one javac meant, so none is retyped.
	 *
	 * @param diagnostic
	 * 		Diagnostic reporting the conversion.
	 * @param printedType
	 * 		Type the value was converted to, as javac printed it.
	 *
	 * @return Member hint with the refined type, or {@code null} when no single member can be identified.
	 */
	@Nullable
	private PhantomHint refinedMember(@Nonnull CompilerDiagnostic diagnostic, @Nonnull String printedType) {
		String target = types.descriptorOf(printedType);
		if (target == null)
			return null;

		// Get the text on the given line, and check if it names a single
		// phantom member that still has Object as its return or field type.
		String text = lineAt(diagnostic, diagnostic.line());
		PhantomHint.Member chosen = memberOnLine(text, member -> member.field()
				? member.descriptor().equals(OBJECT_DESCRIPTOR)
				: member.descriptor().endsWith(")" + OBJECT_DESCRIPTOR));
		if (chosen == null)
			return null;

		// We got an object return, so we can refine the member's type to the target type.
		String descriptor = chosen.field() ? target : chosen.parameterDescriptor() + target;
		return new PhantomHint.Member(chosen.phantomName(), chosen.name(), descriptor, chosen.field(), chosen.staticMember());
	}

	/**
	 * @param text
	 * 		Source line naming the method that throws.
	 * @param exception
	 * 		Exception as javac printed it.
	 *
	 * @return A method hint that declares the exception, derived from the method named on the line,
	 * or {@code null} when no single method is named there, or it already declares the exception.
	 */
	@Nullable
	private PhantomHint declaringException(@Nonnull String text, @Nonnull String exception) {
		PhantomHint.Member chosen = memberOnLine(text, member -> !member.field());
		if (chosen == null)
			return null;

		// If the exception is already declared, we don't need to add it again.
		String internal = types.internalNameOf(exception);
		if (chosen.exceptions().contains(internal))
			return null;

		// Add the exception to the method's list of declared exceptions.
		List<String> exceptions = new ArrayList<>(chosen.exceptions());
		exceptions.add(internal);
		return new PhantomHint.Member(chosen.phantomName(), chosen.name(),
				chosen.descriptor(), false, chosen.staticMember(), exceptions);
	}

	/**
	 * Selects the one existing member hint named on a line of source.
	 *
	 * @param text
	 * 		Source line.
	 * @param accept
	 * 		Filter for the members to consider.
	 *
	 * @return The chosen hint, or {@code null} when no single member can be identified.
	 */
	@Nullable
	private PhantomHint.Member memberOnLine(@Nonnull String text, @Nonnull Predicate<PhantomHint.Member> accept) {
		// Get the latest hints for each member, and filter them to the ones whose name is on the line.
		Map<String, PhantomHint.Member> latest = new TreeMap<>();
		for (PhantomHint hint : existing) {
			if (!(hint instanceof PhantomHint.Member member) || !accept.test(member))
				continue;
			latest.put(member.phantomName() + "#" + member.name() + "#" + member.field() + "#" + member.parameterDescriptor(), member);
		}

		// Find the ones whose name is on the line, and among those the ones whose phantom's printed name is on the line.
		Map<String, PhantomHint.Member> owned = new TreeMap<>();
		Map<String, PhantomHint.Member> named = new TreeMap<>();
		for (Map.Entry<String, PhantomHint.Member> entry : latest.entrySet()) {
			PhantomHint.Member member = entry.getValue();
			Pattern use = member.field()
					? Pattern.compile("\\." + Pattern.quote(member.name()) + "\\b(?!\\s*\\()")
					: Pattern.compile("\\b" + Pattern.quote(member.name()) + "\\s*\\(");
			if (!use.matcher(text).find())
				continue;
			named.put(entry.getKey(), member);
			if (Pattern.compile("\\b" + Pattern.quote(keyOf(member.phantomName())) + "\\b").matcher(text).find())
				owned.put(entry.getKey(), member);
		}

		// If there is only one member whose name is on the line, and it is the only one whose phantom's printed name
		// is on the line, it is safe to assume javac meant that one. Otherwise, its ambiguous.
		Map<String, PhantomHint.Member> candidates = owned.isEmpty() ? named : owned;
		return candidates.size() == 1 ? candidates.values().iterator().next() : null;
	}

	/**
	 * @param diagnostic
	 * 		Diagnostic whose compilation unit owns the requested line.
	 * @param line
	 * 		One-based line number.
	 *
	 * @return Text of the requested line, or an empty string when the source is unknown or the line is out of range.
	 */
	@Nonnull
	private String lineAt(@Nonnull CompilerDiagnostic diagnostic, int line) {
		return JavaSourceScan.lineAt(sourceLines, diagnostic.sourceName(), line);
	}

	/**
	 * @param phantomName
	 * 		Internal name of a generated phantom.
	 *
	 * @return {@code true} when the phantom is an interface.
	 */
	private boolean isInterfacePhantom(@Nonnull String phantomName) {
		JvmClassInfo phantom = types.phantom(phantomName);
		return phantom != null && phantom.hasInterfaceModifier();
	}

	/**
	 * Records a hint unless it was already applied or already found.
	 *
	 * @param hint
	 * 		Hint to record.
	 */
	private void add(@Nonnull PhantomHint hint) {
		if (!existing.contains(hint))
			found.add(hint);
	}
}
