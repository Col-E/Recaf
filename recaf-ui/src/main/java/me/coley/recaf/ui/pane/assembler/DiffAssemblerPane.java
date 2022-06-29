package me.coley.recaf.ui.pane.assembler;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.assemble.ast.PrintContext;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.transformer.BytecodeToAstTransformer;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.ui.behavior.MemberEditor;
import me.coley.recaf.ui.behavior.SaveResult;
import me.coley.recaf.ui.control.ClassBorderPane;
import me.coley.recaf.ui.control.code.Language;
import me.coley.recaf.ui.control.code.Languages;
import me.coley.recaf.ui.control.code.ProblemTracking;
import me.coley.recaf.ui.control.code.SyntaxArea;
import me.coley.recaf.ui.control.code.bytecode.AssemblerArea;
import me.coley.recaf.ui.pane.DiffViewPane;
import org.fxmisc.flowless.Virtualized;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.util.concurrent.CompletableFuture;

public class DiffAssemblerPane extends AssemblerPane implements DiffViewPane.Diffable {

	private final CompletableFuture<Void> future;
	private String code;

	public DiffAssemblerPane(CompletableFuture<Void> future) {
		this.future = future;
		this.code = "";
		getAssemblerArea().setEditable(false);

		// disable analysis on the area
		getAssemblerArea().disableAnalysis();
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
		if(newValue instanceof ClassInfo) {
			ClassInfo classInfo = (ClassInfo) newValue;

			// find class node

			ClassReader reader = classInfo.getClassReader();
			ClassNode node = new ClassNode();
			reader.accept(node, ClassReader.SKIP_FRAMES);

			BytecodeToAstTransformer transformer = new BytecodeToAstTransformer(node);
			transformer.visit();
			Unit unit = transformer.getUnit();
			String result = unit.print(PrintContext.DEFAULT_CTX);

			// update code area
			getAssemblerArea().setText(result);
			code = result;
			future.complete(null);
		}
	}
}
