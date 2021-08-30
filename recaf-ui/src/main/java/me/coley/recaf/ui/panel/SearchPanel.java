package me.coley.recaf.ui.panel;

import com.panemu.tiwulfx.control.dock.DetachableTabPane;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.search.NumberMatchMode;
import me.coley.recaf.search.Search;
import me.coley.recaf.search.TextMatchMode;
import me.coley.recaf.search.query.QueryVisitor;
import me.coley.recaf.search.result.Result;
import me.coley.recaf.ui.control.ActionButton;
import me.coley.recaf.ui.control.ColumnPane;
import me.coley.recaf.ui.control.EnumComboBox;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.NumberUtil;
import me.coley.recaf.util.Threads;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;

import java.awt.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchPanel extends BorderPane {
	private static final Logger logger = Logging.get(SearchPanel.class);
	private final DetachableTabPane tabPane;

	private SearchPanel(String title, Node content) {
		DockingRootPane docking = docking();
		tabPane = docking.createNewTabPane();
		tabPane.getTabs().add(new DockingRootPane.KeyedTab(title, content));
		tabPane.setCloseIfEmpty(true);
		docking.removeFromHistory(tabPane);
		setCenter(tabPane);
	}

	public static SearchPanel createTextSearch() {
		return createTextSearch(null);
	}

	public static SearchPanel createTextSearch(String text) {
		String title = Lang.get("menu.search") + ": " + Lang.get("menu.search.string");
		// Inputs
		TextField txtText = new TextField(text);
		TextMatchMode defaultMode = text == null ? TextMatchMode.CONTAINS : TextMatchMode.EQUALS;
		EnumComboBox<TextMatchMode> comboMode = new EnumComboBox<>(TextMatchMode.class, defaultMode);
		// Layout
		ColumnPane columns = new ColumnPane();
		SearchPanel searchPanel = new SearchPanel(title, columns);
		columns.add(new Label(Lang.get("search.text")), txtText);
		columns.add(new Label(Lang.get("search.textmode")), comboMode);
		columns.add(null, new ActionButton(Lang.get("search.run"), () -> {
			searchPanel.searchText(txtText.getText(), comboMode.getValue());
		}));
		return searchPanel;
	}

	public static SearchPanel createNumberSearch() {
		return createNumberSearch(null);
	}

	public static SearchPanel createNumberSearch(String number) {
		String title = Lang.get("menu.search") + ": " + Lang.get("menu.search.number");
		// Inputs
		TextField txtNumber = new TextField(number);
		EnumComboBox<NumberMatchMode> comboMode = new EnumComboBox<>(NumberMatchMode.class, NumberMatchMode.EQUALS);
		// Layout
		ColumnPane columns = new ColumnPane();
		SearchPanel searchPanel = new SearchPanel(title, columns);
		columns.add(new Label(Lang.get("search.number")), txtNumber);
		columns.add(new Label(Lang.get("search.numbermode")), comboMode);
		columns.add(null, new ActionButton(Lang.get("search.run"), () -> {
			searchPanel.searchNumber(NumberUtil.parse(txtNumber.getText()), comboMode.getValue());
		}));
		return searchPanel;
	}

	public static SearchPanel createReferenceSearch() {
		return createReferenceSearch(null, null, null);
	}

	public static SearchPanel createReferenceSearch(String owner, String name, String desc) {
		String title = Lang.get("menu.search") + ": " + Lang.get("menu.search.references");
		// Inputs
		TextField txtOwner = new TextField(owner);
		TextField txtName = new TextField(name);
		TextField txtDesc = new TextField(desc);
		EnumComboBox<TextMatchMode> comboMode = new EnumComboBox<>(TextMatchMode.class, TextMatchMode.CONTAINS);
		// Layout
		ColumnPane columns = new ColumnPane();
		SearchPanel searchPanel = new SearchPanel(title, columns);
		columns.add(new Label(Lang.get("search.refowner")), txtOwner);
		columns.add(new Label(Lang.get("search.refname")), txtName);
		columns.add(new Label(Lang.get("search.refdesc")), txtDesc);
		columns.add(new Label(Lang.get("search.textmode")), comboMode);
		columns.add(null, new ActionButton(Lang.get("search.run"), () -> {
			searchPanel.searchReference(
					txtOwner.getText(), txtName.getText(), txtDesc.getText(),
					comboMode.getValue());
		}));
		return searchPanel;
	}

	public static SearchPanel createDeclarationSearch() {
		return createDeclarationSearch(null, null, null);
	}

	public static SearchPanel createDeclarationSearch(String owner, String name, String desc) {
		String title = Lang.get("menu.search") + ": " + Lang.get("menu.search.declarations");
		// Inputs
		TextField txtOwner = new TextField(owner);
		TextField txtName = new TextField(name);
		TextField txtDesc = new TextField(desc);
		EnumComboBox<TextMatchMode> comboMode = new EnumComboBox<>(TextMatchMode.class, TextMatchMode.CONTAINS);
		// Layout
		ColumnPane columns = new ColumnPane();
		SearchPanel searchPanel = new SearchPanel(title, columns);
		columns.add(new Label(Lang.get("search.refowner")), txtOwner);
		columns.add(new Label(Lang.get("search.refname")), txtName);
		columns.add(new Label(Lang.get("search.refdesc")), txtDesc);
		columns.add(new Label(Lang.get("search.textmode")), comboMode);
		columns.add(null, new ActionButton(Lang.get("search.run"), () -> {
			searchPanel.searchDeclaration(
					txtOwner.getText(), txtName.getText(), txtDesc.getText(),
					comboMode.getValue());
		}));
		return searchPanel;
	}

	private void runSearch(Search search) {
		Workspace workspace = RecafUI.getController().getWorkspace();
		if (workspace == null) {
			logger.error("Cannot search since no workspace is open!");
			Toolkit.getDefaultToolkit().beep();
			return;
		}
		Resource resource = workspace.getResources().getPrimary();
		Set<Result> results = Collections.synchronizedSet(new TreeSet<>());
		// Multi-thread search, using a countdown latch to track progress across threads
		int threadCount = Math.max(1, Runtime.getRuntime().availableProcessors());
		ExecutorService service = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(resource.getClasses().size());
		for (ClassInfo info : new HashSet<>(resource.getClasses().values())) {
			service.execute(() -> {
				QueryVisitor visitor = search.createQueryVisitor(resource);
				if (visitor != null) {
					new ClassReader(info.getValue()).accept(visitor, 0);
					latch.countDown();
					results.addAll(visitor.getAllResults());
				}
			});
		}
		service.shutdown();
		Threads.run(() -> {
			try {
				long count;
				do {
					count = latch.getCount();
					// TODO: Update "x classes remaining"
					Thread.sleep(10);
				} while (count > 0);
				logger.info("Search yielded {} results", results.size());
				String key = String.valueOf(results.hashCode());
				Threads.runFx(() -> {
					Tab tab = new DockingRootPane.KeyedTab(key, Lang.get("search.results"),
							new ResultsPane(search, results));
					tabPane.getTabs().add(tab);
					tabPane.getSelectionModel().select(tab);
				});
			} catch (InterruptedException ex) {
				logger.error("Interrupted search wait thread!", ex);
			}
		});
	}

	private void searchText(String text, TextMatchMode mode) {
		Search search = new Search().text(text, mode);
		runSearch(search);
	}

	private void searchNumber(Number number, NumberMatchMode mode) {
		Search search = new Search().number(number, mode);
		runSearch(search);
	}

	private void searchReference(String owner, String name, String desc, TextMatchMode mode) {
		Search search = new Search().reference(owner, name, desc, mode);
		runSearch(search);
	}

	private void searchDeclaration(String owner, String name, String desc, TextMatchMode mode) {
		Search search = new Search().declaration(owner, name, desc, mode);
		runSearch(search);
	}

	private static DockingRootPane docking() {
		return RecafUI.getWindows().getMainWindow().getDockingRootPane();
	}
}
