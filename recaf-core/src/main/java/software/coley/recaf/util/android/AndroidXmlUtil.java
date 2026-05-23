package software.coley.recaf.util.android;

import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceFile;
import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceValue;
import com.google.devrel.gmscore.tools.apk.arsc.Chunk;
import com.google.devrel.gmscore.tools.apk.arsc.ChunkWithChunks;
import com.google.devrel.gmscore.tools.apk.arsc.StringPoolChunk;
import com.google.devrel.gmscore.tools.apk.arsc.XmlStartElementChunk;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.BinaryXmlFileInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

/**
 * Android binary XML utilities.
 *
 * @author Matt Coley
 */
public final class AndroidXmlUtil {
	// Pulled from 'Chunk.Type.STRING_POOL.code()' and such - but we need to keep these
	// as constants to make the switch recognize them as 'constants'.
	private static final int RES_STRING_POOL_TYPE = 0x0001;
	private static final int RES_XML_START_NAMESPACE_TYPE = 0x0100;
	private static final int RES_XML_END_NAMESPACE_TYPE = 0x0101;
	private static final int RES_XML_START_ELEMENT_TYPE = 0x0102;
	private static final int RES_XML_END_ELEMENT_TYPE = 0x0103;
	private static final int RES_XML_CDATA_TYPE = 0x0104;
	private static final int RES_XML_RESOURCE_MAP_TYPE = 0x0180;
	private static final int COMPLEX_RADIX_MASK = 0x3;
	private static final int COMPLEX_RADIX_SHIFT = 4;
	private static final int COMPLEX_UNIT_MASK = 0xF;
	private static final float[] COMPLEX_RADIX_MULTIPLIERS = {
			1.0f / (1 << 8),
			1.0f / (1 << 15),
			1.0f / (1 << 23),
			1.0f / (1L << 31)
	};
	private static final String[] DIMENSION_UNITS = {"px", "dp", "sp", "pt", "in", "mm"};
	private static final String[] FRACTION_UNITS = {"%", "%p"};

	/**
	 * Name of the Android manifest file.
	 */
	public static final String MANIFEST_NAME = "AndroidManifest.xml";

	private AndroidXmlUtil() {}

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Resource containing the manifest file.
	 *
	 * @return Start elements of {@value #MANIFEST_NAME} in the given resource, or an empty list when the manifest is absent.
	 */
	@Nonnull
	public static List<XmlElementData> getManifestStartElements(@Nonnull Workspace workspace,
	                                                            @Nonnull WorkspaceResource resource) {
		return getStartElements(workspace, resource, MANIFEST_NAME);
	}

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Resource containing the XML file.
	 * @param fileName
	 * 		XML file name inside the resource.
	 *
	 * @return Start elements of the requested binary XML file, or an empty list when unavailable.
	 */
	@Nonnull
	public static List<XmlElementData> getStartElements(@Nonnull Workspace workspace,
	                                                    @Nonnull WorkspaceResource resource,
	                                                    @Nonnull String fileName) {
		// Check if the file exists and is a binary XML file.
		FileInfo file = resource.getFileBundle().get(fileName);
		if (!(file instanceof BinaryXmlFileInfo xmlInfo))
			return List.of();

		// Check if the file has a string pool, which is required to resolve element and attribute names.
		StringPoolChunk stringChunk = xmlInfo.getStringPoolChunk();
		if (stringChunk == null)
			return List.of();

		// Walk through all chunks and their children to find start element chunks.
		BinaryResourceFile chunkModel = xmlInfo.getChunkModel();
		FilePathNode filePath = PathNodes.filePath(workspace, resource, resource.getFileBundle(), xmlInfo);
		List<XmlElementData> elements = new ArrayList<>();
		Queue<Chunk> chunkQueue = new ArrayDeque<>(chunkModel.getChunks());
		while (!chunkQueue.isEmpty()) {
			Chunk chunk = chunkQueue.remove();
			if (chunk instanceof XmlStartElementChunk start)
				elements.add(new XmlElementData(filePath, stringChunk, start));
			if (chunk instanceof ChunkWithChunks chunkWithChunks)
				chunkQueue.addAll(chunkWithChunks.getChunks().values());
		}
		return elements;
	}

	/**
	 * @param stringPool
	 * 		String pool to query.
	 * @param index
	 * 		String index.
	 *
	 * @return String value, or {@code null} when the index is invalid.
	 */
	@Nullable
	public static String getString(@Nullable StringPoolChunk stringPool, int index) {
		if (stringPool == null || index < 0 || index >= stringPool.getStringCount())
			return null;
		return stringPool.getString(index);
	}

	/**
	 * @param data
	 * 		Raw file content.
	 *
	 * @return {@code true} when the content structurally resembles Android binary XML even if the root chunk type is malformed.
	 */
	public static boolean hasXmlIndicators(@Nullable byte[] data) {
		// Skip if there isn't enough data to read the header and root chunk type.
		if (data == null || data.length < 16)
			return false;

		// Check if the header size and total size fields are within bounds, and that the root chunk type is one of the expected XML chunk types.
		int headerSize = getUnsignedShort(data, 2);
		int totalSize = getInt(data, 4);
		if (headerSize < 8 || headerSize > data.length || totalSize < headerSize || totalSize > data.length)
			return false;

		// Skip if there isn't enough data to read the root chunk type.
		if (headerSize + 8 > data.length)
			return false;

		// Check if the root chunk type is one of the expected XML chunk types.
		return switch (getUnsignedShort(data, headerSize)) {
			case RES_STRING_POOL_TYPE,
			     RES_XML_START_NAMESPACE_TYPE,
			     RES_XML_END_NAMESPACE_TYPE,
			     RES_XML_START_ELEMENT_TYPE,
			     RES_XML_END_ELEMENT_TYPE,
			     RES_XML_CDATA_TYPE,
			     RES_XML_RESOURCE_MAP_TYPE -> true;
			default -> false;
		};
	}

	@Nonnull
	public static String formatValue(@Nonnull AndroidRes resources, @Nonnull AndroidRes.ResourceEntry entry) {
		BinaryResourceValue value = entry.simpleValue();
		if (value == null)
			return "";
		String stringValue = entry.stringValue();
		if (stringValue != null)
			return stringValue;
		String path = entry.resourcePath();
		if (path != null)
			return path;
		return formatBinaryValue(resources, value);
	}

	/**
	 * @param resources
	 * 		Android resources for resolving resource IDs.
	 * @param value
	 * 		Binary value to format.
	 *
	 * @return Readable representation of the binary value.
	 */
	@Nonnull
	public static String formatBinaryValue(@Nonnull AndroidRes resources, @Nonnull BinaryResourceValue value) {
		return switch (value.type()) {
			case REFERENCE, DYNAMIC_REFERENCE -> {
				String name = getResName(resources, value.data());
				yield name == null ? "@" + formatResourceId(value.data()) : "@" + name + " (" + formatResourceId(value.data()) + ")";
			}
			case ATTRIBUTE, DYNAMIC_ATTRIBUTE -> {
				String name = getResName(resources, value.data());
				yield name == null ? "?" + formatResourceId(value.data()) : "?" + name + " (" + formatResourceId(value.data()) + ")";
			}
			case STRING -> Objects.requireNonNullElseGet(resources.getString(value.data()), () -> "string@" + value.data());
			case FLOAT -> formatFloat(Float.intBitsToFloat(value.data()));
			case DIMENSION -> formatComplexValue(value.data(), DIMENSION_UNITS);
			case FRACTION -> formatFraction(value.data());
			case NULL -> switch (value.data()) {
				case 0 -> "null";
				case 1 -> "empty";
				default -> "null(" + formatResourceId(value.data()) + ")";
			};
			case INT_DEC -> Integer.toString(value.data());
			case INT_BOOLEAN -> value.data() == 0 ? "false" : "true";
			case INT_HEX -> formatResourceId(value.data());
			case INT_COLOR_ARGB8 -> "#%08X".formatted(value.data());
			case INT_COLOR_RGB8 -> "#%06X".formatted(value.data() & 0xFFFFFF);
			case INT_COLOR_ARGB4 -> "#%04X".formatted(value.data() & 0xFFFF);
			case INT_COLOR_RGB4 -> "#%03X".formatted(value.data() & 0xFFF);
			default -> value.type() + ": " + value.data();
		};
	}

	/**
	 * @param resources
	 * 		Android resources for resolving resource IDs.
	 * @param key
	 * 		Complex map key.
	 *
	 * @return Readable representation of the key.
	 */
	@Nonnull
	public static String formatComplexKey(@Nonnull AndroidRes resources, int key) {
		String name = getResName(resources, key);
		return name == null ? formatResourceId(key) : name + " (" + formatResourceId(key) + ")";
	}

	/**
	 * @param id
	 * 		Resource identifier.
	 *
	 * @return Hex-encoded resource identifier.
	 */
	@Nonnull
	public static String formatResourceId(int id) {
		return "0x%08X".formatted(id);
	}

	@Nullable
	private static String getResName(@Nonnull AndroidRes resources, int id) {
		String name = resources.getResName(id);
		if (name != null)
			return name;
		String frameworkName = AndroidRes.getAndroidBase().getResName(id);
		return frameworkName == null ? null : "android:" + frameworkName;
	}

	@Nonnull
	private static String formatComplexValue(int complex, @Nonnull String[] units) {
		int unit = complex & COMPLEX_UNIT_MASK;
		String suffix = unit >= 0 && unit < units.length ? units[unit] : "";
		return formatFloat(complexToFloat(complex)) + suffix;
	}

	@Nonnull
	private static String formatFraction(int complex) {
		int unit = complex & COMPLEX_UNIT_MASK;
		String suffix = unit >= 0 && unit < FRACTION_UNITS.length ? FRACTION_UNITS[unit] : "";
		return formatFloat(complexToFloat(complex) * 100f) + suffix;
	}

	private static float complexToFloat(int complex) {
		return (complex & 0xFFFFFF00) * COMPLEX_RADIX_MULTIPLIERS[(complex >> COMPLEX_RADIX_SHIFT) & COMPLEX_RADIX_MASK];
	}

	@Nonnull
	private static String formatFloat(float value) {
		if (value == 0f)
			return "0";
		String text = Float.toString(value);
		int exponentIndex = Math.max(text.indexOf('E'), text.indexOf('e'));
		if (exponentIndex >= 0)
			return text;
		int decimalIndex = text.indexOf('.');
		if (decimalIndex < 0)
			return text;
		int end = text.length();
		while (end > decimalIndex + 1 && text.charAt(end - 1) == '0')
			end--;
		if (end > decimalIndex && text.charAt(end - 1) == '.')
			end--;
		return text.substring(0, end);
	}

	private static int getUnsignedShort(@Nonnull byte[] data, int offset) {
		return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
	}

	private static int getInt(@Nonnull byte[] data, int offset) {
		return (data[offset] & 0xFF) |
				((data[offset + 1] & 0xFF) << 8) |
				((data[offset + 2] & 0xFF) << 16) |
				((data[offset + 3] & 0xFF) << 24);
	}

	/**
	 * Binary XML start element with context.
	 *
	 * @param filePath
	 * 		Path to the originating XML file.
	 * @param strings
	 * 		Associated string pool.
	 * @param element
	 * 		Start element chunk.
	 */
	public record XmlElementData(@Nonnull FilePathNode filePath,
	                             @Nonnull StringPoolChunk strings,
	                             @Nonnull XmlStartElementChunk element) {}
}
