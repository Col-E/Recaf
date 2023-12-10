package me.coley.recaf.ui.pane;

import javafx.beans.binding.StringBinding;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
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
import me.coley.recaf.ui.jfxbuilder.component.container.Containers;
import me.coley.recaf.ui.jfxbuilder.component.control.choice.MultipleChoiceSelector.SelectionMode;
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

import static me.coley.recaf.ui.jfxbuilder.component.control.ControlComponents.*;

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
	 * @param text Initial query.
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
	 * @param number Initial query.
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
	 * @param owner Internal name of owner.
	 * @param name  Reference name.
	 * @param desc  Reference descriptor.
	 * @return Reference search panel.
	 */
	public static SearchPane createReferenceSearch(String owner, String name, String desc) {
		return createReferenceSearch(owner, name, desc, TextMatchMode.CONTAINS);
	}

	/**
	 * @param owner Internal name of owner.
	 * @param name  Reference name.
	 * @param desc  Reference descriptor.
	 * @param mode  Text match mode for member input.
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
	 * @param owner Internal name of owner.
	 * @param name  Declaration name.
	 * @param desc  Declaration descriptor.
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

	static class AdvancedSearch {
		final ObjectProperty<Target> target = new SimpleObjectProperty<>();
		final StringProperty identifier = new SimpleStringProperty();
		final BooleanProperty deprecated = new SimpleBooleanProperty();
		final BooleanProperty publiclyAccessible = new SimpleBooleanProperty();
		final Member member = new Member();
		final Type type = new Type();
		final ObjectProperty<TargetType> targetType = new SimpleObjectProperty<>();
		final BooleanProperty isStatic = new SimpleBooleanProperty();
		final SetProperty<Where> where = new SimpleSetProperty<>();
		SearchPane searchPane;


		enum Target {
			MEMBER, TYPE;
		}

		enum TargetType {
			REFERENCE, DECLARATION;
		}

		enum Where {
			IN_CODE, AS_RETURN_TYPE, AS_PARAMETER, AS_SUPER_TYPE
		}

		static class Member {

			final ObjectProperty<Type> type = new SimpleObjectProperty<>();
			final StringProperty returnType = new SimpleStringProperty();
			final StringProperty parameters = new SimpleStringProperty();

			enum Type {
				FIELD, METHOD;
			}

			final StringProperty owner = new SimpleStringProperty();
		}

		static class Type {
			final SetProperty<TypeType> type = new SimpleSetProperty<>();

			enum TypeType {
				CLASS, RECORD, ENUM, INTERFACE
			}

		}
	}

	public static SearchPane creatAdvancedSearch() {
		StringBinding title = Lang.formatBy("%s: %s", Lang.getBinding("menu.search"),
			Lang.getBinding("menu.search.advanced"));
		var advancedSearch = new AdvancedSearch();
		var whenTargetIsMember = advancedSearch.target.isEqualTo(AdvancedSearch.Target.MEMBER);
		var whenTargetIsMethod = whenTargetIsMember
			.and(advancedSearch.member.type.isEqualTo(AdvancedSearch.Member.Type.METHOD));
		var whenTargetTypeIsDeclaration = advancedSearch.targetType.isEqualTo(AdvancedSearch.TargetType.DECLARATION);
		var whenTargetIsType = advancedSearch.target.isEqualTo(AdvancedSearch.Target.TYPE);


		return advancedSearch.searchPane = new SearchPane(title, Containers
			.gridAsRows(2)
			.padding(5, 10, 5, 10)
			.columnConstraint(c -> c.percentWidth(30))
			.columnConstraint(c -> c.fillWidth(true).h(HPos.LEFT, Priority.ALWAYS))
			.vgap(5)
			.add(label("Target"), choice(AdvancedSearch.Target.values(), AdvancedSearch.Target::name, AdvancedSearch.Target.MEMBER, SelectionMode.SINGLE)
				.bind(advancedSearch.target))
			.add(label("Identifier"), textField().bindText(advancedSearch.identifier))
			.add(label("Deprecated"), checkbox().bind(advancedSearch.deprecated))
			.add(label("Public"), checkbox().bind(advancedSearch.publiclyAccessible))
			.addIf(whenTargetIsMember, label("Owner"), textField().bindText(advancedSearch.member.owner))
			.addIf(whenTargetIsMember,
				label("Member type"),
				choice(AdvancedSearch.Member.Type.values(), AdvancedSearch.Member.Type::name, AdvancedSearch.Member.Type.FIELD, SelectionMode.SINGLE)
					.bind(advancedSearch.member.type)
			)
			.addIf(whenTargetIsType,
				label("Type type"),
				choice(AdvancedSearch.Type.TypeType.values(), AdvancedSearch.Type.TypeType::name, AdvancedSearch.Type.TypeType.CLASS, SelectionMode.MULTIPLE)
					.bind(advancedSearch.type.type)
			)
			.addIf(whenTargetIsMethod, label("Parameters"), textField().bindText(advancedSearch.member.parameters))
			.addIf(whenTargetIsMember, nodeSwitch(AdvancedSearch.Member.Type.values(), advancedSearch.member.type)
					.forCase(AdvancedSearch.Member.Type.FIELD, label("Type"))
					.forCase(AdvancedSearch.Member.Type.METHOD, label("Return type")),
				textField().bindText(advancedSearch.member.returnType)
			)
			.add(
				label("Target Type"),
				choice(AdvancedSearch.TargetType.values(), AdvancedSearch.TargetType::name, AdvancedSearch.TargetType.REFERENCE, SelectionMode.SINGLE)
					.bind(advancedSearch.targetType)
			)
			.addIf(whenTargetTypeIsDeclaration, label("Static"), checkbox().bind(advancedSearch.isStatic))
			.addIf(whenTargetTypeIsDeclaration.not().and(whenTargetIsType), label("Where"),
				choice(AdvancedSearch.Where.values(), AdvancedSearch.Where::name, AdvancedSearch.Where.IN_CODE, SelectionMode.MULTIPLE)
					.bind(advancedSearch.where)
			)
			.build().nodeWithData(advancedSearch)
		);
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
