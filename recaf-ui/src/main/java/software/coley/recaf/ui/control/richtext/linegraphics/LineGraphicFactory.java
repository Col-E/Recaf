package software.coley.recaf.ui.control.richtext.linegraphics;

import jakarta.annotation.Nonnull;
import javafx.scene.Node;
import software.coley.recaf.ui.control.richtext.EditorComponent;
import software.coley.recaf.ui.control.richtext.bracket.BracketMatchGraphicFactory;
import software.coley.recaf.ui.control.richtext.problem.ProblemGutterGraphicFactory;
import software.coley.recaf.ui.control.richtext.problem.ProblemSquiggleGraphicFactory;

import java.util.function.IntFunction;

/**
 * RichTextFX only uses {@link IntFunction} for graphics factories. We want to create our own system where
 * any number of graphic factories can be added, and the result will always be consistently displayed.
 * <br>
 * To facilitate this, we have this type which also is comparable to other instances of the same type.
 * Each graphic factory has a {@link #priority()} which defines its placement in the {@link RootLineGraphicFactory}.
 * Lower values appear first.
 *
 * @author Matt Coley
 * @see AbstractLineGraphicFactory Base implementation of this type.
 * @see RootLineGraphicFactory The root implementation which managed displaying other {@link LineGraphicFactory} instances in order.
 */
public interface LineGraphicFactory extends EditorComponent, Comparable<LineGraphicFactory> {
	/**
	 * Priority for {@link LineNumberFactory}.
	 */
	int P_LINE_NUMBERS = 0;
	/**
	 * Priority for {@link ProblemGutterGraphicFactory}.
	 */
	int P_LINE_PROBLEMS = 100;
	/**
	 * Priority for {@link ProblemSquiggleGraphicFactory}.
	 */
	int P_LINE_PROBLEM_SQUIGGLES = 101;
	/**
	 * Priority for {@link BracketMatchGraphicFactory}.
	 */
	int P_BRACKET_MATCH = 1000;

	/**
	 * @return Order priority for sorting in {@link RootLineGraphicFactory}. Lower values appear first.
	 */
	int priority();

	/**
	 * @param container
	 * 		Container to add nodes to if a graphic needs to be generated.
	 * 		Use {@link LineContainer#addHorizontal(Node)} and {@link LineContainer#addTopLayer(Node)}.
	 * @param paragraph
	 * 		Current paragraph index.
	 * 		Line would be {@code paragraph + 1}.
	 */
	void apply(@Nonnull LineContainer container, int paragraph);

	@Override
	default int compareTo(LineGraphicFactory o) {
		return Integer.compare(priority(), o.priority());
	}
}
