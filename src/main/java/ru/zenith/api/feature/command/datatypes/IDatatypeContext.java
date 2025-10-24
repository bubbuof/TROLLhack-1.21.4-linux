package ru.zenith.api.feature.command.datatypes;

import ru.zenith.api.feature.command.argument.IArgConsumer;

public interface IDatatypeContext {
    IArgConsumer getConsumer();
}
