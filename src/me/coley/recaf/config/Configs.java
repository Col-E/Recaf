package me.coley.recaf.config;
import me.coley.recaf.Recaf;
import me.coley.recaf.config.impl.*;

public class Configs {
	public final ConfAsm asm = new ConfAsm();
	public final ConfTheme theme = new ConfTheme();
	public final ConfUI ui = new ConfUI();
	public final ConfBlocks blocks = new ConfBlocks();
	public final ConfAgent agent = new ConfAgent();

	public void init() {
		Recaf.INSTANCE.logging.info("Loading config: ASM", 2);
		asm.load();
		Recaf.INSTANCE.logging.info("Loading config: Theme", 2);
		theme.load();
		Recaf.INSTANCE.logging.info("Loading config: UserInterface", 2);
		ui.load();
		Recaf.INSTANCE.logging.info("Loading config: CodeBlocks", 2);
		blocks.load();
		Recaf.INSTANCE.logging.info("Loading config: Agent", 2);
		agent.load();
	}	
}
