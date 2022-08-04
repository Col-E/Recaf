package me.coley.recaf.ui.pane;

import javafx.beans.property.IntegerProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import me.coley.recaf.Controller;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.code.MemberInfo;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.behavior.ClassRepresentation;
import me.coley.recaf.ui.behavior.FontSizeChangeable;
import me.coley.recaf.ui.behavior.SaveResult;
import me.coley.recaf.ui.control.ErrorDisplay;
import me.coley.recaf.ui.control.SearchBar;
import me.coley.recaf.ui.control.code.*;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.threading.FxThreadUtil;
import me.coley.recaf.workspace.Workspace;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.jf.baksmali.Adaptors.ClassDefinition;
import org.jf.baksmali.BaksmaliOptions;
import org.jf.baksmali.formatter.BaksmaliWriter;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedClassDef;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.writer.builder.DexBuilder;
import org.jf.dexlib2.writer.io.MemoryDataStore;
import org.jf.smali.smaliFlexLexer;
import org.jf.smali.smaliParser;
import org.jf.smali.smaliTreeWalker;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * An editor for Android Dalvik bytecode.
 *
 * @author Matt Coley
 */
public class SmaliAssemblerPane extends BorderPane implements ClassRepresentation, FontSizeChangeable {
	private static final Logger logger = Logging.get(SmaliAssemblerPane.class);
	private final SyntaxArea smaliArea;
	private DexClassInfo dexClass;
	private boolean ignoreNextDecompile;

	/**
	 * Create and set up the panel.
	 */
	public SmaliAssemblerPane() {
		ProblemTracking tracking = new ProblemTracking();
		tracking.setIndicatorInitializer(new ProblemIndicatorInitializer(tracking));
		smaliArea = new SyntaxArea(Languages.DALVIK_BYTECODE, tracking);
		// Wrap content, create error display
		Node node = new VirtualizedScrollPane<>(smaliArea);
		Node errorDisplay = new ErrorDisplay(smaliArea, tracking);
		// Layout
		StackPane stack = new StackPane();
		StackPane.setAlignment(errorDisplay, Configs.editor().errorIndicatorPos);
		StackPane.setMargin(errorDisplay, new Insets(16, 25, 25, 53));
		stack.getChildren().add(node);
		stack.getChildren().add(errorDisplay);
		setCenter(stack);
		// Search support
		SearchBar.install(this, smaliArea);
	}

	@Override
	public void applyEventsForFontSizeChange(Consumer<Node> consumer) {
		smaliArea.applyEventsForFontSizeChange(consumer);
	}

	@Override
	public void bindFontSize(IntegerProperty property) {
		smaliArea.bindFontSize(property);
	}

	@Override
	public CommonClassInfo getCurrentClassInfo() {
		return dexClass;
	}

	@Override
	public boolean supportsMemberSelection() {
		// TODO: Pattern match the bytecode and find its location
		//       Can probably recycle the ANTLR stuff from the compilation logic
		return false;
	}

	@Override
	public boolean isMemberSelectionReady() {
		return false;
	}

	@Override
	public void selectMember(MemberInfo memberInfo) {
		// no-op
	}

	@Override
	public SaveResult save() {
		// Reset errors
		smaliArea.getProblemTracking().clearOfType(ProblemOrigin.BYTECODE_COMPILE);
		// Input flags
		Opcodes opcodes = dexClass.getOpcodes();
		int apiLevel = opcodes.api;
		// Smali is ANTLR(3) so we got some bloated parse logic here
		String smaliText = smaliArea.getText();
		Reader reader = new StringReader(smaliText);
		smaliFlexLexer lexer = new smaliFlexLexer(reader, apiLevel);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		// Parser has some config
		List<RecognitionException> exceptions = new ArrayList<>();
		smaliParser parser = new smaliParser(tokens) {
			@Override
			public void reportError(RecognitionException e) {
				super.reportError(e);
				exceptions.add(e);
			}
		};
		parser.setVerboseErrors(true);
		parser.setAllowOdex(false);
		parser.setApiLevel(apiLevel);
		try {
			// Parse smali
			smaliParser.smali_file_return result = parser.smali_file();
			if (parser.getNumberOfSyntaxErrors() > 0 || lexer.getNumberOfSyntaxErrors() > 0) {
				for (RecognitionException ex : exceptions) {
					String message = "Token recognition error near: " + ex.token.getText();
					smaliArea.getProblemTracking().addProblem(ex.line,
							new ProblemInfo(ProblemOrigin.BYTECODE_COMPILE, ProblemLevel.ERROR, ex.line, message));
				}
				return SaveResult.FAILURE;
			}
			// Now we gotta put it into a dex file.
			CommonTree tree = result.getTree();
			CommonTreeNodeStream treeStream = new CommonTreeNodeStream(tree);
			treeStream.setTokenStream(tokens);
			DexBuilder dexBuilder = new DexBuilder(opcodes);
			smaliTreeWalker dexGen = new smaliTreeWalker(treeStream);
			// Configure input
			dexGen.setApiLevel(apiLevel);
			dexGen.setVerboseErrors(true);
			dexGen.setDexBuilder(dexBuilder);
			// Parse
			dexGen.smali_file();
			// Write output
			MemoryDataStore dataStore = new MemoryDataStore();
			dexBuilder.writeTo(dataStore);
			DexBackedDexFile dexFile = new DexBackedDexFile(opcodes, dataStore.getBuffer());
			Set<? extends DexBackedClassDef> classes = dexFile.getClasses();
			// Validate we can get the result, and update the workspace
			if (!classes.isEmpty()) {
				DexBackedClassDef value = classes.iterator().next();
				ignoreNextDecompile = true;
				String dexPath = dexClass.getDexPath();
				Controller controller = RecafUI.getController();
				Workspace workspace = controller.getWorkspace();
				workspace.getResources().getPrimary().getDexClasses().put(
						dexPath,
						dexClass.getName(),
						DexClassInfo.parse(dexPath, opcodes, value));
				return SaveResult.SUCCESS;
			}
			logger.error("Smali was parsed, but no output was found!");
			return SaveResult.FAILURE;
		} catch (RecognitionException ex) {
			// Unlike the handling above, recognition exceptions don't ever seem to really be thrown.
			// So bogus input used the above handling. If something happens here, that is odd, and we want to log it.
			smaliArea.getProblemTracking().addProblem(ex.line,
					new ProblemInfo(ProblemOrigin.BYTECODE_COMPILE, ProblemLevel.ERROR, ex.line, ex.getMessage()));
			logger.error("Error on parsing smali", ex);
			return SaveResult.FAILURE;
		} catch (IOException ex) {
			// 'DexBuilder.writeTo' failed
			logger.error("Error on compiling smali", ex);
			return SaveResult.FAILURE;
		}
	}

	@Override
	public boolean supportsEditing() {
		return true;
	}

	@Override
	public Node getNodeRepresentation() {
		return this;
	}

	@Override
	public void onUpdate(CommonClassInfo newValue) {
		if (newValue instanceof DexClassInfo) {
			dexClass = (DexClassInfo) newValue;
			// Skip if we triggered this via save
			if (ignoreNextDecompile) {
				ignoreNextDecompile = false;
				return;
			}
			// Use dexlib to disassemble.
			BaksmaliOptions options = new BaksmaliOptions();
			ClassDef def = dexClass.getClassDef();
			StringWriter stringWriter = new StringWriter();
			try (BaksmaliWriter writer = new BaksmaliWriter(
					stringWriter,
					options.implicitReferences ? def.getType() : null)) {
				ClassDefinition classDefinition = new ClassDefinition(options, def);
				classDefinition.writeTo(writer);
			} catch (Exception ex) {
				logger.error("Failed to disassemble smali for class '{}'", newValue.getName(), ex);
			}
			String text = stringWriter.toString();
			FxThreadUtil.run(() -> smaliArea.setText(text));
		}
	}
}
