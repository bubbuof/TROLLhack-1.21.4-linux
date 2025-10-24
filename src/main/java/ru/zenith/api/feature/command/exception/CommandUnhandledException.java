package ru.zenith.api.feature.command.exception;

import ru.zenith.api.feature.command.ICommand;
import ru.zenith.api.feature.command.argument.ICommandArgument;
import ru.zenith.common.QuickLogger;

import java.util.List;

public class CommandUnhandledException extends RuntimeException implements ICommandException, QuickLogger {

    public CommandUnhandledException(String message) {
        super(message);
    }

    public CommandUnhandledException(Throwable cause) {
        super(cause);
    }

    @Override
    public void handle(ICommand command, List<ICommandArgument> args) {
    }
}
