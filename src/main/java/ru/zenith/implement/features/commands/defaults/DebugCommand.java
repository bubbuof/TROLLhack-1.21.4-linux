package ru.zenith.implement.features.commands.defaults;

import ru.zenith.api.feature.command.Command;
import ru.zenith.api.feature.command.argument.IArgConsumer;
import ru.zenith.api.feature.command.exception.CommandException;

import java.util.List;
import java.util.stream.Stream;

public class DebugCommand extends Command {
    public static boolean debug = false;

    public DebugCommand() {
        super("debug", "dbg");

    }
    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        debug = !debug;
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        return null;
    }

    @Override
    public String getShortDesc() {
        return "developer";
    }

    @Override
    public List<String> getLongDesc() {
        return List.of("Developer");
    }
}
