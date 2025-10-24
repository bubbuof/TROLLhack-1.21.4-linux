package ru.zenith.api.feature.command.datatypes;

import ru.zenith.api.feature.command.exception.CommandException;

public interface IDatatypeFor<T> extends IDatatype  {
    T get(IDatatypeContext datatypeContext) throws CommandException;
}
