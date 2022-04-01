package me.coley.recaf.ui.pane;

import com.google.common.collect.Iterables;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.Controller;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.code.MemberInfo;
import me.coley.recaf.ui.behavior.ClassRepresentation;
import me.coley.recaf.ui.behavior.SaveResult;
import me.coley.recaf.ui.control.code.Languages;
import me.coley.recaf.ui.control.code.ProblemTracking;
import me.coley.recaf.ui.control.code.SyntaxArea;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.threading.FxThreadUtil;
import me.coley.recaf.workspace.Workspace;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenSource;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.jf.baksmali.Adaptors.ClassDefinition;
import org.jf.baksmali.BaksmaliOptions;
import org.jf.baksmali.formatter.BaksmaliWriter;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedClassDef;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.writer.builder.DexBuilder;
import org.jf.dexlib2.writer.io.MemoryDataStore;
import org.jf.smali.LexerErrorInterface;
import org.jf.smali.smaliFlexLexer;
import org.jf.smali.smaliParser;
import org.jf.smali.smaliTreeWalker;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * An editor for Android Dalvik bytecode.
 *
 * @author Matt Coley
 */
public class SmaliAssemblerPane extends BorderPane implements ClassRepresentation {
	private static final Logger logger = Logging.get(SmaliAssemblerPane.class);
	private final ProblemTracking tracking;
	private final SyntaxArea syntaxArea;
	private DexClassInfo dexClass;
	private boolean ignoreNextDecompile;

	/**
	 * Create and set up the panel.
	 */
	public SmaliAssemblerPane() {
		tracking = new ProblemTracking();
		syntaxArea = new SyntaxArea(Languages.JAVA, tracking);
		setCenter(syntaxArea);
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
		Opcodes opcodes = dexClass.getOpcodes();
		int apiLevel = opcodes.api;
		// Smali is ANTLR(3) so we got some bloated parse logic here
		String smaliText = syntaxArea.getText();
		Reader reader = new StringReader(smaliText);
		LexerErrorInterface lexer = new smaliFlexLexer(reader, apiLevel);
		CommonTokenStream tokens = new CommonTokenStream((TokenSource) lexer);
		// Parser has some config
		smaliParser parser = new smaliParser(tokens);
		parser.setVerboseErrors(true);
		parser.setAllowOdex(false);
		parser.setApiLevel(apiLevel);
		try {
			// Parse smali
			smaliParser.smali_file_return result = parser.smali_file();
			if (parser.getNumberOfSyntaxErrors() > 0 || lexer.getNumberOfSyntaxErrors() > 0) {
				throw new RuntimeException("Error occurred while compiling text");
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
			DexBackedClassDef value = Iterables.getFirst(dexFile.getClasses(), null);
			// Validate we can get the result, and update the workspace
			if (value != null) {
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
			logger.error("Error on parsing smali", ex);
			return SaveResult.FAILURE;
		} catch (IOException ex) {
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
			FxThreadUtil.run(() -> syntaxArea.setText(text));
		}
	}
}
