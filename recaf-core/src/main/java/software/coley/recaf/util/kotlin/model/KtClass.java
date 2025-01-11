package software.coley.recaf.util.kotlin.model;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.kotlin.KotlinMetadata;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Model of a class derived from {@link KotlinMetadata}.
 *
 * @author Matt Coley
 */
public non-sealed class KtClass implements KtElement {
	private final KtClassKind kind;
	private final int extraFlags;
	private final String name;
	private final String companionObjectName;
	private final List<KtType> superTypes;
	private final List<KtConstructor> constructors;
	private final List<KtProperty> properties;
	private final List<KtFunction> functions;

	/**
	 * @param kind
	 * 		Kind of class modeled by the metadata model.
	 * @param extraFlags
	 * 		Flags from the {@code "xi"} field of the metadata model.
	 * @param name
	 * 		Internal name of the class.
	 * @param companionObjectName
	 * 		Internal name of the companion object.
	 * @param superTypes
	 * 		List of internal names of extended and implemented types.
	 * @param constructors
	 * 		List of declared constructors in the class.
	 * @param properties
	 * 		List of declared properties in the class.
	 * @param functions
	 * 		List of declared functions in the class.
	 */
	public KtClass(@Nonnull KtClassKind kind,
	               int extraFlags,
	               @Nullable String name,
	               @Nullable String companionObjectName,
	               @Nonnull List<KtType> superTypes,
	               @Nonnull List<KtConstructor> constructors,
	               @Nonnull List<KtProperty> properties,
	               @Nonnull List<KtFunction> functions) {
		this.kind = kind;
		this.extraFlags = extraFlags;
		this.name = name;
		this.companionObjectName = companionObjectName;
		this.superTypes = superTypes;
		this.constructors = constructors;
		this.properties = properties;
		this.functions = functions;
	}

	/**
	 * @return Human legible string displaying the content of this class.
	 */
	@Nonnull
	public String toPrettyString() {
		String extendedTypes;
		if (superTypes.isEmpty())
			extendedTypes = "Object";
		else
			extendedTypes = superTypes.stream().map(KtType::toString).collect(Collectors.joining(", "));
		StringBuilder body = new StringBuilder();
		if (!properties.isEmpty())
			body.append("\n  // ==== Properties");
		for (KtProperty property : properties)
			body.append("\n    ").append(property).append(';');
		if (!constructors.isEmpty())
			body.append("\n  // ==== Constructors");
		for (KtConstructor property : constructors)
			body.append("\n    ").append(property).append(';');
		if (!functions.isEmpty())
			body.append("\n  // ==== Functions");
		for (KtFunction function : functions)
			body.append("\n    ").append(function).append(';');
		if (!body.isEmpty())
			body.append('\n');
		return "class " + StringUtil.shortenPath(Objects.requireNonNullElse(name, "?")) + " extends " + extendedTypes + " {" + body + "}";
	}

	/**
	 * @return Kind of class modeled by the metadata model.
	 */
	@Nonnull
	public KtClassKind getKind() {
		return kind;
	}

	/**
	 * Flag bits:
	 * <ul>
	 * <li>0 - this is a multi-file class facade or part, compiled with {@code -Xmultifile-parts-inherit}.</li>
	 * <li>1 - this class file is compiled by a pre-release version of Kotlin and is not visible to release versions.</li>
	 * <li>2 - this class file is a compiled Kotlin script source file ({@code .kts}).</li>
	 * <li>3 - "strict metadata version semantics". The metadata of this class file is not supposed to be read by the compiler,
	 * whose major.minor version is less than the major.minor version of this metadata ({@code [metadataVersion]}).</li>
	 * <li>4 - this class file is compiled with the new Kotlin compiler backend (JVM IR) introduced in Kotlin 1.4.</li>
	 * <li>5 - this class file has stable metadata and ABI. This is used only for class files compiled with JVM IR
	 * (see flag #4) or FIR (#6),and prevents metadata incompatibility diagnostics from being reported where the class is used.</li>
	 * <li>6 - this class file is compiled with the K2 compiler frontend (FIR). Only valid before metadata version 2.0.0.
	 * Starting from metadata version 2.0.0, this flag is not set anymore, even though FIR is always used.</li>
	 * <li>7 - this class is used in the scope of an inline function and implicitly part of the public ABI. Only valid from metadata version 1.6.0.</li>
	 * </ul>
	 *
	 * @return Flags from the {@code "xi"} field of the metadata model.
	 */
	public int getExtraFlags() {
		return extraFlags;
	}

	/**
	 * @return Internal name of the class.
	 */
	@Nullable
	public String getName() {
		return name;
	}

	/**
	 * @return Internal name of the companion object.
	 */
	@Nullable
	public String getCompanionObjectName() {
		return companionObjectName;
	}

	/**
	 * @return List of internal names of extended and implemented types.
	 */
	@Nonnull
	public List<KtType> getSuperTypes() {
		return superTypes;
	}

	/**
	 * @return List of declared constructors in the class.
	 */
	@Nonnull
	public List<KtConstructor> getConstructors() {
		return constructors;
	}

	/**
	 * @return List of declared properties in the class.
	 */
	@Nonnull
	public List<KtProperty> getProperties() {
		return properties;
	}

	/**
	 * @return List of declared functions in the class.
	 */
	@Nonnull
	public List<KtFunction> getFunctions() {
		return functions;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		KtClass that = (KtClass) o;

		if (!Objects.equals(name, that.name)) return false;
		if (!Objects.equals(companionObjectName, that.companionObjectName))
			return false;
		if (!superTypes.equals(that.superTypes)) return false;
		if (!constructors.equals(that.constructors)) return false;
		if (!properties.equals(that.properties)) return false;
		return functions.equals(that.functions);
	}

	@Override
	public int hashCode() {
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + (companionObjectName != null ? companionObjectName.hashCode() : 0);
		result = 31 * result + superTypes.hashCode();
		result = 31 * result + constructors.hashCode();
		result = 31 * result + properties.hashCode();
		result = 31 * result + functions.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return name;
	}
}
