package ru.zenith.api.feature.command.datatypes;

import ru.zenith.api.feature.command.exception.CommandException;

public interface IDatatypePost<T, O> extends IDatatype {
    T apply(IDatatypeContext datatypeContext, O original) throws CommandException;
}
