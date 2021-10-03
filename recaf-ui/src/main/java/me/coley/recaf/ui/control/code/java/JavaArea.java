package me.coley.recaf.ui.control.code.java;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithRange;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.ContextMenuEvent;
import me.coley.recaf.Controller;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.*;
import me.coley.recaf.compile.CompileManager;
import me.coley.recaf.compile.CompileOption;
import me.coley.recaf.compile.Compiler;
import me.coley.recaf.compile.CompilerResult;
import me.coley.recaf.compile.javac.JavacCompiler;
import me.coley.recaf.config.Configs;
import me.coley.recaf.config.container.CompilerConfig;
import me.coley.recaf.parse.JavaParserHelper;
import me.coley.recaf.parse.JavaParserPrinting;
import me.coley.recaf.parse.WorkspaceTypeSolver;
import me.coley.recaf.ui.behavior.ClassRepresentation;
import me.coley.recaf.ui.behavior.SaveResult;
import me.coley.recaf.ui.context.ContextBuilder;
import me.coley.recaf.ui.control.code.*;
import me.coley.recaf.ui.util.ScrollUtils;
import me.coley.recaf.util.ClearableThreadPool;
import me.coley.recaf.util.JavaVersion;
import me.coley.recaf.util.Threads;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.Workspace;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.fxmisc.flowless.Virtualized;
import org.fxmisc.richtext.CharacterHit;
import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.richtext.model.TwoDimensional;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static me.coley.recaf.parse.JavaParserResolving.resolvedValueToInfo;

/**
 * Syntax area implementation with a focus on Java specific behavior.
 *
 * @author Matt Coley
 */
public class JavaArea extends SyntaxArea implements ClassRepresentation {
	private static final Logger logger = Logging.get(JavaArea.class);
	private final ClearableThreadPool threadPool = new ClearableThreadPool(1, true, "Java AST Parse");
	private CommonClassInfo lastInfo;
	private CompilationUnit lastAST;
	private boolean isLastAstCurrent;
	private Future<?> parseFuture;
	private ContextMenu menu;

	/**
	 * @param problemTracking
	 * 		Optional problem tracking implementation to enable line problem indicators.
	 */
	public JavaArea(ProblemTracking problemTracking) {
		super(Languages.JAVA, problemTracking);
		setOnContextMenuRequested(this::onMenuRequested);
	}

	@Override
	protected void onTextChanged(PlainTextChange change) {
		super.onTextChanged(change);
		// Queue up new parse task, killing prior task if present
		if (threadPool.hasActiveThreads()) {
			threadPool.clear();
		}
		parseFuture = threadPool.submit(this::updateParse);
	}

	@Override
	public CommonClassInfo getCurrentClassInfo() {
		return lastInfo;
	}

	@Override
	public boolean supportsMemberSelection() {
		return true;
	}

	@Override
	public boolean isMemberSelectionReady() {
		return lastAST != null;
	}

	@Override
	public void selectMember(MemberInfo memberInfo) {
		Threads.run(() -> {
			long timeout = Configs.editor().decompileTimeout + 500;
			try {
				Awaitility.await()
						.timeout(timeout, TimeUnit.MILLISECONDS)
						.until(() -> isLastAstCurrent);
				if (parseFuture == null) {
					logger.warn("Cannot select member, parse thread was not initialized!");
				} else if (!parseFuture.isDone() && lastAST == null) {
					logger.warn("Cannot select member, parse thread yielded no valid parse result!");
				} else {
					// Parse thread done, and AST result should be present
					doSelectMember(memberInfo);
				}
			} catch (ConditionTimeoutException e) {
				logger.warn("Cannot select member, member selection timed out after {}ms!", timeout);
			}
		});
	}

	private void doSelectMember(MemberInfo memberInfo) {
		WorkspaceTypeSolver solver = RecafUI.getController().getServices().getTypeSolver();
		if (memberInfo.isField()) {
			lastAST.findFirst(FieldDeclaration.class, dec -> {
				MemberInfo declaredInfo = (MemberInfo) resolvedValueToInfo(solver, dec.resolve());
				return memberInfo.equals(declaredInfo);
			}).flatMap(NodeWithRange::getBegin).ifPresent(this::selectPosition);
		} else if (memberInfo.getName().equals("<init>")) {
			lastAST.findFirst(ConstructorDeclaration.class, dec -> {
				MemberInfo declaredInfo = (MemberInfo) resolvedValueToInfo(solver, dec.resolve());
				return memberInfo.equals(declaredInfo);
			}).flatMap(NodeWithRange::getBegin).ifPresent(this::selectPosition);
		} else {
			lastAST.findFirst(MethodDeclaration.class, dec -> {
				MemberInfo declaredInfo = (MemberInfo) resolvedValueToInfo(solver, dec.resolve());
				return memberInfo.equals(declaredInfo);
			}).flatMap(NodeWithRange::getBegin).ifPresent(this::selectPosition);
		}
	}

	@Override
	public void onUpdate(CommonClassInfo newValue) {
		lastInfo = newValue;
	}

	@Override
	public void cleanup() {
		super.cleanup();
		threadPool.clear();
		threadPool.shutdownNow();
	}

	@Override
	public SaveResult save() {
		Controller controller = RecafUI.getController();
		Workspace workspace = controller.getWorkspace();
		// For now there is only one implementation, plain ol "javac"
		CompilerConfig config = Configs.compiler();
		String compilerName = config.impl;
		CompileManager compileManager = controller.getServices().getCompileManager();
		Compiler compiler = compileManager.get(compilerName);
		boolean findCompiler = false;
		if (compiler == null) {
			logger.warn("Unknown compiler: '{}'.", compilerName);
			findCompiler = true;
		} else if (!compiler.isAvailable()) {
			logger.warn("Unavailable compiler: '{}' - registered, but is not supported", compilerName);
			findCompiler = true;
		}
		// Find first available compiler
		if (findCompiler) {
			compiler = null;
			for (Compiler impl : compileManager.getRegisteredImpls()) {
				if (impl.isAvailable()) {
					logger.warn("Falling back to '{}'.", impl.getName());
					config.impl = impl.getName();
					compiler = impl;
					break;
				}
			}
		}
		// If it is still null
		if (compiler == null) {
			logger.error("There are no available compilers.");
			return SaveResult.FAILURE;
		}
		try {
			// Reset compiler problems
			getProblemTracking().clearOfType(ProblemOrigin.COMPILER);
			// Gather info
			String classSource = getText();
			String className = getClassName();
			int version = getCompileTargetVersion();
			// Configure the compiler
			Map<String, CompileOption<?>> options = new HashMap<>(compiler.getDefaultOptions());
			compiler.setDebug(options, JavacCompiler.createDebugValue(
					config.debugVars, config.debugLines, config.debugSourceName));
			compiler.setTarget(options, version);
			// Invoke and handle result
			CompilerResult result = compiler.compile(className, classSource, options);
			if (result.wasSuccess()) {
				result.getValue().forEach((name, value) -> {
					logger.info("Updating '{}'", name);
					workspace.getResources().getPrimary().getClasses().put(ClassInfo.read(value));
				});
				return SaveResult.SUCCESS;
			} else {
				result.getErrors().forEach(diag -> {
					int line = diag.getLine();
					ProblemInfo info = new ProblemInfo(ProblemOrigin.COMPILER, ProblemLevel.ERROR,
							line, diag.getMessage());
					getProblemTracking().addProblem(line, info);
				});
				return SaveResult.FAILURE;
			}
		} catch (Throwable t) {
			t.printStackTrace();
			logger.error("Failed to compile", t);
			return SaveResult.FAILURE;
		}
	}

	@Override
	public boolean supportsEditing() {
		// The save keybind should invoke the compiler
		return true;
	}

	@Override
	public Node getNodeRepresentation() {
		return this;
	}

	/**
	 * @return Current internal name of the class.
	 */
	private String getClassName() {
		// Use given class name
		CommonClassInfo classInfo = getCurrentClassInfo();
		if (classInfo != null)
			return classInfo.getName();
		// Find it via the source
		String className = JavaParserPrinting.getType(lastAST.getType(0).resolve());
		if (className.charAt(0) == '/')
			className = className.substring(1);
		return className;
	}

	/**
	 * @return Target version to compile with.
	 */
	private int getCompileTargetVersion() {
		// Default to current VM version. We can't compile above this version anyways.
		int version = JavaVersion.get();
		// Use given class version
		CommonClassInfo classInfo = getCurrentClassInfo();
		if (classInfo instanceof ClassInfo) {
			int classVersion = ((ClassInfo) classInfo).getVersion() - JavaVersion.VERSION_OFFSET;
			if (classVersion > version) {
				logger.warn("The class '{}' was compiled with version '{}' " +
						"but the current JVM only supports up to '{}", getClassName(), classVersion, version);
			} else {
				version = classVersion;
			}
		}
		return version;
	}

	private void onMenuRequested(ContextMenuEvent e) {
		// Close old menu
		if (menu != null) {
			menu.hide();
			menu = null;
		}
		// Check if there is parsable AST info
		if (lastAST == null) {
			// TODO: More visually noticeable warning to user that the AST failed to be parsed
			//  - Offer to switch class representation?
			logger.warn("Could not request context menu since the code is not parsable!");
			return;
		}
		// Convert the event position to line/column
		CharacterHit hit = hit(e.getX(), e.getY());
		Position hitPos = offsetToPosition(hit.getInsertionIndex(),
				TwoDimensional.Bias.Backward);
		int line = hitPos.getMajor() + 1; // Position is 0 indexed
		int column = hitPos.getMinor();
		// Sync caret
		moveTo(hit.getInsertionIndex());
		// Check if there is info about the selected item
		JavaParserHelper helper = RecafUI.getController().getServices().getJavaParserHelper();
		Optional<ItemInfo> infoAtPosition = helper.at(lastAST, line, column);
		if (infoAtPosition.isPresent()) {
			if (infoAtPosition.get() instanceof ClassInfo) {
				ClassInfo info = (ClassInfo) infoAtPosition.get();
				menu = ContextBuilder.forClass(info).build();
			} else if (infoAtPosition.get() instanceof DexClassInfo) {
				DexClassInfo info = (DexClassInfo) infoAtPosition.get();
				menu = ContextBuilder.forDexClass(info).build();
			} else if (infoAtPosition.get() instanceof FieldInfo) {
				FieldInfo info = (FieldInfo) infoAtPosition.get();
				CommonClassInfo owner = RecafUI.getController().getWorkspace()
						.getResources().getClass(info.getOwner());
				if (owner != null)
					menu = ContextBuilder.forField(owner, info).build();
			} else if (infoAtPosition.get() instanceof MethodInfo) {
				MethodInfo info = (MethodInfo) infoAtPosition.get();
				CommonClassInfo owner = RecafUI.getController().getWorkspace()
						.getResources().getClass(info.getOwner());
				if (owner != null)
					menu = ContextBuilder.forMethod(owner, info).build();
			}
		}
		// Show if present
		if (menu != null) {
			menu.setAutoHide(true);
			menu.setHideOnEscape(true);
			menu.show(getScene().getWindow(), e.getScreenX(), e.getScreenY());
			menu.requestFocus();
		} else {
			logger.warn("No class or member at selected position [line {}, column {}]", line, column);
		}
	}

	/**
	 * @param text
	 * 		Text to set.
	 */
	public void setText(String text) {
		setText(text, true);
	}

	private void setText(String text, boolean keepPosition) {
		if (keepPosition) {
			// Record prior caret position
			int caret = getCaretPosition();
			// Record prior scroll position
			double estimatedScrollY = 0;
			if (getParent() instanceof Virtualized) {
				Virtualized virtualParent = (Virtualized) getParent();
				estimatedScrollY = virtualParent.getEstimatedScrollY();
			}
			// Update the text
			clear();
			appendText(text);
			// Set to prior caret position
			if (caret >= 0 && caret < text.length()) {
				moveTo(caret);
			}
			// Set to prior scroll position
			if (estimatedScrollY >= 0 && getParent() instanceof Virtualized) {
				Virtualized virtualParent = (Virtualized) getParent();
				ScrollUtils.forceScroll(virtualParent, estimatedScrollY);
			}
		} else {
			clear();
			appendText(text);
		}
	}

	/**
	 * Update the {@link #lastAST latest AST} and {@link #isLastAstCurrent AST is-up-to-date flag}.
	 */
	private void updateParse() {
		JavaParserHelper helper = RecafUI.getController().getServices().getJavaParserHelper();
		ParseResult<CompilationUnit> result = helper.parseClass(getText());
		if (result.getResult().isPresent()) {
			lastAST = result.getResult().get();
			isLastAstCurrent = true;
		} else {
			// We don't nullify the AST but do note that it is not up-to-date.
			isLastAstCurrent = false;
		}
	}

	/**
	 * Select the position of an AST element and {@link #centerParagraph(int) center it on the screen}.
	 *
	 * @param pos
	 * 		Position to select.
	 */
	public void selectPosition(com.github.javaparser.Position pos) {
		selectPosition(pos.line, pos.column);
	}
}
