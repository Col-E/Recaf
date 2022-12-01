package me.coley.recaf.assemble.pipeline;

import me.darknet.assembler.exceptions.AssemblerException;
import me.darknet.assembler.instructions.Argument;
import me.darknet.assembler.parser.Location;

import java.util.Arrays;

public class MissingArgumentsException extends AssemblerException {

    private final Argument[] missing;

    public MissingArgumentsException(Location location, Argument... missing) {
        super("Missing arguments: " + Arrays.toString(missing), location);
        this.missing = missing;
    }

    public Argument[] getMissing() {
        return missing;
    }
}
