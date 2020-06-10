package me.coley.recaf.parse.javadoc;


import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;

/**
 * Breadth-first DOM visitor that builds a formatted string based on the visited content.
 *
 * @author Jonathan Hedley, jonathan@hedley.net - <a href="https://github.com/jhy/jsoup/blob/master/src/main/java/org/jsoup/examples/HtmlToPlainText.java">HtmlToPlainText</a>
 * @author Matt
 */
public class FormattingVisitor implements NodeVisitor {
	private final StringBuilder sb = new StringBuilder();

	@Override
	public void head(Node node, int depth) {
		String name = node.nodeName();
		// TextNodes carry all user-readable text in the DOM.
		if(node instanceof TextNode)
			text(((TextNode) node).text());
		// Other nodes are used only for formatting, not content
		else if(name.equals("li"))
			text("\n * ");
		else if(name.equals("dt"))
			text("  ");
		else if(StringUtil.in(name, "p", "h1", "h2", "h3", "h4", "h5", "tr"))
			spacing(node);
	}

	@Override
	public void tail(Node node, int depth) {
		String name = node.nodeName();
		if(StringUtil.in(name, "br", "dd", "dt", "p", "h1", "h2", "h3", "h4", "h5", "ul", "ol", "table"))
			spacing(node);
	}

	@Override
	public String toString() {
		return sb.toString();
	}

	private void text(String text) {
		// Don't add empty lines first
		if(text.trim().isEmpty() && sb.length() == 0)
			return;
		sb.append(text);
	}

	private void spacing(Node node) {
		// Common for <li><p> patterns, causing additional uneeded space
		if (node.nodeName().equals("p") && node.parent().nodeName().equals("li"))
			return;
		// Add spacing
		text("\n");
	}
}