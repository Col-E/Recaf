package software.coley.recaf.ui.control.richtext.suggest.java;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.objectweb.asm.Opcodes;
import software.coley.recaf.info.member.BasicFieldMember;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.script.AugmentedSource;
import software.coley.recaf.services.script.ScriptSourceAugmentation;
import software.coley.recaf.services.source.AstService;
import software.coley.recaf.services.source.ResolverAdapter;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.AccessibleFields;
import software.coley.recaf.test.dummy.AccessibleMethods;
import software.coley.recaf.test.dummy.ClassWithFieldsAndMethods;
import software.coley.recaf.ui.control.richtext.suggest.TabCompletionConfig;
import software.coley.recaf.ui.control.richtext.suggest.java.typeindex.JavaTypeIndexService;
import software.coley.recaf.workspace.model.EmptyWorkspace;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.sourcesolver.model.CompilationUnitModel;
import software.coley.sourcesolver.model.VariableModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JavaTabCompleter}.
 */
class JavaTabCompleterTest extends TestBase {
	private JavaTypeIndexService typeIndexService;

	@BeforeEach
	void setUp() {
		workspaceManager.setCurrent(null);
		typeIndexService = new JavaTypeIndexService(workspaceManager);
	}

	@Test
	void typeCompletionsUseImplicitScriptImportsWithoutWorkspace() {
		JavaCompletionContext context = scriptContext("""
				public class Demo {
					void run() {}
				}
				""", null);

		assertContainsType(completions(context, new JavaLexicalContext(ContextKind.TYPE, "Work", "", -1, false)),
				"Workspace");
		assertContainsType(completions(context, new JavaLexicalContext(ContextKind.TYPE, "WorkspaceM", "", -1, false)),
				"WorkspaceManager");
		assertContainsType(completions(context, new JavaLexicalContext(ContextKind.TYPE, "Lis", "", -1, false)),
				"List");
		assertContainsType(completions(context, new JavaLexicalContext(ContextKind.TYPE, "Str", "", -1, false)),
				"String");
		assertContainsType(completions(context, new JavaLexicalContext(ContextKind.TYPE, "Sys", "", -1, false)),
				"System");
	}

	@Test
	void annotationTypeCompletionsUseImplicitScriptImportsWithoutWorkspace() {
		JavaCompletionContext context = scriptContext("""
				public class Demo {
					void run() {}
				}
				""", null);

		assertContainsType(completions(context, new JavaLexicalContext(ContextKind.TYPE, "Inj", "", -1, true)),
				"Inject");
		assertContainsType(completions(context, new JavaLexicalContext(ContextKind.TYPE, "Dep", "", -1, true)),
				"Dependent");
	}

	@Test
	void memberCompletionsUseDeclaredScriptFieldMetadataWithoutWorkspace() {
		String source = """
				public class Demo {
					WorkspaceManager wm;
				
					void run() {
						wm.
					}
				}
				""";
		AugmentedSource augmented = ScriptSourceAugmentation.augmentClassScript(source);
		String internalName = augmented.packageInternalName() + "/Demo";
		FieldMember field = new BasicFieldMember("wm",
				"Lsoftware/coley/recaf/services/workspace/WorkspaceManager;", null, 0, null);
		JavaCompletionContext.DeclaredClassInfo declaredClassInfo = new JavaCompletionContext.DeclaredClassInfo(
				internalName,
				Opcodes.ACC_PUBLIC,
				List.of(field),
				List.of(),
				List.of()
		);
		JavaCompletionContext context = scriptContext(source, declaredClassInfo);

		int caret = source.indexOf("wm.") + "wm.".length();
		List<JavaCompletion> completions = completions(context, new JavaLexicalContextParser().parse(source, caret));
		assertContainsMethod(completions, "getCurrent()");
	}

	@Test
	void memberCompletionsUseResolvedScriptFieldMetadataFromImplicitWildcardImports() {
		String source = """
				public class Demo {
					WorkspaceManager wm;
					List<String> list;
				
					void run() {
						wm.
						list.
					}
				}
				""";
		JavaCompletionContext.DeclaredClassInfo declaredClassInfo = resolvedScriptDeclaredClassInfo(source);
		JavaCompletionContext context = scriptContext(source, declaredClassInfo);

		int wmCaret = source.indexOf("wm.") + "wm.".length();
		List<JavaCompletion> wmCompletions = completions(context, new JavaLexicalContextParser().parse(source, wmCaret));
		assertContainsMethod(wmCompletions, "getCurrent()");

		int listCaret = source.indexOf("list.") + "list.".length();
		List<JavaCompletion> listCompletions = completions(context, new JavaLexicalContextParser().parse(source, listCaret));
		assertContainsMethod(listCompletions, "size()");
	}

	@Test
	void snippetScriptsDoNotExposeJavaCompletion() {
		JavaCompletionContext context = new TestCompletionContext(
				EmptyWorkspace.get(),
				null,
				null,
				null,
				null,
				false
		);

		List<JavaCompletion> completions = completions(context,
				new JavaLexicalContext(ContextKind.TYPE, "Str", "", -1, false));
		assertTrue(completions.isEmpty());
	}

	@Test
	void workspaceBackedIdentifierCompletionsStillExposeCurrentClassMembers() throws IOException {
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(AccessibleFields.class));
		ClassPathNode path = workspace.findClass("software/coley/recaf/test/dummy/AccessibleFields");
		assertNotNull(path);

		JavaCompletionContext context = new TestCompletionContext(
				workspace,
				null,
				null,
				path,
				null,
				true
		);

		List<JavaCompletion> completions = completions(context,
				new JavaLexicalContext(ContextKind.IDENTIFIER, "CONSTANT_F", "", -1, false));
		JavaCompletion completion = findCompletion(completions, CompletionKind.FIELD, "CONSTANT_FIELD");
		assertNotNull(completion);
		assertNotNull(completion.path());
	}

	@Test
	void importCompletionsUseWorkspaceTypeIndex() throws IOException {
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(AccessibleFields.class));
		JavaCompletionContext context = new TestCompletionContext(workspace, null, null, null, null, true);

		List<JavaCompletion> packageCompletions = completions(context,
				new JavaLexicalContext(ContextKind.IMPORT, "software.coley.recaf.test.du", "", -1, false));
		assertContainsPackage(packageCompletions, "software.coley.recaf.test.dummy");

		List<JavaCompletion> typeCompletions = completions(context,
				new JavaLexicalContext(ContextKind.IMPORT, "software.coley.recaf.test.dummy.AccessibleF", "", -1, false));
		assertContainsType(typeCompletions, "software.coley.recaf.test.dummy.AccessibleFields");
	}

	@Test
	void packageCompletionsUseWorkspaceTypeIndex() throws IOException {
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(AccessibleFields.class));
		JavaCompletionContext context = new TestCompletionContext(workspace, null, null, null, null, true);

		List<JavaCompletion> completions = completions(context,
				new JavaLexicalContext(ContextKind.PACKAGE, "software.coley.recaf.test.du", "", -1, false));
		assertContainsPackage(completions, "software.coley.recaf.test.dummy");
	}

	@Test
	void identifierProviderRanksLocalsBeforeKeywords() {
		String source = """
				public class Demo {
					void run() {
						String format = "";
						fo
					}
				}
				""";
		JavaCompletionContext context = scriptContext(source, null);
		int caret = source.lastIndexOf("fo") + "fo".length();
		JavaCompletionEngine engine = new JavaCompletionEngine(new TabCompletionConfig());
		List<JavaCompletion> completions = engine.compute(
				new JavaCompletionSession(context, typeIndexService, caret),
				new JavaLexicalContext(ContextKind.IDENTIFIER, "fo", "", -1, false));

		assertEquals(CompletionKind.LOCAL, completions.getFirst().kind());
		assertEquals("format", completions.getFirst().insertionText());
	}

	@Test
	void methodBodyKeywordRulesExcludeTopLevelOnlyKeywords() {
		JavaCompletionSession session = new JavaCompletionSession(new TestCompletionContext(
				EmptyWorkspace.get(),
				null,
				null,
				null,
				null,
				true
		), typeIndexService, -1);
		Set<String> keywords = JavaKeywordCompletionRules.getIdentifierKeywords(session,
				new JavaLexicalContext(ContextKind.IDENTIFIER, "", "", -1, false, KeywordSite.METHOD_BODY));

		assertFalse(keywords.contains("package"), "Method bodies should not offer 'package'");
		assertFalse(keywords.contains("import"), "Method bodies should not offer 'import'");
		assertFalse(keywords.contains("class"), "Method bodies should not offer 'class'");
		assertFalse(keywords.contains("public"), "Method bodies should not offer 'public'");
		assertTrue(keywords.contains("if"), "Method bodies should still offer statement keywords");
	}

	@Test
	void typeCompletionPromotesCommonStringTypes() {
		JavaCompletionContext context = scriptContext("""
				public class Demo {
					void run() {}
				}
				""", null);

		List<JavaCompletion> completions = completions(context,
				new JavaLexicalContext(ContextKind.TYPE, "St", "", -1, false));

		assertOrderedBefore(completions, CompletionKind.TYPE, "String", "StringBuilder");
		assertOrderedBefore(completions, CompletionKind.TYPE, "StringBuilder", "StringBuffer");
	}

	@Test
	void memberCompletionPromotesPrintlnBeforeOtherPrintStreamMembers() {
		String source = """
				public class Demo {
					void run() {
						java.io.PrintStream ps = System.out;
						ps.pr
					}
				}
				""";
		JavaCompletionContext context = scriptContext(source, null);
		int caret = source.lastIndexOf("pr") + "pr".length();
		List<JavaCompletion> completions = completions(context, new JavaLexicalContextParser().parse(source, caret));
		List<JavaCompletion> methodCompletions = new ArrayList<>(completions.stream()
				.filter(completion -> completion.kind() == CompletionKind.METHOD)
				.toList());

		assertFalse(methodCompletions.isEmpty(), "Expected PrintStream member completions");
		assertTrue(methodCompletions.getFirst().displayText().startsWith("println("),
				"Expected println overloads to be promoted first, got: " +
						methodCompletions.stream().map(JavaCompletion::displayText).toList());
	}

	@Test
	void memberCompletionsHideNonPublicMembersOfUnrelatedInstanceFields() throws IOException {
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(AccessibleFields.class));
		String source = """
				import software.coley.recaf.test.dummy.AccessibleFields;
				
				public class Demo {
					void run() {
						AccessibleFields af = null;
						af.p
					}
				}
				""";
		JavaCompletionContext context = scriptContext(workspace, source, null);
		int receiverOffset = source.indexOf("af.p") + 1;
		List<JavaCompletion> completions = completions(context,
				new JavaLexicalContext(ContextKind.MEMBER, "p", "af", receiverOffset, false));

		assertContainsField(completions, "publicField");
		assertMissingField(completions, "privateFinalField");
		assertMissingField(completions, "protectedField");
		assertMissingField(completions, "packageField");
	}

	@Test
	void memberCompletionsHideNonPublicMembersOfUnrelatedInstanceMethods() throws IOException {
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(AccessibleMethods.class));
		String source = """
				import software.coley.recaf.test.dummy.AccessibleMethods;
				
				public class Demo {
					void run() {
						AccessibleMethods methods = null;
						methods.p
					}
				}
				""";
		JavaCompletionContext context = scriptContext(workspace, source, null);
		int receiverOffset = source.indexOf("methods.p") + "methods".length() - 1;
		List<JavaCompletion> completions = completions(context,
				new JavaLexicalContext(ContextKind.MEMBER, "p", "methods", receiverOffset, false));

		assertContainsMethod(completions, "publicMethod()");
		assertMissingMethod(completions, "privateMethod()");
		assertMissingMethod(completions, "protectedMethod()");
		assertMissingMethod(completions, "packageMethod()");
	}

	@Test
	void memberCompletionsRetainCurrentClassPrivateMembers() throws IOException {
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(AccessibleMethods.class));
		ClassPathNode path = workspace.findClass("software/coley/recaf/test/dummy/AccessibleMethods");
		assertNotNull(path);

		AstService astService = recaf.get(AstService.class);
		CompilationUnitModel unit;
		synchronized (astService.getSharedJavaParser()) {
			unit = astService.getSharedJavaParser().parse("class Dummy {}");
		}
		ResolverAdapter resolver = astService.newJavaResolver(workspace, unit);
		JavaCompletionContext context = new TestCompletionContext(workspace, unit, resolver, path, null, true);
		List<JavaCompletion> completions = completions(context,
				new JavaLexicalContext(ContextKind.MEMBER, "pri", "this", -1, false));

		assertContainsMethod(completions, "privateMethod()");
	}

	@Test
	void voidMethodCompletionsAppendStatementTerminator() throws IOException {
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(AccessibleMethods.class, ClassWithFieldsAndMethods.class));
		ClassPathNode accessibleMethodsPath = workspace.findClass("software/coley/recaf/test/dummy/AccessibleMethods");
		ClassPathNode fieldsAndMethodsPath = workspace.findClass("software/coley/recaf/test/dummy/ClassWithFieldsAndMethods");
		assertNotNull(accessibleMethodsPath);
		assertNotNull(fieldsAndMethodsPath);

		JavaCompletionContext voidContext = new TestCompletionContext(workspace, null, null, accessibleMethodsPath, null, true);
		JavaCompletion voidMethod = findCompletion(completions(voidContext,
				new JavaLexicalContext(ContextKind.IDENTIFIER, "publicM", "", -1, false)),
				CompletionKind.METHOD, "publicMethod()");
		assertNotNull(voidMethod);
		assertEquals(";", voidMethod.trailingSuffix());
		assertEquals(1, voidMethod.caretBacktrack());
		assertEquals("publicMethod();", voidMethod.fullInsertionText());

		JavaCompletionContext nonVoidContext = new TestCompletionContext(workspace, null, null, fieldsAndMethodsPath, null, true);
		JavaCompletion nonVoidMethod = findCompletion(completions(nonVoidContext,
				new JavaLexicalContext(ContextKind.IDENTIFIER, "plusT", "", -1, false)),
				CompletionKind.METHOD, "plusTwo()");
		assertNotNull(nonVoidMethod);
		assertEquals("", nonVoidMethod.trailingSuffix());
		assertEquals(0, nonVoidMethod.caretBacktrack());
		assertEquals("plusTwo()", nonVoidMethod.fullInsertionText());

		JavaCompletion voidMethodWithParameters = JavaCompletionFactory.methodCompletion("methodWithParameters",
				"(Ljava/lang/String;JFDLjava/util/List;)V", 0, null, true);
		assertNotNull(voidMethodWithParameters);
		assertEquals(";", voidMethodWithParameters.trailingSuffix());
		assertEquals(2, voidMethodWithParameters.caretBacktrack());
		assertEquals("methodWithParameters();", voidMethodWithParameters.fullInsertionText());

		JavaCompletion nonVoidMethodWithParameters = JavaCompletionFactory.methodCompletion("substring",
				"(II)Ljava/lang/String;", 0, null, true);
		assertEquals("", nonVoidMethodWithParameters.trailingSuffix());
		assertEquals(1, nonVoidMethodWithParameters.caretBacktrack());
		assertEquals("substring()", nonVoidMethodWithParameters.fullInsertionText());
	}

	@Test
	void identifierCompletionsFilterUnsupportedKeywords() {
		String source = """
				public class Demo {
					void run() {
						placeholder
					}
				}
				""";
		JavaCompletionContext context = scriptContext(source, null);
		int caret = source.indexOf("placeholder") + "placeholder".length();

		assertMissingKeyword(identifierCompletions(context, source, caret, "man"), "mandated");
		assertMissingKeyword(identifierCompletions(context, source, caret, "vara"), "varargs");
		assertMissingKeyword(identifierCompletions(context, source, caret, "con"), "const");
		assertMissingKeyword(identifierCompletions(context, source, caret, "got"), "goto");
		assertMissingKeyword(identifierCompletions(context, source, caret, "pac"), "package");
		assertMissingKeyword(identifierCompletions(context, source, caret, "imp"), "import");
		assertMissingKeyword(identifierCompletions(context, source, caret, "mod"), "module");
		assertMissingKeyword(identifierCompletions(context, source, caret, "op"), "open");

		assertContainsKeyword(identifierCompletions(context, source, caret, "els"), "else");
		assertContainsKeyword(identifierCompletions(context, source, caret, "ret"), "return");
	}

	@Test
	void identifierCompletionsFilterKeywordsByContext() {
		String source = """
				public class Demo {
					void run() {
						placeholder
					}
				}
				""";
		JavaCompletionContext context = scriptContext(source, null);
		int caret = source.indexOf("placeholder") + "placeholder".length();

		assertMissingKeyword(identifierCompletions(context, source, caret, "cla"), "class");
		assertMissingKeyword(identifierCompletions(context, source, caret, "enu"), "enum");
		assertMissingKeyword(identifierCompletions(context, source, caret, "pub"), "public");
		assertMissingKeyword(identifierCompletions(context, source, caret, "sta"), "static");
		assertMissingKeyword(identifierCompletions(context, source, caret, "ext"), "extends");

		assertContainsKeyword(identifierCompletions(context, source, caret, "ret"), "return");
		assertContainsKeyword(identifierCompletions(context, source, caret, "if"), "if");
		assertContainsKeyword(identifierCompletions(context, source, caret, "thi"), "this");
		assertContainsKeyword(identifierCompletions(context, source, caret, "ne"), "new");

		String typeBodySource = """
				public class Demo {
					placeholder
				}
				""";
		JavaCompletionContext typeBodyContext = scriptContext(typeBodySource, null);
		int typeBodyCaret = typeBodySource.indexOf("placeholder") + "placeholder".length();

		assertContainsKeyword(identifierCompletions(typeBodyContext, typeBodySource, typeBodyCaret, "cla"), "class");
		assertContainsKeyword(identifierCompletions(typeBodyContext, typeBodySource, typeBodyCaret, "enu"), "enum");
		assertContainsKeyword(identifierCompletions(typeBodyContext, typeBodySource, typeBodyCaret, "pri"), "private");
		assertContainsKeyword(identifierCompletions(typeBodyContext, typeBodySource, typeBodyCaret, "sta"), "static");

		assertMissingKeyword(identifierCompletions(typeBodyContext, typeBodySource, typeBodyCaret, "ret"), "return");
		assertMissingKeyword(identifierCompletions(typeBodyContext, typeBodySource, typeBodyCaret, "els"), "else");

		String methodHeaderSource = """
				public class Demo {
					void run() thro
				}
				""";
		JavaCompletionContext methodHeaderContext = scriptContext(methodHeaderSource, null);
		int methodHeaderCaret = methodHeaderSource.indexOf("thro") + "thro".length();

		assertContainsKeyword(identifierCompletions(methodHeaderContext, methodHeaderSource, methodHeaderCaret, "thro"), "throws");
		assertMissingKeyword(identifierCompletions(methodHeaderContext, methodHeaderSource, methodHeaderCaret, "ret"), "return");

		String topLevelBeforeImportsSource = """
				i
				import java.util.List;
				
				public class Demo {
				}
				""";
		JavaCompletionContext topLevelBeforeImportsContext = scriptContext(topLevelBeforeImportsSource, null);
		int topLevelBeforeImportsCaret = topLevelBeforeImportsSource.indexOf("i") + 1;

		assertContainsKeyword(identifierCompletions(topLevelBeforeImportsContext, topLevelBeforeImportsSource, topLevelBeforeImportsCaret, "imp"), "import");
		assertContainsKeyword(identifierCompletions(topLevelBeforeImportsContext, topLevelBeforeImportsSource, topLevelBeforeImportsCaret, "pac"), "package");
		assertMissingKeyword(identifierCompletions(topLevelBeforeImportsContext, topLevelBeforeImportsSource, topLevelBeforeImportsCaret, "if"), "if");
		assertMissingKeyword(identifierCompletions(topLevelBeforeImportsContext, topLevelBeforeImportsSource, topLevelBeforeImportsCaret, "int"), "int");
		assertMissingKeyword(identifierCompletions(topLevelBeforeImportsContext, topLevelBeforeImportsSource, topLevelBeforeImportsCaret, "impl"), "implements");

		String topLevelSource = "pub";
		JavaCompletionContext topLevelContext = new TestCompletionContext(
				EmptyWorkspace.get(),
				null,
				null,
				null,
				null,
				true
		);
		assertContainsKeyword(identifierCompletions(topLevelContext, topLevelSource.length(), "cla"), "class");
		assertContainsKeyword(identifierCompletions(topLevelContext, topLevelSource.length(), "int"), "interface");
		assertContainsKeyword(identifierCompletions(topLevelContext, topLevelSource.length(), "enu"), "enum");
		assertContainsKeyword(identifierCompletions(topLevelContext, topLevelSource.length(), "rec"), "record");

		String classHeaderSource = """
				class Demo imp
				""";
		int classHeaderCaret = classHeaderSource.indexOf("imp") + "imp".length();
		List<JavaCompletion> classHeaderCompletions = completions(topLevelContext,
				new JavaLexicalContextParser().parse(classHeaderSource, classHeaderCaret));
		assertContainsKeyword(classHeaderCompletions, "implements");

		assertContainsKeyword(completions(topLevelContext,
				new JavaLexicalContext(ContextKind.IDENTIFIER, "cla", "", -1, false)), "class");
		assertContainsKeyword(completions(topLevelContext,
				new JavaLexicalContext(ContextKind.IDENTIFIER, "ret", "", -1, false)), "return");
	}

	@Nonnull
	private JavaCompletionContext scriptContext(@Nonnull String source,
	                                            @Nullable JavaCompletionContext.DeclaredClassInfo declaredClassInfo) {
		return scriptContext(EmptyWorkspace.get(), source, declaredClassInfo);
	}

	@Nonnull
	private JavaCompletionContext scriptContext(@Nonnull Workspace workspace,
	                                            @Nonnull String source,
	                                            @Nullable JavaCompletionContext.DeclaredClassInfo declaredClassInfo) {
		AstService astService = recaf.get(AstService.class);
		AugmentedSource augmented = ScriptSourceAugmentation.augmentClassScript(source);
		CompilationUnitModel unit;
		synchronized (astService.getSharedJavaParser()) {
			unit = astService.getSharedJavaParser().parse(augmented.augmentedSource());
		}
		ResolverAdapter resolver = astService.newJavaResolver(workspace, unit);
		return new TestCompletionContext(workspace, unit, resolver, null, declaredClassInfo, true) {
			@Override
			public int mapCurrentPositionToAst(int pos) {
				return augmented.mapOriginalToAugmented(pos);
			}
		};
	}

	@Nonnull
	private JavaCompletionContext.DeclaredClassInfo resolvedScriptDeclaredClassInfo(@Nonnull String source) {
		AstService astService = recaf.get(AstService.class);
		AugmentedSource augmented = ScriptSourceAugmentation.augmentClassScript(source);
		CompilationUnitModel unit;
		synchronized (astService.getSharedJavaParser()) {
			unit = astService.getSharedJavaParser().parse(augmented.augmentedSource());
		}

		ResolverAdapter resolver = astService.newJavaResolver(EmptyWorkspace.get(), unit);
		String internalName = augmented.packageInternalName() + "/" + unit.getDeclaredClasses().getFirst().getName();
		List<FieldMember> fields = new ArrayList<>();
		for (VariableModel field : unit.getDeclaredClasses().getFirst().getFields()) {
			String descriptor = resolver.descriptorOf(field);
			assertNotNull(descriptor, "Expected script field descriptor to resolve for " + field.getName());
			fields.add(new BasicFieldMember(field.getName(), descriptor, null, 0, null));
		}

		return new JavaCompletionContext.DeclaredClassInfo(
				internalName,
				Opcodes.ACC_PUBLIC,
				List.copyOf(fields),
				List.of(),
				List.of()
		);
	}

	@Nonnull
	private List<JavaCompletion> completions(@Nonnull JavaCompletionContext context,
	                                         @Nonnull JavaLexicalContext lexicalContext) {
		JavaTabCompleter completer = newCompleter(context);
		completer.setContext(lexicalContext);
		return completer.computeCurrentCompletions();
	}

	@Nonnull
	private List<JavaCompletion> identifierCompletions(@Nonnull JavaCompletionContext context, int caret, @Nonnull String partial) {
		JavaCompletionEngine engine = new JavaCompletionEngine(new TabCompletionConfig());
		return engine.compute(new JavaCompletionSession(context, typeIndexService, caret),
				new JavaLexicalContext(ContextKind.IDENTIFIER, partial, "", -1, false));
	}

	@Nonnull
	private List<JavaCompletion> identifierCompletions(@Nonnull JavaCompletionContext context,
	                                                   @Nonnull String source,
	                                                   int caret,
	                                                   @Nonnull String partial) {
		JavaLexicalContext parsedContext = new JavaLexicalContextParser().parse(source, caret);
		JavaCompletionEngine engine = new JavaCompletionEngine(new TabCompletionConfig());
		return engine.compute(new JavaCompletionSession(context, typeIndexService, caret),
				new JavaLexicalContext(parsedContext.kind(), partial, parsedContext.receiverText(),
						parsedContext.receiverResolveOffset(), parsedContext.annotationOnly(), parsedContext.keywordSite()));
	}

	private static void assertContainsKeyword(@Nonnull List<JavaCompletion> completions, @Nonnull String keyword) {
		assertNotNull(findCompletion(completions, CompletionKind.KEYWORD, keyword),
				"Missing keyword completion: " + keyword + " from " +
						completions.stream().map(JavaCompletion::insertionText).toList());
	}

	private static void assertMissingKeyword(@Nonnull List<JavaCompletion> completions, @Nonnull String keyword) {
		JavaCompletion completion = findCompletion(completions, CompletionKind.KEYWORD, keyword);
		assertTrue(completion == null,
				"Unexpected keyword completion: " + keyword + " from " +
						completions.stream().map(JavaCompletion::insertionText).toList());
	}

	@Nonnull
	private JavaTabCompleter newCompleter(@Nonnull JavaCompletionContext context) {
		return new JavaTabCompleter(context, Mockito.mock(CellConfigurationService.class), typeIndexService, new TabCompletionConfig());
	}

	private static void assertContainsType(@Nonnull List<JavaCompletion> completions, @Nonnull String typeName) {
		assertNotNull(findCompletion(completions, CompletionKind.TYPE, typeName),
				"Missing type completion: " + typeName + " from " +
						completions.stream().map(JavaCompletion::insertionText).toList());
	}

	private static void assertContainsMethod(@Nonnull List<JavaCompletion> completions, @Nonnull String insertionText) {
		assertNotNull(findCompletion(completions, CompletionKind.METHOD, insertionText),
				"Missing method completion: " + insertionText + " from " +
						completions.stream().map(JavaCompletion::insertionText).toList());
	}

	private static void assertMissingMethod(@Nonnull List<JavaCompletion> completions, @Nonnull String insertionText) {
		JavaCompletion completion = findCompletion(completions, CompletionKind.METHOD, insertionText);
		assertTrue(completion == null,
				"Unexpected method completion: " + insertionText + " from " +
						completions.stream().map(JavaCompletion::insertionText).toList());
	}

	private static void assertContainsField(@Nonnull List<JavaCompletion> completions, @Nonnull String insertionText) {
		assertNotNull(findCompletion(completions, CompletionKind.FIELD, insertionText),
				"Missing field completion: " + insertionText + " from " +
						completions.stream().map(JavaCompletion::insertionText).toList());
	}

	private static void assertMissingField(@Nonnull List<JavaCompletion> completions, @Nonnull String insertionText) {
		JavaCompletion completion = findCompletion(completions, CompletionKind.FIELD, insertionText);
		assertTrue(completion == null,
				"Unexpected field completion: " + insertionText + " from " +
						completions.stream().map(JavaCompletion::insertionText).toList());
	}

	private static void assertContainsPackage(@Nonnull List<JavaCompletion> completions, @Nonnull String packageName) {
		assertNotNull(findCompletion(completions, CompletionKind.PACKAGE, packageName),
				"Missing package completion: " + packageName);
	}

	private static void assertOrderedBefore(@Nonnull List<JavaCompletion> completions,
	                                        @Nonnull CompletionKind kind,
	                                        @Nonnull String firstInsertionText,
	                                        @Nonnull String secondInsertionText) {
		int firstIndex = indexOfCompletion(completions, kind, firstInsertionText);
		int secondIndex = indexOfCompletion(completions, kind, secondInsertionText);
		assertTrue(firstIndex >= 0, "Missing completion: " + firstInsertionText);
		assertTrue(secondIndex >= 0, "Missing completion: " + secondInsertionText);
		assertTrue(firstIndex < secondIndex,
				"Expected '" + firstInsertionText + "' before '" + secondInsertionText + "' but got " +
						completions.stream()
								.filter(completion -> completion.kind() == kind)
								.map(JavaCompletion::insertionText)
								.toList());
	}

	@Nullable
	private static JavaCompletion findCompletion(@Nonnull List<JavaCompletion> completions,
	                                             @Nonnull CompletionKind kind,
	                                             @Nonnull String insertionText) {
		for (JavaCompletion completion : completions) {
			if (completion.kind() == kind && completion.insertionText().equals(insertionText))
				return completion;
		}
		return null;
	}

	private static int indexOfCompletion(@Nonnull List<JavaCompletion> completions,
	                                     @Nonnull CompletionKind kind,
	                                     @Nonnull String insertionText) {
		for (int i = 0; i < completions.size(); i++) {
			JavaCompletion completion = completions.get(i);
			if (completion.kind() == kind && completion.insertionText().equals(insertionText))
				return i;
		}
		return -1;
	}

	private static class TestCompletionContext implements JavaCompletionContext {
		private final Workspace workspace;
		private final CompilationUnitModel unit;
		private final ResolverAdapter resolver;
		private final ClassPathNode path;
		private final DeclaredClassInfo declaredClassInfo;
		private final boolean completionAvailable;

		private TestCompletionContext(@Nonnull Workspace workspace,
		                              @Nullable CompilationUnitModel unit,
		                              @Nullable ResolverAdapter resolver,
		                              @Nullable ClassPathNode path,
		                              @Nullable DeclaredClassInfo declaredClassInfo,
		                              boolean completionAvailable) {
			this.workspace = workspace;
			this.unit = unit;
			this.resolver = resolver;
			this.path = path;
			this.declaredClassInfo = declaredClassInfo;
			this.completionAvailable = completionAvailable;
		}

		@Nonnull
		@Override
		public Workspace getWorkspace() {
			return workspace;
		}

		@Nullable
		@Override
		public CompilationUnitModel getUnit() {
			return unit;
		}

		@Nullable
		@Override
		public ResolverAdapter getResolver() {
			return resolver;
		}

		@Override
		public int mapCurrentPositionToAst(int pos) {
			return pos;
		}

		@Nullable
		@Override
		public software.coley.sourcesolver.resolve.result.Resolution resolveRawPositionSilently(int pos) {
			if (resolver == null)
				return null;
			return resolver.resolveAt(mapCurrentPositionToAst(pos), null);
		}

		@Nullable
		@Override
		public ClassPathNode getPath() {
			return path;
		}

		@Nullable
		@Override
		public DeclaredClassInfo getDeclaredClassInfo() {
			return declaredClassInfo;
		}

		@Override
		public boolean isCompletionAvailable() {
			return completionAvailable;
		}
	}
}
