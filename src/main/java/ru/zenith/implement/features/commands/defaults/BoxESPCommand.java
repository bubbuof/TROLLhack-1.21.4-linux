package ru.zenith.implement.features.commands.defaults;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.zenith.api.feature.command.Command;
import ru.zenith.api.feature.command.argument.IArgConsumer;
import ru.zenith.api.feature.command.datatypes.BlockDataType;
import ru.zenith.api.feature.command.datatypes.BlockESPDataType;
import ru.zenith.api.feature.command.datatypes.EntityDataType;
import ru.zenith.api.feature.command.datatypes.EntityESPDataType;
import ru.zenith.api.feature.command.exception.CommandException;
import ru.zenith.api.feature.command.helpers.Paginator;
import ru.zenith.api.feature.command.helpers.TabCompleteHelper;
import ru.zenith.api.repository.box.BoxESPRepository;
import ru.zenith.common.QuickImports;
import ru.zenith.core.Main;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static ru.zenith.api.feature.command.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class BoxESPCommand extends Command implements QuickImports {
    final BoxESPRepository repository;

    protected BoxESPCommand(Main main) {
        super("box");
        this.repository = main.getBoxESPRepository();
    }

    @Compile
    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        String arg = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";
        switch (arg) {
            case "add" -> handleAdd(args);
            case "remove" -> handleRemove(args);
            case "clear" -> handleClear(args);
            case "list" -> handleList(label, args);
        }
    }

    @Compile
    private void handleAdd(IArgConsumer args) throws CommandException {
        args.requireMin(2);
        String type = args.getString().toLowerCase(), name = args.getString();
        int color = args.has(1) ? args.getArgs().getFirst().getAs(Integer.class) : 0;
        switch (type) {
            case "block" -> BlockDataType.INSTANCE.findBlock(name).ifPresentOrElse(block -> {
                if (!repository.blocks.containsKey(block)) {
                    repository.blocks.put(block, color);
                    StringBuilder builder = new StringBuilder();
                    builder.append("Добавлен блок: ").append(Formatting.GREEN).append(block.getName().getString()).append(Formatting.GRAY);
                    if (color != 0) builder.append(", Цвет: ").append(Formatting.GREEN).append(color);
                    logDirect(builder.toString(), Formatting.GRAY);
                } else
                    logDirect(Formatting.GRAY + "Блок \"" + Formatting.RED + block.getName().getString() + Formatting.GRAY + "\" уже добавлен");
            }, () -> logDirect(Formatting.RED + "Не удалось найти блок"));
            case "entity" -> EntityDataType.INSTANCE.findEntity(name).ifPresentOrElse(entity -> {
                if (!repository.entities.containsKey(entity)) {
                    repository.entities.put(entity, color);
                    StringBuilder builder = new StringBuilder();
                    builder.append("Добавлена сущность: ").append(Formatting.GREEN).append(entity.getName().getString()).append(Formatting.GRAY);
                    if (color != 0) builder.append(", Цвет: ").append(Formatting.GREEN).append(color);
                    logDirect(builder.toString(), Formatting.GRAY);
                } else
                    logDirect(Formatting.GRAY + "Сущность \"" + Formatting.RED + entity.getName().getString() + Formatting.GRAY + "\" уже добавлен");
            }, () -> logDirect(Formatting.RED + "Не удалось найти сущность"));
            default -> logDirect(Formatting.RED + "Не верный тип");
        }
    }

    @Compile
    private void handleRemove(IArgConsumer args) throws CommandException {
        args.requireMin(2);
        String type = args.getString().toLowerCase(), name = args.getString();
        switch (type) {
            case "block" ->
                    repository.blocks.keySet().stream().filter(block -> block.getName().getString().replace(" ", "_").equalsIgnoreCase(name)).findFirst().ifPresentOrElse(block -> {
                        repository.blocks.remove(block);
                        logDirect("Удален блок: " + Formatting.GREEN + block.getName().getString(), Formatting.GRAY);
                    }, () -> logDirect(Formatting.RED + "Не удалось найти блок"));
            case "entity" ->
                    repository.entities.keySet().stream().filter(block -> block.getName().getString().replace(" ", "_").equalsIgnoreCase(name)).findFirst().ifPresentOrElse(block -> {
                        repository.entities.remove(block);
                        logDirect("Удалена сущность: " + Formatting.GREEN + block.getName().getString(), Formatting.GRAY);
                    }, () -> logDirect(Formatting.RED + "Не удалось найти сущность"));
            default -> logDirect(Formatting.RED + "Не верный тип");
        }
    }

    @Compile
    private void handleList(String label, IArgConsumer args) throws CommandException {
        args.requireMin(1);
        String type = args.getString().toLowerCase();
        switch (type) {
            case "block" ->
                    Paginator.paginate(args, new Paginator<>(repository.blocks.entrySet().stream().toList()), () -> logDirect("Список блоков:"),
                            block -> {
                                Text text = Text.literal(Formatting.GRAY + "Название: " + Formatting.RED + block.getKey().getName().getString());
                                if (block.getValue() != 0)
                                    text.copy().append(Formatting.GRAY + "Цвет: " + Formatting.RED + block.getValue());
                                return text;
                            }, FORCE_COMMAND_PREFIX + label);
            case "entity" ->
                    Paginator.paginate(args, new Paginator<>(repository.entities.entrySet().stream().toList()), () -> logDirect("Список Сущностей:"),
                            entity -> {
                                Text text = Text.literal(Formatting.GRAY + "Название: " + Formatting.RED + entity.getKey().getName().getString());
                                if (entity.getValue() != 0)
                                    text.copy().append(Formatting.GRAY + "Цвет: " + Formatting.RED + entity.getValue());
                                return text;
                            }, FORCE_COMMAND_PREFIX + label);
            default -> logDirect(Formatting.RED + "Не верный тип");
        }
    }

    @Compile
    private void handleClear(IArgConsumer args) throws CommandException {
        args.requireMin(1);
        String type = args.getString().toLowerCase();
        switch (type) {
            case "block" -> {
                repository.blocks.clear();
                logDirect(Formatting.GREEN + "Все блоки были удалены.");
            }
            case "entity" -> {
                repository.entities.clear();
                logDirect(Formatting.GREEN + "Все сущности были удалены.");
            }
            default -> logDirect(Formatting.RED + "Не верный тип");
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        String arg = args.getString();
        if (args.has(4)) {
            return Stream.empty();
        } else if (args.has(3)) {
            return new TabCompleteHelper().append("Цвет").sortAlphabetically().stream();
        } else if (args.has(2)) {
            String nextArg = args.getString().toLowerCase();
            switch (nextArg) {
                case "block" -> {
                    if (arg.equalsIgnoreCase("remove")) return args.tabCompleteDatatype(BlockESPDataType.INSTANCE);
                    else if (arg.equalsIgnoreCase("add")) return args.tabCompleteDatatype(BlockDataType.INSTANCE);
                }
                case "entity" -> {
                    if (arg.equalsIgnoreCase("remove")) return args.tabCompleteDatatype(EntityESPDataType.INSTANCE);
                    else if (arg.equalsIgnoreCase("add")) return args.tabCompleteDatatype(EntityDataType.INSTANCE);
                }
            }
        } else if (args.hasAny()) {
            return new TabCompleteHelper().append("Block", "Entity").filterPrefix(args.getString()).sortAlphabetically().stream();
        } else {
            return new TabCompleteHelper().append("add", "remove", "list", "clear").filterPrefix(arg).sortAlphabetically().stream();
        }
        return Stream.empty();
    }


    @Override
    public String getShortDesc() {
        return "Позволяет отображать боксы в мире";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "С помощью этой команды можно добавлять/удалять рендер боксов в мире",
                "",
                "Использование:",
                "> box add <Block/Entity> <block/entity> <color> - Добавляет блок/сущность",
                "> box remove <Block/Entity> <block/entity> - Удаляет блок/сущность",
                "> box list <Block/Entity> - Возвращает список блоков/сущностей",
                "> box clear <Block/Entity> - Очищает блоков/сущностей"
        );
    }
}

