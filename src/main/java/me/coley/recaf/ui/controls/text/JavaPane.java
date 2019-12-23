package me.coley.recaf.ui.controls.text;

import com.github.javaparser.Range;
import javafx.application.Platform;
import me.coley.recaf.compiler.JavacCompiler;
import me.coley.recaf.compiler.TargetVersion;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.parse.source.SourceCode;
import me.coley.recaf.ui.controls.text.model.Languages;
import me.coley.recaf.util.*;
import me.coley.recaf.workspace.*;

import javax.tools.ToolProvider;
import java.util.*;

/**
 * Java-focused text editor.
 *
 * @author Matt
 */
public class JavaPane extends TextPane {
	public static final int HOVER_ERR_TIME = 50;
	public static final int HOVER_DOC_TIME = 700;
	private final JavaErrorHandling errHandler = new JavaErrorHandling(this);
	private final JavaResource resource;
	private SourceCode code;
	private JavaDocHandling docHandler;
	private JavaContextHandling contextHandler;

	/**
	 * @param controller
	 * 		Controller to act on.
	 * @param resource
	 * 		Resource containing the code.
	 */
	public JavaPane(GuiController controller, JavaResource resource) {
		super(controller, Languages.find("java"));
		this.resource = resource;
		setOnCodeChange(text -> errHandler.onCodeChange(text, () -> {
			code = new SourceCode(resource, getText());
			code.analyze(controller.getWorkspace());
			docHandler = new JavaDocHandling(this, controller, code);
			contextHandler = new JavaContextHandling(this, controller, code);
		}));
	}

	@Override
	protected boolean hasError(int line) {
		return errHandler.hasError(line);
	}

	@Override
	protected String getLineComment(int line) {
		return errHandler.getLineComment(line);
	}

	@Override
	public void setText(String text) {
		if (!canCompile())
			text = LangUtil.translate("ui.bean.class.recompile.unsupported") + text;
		super.setText(text);
	}

	/**
	 * Compiles the current source code.
	 *
	 * @param name
	 * 		Class name to compile.
	 *
	 * @return Recompiled bytecode of classes <i>(Should there be any inner classes)</i>.
	 */
	public Map<String, byte[]> save(String name) {
		if (!canCompile())
			throw new UnsupportedOperationException("Recompilation not supported in read-only mode");
		int version = ClassUtil.getVersion(resource.getClasses().get(name));
		JavacCompiler javac = new JavacCompiler();
		javac.setClassPath(getClassPath());
		javac.addUnit(name, getText());
		javac.options().lineNumbers = true;
		javac.options().variables = true;
		javac.options().sourceName = true;
		javac.options().setTarget(TargetVersion.fromClassMajor(version));
		javac.setCompileListener(errHandler);
		if (javac.compile())
			return javac.getUnits();
		else
			throw new IllegalStateException("Failed compile");
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
		// Delay until analysis has run
		while(code == null || (code.getUnit() == null && !errHandler.hasErrors()))
			try {
				Thread.sleep(50);
			} catch(InterruptedException ex) { /* ignored */ }
		// Select member if unit analysis was a success
		if (code != null && code.getUnit() != null) {
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
	private List<String> getClassPath() {
		List<String> path = new ArrayList<>();
		add(path, controller.getWorkspace().getPrimary());
		for (JavaResource resource : controller.getWorkspace().getLibraries())
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
			path.add(fsr.getFile().getAbsolutePath());
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
}
