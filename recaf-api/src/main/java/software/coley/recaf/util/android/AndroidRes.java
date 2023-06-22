package software.coley.recaf.util.android;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.devrel.gmscore.tools.apk.arsc.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ArscFileInfo;
import software.coley.recaf.util.MultiMap;
import software.coley.recaf.workspace.model.resource.AndroidApiResource;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Android resource information.
 * <ul>
 *     <li>{@link #getAndroidBase()} - Common Android resource information</li>
 *     <li>{@link #fromArsc(ArscFileInfo)} - Android resource information from an ARSC file</li>
 * </ul>
 *
 * @author Matt Coley
 */
public class AndroidRes {
	private static final XmlMapper MAPPER = new XmlMapper();
	private static final AndroidRes ANDROID_BASE;
	private final Int2ObjectMap<String> resIdToName;
	private final Object2IntMap<String> resNameToId;
	private final Map<String, String> attrToFormat;
	private final Map<String, Object2LongMap<String>> attrToEnum;
	private final Map<String, Object2LongMap<String>> attrToFlags;
	private final Map<String, BinaryResourceValue> attrToSimpleResource;
	private final Map<String, Map<Integer, BinaryResourceValue>> attrToComplexResource;
	private final MultiMap<String, String, Set<String>> formatToAttrs;

	private AndroidRes(@Nonnull Int2ObjectMap<String> resIdToName,
					   @Nonnull Object2IntMap<String> resNameToId,
					   @Nonnull Map<String, String> attrToFormat,
					   @Nonnull Map<String, Object2LongMap<String>> attrToEnum,
					   @Nonnull Map<String, Object2LongMap<String>> attrToFlags,
					   @Nonnull Map<String, BinaryResourceValue> attrToSimpleResource,
					   @Nonnull Map<String, Map<Integer, BinaryResourceValue>> attrToComplexResource,
					   @Nonnull MultiMap<String, String, Set<String>> formatToAttrs) {
		this.resIdToName = resIdToName;
		this.resNameToId = resNameToId;
		this.attrToFormat = attrToFormat;
		this.attrToEnum = attrToEnum;
		this.attrToFlags = attrToFlags;
		this.attrToSimpleResource = attrToSimpleResource;
		this.attrToComplexResource = attrToComplexResource;
		this.formatToAttrs = formatToAttrs;
	}

	/**
	 * @param arsc
	 * 		ARSC resource file to parse resource information from.
	 *
	 * @return Resource info from the ARSC file.
	 */
	@Nonnull
	public static AndroidRes fromArsc(@Nonnull ArscFileInfo arsc) {
		BinaryResourceFile chunkModel = arsc.getChunkModel();
		List<Chunk> chunks = chunkModel.getChunks();

		// Maps to collect data into.
		Int2ObjectMap<String> resIdToName = new Int2ObjectOpenHashMap<>();
		Object2IntMap<String> resNameToId = new Object2IntOpenHashMap<>();
		Map<String, String> attrToFormat = new TreeMap<>();
		Map<String, Object2LongMap<String>> attrToEnum = new TreeMap<>();
		Map<String, Object2LongMap<String>> attrToFlags = new TreeMap<>();
		Map<String, BinaryResourceValue> attrToSimpleResource = new TreeMap<>();
		Map<String, Map<Integer, BinaryResourceValue>> attrToComplexResource = new TreeMap<>();
		MultiMap<String, String, Set<String>> formatToAttrs = MultiMap.from(new TreeMap<>(), HashSet::new);

		// Parse the chunks in the ARSC, extracting all entries
		for (Chunk chunk : chunks) {
			if (chunk instanceof ResourceTableChunk resourceTableChunk) {
				for (PackageChunk packageChunk : resourceTableChunk.getPackages()) {
					int packageId = packageChunk.getId();
					for (TypeChunk typeChunk : packageChunk.getTypeChunks()) {
						int typeId = typeChunk.getId();
						String typePrefix = typeChunk.getTypeName();
						typeChunk.getEntries().forEach((entryId, entry) -> {
							int entryResId = packageId << 24 | typeId << 16 | entryId;
							String key = entry.key();
							String mapKey = typePrefix + "/" + key;
							resIdToName.put(entryResId, mapKey);
							resNameToId.put(mapKey, entryResId);
							if (entry.isComplex()) {
								Map<Integer, BinaryResourceValue> values = entry.values();
								attrToComplexResource.put(mapKey, values);
							} else {
								BinaryResourceValue value = entry.value();
								attrToSimpleResource.put(mapKey, value);
							}
						});
					}
				}
			}
		}

		return new AndroidRes(resIdToName, resNameToId, attrToFormat, attrToEnum, attrToFlags,
				attrToSimpleResource, attrToComplexResource, formatToAttrs);
	}

	/**
	 * @return Instance of core Android resources.
	 */
	@Nonnull
	public static AndroidRes getAndroidBase() {
		return ANDROID_BASE;
	}

	/**
	 * @param attrName
	 * 		Local attribute name, without the {@code attr/} prefix.
	 *
	 * @return Resource ID.
	 */
	public int getAttrResId(@Nonnull String attrName) {
		return getResId("attr/" + attrName);
	}

	/**
	 * @param resName
	 * 		Resource name.
	 *
	 * @return Resource ID.
	 */
	public int getResId(@Nonnull String resName) {
		return resNameToId.getOrDefault(resName, -1);
	}

	/**
	 * @param resId
	 * 		Resource ID.
	 *
	 * @return Name of resource, or {@code null} if not present.
	 */
	@Nullable
	public String getResName(int resId) {
		return resIdToName.get(resId);
	}

	/**
	 * @param resId
	 * 		Resource ID.
	 *
	 * @return {@code true} when the resource is a known enum.
	 */
	public boolean isResEnum(int resId) {
		String resName = getResName(resId);
		if (resName == null)
			return false;
		return isResEnum(resName);
	}

	/**
	 * @param resName
	 * 		Resource name.
	 *
	 * @return {@code true} when the resource is a known enum.
	 */
	public boolean isResEnum(@Nonnull String resName) {
		return attrToEnum.containsKey(resName);
	}

	/**
	 * @param resName
	 * 		Resource name.
	 * @param value
	 * 		Enum value key.
	 *
	 * @return Enum name for the associated value if known, otherwise {@code null}.
	 */
	@Nullable
	public String getResEnumName(@Nonnull String resName, long value) {
		Object2LongMap<String> values = attrToEnum.get(resName);
		if (values == null)
			return null;
		return values.object2LongEntrySet().stream()
				.filter(e -> e.getLongValue() == value)
				.findFirst()
				.map(Map.Entry::getKey)
				.orElse(null);
	}

	/**
	 * @param resId
	 * 		Resource ID.
	 *
	 * @return {@code true} when the resource is a known flag.
	 */
	public boolean isResFlag(int resId) {
		String resName = getResName(resId);
		if (resName == null)
			return false;
		return isResEnum(resName);
	}

	/**
	 * @param resName
	 * 		Resource name.
	 *
	 * @return {@code true} when the resource is a known flag.
	 */
	public boolean isResFlag(@Nonnull String resName) {
		return attrToFlags.containsKey(resName);
	}

	/**
	 * @param resName
	 * 		Resource name.
	 * @param mask
	 * 		Flag value mask.
	 *
	 * @return Flag names <i>(Separated by {@code |})</i> for the associated value if known, otherwise {@code null}.
	 */
	@Nonnull
	public String getResFlagNames(@Nonnull String resName, long mask) {
		Object2LongMap<String> values = attrToFlags.get(resName);
		if (values == null)
			return "";
		return values.object2LongEntrySet().stream()
				.filter(e -> (mask & e.getLongValue()) != 0L)
				.sorted(Comparator.comparingLong(Object2LongMap.Entry::getLongValue))
				.map(Map.Entry::getKey)
				.collect(Collectors.joining("|"));
	}

	static {
		try {
			// Parse res-map entries (hex=name)
			String resMapText = new String(AndroidApiResource.class.getResourceAsStream("/android/res-map.txt").readAllBytes());
			String[] resMapLines = resMapText.split("\n");
			Int2ObjectMap<String> resIdToName = new Int2ObjectOpenHashMap<>(resMapLines.length);
			Object2IntMap<String> resNameToId = new Object2IntOpenHashMap<>(resMapLines.length);
			for (String line : resMapLines) {
				String[] kv = line.split("=");
				int key = Integer.parseInt(kv[0], 16);
				String name = kv[1];
				resIdToName.put(key, name);
				resNameToId.put(name, key);
			}

			// Parse the attribute manifest
			MultiMap<String, String, Set<String>> formatToAttrs = MultiMap.from(new TreeMap<>(), HashSet::new);
			Map<String, String> attrToFormat = new TreeMap<>();
			Map<String, Object2LongMap<String>> attrToEnum = new TreeMap<>();
			Map<String, Object2LongMap<String>> attrToFlags = new TreeMap<>();
			JsonNode tree = MAPPER.readTree(AndroidApiResource.class.getResourceAsStream("/android/attrs_manifest.xml"));
			for (JsonNode child : tree)
				visit(formatToAttrs, attrToFormat, attrToEnum, attrToFlags, child);
			tree = MAPPER.readTree(AndroidApiResource.class.getResourceAsStream("/android/attrs.xml"));
			for (JsonNode child : tree)
				visit(formatToAttrs, attrToFormat, attrToEnum, attrToFlags, child);
			ANDROID_BASE = new AndroidRes(resIdToName, resNameToId,
					attrToFormat, attrToEnum, attrToFlags, Collections.emptyMap(), Collections.emptyMap(), formatToAttrs);
		} catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static void visit(@Nonnull MultiMap<String, String, Set<String>> formatToAttrs,
							  @Nonnull Map<String, String> attrToFormat,
							  @Nonnull Map<String, Object2LongMap<String>> attrToEnum,
							  @Nonnull Map<String, Object2LongMap<String>> attrToFlags,
							  @Nonnull JsonNode node) {
		JsonNode nameNode = node.get("name");
		if (nameNode != null && node instanceof ObjectNode childObject) {
			String name = nameNode.asText();

			// Handle different kinds of entries
			JsonNode formatNode = childObject.get("format");
			if (formatNode != null) {
				String format = formatNode.asText();
				attrToFormat.put(name, format);
				formatToAttrs.put(format, name);
				return;
			} else {
				JsonNode flagNode = childObject.get("flag");
				if (flagNode instanceof ArrayNode flagArray) {
					Object2LongMap<String> nameToValue = new Object2LongOpenHashMap<>();
					for (JsonNode flagEntry : flagArray) {
						String flagName = flagEntry.get("name").asText();
						String flagValueText = flagEntry.get("value").asText();
						if (flagValueText.startsWith("0x"))
							flagValueText = flagValueText.substring(2);
						long flagValue = Long.parseLong(flagValueText, 16);
						nameToValue.put(flagName, flagValue);
					}
					attrToFlags.put(name, nameToValue);
					return;
				} else if (node.get("enum") instanceof ArrayNode enumArray) {
					Object2LongMap<String> nameToValue = new Object2LongOpenHashMap<>();
					for (JsonNode flagEntry : enumArray) {
						String enumName = flagEntry.get("name").asText();
						String enumValueText = flagEntry.get("value").asText();
						if (enumValueText.startsWith("0x"))
							enumValueText = enumValueText.substring(2);
						long enumValue = Long.parseLong(enumValueText);
						nameToValue.put(enumName, enumValue);
					}
					attrToEnum.put(name, nameToValue);
					return;
				}
			}
		}

		// Visit the children if this node is not a child and isn't one of the types we expect.
		for (JsonNode child : node)
			visit(formatToAttrs, attrToFormat, attrToEnum, attrToFlags, child);
	}
}
