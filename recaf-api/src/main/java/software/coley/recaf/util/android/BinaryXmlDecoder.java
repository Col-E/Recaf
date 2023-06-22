package software.coley.recaf.util.android;

import com.android.SdkConstants;
import com.android.tools.apk.analyzer.BinaryXmlParser;
import com.android.xml.XmlBuilder;
import com.google.devrel.gmscore.tools.apk.arsc.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ArscFileInfo;
import software.coley.recaf.info.BinaryXmlFileInfo;
import software.coley.recaf.info.properties.builtin.BinaryXmlDecodedProperty;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Modified variant of {@link BinaryXmlParser} that can pull values from an ARSC file.
 *
 * @author Matt Coley
 */
public class BinaryXmlDecoder {
	private static final Logger logger = Logging.get(BinaryXmlDecoder.class);

	@Nonnull
	public static String toXml(@Nonnull BinaryXmlFileInfo binaryXml,
							   @Nullable ArscFileInfo arsc) throws IOException {
		// Check for prior value.
		String decode = BinaryXmlDecodedProperty.get(binaryXml);
		if (decode != null)
			return decode;

		// Decode binary XML chunks.
		BinaryResourceFile binaryResource = binaryXml.getChunkModel();
		List<Chunk> chunks = binaryResource.getChunks();
		if (chunks.size() != 1)
			throw new IOException(binaryXml.getName() + " has " + chunks.size() + " chunks, expected 1");

		// Map chunk model to XML.
		Chunk chunk = chunks.get(0);
		if (chunk instanceof XmlChunk xmlChunk) {
			XmlPrinter printer = new XmlPrinter(arsc);
			visitChunks(xmlChunk.getChunks(), printer);
			decode = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
					printer.getReconstructedXml();
			BinaryXmlDecodedProperty.set(binaryXml, decode);
			return decode;
		}
		throw new IOException(binaryXml.getName() + "'s chunk is not a XML chunk");
	}

	/**
	 * @param chunks
	 * 		Chunks to visit.
	 * @param handler
	 * 		XML printer implementation.
	 * 		Use {@link XmlPrinter#getReconstructedXml()} to see get the XML output.
	 */
	public static void visitChunks(@Nonnull Map<Integer, Chunk> chunks, @Nonnull XmlPrinter handler) {
		List<Chunk> contentChunks = sortByOffset(chunks);
		for (Chunk chunk : contentChunks) {
			if (chunk instanceof StringPoolChunk stringChunk) {
				handler.stringPool(stringChunk);
			} else if (chunk instanceof XmlResourceMapChunk resourceMap) {
				handler.xmlResourceMap(resourceMap);
			} else if (chunk instanceof XmlNamespaceStartChunk nsStart) {
				handler.startNamespace(nsStart);
			} else if (chunk instanceof XmlNamespaceEndChunk nsEnd) {
				handler.endNamespace(nsEnd);
			} else if (chunk instanceof XmlStartElementChunk startElement) {
				handler.startElement(startElement);
			} else if (chunk instanceof XmlEndElementChunk endElement) {
				handler.endElement(endElement);
			} else {
				logger.warn("Unhandled XML chunk type: {}", chunk.getClass().getSimpleName());
			}
		}
	}

	@Nonnull
	private static List<Chunk> sortByOffset(@Nonnull Map<Integer, Chunk> contentChunks) {
		return contentChunks.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.map(Map.Entry::getValue)
				.collect(Collectors.toList());
	}

	/**
	 * XML printer for use in {@link #visitChunks(Map, XmlPrinter)}.
	 */
	public static class XmlPrinter {
		private final XmlBuilder builder;
		private final Map<String, String> namespaces = new HashMap<>();
		private final AndroidRes arscRes;
		private boolean namespacesAdded;
		private StringPoolChunk stringPool;

		/**
		 * @param arsc
		 * 		Optional ARSC file to provide additional information for decoding.
		 */
		public XmlPrinter(@Nullable ArscFileInfo arsc) {
			builder = new XmlBuilder();
			arscRes = arsc == null ? null : arsc.getResourceInfo();
		}

		/**
		 * Sets the current string pool.
		 *
		 * @param chunk
		 * 		String chunk to visit.
		 */
		public void stringPool(@Nonnull StringPoolChunk chunk) {
			stringPool = chunk;
		}

		/**
		 * Associates the namespace URI to the namespace prefix.
		 *
		 * @param chunk
		 * 		Namespace chunk to visit.
		 */
		public void startNamespace(@Nonnull XmlNamespaceStartChunk chunk) {
			// Collect mapping of namespaces to prefixes
			// Used later when handling creation of elements referencing namespaces.
			namespaces.put(chunk.getUri(), chunk.getPrefix());
		}

		/**
		 * Appends the element open tag and attributes.
		 *
		 * @param chunk
		 * 		XML element chunk to visit.
		 */
		public void startElement(@Nonnull XmlStartElementChunk chunk) {
			builder.startTag(chunk.getName());

			// If this is the first tag, also print out the namespaces
			if (!namespacesAdded && !namespaces.isEmpty()) {
				namespacesAdded = true;
				for (Map.Entry<String, String> entry : namespaces.entrySet()) {
					builder.attribute(SdkConstants.XMLNS, entry.getValue(), entry.getKey());
				}
			}

			for (XmlAttribute xmlAttribute : chunk.getAttributes()) {
				String prefix = namespaces.get(xmlAttribute.namespace());
				if (prefix == null) prefix = "";
				builder.attribute(prefix, xmlAttribute.name(), getValue(xmlAttribute));
			}
		}

		/**
		 * Does nothing.
		 *
		 * @param chunk
		 * 		Resource map chunk to visit.
		 */
		public void xmlResourceMap(@Nonnull XmlResourceMapChunk chunk) {
			// no-op
		}

		/**
		 * Does nothing.
		 *
		 * @param chunk
		 * 		Namespace end chunk to visit.
		 */
		public void endNamespace(@Nonnull XmlNamespaceEndChunk chunk) {
			// no-op
		}

		/**
		 * Appends the closing element tag.
		 *
		 * @param chunk
		 * 		Element end chunk to visit.
		 */
		public void endElement(@Nonnull XmlEndElementChunk chunk) {
			builder.endTag(chunk.getName());
		}

		/**
		 * @return XML output.
		 */
		@Nonnull
		public String getReconstructedXml() {
			return builder.toString();
		}

		@Nonnull
		private String getValue(@Nonnull XmlAttribute attribute) {
			String rawValue = attribute.rawValue();
			if (!(rawValue == null || rawValue.isEmpty()))
				return rawValue;

			BinaryResourceValue resValue = attribute.typedValue();
			return formatValue(resValue, attribute.name());
		}

		/**
		 * @param resValue
		 * 		The value to format into a string representation.
		 * @param elementName
		 * 		The name of the element holding the value.
		 *
		 * @return Formatted string.
		 */
		@Nonnull
		public String formatValue(@Nonnull BinaryResourceValue resValue,
								  @Nonnull String elementName) {
			AndroidRes androidRes = AndroidRes.getAndroidBase();
			int data = resValue.data();
			switch (resValue.type()) {
				case NULL:
					return "null";
				case ATTRIBUTE: {
					String resName = arscRes.getResName(data);
					if (resName != null)
						return "?" + resName;
					resName = androidRes.getResName(data);
					if (resName != null)
						return "?android:" + resName;
					return String.format(Locale.US, "?0x%1$x", data);
				}
				case STRING:
					return stringPool != null && stringPool.getStringCount() < data
							? stringPool.getString(data)
							: String.format(Locale.US, "@string/0x%1$x", data);
				case FLOAT:
					return String.format(Locale.US, "%f", (float) data);
				case FRACTION:
					return AndroidFormatting.toFractionString(data);
				case DIMENSION:
					return AndroidFormatting.toDimensionString(data);
				case REFERENCE:
				case DYNAMIC_REFERENCE: {
					if (data == 0)
						return "0";
					String resName = arscRes.getResName(data);
					if (resName != null)
						return "@" + resName;
					resName = androidRes.getResName(data);
					if (resName != null)
						return "@android:" + resName;
					return String.format(Locale.US, "@ref/0x%1$08x", data);
				}
				case DYNAMIC_ATTRIBUTE:
					// TODO: Google's XmlPrinter has no reference implementation
					break;
				case INT_DEC: {
					String rep = null;
					if (androidRes.isResFlag(elementName))
						rep = androidRes.getResFlagNames(elementName, data);
					else if (androidRes.isResEnum(elementName))
						rep = androidRes.getResEnumName(elementName, data);
					if (rep == null)
						rep = Integer.toString(data);
					return rep;
				}
				case INT_HEX: {
					String rep = null;
					if (androidRes.isResFlag(elementName))
						rep = androidRes.getResFlagNames(elementName, data);
					else if (androidRes.isResEnum(elementName))
						rep = androidRes.getResEnumName(elementName, data);
					if (rep == null)
						rep = "0x" + Integer.toHexString(data);
					return rep;
				}
				case INT_BOOLEAN:
					return Boolean.toString(data != 0);
				case INT_COLOR_ARGB8:
					return String.format("argb8(0x%x)", data);
				case INT_COLOR_RGB8:
					return String.format("rgb8(0x%x)", data);
				case INT_COLOR_ARGB4:
					return String.format("argb4(0x%x)", data);
				case INT_COLOR_RGB4:
					return String.format("rgb4(0x%x)", data);
			}

			return String.format("@res/0x%x", data);
		}
	}
}
