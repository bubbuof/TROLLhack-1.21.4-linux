package ru.zenith.api.feature.command.datatypes;

import ru.zenith.api.feature.command.exception.CommandException;
import ru.zenith.common.QuickImports;

import java.util.stream.Stream;

public interface IDatatype extends QuickImports {
    Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException;
}
