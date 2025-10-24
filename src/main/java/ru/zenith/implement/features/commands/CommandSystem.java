package ru.zenith.implement.features.commands;

import ru.zenith.api.feature.command.ICommandSystem;
import ru.zenith.api.feature.command.argparser.IArgParserManager;
import ru.zenith.implement.features.commands.argparser.ArgParserManager;

public enum CommandSystem implements ICommandSystem {
    INSTANCE;

    @Override
    public IArgParserManager getParserManager() {
        return ArgParserManager.INSTANCE;
    }
}
