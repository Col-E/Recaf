package software.coley.recaf.ui.control.richtext.linegraphics;

import jakarta.annotation.Nonnull;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.bracket.BracketMatchGraphicFactory;
import software.coley.recaf.ui.control.richtext.problem.ProblemGutterGraphicFactory;
import software.coley.recaf.ui.control.richtext.problem.ProblemSquiggleGraphicFactory;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.IntFunction;

/**
 * Graphic factory for {@link Editor}.
 * <br>
 * Handles registration and display of additional {@link LineGraphicFactory} instances in a consistent manner.
 *
 * @author Matt Coley
 */
public class RootLineGraphicFactory extends AbstractLineGraphicFactory implements IntFunction<Node> {
	private final SortedSet<LineGraphicFactory> factories = new TreeSet<>();
	private final Editor editor;

	/**
	 * @param editor
	 * 		Base editor to work off of.
	 */
	public RootLineGraphicFactory(@Nonnull Editor editor) {
		super(-1);
		this.editor = editor;
		addLineGraphicFactory(new LineNumberFactory());
	}

	/**
	 * Adds the default graphic factories used for editor displays when the content is code.
	 */
	public void addDefaultCodeGraphicFactories() {
		addLineGraphicFactories(
				new BracketMatchGraphicFactory(),
				new ProblemGutterGraphicFactory(),
				new ProblemSquiggleGraphicFactory()
		);
	}

	/**
	 * Adds the given factories.
	 *
	 * @param factories
	 * 		Graphic factories to add.
	 */
	public void addLineGraphicFactories(LineGraphicFactory... factories) {
		for (LineGraphicFactory factory : factories)
			addLineGraphicFactory(factory);
	}

	/**
	 * Adds the given factory.
	 *
	 * @param factory
	 * 		Graphic factory to add.
	 */
	public void addLineGraphicFactory(@Nonnull LineGraphicFactory factory) {
		factories.add(factory);
		factory.install(editor);
	}

	/**
	 * @param factory
	 * 		Graphic factory to remove.
	 *
	 * @return {@code true} when removed. {@code false} when did not exist.
	 */
	public boolean removeLineGraphicFactory(@Nonnull LineGraphicFactory factory) {
		if (factories.remove(factory)) {
			factory.uninstall(editor);
			return true;
		}
		return false;
	}

	@Override
	public void apply(@Nonnull LineContainer container, int paragraph) {
		// no-op, this method is implemented by line-graphic factory children.
	}

	@Override
	public Node apply(int paragraph) {
		// Add all sub-factories in sorted order.
		LineContainer lineContainer = new LineContainer();
		for (LineGraphicFactory factory : factories)
			factory.apply(lineContainer, paragraph);

		// Wrap so the padding of the HBox expands the space of the 'lineno'.
		BorderPane wrapper = new BorderPane(lineContainer);
		wrapper.getStyleClass().add("lineno");
		// Note: The dimensions you will see on 'wrapper' do not appear to map its effective bounds.
		// We used to set the cursor to DEFAULT here, but this led to the editor's TEXT cursor being
		// replaced even when the visible bounds of this wrapper were not intersected with, and all
		// sub-nodes of the wrapper were marked as mouse-transparent.
		// The solution for now seems to just not specify a cursor.
		return wrapper;
	}

	@Override
	public void install(@Nonnull Editor editor) {
		// no-op
	}

	@Override
	public void uninstall(@Nonnull Editor editor) {
		throw new IllegalArgumentException("The root line graphic factory should never be uninstalled!");
	}
}
