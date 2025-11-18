package software.coley.recaf.services.tutorial;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import software.coley.cafedude.InvalidClassException;
import software.coley.cafedude.classfile.ClassFile;
import software.coley.cafedude.classfile.constant.CpUtf8;
import software.coley.cafedude.io.ClassFileReader;
import software.coley.cafedude.io.ClassFileWriter;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.IncompletePathException;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.services.comment.ClassComments;
import software.coley.recaf.services.comment.CommentManager;
import software.coley.recaf.services.comment.WorkspaceComments;
import software.coley.recaf.services.decompile.DecompilerManager;
import software.coley.recaf.services.decompile.filter.JvmBytecodeFilter;
import software.coley.recaf.services.mapping.BasicMappingsRemapper;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.Mappings;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.tutorial.content.Chapter1;
import software.coley.recaf.services.tutorial.content.Chapter2;
import software.coley.recaf.services.tutorial.content.Chapter3;
import software.coley.recaf.services.tutorial.content.Chapter4;
import software.coley.recaf.services.tutorial.content.Chapter5;
import software.coley.recaf.services.tutorial.content.Chapter6;
import software.coley.recaf.services.tutorial.content.Chapter7;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.control.richtext.source.JavaContextActionManager;
import software.coley.recaf.util.ClassDefiner;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.threading.ThreadUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.ResourceJvmClassListener;
import software.coley.recaf.workspace.model.resource.RuntimeWorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResourceBuilder;

import java.awt.Toolkit;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Constructs instances of {@link TutorialWorkspace}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class TutorialWorkspaceBuilder {
	private final CommentManager commentManager;
	private final WorkspaceManager workspaceManager;
	private final DecompilerManager decompilerManager;
	private final JavaContextActionManager javaContextActionManager;
	private final Actions actions;
	private final TutorialConfig tutorialConfig;

	@Inject
	public TutorialWorkspaceBuilder(@Nonnull CommentManager commentManager,
	                                @Nonnull WorkspaceManager workspaceManager,
	                                @Nonnull DecompilerManager decompilerManager,
	                                @Nonnull JavaContextActionManager javaContextActionManager,
	                                @Nonnull Actions actions,
	                                @Nonnull TutorialConfig tutorialConfig) {
		this.commentManager = commentManager;
		this.workspaceManager = workspaceManager;
		this.decompilerManager = decompilerManager;
		this.javaContextActionManager = javaContextActionManager;
		this.actions = actions;
		this.tutorialConfig = tutorialConfig;
	}

	/**
	 * @return New tutorial workspace instance.
	 */
	@Nonnull
	public TutorialWorkspace generateWorkspace() {
		TutorialWorkspaceResource resource = new TutorialWorkspaceResource(new WorkspaceResourceBuilder());
		TutorialWorkspace workspace = new TutorialWorkspace(resource);
		JvmClassBundle bundle = resource.getJvmClassBundle();

		// TODO: Additional Chapters
		//  More docking and UI
		//     - Note mentioning how drag-drop works
		//        - Mention that it may be useful to have things side-by-side for this tutorial
		//     - Click tabs to collapse them if they are perpendicular (Accordion spanning)
		//  More searching
		//     - Local search
		//         - Control + F to open find
		//         - Control + R to open replace
		//            - Features of these search bars
		//     - Instruction disassembly search
		//  Mapping
		//     - Renaming junk into easy to understand names
		//     - Automated mass renaming
		//     - Advanced mapping
		//        - Minecraft mod example
		//           - Fabric because yarn mappings are easy to grab
		//           - Cover "assume unique keys"
		//  Deobfuscation
		//     - Different obfuscation examples
		//        - Showcase individual transformers

		// TODO: Only add content to workspace as user completes tutorial actions
		JvmClassInfo chapter1 = fromRuntimeClass(Chapter1.class);
		JvmClassInfo chapter2 = fromRuntimeClass(Chapter2.class);
		JvmClassInfo chapter3 = addSecretConstant(fromRuntimeClass(Chapter3.class));
		JvmClassInfo chapter4 = fromRuntimeClass(Chapter4.class);
		JvmClassInfo chapter5 = fromRuntimeClass(Chapter5.class);
		JvmClassInfo chapter6 = fromRuntimeClass(Chapter6.class);
		JvmClassInfo chapter7 = fromRuntimeClass(Chapter7.class);
		bundle.put(chapter1);
		bundle.put(chapter2);
		bundle.put(chapter3);
		bundle.put(chapter4);
		bundle.put(chapter5);
		bundle.put(chapter6);
		bundle.put(chapter7);

		ClassPathNode chapter1path = PathNodes.classPath(workspace, resource, bundle, chapter1);
		ClassPathNode chapter2path = PathNodes.classPath(workspace, resource, bundle, chapter2);
		ClassPathNode chapter3path = PathNodes.classPath(workspace, resource, bundle, chapter3);
		ClassPathNode chapter4path = PathNodes.classPath(workspace, resource, bundle, chapter4);
		ClassPathNode chapter5path = PathNodes.classPath(workspace, resource, bundle, chapter5);
		ClassPathNode chapter6path = PathNodes.classPath(workspace, resource, bundle, chapter6);
		ClassPathNode chapter7path = PathNodes.classPath(workspace, resource, bundle, chapter7);

		// Fill in comments for all the classes so that the decompiler will show them.
		commentManager.removeWorkspaceComments(workspace);
		WorkspaceComments workspaceComments = commentManager.getOrCreateWorkspaceComments(workspace);

		ClassComments chapter1Comments = Objects.requireNonNull(workspaceComments.getOrCreateClassComments(chapter1path));
		chapter1Comments.setClassComment(Lang.get("tutorial.1.class"));
		chapter1Comments.setFieldComment("message", "Ljava/lang/String;", Lang.get("tutorial.1.field"));
		chapter1Comments.setMethodComment("main", "([Ljava/lang/String;)V", Lang.get("tutorial.1.main"));
		chapter1Comments.setMethodComment("run", "()V", Lang.get("tutorial.1.run"));

		ClassComments chapter2Comments = Objects.requireNonNull(workspaceComments.getOrCreateClassComments(chapter2path));
		chapter2Comments.setClassComment(Lang.get("tutorial.2.class"));
		chapter2Comments.setFieldComment("findTheHiddenMessage", "I", Lang.get("tutorial.2.field"));
		chapter2Comments.setMethodComment("hiddenMethod", "()V", Lang.get("tutorial.2.method"));

		ClassComments chapter3Comments = Objects.requireNonNull(workspaceComments.getOrCreateClassComments(chapter3path));
		chapter3Comments.setClassComment(Lang.get("tutorial.3.class"));
		chapter3Comments.setFieldComment("answer", "Ljava/lang/String;", Lang.get("tutorial.3.field"));
		chapter3Comments.setMethodComment("impl", "()V", Lang.get("tutorial.3.method"));

		ClassComments chapter4Comments = Objects.requireNonNull(workspaceComments.getOrCreateClassComments(chapter4path));
		chapter4Comments.setClassComment(Lang.get("tutorial.4.class"));
		chapter4Comments.setFieldComment("answer", "I", Lang.get("tutorial.4.field"));
		chapter4Comments.setMethodComment("main", "([Ljava/lang/String;)V", Lang.get("tutorial.4.method"));

		ClassComments chapter5Comments = Objects.requireNonNull(workspaceComments.getOrCreateClassComments(chapter5path));
		chapter5Comments.setClassComment(Lang.get("tutorial.5.class"));
		chapter5Comments.setMethodComment("main", "([Ljava/lang/String;)V", Lang.get("tutorial.5.main"));
		chapter5Comments.setMethodComment("decrypt", "()Ljava/lang/String;", Lang.get("tutorial.5.decrypt"));
		chapter5Comments.setMethodComment("nice", "()L" + chapter6.getName() + ";", Lang.get("tutorial.5.finished"));

		ClassComments chapter6Comments = Objects.requireNonNull(workspaceComments.getOrCreateClassComments(chapter6path));
		chapter6Comments.setClassComment(Lang.get("tutorial.6.class"));
		chapter6Comments.setMethodComment("whereIsThisUsed", "()Ljava/lang/String;", Lang.get("tutorial.6.method"));

		ClassComments chapter7Comments = Objects.requireNonNull(workspaceComments.getOrCreateClassComments(chapter7path));
		chapter7Comments.setClassComment(Lang.get("tutorial.7.class"));

		AtomicBoolean hiddenChapter2 = new AtomicBoolean(true);
		AtomicBoolean usedJavaToBytecode = new AtomicBoolean(false);
		JvmBytecodeFilter decompileFilter = new JvmBytecodeFilter() {
			@Nonnull
			@Override
			public byte[] filter(@Nonnull Workspace workspace, @Nonnull JvmClassInfo initialClassInfo, @Nonnull byte[] bytecode) {
				if (isChapter(2, initialClassInfo) && hiddenChapter2.get()) {
					// Filter class to only show the field
					ClassReader reader = new ClassReader(bytecode);
					ClassWriter cw = new ClassWriter(reader, 0);
					ClassVisitor cv = new ClassVisitor(RecafConstants.getAsmVersion(), cw) {
						@Override
						public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
							// Filter out parents
							super.visit(version, access, name, signature, "java/lang/Object", null);
						}

						@Override
						public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
							// Filter out methods
							return null;
						}
					};
					reader.accept(cv, ClassReader.SKIP_CODE);
					return cw.toByteArray();
				}
				return bytecode;
			}
		};
		JavaContextActionManager.SelectListener selectListener = memberPath -> {
			if (memberPath.getValue().getName().equals("hiddenMethod")) {
				// Force refresh
				hiddenChapter2.set(false);
				bundle.put(chapter2.toJvmClassBuilder().build());
			}
		};
		resource.addResourceJvmClassListener(new ResourceJvmClassListener() {
			private static final int delayMs = 600;

			@Override
			public void onUpdateClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo oldCls, @Nonnull JvmClassInfo newCls) {
				// Confirm user changed the message
				if (isChapter(1, newCls) && !newCls.getFields().isEmpty() && !Chapter1.message.equals(newCls.getFields().getFirst().getDefaultValue())) {
					FxThreadUtil.delayedRun(delayMs, () -> open(chapter2path));
				}

				// Confirm user found the correct value
				else if (isChapter(3, newCls) && !newCls.getFields().isEmpty()) {
					if ("LowLevelView".equals(newCls.getFields().getFirst().getDefaultValue()))
						FxThreadUtil.delayedRun(delayMs, () -> open(chapter4path));
					else
						Toolkit.getDefaultToolkit().beep();
				}

				// Confirm user found the correct value
				else if (isChapter(4, newCls) && !newCls.getFields().isEmpty() && Integer.valueOf(25565).equals(newCls.getFields().getFirst().getDefaultValue())) {
					FxThreadUtil.delayedRun(delayMs, () -> open(chapter5path));
				}

				// Confirm user used the 'java to bytecode' feature and correctly implemented the method
				else if (isChapter(5, newCls) && !usedJavaToBytecode.get()) {
					ThreadUtil.runDelayed(delayMs, () -> { // Need to dispatch async after bundle is updated (it is not at this point in the listener)
						try {
							String name = newCls.getName();
							ClassDefiner definer = new ClassDefiner(name, newCls.getBytecode());
							Class<?> cls = definer.findClass(name);
							Method decrypt = cls.getDeclaredMethod("decrypt");
							Object result = decrypt.invoke(null);
							if ("converted-with-ease".equals(result)) {
								usedJavaToBytecode.set(true);

								// Update class to reveal a new method that tells the user how to go to the next chapter.
								ClassReader reader = newCls.getClassReader();
								ClassWriter writer = new ClassWriter(0);
								ClassVisitor modder = new ClassVisitor(RecafConstants.getAsmVersion(), writer) {
									@Override
									public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
										super.visit(version, access, name, signature, superName, interfaces);

										// Insert this method at the top
										MethodVisitor mv = super.visitMethod(Opcodes.ACC_STATIC, "nice", "()L" + chapter6.getName() + ";", null, null);
										mv.visitCode();
										mv.visitInsn(Opcodes.ACONST_NULL);
										mv.visitInsn(Opcodes.ARETURN);
										mv.visitMaxs(1, 1);
										mv.visitEnd();
									}

									@Override
									public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
										// Keep only our inserted method above, and the decrypt method the user just implemented
										if (name.indexOf(' ') < 0)
											return null;
										return super.visitMethod(access, name, descriptor, signature, exceptions);
									}
								};
								reader.accept(modder, 0);
								byte[] bytes = writer.toByteArray();
								bundle.put(name, new JvmClassInfoBuilder(bytes).build());
							}
						} catch (Throwable t) {
							Toolkit.getDefaultToolkit().beep();
						}
					});
				}

				// End the tutorial
				else if (isChapter(7, newCls)) {
					tutorialConfig.getFinishedTutorial().setValue(true);
					ThreadUtil.runDelayed(delayMs, () -> workspaceManager.setCurrent(null));
				}
			}

			@Override
			public void onNewClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo cls) {}

			@Override
			public void onRemoveClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo cls) {}
		});
		decompilerManager.addJvmBytecodeFilter(decompileFilter);
		workspaceManager.addWorkspaceOpenListener(w -> {
			if (workspace == w) {
				decompilerManager.addJvmBytecodeFilter(decompileFilter);
				javaContextActionManager.addSelectListener(selectListener);
				open(chapter1path);
			} else {
				decompilerManager.removeJvmBytecodeFilter(decompileFilter);
				javaContextActionManager.removeSelectListener(selectListener);
			}
		});
		return workspace;
	}

	private void open(@Nonnull ClassPathNode path) {
		FxThreadUtil.run(() -> {
			try {
				actions.gotoDeclaration(path);
			} catch (IncompletePathException ex) {
				throw new RuntimeException("Failed navigating to class", ex);
			}
		});
	}

	private static boolean isChapter(int chapter, @Nonnull ClassInfo cls) {
		return cls.getName().endsWith("Chapter" + chapter);
	}

	@Nonnull
	private static JvmClassInfo addSecretConstant(@Nonnull JvmClassInfo cls) {
		byte[] modified;
		try {
			ClassFileReader reader = new ClassFileReader();
			ClassFile file = reader.read(cls.getBytecode());
			file.getPool().add(new CpUtf8("You found the secret! Change back to the 'Decompile' view and put it in the answer field."));
			file.getPool().add(new CpUtf8("LowLevelView"));

			ClassFileWriter writer = new ClassFileWriter();
			modified = writer.write(file);
		} catch (InvalidClassException ex) {
			throw new RuntimeException("Failed to read class", ex);
		}
		return cls.toJvmClassBuilder()
				.withBytecode(modified)
				.build();
	}

	@Nonnull
	private static JvmClassInfo fromRuntimeClass(@Nonnull Class<?> c) {
		byte[] classBytes = RuntimeWorkspaceResource.getRuntimeClass(c);
		if (classBytes == null)
			throw new RuntimeException("Failed reading tutorial class: " + c.getName());

		ClassWriter cw = new ClassWriter(0);
		ClassRemapper remapper = new ClassRemapper(cw, new BasicMappingsRemapper(chapterMappings));
		new ClassReader(classBytes).accept(remapper, 0);
		return new JvmClassInfoBuilder(cw.toByteArray()).build();
	}

	private static final Mappings chapterMappings = new Mappings() {
		private static final String chapterPackage = Chapter1.class.getPackageName().replace('.', '/');

		@Override
		public String getMappedClassName(@Nonnull String internalName) {
			// Move the classes to the default package
			if (internalName.startsWith(chapterPackage))
				return internalName.substring(chapterPackage.length() + 1);
			return internalName;
		}

		@Override
		public String getMappedFieldName(@Nonnull String ownerName, @Nonnull String fieldName, @Nonnull String fieldDesc) {
			return fieldName;
		}

		@Override
		public String getMappedMethodName(@Nonnull String ownerName, @Nonnull String methodName, @Nonnull String methodDesc) {
			// Rename the chapter-5 decrypt method
			if (ownerName.endsWith("Chapter5")
					&& methodName.equals("decrypt")
					&& methodDesc.equals("(Ljava/lang/String;)Ljava/lang/String;"))
				return "this whole sentence is the name of the method";
			return methodName;
		}

		@Override
		public String getMappedVariableName(@Nonnull String className, @Nonnull String methodName, @Nonnull String methodDesc, @Nullable String name, @Nullable String desc, int index) {
			return name;
		}

		@Nonnull
		@Override
		public IntermediateMappings exportIntermediate() {
			throw new IllegalStateException();
		}
	};
}
