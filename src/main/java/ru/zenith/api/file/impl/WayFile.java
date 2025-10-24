package ru.zenith.api.file.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import ru.zenith.api.file.ClientFile;
import ru.zenith.api.file.exception.FileLoadException;
import ru.zenith.api.file.exception.FileSaveException;
import ru.zenith.api.repository.way.Way;
import ru.zenith.api.repository.way.WayRepository;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WayFile extends ClientFile {
    WayRepository wayRepository;

    public WayFile(WayRepository wayRepository) {
        super("way");
        this.wayRepository = wayRepository;
    }

    @Override
    public void saveToFile(File path) throws FileSaveException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File file = new File(path, getName() + ".json");

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(wayRepository.wayList, writer);
        } catch (JsonIOException | IOException e) {
            throw new FileSaveException(String.format("Failed to save %s to file", getName()), e);
        }
    }

    @Override
    public void loadFromFile(File path) throws FileLoadException {
        Gson gson = new Gson();
        File file = new File(path, getName() + ".json");

        try (FileReader reader = new FileReader(file)) {
            Way[] ways = gson.fromJson(reader, Way[].class);
            wayRepository.wayList.clear();
            wayRepository.wayList.addAll(Arrays.asList(ways));
        } catch (IOException e) {
            throw new FileLoadException(String.format("Failed to load %s from file", getName()), e);
        } catch (JsonSyntaxException e) {
            throw new FileLoadException(String.format("JSON syntax error, %s config cannot be loaded", getName()), e);
        } catch (JsonIOException e) {
            throw new FileLoadException(String.format("JSON IO error, %s config cannot be loaded", getName()), e);
        }
    }
}
