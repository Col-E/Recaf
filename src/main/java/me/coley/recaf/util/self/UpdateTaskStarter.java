package me.coley.recaf.util.self;

import me.coley.recaf.Recaf;
import org.objectweb.asm.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Creates an independent update script and executes it.
 *
 * @author Matt
 */
public class UpdateTaskStarter implements Opcodes {
	private static final String CLASS_NAME = "UpdateRunner";
	private final String filePath;
	private final String url;
	private final long delay;
	private final String[] args;

	/**
	 * @param filePath
	 * 		Destination of downloaded artifact.
	 * @param url
	 * 		Artifact url to download.
	 * @param delay
	 * 		Delay before starting download.
	 * @param args
	 * 		Arguments to pass to downloaded artifact.
	 */
	public UpdateTaskStarter(String filePath, String url, long delay, String[] args) {
		this.filePath = filePath;
		this.url = url;
		this.delay = delay;
		this.args = args;
	}

	/**
	 * Start the updater.
	 *
	 * @throws IOException
	 * 		When the update runner class cannot be written to,
	 * 		or when the process failed to start.
	 */
	public void start() throws IOException {
		// Write updater class
		Path updaterPath = Recaf.getDirectory().resolve(CLASS_NAME +".class");
		Files.write(updaterPath, dump(), StandardOpenOption.CREATE);
		// Execute
		List<String> procArgs = new LinkedList<>();
		procArgs.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
		procArgs.add("-cp");
		procArgs.add(Recaf.getDirectory().toString());
		procArgs.add(CLASS_NAME);
		if (args != null)
			Collections.addAll(procArgs, args);
		new ProcessBuilder(procArgs).start();
	}

	/**
	 * Auto-generated code by ASMifier, slightly modified.
	 *
	 * @return Bytecode of runner class.
	 */
	private byte[] dump() {
		// This is the original method code that we will be writing to a file.
		/*
		public static void main(String[] args) throws Throwable {
			// Wait
			Thread.sleep(DELAY_HERE);
			// Download
			String pathStr = "FILE_PATH_HERE";
			try(InputStream is = new URL("URL_HERE").openStream()) {
				Files.copy(is, Paths.get(pathStr), StandardCopyOption.REPLACE_EXISTING);
			}
			// Execute downloaded jar
			List<String> procArgs = new ArrayList<>();
			procArgs.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
			procArgs.add("-jar");
			procArgs.add(pathStr);
			Collections.addAll(procArgs, args);
			new ProcessBuilder(procArgs).start();
		}
		*/
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		cw.visit(50, ACC_PUBLIC + ACC_SUPER, CLASS_NAME, null, "java/lang/Object", null);
		// Constructor
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		mv.visitInsn(RETURN);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
		// Main method
		mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null,
				new String[]{"java/lang/Throwable"});
		mv.visitCode();
		Label l0 = new Label();
		Label l1 = new Label();
		Label l2 = new Label();
		mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Throwable");
		Label l3 = new Label();
		Label l4 = new Label();
		Label l5 = new Label();
		mv.visitTryCatchBlock(l3, l4, l5, "java/lang/Throwable");
		Label l6 = new Label();
		mv.visitTryCatchBlock(l3, l4, l6, null);
		Label l7 = new Label();
		Label l8 = new Label();
		Label l9 = new Label();
		mv.visitTryCatchBlock(l7, l8, l9, "java/lang/Throwable");
		Label l10 = new Label();
		mv.visitTryCatchBlock(l5, l10, l6, null);
		Label l11 = new Label();
		mv.visitLabel(l11);
		mv.visitLineNumber(16, l11);
		mv.visitLdcInsn(delay);
		mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "sleep", "(J)V", false);
		Label l12 = new Label();
		mv.visitLabel(l12);
		mv.visitLineNumber(18, l12);
		mv.visitLdcInsn(filePath);
		mv.visitVarInsn(ASTORE, 1);
		Label l13 = new Label();
		mv.visitLabel(l13);
		mv.visitLineNumber(19, l13);
		mv.visitTypeInsn(NEW, "java/net/URL");
		mv.visitInsn(DUP);
		mv.visitLdcInsn(url);
		mv.visitMethodInsn(INVOKESPECIAL, "java/net/URL", "<init>", "(Ljava/lang/String;)V", false);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/net/URL", "openStream", "()Ljava/io/InputStream;", false);
		mv.visitVarInsn(ASTORE, 2);
		Label l14 = new Label();
		mv.visitLabel(l14);
		mv.visitInsn(ACONST_NULL);
		mv.visitVarInsn(ASTORE, 3);
		mv.visitLabel(l3);
		mv.visitLineNumber(20, l3);
		mv.visitVarInsn(ALOAD, 2);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitInsn(ICONST_0);
		mv.visitTypeInsn(ANEWARRAY, "java/lang/String");
		mv.visitMethodInsn(INVOKESTATIC, "java/nio/file/Paths", "get",
				"(Ljava/lang/String;[Ljava/lang/String;)Ljava/nio/file/Path;", false);
		mv.visitInsn(ICONST_1);
		mv.visitTypeInsn(ANEWARRAY, "java/nio/file/CopyOption");
		mv.visitInsn(DUP);
		mv.visitInsn(ICONST_0);
		mv.visitFieldInsn(GETSTATIC, "java/nio/file/StandardCopyOption", "REPLACE_EXISTING",
				"Ljava/nio/file/StandardCopyOption;");
		mv.visitInsn(AASTORE);
		mv.visitMethodInsn(INVOKESTATIC, "java/nio/file/Files", "copy",
				"(Ljava/io/InputStream;Ljava/nio/file/Path;[Ljava/nio/file/CopyOption;)J", false);
		mv.visitInsn(POP2);
		mv.visitLabel(l4);
		mv.visitLineNumber(21, l4);
		mv.visitVarInsn(ALOAD, 2);
		Label l15 = new Label();
		mv.visitJumpInsn(IFNULL, l15);
		mv.visitVarInsn(ALOAD, 3);
		Label l16 = new Label();
		mv.visitJumpInsn(IFNULL, l16);
		mv.visitLabel(l0);
		mv.visitVarInsn(ALOAD, 2);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/InputStream", "close", "()V", false);
		mv.visitLabel(l1);
		mv.visitJumpInsn(GOTO, l15);
		mv.visitLabel(l2);
		mv.visitVarInsn(ASTORE, 4);
		mv.visitVarInsn(ALOAD, 3);
		mv.visitVarInsn(ALOAD, 4);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "addSuppressed",
				"(Ljava/lang/Throwable;)V", false);
		mv.visitJumpInsn(GOTO, l15);
		mv.visitLabel(l16);
		mv.visitVarInsn(ALOAD, 2);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/InputStream", "close", "()V", false);
		mv.visitJumpInsn(GOTO, l15);
		mv.visitLabel(l5);
		mv.visitLineNumber(19, l5);
		mv.visitVarInsn(ASTORE, 4);
		mv.visitVarInsn(ALOAD, 4);
		mv.visitVarInsn(ASTORE, 3);
		mv.visitVarInsn(ALOAD, 4);
		mv.visitInsn(ATHROW);
		mv.visitLabel(l6);
		mv.visitLineNumber(21, l6);
		mv.visitVarInsn(ASTORE, 5);
		mv.visitLabel(l10);
		mv.visitVarInsn(ALOAD, 2);
		Label l17 = new Label();
		mv.visitJumpInsn(IFNULL, l17);
		mv.visitVarInsn(ALOAD, 3);
		Label l18 = new Label();
		mv.visitJumpInsn(IFNULL, l18);
		mv.visitLabel(l7);
		mv.visitVarInsn(ALOAD, 2);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/InputStream", "close", "()V", false);
		mv.visitLabel(l8);
		mv.visitJumpInsn(GOTO, l17);
		mv.visitLabel(l9);
		mv.visitVarInsn(ASTORE, 6);
		mv.visitVarInsn(ALOAD, 3);
		mv.visitVarInsn(ALOAD, 6);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "addSuppressed",
				"(Ljava/lang/Throwable;)V", false);
		mv.visitJumpInsn(GOTO, l17);
		mv.visitLabel(l18);
		mv.visitVarInsn(ALOAD, 2);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/InputStream", "close", "()V", false);
		mv.visitLabel(l17);
		mv.visitVarInsn(ALOAD, 5);
		mv.visitInsn(ATHROW);
		mv.visitLabel(l15);
		mv.visitLineNumber(23, l15);
		mv.visitTypeInsn(NEW, "java/util/ArrayList");
		mv.visitInsn(DUP);
		mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);
		mv.visitVarInsn(ASTORE, 2);
		mv.visitVarInsn(ALOAD, 2);
		mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
		mv.visitInsn(DUP);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
		mv.visitLdcInsn("java.home");
		mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "getProperty",
				"(Ljava/lang/String;)Ljava/lang/String;", false);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
				"(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
		mv.visitFieldInsn(GETSTATIC, "java/io/File", "separator", "Ljava/lang/String;");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
				"(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
		mv.visitLdcInsn("bin");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
				"(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
		mv.visitFieldInsn(GETSTATIC, "java/io/File", "separator", "Ljava/lang/String;");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
				"(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
		mv.visitLdcInsn("java");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
				"(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString",
				"()Ljava/lang/String;", false);
		mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
		mv.visitInsn(POP);
		mv.visitVarInsn(ALOAD, 2);
		mv.visitLdcInsn("-jar");
		mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
		mv.visitInsn(POP);
		mv.visitVarInsn(ALOAD, 2);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
		mv.visitInsn(POP);
		mv.visitVarInsn(ALOAD, 2);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESTATIC, "java/util/Collections", "addAll",
				"(Ljava/util/Collection;[Ljava/lang/Object;)Z", false);
		mv.visitInsn(POP);
		mv.visitTypeInsn(NEW, "java/lang/ProcessBuilder");
		mv.visitInsn(DUP);
		mv.visitVarInsn(ALOAD, 2);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/ProcessBuilder", "<init>", "(Ljava/util/List;)V", false);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/ProcessBuilder", "start", "()Ljava/lang/Process;", false);
		mv.visitInsn(POP);
		mv.visitInsn(RETURN);
		mv.visitMaxs(6, 7);
		mv.visitEnd();
		cw.visitEnd();
		return cw.toByteArray();
	}
}
