package software.coley.recaf.services.info;

import javafx.scene.Node;

/**
 * Interface for consuming generated summaries.
 *
 * @author Matt Coley
 */
public interface SummaryConsumer {
	/**
	 * Appends a single line.
	 *
	 * @param node
	 * 		Single line content.
	 */
	void appendSummary(Node node);

	/**
	 * Appends a single line, constructed from two parts.
	 *
	 * @param left
	 * 		Left portion of content.
	 * @param right
	 * 		Right portion of content.
	 */
	void appendSummary(Node left, Node right);
}
