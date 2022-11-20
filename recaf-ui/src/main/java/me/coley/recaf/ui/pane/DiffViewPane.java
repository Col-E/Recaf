package me.coley.recaf.ui.pane;

import difflib.Chunk;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import javafx.beans.property.IntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.Controller;
import me.coley.recaf.ControllerListener;
import me.coley.recaf.assemble.ContextualPipeline;
import me.coley.recaf.assemble.ast.PrintContext;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.transformer.BytecodeToAstTransformer;
import me.coley.recaf.code.*;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.DiffViewMode;
import me.coley.recaf.ui.behavior.FontSizeChangeable;
import me.coley.recaf.ui.behavior.SaveResult;
import me.coley.recaf.ui.behavior.ScrollSnapshot;
import me.coley.recaf.ui.control.BoundLabel;
import me.coley.recaf.ui.control.PannableImageView;
import me.coley.recaf.ui.control.TextView;
import me.coley.recaf.ui.control.code.Language;
import me.coley.recaf.ui.control.code.Languages;
import me.coley.recaf.ui.control.code.ProblemTracking;
import me.coley.recaf.ui.control.code.SyntaxArea;
import me.coley.recaf.ui.control.code.bytecode.AssemblerArea;
import me.coley.recaf.ui.control.code.java.JavaArea;
import me.coley.recaf.ui.control.hex.HexView;
import me.coley.recaf.ui.control.tree.CellOriginType;
import me.coley.recaf.ui.pane.assembler.AssemblerPane;
import me.coley.recaf.ui.util.CellFactory;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.ByteHeaderUtil;
import me.coley.recaf.util.TextDisplayUtil;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.threading.FxThreadUtil;
import me.coley.recaf.util.threading.ThreadUtil;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.ResourceClassListener;
import me.coley.recaf.workspace.resource.ResourceDexClassListener;
import me.coley.recaf.workspace.resource.ResourceFileListener;
import org.fxmisc.flowless.Virtualized;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A simple diff viewer to show a comparison of the changes made to classes/files.
 *
 * @author Matt Coley
 */
public class DiffViewPane extends BorderPane implements ControllerListener,
		ResourceClassListener, ResourceDexClassListener, ResourceFileListener, FontSizeChangeable {
	private static final Logger logger = Logging.get(DiffViewPane.class);
	private static final long TIMEOUT_MS = 10_000;
	private final ObservableList<ItemInfo> items = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
	private Workspace workspace;

	/**
	 * Create the diff viewer, and add listeners to the controller so that we can live update which
	 * classes and files have history to display.
	 *
	 * @param controller
	 * 		Controller to add listener to.
	 */
	public DiffViewPane(Controller controller) {
		controller.addListener(this);
		// Primary display of selected item
		BorderPane content = new BorderPane();
		// List of modified items (classes/files/etc)
		ListView<ItemInfo> listView = new ListView<>();
		listView.setMinWidth(220);
		listView.setItems(items);
		listView.setCellFactory(c -> new ListCell<>() {
			@Override
			protected void updateItem(ItemInfo item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setGraphic(null);
					setText(null);
					setOnMousePressed(null);
				} else {
					CellFactory.update(CellOriginType.SEARCH_RESULTS, this, workspace.getResources().getPrimary(), item);
					// Override with full name
					setText(TextDisplayUtil.shortenEscapeLimit(item.getName()));
					setOnMousePressed(e -> {
						SplitPane pane = (SplitPane) createDiffDisplay(item);
						pane.getItems().forEach(paneItem -> {
							if (paneItem instanceof FontSizeChangeable) {
								((FontSizeChangeable) paneItem).bindFontSize(Configs.display().fontSize);
								((FontSizeChangeable) paneItem).applyEventsForFontSizeChange(FontSizeChangeable.DEFAULT_APPLIER);
							}
						});
						content.setCenter(pane);
					});
				}
			}
		});
		// Layout
		SplitPane split = new SplitPane();
		split.getItems().addAll(listView, content);
		setCenter(split);
		SplitPane.setResizableWithParent(listView, false);
	}

	private Node createDiffDisplay(ItemInfo item) {
		Resource primary = workspace.getResources().getPrimary();
		SplitPane split = new SplitPane();
		if (item instanceof ClassInfo) {
			Stack<ClassInfo> history = primary.getClasses().getHistory(item.getName());
			if (history == null)
				return new BoundLabel(Lang.getBinding("modifications.none"));
			ClassInfo current = (ClassInfo) item;
			ClassInfo initial = history.firstElement();
			// Create a countdown for the two classes to decompile
			DiffViewMode mode = Configs.editor().diffViewMode;
			if (mode == DiffViewMode.DECOMPILE) {
				CompletableFuture<Void> currentFuture = new CompletableFuture<>();
				CompletableFuture<Void> initialFuture = new CompletableFuture<>();
				DiffableDecompilePane currentDecompile = new DiffableDecompilePane(currentFuture);
				DiffableDecompilePane initialDecompile = new DiffableDecompilePane(initialFuture);
				currentDecompile.onUpdate(current);
				initialDecompile.onUpdate(initial);
				// Add to the UI
				split.getItems().addAll(initialDecompile, currentDecompile);
				// When the class versions both get decompiled, run a basic text diff and highlight modified lines.
				CompletableFuture.allOf(currentFuture, initialFuture)
						.orTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
						.whenCompleteAsync((__, t) -> {
							if (t != null) {
								logger.error("Failed to make diff view", t);
							} else {
								highlightDiff(initialDecompile, currentDecompile);
							}
						}, ThreadUtil.executor());
			} else {
				DiffAssemblerPane currentAssembler = new DiffAssemblerPane();
				DiffAssemblerPane initialAssembler = new DiffAssemblerPane();
				// Add to the UI
				split.getItems().addAll(initialAssembler, currentAssembler);
				// Create the disassembly by passing in the class states
				currentAssembler.onUpdate(current);
				initialAssembler.onUpdate(initial);
				// The disassembly is on the same thread and is fast, so we can immediately highlight
				highlightDiff(initialAssembler, currentAssembler);
			}
			return split;
		} else if (item instanceof DexClassInfo) {
			// TODO: Android diff
			//  - Probably just go with 'SmaliAssemblerPane' similar to how the java logic above is done
			//    and do the same standard text-diff highlighting.
			return new Label("TODO: Android diff");
		} else if (item instanceof FileInfo) {
			Stack<FileInfo> history = primary.getFiles().getHistory(item.getName());
			FileInfo current = (FileInfo) item;
			FileInfo initial = history.firstElement();
			byte[] currentRaw = current.getValue();
			byte[] initialRaw = initial.getValue();
			if (ByteHeaderUtil.matchAny(currentRaw, ByteHeaderUtil.IMAGE_HEADERS)) {
				PannableImageView initialImage = new PannableImageView();
				PannableImageView currentImage = new PannableImageView();
				initialImage.setImage(initialRaw);
				currentImage.setImage(currentRaw);
				split.getItems().addAll(initialImage, currentImage);
			} else if (current.isText()) {
				Language language = Languages.get(current.getExtension());
				DiffableTextView initialText = new DiffableTextView(language);
				DiffableTextView currentText = new DiffableTextView(language);
				initialText.onUpdate(initial);
				currentText.onUpdate(current);
				initialText.getTextArea().setEditable(false);
				currentText.getTextArea().setEditable(false);
				split.getItems().addAll(initialText, currentText);
				highlightDiff(initialText, currentText);
				currentText.bindScrollTo(initialText);
			} else {
				// TODO: How do we want to go around highlighting hex diffs?
				//  - Representing 'inserted' and 'removed' can't use padding like text-view can
				//  - Can just do simple direct highlights without padding probably
				//     - Need access to each cell/offset to do this
				HexView initialHex = new HexView();
				HexView currentHex = new HexView();
				initialHex.onUpdate(initialRaw);
				currentHex.onUpdate(currentRaw);
				// TODO: Should update HexView to support 'setEditable(boolean)' which disables HexRow editable cells.
				split.getItems().addAll(initialHex, currentHex);
			}
			return split;
		} else {
			throw new IllegalStateException("Unknown info type: " + ((item == null) ? "null" : item.getClass()));
		}
	}

	private void highlightDiff(Diffable initial, Diffable current) {
		Patch<String> diff = DiffUtils.diff(
				Arrays.asList(initial.getText().split("\n")),
				Arrays.asList(current.getText().split("\n"))
		);
		if (diff.getDeltas().isEmpty()) {
			return;
		}
		// Without the delay, the text insertions fight with text styling thread
		FxThreadUtil.delayedRun(100, () -> {
			int deletions = 0;
			int insertions = 0;
			int currentOffset = 0;
			int initialOffset = 0;
			for (Delta<String> delta : diff.getDeltas()) {
				Chunk<String> original = delta.getOriginal();
				Chunk<String> revised = delta.getRevised();
				switch (delta.getType()) {
					case CHANGE:
						// Changes can also insert/remove lines
						if (revised.size() > original.size()) {
							// Change has inserted text
							current.markDiffChunk(revised, "insertion", deletions - 1);
							int insertStartLine = original.getPosition() + initialOffset;
							int insertedCount = revised.size() - 1;
							int chunkDiffOR = original.getPosition() - revised.getPosition();
							initial.getCodeArea().insertText(insertStartLine, 0, "\n".repeat(insertedCount));
							initial.markDiffChunk(revised, "insertion", chunkDiffOR + initialOffset - 1);
							insertions += insertedCount;
							initialOffset += insertedCount;
						} else if (revised.size() < original.size()) {
							// Change has removed text
							initial.markDiffChunk(original, "deletion", insertions - 1);
							int deleteStartLine = revised.getPosition() + currentOffset;
							int deletedCount = original.size() - 1;
							int chunkDiffRO = revised.getPosition() - original.getPosition();
							current.getCodeArea().insertText(deleteStartLine, 0, "\n".repeat(deletedCount));
							current.markDiffChunk(original, "deletion", chunkDiffRO + currentOffset - 1);
							deletions += deletedCount;
							currentOffset += deletedCount;
						} else {
							// Change has modified an equal number of lines
							current.markDiffChunk(revised, "change", deletions);
							initial.markDiffChunk(original, "change", insertions);
						}
						break;
					case DELETE:
						initial.markDiffChunk(original, "deletion", insertions);
						int deleteStartLine = revised.getPosition() + currentOffset;
						int deletedCount = original.size();
						int chunkDiffRO = revised.getPosition() - original.getPosition();
						current.getCodeArea().insertText(deleteStartLine, 0, "\n".repeat(deletedCount));
						current.markDiffChunk(original, "deletion", chunkDiffRO + currentOffset);
						deletions += deletedCount;
						currentOffset += deletedCount;
						break;
					case INSERT:
						current.markDiffChunk(revised, "insertion", deletions);
						int insertStartLine = original.getPosition() + initialOffset;
						int insertedCount = revised.size();
						int chunkDiffOR = original.getPosition() - revised.getPosition();
						initial.getCodeArea().insertText(insertStartLine, 0, "\n".repeat(insertedCount));
						initial.markDiffChunk(revised, "insertion", chunkDiffOR + initialOffset);
						insertions += insertedCount;
						initialOffset += insertedCount;
						break;
				}
			}
			// Now that text insertions are done, we can link the two diffable components scrolling
			current.bindScrollTo(initial);
		});
		initial.getCodeArea().showParagraphAtCenter(0);
	}

	@Override
	public void onNewWorkspace(Workspace oldWorkspace, Workspace newWorkspace) {
		workspace = newWorkspace;
		// Clear old items
		items.clear();
		// Add listeners to receive new items
		if (newWorkspace != null) {
			Resource resource = newWorkspace.getResources().getPrimary();
			resource.addClassListener(this);
			resource.addDexListener(this);
			resource.addFileListener(this);
		}
	}

	@Override
	public void onNewClass(Resource resource, ClassInfo newValue) {
		items.add(newValue);
		items.sort(Comparator.comparing(ItemInfo::getName));
	}

	@Override
	public void onRemoveClass(Resource resource, ClassInfo oldValue) {
		items.remove(oldValue);
	}

	@Override
	public void onUpdateClass(Resource resource, ClassInfo oldValue, ClassInfo newValue) {
		items.remove(oldValue);
		items.add(newValue);
		items.sort(Comparator.comparing(ItemInfo::getName));
	}

	@Override
	public void onNewDexClass(Resource resource, String dexName, DexClassInfo newValue) {
		items.add(newValue);
		items.sort(Comparator.comparing(ItemInfo::getName));
	}

	@Override
	public void onRemoveDexClass(Resource resource, String dexName, DexClassInfo oldValue) {
		items.remove(oldValue);
	}

	@Override
	public void onUpdateDexClass(Resource resource, String dexName, DexClassInfo oldValue, DexClassInfo newValue) {
		items.remove(oldValue);
		items.add(newValue);
		items.sort(Comparator.comparing(ItemInfo::getName));
	}

	@Override
	public void onNewFile(Resource resource, FileInfo newValue) {
		items.add(newValue);
		items.sort(Comparator.comparing(ItemInfo::getName));
	}

	@Override
	public void onRemoveFile(Resource resource, FileInfo oldValue) {
		items.remove(oldValue);
	}

	@Override
	public void onUpdateFile(Resource resource, FileInfo oldValue, FileInfo newValue) {
		items.remove(oldValue);
		items.add(newValue);
		items.sort(Comparator.comparing(ItemInfo::getName));
	}

	@Override
	public void bindFontSize(IntegerProperty property) {
		// will be done upon opening the view
	}

	@Override
	public void applyEventsForFontSizeChange(Consumer<Node> consumer) {
		// will be done upon opening the view
	}

	/**
	 * Simple interface to make text-diff logic backed by {@link CodeArea} easy to implement.
	 *
	 * @author Matt Coley
	 */
	public interface Diffable {
		default void markDiffChunk(Chunk<String> chunk, String style, int offset) {
			CodeArea area = getCodeArea();
			int pos = chunk.getPosition() + offset;
			int len = chunk.size();
			Collection<String> styles = Collections.singleton(style);
			for (int i = 0; i < len; i++)
				area.setParagraphStyle(pos + i, styles);
		}

		default void bindScrollTo(Diffable other) {
			getScroll().estimatedScrollYProperty().bindBidirectional(other.getScroll().estimatedScrollYProperty());
		}

		default String getText() {
			return getCodeArea().getText();
		}

		Virtualized getScroll();

		CodeArea getCodeArea();
	}

	/**
	 * An extension of the text-view for line difference highlighting.
	 *
	 * @author Matt Coley
	 */
	private static class DiffableTextView extends TextView implements Diffable {
		/**
		 * @param language
		 * 		Language to use for syntax highlighting.
		 */
		public DiffableTextView(Language language) {
			super(language, null);
		}

		@Override
		public CodeArea getCodeArea() {
			return getTextArea();
		}

		@Override
		public VirtualizedScrollPane<SyntaxArea> getScroll() {
			return super.getScroll();
		}

		@Override
		public SaveResult save() {
			return SaveResult.IGNORED;
		}

		@Override
		public boolean supportsEditing() {
			return false;
		}
	}

	/**
	 * An extension of the Java decompile pane for line difference highlighting.
	 *
	 * @author Matt Coley
	 */
	private static class DiffableDecompilePane extends DecompilePane implements Diffable {
		private final CompletableFuture<Void> future;
		private String code;

		public DiffableDecompilePane(CompletableFuture<Void> future) {
			this.future = future;
			getJavaArea().setEditable(false);
			// This is a lazy fix.
			// Prevent users from switching decompiler and thus invalidating the text.
			setBottom(null);
		}

		/**
		 * @return Decompiled code.
		 */
		public String getCode() {
			return code;
		}

		@Override
		public CodeArea getCodeArea() {
			return getJavaArea();
		}

		@Override
		public VirtualizedScrollPane<JavaArea> getScroll() {
			return super.getScroll();
		}

		@Override
		public SaveResult save() {
			return SaveResult.IGNORED;
		}

		@Override
		public boolean supportsEditing() {
			return false;
		}

		@Override
		public ScrollSnapshot makeScrollSnapshot() {
			return () -> {
				// no-op
			};
		}

		@Override
		protected void onDecompileCompletion(String code) {
			this.code = code;
			this.future.complete(null);
		}
	}

	/**
	 * An extension of the assembler pane for line difference highlighting.
	 *
	 * @author Justus Garbe
	 */
	private static class DiffAssemblerPane extends AssemblerPane implements Diffable {
		public DiffAssemblerPane() {
			getAssemblerArea().setEditable(false);
		}

		@Override
		protected AssemblerArea createAssembler(ProblemTracking tracking, ContextualPipeline pipeline) {
			return new AssemblerArea(tracking, pipeline) {
				@Override
				protected void handleAstUpdate() {
					// Do nothing, we only care about the text.
					// It does not need to be parsed.
				}
			};
		}

		@Override
		public VirtualizedScrollPane<AssemblerArea> getScroll() {
			return super.getScroll();
		}

		@Override
		public boolean supportsEditing() {
			return false;
		}

		@Override
		public SaveResult save() {
			return SaveResult.IGNORED;
		}

		@Override
		public CodeArea getCodeArea() {
			return super.getAssemblerArea();
		}

		@Override
		public void onUpdate(CommonClassInfo newValue) {
			if (newValue instanceof ClassInfo) {
				ClassInfo classInfo = (ClassInfo) newValue;
				// Find class node
				ClassReader reader = classInfo.getClassReader();
				ClassNode node = new ClassNode();
				reader.accept(node, ClassReader.SKIP_FRAMES);
				// Transform the node to text representation
				getAssemblerArea().onUpdate(newValue);
				BytecodeToAstTransformer transformer = new BytecodeToAstTransformer(node);
				transformer.visit();
				Unit unit = transformer.getUnit();
				String result = unit.print(PrintContext.DEFAULT_CTX);
				// Update code area
				getAssemblerArea().setText(result);
			}
		}
	}
}
