package software.coley.recaf.cli;

import picocli.CommandLine;
import software.coley.recaf.Bootstrap;
import software.coley.recaf.Recaf;
import software.coley.recaf.RecafBuildConfig;
import software.coley.recaf.cli.command.DecompileCommand;
import software.coley.recaf.cli.command.RecafCommand;
import software.coley.recaf.launch.LaunchCommand;

import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "recaf-cli",
        mixinStandardHelpOptions = true,
        version = RecafBuildConfig.VERSION,
        description = "Recaf: The modern Java reverse engineering tool.",
        subcommands = {
                LaunchCommand.class,
                DecompileCommand.class
        }
)
public class Main implements Callable<Integer> {
    private boolean launchUi = false;
    private Recaf recaf;

    public int run(String[] args) {
        // Get the recaf instance. It should be initialized by the caller.
        recaf = Bootstrap.get();

        CommandLine cmd = new CommandLine(this);
        cmd.setExecutionStrategy(new CommandLine.RunLast() {
            @Override
            public int execute(CommandLine.ParseResult parseResult) throws CommandLine.ExecutionException {
                for (CommandLine commandLine : parseResult.asCommandLineList()) {
                    Object command = commandLine.getCommand();
                    if (command instanceof RecafCommand) {
                        ((RecafCommand) command).setRecaf(recaf);
                    }
                }
                return super.execute(parseResult);
            }
        });
        return cmd.execute(args);
    }

    @Override
    public Integer call() throws Exception {
        launchUi = true;
        return 0;
    }

    public boolean shouldLaunchUi() {
        return launchUi;
    }
}
