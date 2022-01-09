package me.coley.recaf.ui.prompt;

import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.*;
import me.coley.recaf.ui.CommonUX;
import me.coley.recaf.ui.control.tree.CellOriginType;
import me.coley.recaf.ui.util.CellFactory;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.ui.window.GenericWindow;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.Threads;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A prompt where users can type in a class or file name,
 * and quickly open it with only the keyboard <i>(Or mouse if they want)</i>.
 *
 * @author Matt Coley
 */
public class QuickNavPrompt extends GenericWindow {
	private static final AtomicBoolean showing = new AtomicBoolean(false);
	private static final QuickNav nav = new QuickNav();
	private static QuickNavPrompt instance;

	/**
	 * New prompt instance.
	 */
	private QuickNavPrompt() {
		super(nav);
		setAlwaysOnTop(true);
		setTitle(Lang.get("dialog.quicknav"));
		setWidth(500);
		setOnHiding(e -> showing.set(false));
	}

	/**
	 * Hides the prompt.
	 * <br>
	 * Can't name shadow "close" or "hide".
	 */
	private static void vanish() {
		if (showing.get()) {
			instance().hide();
			showing.set(false);
		}
	}

	/**
	 * Shows the prompt.
	 */
	public static void open() {
		if (!showing.get()) {
			showing.set(true);
			instance().show();
			nav.search.requestFocus();
		}
	}

	/**
	 * @return The prompt instance.
	 */
	private static QuickNavPrompt instance() {
		if (instance == null)
			instance = new QuickNavPrompt();
		return instance;
	}

	private static class QuickNav extends BorderPane {
		private static final ExecutorService threadPool = Executors.newSingleThreadExecutor();
		private final TextField search = new TextField();
		private final ListView<ItemWrapper> list = new ListView<>();
		private String lastSearch;

		private QuickNav() {
			search.setPromptText("Search: Class/file name...");
			setOnKeyPressed(e -> {
				if (e.getCode() == KeyCode.ESCAPE) {
					vanish();
				} else if (!search.isFocused() && e.getCode() != KeyCode.ENTER) {
					search.requestFocus();
					search.positionCaret(search.getLength());
				}
			});
			search.setOnKeyPressed(e -> {
				if (e.getCode() == KeyCode.ESCAPE)
					vanish();
				else if (e.getCode().isArrowKey()) {
					list.requestFocus();
				}
			});
			search.setOnKeyReleased(e -> {
				String currentSearch = search.getText();
				if (!currentSearch.equals(lastSearch)) {
					updateSearch(currentSearch);
					lastSearch = currentSearch;
				}
			});
			list.setOnKeyPressed(e -> {
				if (e.getCode() == KeyCode.ESCAPE)
					vanish();
				else if (e.getCode() != KeyCode.ENTER) {
					return;
				}
				ItemWrapper wrapper = list.getSelectionModel().getSelectedItem();
				handle(wrapper);
			});
			list.setCellFactory(c -> new ListCell<ItemWrapper>() {
				@Override
				protected void updateItem(ItemWrapper item, boolean empty) {
					super.updateItem(item, empty);
					if (empty || item == null) {
						setGraphic(null);
						setText(null);
					} else {
						CellFactory.update(CellOriginType.QUICK_NAV, this, item.resource, item.info);
						// Override with full name
						if (item.info instanceof MemberInfo) {
							MemberInfo member = (MemberInfo) item.info;
							String def = member.isMethod() ?
									member.getName() + member.getDescriptor() : member.getName();
							setText(StringUtil.shortenPath(member.getOwner()) + " " + def);
						} else {
							setText(item.info.getName());
						}

					}
				}
			});
			list.setOnMouseClicked(click -> {
				if (click.getClickCount() == 2) {
					ItemWrapper wrapper = list.getSelectionModel().getSelectedItem();
					if (wrapper != null) {
						handle(wrapper);
						vanish();
					}
				}
			});
			setTop(search);
			setCenter(list);
		}

		private void handle(ItemWrapper wrapper) {
			if (wrapper != null) {
				ItemInfo info = wrapper.info;
				vanish();
				if (info instanceof ClassInfo) {
					CommonUX.openClass((ClassInfo) info);
				} else if (info instanceof FileInfo) {
					CommonUX.openFile((FileInfo) info);
				} else if (info instanceof DexClassInfo) {
					CommonUX.openDexClass((DexClassInfo) info);
				} else if (info instanceof MemberInfo) {
					Workspace workspace = RecafUI.getController().getWorkspace();
					if (workspace == null)
						return;
					MemberInfo memberInfo = (MemberInfo) info;
					CommonClassInfo ownerInfo = workspace.getResources().getClass(memberInfo.getOwner());
					if (ownerInfo == null)
						ownerInfo = workspace.getResources().getDexClass(memberInfo.getOwner());
					CommonUX.openMember(ownerInfo, memberInfo);
				}
			}
		}

		private void updateSearch(String text) {
			list.getItems().clear();
			Workspace workspace = RecafUI.getController().getWorkspace();
			if (workspace == null || text.isEmpty())
				return;
			threadPool.submit(() -> {
				// For interruptible support we track the thread interrupt state as a boolean return value.
				// If the value is false we know the thread is interrupted and abort further processing.
				List<ItemWrapper> items = new ArrayList<>();
				boolean results = searchClasses(items, text, workspace.getResources().getPrimary()) &&
						searchFiles(items, text, workspace.getResources().getPrimary());
				if (results)
					Threads.runFx(() -> list.getItems().addAll(items));
			});
		}

		private static boolean searchClasses(List<ItemWrapper> list, String text, Resource resource) {
			text = text.toLowerCase();
			for (ClassInfo info : resource.getClasses().values()) {
				if (Thread.interrupted())
					return false;
				if (info.getName().toLowerCase().contains(text)) {
					list.add(new ItemWrapper(resource, info));
				}
				for (MemberInfo memberInfo : info.getFields()) {
					if (memberInfo.getName().toLowerCase().contains(text)) {
						list.add(new ItemWrapper(resource, memberInfo));
					}
				}
				for (MemberInfo memberInfo : info.getMethods()) {
					if (memberInfo.getName().toLowerCase().contains(text)) {
						list.add(new ItemWrapper(resource, memberInfo));
					}
				}
			}
			for (DexClassInfo info : resource.getDexClasses().values()) {
				if (Thread.interrupted())
					return false;
				if (info.getName().toLowerCase().contains(text)) {
					list.add(new ItemWrapper(resource, info));
				}
			}
			return true;
		}

		private static boolean searchFiles(List<ItemWrapper> list, String text, Resource resource) {
			text = text.toLowerCase();
			for (FileInfo info : resource.getFiles().values()) {
				if (Thread.interrupted())
					return false;
				if (info.getName().toLowerCase().contains(text)) {
					list.add(new ItemWrapper(resource, info));
				}
			}
			return true;
		}

		private static class ItemWrapper {
			private final Resource resource;
			private final ItemInfo info;

			private ItemWrapper(Resource resource, ItemInfo info) {
				this.resource = resource;
				this.info = info;
			}
		}
	}
}
