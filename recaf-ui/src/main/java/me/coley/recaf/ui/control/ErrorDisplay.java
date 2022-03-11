package me.coley.recaf.ui.control;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.control.code.*;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.util.threading.FxThreadUtil;

/**
 * A box that expands when clicked, revealing problems in the target area.
 * Each problem entry can be clicked to jump to the line in question.
 * <br>
 * Usage should be with a {@link javafx.scene.layout.StackPane}.
 * For an example see the {@link me.coley.recaf.ui.pane.DecompilePane}.
 *
 * @author Matt Coley
 */
public class ErrorDisplay extends VBox implements ProblemUpdateListener {
	private final ObservableList<ProblemInfo> problems = FXCollections.observableArrayList();
	private final SyntaxArea area;
	private Runnable close;

	/**
	 * @param area
	 * 		Area to be associated with.
	 * @param tracking
	 * 		Problem tracker to pull data from.
	 */
	public ErrorDisplay(SyntaxArea area, ProblemTracking tracking) {
		this.area = area;
		setPickOnBounds(false);
		tracking.addProblemListener(this);
		setAlignment(Configs.editor().errorIndicatorPos);
		setOnKeyPressed(e -> {
			if (close != null && e.getCode() == KeyCode.ESCAPE) {
				close.run();
			}
		});
	}

	private synchronized void refresh() {
		getChildren().clear();
		if (!problems.isEmpty()) {
			VBox wrapper = new VBox();
			wrapper.setFillWidth(true);
			ScrollPane scroll = new ScrollPane(wrapper);
			scroll.setFitToWidth(true);
			scroll.setVisible(false);
			Runnable toggle = () -> {
				scroll.setVisible(!scroll.isVisible());
				if (scroll.isVisible())
					scroll.requestFocus();
			};
			close = () -> {
				scroll.setVisible(false);
				area.requestFocus();
			};
			ProblemLevel highestLevel = problems.stream()
					.map(ProblemInfo::getLevel)
					.reduce(ProblemLevel.INFO, (p1, p2) -> p1.ordinal() < p2.ordinal() ? p1 : p2);
			Label baseLabel = new Label(problems.size() + (problems.size() == 1 ? " Problem" : " Problems"));
			switch (highestLevel) {
				case INFO:
					baseLabel.setTextFill(Color.BLUE.brighter());
					baseLabel.setGraphic(Icons.getScaledIconView(Icons.INFO));
					break;
				case WARNING:
					baseLabel.setTextFill(Color.YELLOW);
					baseLabel.setGraphic(Icons.getScaledIconView(Icons.WARNING));
					break;
				case ERROR:
				default:
					baseLabel.setGraphic(Icons.getScaledIconView(Icons.ERROR));
					baseLabel.setTextFill(Color.RED.brighter());
					break;
			}
			baseLabel.getStyleClass().add("b");
			baseLabel.getStyleClass().add("tooltip");
			baseLabel.setCursor(Cursor.HAND);
			baseLabel.setOnMousePressed(e -> toggle.run());
			getChildren().add(baseLabel);
			problems.sorted().forEach(p -> {
				Label lineGraphic = new Label(p.getLine() + ": ");
				lineGraphic.getStyleClass().add("b");
				Label lblProblem = new Label(p.getMessage());
				lblProblem.setGraphic(lineGraphic);
				lblProblem.setOnMouseClicked(e -> {
					int line = p.getLine();
					if (line > 0 && line < area.getParagraphs().size()) {
						area.selectPosition(line, 0);
						area.requestFocus();
						toggle.run();
					}
				});
				lblProblem.setOnMouseEntered(e -> lblProblem.getStyleClass().add("tooltip-hover"));
				lblProblem.setOnMouseExited(e -> lblProblem.getStyleClass().remove("tooltip-hover"));
				lblProblem.setCursor(Cursor.HAND);
				lblProblem.setPadding(new Insets(10));
				switch (highestLevel) {
					case INFO:
						lineGraphic.setTextFill(Color.BLUE.brighter());
						lblProblem.setTextFill(Color.BLUE.brighter());
						break;
					case WARNING:
						lineGraphic.setTextFill(Color.YELLOW);
						lblProblem.setTextFill(Color.YELLOW);
						break;
					case ERROR:
					default:
						lineGraphic.setTextFill(Color.RED.brighter());
						lblProblem.setTextFill(Color.RED.brighter());
						break;
				}
				wrapper.getChildren().add(lblProblem);
			});
			wrapper.getStyleClass().add("tooltip");
			getChildren().add(scroll);
		}
		autosize();
	}

	@Override
	public void onProblemAdded(int line, ProblemInfo info) {
		problems.add(info);
		FxThreadUtil.run(this::refresh);
	}

	@Override
	public void onProblemRemoved(int line, ProblemInfo info) {
		problems.remove(info);
		FxThreadUtil.run(this::refresh);
	}
}
