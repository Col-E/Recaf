package software.coley.recaf.ui.control.richtext.suggest;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTCode;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.util.BlwOpcodes;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.PlainTextChange;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import regexodus.Matcher;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceVertex;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.pane.editing.assembler.AssemblerPane;
import software.coley.recaf.util.Icons;
import software.coley.recaf.util.RegexUtil;
import software.coley.recaf.util.SVG;
import software.coley.recaf.workspace.model.Workspace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Tab completion for {@link AssemblerPane}.
 *
 * @author Matt Coley
 */
public class AssemblerTabCompleter implements TabCompleter<AssemblerTabCompleter.AssemblerCompletion> {
	private final CompletionPopup<AssemblerCompletion> completionPopup;

	private final Workspace workspace;
	private final InheritanceGraph inheritanceGraph;
	private final CellConfigurationService configurationService;
	private CodeArea area;
	private List<ASTElement> ast;
	private Context context;

	/**
	 * @param workspace
	 * 		Workspace to pull class info from.
	 * @param inheritanceGraph
	 * 		Graph to pull hierarchies from.
	 */
	public AssemblerTabCompleter(@Nonnull Workspace workspace,
	                             @Nonnull InheritanceGraph inheritanceGraph,
	                             @Nonnull CellConfigurationService configurationService,
								 @Nonnull TabCompletionConfig config) {
		this.workspace = workspace;
		this.inheritanceGraph = inheritanceGraph;
		this.configurationService = configurationService;
		this.completionPopup = new AssemblerCompletionPopup(config, 15);
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
	public List<AssemblerCompletion> computeCurrentCompletions() {
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
	public boolean isSpecialCompletableKeyCode(@Nullable KeyCode code) {
		// Support completing:
		// - '/' which is used to separate packages in class names
		// - '[' which is used in array descriptors
		// - ';' which is used to mark the end of object type descriptors
		return code == KeyCode.SLASH || code == KeyCode.OPEN_BRACKET || code == KeyCode.SEMICOLON;
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
			} else if (element != null) {
				element = element.pick(caret);
			}
		}
		if (!inCode) {
			context = new EmptyContext();
			return;
		}

		// Get the line to regex match, and the trimmed slice of it for tab-completion's "partial text" to complete.
		// The line content we track should be up to the caret, but no further since we're completing the text where
		// the caret is at and not the end of the line.
		int caretColumn = area.getCaretColumn();
		String line = area.getParagraph(area.getCurrentParagraph()).getText();
		if (caretColumn <= line.length())
			line = line.substring(0, caretColumn);
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
			matcher = RegexUtil.getMatcher("^\\s*(?:(?:invoke)(?:virtual|interface|special|static)(?:interface)?)\\s+" +
					"({type}[\\w\\/]+)?" +
					"(?:\\.({name}\\w+)?)?" +
					"(?:\\s+({desc}.+))?\\s*$", line);
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
		String trimmedContext = context.trim();
		if (trimmedContext.isBlank())
			return false;
		return completionHandler.test(trimmedContext);
	}

	private class AssemblerCompletionPopup extends CompletionPopup<AssemblerCompletion> {
		private AssemblerCompletionPopup(@Nonnull TabCompletionConfig config, int maxItemsToShow) {
			super(config, STANDARD_CELL_SIZE, maxItemsToShow, AssemblerCompletion::text, completion -> switch (completion) {
				case AssemblerCompletion.Opcode opcode -> {
					String text = opcode.text();
					if (text.startsWith("invoke"))
						yield Icons.getIconView(Icons.METHOD);
					if (text.startsWith("get") || text.startsWith("put"))
						yield Icons.getIconView(Icons.FIELD);
					if (text.endsWith("load"))
						yield SVG.ofIconFile(SVG.REF_READ);
					if (text.endsWith("store") || text.equals("iinc"))
						yield SVG.ofIconFile(SVG.REF_WRITE);
					if (text.contains("const") || text.equals("ldc"))
						yield Icons.getIconView(Icons.PRIMITIVE);
					if (text.startsWith("if") || text.endsWith("switch"))
						yield new FontIconView(CarbonIcons.FLOW, Color.STEELBLUE);
					if (text.endsWith("add"))
						yield new FontIconView(CarbonIcons.ADD, Color.LIMEGREEN);
					if (text.endsWith("sub") || text.endsWith("neg"))
						yield new FontIconView(CarbonIcons.SUBTRACT, Color.RED);
					if (text.startsWith("dup"))
						yield new FontIconView(CarbonIcons.COPY, Color.DARKGRAY);
					if (text.endsWith("return"))
						yield new FontIconView(CarbonIcons.TEXT_NEW_LINE, Color.STEELBLUE);
					if (text.equals("new") || text.equals("checkcast") || text.equals("instanceof"))
						yield Icons.getIconView(Icons.CLASS);
					if (text.indexOf('2') == 1) // x2y conversions
						yield SVG.ofIconFile(SVG.TYPE_CONVERSION);
					if (text.contains("cmp"))
						yield new FontIconView(CarbonIcons.COMPARE, Color.DARKGRAY);
					if (text.contains("array"))
						yield Icons.getIconView(Icons.ARRAY);
					if (text.equals("athrow"))
						yield SVG.ofIconFile(SVG.EXCEPTION_BREAKPOINT);
					yield Icons.getIconView(Icons.INTERNAL);
				}
				case AssemblerCompletion.Type type -> configurationService.graphicOf(type.path());
				case AssemblerCompletion.Member member -> configurationService.graphicOf(member.path());
			});
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
		default List<AssemblerCompletion> complete() {
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
	private record EmptyContext() implements Context {}

	/**
	 * The user is in the code block with only a single token.
	 * <br>
	 * We should offer opcodes.
	 *
	 * @param partialInput
	 * 		Partial input text.
	 */
	private record CodeOpcodeContext(@Nonnull String partialInput) implements Context {
		private static final SortedSet<String> opcodes = new TreeSet<>();

		static {
			opcodes.addAll(BlwOpcodes.getFilteredOpcodes().keySet());
			opcodes.removeIf(op -> (op.contains("store") || op.contains("load")) && op.indexOf('_') > -1);
		}

		@Nonnull
		@Override
		public String partialMatchedText() {
			return partialInput;
		}

		@Nonnull
		@Override
		public List<AssemblerCompletion> complete() {
			List<AssemblerCompletion> items = new ArrayList<>();
			String trimmed = partialInput.trim();
			for (String opcode : opcodes) {
				if (opcode.startsWith(trimmed) && !opcode.equals(trimmed))
					items.add(new AssemblerCompletion.Opcode(opcode));
			}
			return items;
		}
	}

	/**
	 * The user is in the code block typing out a field reference.
	 * <br>
	 * We should offer various parts of the field reference based on what is currently written.
	 */
	private class CodeFieldContext extends ReferenceContext {
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
		public List<AssemblerCompletion> complete() {
			boolean isStatic = partialInput.contains("getstatic ") || partialInput.contains("putstatic ");
			return complete(workspace, inheritanceGraph, c -> c.fieldStream().filter(m -> isStatic == m.hasStaticModifier()));
		}
	}

	/**
	 * The user is in the code block typing out a method reference.
	 * <br>
	 * We should offer various parts of the method reference based on what is currently written.
	 */
	private class CodeMethodContext extends ReferenceContext {
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
		public List<AssemblerCompletion> complete() {
			boolean isStatic = partialInput.contains("invokestatic ") || partialInput.contains("invokestaticinterface ");
			boolean isSpecial = partialInput.contains("invokespecial ");
			return complete(workspace, inheritanceGraph, c -> c.methodStream().filter(m -> {
				// Only invokespecial can be used to call constructors.
				if (m.getName().startsWith("<") && !isSpecial)
					return false;

				// Limit by static matching
				return isStatic == m.hasStaticModifier();
			}));
		}
	}

	/**
	 * Type or member reference context.
	 */
	private abstract static class ReferenceContext implements Context {
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
		public abstract List<AssemblerCompletion> complete();

		@Nonnull
		protected List<AssemblerCompletion> complete(@Nonnull Workspace workspace, @Nonnull InheritanceGraph inheritanceGraph,
		                                             @Nonnull Function<ClassInfo, Stream<? extends ClassMember>> classMemberLookup) {
			// Skip if no owner type specified.
			if (owner == null) return Collections.emptyList();

			// Complete class names if the '.' separator is not seen, otherwise suggest any member in the class.
			if (name == null && desc == null) {
				// TODO: startsWith is nice, but mirroring IntelliJ would be nicer
				//  - Should be able to complete "new Strin" to "new java/lang/String"
				//    - But out completion system just appends text so we'd need to do some more work there.
				//  - Also like IntelliJ we should prioritize common completions over niche ones
				//    - Also not something that happens right here
				if (partialInput.endsWith(".")) {
					ClassPathNode ownerPath = workspace.findClass(owner);
					if (ownerPath != null) {
						List<ClassMember> items = new ArrayList<>();
						InheritanceVertex vertex = inheritanceGraph.getVertex(owner);
						if (vertex != null)
							for (InheritanceVertex parent : vertex.getAllParents())
								classMemberLookup.apply(parent.getValue()).forEach(items::add);
						classMemberLookup.apply(ownerPath.getValue()).forEach(items::add);
						return items.stream()
								.map(ownerPath::child)
								.map(AssemblerCompletion.Member::new)
								.map(AssemblerCompletion.class::cast)
								.toList();
					}
				} else {
					// No separator, user is still typing out a class name.
					return workspace.findClasses(c -> c.getName().startsWith(owner)).stream()
							.map(AssemblerCompletion.Type::new)
							.map(AssemblerCompletion.class::cast)
							.toList();
				}
			}

			// Complete member name/descriptors if we have "owner.x"
			if (name != null && desc == null) {
				ClassPathNode ownerPath = workspace.findClass(owner);
				if (ownerPath != null) {
					List<ClassMember> items = new ArrayList<>();
					InheritanceVertex vertex = inheritanceGraph.getVertex(owner);
					Predicate<ClassMember> filter = member -> member.getName().startsWith(name);
					if (vertex != null)
						for (InheritanceVertex parent : vertex.getAllParents())
							classMemberLookup.apply(parent.getValue())
									.filter(filter)
									.forEach(items::add);
					classMemberLookup.apply(ownerPath.getValue())
							.filter(filter)
							.forEach(items::add);
					return items.stream()
							.map(ownerPath::child)
							.map(AssemblerCompletion.Member::new)
							.map(AssemblerCompletion.class::cast)
							.toList();
				}
			}

			// Complete member descriptors if we have "owner.name"
			if (desc != null) {
				ClassPathNode ownerPath = workspace.findClass(owner);
				if (ownerPath != null) {
					List<ClassMember> items = new ArrayList<>();
					InheritanceVertex vertex = inheritanceGraph.getVertex(owner);
					Predicate<ClassMember> filter = member -> member.getName().equals(name) && member.getDescriptor().startsWith(desc);
					if (vertex != null)
						for (InheritanceVertex parent : vertex.getAllParents())
							classMemberLookup.apply(parent.getValue())
									.filter(filter)
									.forEach(items::add);
					classMemberLookup.apply(ownerPath.getValue())
							.filter(filter)
							.forEach(items::add);
					return items.stream()
							.map(ownerPath::child)
							.map(AssemblerCompletion.Member::new)
							.map(AssemblerCompletion.class::cast)
							.toList();
				}
			}

			return Collections.emptyList();
		}
	}

	/**
	 * Completion value type with subtypes for different kinds of completable assembler content.
	 */
	public sealed interface AssemblerCompletion {
		/**
		 * @return Text to complete.
		 */
		@Nonnull
		String text();

		/**
		 * @param text
		 * 		Opcode name.
		 */
		record Opcode(@Nonnull String text) implements AssemblerCompletion {}

		/**
		 * @param path
		 * 		Path to type to complete.
		 */
		record Type(@Nonnull ClassPathNode path) implements AssemblerCompletion {
			@Nonnull
			@Override
			public String text() {
				return path.getValue().getName();
			}
		}

		/**
		 * @param path
		 * 		Path to member to complete.
		 */
		record Member(@Nonnull ClassMemberPathNode path) implements AssemblerCompletion {
			@Nonnull
			@Override
			public String text() {
				ClassMember member = path.getValue();
				return member.getName() + " " + member.getDescriptor();
			}
		}
	}
}
