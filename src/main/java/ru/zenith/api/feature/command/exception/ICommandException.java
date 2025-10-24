package ru.zenith.api.feature.command.exception;

import net.minecraft.util.Formatting;
import ru.zenith.api.feature.command.ICommand;
import ru.zenith.api.feature.command.argument.ICommandArgument;
import ru.zenith.common.QuickLogger;

import java.util.List;

public interface ICommandException extends QuickLogger {

    String getMessage();

    default void handle(ICommand command, List<ICommandArgument> args) {
        logDirect(
                this.getMessage(),
                Formatting.RED
        );
    }
}
