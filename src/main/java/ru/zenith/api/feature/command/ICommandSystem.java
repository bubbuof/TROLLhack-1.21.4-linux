package ru.zenith.api.feature.command;

import ru.zenith.api.feature.command.argparser.IArgParserManager;

public interface ICommandSystem {
    IArgParserManager getParserManager();
}
