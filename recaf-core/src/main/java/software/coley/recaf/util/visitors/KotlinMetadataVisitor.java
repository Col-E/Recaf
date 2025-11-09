package software.coley.recaf.util.visitors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.kotlin.metadata.ProtoBuf;
import org.objectweb.asm.AnnotationVisitor;
import software.coley.recaf.RecafConstants;

import java.util.List;
import java.util.function.Consumer;

/**
 * A visitor for extracting the values of kotlin {@code Metadata} annotations.
 * <br>
 * Reference:
 * <a href="https://github.com/JetBrains/kotlin/blob/master/libraries/stdlib/jvm/runtime/kotlin/Metadata.kt">Metadata.kt</a>
 *
 * @author Matt Coley
 */
public class KotlinMetadataVisitor extends AnnotationVisitor {
	private static final String KIND_NAME = "k";
	private static final String PACKAGE_NAME = "pn";
	private static final String METADATA_VERSION_NAME = "mv";
	private static final String BYTECODE_VERSION_NAME = "bv";
	private static final String DATA1_NAME = "d1";
	private static final String DATA2_NAME = "d2";
	private static final String EXTRA_STRING_NAME = "xs";
	private static final String EXTRA_INT_NAME = "xi";
	private final String owner;
	private final Consumer<KotlinMetadataVisitor> onComplete;
	private int kind;
	private String packageName;
	private int[] metadataVersion;
	private int[] bytecodeVersion;
	private String[] data1;
	private String[] data2;
	private String extraString;
	private int extraInt;

	/**
	 * New visitor.
	 *
	 * @param owner
	 * 		Internal name of defining class.
	 * @param visitor
	 * 		Parent visitor.
	 * @param onComplete
	 * 		Action to run when annotation is completely read.
	 */
	public KotlinMetadataVisitor(@Nonnull String owner,
	                             @Nullable AnnotationVisitor visitor,
	                             @Nullable Consumer<KotlinMetadataVisitor> onComplete) {
		super(RecafConstants.getAsmVersion(), visitor);
		this.owner = owner;
		this.onComplete = onComplete;
	}

	@Override
	public void visit(String name, Object value) {
		super.visit(name, value);
		if (KIND_NAME.equals(name) && value instanceof Integer)
			kind = ((Integer) value);
		else if (PACKAGE_NAME.equals(name) && value instanceof String)
			packageName = value.toString();
		else if (EXTRA_STRING_NAME.equals(name) && value instanceof String)
			extraString = value.toString();
		else if (EXTRA_INT_NAME.equals(name) && value instanceof Integer)
			extraInt = ((Integer) value);
			// Despite being arrays, ASM can call this visit for primitive array values... because why not lol
		else if (METADATA_VERSION_NAME.equals(name))
			metadataVersion = (int[]) value;
		else if (BYTECODE_VERSION_NAME.equals(name))
			bytecodeVersion = (int[]) value;
	}

	@Override
	public AnnotationVisitor visitArray(String name) {
		AnnotationVisitor visitor = super.visitArray(name);
		if (DATA1_NAME.equals(name)) {
			visitor = new AnnotationArrayVisitor<>(visitor,
					(List<String> contents) -> data1 = contents.toArray(new String[0]));
		} else if (DATA2_NAME.equals(name)) {
			visitor = new AnnotationArrayVisitor<>(visitor,
					(List<String> contents) -> data2 = contents.toArray(new String[0]));
		} else if (METADATA_VERSION_NAME.equals(name)) {
			visitor = new AnnotationArrayVisitor<>(visitor,
					(List<Integer> contents) -> metadataVersion = contents.stream().mapToInt(i -> i).toArray());
		} else if (BYTECODE_VERSION_NAME.equals(name)) {
			visitor = new AnnotationArrayVisitor<>(visitor,
					(List<Integer> contents) -> bytecodeVersion = contents.stream().mapToInt(i -> i).toArray());
		}
		return visitor;
	}

	@Override
	public void visitEnd() {
		super.visitEnd();
		if (onComplete != null)
			onComplete.accept(this);
	}

	/**
	 * @return Name of the class with the {@code @Metadata} annotation.
	 */
	@Nonnull
	public String getDefiningClass() {
		return owner;
	}

	/**
	 * The <i>"k"</i> field.
	 * <br>
	 * Possible values <i>(And how to parse them according to {@code kotlin/metadata/jvm/internal/JvmReadUtils.kt})</i> in order:
	 * <ol>
	 *      <li>Class: {@link ProtoBuf.Class}</li>
	 *      <li>File: {@link ProtoBuf.Package}</li>
	 *      <li>Synthetic class: {@link ProtoBuf.Function} <i>(Lambda)</i></li>
	 *      <li>Multi-file class facade</li>
	 *      <li>Multi-file class part</li>
	 * </ol>
	 * Generally for usage in this visitor, the value should always be {@code 1} for {@code Class}.
	 *
	 * @return Kind of file annotated.
	 */
	public int getKind() {
		return kind;
	}

	/**
	 * The <i>"pn"</i> field.
	 * <br>
	 * Fully qualified name of the package this class is located in, from Kotlin's point of view, or empty string if this name
	 * does not differ from the JVM's package FQ name. These names can be different in case the [JvmPackageName] annotation is used.
	 *
	 * @return Fully qualified name of the package this class is located in.
	 * Empty string if this name does not differ from the JVM's package FQ name.
	 */
	public String getPackageName() {
		return packageName;
	}

	/**
	 * The <i>"mv"</i> field.
	 *
	 * @return The version of the metadata provided in the arguments of this annotation.
	 */
	public int[] getMetadataVersion() {
		return metadataVersion;
	}

	/**
	 * The <i>"bv"</i> field.
	 *
	 * @return The version of the bytecode interface <i>(naming conventions, signatures)</i>
	 * of the class file annotated with this annotation.
	 */
	public int[] getBytecodeVersion() {
		return bytecodeVersion;
	}

	/**
	 * The <i>"d1"</i> field.
	 * <br>
	 * Contains the actual data model in a binary format, encoded into a string.
	 *
	 * @return Encoded data model.
	 */
	public String[] getData1() {
		return data1;
	}

	/**
	 * The <i>"d2"</i> field.
	 * <br>
	 * Contains the string table for resolving contents of {@link #getData1() "d1"}.
	 *
	 * @return The <i>"d2"</i> field. Contains the string table for resolving contents of {@link #getData1() "d1"}.
	 */
	public String[] getData2() {
		return data2;
	}

	/**
	 * The <i>"xs"</i> field.
	 *
	 * @return An extra string. For a multi-file part class, internal name of the facade class.
	 */
	public String getExtraString() {
		return extraString;
	}

	/**
	 * The <i>"xi"</i> field.
	 * <br>
	 * Bits of this number represent the following flags:
	 * <ul>
	 * <li>0 - This is a multi-file class facade or part, compiled with `-Xmultifile-parts-inherit`.</li>
	 * <li>1 - This class file is compiled by a pre-release version of Kotlin and is not visible to release versions.</li>
	 * <li>2 - This class file is a compiled Kotlin script source file (.kts).</li>
	 * <li>3 - "strict metadata version semantics". The metadata of this class file is not supposed to be read by the compiler, whose major.minor version is less than the major.minor version of this metadata ([metadataVersion]).</li>
	 * <li>4 - This class file is compiled with the new Kotlin compiler backend (JVM IR) introduced in Kotlin 1.4.</li>
	 * <li>5 - This class file has stable metadata and ABI. This is used only for class files compiled with JVM IR (see flag #4) or FIR (#6), and prevents metadata incompatibility diagnostics from being reported where the class is used.</li>
	 * <li>6 - This class file is compiled with the new Kotlin compiler frontend (FIR).</li>
	 * <li>7 - This class is used in the scope of an inline function and implicitly part of the public ABI. Only valid from metadata version 1.6.0.</li>
	 * </ul>
	 *
	 * @return An extra int. Different bits of the number represent different flags.
	 */
	public int getExtraInt() {
		return extraInt;
	}
}