package me.coley.recaf.control.headless;

import me.coley.recaf.Recaf;
import me.coley.recaf.command.impl.Disassemble;
import me.coley.recaf.parse.assembly.LineParseException;
import me.coley.recaf.parse.assembly.VerifyException;
import me.coley.recaf.parse.assembly.visitors.AssemblyVisitor;
import me.coley.recaf.util.ClassUtil;
import org.apache.commons.io.FileUtils;
import org.jline.builtins.Nano;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import java.io.File;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static me.coley.recaf.util.Log.*;

/**
 * A utility using JLine to interactively editing varied content.
 *
 * @author Matt
 */
public class JLineEditor {
	private final Terminal terminal;

	/**
	 * Create a editor with the JLine terminal.
	 *
	 * @param terminal Terminal to work off of.
	 */
	public JLineEditor(Terminal terminal) {
		this.terminal = terminal;
	}

	/**
	 * Open an editor with the given title / file to load from. Blocks until the editor is closed.
	 *
	 * @param title
	 * 		Editor title.
	 * @param file
	 * 		File containing content to read/write to.
	 *
	 * @throws IOException
	 * 		Thrown if the file cannot be opened.
	 */
	private void openEditor(String title, File file) throws IOException {
		Nano nano = new Nano(terminal, file);
		nano.title = title;
		nano.open(file.getAbsolutePath());
		nano.run();
		terminal.puts(InfoCmp.Capability.clear_screen);
		terminal.flush();
	}

	/**
	 * Open and handle editing disassembled method code.
	 *
	 * @param wrapper
	 * 		Disassembly results.
	 */
	public void open(Disassemble.Result wrapper) {
		try {
			MethodNode mn = wrapper.getMethod();
			ClassNode cn = wrapper.getOwner();
			// Write disassembled text to file
			File tmp = File.createTempFile("recaf", "disass");
			FileUtils.write(tmp, wrapper.getDisassembled(), UTF_8);
			long last = tmp.lastModified();
			// Open editor
			openEditor("Assembler", tmp);
			// Check if any edits occured.
			if (tmp.lastModified() == last)
				return;
			// Assemble modified code
			String code = FileUtils.readFileToString(tmp, UTF_8);
			AssemblyVisitor av = new AssemblyVisitor();
			av.setDoAddVariables(true);
			av.setupMethod(mn.access, mn.desc);
			av.visit(code);
			av.getMethod().name = mn.name;
			// Verify
			try {
				av.verify();
			} catch(VerifyException ex) {
				AbstractInsnNode insn = ex.getInsn();
				if (insn == null)
					error(ex, "Non-analysis related exception occurred");
				else
					error("{}\nCause on line: {}", ex.getMessage(), av.getLine(insn));
				return;
			}
			// Replace method
			int index = cn.methods.indexOf(mn);
			if(index >= 0)
				cn.methods.set(index, av.getMethod());
			else
				throw new IllegalStateException("Failed to replace method, " +
						"modified method no longer exists in the class?");
			byte[] value = ClassUtil.toCode(cn, ClassWriter.COMPUTE_FRAMES);
			Recaf.getCurrentWorkspace().getPrimary().getClasses().put(cn.name, value);
			// Cleanup temp
			tmp.delete();
			info("Updated {}.{}{}", cn.name, mn.name, mn.desc);
		} catch(LineParseException ex) {
			error(ex, "On line: " + ex.getLine() + " '" + ex.getText() + "'");
		} catch(IOException ex) {
			error(ex, "IO error");
		}
	}
}
