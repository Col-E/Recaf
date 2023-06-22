package software.coley.recaf.ui.wizard;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.util.Lang;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Multi-page wizard. Shows one {@link WizardPage} at a time.
 *
 * @author Matt Coley
 * @see WizardPage
 */
public class Wizard extends VBox {
	private final List<WizardPage> pages;
	private Runnable onFinish;

	/**
	 * @param pages
	 * 		Array of wizard pages.
	 */
	public Wizard(WizardPage... pages) {
		this(List.of(pages));
	}

	/**
	 * @param pages
	 * 		List of wizard pages.
	 */
	public Wizard(@Nonnull List<WizardPage> pages) {
		if (pages.isEmpty())
			throw new IllegalArgumentException("Must have at least 1 item for wizard content.");
		this.pages = pages;
		WizardSteps steps = new WizardSteps();

		// Setup display with first page of content
		BorderPane content = new BorderPane();
		steps.selectedPageProperty().addListener((obs, old, cur) -> content.setCenter(cur.getDisplay()));
		steps.selectedPageProperty().set(pages.get(0));

		// [previous] button
		Button previous = new ActionButton(Lang.getBinding("dialog.previous"), steps::backward);
		previous.getStyleClass().addAll(Styles.FLAT);
		previous.disableProperty().bind(steps.canGoBackProperty().not());

		// [next] button
		StringBinding nextBinding = Bindings.createStringBinding(
				() -> steps.canGoForwardProperty().get() ? Lang.get("dialog.next") : Lang.get("dialog.finish"),
				steps.canGoForwardProperty()
		);
		Button next = new ActionButton(nextBinding, () -> {
			// Cannot go forward only for last page (finish) so handle finish callback here
			if (!steps.canGoForwardProperty().get() && onFinish != null) {
				onFinish.run();
			}

			// Standard progression
			if (steps.getSelectedPage().canProgressProperty().get()) {
				steps.forward();
			} else {
				Toolkit.getDefaultToolkit().beep();
			}
		});
		next.setDefaultButton(true);

		// Layout
		HBox controls = new HBox(previous, new Spacer(), next);
		controls.setAlignment(Pos.CENTER_LEFT);
		VBox.setVgrow(content, Priority.ALWAYS);
		VBox.setVgrow(steps, Priority.NEVER);
		VBox.setVgrow(controls, Priority.NEVER);
		setMinWidth(600);
		setPadding(new Insets(15));
		getChildren().addAll(steps, content, new Separator(), controls);
	}

	/**
	 * @param onFinish
	 * 		Action to run when the user presses the finish button on the last page.
	 */
	public void setOnFinish(Runnable onFinish) {
		this.onFinish = onFinish;
	}

	/**
	 * Page content, with ability to prevent continuation until some input constraints
	 * are met by binding {@link WizardPage#canProgressProperty()}.
	 */
	public abstract static class WizardPage {
		private final BooleanProperty canProgress = new SimpleBooleanProperty();
		private final StringBinding name;
		private Node display;

		/**
		 * @param name
		 * 		Page name.
		 */
		protected WizardPage(StringBinding name) {
			this.name = name;
		}

		/**
		 * @return Page display node.
		 */
		public Node getDisplay() {
			if (display == null) display = createDisplay();
			return display;
		}

		/**
		 * @return Page display node.
		 */
		public StringBinding getName() {
			return name;
		}

		/**
		 * @param state
		 * 		Progression allowed state.
		 */
		public void setCanProgress(boolean state) {
			canProgress.set(state);
		}

		/**
		 * @return Progression property, typically bound by the implementation to some input field(s).
		 */
		public BooleanProperty canProgressProperty() {
			return canProgress;
		}

		/**
		 * @return Node for {@link #getDisplay()}.
		 */
		protected abstract Node createDisplay();
	}

	/**
	 * Step manager for the current wizard.
	 * <br>
	 * Largely lifted from AtlantaFX's widget sampler.
	 */
	private class WizardSteps extends HBox {
		private static final PseudoClass SELECTED = PseudoClass.getPseudoClass(Styles.SUCCESS);
		private final ObjectProperty<WizardPage> selectedPage = new SimpleObjectProperty<>();
		private final BooleanBinding canGoBack;
		private final BooleanBinding canGoForward;

		private WizardSteps() {
			setPadding(new Insets(20));
			setSpacing(15);
			setAlignment(Pos.CENTER);

			canGoBack = Bindings.createBooleanBinding(() -> {
				if (selectedPage.get() == null) return false;
				int current = pages.indexOf(selectedPage.get());
				return current > 0 && current <= pages.size() - 1;
			}, selectedPage);

			canGoForward = Bindings.createBooleanBinding(() -> {
				if (selectedPage.get() == null) return false;
				int current = pages.indexOf(selectedPage.get());
				return current >= 0 && current < pages.size() - 1;
			}, selectedPage);

			selectedPage.addListener((obs, old, cur) -> {
				if (old != null)
					old.getDisplay().pseudoClassStateChanged(SELECTED, false);
				if (cur != null)
					cur.getDisplay().pseudoClassStateChanged(SELECTED, true);
			});

			// Populate from pages
			List<Node> children = new ArrayList<>();
			for (int i = 0; i < pages.size(); i++) {
				WizardPage page = pages.get(i);

				// Create graphic to represent the page in the ordered list
				String indexName = String.valueOf(i + 1);
				Label indexTitle = new BoundLabel(page.getName());
				int j = i;
				selectedPage.addListener((obs, old, val) -> {
					Node indexGraphic; // Hack to make the 'current' location more visible with default theme
					if (val == page) {
						indexGraphic = new Button(indexName);
						indexGraphic.getStyleClass().addAll(Styles.ROUNDED);
					} else {
						indexGraphic = new Button(indexName);
						indexGraphic.getStyleClass().addAll(Styles.ROUNDED);
					}

					// Fill the button if the page is 'done'
					if (j < pages.indexOf(selectedPage.get())) {
						indexGraphic.getStyleClass().addAll(Styles.ACCENT, Styles.SUCCESS);
					}

					indexGraphic.setMouseTransparent(true);
					indexGraphic.setFocusTraversable(false);
					indexTitle.setGraphic(indexGraphic);
				});
				children.add(indexTitle);

				// Add separator between pages
				if (i < pages.size() - 1) {
					Separator sep = new Separator();
					HBox.setHgrow(sep, Priority.ALWAYS);
					children.add(sep);
				}
			}
			getChildren().setAll(children);
		}

		/**
		 * @return Current selected/displayed page.
		 */
		public WizardPage getSelectedPage() {
			return selectedPage.get();
		}

		/**
		 * @return Property of current selected/displayed page.
		 */
		public ObjectProperty<WizardPage> selectedPageProperty() {
			return selectedPage;
		}

		/**
		 * @return Binding of when user can go back a page.
		 */
		public BooleanBinding canGoBackProperty() {
			return canGoBack;
		}

		/**
		 * @return Binding of when user can go forward a page.
		 */
		public BooleanBinding canGoForwardProperty() {
			return canGoForward;
		}

		/**
		 * Go back a page if allowed.
		 *
		 * @return {@code true} when moved.
		 */
		public boolean backward() {
			if (!canGoBack.get()) return false;
			int current = pages.indexOf(selectedPage.get());
			selectedPage.set(pages.get(current - 1));
			return true;
		}

		/**
		 * Go forward a page if allowed.
		 *
		 * @return {@code true} when moved.
		 */
		public boolean forward() {
			if (!canGoForward.get()) return false;
			int current = pages.indexOf(selectedPage.get());
			selectedPage.set(pages.get(current + 1));
			return true;
		}
	}
}
