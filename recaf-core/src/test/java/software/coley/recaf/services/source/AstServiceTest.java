package software.coley.recaf.services.source;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.DirectoryPathNode;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.Mappings;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.AccessibleFields;
import software.coley.recaf.test.dummy.AnnotationImpl;
import software.coley.recaf.test.dummy.ClassWithConstructor;
import software.coley.recaf.test.dummy.ClassWithExceptions;
import software.coley.recaf.test.dummy.ClassWithMethodReference;
import software.coley.recaf.test.dummy.ClassWithMultipleMethods;
import software.coley.recaf.test.dummy.ClassWithStaticInit;
import software.coley.recaf.test.dummy.DummyEnum;
import software.coley.recaf.test.dummy.HelloWorld;
import software.coley.recaf.test.dummy.OverlapClassAB;
import software.coley.recaf.test.dummy.OverlapInterfaceA;
import software.coley.recaf.test.dummy.OverlapInterfaceB;
import software.coley.recaf.test.dummy.StringList;
import software.coley.recaf.test.dummy.StringListUser;
import software.coley.recaf.test.dummy.StringSupplier;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.Types;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.BasicJvmClassBundle;
import software.coley.sourcesolver.Parser;
import software.coley.sourcesolver.model.CompilationUnitModel;

import java.io.IOException;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AstService}
 */
@SuppressWarnings("DataFlowIssue")
public class AstServiceTest extends TestBase {
	static JvmClassInfo thisClass;
	static AstService service;
	static Parser parser;

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
				ClassWithMethodReference.class,
				AccessibleFields.class,
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
		workspaceManager.setCurrent(workspace);
		service = recaf.get(AstService.class);
		parser = service.getSharedJavaParser();
	}

	@Nested
	class Resolving {
		@Test
		void testPackage() {
			String source = """
					package software.coley.recaf.test.dummy;
					enum DummyEnum {}
					""";
			handleUnit(source, unit -> {
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
			handleUnit(source, unit -> {
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
			handleUnit(source, unit -> {
				validateRange(unit, source, "import java.io.File;", IsDeclarationTarget.REFERENCE, ClassPathNode.class, classPath -> {
					assertEquals("java/io/File", classPath.getValue().getName());
				});
				validateRange(unit, source, "import software.coley.recaf.test.dummy.DummyEnum;", IsDeclarationTarget.REFERENCE, ClassPathNode.class, classPath -> {
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
			handleUnit(source, unit -> {
				// The static member import takes precedence across the whole import statement
				validateRange(unit, source, "import static software.coley.recaf.test.dummy.DummyEnum.ONE;", IsDeclarationTarget.REFERENCE, ClassMemberPathNode.class, memberPath -> {
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
			handleUnit(source, unit -> {
				validateRange(unit, source, "class HelloWorld", IsDeclarationTarget.DECLARATION, ClassPathNode.class, classPath -> {
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
			handleUnit(source, unit -> {
				validateRange(unit, source, "interface HelloWorld", IsDeclarationTarget.DECLARATION, ClassPathNode.class, classPath -> {
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
			handleUnit(source, unit -> {
				validateRange(unit, source, "OverlapInterfaceA", IsDeclarationTarget.REFERENCE, ClassPathNode.class, classPath -> {
					assertEquals("software/coley/recaf/test/dummy/OverlapInterfaceA", classPath.getValue().getName());
				});
				validateRange(unit, source, "OverlapInterfaceB", IsDeclarationTarget.REFERENCE, ClassPathNode.class, classPath -> {
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
			handleUnit(source, unit -> {
				validateRange(unit, source, "enum HelloWorld", IsDeclarationTarget.DECLARATION, ClassPathNode.class, classPath -> {
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
			handleUnit(source, unit -> {
				validateRange(unit, source, "record HelloWorld", IsDeclarationTarget.DECLARATION, ClassPathNode.class, classPath -> {
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
			handleUnit(source, unit -> {
				validateRange(unit, source, "OBJECT_TYPE", IsDeclarationTarget.DECLARATION, ClassMemberPathNode.class, memberPath -> {
					ClassMember member = memberPath.getValue();
					assertEquals("OBJECT_TYPE", member.getName());
				});
				validateRange(unit, source, "STRING_TYPE", IsDeclarationTarget.DECLARATION, ClassMemberPathNode.class, memberPath -> {
					ClassMember member = memberPath.getValue();
					assertEquals("STRING_TYPE", member.getName());
				});
				validateRange(unit, source, "PRIMITIVES", IsDeclarationTarget.DECLARATION, ClassMemberPathNode.class, memberPath -> {
					ClassMember member = memberPath.getValue();
					assertEquals("PRIMITIVES", member.getName());
				});
				validateRange(unit, source, "getObjectType", IsDeclarationTarget.REFERENCE, ClassMemberPathNode.class, memberPath -> {
					ClassMember member = memberPath.getValue();
					assertEquals("getObjectType", member.getName());
				});
				int start = source.indexOf("new Type(");
				int end = start + "new Type".length();
				validateRange(unit, start, end, source, IsDeclarationTarget.REFERENCE, ClassPathNode.class, classPath -> {
					assertEquals("org/objectweb/asm/Type", classPath.getValue().getName());
				});
				start = source.indexOf("new Type[");
				end = start + "new Type".length();
				validateRange(unit, start, end, source, IsDeclarationTarget.REFERENCE, ClassPathNode.class, classPath -> {
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
			handleUnit(source, unit -> {
				validateRange(unit, source, "ONE", IsDeclarationTarget.DECLARATION, ClassMemberPathNode.class, memberPath -> {
					ClassMember member = memberPath.getValue();
					assertEquals("ONE", member.getName());
				});
				validateRange(unit, source, "TWO", IsDeclarationTarget.DECLARATION, ClassMemberPathNode.class, memberPath -> {
					ClassMember member = memberPath.getValue();
					assertEquals("TWO", member.getName());
				});
				validateRange(unit, source, "THREE", IsDeclarationTarget.DECLARATION, ClassMemberPathNode.class, memberPath -> {
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
			handleUnit(source, unit -> {
				validateRange(unit, source, "name()", IsDeclarationTarget.REFERENCE, ClassMemberPathNode.class, memberPath -> {
					ClassMember member = memberPath.getValue();
					assertEquals("name", member.getName());
					assertEquals("()Ljava/lang/String;", member.getDescriptor());
				});
				validateRange(unit, source, "enumConstant::name", IsDeclarationTarget.REFERENCE, ClassMemberPathNode.class, memberPath -> {
					ClassMember member = memberPath.getValue();
					assertEquals("name", member.getName());
					assertEquals("()Ljava/lang/String;", member.getDescriptor());
				});
				validateRange(unit, source, "getEnumConstants", IsDeclarationTarget.REFERENCE, ClassPathNode.class, classPath -> {
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
			handleUnit(source, unit -> {
				validateRange(unit, source, "ClassWithExceptions", IsDeclarationTarget.REFERENCE, ClassPathNode.class, classPath -> {
					assertEquals("software/coley/recaf/test/dummy/ClassWithExceptions", classPath.getValue().getName());
				});
				validateRange(unit, source, "readInt", IsDeclarationTarget.REFERENCE, ClassMemberPathNode.class, memberPath -> {
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
			handleUnit(source, unit -> {
				validateRange(unit, source, "loadLibrary", IsDeclarationTarget.REFERENCE, ClassMemberPathNode.class, memberPath -> {
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
			handleUnit(source, unit -> {
				validateRange(unit, source, "ClassWithStaticInit", IsDeclarationTarget.REFERENCE, ClassPathNode.class, classPath -> {
					assertEquals("software/coley/recaf/test/dummy/ClassWithStaticInit", classPath.getValue().getName());
				});
				validateRange(unit, source, "i ", IsDeclarationTarget.REFERENCE, ClassMemberPathNode.class, memberPath -> {
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
			handleUnit(source, unit -> {
				int start = source.indexOf("ClassWithConstructor()");
				int end = start + "ClassWithConstructor".length();
				validateRange(unit, start, end, source, IsDeclarationTarget.DECLARATION, ClassMemberPathNode.class, memberPath -> {
					ClassMember member = memberPath.getValue();
					assertEquals("<init>", member.getName());
					assertEquals("()V", member.getDescriptor());
				});
				start = source.indexOf("ClassWithConstructor(int i)");
				end = start + "ClassWithConstructor".length();
				validateRange(unit, start, end, source, IsDeclarationTarget.DECLARATION, ClassMemberPathNode.class, memberPath -> {
					ClassMember member = memberPath.getValue();
					assertEquals("<init>", member.getName());
					assertEquals("(I)V", member.getDescriptor());
				});
				start = source.indexOf("ClassWithConstructor(int i, int j)");
				end = start + "ClassWithConstructor".length();
				validateRange(unit, start, end, source, IsDeclarationTarget.DECLARATION, ClassMemberPathNode.class, memberPath -> {
					ClassMember member = memberPath.getValue();
					assertEquals("<init>", member.getName());
					assertEquals("(II)V", member.getDescriptor());
				});
				start = source.indexOf("ClassWithConstructor(DummyEnum dummyEnum, StringSupplier supplier)");
				end = start + "ClassWithConstructor".length();
				validateRange(unit, start, end, source, IsDeclarationTarget.DECLARATION, ClassMemberPathNode.class, memberPath -> {
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
			handleUnit(source, unit -> {
				int staticBlockIndex = source.lastIndexOf("static {");
				validateRange(unit, staticBlockIndex, staticBlockIndex + 6, source, IsDeclarationTarget.DECLARATION, ClassMemberPathNode.class, memberPath -> {
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
			handleUnit(source, unit -> {
				validateRange(unit, source, "of(", IsDeclarationTarget.REFERENCE, ClassMemberPathNode.class, memberPath -> {
					ClassMember member = memberPath.getValue();
					assertEquals("of", member.getName());
				});
				validateRange(unit, source, "unique", IsDeclarationTarget.REFERENCE, ClassMemberPathNode.class, memberPath -> {
					ClassMember member = memberPath.getValue();
					assertEquals("unique", member.getName());
				});
				int start = source.indexOf("StringList list");
				int end = start + "StringList".length();
				validateRange(unit, start, end, source, IsDeclarationTarget.REFERENCE, ClassPathNode.class, classPath -> {
					assertEquals("software/coley/recaf/test/dummy/StringList", classPath.getValue().getName());
				});
				start = source.indexOf("StringList.of");
				end = start + "StringList".length();
				validateRange(unit, start, end, source, IsDeclarationTarget.REFERENCE, ClassPathNode.class, classPath -> {
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
			handleUnit(source, unit -> {
				int start = source.indexOf("this");
				int end = start + "this".length();
				validateRange(unit, start, end, source, IsDeclarationTarget.REFERENCE, ClassPathNode.class, classPath -> {
					assertEquals("software/coley/recaf/test/dummy/StringList", classPath.getValue().getName());
				});
				start = source.lastIndexOf("this");
				end = start + "this".length();
				validateRange(unit, start, end, source, IsDeclarationTarget.REFERENCE, ClassPathNode.class, classPath -> {
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
			handleUnit(source, unit -> {
				IntermediateMappings mappings = new IntermediateMappings();
				mappings.addClass(DummyEnum.class.getName().replace('.', '/'), "com/example/MyEnum");

				String modified = applyMappings(unit, mappings);
				assertTrue(modified.startsWith("package com.example;"));
				assertTrue(modified.contains("enum MyEnum {"));
			});
		}

		@Test
		void renameField_EnumConst() {
			String source = """
					package software.coley.recaf.test.dummy;
										
					enum DummyEnum {
						ONE, TWO, THREE
					}
					""";
			handleUnit(source, unit -> {
				IntermediateMappings mappings = new IntermediateMappings();
				String owner = DummyEnum.class.getName().replace('.', '/');
				mappings.addField(owner, "L" + owner + ";", "ONE", "FIRST");

				String modified = applyMappings(unit, mappings);
				assertTrue(modified.contains("FIRST,"));
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
			handleUnit(source, unit -> {
				IntermediateMappings mappings = new IntermediateMappings();
				mappings.addClass(HelloWorld.class.getName().replace('.', '/'), "com/example/Howdy");

				String modified = applyMappings(unit, mappings);
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
			handleUnit(source, unit -> {
				IntermediateMappings mappings = new IntermediateMappings();
				mappings.addClass(ClassWithExceptions.class.getName().replace('.', '/'), "com/example/Call");

				String modified = applyMappings(unit, mappings);
				assertTrue(modified.contains("Call.readInt("));
			});
		}

		@Test
		void renameClass_ReplacePackageImport() {
			String source = """
					package software.coley.recaf.test.dummy;
										
					import software.coley.recaf.util.*;
										
					class HelloWorld {
						static void main(String[] args) {
							System.out.println(StringUtil.getCommonSuffix(args[0], args[1]));
						}
					}
					""";
			handleUnit(source, unit -> {
				// We'll just map one class in the util package.
				// We should import the renamed class explicitly, but keep the
				// star import since other classes in the package were not renamed
				IntermediateMappings mappings = new IntermediateMappings();
				mappings.addClass(StringUtil.class.getName().replace('.', '/'), "com/example/Strs");

				String modified = applyMappings(unit, mappings);
				assertTrue(modified.contains("import software.coley.recaf.util.*;"), "Original star import should be kept");
				assertTrue(modified.contains("import com.example.Strs"), "Missing explicit import of renamed class");
				assertTrue(modified.contains("Strs.getCommonSuffix("), "Reference to method in renamed class not updated");
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
			handleUnit(source, unit -> {
				IntermediateMappings mappings = new IntermediateMappings();
				mappings.addClass(ClassWithExceptions.class.getName().replace('.', '/'), "com/example/Call");

				String modified = applyMappings(unit, mappings);
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
			handleUnit(source, unit -> {
				IntermediateMappings mappings = new IntermediateMappings();
				mappings.addClass(ClassWithExceptions.class.getName().replace('.', '/'), "com/example/Foo");

				String modified = applyMappings(unit, mappings);
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
			handleUnit(source, unit -> {
				IntermediateMappings mappings = new IntermediateMappings();
				mappings.addClass(DummyEnum.class.getName().replace('.', '/'), "com/example/Singleton");

				String modified = applyMappings(unit, mappings);
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
			handleUnit(source, unit -> {
				IntermediateMappings mappings = new IntermediateMappings();
				mappings.addClass(ClassWithExceptions.class.getName().replace('.', '/'), "com/example/Foo");

				String modified = applyMappings(unit, mappings);
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
			handleUnit(source, unit -> {
				IntermediateMappings mappings = new IntermediateMappings();
				mappings.addClass(HelloWorld.class.getName().replace('.', '/'), "com/example/Howdy");

				String modified = applyMappings(unit, mappings);
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
			handleUnit(source, unit -> {
				IntermediateMappings mappings = new IntermediateMappings();
				mappings.addClass(HelloWorld.class.getName().replace('.', '/'), "com/example/Howdy");

				String modified = applyMappings(unit, mappings);
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
			handleUnit(source, unit -> {
				IntermediateMappings mappings = new IntermediateMappings();
				mappings.addClass(ClassWithConstructor.class.getName().replace('.', '/'), "com/example/Howdy");

				String modified = applyMappings(unit, mappings);
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
			handleUnit(source, unit -> {
				IntermediateMappings mappings = new IntermediateMappings();
				mappings.addClass(HelloWorld.class.getName().replace('.', '/'), "com/example/Howdy");

				String modified = applyMappings(unit, mappings);
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
			handleUnit(source, unit -> {
				IntermediateMappings mappings = new IntermediateMappings();
				mappings.addClass(Types.class.getName().replace('.', '/'), "com/example/TypeUtil");

				String modified = applyMappings(unit, mappings);
				System.err.println(modified);
				assertTrue(modified.contains("com.example.TypeUtil value = new com.example.TypeUtil();"));
			});
		}

		@Test
		void renameMember_FieldName() {
			String source = """
					package software.coley.recaf.test.dummy;
										
					class AccessibleFields {
						public static int CONSTANT_FIELD;
					 	private int privateFinalField = 8;
					 	protected int protectedField;
					 	public int publicField;
					 	final int packageField;
						
						AccessibleFields() {
							this.packageField = 3;
							packageField = 4;
							protectedField = packageField;
							publicField = CONSTANT_FIELD;
						}
						
						static {
							AccessibleFields.CONSTANT_FIELD = 32;
						}
					}
					""";
			handleUnit(source, unit -> {
				IntermediateMappings mappings = new IntermediateMappings();
				mappings.addField(AccessibleFields.class.getName().replace('.', '/'), "I", "CONSTANT_FIELD", "KON_FID");
				mappings.addField(AccessibleFields.class.getName().replace('.', '/'), "I", "privateFinalField", "priFid");
				mappings.addField(AccessibleFields.class.getName().replace('.', '/'), "I", "protectedField", "proFid");
				mappings.addField(AccessibleFields.class.getName().replace('.', '/'), "I", "publicField", "puFid");
				mappings.addField(AccessibleFields.class.getName().replace('.', '/'), "I", "packageField", "pkFid");

				String modified = applyMappings(unit, mappings);
				assertTrue(modified.contains("int KON_FID;"));
				assertTrue(modified.contains("int priFid = 8;"));
				assertTrue(modified.contains("int proFid;"));
				assertTrue(modified.contains("int puFid;"));
				assertTrue(modified.contains("int pkFid;"));

				assertTrue(modified.contains("this.pkFid = 3"));
				assertTrue(modified.contains("pkFid = 4;"));
				assertTrue(modified.contains("proFid = pkFid;"));
				assertTrue(modified.contains("puFid = KON_FID;"));

				assertTrue(modified.contains("AccessibleFields.KON_FID = 32;"));
			});
		}

		@Test
		void renameMember_MethodName() {
			String source = """
					package software.coley.recaf.test.dummy;
										
					import java.util.function.Supplier;
										
					class ClassWithMethodReference {
						static {
							Supplier<String> fooSupplier = ClassWithMethodReference::foo;
							foo();
						}
						
						static String foo() { return "foo"; }
					}
					""";
			handleUnit(source, unit -> {
				IntermediateMappings mappings = new IntermediateMappings();
				mappings.addMethod(ClassWithMethodReference.class.getName().replace('.', '/'), "()Ljava/lang/String;", "foo", "bar");

				String modified = applyMappings(unit, mappings);
				assertTrue(modified.contains("Supplier<String> fooSupplier = ClassWithMethodReference::bar;"));
				assertTrue(modified.contains("bar();"));
				assertTrue(modified.contains("static String bar() { return \"foo\"; }"));
			});
		}

		@Test
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
			handleUnit(source, unit -> {
				IntermediateMappings mappings = new IntermediateMappings();
				mappings.addMethod(ClassWithExceptions.class.getName().replace('.', '/'), "(Ljava/lang/Object;)I", "readInt", "foo");

				String modified = applyMappings(unit, mappings);
				assertTrue(modified.contains("import static software.coley.recaf.test.dummy.ClassWithExceptions.foo;"));
				assertTrue(modified.contains("foo(args[0]);"));
			});
		}

		@Nonnull
		private String applyMappings(@Nonnull CompilationUnitModel unit, @Nonnull Mappings mappings) {
			return service.applyMappings(unit, service.newJavaResolver(unit), mappings);
		}
	}

	@Nested
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
			handleUnit(source, unit -> {
				int firstOne = source.indexOf("ONE,");
				validateRange(unit, firstOne, firstOne + 3, source, ClassMemberPathNode.class, memberPath -> {
					ClassInfo owner = memberPath.getValueOfType(ClassInfo.class);
					ClassMember member = memberPath.getValue();
					assertEquals("software/coley/recaf/test/dummy/DummyEnum", owner.getName());
					assertEquals("ONE", member.getName());
				});

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
			handleUnit(source, unit -> {
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
			handleUnit(source, unit -> {
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
			handleUnit(source, unit -> {
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

	private static <T> void validateRange(@Nonnull CompilationUnitModel unit,
	                                      @Nonnull String source, @Nonnull String match,
	                                      @Nonnull Class<T> targetType,
	                                      @Nonnull Consumer<T> consumer) {
		int start = source.indexOf(match);
		int end = start + match.length();
		validateRange(unit, start, end, source, targetType, consumer);
	}

	private static <T> void validateRange(@Nonnull CompilationUnitModel unit,
	                                      @Nonnull String source, @Nonnull String match,
	                                      @Nonnull IsDeclarationTarget declarationTarget,
	                                      @Nonnull Class<T> targetType,
	                                      @Nonnull Consumer<T> consumer) {
		int start = source.indexOf(match);
		int end = start + match.length();
		validateRange(unit, start, end, source, declarationTarget, targetType, consumer);
	}

	private static <T> void validateRange(@Nonnull CompilationUnitModel unit,
	                                      int start, int end,
	                                      @Nonnull String source,
	                                      @Nonnull Class<T> targetType,
	                                      @Nonnull Consumer<T> consumer) {
		validateRange(unit, start, end, source, IsDeclarationTarget.DONT_CARE, targetType, consumer);
	}

	@SuppressWarnings("unchecked")
	private static <T> void validateRange(@Nonnull CompilationUnitModel unit,
	                                      int start, int end,
	                                      @Nonnull String source,
	                                      @Nonnull IsDeclarationTarget declarationTarget,
	                                      @Nonnull Class<T> targetType,
	                                      @Nonnull Consumer<T> consumer) {
		for (int i = start; i < end; i++) {
			AstResolveResult result = service.newJavaResolver(unit).resolveThenAdapt(i);
			String suffix = " at index: " + i + " in range [" + start + "-" + end + "]";
			if (result == null) {
				fail("Failed to resolve target" + suffix);
			} else if (!declarationTarget.isSatisfiedBy(result.isDeclaration())) {
				fail("Failed to clarify declaration vs reference relation" + suffix);
			} else if (!targetType.isAssignableFrom(result.path().getClass())) {
				fail("Failed to resolve target as expected type '" + targetType + "'" + suffix);
			} else {
				consumer.accept((T) result.path());
			}
		}
	}

	@SuppressWarnings("LanguageMismatch")
	private static void handleUnit(@Nonnull String source, @Nullable Consumer<CompilationUnitModel> consumer) {
		if (consumer != null)
			consumer.accept(parser.parse(source));
	}

	private enum IsDeclarationTarget {
		DECLARATION, REFERENCE, DONT_CARE;

		public boolean isSatisfiedBy(boolean isDeclaration) {
			return switch (this) {
				case DONT_CARE -> true;
				case REFERENCE -> !isDeclaration;
				case DECLARATION -> isDeclaration;
			};
		}
	}
}
