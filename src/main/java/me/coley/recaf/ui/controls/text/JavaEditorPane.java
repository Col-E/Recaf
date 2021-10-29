package me.coley.recaf.ui.controls.text;

import com.github.javaparser.ParseResult;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import javafx.application.Platform;
import me.coley.recaf.compiler.JavacCompiler;
import me.coley.recaf.compiler.JavacTargetVersion;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.parse.source.SourceCode;
import me.coley.recaf.parse.source.SourceCodeException;
import me.coley.recaf.ui.controls.ClassEditor;
import me.coley.recaf.ui.controls.text.model.Languages;
import me.coley.recaf.util.*;
import me.coley.recaf.workspace.*;

import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Java-focused text editor.
 *
 * @author Matt
 */
public class JavaEditorPane extends EditorPane<JavaErrorHandling, JavaContextHandling> implements ClassEditor {
	public static final int HOVER_ERR_TIME = 50;
	public static final int HOVER_DOC_TIME = 700;
	private final JavaResource resource;
	private SourceCode code;
	private JavaDocHandling docHandler;

	/**
	 * @param controller
	 * 		Controller to act on.
	 * @param resource
	 * 		Resource containing the code.
	 */
	public JavaEditorPane(GuiController controller, JavaResource resource) {
		this(controller, resource, null);
	}

	/**
	 * @param controller
	 * 		Controller to act on.
	 * @param resource
	 * 		Resource containing the code.
	 * @param initialText
	 * 		Initial text.
	 */
	public JavaEditorPane(GuiController controller, JavaResource resource, String initialText) {
		super(controller, Languages.find("java"), JavaContextHandling::new);
		this.resource = resource;
		if (initialText != null)
			setText(initialText);
		setErrorHandler(new JavaErrorHandling(this));
		setOnCodeChange(text -> getErrorHandler().onCodeChange(this::parseCode));
		setOnKeyReleased(e -> {
			if(controller.config().keys().gotoDef.match(e))
				contextHandler.gotoSelectedDef();
			else if(controller.config().keys().rename.match(e))
				contextHandler.openRenameInput();
		});
	}

	private void parseCode() throws SourceCodeException {
		code = new SourceCode(resource, getText());
		try {
			code.analyze(controller.getWorkspace());
			docHandler = new JavaDocHandling(this, controller, code);
			contextHandler.setCode(code);
		} catch (SourceCodeException e) {
			if (JavaParserUtil.isCompilationUnitParseable(code.getUnit())) {  // Also accept partial result
				docHandler = new JavaDocHandling(this, controller, code);
				contextHandler.setCode(code);
			} else if (contextHandler.getCode() == null) {  // If the code has never been parsed successfully before now
				ParseResult<CompilationUnit> parseResult = code.analyzeFiltered(controller.getWorkspace(),
						e.getResult().getProblems());
				if (parseResult.isSuccessful() || parseResult.getResult()
						.filter(JavaParserUtil::isCompilationUnitParseable).isPresent()) {
					docHandler = new JavaDocHandling(this, controller, code);
					contextHandler.setCode(code);

					// We've got the basics parsed and working. Time to reset the unit back to
					// the original source (with `contextHandler.getCode()` not longer null).
					// This will throw the same SourceCodeException
					parseCode();
				}
			}
			throw e;
		}
	}

	@Override
	public void setText(String text) {
		if (!canCompile())
			text = LangUtil.translate("ui.bean.class.recompile.unsupported") + text;
		if (!getText().equals(text))
			super.setText(text);
	}

	@Override
	public Map<String, byte[]> save(String name) {
		if (!canCompile())
			throw new UnsupportedOperationException("Recompilation not supported in read-only mode");
		List<String> path;
		try {
			path = getClassPath();
		} catch(IOException e) {
			throw new IllegalStateException("Failed writing temp resources before compiling");
		}
		int version = ClassUtil.getVersion(resource.getClasses().get(name));
		JavacCompiler javac = new JavacCompiler();
		javac.setClassPath(path);
		javac.addUnit(name, getText());
		javac.options().lineNumbers = true;
		javac.options().variables = true;
		javac.options().sourceName = true;
		JavacTargetVersion classVersion = JavacTargetVersion.fromClassMajor(version);
		JavacTargetVersion minSupportedVersion = JavacTargetVersion.getMinJavacSupport();
		JavacTargetVersion maxSupportedVersion = JavacTargetVersion.getMaxJavacSupport();
		if (minSupportedVersion.ordinal() > classVersion.ordinal())
			classVersion = minSupportedVersion;
		if (maxSupportedVersion.ordinal() < classVersion.ordinal())
			classVersion = maxSupportedVersion;
		javac.options().setTarget(classVersion);
		javac.setCompileListener(getErrorHandler());
		if (javac.compile())
			return javac.getUnits();
		else
			throw new IllegalStateException("Failed compile due to compilation errors");
	}

	@Override
	public void selectMember(String name, String desc) {
		// Delay until analysis has run
		if (code == null) {
			// Reschedule the check
			try {
				Thread.sleep(50);
			} catch (Exception e) { /* ignored */ }
			ThreadUtil.run(() -> selectMember(name, desc));
			return;
		}

		// Select member if unit analysis was a success
		if (code.getUnit() != null) {
			// Jump to range if found
			Optional<Range> range = JavaParserUtil.getMemberRange(code.getUnit(), name, desc);
			if(range.isPresent()) {
				int line = range.get().begin.line - 1;
				int column = range.get().begin.column - 1;
				Platform.runLater(() -> {
					codeArea.moveTo(line, column);
					codeArea.requestFollowCaret();
					codeArea.requestFocus();
				});
			}
		}
	}

	/**
	 * @return Classpath from workspace.
	 */
	private List<String> getClassPath() throws IOException {
		List<String> path = new ArrayList<>();
		// Reference the most up-to-date primary definitions
		File temp = controller.getWorkspace().getTemporaryPrimaryDefinitionJar();
		if(temp.exists())
			path.add(temp.getAbsolutePath());
		else
			add(path, controller.getWorkspace().getPrimary());
		// Add backing resources
		for(JavaResource resource : controller.getWorkspace().getLibraries())
			add(path, resource);
		return path;
	}

	/**
	 * Add the resource to the classpath.
	 *
	 * @param path
	 * 		Classpath to build on.
	 * @param resource
	 * 		Resource to add.
	 */
	private void add(List<String> path, JavaResource resource) {
		if (resource instanceof FileSystemResource) {
			FileSystemResource fsr = (FileSystemResource) resource;
			path.add(IOUtil.toString(fsr.getPath()));
		} else if (resource instanceof DeferringResource) {
			JavaResource deferred = ((DeferringResource) resource).getBacking();
			add(path, deferred);
		}
	}

	/**
	 * @return {@code true} if compilation is supported.
	 */
	public boolean canCompile() {
		return ToolProvider.getSystemJavaCompiler() != null;
	}

	/**
	 * @return Parsed &amp; analyzed code.
	 */
	public SourceCode getAnalyzedCode() {
		return code;
	}
}
