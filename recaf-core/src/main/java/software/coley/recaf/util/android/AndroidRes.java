package software.coley.recaf.util.android;

import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceFile;
import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceValue;
import com.google.devrel.gmscore.tools.apk.arsc.Chunk;
import com.google.devrel.gmscore.tools.apk.arsc.PackageChunk;
import com.google.devrel.gmscore.tools.apk.arsc.ResourceTableChunk;
import com.google.devrel.gmscore.tools.apk.arsc.TypeChunk;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.android.xml.AndroidResourceProvider;
import software.coley.recaf.workspace.model.resource.AndroidApiResource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Android resource information.
 * <ul>
 *     <li>{@link #getAndroidBase()} - Common Android resource information</li>
 *     <li>{@link #fromArsc(BinaryResourceFile)} - Android resource information from an ARSC file</li>
 * </ul>
 *
 * @author Matt Coley
 */
public class AndroidRes implements AndroidResourceProvider {
	private static final AndroidRes ANDROID_BASE;
	private final Int2ObjectMap<String> resIdToName;
	private final Object2IntMap<String> resNameToId;
	private final Map<String, String> attrToFormat;
	private final Map<String, Object2LongMap<String>> attrToEnum;
	private final Map<String, Object2LongMap<String>> attrToFlags;
	private final Map<String, BinaryResourceValue> attrToSimpleResource;
	private final Map<String, Map<Integer, BinaryResourceValue>> attrToComplexResource;
	private final Map<String, Set<String>> formatToAttrs;

	private AndroidRes(@Nonnull Int2ObjectMap<String> resIdToName,
	                   @Nonnull Object2IntMap<String> resNameToId,
	                   @Nonnull Map<String, String> attrToFormat,
	                   @Nonnull Map<String, Object2LongMap<String>> attrToEnum,
	                   @Nonnull Map<String, Object2LongMap<String>> attrToFlags,
	                   @Nonnull Map<String, BinaryResourceValue> attrToSimpleResource,
	                   @Nonnull Map<String, Map<Integer, BinaryResourceValue>> attrToComplexResource,
	                   @Nonnull Map<String, Set<String>> formatToAttrs) {
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
	 * @return Resource information model unpacked from the chunked data.
	 */
	@Nonnull
	public static AndroidRes fromArsc(@Nonnull BinaryResourceFile arsc) {
		List<Chunk> chunks = arsc.getChunks();

		// Maps to collect data into.
		Int2ObjectMap<String> resIdToName = new Int2ObjectOpenHashMap<>();
		Object2IntMap<String> resNameToId = new Object2IntOpenHashMap<>();
		Map<String, String> attrToFormat = new TreeMap<>();
		Map<String, Object2LongMap<String>> attrToEnum = new TreeMap<>();
		Map<String, Object2LongMap<String>> attrToFlags = new TreeMap<>();
		Map<String, BinaryResourceValue> attrToSimpleResource = new TreeMap<>();
		Map<String, Map<Integer, BinaryResourceValue>> attrToComplexResource = new TreeMap<>();
		Map<String, Set<String>> formatToAttrs = new TreeMap<>();

		// Parse the chunks in the ARSC, extracting all entries
		for (Chunk chunk : chunks) {
			if (chunk instanceof ResourceTableChunk) {
				ResourceTableChunk resourceTableChunk = (ResourceTableChunk) chunk;
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

	@Override
	public boolean hasResName(int resId) {
		return resIdToName.get(resId) != null;
	}

	@Override
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
		return hasResEnum(resName);
	}

	@Override
	public boolean hasResEnum(@Nonnull String resName) {
		return attrToEnum.containsKey(resName);
	}

	@Override
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
	 * @param resName
	 * 		Resource name.
	 *
	 * @return {@code true} when the resource is a known value.
	 */
	public boolean isSimpleResValue(@Nonnull String resName) {
		return attrToSimpleResource.containsKey(resName);
	}

	/**
	 * @param resName
	 * 		Resource name.
	 *
	 * @return Resource value for the associated value if known, otherwise {@code null}.
	 */
	@Nullable
	public BinaryResourceValue getSimpleResValue(@Nonnull String resName) {
		return attrToSimpleResource.get(resName);
	}

	/**
	 * @param resName
	 * 		Resource name.
	 *
	 * @return {@code true} when the resource is a known value.
	 */
	public boolean isComplexResValue(@Nonnull String resName) {
		return attrToComplexResource.containsKey(resName);
	}

	/**
	 * @param resName
	 * 		Resource name.
	 *
	 * @return Resource value for the associated value if known, otherwise {@code null}.
	 */
	@Nullable
	public Map<Integer, BinaryResourceValue> getComplexResValue(@Nonnull String resName) {
		return attrToComplexResource.get(resName);
	}

	/**
	 * @param attrName
	 * 		Attribute name.
	 *
	 * @return {@code true} when the attribute has an associated format.
	 */
	public boolean attributeHasFormat(@Nonnull String attrName) {
		return attrToFormat.containsKey(attrName);
	}

	/**
	 * @param attrName
	 * 		Attribute name.
	 *
	 * @return Format identifier for the associated value if known, otherwise {@code null}.
	 */
	@Nullable
	public String getAttributeFormat(@Nonnull String attrName) {
		return attrToFormat.get(attrName);
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
		return hasResEnum(resName);
	}

	@Override
	public boolean hasResFlag(@Nonnull String resName) {
		return attrToFlags.containsKey(resName);
	}

	@Override
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

	private static final Gson GSON = new GsonBuilder().create();

	static {
		try {
			// Parse res-map entries (hex=name)
			String resMapText = new String(AndroidApiResource.class.getResourceAsStream("/android/res-map.txt").readAllBytes());
			String[] resMapLines = resMapText.split("[\n\r]+");
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
			Map<String, Set<String>> formatToAttrs = new TreeMap<>();
			Map<String, String> attrToFormat = new TreeMap<>();
			Map<String, Object2LongMap<String>> attrToEnum = new TreeMap<>();
			Map<String, Object2LongMap<String>> attrToFlags = new TreeMap<>();

			JsonObject tree = (JsonObject) JsonParser.parseReader(new InputStreamReader(AndroidRes.class.getResourceAsStream("/android/attrs.json")));
			visit(formatToAttrs, attrToFormat, attrToEnum, attrToFlags, tree);
			ANDROID_BASE = new AndroidRes(resIdToName, resNameToId,
					attrToFormat, attrToEnum, attrToFlags, Collections.emptyMap(), Collections.emptyMap(), formatToAttrs);
		} catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static void visit(@Nonnull Map<String, Set<String>> formatToAttrs,
	                          @Nonnull Map<String, String> attrToFormat,
	                          @Nonnull Map<String, Object2LongMap<String>> attrToEnum,
	                          @Nonnull Map<String, Object2LongMap<String>> attrToFlags,
	                          @Nonnull JsonObject node) {
		JsonElement nameNode = node.get("name");
		if (nameNode != null && node instanceof JsonObject childObject) {
			String name = nameNode.getAsString();

			// Handle different kinds of entries
			JsonElement formatNode = childObject.get("format");
			if (formatNode != null) {
				String format = formatNode.getAsString();
				attrToFormat.put(name, format);
				formatToAttrs.computeIfAbsent(format, f -> new TreeSet<>()).add(name);
				return;
			} else {
				JsonElement flagNode = childObject.get("flag");
				if (flagNode instanceof JsonArray flagArray) {
					Object2LongMap<String> nameToValue = new Object2LongOpenHashMap<>();
					for (JsonElement flagEntry : flagArray) {
						JsonObject flagObject = flagEntry.getAsJsonObject();
						String flagName = flagObject.get("name").getAsString();
						String flagValueText = flagObject.get("value").getAsString();
						if (flagValueText.startsWith("0x"))
							flagValueText = flagValueText.substring(2);
						long flagValue = Long.parseLong(flagValueText, 16);
						nameToValue.put(flagName, flagValue);
					}
					attrToFlags.put(name, nameToValue);
					return;
				} else {
					JsonElement enumNode = node.get("enum");
					if (enumNode instanceof JsonArray enumArray) {
						Object2LongMap<String> nameToValue = new Object2LongOpenHashMap<>();
						for (JsonElement flagEntry : enumArray) {
							JsonObject flagObject = flagEntry.getAsJsonObject();
							String enumName = flagObject.get("name").getAsString();
							String enumValueText = flagObject.get("value").getAsString();
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
		}

		// Visit the children if this node is not a child and isn't one of the types we expect.
		for (JsonElement child : node.asMap().values())
			if (child instanceof JsonObject childObject)
				visit(formatToAttrs, attrToFormat, attrToEnum, attrToFlags, childObject);
	}
}
