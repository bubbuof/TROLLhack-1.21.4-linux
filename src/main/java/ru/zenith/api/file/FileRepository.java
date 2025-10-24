package ru.zenith.api.file;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import ru.zenith.api.file.impl.*;
import ru.zenith.core.Main;

import java.util.ArrayList;
import java.util.List;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileRepository {
    List<ClientFile> clientFiles = new ArrayList<>();

    public void setup(Main main) {
        register(
                new ModuleFile(main.getModuleRepository(), main.getDraggableRepository()),
                new EntityESPFile(main.getBoxESPRepository()),
                new BlockESPFile(main.getBoxESPRepository()),
                new MacroFile(main.getMacroRepository()),
                new WayFile(main.getWayRepository()),
                new PrefixFile(),
                new FriendFile()
        );
    }

    public void register(ClientFile... clientFIle) {
        clientFiles.addAll(List.of(clientFIle));
    }
}
