package software.coley.recaf.util.kotlin;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.kotlin.metadata.MetadataNameResolver;
import org.jetbrains.kotlin.metadata.MetadataUtils;
import org.jetbrains.kotlin.metadata.ProtoBuf;
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf;
import org.jetbrains.kotlin.metadata.jvm.deserialization.BitEncoding;
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.slf4j.Logger;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.util.kotlin.model.KtClass;
import software.coley.recaf.util.kotlin.model.KtClassKind;
import software.coley.recaf.util.kotlin.model.KtConstructor;
import software.coley.recaf.util.kotlin.model.KtFunction;
import software.coley.recaf.util.kotlin.model.KtNullability;
import software.coley.recaf.util.kotlin.model.KtParameter;
import software.coley.recaf.util.kotlin.model.KtProperty;
import software.coley.recaf.util.kotlin.model.KtType;
import software.coley.recaf.util.kotlin.model.KtVariable;
import software.coley.recaf.util.visitors.KotlinMetadataVisitor;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Container for information extracted from Kotlin's {@code @Metadata} annotation.
 *
 * @author Matt Coley
 */
public class KotlinMetadata {
	private static final Logger logger = Logging.get(KotlinMetadata.class);

	/** Descriptor of metadata annotation */
	public static final String METADATA_DESC = "Lkotlin/Metadata;";
	private static final ExtensionRegistryLite EXTENSIONS = ExtensionRegistryLite.newInstance();
	private static int parseFailCount;
	private final Supplier<KtClass> ktModelSupplier;

	static {
		MetadataUtils.register(EXTENSIONS);
	}

	private KotlinMetadata(@Nonnull MetadataNameResolver resolver,
	                       @Nonnull KotlinMetadataVisitor visitor,
	                       @Nonnull Supplier<KtClass> ktModelSupplier) {
		this.ktModelSupplier = ktModelSupplier;
	}

	private KotlinMetadata(@Nonnull ProtoBuf.Class cls,
	                       @Nonnull MetadataNameResolver resolver,
	                       @Nonnull KotlinMetadataVisitor visitor) {
		this(resolver, visitor, () -> {
			KtClassKind kind = KtClassKind.fromKindInt(visitor.getKind());
			int extraFlags = visitor.getExtraInt();
			String fqName = cls.hasFqName() ? resolver.resolve(cls.getFqName()) : visitor.getDefiningClass();
			if (fqName != null)
				fqName = fqName.replace('.', '$');
			List<KtType> superTypes = cls.getSupertypeList().stream()
					.map(t -> mapType(resolver, t))
					.toList();
			String companionObjectName = cls.hasCompanionObjectName() ? resolver.resolve(cls.getCompanionObjectName()) : null;
			List<KtConstructor> constructors = newList(cls.getConstructorCount());
			for (var constructor : cls.getConstructorList())
				constructors.add(new KtConstructor(fqName, constructor.getValueParameterList().stream().map(vp -> mapParameter(resolver, vp)).toList()));
			List<KtProperty> properties = newList(cls.getPropertyCount());
			for (var property : cls.getPropertyList())
				properties.add(mapProperty(resolver, property));
			List<KtFunction> functions = newList(cls.getFunctionCount());
			for (var function : cls.getFunctionList())
				functions.add(mapFunction(resolver, function));
			return new KtClass(kind, extraFlags, fqName, companionObjectName,
					superTypes,
					constructors,
					properties,
					functions);
		});
	}

	private KotlinMetadata(@Nonnull ProtoBuf.Package pkg,
	                       @Nonnull MetadataNameResolver resolver,
	                       @Nonnull KotlinMetadataVisitor visitor) {
		this(resolver, visitor, () -> {
			KtClassKind kind = KtClassKind.fromKindInt(visitor.getKind());
			int extraFlags = visitor.getExtraInt();
			List<KtProperty> properties = newList(pkg.getPropertyCount());
			for (var property : pkg.getPropertyList())
				properties.add(mapProperty(resolver, property));
			List<KtFunction> functions = newList(pkg.getFunctionCount());
			for (var function : pkg.getFunctionList())
				functions.add(mapFunction(resolver, function));
			return new KtClass(kind, extraFlags, visitor.getDefiningClass(), null,
					Collections.emptyList(),
					Collections.emptyList(),
					properties,
					functions);
		});
	}

	private KotlinMetadata(@Nonnull ProtoBuf.Function func,
	                       @Nonnull MetadataNameResolver resolver,
	                       @Nonnull KotlinMetadataVisitor visitor) {
		this(resolver, visitor, () -> {
			KtClassKind kind = KtClassKind.fromKindInt(visitor.getKind());
			int extraFlags = visitor.getExtraInt();
			return new KtClass(kind, extraFlags, visitor.getDefiningClass(), null,
					Collections.emptyList(),
					Collections.emptyList(),
					Collections.emptyList(),
					Collections.singletonList(mapFunction(resolver, func)));
		});
	}

	@Nonnull
	private KtClass extractKtModel() {
		return ktModelSupplier.get();
	}

	/**
	 * @param bytes
	 * 		Bytes of class to extract Metadata from.
	 *
	 * @return Data extracted from visitor.
	 */
	@Nullable
	protected static KotlinMetadata extractMetadata(@Nonnull byte[] bytes) {
		return extractMetadata(new ClassReader(bytes));
	}

	/**
	 * @param reader
	 * 		Reader wrapper of class to extract Metadata from.
	 *
	 * @return Data extracted from visitor.
	 */
	@Nullable
	protected static KotlinMetadata extractMetadata(@Nonnull ClassReader reader) {
		String className = reader.getClassName();
		KotlinMetadata[] wrapper = new KotlinMetadata[1];
		reader.accept(new ClassVisitor(RecafConstants.getAsmVersion()) {
			@Override
			public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
				AnnotationVisitor visitor = super.visitAnnotation(descriptor, visible);
				if (METADATA_DESC.equals(descriptor))
					visitor = new KotlinMetadataVisitor(className, visitor, v -> wrapper[0] = extractMetadata(v));
				return visitor;
			}
		}, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
		return wrapper[0];
	}

	/**
	 * @param visitor
	 * 		Metadata visitor which has visited a class file.
	 *
	 * @return Data extracted from visitor.
	 */
	@Nullable
	protected static KotlinMetadata extractMetadata(@Nonnull KotlinMetadataVisitor visitor) {
		try {
			// Skip if visitor does not have data populated.
			String[] data1 = visitor.getData1();
			String[] data2 = visitor.getData2();
			if (data1 == null || data2 == null)
				return null;

			// Unpack "d1"
			byte[] data1Decoded = BitEncoding.decodeBytes(data1);
			ByteArrayInputStream data1Stream = new ByteArrayInputStream(data1Decoded);
			JvmProtoBuf.StringTableTypes types = JvmProtoBuf.StringTableTypes.parseDelimitedFrom(data1Stream, EXTENSIONS);

			// Wrap with "d2" so the human-readable names can be extracted
			MetadataNameResolver resolver = new MetadataNameResolver(types, data2);

			// Parse unpacked "d1" based on the metadata kind
			switch (visitor.getKind()) {
				case 1 /* Class */ -> {
					ProtoBuf.Class cls = ProtoBuf.Class.parseFrom(data1Stream, EXTENSIONS);
					return new KotlinMetadata(cls, resolver, visitor);
				}
				case 2 /* File -> Package */,
				     5 /* Multi-file class part -> Package */ -> {
					ProtoBuf.Package pkg = ProtoBuf.Package.parseFrom(data1Stream, EXTENSIONS);
					return new KotlinMetadata(pkg, resolver, visitor);
				}
				case 3 /* Synthetic Class -> Lambda -> Function */ -> {
					ProtoBuf.Function func = ProtoBuf.Function.parseFrom(data1Stream, EXTENSIONS);
					return new KotlinMetadata(func, resolver, visitor);
				}
				default -> throw new UnsupportedOperationException("Unsupported @Metadata type: " + visitor.getKind());
			}
		} catch (Exception ex) {
			// If the protobuf parser doesn't like the data, there's not really much that can be done about it.
			if (parseFailCount++ < 5)
				logger.warn("Kotlin @Metadata 'd1' format failed to parse", ex);
			return null;
		}
	}

	/**
	 * @param cls
	 * 		Class model of a kotlin class with a {@code @Metadata} annotation to extract information from.
	 *
	 * @return Extracted class model from the {@code @Metadata} or {@code null} if no annotation was found.
	 */
	@Nullable
	public static KtClass extractKtModel(@Nonnull ClassInfo cls) {
		if (cls instanceof JvmClassInfo jvm)
			return extractKtModel(jvm.getClassReader());
		return null;
	}

	/**
	 * @param bytes
	 * 		Bytecode of a kotlin class with a {@code @Metadata} annotation to extract information from.
	 *
	 * @return Extracted class model from the {@code @Metadata} or {@code null} if no annotation was found.
	 */
	@Nullable
	public static KtClass extractKtModel(@Nonnull byte[] bytes) {
		KotlinMetadata meta = extractMetadata(bytes);
		if (meta == null)
			return null;
		return meta.extractKtModel();
	}

	/**
	 * @param reader
	 * 		Class reader for a kotlin class with a {@code @Metadata} annotation to extract information from.
	 *
	 * @return Extracted class model from the {@code @Metadata} or {@code null} if no annotation was found.
	 */
	@Nullable
	public static KtClass extractKtModel(@Nonnull ClassReader reader) {
		KotlinMetadata meta = extractMetadata(reader);
		if (meta == null)
			return null;
		return meta.extractKtModel();
	}

	/**
	 * @param visitor
	 * 		Visitor of a kotlin class with a {@code @Metadata} annotation to extract information from.
	 *
	 * @return Extracted class model from the {@code @Metadata} or {@code null} if no annotation was found.
	 */
	@Nullable
	public static KtClass extractKtModel(@Nonnull KotlinMetadataVisitor visitor) {
		KotlinMetadata meta = extractMetadata(visitor);
		if (meta == null)
			return null;
		return meta.extractKtModel();
	}

	@Nonnull
	private static KtFunction mapFunction(@Nonnull MetadataNameResolver res, @Nonnull ProtoBuf.Function function) {
		String name = function.hasName() ? res.resolve(function.getName()) : null;
		KtType returnType = function.hasReturnType() ? mapType(res, function.getReturnType()) : null;
		List<KtVariable> parameters = function.getValueParameterList().stream().map(vp -> mapParameter(res, vp)).toList();
		return new KtFunction(name, returnType, parameters);
	}

	@Nonnull
	private static KtType mapType(@Nonnull MetadataNameResolver res, @Nonnull ProtoBuf.Type type) {
		String name;
		if (type.hasClassName())
			name = res.resolve(type.getClassName());
		else if (type.hasTypeParameter())
			name = res.resolve(type.getTypeParameter());
		else if (type.hasTypeParameterName())
			name = res.resolve(type.getTypeParameterName());
		else if (type.hasTypeAliasName())
			name = res.resolve(type.getTypeAliasName());
		else
			name = "Unknown";
		name = name.replace('.', '$');
		List<KtType> arguments = newList(type.getArgumentCount());
		for (var argument : type.getArgumentList())
			arguments.add(mapType(res, argument.getType()));
		KtNullability nullability;
		if (type.hasNullable())
			nullability = type.getNullable() ? KtNullability.NULLABLE : KtNullability.NONNULL;
		else
			nullability = KtNullability.UNKNOWN;
		return new KtType(name, arguments, nullability);
	}

	@Nonnull
	private static KtProperty mapProperty(@Nonnull MetadataNameResolver res, @Nonnull ProtoBuf.Property property) {
		String name = property.hasName() ? res.resolve(property.getName()) : null;
		KtType type = property.hasReturnType() ? mapType(res, property.getReturnType()) : null;
		return new KtProperty(name, type);
	}

	@Nonnull
	private static KtVariable mapVariable(@Nonnull MetadataNameResolver res, @Nonnull ProtoBuf.ValueParameter vp) {
		String name = vp.hasName() ? res.resolve(vp.getName()) : null;
		KtType type = vp.hasType() ? mapType(res, vp.getType()) : null;
		return new KtVariable(name, type);
	}

	@Nonnull
	private static KtVariable mapParameter(@Nonnull MetadataNameResolver res, @Nonnull ProtoBuf.ValueParameter vp) {
		String name = vp.hasName() ? res.resolve(vp.getName()) : null;
		KtType type = vp.hasType() ? mapType(res, vp.getType()) : null;
		return new KtParameter(name, type);
	}

	@Nonnull
	private static <T> List<T> newList(int size) {
		return size == 0 ? Collections.emptyList() : new ArrayList<>(size);
	}
}