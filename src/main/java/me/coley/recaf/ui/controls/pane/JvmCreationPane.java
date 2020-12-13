package me.coley.recaf.ui.controls.pane;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.ui.controls.ActionButton;
import me.coley.recaf.ui.controls.ExceptionAlert;
import me.coley.recaf.util.*;
import me.coley.recaf.util.self.SelfReferenceUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static me.coley.recaf.util.LangUtil.translate;

/**
 * UI for creating new JVM processes to attach to.
 *
 * @author Matt
 */
public class JvmCreationPane extends GridPane {
	// Executable
	private final FileItem fileJava;
	// Classpath
	private final BorderPane cpPane = new BorderPane();
	private final FileItem fileJar;
	private final ListView<FileItem> listCpItems = new ListView<>();
	private final TextField txtMainClass = new TextField();
	private final Button btnAddCpListItem;
	private boolean isJar;
	// Args
	private final TextField txtJvmArgs = new TextField();
	private final TextField txtAppArgs = new TextField();
	// Run
	private final Button btnRun;

	/**
	 * @param controller
	 * 		Controller to use.
	 */
	public JvmCreationPane(GuiController controller) {
		fileJava = new FileItem(controller, null);
		fileJar = new FileItem(controller, null);
		fileJava.setPrompt("<system-path::java>");
		btnAddCpListItem = new ActionButton(translate("misc.add"), () -> {
			listCpItems.getItems().add(new FileItem(controller, listCpItems));
		});
		btnRun = new ActionButton(translate("ui.attach"), () -> {
			List<String> command = new LinkedList<>();
			String execPath = (fileJava.isEmpty() ? IOUtil.toString(VMUtil.getJavaPath()) : fileJava.getPath());
			String jvmArgs = txtJvmArgs.getText();
			command.add(execPath);
			command.add(jvmArgs);
			command.add("-javaagent:\"" + SelfReferenceUtil.get().getPath() + '"');
			if (isJar) {
				command.addAll(Arrays.asList("-jar", fileJar.getPath()));
			} else {
				command.addAll(Arrays.asList("-cp", String.join(File.pathSeparator, getPaths()),
						txtMainClass.getText()));
			}
			String appArgs = txtAppArgs.getText();
			command.addAll(Arrays.asList(appArgs.split(" ")));
			try {
				ProcessBuilder.Redirect redirect = ProcessBuilder.Redirect.to(Log.logFile.toFile());
				Process process = new ProcessBuilder().command(command)
						.redirectOutput(redirect)
						.redirectError(redirect)
						.start();
				Log.info("Started new process: {}", String.join(" ", command));
				ThreadUtil.run(() -> {
					boolean exit = ProcessUtil.waitFor(process, 10L, TimeUnit.SECONDS);
					if (exit) {
						int exitCode = process.exitValue();
						if (exitCode != 0) {
							Log.error("Failed to start JVM. Exit code: {}", exitCode);
							ExceptionAlert.show(new IllegalStateException("Process finished with exit code: " +
									exitCode), "Failed to start JVM.");
						}
					}
				});
			} catch (IOException e) {
				Log.error(e, "Failed to start JVM: {}", e.getMessage());
				ExceptionAlert.show(e, "Failed to start JVM.");
			}
		});
		setup();
	}

	private void setup() {
		// Grid config
		setVgap(4);
		setHgap(5);
		setPadding(new Insets(15));
		setAlignment(Pos.TOP_CENTER);
		ColumnConstraints column1 = new ColumnConstraints();
		ColumnConstraints column2 = new ColumnConstraints();
		column1.setPercentWidth(25);
		column2.setPercentWidth(100 - column1.getPercentWidth());
		getColumnConstraints().addAll(column1, column2);
		// System
		addRow(this, 0, new Label("Java"), fileJava);
		addRow(this, 1, new Label("Classpath type"), createCpTypeRadios());
		addRow(this, 2, new Label("Classpath"), cpPane);
		addRow(this, 3, new Label("JVM args"), txtJvmArgs);
		addRow(this, 4, new Label("Application args"), txtAppArgs);
		add(btnRun, 1, 5);
	}

	/**
	 * @return Radio button group that swaps out {@link #cpPane} content.
	 */
	private Node createCpTypeRadios() {
		// Two options
		RadioButton radJar = new RadioButton("Runnable Jar");
		RadioButton radCp = new RadioButton("Classpath list");
		// Multiple cp item layout
		BorderPane bpCpList = new BorderPane();
		GridPane grid = new GridPane();
		grid.setVgap(4);
		grid.setHgap(5);
		grid.setPadding(new Insets(15));
		grid.setAlignment(Pos.CENTER);
		addRow(grid, 0, new Label(""), btnAddCpListItem);
		addRow(grid, 1, new Label("Main class"), txtMainClass);
		bpCpList.setCenter(listCpItems);
		bpCpList.setBottom(grid);
		// Set to jar
		radJar.setOnAction(e -> {
			isJar = true;
			radCp.setSelected(false);
			cpPane.setCenter(fileJar);
		});
		// Set to multiple
		radCp.setOnAction(e -> {
			isJar = false;
			radJar.setSelected(false);
			cpPane.setCenter(bpCpList);
		});
		// Use jar layout by default
		radJar.fire();
		return new VBox(radJar, radCp);
	}

	private static void addRow(GridPane grid, int rowIndex, Node... children) {
		grid.addRow(rowIndex, children);
		if (children[0].getClass() == Label.class) {
			children[0].getStyleClass().add("bold");
		}
	}

	/**
	 * @return Collection of added classpath values.
	 */
	private Collection<String> getPaths() {
		return listCpItems.getItems().stream().map(FileItem::getPath).collect(Collectors.toList());
	}

	/**
	 * Wrapper component for a file, shown in a text field with action buttons for selecting a file via a prompt.
	 */
	static class FileItem extends BorderPane {
		private final FileChooser fc = new FileChooser();
		private final TextField text = new TextField();

		/**
		 * @param controller
		 * 		Controller to pull config from for initial directory for file prompt.
		 * @param parent
		 * 		Parent listview to support removing self. {@code null} if not in a list.
		 */
		private FileItem(GuiController controller, ListView<FileItem> parent) {
			fc.setInitialDirectory(controller.config().backend().getRecentLoadDir());
			// Update text
			Button btnSelect = new ActionButton(translate("misc.select"), () -> {
				File file = fc.showOpenDialog(null);
				if (file != null) {
					text.setText(file.getAbsolutePath());
				}
			});
			setCenter(text);
			// Remove this item
			if (parent != null) {
				Button btnDelete = new ActionButton(translate("misc.remove"), () -> parent.getItems().remove(this));
				setRight(new HBox(btnSelect, btnDelete));
			} else {
				setRight(new HBox(btnSelect));
			}
		}

		private String getPath() {
			return text.getText();
		}

		private boolean isEmpty() {
			return getPath().trim().isEmpty();
		}

		private void setPrompt(String msg) {
			text.setPromptText(msg);
		}
	}
}
