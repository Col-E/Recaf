package me.coley.recaf.ui.test;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import me.coley.recaf.ui.control.code.*;
import me.coley.recaf.ui.window.WindowBase;
import org.fxmisc.flowless.VirtualizedScrollPane;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Testing.
 */
public class FooMain extends Application {
	private static final int WIDTH = 500;
	private static final int HEIGHT = 350;

	/**
	 * Entry.
	 * @param args Args.
	 */
	public static void main(String[] args) {
		Application.launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		// Demo
		ProblemTracking tracking = new ProblemTracking();
		tracking.setIndicatorInitializer(new ProblemIndicatorInitializer(tracking));
		SyntaxArea editor = new SyntaxArea(Languages.get("java"), tracking);
		// Give it some content
		editor.setMinSize(WIDTH, HEIGHT);
		editor.appendText(
				"package com.example;\n" +
				"\n" +
				"import java.util.*;\n" +
				"\n" +
				"@ExampleAnno\n" +
				"public class Example {\n" +
				"    void hello() {\n" +
				"        System.out.println(\"Hello world!\");\n" +
				"    }\n" +
				"}");
		// editor.setStyle(3,12, list("keyword"));
		// tracking.addProblem(1, new ProblemInfo(ProblemLevel.INFO, "Line 1"));
		// tracking.addProblem(2, new ProblemInfo(ProblemLevel.ERROR, "Line 2"));
		// tracking.addProblem(7, new ProblemInfo(ProblemLevel.WARNING, "Line 7"));

		stage.setScene(new Scene(new VirtualizedScrollPane<>(editor), WIDTH, HEIGHT));
		//stage.setScene(new Scene(editor, WIDTH, HEIGHT));
		WindowBase.addStylesheets(stage.getScene().getStylesheets());
		stage.show();
		stage.setOnCloseRequest(e -> System.exit(0));
	}

	private Collection<String> list(String keyword) {
		List<String> list = new ArrayList<>();
		list.add(keyword);
		return list;
	}
}
