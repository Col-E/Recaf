package software.coley.recaf.services.source;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.*;
import org.objectweb.asm.Type;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import software.coley.collections.Unchecked;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.DirectoryPathNode;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.*;
import software.coley.recaf.util.Types;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.BasicJvmClassBundle;

import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AstService}
 */
@SuppressWarnings("DataFlowIssue")
public class AstServiceTest extends TestBase {
	static JvmClassInfo thisClass;
	static AstService service;
	static AstContextHelper helper;
	static JavaParser parser;

	@BeforeAll
	static void setup() throws IOException {
		thisClass = TestClassUtils.fromRuntimeClass(AstServiceTest.class);
		BasicJvmClassBundle bundle = TestClassUtils.fromClasses(
				DummyEnum.class,
				StringSupplier.class,
				ClassWithConstructor.class,
				ClassWithExceptions.class,
				ClassWithStaticInit.class,
				ClassWithMultipleMethods.class,
				OverlapClassAB.class,
				OverlapInterfaceA.class,
				OverlapInterfaceB.class,
				StringList.class,
				StringListUser.class,
				HelloWorld.class,
				Types.class,
				Type.class,
				AnnotationImpl.class
		);
		Workspace workspace = TestClassUtils.fromBundle(bundle);
		helper = new AstContextHelper(workspace);
		workspaceManager.setCurrent(workspace);
		service = recaf.get(AstService.class);
		parser = service.newParser(thisClass);
	}

	@AfterEach
	void cleanup() {
		// Flush in-memory caches.
		parser.reset();

		// For some reason, you need to re-allocate the parser to actually gain the full benefits
		// of the prior cache flush.
		parser = service.newParser(thisClass);
	}

	@Nested
	class Resolving {
		@Test
		void testPackage() {
			String source = """
					package software.coley.recaf.test.dummy;
					enum DummyEnum {}
					""";
			handleUnit(source, (unit, ctx) -> {
				validateRange(unit, source, "package software.coley.recaf.test.dummy;", DirectoryPathNode.class, packagePath -> {
					assertEquals("software/coley/recaf/test/dummy", packagePath.getValue());
				});
			});
		}

		@Test
		void testPackage_NoEndingSemicolon() {
			String source = """
					package software.coley.recaf.test.dummy\t
					enum DummyEnum {}
					""";
			handleUnit(source, (unit, ctx) -> {
				validateRange(unit, source, "package software.coley.recaf.test.dummy", DirectoryPathNode.class, packagePath -> {
					assertEquals("software/coley/recaf/test/dummy", packagePath.getValue());
				});
			});
		}

		@Test
		void testImport() {
			String source = """
					package software.coley.recaf.test.dummy;
					import java.io.File;
					import software.coley.recaf.test.dummy.DummyEnum;
					class HelloWorld {}
					""";
			handleUnit(source, (unit, ctx) -> {
				validateRange(unit, source, "import java.io.File;", ClassPathNode.class, classPath -> {
					assertEquals("java/io/File", classPath.getValue().getName());
				});
				validateRange(unit, source, "import software.coley.recaf.test.dummy.DummyEnum;", ClassPathNode.class, classPath -> {
					assertEquals("software/coley/recaf/test/dummy/DummyEnum", classPath.getValue().getName());
				});
			});
		}

		@Test
		void testImportStaticMember() {
			String source = """
					package software.coley.recaf.test.dummy;
					import static software.coley.recaf.test.dummy.DummyEnum.ONE;
					class HelloWorld {}
					""";
			handleUnit(source, (unit, ctx) -> {
				// The 'DummyEnum' takes precedence
				validateRange(unit, source, "DummyEnum", ClassPathNode.class, classPath -> {
					assertEquals("software/coley/recaf/test/dummy/DummyEnum", classPath.getValue().getName());
				});

				// The static import takes precedence
				validateRange(unit, source, "import static software.coley.recaf.test.dummy.", ClassMemberPathNode.class, memberPath -> {
					assertEquals("software/coley/recaf/test/dummy/DummyEnum",
							memberPath.getValueOfType(ClassInfo.class).getName());
					assertEquals("ONE", memberPath.getValue().getName());
				});
				validateRange(unit, source, "ONE;", ClassMemberPathNode.class, memberPath -> {
					assertEquals("software/coley/recaf/test/dummy/DummyEnum",
							memberPath.getValueOfType(ClassInfo.class).getName());
					assertEquals("ONE", memberPath.getValue().getName());
				});
			});
		}

		@Test
		void testClassDeclaration() {
			String source = """
					package software.coley.recaf.test.dummy;
					class HelloWorld {}
					""";
			handleUnit(source, (unit, ctx) -> {
				validateRange(unit, source, "class HelloWorld", ClassPathNode.class, classPath -> {
					assertEquals("software/coley/recaf/test/dummy/HelloWorld", classPath.getValue().getName());
				});
			});
		}

		@Test
		void testInterfaceDeclaration() {
			String source = """
					package software.coley.recaf.test.dummy;
					interface HelloWorld {}
					""";
			handleUnit(source, (unit, ctx) -> {
				validateRange(unit, source, "interface HelloWorld", ClassPathNode.class, classPath -> {
					assertEquals("software/coley/recaf/test/dummy/HelloWorld", classPath.getValue().getName());
				});
			});
		}

		@Test
		void testResolveInterfaces() {
			String source = """
					package software.coley.recaf.test.dummy;
					public class OverlapClassAB implements OverlapInterfaceA, OverlapInterfaceB {
						public void methodA() {}
						public void methodB() {}
					}
					""";
			handleUnit(source, (unit, ctx) -> {
				validateRange(unit, source, "OverlapInterfaceA", ClassPathNode.class, classPath -> {
					assertEquals("software/coley/recaf/test/dummy/OverlapInterfaceA", classPath.getValue().getName());
				});
				validateRange(unit, source, "OverlapInterfaceB", ClassPathNode.class, classPath -> {
					assertEquals("software/coley/recaf/test/dummy/OverlapInterfaceB", classPath.getValue().getName());
				});
			});
		}

		@Test
		void testEnumDeclaration() {
			String source = """
					package software.coley.recaf.test.dummy;
					enum HelloWorld {}
					""";
			handleUnit(source, (unit, ctx) -> {
				validateRange(unit, source, "enum HelloWorld", ClassPathNode.class, classPath -> {
					assertEquals("software/coley/recaf/test/dummy/HelloWorld", classPath.getValue().getName());
				});
			});
		}

		@Test
		void testRecordDeclaration() {
			String source = """
					package software.coley.recaf.test.dummy;
					record HelloWorld() {}
					""";
			handleUnit(source, (unit, ctx) -> {
				validateRange(unit, source, "record HelloWorld", ClassPathNode.class, classPath -> {
					assertEquals("software/coley/recaf/test/dummy/HelloWorld", classPath.getValue().getName());
				});
			});
		}

		@Test
		void testFieldDeclaration_Normal() {
			String source = """
					package software.coley.recaf.util;
					import org.objectweb.asm.Type;
					public class Types {
					  	public static final Type OBJECT_TYPE = Type.getObjectType("java/lang/Object");
					  	public static final Type STRING_TYPE = new Type();
					  	private static final Type[] PRIMITIVES = new Type[0];
					}
					""";
			handleUnit(source, (unit, ctx) -> {
				validateRange(unit, source, "OBJECT_TYPE", ClassMemberPathNode.class, memberPath -> {
					ClassMember member = memberPath.getValue();
					assertEquals("OBJECT_TYPE", member.getName());
				});
				validateRange(unit, source, "STRING_TYPE", ClassMemberPathNode.class, memberPath -> {
					ClassMember member = memberPath.getValue();
					assertEquals("STRING_TYPE", member.getName());
				});
				validateRange(unit, source, "PRIMITIVES", ClassMemberPathNode.class, memberPath -> {
					ClassMember member = memberPath.getValue();
					assertEquals("PRIMITIVES", member.getName());
				});
				validateRange(unit, source, "getObjectType", ClassMemberPathNode.class, memberPath -> {
					ClassMember member = memberPath.getValue();
					assertEquals("getObjectType", member.getName());
				});
				validateRange(unit, source, "new Type[0]", ClassPathNode.class, classPath -> {
					assertEquals("org/objectweb/asm/Type", classPath.getValue().getName());
				});
			});
		}

		@Test
		void testFieldDeclaration_EnumConstant() {
			String source = """
					package software.coley.recaf.test.dummy;
					public enum DummyEnum {
					 	ONE,
					 	TWO,
					 	THREE
					 }
					""";
			handleUnit(source, (unit, ctx) -> {
				validateRange(unit, source, "ONE", ClassMemberPathNode.class, memberPath -> {
					ClassMember member = memberPath.getValue();
					assertEquals("ONE", member.getName());
				});
				validateRange(unit, source, "TWO", ClassMemberPathNode.class, memberPath -> {
					ClassMember member = memberPath.getValue();
					assertEquals("TWO", member.getName());
				});
				validateRange(unit, source, "THREE", ClassMemberPathNode.class, memberPath -> {
					ClassMember member = memberPath.getValue();
					assertEquals("THREE", member.getName());
				});
			});
		}

		@Test
		@Disabled("Fails since generic type is used as return type, getEnumConstants() = DummyEnum[] not base type Enum[]")
		void testMethodReference_SyntheticEnumMethod() {
			String source = """
					package software.coley.recaf.test.dummy;
					     
					public class DummyEnumPrinter {
						public static void main(String[] args) {
							for (DummyEnum enumConstant : DummyEnum.class.getEnumConstants()) {
								String name = enumConstant.name();
								System.out.println(name);
								
								Supplier<String> supplier = enumConstant::name;
								System.out.println(supplier.get());
							}
						}
					}
					""";
			// TODO: More tests validating generic methods being resolved to NOT the <T> type once this behavior is fixed
			handleUnit(source, (unit, ctx) -> {
				validateRange(unit, source, "name()", ClassMemberPathNode.class, memberPath -> {
					ClassMember member = memberPath.getValue();
					assertEquals("name", member.getName());
					assertEquals("()Ljava/lang/String;", member.getDescriptor());
				});
				validateRange(unit, source, "enumConstant::name", ClassMemberPathNode.class, memberPath -> {
					ClassMember member = memberPath.getValue();
					assertEquals("name", member.getName());
					assertEquals("()Ljava/lang/String;", member.getDescriptor());
				});
				validateRange(unit, source, "getEnumConstants", ClassPathNode.class, classPath -> {
					assertEquals("software/coley/recaf/test/dummy/DummyEnum", classPath.getValue().getName());
				});
			});
		}

		@Test
		void testStaticMethodCall() {
			String source = """
					package software.coley.recaf.test.dummy;
					  										
					class HelloWorld {
						static void main(String[] args) {
							ClassWithExceptions.readInt(args[0]);
						}
					}
					""";
			handleUnit(source, (unit, ctx) -> {
				validateRange(unit, source, "ClassWithExceptions", ClassPathNode.class, classPath -> {
					assertEquals("software/coley/recaf/test/dummy/ClassWithExceptions", classPath.getValue().getName());
				});
				validateRange(unit, source, "readInt", ClassMemberPathNode.class, memberPath -> {
					ClassMember member = memberPath.getValue();
					assertEquals("readInt", member.getName());
					assertEquals("(Ljava/lang/Object;)I", member.getDescriptor());
				});
			});
		}

		@Test
		void testStaticMethodCall_JavaLangSystem() {
			String source = """
					package software.coley.recaf.test.dummy;
					  										
					class HelloWorld {
						static void main(String[] args) {
							System.loadLibrary("nativelibname");
						}
					}
					""";
			handleUnit(source, (unit, ctx) -> {
				validateRange(unit, source, "loadLibrary", ClassMemberPathNode.class, memberPath -> {
					ClassMember member = memberPath.getValue();
					assertEquals("loadLibrary", member.getName());
					assertEquals("(Ljava/lang/String;)V", member.getDescriptor());
				});
			});
		}

		@Test
		void testStaticFieldRef() {
			String source = """
					package software.coley.recaf.test.dummy;
					  										
					class HelloWorld {
						static void main(String[] args) {
							ClassWithStaticInit.i = 0;
						}
					}
					""";
			handleUnit(source, (unit, ctx) -> {
				validateRange(unit, source, "ClassWithStaticInit", ClassPathNode.class, classPath -> {
					assertEquals("software/coley/recaf/test/dummy/ClassWithStaticInit", classPath.getValue().getName());
				});
				validateRange(unit, source, "i ", ClassMemberPathNode.class, memberPath -> {
					ClassMember member = memberPath.getValue();
					assertEquals("software/coley/recaf/test/dummy/ClassWithStaticInit", member.getDeclaringClass().getName());
					assertEquals("i", member.getName());
					assertEquals("I", member.getDescriptor());
				});
			});
		}

		@Test
		void testConstructorDeclaration() {
			String source = """
					package software.coley.recaf.test.dummy;
					public class ClassWithConstructor {
					  	public ClassWithConstructor() {}
					  	public ClassWithConstructor(int i) {}
					  	public ClassWithConstructor(int i, int j) {}
					  	public ClassWithConstructor(DummyEnum dummyEnum, StringSupplier supplier) {}
					}
					""";
			handleUnit(source, (unit, ctx) -> {
				validateRange(unit, source, "ClassWithConstructor()", ClassMemberPathNode.class, memberPath -> {
					ClassMember member = memberPath.getValue();
					assertEquals("<init>", member.getName());
					assertEquals("()V", member.getDescriptor());
				});
				validateRange(unit, source, "ClassWithConstructor(int i)", ClassMemberPathNode.class, memberPath -> {
					ClassMember member = memberPath.getValue();
					assertEquals("<init>", member.getName());
					assertEquals("(I)V", member.getDescriptor());
				});
				validateRange(unit, source, "ClassWithConstructor(int i, int j)", ClassMemberPathNode.class, memberPath -> {
					ClassMember member = memberPath.getValue();
					assertEquals("<init>", member.getName());
					assertEquals("(II)V", member.getDescriptor());
				});
				int start = source.indexOf("ClassWithConstructor(DummyEnum dummyEnum, StringSupplier supplier)");
				int end = start + "ClassWithConstructor".length();
				validateRange(unit, start, end, source, ClassMemberPathNode.class, memberPath -> {
					ClassMember member = memberPath.getValue();
					assertEquals("<init>", member.getName());
					assertEquals("(Lsoftware/coley/recaf/test/dummy/DummyEnum;" +
							"Lsoftware/coley/recaf/test/dummy/StringSupplier;)V", member.getDescriptor());
				});
			});
		}

		@Test
		void testStaticInitializer() {
			String source = """
					package software.coley.recaf.test.dummy;
					public class ClassWithStaticInit {
					 	public static int i;
					 	static { i = 42; }
					 }
					""";
			handleUnit(source, (unit, ctx) -> {
				validateRange(unit, source, "static {", ClassMemberPathNode.class, memberPath -> {
					ClassMember member = memberPath.getValue();
					assertEquals("<clinit>", member.getName());
					assertEquals("()V", member.getDescriptor());
				});
			});
		}

		@Test
		void testStringList() {
			String source = """
					package software.coley.recaf.test.dummy;
					public class StringListUser {
					  	public static void main(String[] args) {
					  		StringList list = StringList.of("foo");
					  		for (String string : list.unique()) {
					  			System.out.println(string);
					  		}
					  	}
					}
					""";
			handleUnit(source, (unit, ctx) -> {
				validateRange(unit, source, "of(", ClassMemberPathNode.class, memberPath -> {
					ClassMember member = memberPath.getValue();
					assertEquals("of", member.getName());
				});
				validateRange(unit, source, "unique()", ClassMemberPathNode.class, memberPath -> {
					ClassMember member = memberPath.getValue();
					assertEquals("unique", member.getName());
				});
				int start = source.indexOf("StringList list");
				int end = start + "StringList".length();
				validateRange(unit, start, end, source, ClassPathNode.class, classPath -> {
					assertEquals("software/coley/recaf/test/dummy/StringList", classPath.getValue().getName());
				});
				start = source.indexOf("StringList.of");
				end = start + "StringList".length();
				validateRange(unit, start, end, source, ClassPathNode.class, classPath -> {
					assertEquals("software/coley/recaf/test/dummy/StringList", classPath.getValue().getName());
				});
			});
		}

		@Test
		void testResolveThis() {
			// OpenRewrite can be funky about how it treats 'this'.
			// So we want to have a test to make sure it gets resolved correctly.
			String source = """
					package software.coley.recaf.test.dummy;
										
					import java.util.ArrayList;
					import java.util.Arrays;
					import java.util.LinkedHashSet;
					import java.util.Set;
										
					public class StringList extends ArrayList<String> {
						public Set<String> unique() {
							Object string = this.toString();
							
							return new LinkedHashSet<>(this);
						}
					}
					""";
			handleUnit(source, (unit, ctx) -> {
				int start = source.indexOf("this");
				int end = start + "this".length();
				validateRange(unit, start, end, source, ClassPathNode.class, classPath -> {
					assertEquals("software/coley/recaf/test/dummy/StringList", classPath.getValue().getName());
				});
				start = source.lastIndexOf("this");
				end = start + "this".length();
				validateRange(unit, start, end, source, ClassPathNode.class, classPath -> {
					assertEquals("software/coley/recaf/test/dummy/StringList", classPath.getValue().getName());
				});
			});
		}
	}

	@Nested
	class Mapping {
		@Test
		void renameClass_ReplacesPackage() {
			String source = """
					package software.coley.recaf.test.dummy;
										
					enum DummyEnum {
						ONE, TWO, THREE
					}
					""";
			handleUnit(source, (unit, ctx) -> {
				IntermediateMappings mappings = new IntermediateMappings();
				mappings.addClass(DummyEnum.class.getName().replace('.', '/'), "com/example/MyEnum");
				AstMappingVisitor visitor = new AstMappingVisitor(mappings);

				String modified = unit.acceptJava(visitor, ctx).print(new Cursor(null, unit));
				assertTrue(modified.startsWith("package com.example;"));
				assertTrue(modified.contains("enum MyEnum {"));
			});
		}

		@Test
		void renameClass_ReplacesCast() {
			String source = """
					package software.coley.recaf.test.dummy;
										
					class HelloWorld {
						static void main(Object arg) {
							HelloWorld casted = (HelloWorld) arg;
						}
					}
					""";
			handleUnit(source, (unit, ctx) -> {
				IntermediateMappings mappings = new IntermediateMappings();
				mappings.addClass(HelloWorld.class.getName().replace('.', '/'), "com/example/Howdy");
				AstMappingVisitor visitor = new AstMappingVisitor(mappings);

				String modified = unit.acceptJava(visitor, ctx).print(new Cursor(null, unit));
				assertTrue(modified.startsWith("package com.example;"));
				assertTrue(modified.contains("class Howdy {"));
				assertTrue(modified.contains("Howdy casted = (Howdy) arg;"));
			});
		}

		@Test
		void renameClass_ReplaceStaticCallContextInSamePackage() {
			String source = """
					package software.coley.recaf.test.dummy;
										
					class HelloWorld {
						static void main(String[] args) {
							ClassWithExceptions.readInt(args[0]);
						}
					}
					""";
			handleUnit(source, (unit, ctx) -> {
				IntermediateMappings mappings = new IntermediateMappings();
				mappings.addClass(ClassWithExceptions.class.getName().replace('.', '/'), "com/example/Call");
				AstMappingVisitor visitor = new AstMappingVisitor(mappings);

				String modified = unit.acceptJava(visitor, ctx).print(new Cursor(null, unit));
				assertTrue(modified.contains("Call.readInt("));
			});
		}

		@Test
		void renameClass_ReplaceImportOfStaticCall() {
			String source = """
					package software.coley.recaf.test.dummy;
										
					import static software.coley.recaf.test.dummy.ClassWithExceptions.readInt;
										
					class HelloWorld {
						static void main(String[] args) {
							readInt(args[0]);
						}
					}
					""";
			handleUnit(source, (unit, ctx) -> {
				IntermediateMappings mappings = new IntermediateMappings();
				mappings.addClass(ClassWithExceptions.class.getName().replace('.', '/'), "com/example/Call");
				AstMappingVisitor visitor = new AstMappingVisitor(mappings);

				String modified = unit.acceptJava(visitor, ctx).print(new Cursor(null, unit));
				assertTrue(modified.contains("import static com.example.Call.readInt"));
			});
		}

		@Test
		void renameClass_ArrayDecAndNew() {
			String source = """
					package software.coley.recaf.test.dummy;
										
					class HelloWorld {
						static void main(String[] args) {
							ClassWithExceptions[] bar = new ClassWithExceptions[0];
						}
					}
					""";
			handleUnit(source, (unit, ctx) -> {
				IntermediateMappings mappings = new IntermediateMappings();
				mappings.addClass(ClassWithExceptions.class.getName().replace('.', '/'), "com/example/Foo");
				AstMappingVisitor visitor = new AstMappingVisitor(mappings);

				String modified = unit.acceptJava(visitor, ctx).print(new Cursor(null, unit));
				assertTrue(modified.contains("Foo[] bar = new Foo[0];"));
			});
		}

		@Test
		void renameClass_FieldStaticQualifier() {
			String source = """
					package software.coley.recaf.test.dummy;
										
					class HelloWorld {
						static void main(String[] args) {
							DummyEnum one = DummyEnum.ONE;
							String two = DummyEnum.valueOf("TWO").name();
						}
					}
					""";
			handleUnit(source, (unit, ctx) -> {
				IntermediateMappings mappings = new IntermediateMappings();
				mappings.addClass(DummyEnum.class.getName().replace('.', '/'), "com/example/Singleton");
				AstMappingVisitor visitor = new AstMappingVisitor(mappings);

				String modified = unit.acceptJava(visitor, ctx).print(new Cursor(null, unit));
				assertTrue(modified.contains("Singleton one = Singleton.ONE;"));
				assertTrue(modified.contains("String two = Singleton.valueOf(\"TWO\").name();"));
			});
		}

		@Test
		void renameClass_FieldAndVariableDeclarations() {
			String source = """
					package software.coley.recaf.test.dummy;
										
					class HelloWorld {
						ClassWithExceptions[] array;
						ClassWithExceptions single;
										
						static void main(String[] args) {
							ClassWithExceptions[] local_array = null;
							ClassWithExceptions local_single = null;
						}
					}
					""";
			handleUnit(source, (unit, ctx) -> {
				IntermediateMappings mappings = new IntermediateMappings();
				mappings.addClass(ClassWithExceptions.class.getName().replace('.', '/'), "com/example/Foo");
				AstMappingVisitor visitor = new AstMappingVisitor(mappings);

				String modified = unit.acceptJava(visitor, ctx).print(new Cursor(null, unit));
				assertTrue(modified.contains("Foo[] array;"));
				assertTrue(modified.contains("Foo single;"));
				assertTrue(modified.contains("Foo[] local_array = null;"));
				assertTrue(modified.contains("Foo local_single = null;"));
			});
		}

		@Test
		void renameClass_MethodReturnAndArgs() {
			String source = """
					package software.coley.recaf.test.dummy;
										
					class HelloWorld {
						static HelloWorld get() { return null; }
						void accept(HelloWorld arg) {}
					}
					""";
			handleUnit(source, (unit, ctx) -> {
				IntermediateMappings mappings = new IntermediateMappings();
				mappings.addClass(HelloWorld.class.getName().replace('.', '/'), "com/example/Howdy");
				AstMappingVisitor visitor = new AstMappingVisitor(mappings);

				String modified = unit.acceptJava(visitor, ctx).print(new Cursor(null, unit));
				assertTrue(modified.contains("static Howdy get()"));
				assertTrue(modified.contains("void accept(Howdy arg)"));
			});
		}

		@Test
		void renameClass_Constructor() {
			String source = """
					package software.coley.recaf.test.dummy;
										
					class HelloWorld {
						private HelloWorld(String s) {}
						HelloWorld() {}
					}
					""";
			handleUnit(source, (unit, ctx) -> {
				IntermediateMappings mappings = new IntermediateMappings();
				mappings.addClass(HelloWorld.class.getName().replace('.', '/'), "com/example/Howdy");
				AstMappingVisitor visitor = new AstMappingVisitor(mappings);

				String modified = unit.acceptJava(visitor, ctx).print(new Cursor(null, unit));
				assertTrue(modified.contains("private Howdy(String s) {}"));
				assertTrue(modified.contains("Howdy() {}"));
			});

			source = """
					package software.coley.recaf.test.dummy;
					public class ClassWithConstructor {
					  	private ClassWithConstructor() {}
					  	ClassWithConstructor(int i) {}
					  	protected ClassWithConstructor(int i, int j) {}
					  	public ClassWithConstructor(DummyEnum dummyEnum, StringSupplier supplier) {}
					}
					""";
			handleUnit(source, (unit, ctx) -> {
				IntermediateMappings mappings = new IntermediateMappings();
				mappings.addClass(ClassWithConstructor.class.getName().replace('.', '/'), "com/example/Howdy");
				AstMappingVisitor visitor = new AstMappingVisitor(mappings);

				String modified = unit.acceptJava(visitor, ctx).print(new Cursor(null, unit));
				assertTrue(modified.contains("Howdy() {}"));
				assertTrue(modified.contains("Howdy(int i) {}"));
				assertTrue(modified.contains("Howdy(int i, int j) {}"));
				assertTrue(modified.contains("Howdy(DummyEnum dummyEnum, StringSupplier supplier) {}"));
			});
		}

		@Test
		void renameClass_MethodReference() {
			String source = """
					package software.coley.recaf.test.dummy;
										
					import java.util.function.Supplier;
										
					class HelloWorld {
						static {
							Supplier<HelloWorld> worldSupplier = HelloWorld::new;
						}
					}
					""";
			handleUnit(source, (unit, ctx) -> {
				IntermediateMappings mappings = new IntermediateMappings();
				mappings.addClass(HelloWorld.class.getName().replace('.', '/'), "com/example/Howdy");
				AstMappingVisitor visitor = new AstMappingVisitor(mappings);

				String modified = unit.acceptJava(visitor, ctx).print(new Cursor(null, unit));
				assertTrue(modified.contains("Supplier<Howdy> worldSupplier = Howdy::new"));
			});
		}

		@Test
		@Disabled("Pending support in mapping visitor")
		void renameClass_QualifiedNameReference() {
			String source = """
					package software.coley.recaf.test.dummy;
										
					class HelloWorld {
						static {
							software.coley.recaf.util.Types value = new software.coley.recaf.util.Types();
						}
					}
					""";
			handleUnit(source, (unit, ctx) -> {
				IntermediateMappings mappings = new IntermediateMappings();
				mappings.addClass(Types.class.getName().replace('.', '/'), "com/example/TypeUtil");
				AstMappingVisitor visitor = new AstMappingVisitor(mappings);

				String modified = unit.acceptJava(visitor, ctx).print(new Cursor(null, unit));
				System.err.println(modified);
				assertTrue(modified.contains("com.example.TypeUtil value = new com.example.TypeUtil();"));
			});
		}

		@Test
		void renameMember_FieldName() {
			String source = """
					package software.coley.recaf.test.dummy;
										
					class HelloWorld {
						static String foo;
						String fizz;
						
						HelloWorld() {
							this.fizz = "fizz";
							fizz = "buzz";
							fizz = foo;
							fizz.toString();
						}
						
						static {
							HelloWorld.foo = "foo";
							foo = "bar";
						}
					}
					""";
			handleUnit(source, (unit, ctx) -> {
				IntermediateMappings mappings = new IntermediateMappings();
				mappings.addField(HelloWorld.class.getName().replace('.', '/'), "Ljava/lang/String;", "foo", "bar");
				mappings.addField(HelloWorld.class.getName().replace('.', '/'), "Ljava/lang/String;", "fizz", "buzz");
				AstMappingVisitor visitor = new AstMappingVisitor(mappings);

				String modified = unit.acceptJava(visitor, ctx).print(new Cursor(null, unit));
				assertTrue(modified.contains("static String bar;"));
				assertTrue(modified.contains("String buzz;"));

				assertTrue(modified.contains("this.buzz = \"fizz\";"));
				assertTrue(modified.contains("buzz = \"buzz\";"));
				assertTrue(modified.contains("buzz = bar;"));
				assertTrue(modified.contains("buzz.toString();"));

				assertTrue(modified.contains("HelloWorld.bar = \"foo\";"));
				assertTrue(modified.contains("bar = \"bar\";"));
			});
		}

		@Test
		void renameMember_MethodName() {
			String source = """
					package software.coley.recaf.test.dummy;
										
					import java.util.function.Supplier;
										
					class HelloWorld {
						static {
							Supplier<String> fooSupplier = HelloWorld::foo;
							
							foo();
						}
						
						static String foo() { return "foo"; }
					}
					""";
			handleUnit(source, (unit, ctx) -> {
				IntermediateMappings mappings = new IntermediateMappings();
				mappings.addMethod(HelloWorld.class.getName().replace('.', '/'), "()Ljava/lang/String;", "foo", "bar");
				AstMappingVisitor visitor = new AstMappingVisitor(mappings);

				String modified = unit.acceptJava(visitor, ctx).print(new Cursor(null, unit));
				assertTrue(modified.contains("Supplier<String> fooSupplier = HelloWorld::bar;"));
				assertTrue(modified.contains("bar();"));
				assertTrue(modified.contains("static String bar() { return \"foo\"; }"));
			});
		}

		@Test
		@Disabled("Resolving the exact member reference of static import not directly supported by OpenRewrite")
		void renameMember_MethodNameStaticallyImported() {
			String source = """
					package software.coley.recaf.test.dummy;
										
					import static software.coley.recaf.test.dummy.ClassWithExceptions.readInt;
										
					class HelloWorld {
						static void main(String[] args) {
							readInt(args[0]);
						}
					}
					""";
			handleUnit(source, (unit, ctx) -> {
				IntermediateMappings mappings = new IntermediateMappings();
				mappings.addMethod(ClassWithExceptions.class.getName().replace('.', '/'), "(Ljava/lang/Object;)I", "readInt", "foo");
				AstMappingVisitor visitor = new AstMappingVisitor(mappings);

				String modified = unit.acceptJava(visitor, ctx).print(new Cursor(null, unit));
				assertTrue(modified.contains("import static software.coley.recaf.test.dummy.ClassWithExceptions.foo;"));
				assertTrue(modified.contains("foo(args[0]);"));
			});
		}
	}

	@Nested
	@SuppressWarnings("deprecation")
	class ErroneousInput {
		@Test
		void testResolveWithInvalidGoto() {
			// CFR can emit illegal code like this with patterns such as:
			//   ** GOTO lbl-1000
			//   else lbl-1000:
			// So in our AST resolving we simply check if the AST model's text and the input text are the same
			// for all given positions. When a mismatch is detected, its assumed the AST dropped tokens present
			// in the input text since they were invalid.
			//
			// With this assumption we scan forward until we get our next match.
			String source = """
					package software.coley.recaf.test.dummy;
										
					enum DummyEnum {
						ONE, TWO, THREE;
						
						void foo() {}
						
						void bar() {
						   ** GOTO lbl-1000
						   
						   ONE.foo();
						}
					}
					""";
			handleUnit(source, (unit, ctx) -> {
				assertNotEquals(source, unit.print(), "Expected OpenRewrite AST to drop illegal chars");

				int lastOne = source.lastIndexOf("ONE");
				validateRange(unit, lastOne, lastOne + 3, source, ClassMemberPathNode.class, memberPath -> {
					ClassInfo owner = memberPath.getValueOfType(ClassInfo.class);
					ClassMember member = memberPath.getValue();
					assertEquals("software/coley/recaf/test/dummy/DummyEnum", owner.getName());
					assertEquals("ONE", member.getName());
				});
			});
		}

		@Test
		void testResolveWithReplacedTokens() {
			String source = """
					package software.coley.recaf.test.dummy;
										
					enum DummyEnum {
						ONE, TWO, THREE;
						
						void foo() {}
						
						void bar() {
						   ONE.foo();
						}
					}
					""";
			String sourceReplaced = source.replace("foo() {}", "xyz(int param) {\n\t\tbar();\n\t}");
			handleUnit(source, (unit, ctx) -> {
				// Now that the unit is parsed from the original source
				// we will use the modified source to simulate the unit making changes to the source.
				int lastOne = sourceReplaced.lastIndexOf("ONE");
				validateRange(unit, lastOne, lastOne + 3, sourceReplaced, ClassMemberPathNode.class, memberPath -> {
					ClassInfo owner = memberPath.getValueOfType(ClassInfo.class);
					ClassMember member = memberPath.getValue();
					assertEquals("software/coley/recaf/test/dummy/DummyEnum", owner.getName());
					assertEquals("ONE", member.getName());
				});
			});
		}

		@Test
		void testResolveWithMissingEndBraces() {
			String source = """
					package software.coley.recaf.test.dummy;
										
					public class ClassWithMultipleMethods {
						public static String append(String one, String two) {
							return one + two;
						}
						public static String append(String one, String two, String three) {
							return append(append(one, two), three);
						}
						public static int add(int a, int b) {
							return a + b;
						}
						public static int add(int a, int b, int c) {
							return add(add(a, b), c);
					""";
			handleUnit(source, (unit, ctx) -> {
				int index = source.indexOf("append(one, two)");
				validateRange(unit, index + 1, index + 4, source, ClassMemberPathNode.class, memberPath -> {
					ClassInfo owner = memberPath.getValueOfType(ClassInfo.class);
					ClassMember member = memberPath.getValue();
					assertEquals("software/coley/recaf/test/dummy/ClassWithMultipleMethods", owner.getName());
					assertEquals("append", member.getName());
					assertEquals("(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", member.getDescriptor());
				});
			});
		}

		@Test
		@Disabled("u0000 in the variable name causes the following ', String two' to be treated as white-space" +
				"So the definition becomes (String)String, which is wrong. This arises from javac handling, so it" +
				"cannot be fixed")
		void testResolveWithNullTerminatorParameterName() {
			String source = """
					package software.coley.recaf.test.dummy;
										
					public class ClassWithMultipleMethods {
						public static String append(String \0, String two) {
							return "one" + two;
						}
						public static String append(String one, String two, String three) {
							return append(append(one, two), three);
						}
						public static int add(int a, int b) {
							return a + b;
						}
						public static int add(int a, int b, int c) {
							return add(add(a, b), c);
						}
					}
					""";
			handleUnit(source, (unit, ctx) -> {
				int index = source.indexOf("append(one, two)");
				validateRange(unit, index + 1, index + 4, source, ClassMemberPathNode.class, memberPath -> {
					ClassInfo owner = memberPath.getValueOfType(ClassInfo.class);
					ClassMember member = memberPath.getValue();
					assertEquals("software/coley/recaf/test/dummy/ClassWithMultipleMethods", owner.getName());
					assertEquals("append", member.getName());
					assertEquals("(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", member.getDescriptor());
				});
			});
		}

		@Test
		@Disabled("Reserved keywords break the parser in the same way as the test above with u0000")
		void testResolveWithKeywordParameterName() {
			String source = """
					package software.coley.recaf.test.dummy;
										
					public class ClassWithMultipleMethods {
						public static String append(String static, String two) {
							return "one" + two;
						}
						public static String append(String one, String two, String three) {
							return append(append(one, two), three);
						}
						public static int add(int a, int b) {
							return a + b;
						}
						public static int add(int a, int b, int c) {
							return add(add(a, b), c);
						}
					}
					""";
			handleUnit(source, (unit, ctx) -> {
				unit = (J.CompilationUnit) unit.acceptJava(new SpaceFilteringVisitor(), ctx);
				int index = source.indexOf("append(one, two)");
				validateRange(unit, index + 1, index + 4, source, ClassMemberPathNode.class, memberPath -> {
					ClassInfo owner = memberPath.getValueOfType(ClassInfo.class);
					ClassMember member = memberPath.getValue();
					assertEquals("software/coley/recaf/test/dummy/ClassWithMultipleMethods", owner.getName());
					assertEquals("append", member.getName());
					assertEquals("(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", member.getDescriptor());
				});
			});
		}
	}

	private static <T> void validateRange(@Nonnull J.CompilationUnit unit,
										  @Nonnull String source, @Nonnull String match,
										  @Nonnull Class<T> targetType,
										  @Nonnull Consumer<T> consumer) {
		int start = source.indexOf(match);
		int end = start + match.length();
		validateRange(unit, start, end, source, targetType, consumer);
	}

	@SuppressWarnings("unchecked")
	private static <T> void validateRange(@Nonnull J.CompilationUnit unit,
										  int start, int end,
										  @Nonnull String source,
										  @Nonnull Class<T> targetType,
										  @Nonnull Consumer<T> consumer) {
		for (int i = start; i < end; i++) {
			AstResolveResult result = helper.resolve(unit, i, source);
			if (result != null && targetType.isAssignableFrom(result.path().getClass())) {
				consumer.accept((T) result.path());
			} else {
				fail("Failed to identify target at index: " + i + " in range [" + start + "-" + end + "]");
			}
		}
	}

	@SuppressWarnings("LanguageMismatch")
	private static void handleUnit(String source, BiConsumer<J.CompilationUnit, ExecutionContext> consumer) {
		InMemoryExecutionContext context = new InMemoryExecutionContext(Throwable::printStackTrace);
		List<J.CompilationUnit> units = Unchecked.cast(parser.parse(context, source).collect(Collectors.toList()));
		assertEquals(1, units.size());
		if (consumer != null)
			consumer.accept(units.get(0), context);
	}
}
