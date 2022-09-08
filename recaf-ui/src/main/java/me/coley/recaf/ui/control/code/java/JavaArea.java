package me.coley.recaf.ui.control.code.java;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.nodeTypes.NodeWithRange;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import me.coley.recaf.Controller;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.*;
import me.coley.recaf.compile.CompileOption;
import me.coley.recaf.compile.Compiler;
import me.coley.recaf.compile.CompilerManager;
import me.coley.recaf.compile.CompilerResult;
import me.coley.recaf.compile.javac.JavacCompiler;
import me.coley.recaf.config.Configs;
import me.coley.recaf.config.container.CompilerConfig;
import me.coley.recaf.config.container.KeybindConfig;
import me.coley.recaf.parse.JavaParserHelper;
import me.coley.recaf.parse.JavaParserPrinting;
import me.coley.recaf.parse.ParseHitResult;
import me.coley.recaf.parse.WorkspaceTypeSolver;
import me.coley.recaf.ui.CommonUX;
import me.coley.recaf.ui.behavior.ClassRepresentation;
import me.coley.recaf.ui.behavior.SaveResult;
import me.coley.recaf.ui.context.ContextBuilder;
import me.coley.recaf.ui.context.DeclarableContextBuilder;
import me.coley.recaf.ui.control.NavigationBar;
import me.coley.recaf.ui.control.code.*;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.JavaVersion;
import me.coley.recaf.util.NodeEvents;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.threading.FxThreadUtil;
import me.coley.recaf.util.threading.ThreadUtil;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resources;
import org.fxmisc.richtext.CharacterHit;
import org.fxmisc.richtext.model.TwoDimensional;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.github.javaparser.ast.Node.Parsedness.UNPARSABLE;
import static me.coley.recaf.parse.JavaParserResolving.resolvedValueToInfo;

/**
 * Syntax area implementation with a focus on Java specific behavior.
 *
 * @author Matt Coley
 */
public class JavaArea extends SyntaxArea implements ClassRepresentation {
	private static final Logger logger = Logging.get(JavaArea.class);
	private CommonClassInfo lastInfo;
	private CompletableFuture<CompilationUnit> parseFuture;
	private Consumer<CompilationUnit> queuedParseAction;
	private ContextMenu menu;

	/**
	 * @param problemTracking
	 * 		Optional problem tracking implementation to enable line problem indicators.
	 */
	public JavaArea(ProblemTracking problemTracking) {
		super(Languages.JAVA, problemTracking);
		setOnContextMenuRequested(this::onMenuRequested);
		caretPositionProperty().addListener((ob, old, cur) -> NavigationBar.getInstance().tryUpdateNavbar(this));
		setOnMousePressed(e -> {
			if (e.isMiddleButtonDown() || (e.isPrimaryButtonDown() && e.isControlDown()))
				handleNavigation(e);
		});
		NodeEvents.addKeyPressHandler(this, e -> {
			int pos = getCaretPosition();
			KeybindConfig config = Configs.keybinds();
			if (config.gotoDef.match(e))
				handleNavigation(pos);
			else if (config.rename.match(e)) {
				ContextBuilder contextBuilder = menuBuilderFor(pos);
				if (contextBuilder instanceof DeclarableContextBuilder)
					((DeclarableContextBuilder) contextBuilder).rename();
			} else if (config.searchReferences.match(e)) {
				ContextBuilder contextBuilder = menuBuilderFor(pos);
				if (contextBuilder instanceof DeclarableContextBuilder)
					((DeclarableContextBuilder) contextBuilder).search();
			}
		});
		textProperty().addListener((observable, oldValue, newValue) -> {
			// Queue up new parse task, killing prior task if present
			if (parseFuture != null) {
				parseFuture.cancel(true);
			}
			CompletableFuture<CompilationUnit> unitFuture = ThreadUtil.run(this::updateParse);
			if (queuedParseAction != null) {
				Consumer<CompilationUnit> action = queuedParseAction;
				unitFuture = unitFuture.thenApplyAsync(ast -> {
					action.accept(ast);
					return ast;
				}, FxThreadUtil.executor());
				queuedParseAction = null;
			}
			parseFuture = unitFuture;
		});
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
		return parseFuture != null && parseFuture.isDone();
	}

	@Override
	public void selectMember(MemberInfo memberInfo) {
		if (memberInfo == null)
			return;
		if (isMemberSelectionReady())
			internalSelect(memberInfo);
		else
			internalQueueSelect(memberInfo);
	}

	private void internalQueueSelect(MemberInfo memberInfo) {
		queuedParseAction = ast -> doSelectMember(ast, memberInfo);
	}

	private void internalSelect(MemberInfo memberInfo) {
		parseFuture.orTimeout(Configs.decompiler().decompileTimeout, TimeUnit.MILLISECONDS)
				.thenAcceptAsync(ast -> {
					// Parse thread done, and AST result should be present
					doSelectMember(ast, memberInfo);
				}, FxThreadUtil.executor());
	}

	private void doSelectMember(CompilationUnit ast, MemberInfo memberInfo) {
		WorkspaceTypeSolver solver = RecafUI.getController().getServices().getSymbolSolver().getTypeSolver();
		if (memberInfo.isField()) {
			ast.findFirst(FieldDeclaration.class, dec -> {
				MemberInfo declaredInfo = (MemberInfo) resolvedValueToInfo(solver, dec.resolve());
				return memberInfo.equals(declaredInfo);
			}).flatMap(NodeWithRange::getBegin).ifPresent(this::selectPosition);
			// Check for enum constants, which JavaParser treats differently
			if (memberInfo.getDescriptor().length() > 2) {
				ast.findFirst(EnumConstantDeclaration.class, dec -> {
					MemberInfo declaredInfo = (MemberInfo) resolvedValueToInfo(solver, dec.resolve());
					return memberInfo.equals(declaredInfo);
				}).flatMap(NodeWithRange::getBegin).ifPresent(this::selectPosition);
			}
		} else if (memberInfo.getName().equals("<init>")) {
			ast.findFirst(ConstructorDeclaration.class, dec -> {
				MemberInfo declaredInfo = (MemberInfo) resolvedValueToInfo(solver, dec.resolve());
				return memberInfo.equals(declaredInfo);
			}).flatMap(NodeWithRange::getBegin).ifPresent(this::selectPosition);
		} else if (memberInfo.getName().equals("<clinit>")) {
			ast.findFirst(InitializerDeclaration.class, InitializerDeclaration::isStatic)
					.flatMap(NodeWithRange::getBegin).ifPresent(this::selectPosition);
		} else {
			ast.findFirst(MethodDeclaration.class, dec -> {
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
	}

	@Override
	public SaveResult save() {
		Controller controller = RecafUI.getController();
		Workspace workspace = controller.getWorkspace();
		// For now there is only one implementation, plain ol "javac"
		CompilerConfig config = Configs.compiler();
		String compilerName = config.impl;
		CompilerManager compilerManager = controller.getServices().getCompilerManager();
		Compiler compiler = compilerManager.get(compilerName);
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
			for (Compiler impl : compilerManager.getRegisteredImpls()) {
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
			getProblemTracking().clearOfType(ProblemOrigin.JAVA_COMPILE);
			// Gather info
			String classSource = getText();
			String className = getClassName();
			int version = getCompileTargetVersion();
			// Configure the compiler
			Map<String, CompileOption<?>> options = new HashMap<>(compiler.getDefaultOptions());
			compiler.setDebug(options, JavacCompiler.createDebugValue(
					config.debugVars, config.debugLines, config.debugSourceName));
			compiler.setTarget(options, version);
			// Add classpath
			Resources resources = workspace.getResources();
			compiler.clearVirtualClassPath();
			compiler.addVirtualClassPath(resources.getPrimary());
			compiler.addVirtualClassPath(resources.getInternalLibraries());
			compiler.addVirtualClassPath(resources.getLibraries());
			// Invoke and handle result
			CompilerResult result = compiler.compile(className, classSource, options);
			if (result.wasSuccess()) {
				result.getValue().forEach((name, value) -> {
					logger.info("Updating '{}'", name);
					resources.getPrimary().getClasses().put(ClassInfo.read(value));
				});
				return SaveResult.SUCCESS;
			} else {
				result.getErrors().forEach(diag -> {
					int line = diag.getLine();
					ProblemInfo info = new ProblemInfo(ProblemOrigin.JAVA_COMPILE, ProblemLevel.ERROR,
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
		String className = JavaParserPrinting.getType(parseFuture.getNow(null).getType(0).resolve());
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
		if (!parseFuture.isDone()) {
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
		// Check if we resolved the content at the index, and were able to make a menu.
		ContextBuilder menuBuilder = menuBuilderFor(hit.getInsertionIndex());
		if (menuBuilder != null) {
			// Show if present
			menu = menuBuilder.build();
			menu.setAutoHide(true);
			menu.setHideOnEscape(true);
			menu.show(getScene().getWindow(), e.getScreenX(), e.getScreenY());
			menu.requestFocus();
		} else {
			logger.warn("No recognized class or member at selected position [line {}, column {}]", line, column);
		}
	}

	private ContextBuilder menuBuilderFor(int position) {
		Optional<ParseHitResult> infoAtPosition = resolveAtPosition(position);
		if (infoAtPosition.isPresent()) {
			ItemInfo info = infoAtPosition.get().getInfo();
			boolean dec = infoAtPosition.get().isDeclaration();
			if (info instanceof ClassInfo) {
				ClassInfo classInfo = (ClassInfo) info;
				return ContextBuilder.forClass(classInfo).setDeclaration(dec);
			} else if (info instanceof DexClassInfo) {
				DexClassInfo dexClassInfo = (DexClassInfo) info;
				return ContextBuilder.forDexClass(dexClassInfo);
			} else if (info instanceof FieldInfo) {
				FieldInfo fieldInfo = (FieldInfo) info;
				CommonClassInfo owner = RecafUI.getController().getWorkspace()
						.getResources().getClass(fieldInfo.getOwner());
				if (owner != null)
					return ContextBuilder.forField(owner, fieldInfo).setDeclaration(dec);
			} else if (info instanceof MethodInfo) {
				MethodInfo methodInfo = (MethodInfo) info;
				CommonClassInfo owner = RecafUI.getController().getWorkspace()
						.getResources().getClass(methodInfo.getOwner());
				if (owner != null)
					return ContextBuilder.forMethod(owner, methodInfo).setDeclaration(dec);
			} else if (info instanceof LiteralExpressionInfo) {
				return ContextBuilder.forLiteralExpression((LiteralExpressionInfo) info, this);
			}
		}
		return null;
	}

	private void handleNavigation(MouseEvent e) {
		// Convert the event position to line/column
		CharacterHit hit = hit(e.getX(), e.getY());
		handleNavigation(hit.getInsertionIndex());
	}

	private void handleNavigation(int index) {
		// Check if there is info about the selected item
		Optional<ParseHitResult> infoAtPosition = resolveAtPosition(index);
		if (infoAtPosition.isPresent()) {
			ItemInfo info = infoAtPosition.get().getInfo();
			if (info instanceof ClassInfo) {
				ClassInfo classInfo = (ClassInfo) info;
				CommonUX.openClass(classInfo);
			} else if (info instanceof DexClassInfo) {
				DexClassInfo dexClassInfo = (DexClassInfo) info;
				CommonUX.openClass(dexClassInfo);
			} else if (info instanceof FieldInfo) {
				FieldInfo fieldInfo = (FieldInfo) info;
				CommonClassInfo owner = RecafUI.getController().getWorkspace()
						.getResources().getClass(fieldInfo.getOwner());
				if (owner != null)
					CommonUX.openMember(owner, fieldInfo);
			} else if (info instanceof MethodInfo) {
				MethodInfo methodInfo = (MethodInfo) info;
				CommonClassInfo owner = RecafUI.getController().getWorkspace()
						.getResources().getClass(methodInfo.getOwner());
				if (owner != null)
					CommonUX.openMember(owner, methodInfo);
			}
		}
	}

	/**
	 * @param position
	 * 		Absolute position in the document.
	 *
	 * @return Parse result containing information about what is at the given position.
	 */
	public Optional<ParseHitResult> declarationAtPosition(int position) {
		// Get position line/column
		position = Math.min(Math.max(0, position), getLength() - 1);
		Position hitPos = offsetToPosition(position, TwoDimensional.Bias.Backward);
		int line = hitPos.getMajor() + 1; // Position is 0 indexed
		int column = hitPos.getMinor();
		// Parse what is at the location
		JavaParserHelper helper = RecafUI.getController().getServices().getJavaParserHelper();
		return helper.declarationAt(parseFuture.getNow(null), line, column);
	}

	/**
	 * @param position
	 * 		Absolute position in the document.
	 *
	 * @return Parse result containing information about what is at the given position.
	 */
	public Optional<ParseHitResult> resolveAtPosition(int position) {
		// Get position line/column
		position = Math.min(Math.max(0, position), getLength() - 1);
		Position hitPos = offsetToPosition(position, TwoDimensional.Bias.Backward);
		int line = hitPos.getMajor() + 1; // Position is 0 indexed
		int column = hitPos.getMinor();
		// Parse what is at the location
		JavaParserHelper helper = RecafUI.getController().getServices().getJavaParserHelper();
		return helper.at(parseFuture.getNow(null), line, column);
	}

	/**
	 * Update the latest AST.
	 */
	private CompilationUnit updateParse() {
		JavaParserHelper helper = RecafUI.getController().getServices().getJavaParserHelper();
		ParseResult<CompilationUnit> result = helper.parseClass(getText());
		CompilationUnit unit = result.getResult().orElse(null);
		// Display warning to users that code was unparsable so that they stop asking us why right-click doesn't work.
		if (getProblemTracking() != null) {
			if (unit == null || unit.getParsed() == UNPARSABLE) {
				getProblemTracking().addProblem(-2, new ProblemInfo(
						ProblemOrigin.JAVA_SYNTAX, ProblemLevel.WARNING, -2,
						Lang.getBinding("java.unparsable").get()));
			} else {
				getProblemTracking().removeProblem(-2);
			}
		}
		return unit;
	}

	/**
	 * Select the position of an AST element and center it on the screen.
	 *
	 * @param pos
	 * 		Position to select.
	 */
	public void selectPosition(com.github.javaparser.Position pos) {
		selectPosition(pos.line, pos.column);
	}
}
