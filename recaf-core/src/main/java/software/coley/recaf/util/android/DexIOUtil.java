package software.coley.recaf.util.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.file.DexHeader;
import me.darknet.dex.file.DexMap;
import me.darknet.dex.file.EncodedField;
import me.darknet.dex.file.EncodedMethod;
import me.darknet.dex.file.items.ClassDataItem;
import me.darknet.dex.file.items.ClassDefItem;
import me.darknet.dex.file.items.FieldItem;
import me.darknet.dex.file.items.MethodItem;
import me.darknet.dex.io.Input;
import me.darknet.dex.tree.DexFile;
import me.darknet.dex.tree.definitions.ClassDefinition;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.builder.AndroidClassInfoBuilder;
import software.coley.recaf.util.io.ByteSource;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.AndroidDexMetadata;
import software.coley.recaf.workspace.model.bundle.AndroidDexMetadata.DexClassLayout;
import software.coley.recaf.workspace.model.bundle.AndroidDexMetadata.DexEntryProperties;
import software.coley.recaf.workspace.model.bundle.BasicAndroidClassBundle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Dex file reading and writing.
 *
 * @author Matt Coley
 */
public class DexIOUtil {
	/**
	 * @param source
	 * 		Content source to read from. Must be wrapping a DEX file.
	 *
	 * @return Bundle of classes from the DEX file.
	 *
	 * @throws IOException
	 * 		When the DEX file cannot be read from.
	 */
	@Nonnull
	public static AndroidClassBundle read(@Nonnull ByteSource source) throws IOException {
		return read(source, DexEntryProperties.empty());
	}

	/**
	 * @param source
	 * 		Content source to read from. Must be wrapping a DEX file.
	 * @param entryProperties
	 * 		Properties of the source ZIP entry, or empty properties for standalone DEX files.
	 *
	 * @return Bundle of classes from the DEX file.
	 *
	 * @throws IOException
	 * 		When the DEX file cannot be read from.
	 */
	@Nonnull
	public static AndroidClassBundle read(@Nonnull ByteSource source,
	                                      @Nonnull DexEntryProperties entryProperties) throws IOException {
		return read(source.readAll(), entryProperties);
	}

	/**
	 * @param dex
	 * 		Raw bytes of a DEX file.
	 *
	 * @return Bundle of classes from the DEX file.
	 *
	 * @throws IOException
	 * 		When the DEX file cannot be read from.
	 */
	@Nonnull
	public static AndroidClassBundle read(@Nonnull byte[] dex) throws IOException {
		return read(dex, DexEntryProperties.empty());
	}

	/**
	 * @param dex
	 * 		Raw bytes of a DEX file.
	 * @param entryProperties
	 * 		Properties of the source ZIP entry, or empty properties for standalone DEX files.
	 *
	 * @return Bundle of classes from the DEX file.
	 *
	 * @throws IOException
	 * 		When the DEX file cannot be read from.
	 */
	@Nonnull
	private static AndroidClassBundle read(@Nonnull byte[] dex,
	                                       @Nonnull DexEntryProperties entryProperties) throws IOException {
		DexHeader header = DexHeader.CODEC.read(Input.wrap(dex));
		DexMap map = header.map();
		DexFile dexFile = DexFile.CODEC.map(header, map);

		// Retain class-definition order and hidden-API layout for later use.
		List<String> classOrder = dexFile.definitions().stream()
				.map(definition -> definition.getType().internalName())
				.toList();
		List<DexClassLayout> hiddenApiLayout = dexFile.hiddenApi() == null ? List.of() : getEncodedClassLayout(map);
		AndroidDexMetadata metadata = new AndroidDexMetadata(classOrder, hiddenApiLayout, dexFile.hiddenApi(), entryProperties);

		// Populate bundle.
		BasicAndroidClassBundle classBundle = new BasicAndroidClassBundle(header.version(), header.link(), metadata);
		for (ClassDefinition dexClass : dexFile.definitions()) {
			AndroidClassInfo classInfo = new AndroidClassInfoBuilder()
					.adaptFrom(dexClass)
					.build();
			classBundle.initialPut(classInfo);
		}
		return classBundle;
	}

	/**
	 * @param header
	 * 		DEX header whose encoded class definitions should be summarized.
	 *
	 * @return Encoded class/member layout in DEX class-definition order.
	 */
	@Nonnull
	public static List<DexClassLayout> getEncodedClassLayout(@Nonnull DexHeader header) {
		return getEncodedClassLayout(header.map());
	}

	/**
	 * @param map
	 * 		DEX map whose encoded class definitions should be summarized.
	 *
	 * @return Encoded class/member layout in DEX class-definition order.
	 */
	@Nonnull
	private static List<DexClassLayout> getEncodedClassLayout(@Nonnull DexMap map) {
		List<DexClassLayout> layouts = new ArrayList<>(map.classes().size());
		for (ClassDefItem classDef : map.classes()) {
			ClassDataItem classData = classDef.classData();
			List<String> staticFields = new ArrayList<>();
			List<String> instanceFields = new ArrayList<>();
			List<String> directMethods = new ArrayList<>();
			List<String> virtualMethods = new ArrayList<>();
			if (classData != null) {
				for (EncodedField field : classData.staticFields())
					staticFields.add(fieldSignature(field.field()));
				for (EncodedField field : classData.instanceFields())
					instanceFields.add(fieldSignature(field.field()));
				for (EncodedMethod method : classData.directMethods())
					directMethods.add(methodSignature(method.method()));
				for (EncodedMethod method : classData.virtualMethods())
					virtualMethods.add(methodSignature(method.method()));
			}
			layouts.add(new DexClassLayout(classDef.type().descriptor().string(), staticFields,
					instanceFields, directMethods, virtualMethods));
		}
		return List.copyOf(layouts);
	}

	@Nonnull
	private static String fieldSignature(@Nonnull FieldItem field) {
		return field.name().string() + field.type().descriptor().string();
	}

	@Nonnull
	private static String methodSignature(@Nonnull MethodItem method) {
		StringBuilder signature = new StringBuilder(method.name().string()).append('(');
		method.proto().parameters().types().forEach(type -> signature.append(type.descriptor().string()));
		return signature.append(')').append(method.proto().returnType().descriptor().string()).toString();
	}
}
