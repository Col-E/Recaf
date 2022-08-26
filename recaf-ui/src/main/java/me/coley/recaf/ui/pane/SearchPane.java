package me.coley.recaf.ui.pane;

import javafx.beans.binding.StringBinding;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.search.NumberMatchMode;
import me.coley.recaf.search.Search;
import me.coley.recaf.search.TextMatchMode;
import me.coley.recaf.search.query.QueryVisitor;
import me.coley.recaf.search.result.Result;
import me.coley.recaf.ui.control.ActionButton;
import me.coley.recaf.ui.control.BoundLabel;
import me.coley.recaf.ui.control.ColumnPane;
import me.coley.recaf.ui.control.EnumComboBox;
import me.coley.recaf.ui.docking.DockTab;
import me.coley.recaf.ui.docking.DockingRegion;
import me.coley.recaf.ui.docking.RecafDockingManager;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.NumberUtil;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.threading.FxThreadUtil;
import me.coley.recaf.util.threading.ThreadUtil;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Panel for search operations.
 *
 * @author Matt Coley
 */
public class SearchPane extends BorderPane {
	private static final Logger logger = Logging.get(SearchPane.class);
	private final DockTab containingTab;

	private SearchPane(ObservableValue<String> title, Node content) {
		DockingWrapperPane wrapper = DockingWrapperPane.builder()
				.title(title)
				.content(content)
				.size(600, 300)
				.build();
		containingTab = wrapper.getTab();
		setCenter(wrapper);
	}


	/**
	 * @return Empty text search panel.
	 */
	public static SearchPane createTextSearch() {
		return createTextSearch("");
	}

	/**
	 * @param text
	 * 		Initial query.
	 *
	 * @return Text search panel.
	 */
	public static SearchPane createTextSearch(String text) {
		StringBinding title = Lang.formatBy("%s: %s", Lang.getBinding("menu.search"),
				Lang.getBinding("menu.search.string"));
		// Inputs
		TextField txtText = new TextField(text);
		EnumComboBox<TextMatchMode> comboMode = new EnumComboBox<>(TextMatchMode.class, TextMatchMode.CONTAINS);
		// Layout
		ColumnPane columns = new ColumnPane();
		SearchPane searchPane = new SearchPane(title, columns);
		Label textLabel = new BoundLabel(Lang.getBinding("search.text"));
		columns.add(textLabel, txtText);
		Label modeLabel = new BoundLabel(Lang.getBinding("search.textmode"));
		columns.add(modeLabel, comboMode);
		columns.add(null, new ActionButton(Lang.getBinding("search.run"), () -> {
			searchPane.searchText(txtText.getText(), comboMode.getValue());
		}));
		return searchPane;
	}

	/**
	 * @return Empty number search panel.
	 */
	public static SearchPane createNumberSearch() {
		return createNumberSearch(null);
	}

	/**
	 * @param number
	 * 		Initial query.
	 *
	 * @return Number search panel.
	 */
	public static SearchPane createNumberSearch(String number) {
		StringBinding title = Lang.formatBy("%s: %s", Lang.getBinding("menu.search"),
				Lang.getBinding("menu.search.number"));
		// Inputs
		TextField txtNumber = new TextField(number);
		EnumComboBox<NumberMatchMode> comboMode = new EnumComboBox<>(NumberMatchMode.class, NumberMatchMode.EQUALS);
		// Layout
		ColumnPane columns = new ColumnPane();
		SearchPane searchPane = new SearchPane(title, columns);
		Label numberLabel = new BoundLabel(Lang.getBinding("search.number"));
		columns.add(numberLabel, txtNumber);
		Label modeLabel = new BoundLabel(Lang.getBinding("search.numbermode"));
		columns.add(modeLabel, comboMode);
		columns.add(null, new ActionButton(Lang.getBinding("search.run"), () -> {
			searchPane.searchNumber(NumberUtil.parse(txtNumber.getText()), comboMode.getValue());
		}));
		return searchPane;
	}

	/**
	 * @return Empty reference search panel.
	 */
	public static SearchPane createReferenceSearch() {
		return createReferenceSearch(null, null, null);
	}

	/**
	 * @param owner
	 * 		Internal name of owner.
	 * @param name
	 * 		Reference name.
	 * @param desc
	 * 		Reference descriptor.
	 *
	 * @return Reference search panel.
	 */
	public static SearchPane createReferenceSearch(String owner, String name, String desc) {
		return createReferenceSearch(owner, name, desc, TextMatchMode.CONTAINS);
	}

	/**
	 * @param owner
	 * 		Internal name of owner.
	 * @param name
	 * 		Reference name.
	 * @param desc
	 * 		Reference descriptor.
	 * @param mode
	 * 		Text match mode for member input.
	 *
	 * @return Reference search panel.
	 */
	public static SearchPane createReferenceSearch(String owner, String name, String desc, TextMatchMode mode) {
		StringBinding title = Lang.formatBy("%s: %s", Lang.getBinding("menu.search"),
				Lang.getBinding("menu.search.references"));
		// Inputs
		TextField txtOwner = new TextField(owner);
		TextField txtName = new TextField(name);
		TextField txtDesc = new TextField(desc);
		EnumComboBox<TextMatchMode> comboMode = new EnumComboBox<>(TextMatchMode.class, mode);
		// Layout
		ColumnPane columns = new ColumnPane();
		SearchPane searchPane = new SearchPane(title, columns);
		Label ownerLabel = new BoundLabel(Lang.getBinding("search.refowner"));
		columns.add(ownerLabel, txtOwner);
		Label nameLabel = new BoundLabel(Lang.getBinding("search.refname"));
		columns.add(nameLabel, txtName);
		Label descLabel = new BoundLabel(Lang.getBinding("search.refdesc"));
		columns.add(descLabel, txtDesc);
		Label modeLabel = new BoundLabel(Lang.getBinding("search.textmode"));
		columns.add(modeLabel, comboMode);
		columns.add(null, new ActionButton(Lang.getBinding("search.run"), () -> {
			searchPane.searchReference(
					txtOwner.getText(), txtName.getText(), txtDesc.getText(),
					comboMode.getValue());
		}));
		return searchPane;
	}

	/**
	 * @return Empty declaration search panel.
	 */
	public static SearchPane createDeclarationSearch() {
		return createDeclarationSearch(null, null, null);
	}

	/**
	 * @param owner
	 * 		Internal name of owner.
	 * @param name
	 * 		Declaration name.
	 * @param desc
	 * 		Declaration descriptor.
	 *
	 * @return Declaration search panel.
	 */
	public static SearchPane createDeclarationSearch(String owner, String name, String desc) {
		StringBinding title = Lang.formatBy("%s: %s", Lang.getBinding("menu.search"),
				Lang.getBinding("menu.search.declarations"));
		// Inputs
		TextField txtOwner = new TextField(owner);
		TextField txtName = new TextField(name);
		TextField txtDesc = new TextField(desc);
		EnumComboBox<TextMatchMode> comboMode = new EnumComboBox<>(TextMatchMode.class, TextMatchMode.CONTAINS);
		// Layout
		ColumnPane columns = new ColumnPane();
		SearchPane searchPane = new SearchPane(title, columns);
		Label ownerLabel = new BoundLabel(Lang.getBinding("search.refowner"));
		columns.add(ownerLabel, txtOwner);
		Label nameLabel = new BoundLabel(Lang.getBinding("search.refname"));
		columns.add(nameLabel, txtName);
		Label descLabel = new BoundLabel(Lang.getBinding("search.refdesc"));
		columns.add(descLabel, txtDesc);
		Label modeLabel = new BoundLabel(Lang.getBinding("search.textmode"));
		columns.add(modeLabel, comboMode);
		columns.add(null, new ActionButton(Lang.getBinding("search.run"), () -> {
			searchPane.searchDeclaration(
					txtOwner.getText(), txtName.getText(), txtDesc.getText(),
					comboMode.getValue());
		}));
		return searchPane;
	}

	private void runSearch(Search search) {
		Workspace workspace = RecafUI.getController().getWorkspace();
		if (workspace == null) {
			logger.error("Cannot search since no workspace is open!");
			Toolkit.getDefaultToolkit().beep();
			return;
		}
		Resource resource = workspace.getResources().getPrimary();
		Set<Result> results = ConcurrentHashMap.newKeySet(1024);
		// Multi-thread search, using an atomic integer to track progress across threads
		AtomicInteger latch = new AtomicInteger(resource.getClasses().size() + resource.getFiles().size());
		for (ClassInfo info : new HashSet<>(resource.getClasses().values())) {
			ThreadUtil.run(() -> {
				QueryVisitor visitor = search.createQueryVisitor(resource);
				if (visitor != null) {
					info.getClassReader().accept(visitor, ClassReader.SKIP_FRAMES);
					visitor.storeResults(results);
				}
				if (latch.decrementAndGet() == 0) {
					onSearchFinish(search, results);
				}
			});
		}
		for (FileInfo info : new HashSet<>(resource.getFiles().values())) {
			ThreadUtil.run(() -> {
				QueryVisitor visitor = search.createQueryVisitor(resource);
				if (visitor != null) {
					visitor.visitFile(info);
					visitor.storeResults(results);
				}
				if (latch.decrementAndGet() == 0) {
					onSearchFinish(search, results);
				}
			});
		}
	}

	private void onSearchFinish(Search search, Set<Result> results) {
		logger.info("Search yielded {} results", results.size());
		TreeSet<Result> sorted = new TreeSet<>(results);
		ResultsPane resultsPane = new ResultsPane(search, sorted);
		FxThreadUtil.run(() -> {
			DockingRegion region = containingTab.getParent();
			DockTab tab = RecafDockingManager.getInstance().createTabIn(region,
					() -> new DockTab(Lang.getBinding("search.results"), resultsPane));
			region.getSelectionModel().select(tab);
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
}
