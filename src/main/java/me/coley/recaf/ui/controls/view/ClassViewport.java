package me.coley.recaf.ui.controls.view;

import javafx.application.Platform;
import javafx.scene.input.KeyEvent;
import me.coley.recaf.config.ConfigManager;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.decompile.DecompileImpl;
import me.coley.recaf.plugin.PluginKeybinds;
import me.coley.recaf.ui.controls.ClassEditor;
import me.coley.recaf.ui.controls.FontSlider;
import me.coley.recaf.ui.controls.HexEditor;
import me.coley.recaf.ui.controls.popup.SuggestionWindow;
import me.coley.recaf.ui.controls.node.ClassNodeEditorPane;
import me.coley.recaf.ui.controls.text.JavaEditorPane;
import me.coley.recaf.util.*;
import me.coley.recaf.workspace.History;
import me.coley.recaf.workspace.JavaResource;
import org.fxmisc.richtext.CodeArea;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Multi-view wrapper for classes in resources.
 *
 * @author Matt
 */
public class ClassViewport extends EditorViewport {
	private ClassMode overrideMode;
	private DecompileImpl overrideDecompiler;
	private double lastScrollX;
	private double lastScrollY;

	/**
	 * @param controller
	 * 		Controller context.
	 * @param resource
	 * 		Resource the file resides in.
	 * @param path
	 * 		Path to file.
	 */
	public ClassViewport(GuiController controller, JavaResource resource, String path) {
		super(controller, resource, path);
	}

	@Override
	protected void handleKeyReleased(KeyEvent e) {
		super.handleKeyReleased(e);
		// Custom bind support
		PluginKeybinds.getInstance().getClassViewBinds().forEach((bind, action) -> {
			try {
				if (bind.match(e))
					action.accept(this);
			} catch(Throwable t) {
				Log.error(t, "Failed executing class keybind action");
			}
		});

		ConfigManager config = controller.config();

		if(config.keys().swapview.match(e)) {
			setOverrideMode(ClassMode.values()[(getClassMode().ordinal() + 1) % ClassMode.values().length]);
			requestFocus();
		}

		if(config.keys().incFontSize.match(e)) {
			config.display().monoFontSize++;
			FontSlider.update(controller);
			config.save();
		}

		if(config.keys().decFontSize.match(e)) {
			config.display().monoFontSize--;
			FontSlider.update(controller);
			config.save();
		}
	}

	@Override
	protected History getHistory(String path) {
		return resource.getClassHistory(path);
	}

	@Override
	protected Map<String, byte[]> getMap() {
		return resource.getClasses();
	}

	@Override
	public void updateView() {
		switch(getClassMode()) {
			case DECOMPILE: {
				// Fetch decompiler

				DecompileImpl decompiler = getDecompiler();
				long timeout = controller.config().decompile().timeout;
				boolean showSuggestions = controller.config().display().suggestClassWithErrors;
				// Get or create pane
				String initialText = "// Decompiling class: " + path + "\n" +
						"// - Decompiler: " + decompiler.getNameAndVersion() + "\n";
				JavaEditorPane pane = null;
				if (getCenter() instanceof JavaEditorPane) {
					pane = (JavaEditorPane) getCenter();
					if (!pane.getCodeArea().getText().equals(initialText)){
						trySaveCurrentScrollPosition();
					}
					pane.setText(initialText);
				} else {
					pane = new JavaEditorPane(controller, resource, initialText);
					pane.setWrapText(controller.config().display().forceWordWrap);
					setCenter(pane);
				}
				pane.setEditable(pane.canCompile() && resource.isPrimary());
				// Actions
				Supplier<String> supplier = () -> {
					// SUPPLIER: Fetch decompiled code
					String decompilerPrefix = (controller.config().decompile().showName ?
							"// Decompiled with: " + decompiler.getNameAndVersion() + "\n" : "");
					byte[] clazz = controller.getWorkspace().getRawClass(path);
					int version = ClassUtil.getVersion(clazz) - ClassUtil.VERSION_OFFSET;
					String classVersionPrefix = "// Class Version: " + version + "\n";
					String decompile = decompilerPrefix + classVersionPrefix +
							decompiler.create(controller).decompile(path);
					return EscapeUtil.unescapeUnicode(decompile);
				};
				JavaEditorPane finalPane = pane;
				Consumer<String> consumer = decompile -> {
					// CONSUMER: Set decompiled text and check for errors
					// Update text
					Platform.runLater(() -> {
						finalPane.setText(decompile);
						finalPane.forgetHistory();
						if (lastScrollY > 0) {
							finalPane.getCodeArea().scrollXToPixel(lastScrollX);
							finalPane.getCodeArea().scrollYToPixel(lastScrollY);
						} else {
							finalPane.scrollToTop();
						}
					});
					// Sometimes the code analysis gets stuck on the initial commented out text...
					// This checks for getting stuck and forces an update. Hacky, but does the job.
					ThreadUtil.runJfxDelayed(600, () -> {
						int errorCheckDelay = 0;
						if (finalPane.getAnalyzedCode().getCode().length() != decompile.length()) {
							finalPane.appendText(" ");
							errorCheckDelay = 400;
						}
						// Show popup suggesting switching modes when the decompile has errors
						if (showSuggestions) {
							ThreadUtil.runJfxDelayed(errorCheckDelay, () -> {
								if(finalPane.getErrorHandler().hasErrors()) {
									SuggestionWindow.suggestAltDecompile(controller, this).show(this);
								}
							});
						}
					});
				};
				Runnable timeoutAction = () -> {
					// TIMEOUT: Suggest another decompiler
					Platform.runLater(() -> {
						finalPane.appendText("// \n// Timed out after " + timeout + " ms\n// \n" +
								"// Suggestion: Change the decompiler or switch the class mode to " +
								ClassMode.TABLE.name());
					});
					// Show popup suggesting switching modes when the decompile fails
					if(showSuggestions) {
						ThreadUtil.runJfxDelayed(100, () -> {
							SuggestionWindow.suggestTimeoutDecompile(controller, this).show(this);
						});
					}
				};
				Consumer<Throwable> handler = t -> {
					// ERROR-HANDLER: Print decompile error
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					t.printStackTrace(pw);
					String decompile = LangUtil.translate("decompile.fail") + "\n\nError Message: "
							+ t.getMessage() + "\n\nStackTrace:\n" + sw.toString();
					finalPane.setEditable(false);
					// Show popup suggesting switching modes when the decompile fails
					if(showSuggestions) {
						ThreadUtil.runJfxDelayed(100, () -> {
							SuggestionWindow.suggestFailedDecompile(controller, this).show(this);
						});
					}
					// Update text
					Platform.runLater(() -> {
						finalPane.appendText("\n/*\n" + decompile + "\n*/");
						finalPane.forgetHistory();
					});
				};
				// Run actions
				ThreadUtil.runSupplyConsumer(supplier, timeout, timeoutAction, consumer, handler);
				break;
			}
			case TABLE: {
				// TODO: like how Recaf was in 1.X
				ClassNodeEditorPane pane = null;
				ClassReader cr = controller.getWorkspace().getClassReader(path);
				ClassNode node = ClassUtil.getNode(cr, ClassReader.SKIP_FRAMES);
				if(getCenter() instanceof ClassNodeEditorPane) {
					pane = (ClassNodeEditorPane) getCenter();
					pane.refresh(node);
				} else {
					pane = new ClassNodeEditorPane(controller, node);
					setCenter(pane);
				}
				break;
			}
			case HEX:
			default:
				HexEditor hex = new HexEditor(last);
				hex.setContentCallback(array -> current = array);
				hex.setEditable(resource.isPrimary());
				setCenter(hex);
				break;
		}
	}

	@Override
	public void save() {
		// Handle saving for editing decompiled java
		if (getCenter() instanceof JavaEditorPane) {
			try {
				Map<String, byte[]> map = ((ClassEditor) getCenter()).save(path);
				current = map.get(path);
				// Save other inners
				map.remove(path);
				JavaResource resource = controller.getWorkspace().getPrimary();
				map.forEach((key, value) -> {
					resource.getClasses().put(key, value);
					resource.getClassHistory(key).push(value);
				});
			} catch(UnsupportedOperationException ex) {
				Log.warn("Recompiling not supported. Please run Recaf with a JDK.", path);
				return;
			} catch(Exception ex) {
				Log.error("Failed recompiling code for '{}' - Reason: '{}'", path, ex.getMessage());
				return;
			}
		} else if (getCenter() instanceof ClassNodeEditorPane) {
			try {
				current = ((ClassEditor) getCenter()).save(path).get(path);
			} catch(IllegalStateException ex) {
				Log.error("Failed saving changes for '{}' - Reason: '{}'", path, ex.getMessage());
				return;
			} catch(Throwable t) {
				Log.error(t, "Failed saving changes for '{}' - Uncaught exception", path);
				return;
			}
		}
		// Save content
		super.save();
	}

	/**
	 * Jump to the definition of the given member.
	 *
	 * @param name
	 * 		Member name.
	 * @param desc
	 * 		Member descriptor.
	 */
	public void selectMember(String name, String desc) {
		if (getCenter() instanceof  ClassEditor)
			((ClassEditor)getCenter()).selectMember(name, desc);
	}

	/**
	 * Set a new mode to view classes in then refresh the view.
	 *
	 * @param overrideMode
	 * 		New mode to view classes in.
	 */
	public void setOverrideMode(ClassMode overrideMode) {
		this.overrideMode = overrideMode;
		updateView();
	}

	/**
	 * Set the tab's decompiler then refresh the view.
	 * @param overrideDecompiler New mode to view classes in.
	 */
	public void setOverrideDecompiler(DecompileImpl overrideDecompiler) {
		this.overrideDecompiler = overrideDecompiler;
		// Ensure the mode is set to decompile & refresh the view
		setOverrideMode(ClassMode.DECOMPILE);
	}

	/**
	 * @return Decompiler to use when the {@link #getClassMode() mode} is set to decompile.
	 */
	public DecompileImpl getDecompiler() {
		if (overrideDecompiler != null)
			return overrideDecompiler;
		return controller.config().decompile().decompiler;
	}

	/**
	 * @return Mode that indicated which view to use for modifying classes.
	 */
	public ClassMode getClassMode() {
		if (overrideMode != null)
			return overrideMode;
		return controller.config().display().classEditorMode;
	}

	/**
	 * @return Controller
	 */
	public GuiController getController() {
		return controller;
	}

	private void trySaveCurrentScrollPosition(){
		if (getCenter() instanceof JavaEditorPane){
			CodeArea codeArea = ((JavaEditorPane) getCenter()).getCodeArea();
			lastScrollX = codeArea.getEstimatedScrollX();
			lastScrollY = codeArea.getEstimatedScrollY();
		}
	}

	/**
	 * Viewport editor type.
	 */
	public enum ClassMode {
		DECOMPILE, TABLE, HEX;

		@Override
		public String toString() {
			return StringUtil.toString(this);
		}
	}
}
