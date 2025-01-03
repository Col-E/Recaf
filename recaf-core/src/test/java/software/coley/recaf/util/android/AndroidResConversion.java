package software.coley.recaf.util.android;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * Not actually a test but a utility for {@link AndroidRes}.
 *
 * @author Matt Coley
 */
public class AndroidResConversion {
	private static final XmlMapper MAPPER = new XmlMapper();

	public static void main(String[] args) throws Exception {
		// We just want to merge these two XML files into one model and then slap it back into JSON
		JsonNode tree1 = MAPPER.readTree(AndroidRes.class.getResourceAsStream("/android/attrs_manifest.xml"));
		JsonNode tree2 = MAPPER.readTree(AndroidRes.class.getResourceAsStream("/android/attrs.xml"));
		ObjectNode merged = MAPPER.createObjectNode();
		merged.putIfAbsent("attrs_manifest", tree1);
		merged.putIfAbsent("attrs", tree2);
		String string = merged.toString();
		System.out.println(string);
	}
}
