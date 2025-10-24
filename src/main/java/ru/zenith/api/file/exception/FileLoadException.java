package ru.zenith.api.file.exception;

public class FileLoadException extends FileProcessingException {
    public FileLoadException(String message) {
        super(message);
    }

    public FileLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}