package software.coley.recaf.ui.control.richtext.suggest;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.input.KeyEvent;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTCode;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.util.BlwOpcodes;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.PlainTextChange;
import regexodus.Matcher;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.pane.editing.assembler.AssemblerPane;
import software.coley.recaf.util.RegexUtil;
import software.coley.recaf.workspace.model.Workspace;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Tab completion for {@link AssemblerPane}.
 *
 * @author Matt Coley
 */
public class AssemblerTabCompleter implements TabCompleter<String> {
	private final CompletionPopup<String> completionPopup = new StringCompletionPopup(15);
	private final Workspace workspace;
	private CodeArea area;
	private List<ASTElement> ast;
	private Context context;

	/**
	 * @param workspace
	 * 		Workspace to pull class info from.
	 */
	public AssemblerTabCompleter(@Nonnull Workspace workspace) {
		this.workspace = workspace;
	}

	/**
	 * @param ast
	 * 		AST to work off of.
	 */
	public void setAst(@Nonnull List<ASTElement> ast) {
		this.ast = ast;
	}

	/**
	 * Remove AST to work off of.
	 */
	public void clearAst() {
		ast = null;
	}

	@Override
	public boolean requestCompletion(@Nonnull KeyEvent event) {
		// Recompute line context to ensure its up-to-date.
		recomputeLineContext();

		// Skip if no text context or empty text context.
		if (context instanceof EmptyContext)
			return false;

		// Complete if the completion popup is showing.
		return completionPopup.isShowing() && completeFromContext(context.partialMatchedText(), completionPopup::doComplete);
	}

	@Nonnull
	@Override
	public List<String> computeCurrentCompletions() {
		return context.complete();
	}

	@Override
	public void onFineTextUpdate(@Nonnull PlainTextChange change) {
		recomputeLineContext();
	}

	@Override
	public void onRoughTextUpdate(@Nonnull List<PlainTextChange> changes) {
		// no-op
	}

	@Override
	public void install(@Nonnull Editor editor) {
		area = editor.getCodeArea();
		completionPopup.install(area, this);
	}

	@Override
	public void uninstall(@Nonnull Editor editor) {
		completionPopup.uninstall();
		area = null;
	}

	private void recomputeLineContext() {
		// Must have AST to infer data
		if (ast == null || ast.isEmpty()) {
			context = new EmptyContext();
			return;
		}

		// Must be in code block
		boolean inCode = false;
		ASTElement element = ast.getFirst();
		int caret = area.getCaretPosition();
		for (int i = 0; i < 5; i++) {
			if (element instanceof ASTMethod method) {
				ASTCode code = method.code();
				if (code.range().within(caret))
					inCode = true;
				break;
			} else {
				element = element.pick(caret);
			}
		}
		if (!inCode) {
			context = new EmptyContext();
			return;
		}

		// Get the line to regex match, and the trimmed slice of it for tab-completion's "partial text" to complete.
		String line = area.getParagraph(area.getCurrentParagraph()).getText();
		String partialText = line.trim();

		// Skip if there is nothing to complete.
		if (partialText.isEmpty()) {
			context = new EmptyContext();
			return;
		}

		// Match for:
		//  - Opcodes
		//  - Field references
		//  - Method references
		if (RegexUtil.matches("^\\s*(\\w+)\\s*$", line)) {
			context = new CodeOpcodeContext(partialText);
		} else {
			// Field matching
			Matcher matcher = RegexUtil.getMatcher("^\\s*(?:(?:get|put)(?:static|field))\\s+({type}[\\w\\/]+)?(?:\\.({name}\\w+)?)?(?:\\s+({desc}[\\w\\/;]+))?\\s*$", line);
			if (matcher.find()) {
				String type = matcher.group("type");
				String name = matcher.group("name");
				String desc = matcher.group("desc");
				context = new CodeFieldContext(partialText, type, name, desc);
				return;
			}

			// Method matching
			matcher = RegexUtil.getMatcher("^\\s*(?:(?:invoke)(?:virtual|interface|special|static)(?:interface)?)\\s+({type}[\\w\\/]+)?(?:\\.({name}\\w+)?)?(?:\\s+({desc}[\\w\\/;]+))?\\s*$", line);
			if (matcher.find()) {
				String type = matcher.group("type");
				String name = matcher.group("name");
				String desc = matcher.group("desc");
				context = new CodeMethodContext(partialText, type, name, desc);
				return;
			}

			// Type matching for 'new/checkcast/instanceof'
			matcher = RegexUtil.getMatcher("^\\s*(?:new|checkcast|instanceof)\\s+({type}[\\w\\/]+)?\\s*$", line);
			if (matcher.find()) {
				// Can re-use field/method ctx here for type name completion
				String type = matcher.group("type");
				context = new CodeMethodContext(partialText, type, null, null);
				return;
			}

			// Unknown
			context = new EmptyContext();
		}
	}

	private static boolean completeFromContext(@Nullable String context, @Nonnull Predicate<String> completionHandler) {
		if (context == null)
			return false;
		String group = null;
		Matcher matcher = RegexUtil.getMatcher("[\\w\\/]+", context);
		while (matcher.find())
			group = matcher.group();
		return group != null && completionHandler.test(group);
	}

	private class StringCompletionPopup extends CompletionPopup<String> {
		private StringCompletionPopup(int maxItemsToShow) {
			super(STANDARD_CELL_SIZE, maxItemsToShow, t -> t);
		}

		@Override
		public void completeCurrentSelection() {
			completeFromContext(context.partialMatchedText(), this::doComplete);
		}
	}

	/**
	 * Context model.
	 */
	private interface Context {
		@Nonnull
		default List<String> complete() {
			return Collections.emptyList();
		}

		@Nonnull
		default String partialMatchedText() {
			return "";
		}
	}

	/**
	 * Nothing to offer.
	 */
	record EmptyContext() implements Context {}

	/**
	 * The user is in the code block with only a single token.
	 * <br>
	 * We should offer opcodes.
	 *
	 * @param partialInput
	 * 		Partial input text.
	 */
	record CodeOpcodeContext(@Nonnull String partialInput) implements Context {
		private static final SortedSet<String> opcodes = new TreeSet<>(BlwOpcodes.getFilteredOpcodes().keySet());

		@Nonnull
		@Override
		public String partialMatchedText() {
			return partialInput;
		}

		@Nonnull
		@Override
		public List<String> complete() {
			List<String> items = new ArrayList<>();
			String trimmed = partialInput.trim();
			for (String opcode : opcodes)
				if (opcode.startsWith(trimmed) && !opcode.equals(trimmed))
					items.add(opcode);
			return items;
		}
	}

	/**
	 * The user is in the code block typing out a field reference.
	 * <br>
	 * We should offer various parts of the field reference based on what is currently written.
	 */
	class CodeFieldContext extends ReferenceContext {
		/**
		 * @param partialInput
		 * 		Partial input text.
		 * @param owner
		 * 		Reference owner text. May be partial if the type/name are {@code null}.
		 * @param name
		 * 		Reference name text. May be partial if the type is {@code null}.
		 * @param desc
		 * 		Reference type text. May be partial.
		 */
		public CodeFieldContext(@Nonnull String partialInput, @Nullable String owner, @Nullable String name, @Nullable String desc) {
			super(partialInput, owner, name, desc);
		}

		@Nonnull
		@Override
		public List<String> complete() {
			return complete(workspace, ClassInfo::fieldStream);
		}
	}

	/**
	 * The user is in the code block typing out a method reference.
	 * <br>
	 * We should offer various parts of the method reference based on what is currently written.
	 */
	class CodeMethodContext extends ReferenceContext {
		/**
		 * @param partialInput
		 * 		Partial input text.
		 * @param owner
		 * 		Reference owner text. May be partial if the type/name are {@code null}.
		 * @param name
		 * 		Reference name text. May be partial if the type is {@code null}.
		 * @param desc
		 * 		Reference type text. May be partial.
		 */
		CodeMethodContext(@Nonnull String partialInput, @Nullable String owner, @Nullable String name, @Nullable String desc) {
			super(partialInput, owner, name, desc);
		}

		@Nonnull
		@Override
		public List<String> complete() {
			// TODO: Will probably want to support more than just the declared methods
			//  - If a class "Foo" extends "Bar" any "Foo.x" should complete available methods from "Bar"
			return complete(workspace, ClassInfo::methodStream);
		}
	}


	abstract static class ReferenceContext implements Context {
		protected final String partialInput;
		protected final String owner;
		protected final String name;
		protected final String desc;

		/**
		 * @param partialInput
		 * 		Partial input text.
		 * @param owner
		 * 		Reference owner text. May be partial if the type/name are {@code null}.
		 * @param name
		 * 		Reference name text. May be partial if the type is {@code null}.
		 * @param desc
		 * 		Reference type text. May be partial.
		 */
		public ReferenceContext(@Nonnull String partialInput, @Nullable String owner, @Nullable String name, @Nullable String desc) {
			this.partialInput = partialInput;
			this.owner = owner;
			this.name = name;
			this.desc = desc;
		}

		@Override
		@Nonnull
		public final String partialMatchedText() {
			return partialInput;
		}

		@Nonnull
		@Override
		public abstract List<String> complete();

		@Nonnull
		protected List<String> complete(@Nonnull Workspace workspace,
		                                @Nonnull Function<ClassInfo, Stream<? extends ClassMember>> classMemberLookup) {
			// Skip if no owner type specified.
			if (owner == null) return Collections.emptyList();

			// Complete class names
			if (name == null && desc == null) {
				// TODO: startsWith is nice, but mirroring IntelliJ would be nicer
				//  - Should be able to complete "new Strin" to "new java/lang/String"
				//    - But out completion system just appends text so we'd need to do some more work there.

				// TODO: System class names not completed since those are only populated on-demand in the workspace
				return workspace.findClasses(c -> c.getName().startsWith(owner)).stream()
						.map(c -> c.getValue().getName()).toList();
			}

			// TODO: Members should be filtered by access as well (can be done in calling stream lookup)

			// Complete member name/descriptors if we have "owner.x"
			if (desc == null) {
				ClassPathNode ownerPath = workspace.findClass(owner);
				if (ownerPath != null) {
					return classMemberLookup.apply(ownerPath.getValue())
							.filter(member -> member.getName().startsWith(name))
							.map(member -> member.getName() + " " + member.getDescriptor())
							.toList();
				}
			}

			// Complete member descriptors if we have "owner.name"
			if (desc != null) {
				ClassPathNode ownerPath = workspace.findClass(owner);
				if (ownerPath != null) {
					return classMemberLookup.apply(ownerPath.getValue())
							.filter(member -> member.getName().equals(name) && member.getDescriptor().startsWith(desc))
							.map(ClassMember::getDescriptor)
							.toList();
				}
			}

			return Collections.emptyList();
		}
	}
}
