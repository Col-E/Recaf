package me.coley.recaf.parse.javadoc;

import me.coley.recaf.util.StringUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.ParseErrorList;

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
			el =  el.child(el.children().size() - 1);
			if (el.tagName().equals("div") && el.className().equals("block"))
				return description = el.text();
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
		Element root = doc.getElementsByClass("inheritance").get(0);
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
}
