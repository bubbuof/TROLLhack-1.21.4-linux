package ru.zenith.api.feature.command.exception;

import ru.zenith.api.feature.command.ICommand;
import ru.zenith.api.feature.command.argument.ICommandArgument;
import ru.zenith.common.QuickLogger;

import java.util.List;

public class CommandNotFoundException extends CommandException implements QuickLogger {

    public final String command;

    public CommandNotFoundException(String command) {
        super(String.format("Команда не найдена: %s", command));
        this.command = command;
    }

    @Override
    public void handle(ICommand command, List<ICommandArgument> args) {
       logDirect(getMessage());
    }
}
