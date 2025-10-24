package ru.zenith.core;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.kotopushka.compiler.sdk.annotations.CompileBytecode;
import ru.kotopushka.compiler.sdk.annotations.Initialization;
import ru.kotopushka.compiler.sdk.annotations.VMProtect;
import ru.kotopushka.compiler.sdk.enums.VMProtectType;
import ru.zenith.api.file.exception.FileProcessingException;
import ru.zenith.api.repository.box.BoxESPRepository;
import ru.zenith.api.repository.rct.RCTRepository;
import ru.zenith.api.repository.way.WayRepository;
import ru.zenith.api.system.discord.DiscordManager;
import ru.zenith.api.feature.draggable.DraggableRepository;
import ru.zenith.api.file.*;
import ru.zenith.api.repository.macro.MacroRepository;
import ru.zenith.api.event.EventManager;
import ru.zenith.api.feature.module.ModuleProvider;
import ru.zenith.api.feature.module.ModuleRepository;
import ru.zenith.api.feature.module.ModuleSwitcher;
import ru.zenith.api.system.sound.SoundManager;
import ru.zenith.common.util.logger.LoggerUtil;
import ru.zenith.common.util.render.ScissorManager;
import ru.zenith.core.client.ClientInfo;
import ru.zenith.core.client.ClientInfoProvider;
import ru.zenith.core.listener.ListenerRepository;
import ru.zenith.implement.features.commands.CommandDispatcher;
import ru.zenith.implement.features.commands.manager.CommandRepository;
import ru.zenith.implement.features.modules.combat.killaura.attack.AttackPerpetrator;
import ru.zenith.implement.screens.menu.MenuScreen;

import java.io.File;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Main implements ModInitializer {

    @Getter
    static Main instance;
    EventManager eventManager = new EventManager();
    ModuleRepository moduleRepository;
    ModuleSwitcher moduleSwitcher;
    CommandRepository commandRepository;
    CommandDispatcher commandDispatcher;
    BoxESPRepository boxESPRepository = new BoxESPRepository(eventManager);
    MacroRepository macroRepository = new MacroRepository(eventManager);
    WayRepository wayRepository = new WayRepository(eventManager);
    RCTRepository RCTRepository = new RCTRepository(eventManager);
    ModuleProvider moduleProvider;
    DraggableRepository draggableRepository;
    DiscordManager discordManager;
    FileRepository fileRepository;
    FileController fileController;
    ScissorManager scissorManager = new ScissorManager();
    ClientInfoProvider clientInfoProvider;
    ListenerRepository listenerRepository;
    AttackPerpetrator attackPerpetrator = new AttackPerpetrator();
    boolean initialized;

    @Override
    @CompileBytecode
    public void onInitialize() {
        instance = this;

        initClientInfoProvider();
        initModules();
        initDraggable();
        initFileManager();
        initCommands();
        initListeners();
        initDiscordRPC();
        SoundManager.init();
        MenuScreen menuScreen = new MenuScreen();
        menuScreen.initialize();

        initialized = true;
    }

    @Compile
    @Initialization
    private void initDraggable() {
        draggableRepository = new DraggableRepository();
        draggableRepository.setup();
    }

    @Compile
    @Initialization
    private void initModules() {
        moduleRepository = new ModuleRepository();
        moduleRepository.setup();
        moduleProvider = new ModuleProvider(moduleRepository.modules());
        moduleSwitcher = new ModuleSwitcher(moduleRepository.modules(), eventManager);
    }

    @Compile
    @Initialization
    private void initCommands() {
        commandRepository = new CommandRepository();
        commandDispatcher = new CommandDispatcher(eventManager);
    }


    private void initDiscordRPC() {
        discordManager = new DiscordManager();
        discordManager.init();
    }


    private void initClientInfoProvider() {
        File clientDirectory = new File(MinecraftClient.getInstance().runDirectory, "\\zenith\\");
        File filesDirectory = new File(clientDirectory, "\\files\\");
        File moduleFilesDirectory = new File(filesDirectory, "\\config\\");
        clientInfoProvider = new ClientInfo("ZENITH", "FABOS", "ADMIN", clientDirectory, filesDirectory, moduleFilesDirectory);
    }

    private void initFileManager() {
        DirectoryCreator directoryCreator = new DirectoryCreator();
        directoryCreator.createDirectories(clientInfoProvider.clientDir(), clientInfoProvider.filesDir(), clientInfoProvider.configsDir());
        fileRepository = new FileRepository();
        fileRepository.setup(this);
        fileController = new FileController(fileRepository.getClientFiles(), clientInfoProvider.filesDir(), clientInfoProvider.configsDir());
        try {
            fileController.loadFiles();
        } catch (FileProcessingException e) {
            LoggerUtil.error("Error occurred while loading files: " + e.getMessage() + " " + e.getCause());
        }
    }


    private void initListeners() {
        listenerRepository = new ListenerRepository();
        listenerRepository.setup();
    }
}
