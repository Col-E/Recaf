package me.coley.recaf.parse.javadoc;

import me.coley.recaf.util.StringUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.ParseErrorList;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeTraversor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Javadocs wrapper for a class.
 *
 * @author Matt
 */
public class Javadocs {
	public static final String NO_DESCRIPTION = "n/a";
	private static final String DEFAULT_PACKAGE = "";
	private final String html;
	private final String name;
	private final String packageName;
	private Document doc;
	private String description;
	private List<String> inheritance;
	private List<String> interfaces;
	private List<String> subclasses;
	private List<DocField> fields;
	private List<DocMethod> methods;

	/**
	 * @param name
	 * 		Internal name of class.
	 * @param html
	 * 		Documentation HTML for the class.
	 */
	public Javadocs(String name, String html) {
		this.html = html;
		this.name = name.substring(0, name.lastIndexOf("."));
		this.packageName = name.contains("/") ? name.substring(0, name.lastIndexOf("/")) : DEFAULT_PACKAGE;
	}

	/**
	 * Analyze the source code minimally.
	 *
	 * @throws DocumentationParseException
	 * 		Thrown when the parse tree encountered errors.
	 * 		The file may be partially parsed even if thrown.
	 */
	public void parse() throws DocumentationParseException {
		doc = Jsoup.parse(html);
		ParseErrorList errors = doc.parser().getErrors();
		if (!errors.isEmpty())
			throw new DocumentationParseException(html, errors);
	}

	/**
	 * @return Package the class resides in.
	 */
	public String getPackageName() {
		return packageName;
	}

	/**
	 * @return Internal class name representation.
	 */
	public String getInternalName() {
		return name;
	}

	/**
	 * @return Class description.
	 */
	public String getDescription() {
		if (description !=null)
			return description;
		try {
			// Inside <div class="description"><ul><li>
			// - Get last <div class="block">
			// - Return string content.
			Element el = doc.getElementsByClass("description").get(0).child(0).child(0);
			int i = el.children().size() - 1;
			while (i > 0) {
				Element ec = el.child(i);
				if(ec.tagName().equals("div") && ec.className().equals("block"))
					return description = text(ec);
				i--;
			}
		} catch(IndexOutOfBoundsException ex) {
			// Expected
		}
		// Description not found
		return description = NO_DESCRIPTION;
	}

	/**
	 * @return List of classes this class extends. First element is the parent class, last element
	 * is the Object class.
	 */
	public List<String> getInheritance() {
		if(inheritance != null)
			return inheritance;
		// Chain of "ul > li > ul > ..."
		// - first will be the root of the chain
		// We can simply pattern match the display text.
		Elements in = doc.getElementsByClass("inheritance");
		if (in == null || in.isEmpty())
			return inheritance = Collections.emptyList();
		Element root = in.get(0);
		String[] lines = StringUtil.splitNewlineSkipEmpty(root.wholeText());
		List<String> list = Arrays.asList(lines);
		Collections.reverse(list);
		return inheritance =
				list.stream().map(s -> s.replace('.', '/')).collect(Collectors.toList());
	}

	/**
	 * @return List of <i>all</i> implemented interfaces.
	 */
	public List<String> getInterfaces() {
		if (interfaces != null)
			return interfaces;
		try {
			return interfaces = scanLinks("All Implemented Interfaces:");
		} catch(IndexOutOfBoundsException ex) {
			// Expected, no interfaces specified
		}
		return interfaces = Collections.emptyList();
	}

	/**
	 * @return List of direct subclasses.
	 */
	public List<String> getSubclasses() {
		if (subclasses != null)
			return subclasses;
		try {
			return subclasses = scanLinks("Direct Known Subclasses:");
		} catch(IndexOutOfBoundsException ex) {
			// Expected, no subclasses specified
		}
		return subclasses = Collections.emptyList();
	}

	/**
	 * Common scan function for subclasses/implemented interfaces.
	 *
	 * @param ownText
	 * 		Text of the &lt;dt&gt; element to match as the starting position of the scan.
	 *
	 * @return List of internal names referenced by links.
	 */
	private List<String> scanLinks(String ownText) {
		List<String> list = new ArrayList<>();
		// <dt>ownText</dt>
		// - Get parent of this
		// - Iterate over <dd><code>'s children
		// - Contains a <a>, use "a.href" to parse internal name
		Element el = doc.getElementsContainingOwnText(ownText).get(0);
		int indexOf = el.parent().children().indexOf(el);
		el = el.parent().child(indexOf + 1);
		// Java 8: A series of <a>
		// - Link is essentially the exact same as internal name
		// Java 9: A series of <code> <a>
		// - Link is relative
		for (Element e : el.children()) {
			if (e.tagName().equals("a")) {
				// Java 8 or lower
				String url = e.attr("href");
				if(url != null)
					list.add(url.substring(url.lastIndexOf("../") + 3, url.length() - 5));
			} else if (e.tagName().equals("code")) {
				// Java 9 or higher, must resolve relative paths
				e = e.child(0);
				String url = e.attr("href");
				String rel = getPackageName();
				while (url.contains("../")) {
					url = url.substring(3);
					rel = rel.substring(0, rel.lastIndexOf("/"));
				}
				rel += "/" + url.substring(0, url.length() - 5);
				list.add(rel);
			}
		}
		return list;
	}

	/**
	 * @return List of field docs.
	 */
	public List<DocField> getFields() {
		if (fields != null)
			return fields;
		// <h3>Method Detail</h2>
		// - Get parent of this
		// - Iterate over <ul><li> children
		// - Parse for info
		List<DocField> list = new ArrayList<>();
		Elements fields = doc.getElementsContainingOwnText("Field Detail");
		if (fields == null || fields.isEmpty()) {
			this.fields = list;
			return list;
		}
		Element el = fields.get(0);
		el = el.parent();
		for (Element e : el.children()) {
			if (!e.tagName().equals("ul"))
				continue;
			// Select the <li> child
			e = e.child(0);
			String name = null;
			String data = null;
			StringBuilder description = new StringBuilder();
			for (Element c : e.children()){
				// Header block
				if (c.tagName().equals("h4"))
					name = c.ownText();
				// Definition block
				else if (c.tagName().equals("pre"))
					data = c.text();
				// Standard description block
				else if (c.tagName().equals("div"))
					description.append(text(c)).append("\n");
			}
			Objects.requireNonNull(data, "Failed to parse Javadoc for field:\n" + e.html());
			// data == <modifiers> <type> <name>
			// - Sometimes odd unicode char (160) exists instead of space
			String[] split = data.replace('\u00A0', ' ').replace("&nbsp;", " ").split("\\s");
			int typeIndex = split.length - 2;
			String type = split[typeIndex];
			List<String> modifiers = Arrays.asList(Arrays.copyOfRange(split, 0, typeIndex));
			list.add(new DocField(modifiers, name, description.toString().trim(), type));
		}
		return this.fields = list;
	}

	/**
	 * @return List of method docs.
	 */
	public List<DocMethod> getMethods() {
		if (methods != null)
			return methods;
		// <h3>Method Detail</h2>
		// - Get parent of this
		// - Iterate over <ul><li> children
		// - Parse for info
		List<DocMethod> list = new ArrayList<>();
		Elements methods = doc.getElementsContainingOwnText("Method Detail");
		if (methods == null || methods.isEmpty()) {
			this.methods = list;
			return list;
		}
		Element el = methods.get(0);
		el = el.parent();
		for (Element e : el.children()) {
			if (!e.tagName().equals("ul"))
				continue;
			// Select the <li> child
			e = e.child(0);
			String name = null;
			String data = null;
			StringBuilder retDescription = new StringBuilder();
			StringBuilder description = new StringBuilder();
			List<DocParameter> parameters = new ArrayList<>();
			for (Element c : e.children()){
				// Header block
				if (c.tagName().equals("h4"))
					name = c.ownText();
				// Definition block
				else if (c.tagName().equals("pre"))
					data = c.text();
				// Standard description block
				else if (c.tagName().equals("div"))
					description.append(text(c)).append("\n");
				// Contains children of items like "Parameters:" & "Returns:"
				// - <dt>'s text is the header
				// - following <dd>s are content
				else if (c.tagName().equals("dl"))
					// increment i based on key/content type
					parseMethodDescriptor(c, retDescription, parameters);
			}
			Objects.requireNonNull(data, "Failed to parse Javadoc for method:\n" + e.html());
			// data == <modifiers> <type> <name(args>
			// - Sometimes odd unicode char (160) exists instead of space
			String[] split = data.replace('\u00A0', ' ').substring(0, data.indexOf('(')).split("\\s");
			int typeIndex = split.length - 2;
			String type = split[typeIndex];
			if (type.contains("<"))
				type = type.substring(0, type.indexOf("<"));
			List<String> modifiers = Arrays.asList(Arrays.copyOfRange(split, 0, typeIndex));
			list.add(new DocMethod(modifiers, name, description.toString().trim(),
					retDescription.toString(), type, parameters));
		}
		return this.methods = list;
	}

	private void parseMethodDescriptor(Element c, StringBuilder retDesc, List<DocParameter> params) {
		for (int i = 0; i < c.children().size();) {
			Element cc = c.child(i);
			String key = cc.text();
			if (key.startsWith("Returns")) {
				// Returns should just have one following element
				retDesc.append(c.child(i+1).text());
				i+=2;
			} else if (key.startsWith("Parameters")) {
				// Parameters followed by 0 or more <dd> content elments
				// <dd><code>parameter</code> - description</dd>
				while (i < c.children().size() - 1) {
					Element value = c.child(i + 1);
					if (!value.tagName().equals("dd"))
						break;
					String pname = value.child(0).text();
					String pdesc = value.text();
					if (pdesc.length() > pname.length() + 3)
						pdesc = pdesc.substring(pname.length() + 3);
					params.add(new DocParameter(pname, pdesc));
					i++;
				}
				i++;
			} else {
				// Unknown documentation element
				i++;
			}
		}
	}

	private static String text(Element element) {
		FormattingVisitor formatter = new FormattingVisitor();
		NodeTraversor.traverse(formatter, element);
		return formatter.toString();
	}
}
