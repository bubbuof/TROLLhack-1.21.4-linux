package ru.zenith.implement.features.commands.defaults;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.zenith.api.feature.command.Command;
import ru.zenith.api.feature.command.argument.IArgConsumer;
import ru.zenith.api.feature.command.datatypes.*;
import ru.zenith.api.feature.command.exception.CommandException;
import ru.zenith.api.feature.command.helpers.Paginator;
import ru.zenith.api.feature.command.helpers.TabCompleteHelper;
import ru.zenith.api.repository.way.WayRepository;
import ru.zenith.common.QuickImports;
import ru.zenith.core.Main;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static ru.zenith.api.feature.command.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class WayCommand extends Command implements QuickImports {
    final WayRepository wayRepository;

    protected WayCommand(Main main) {
        super("way");
        wayRepository = main.getWayRepository();
    }

    @Compile
    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        String arg = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";
        switch (arg) {
            case "add" -> handleAddWay(args);
            case "remove" -> handleRemoveWay(args);
            case "clear" -> handleClearWays(args);
            case "list" -> handleListWays(label, args);
        }
    }

    @Compile
    private void handleAddWay(IArgConsumer args) throws CommandException {
        args.requireMin(4);
        String name = args.getString();
        int x = args.getArgs().get(0).getAs(Integer.class);
        int y = args.getArgs().get(1).getAs(Integer.class);
        int z = args.getArgs().get(2).getAs(Integer.class);

        if (wayRepository.hasWay(name)) {
            logDirect("Метка с таким именем уже есть в списке!", Formatting.RED);
            return;
        }

        String address = mc.getNetworkHandler() == null ? "vanilla" : mc.getNetworkHandler().getServerInfo() == null ? "vanilla" : mc.getNetworkHandler().getServerInfo().address;
        logDirect("Добавлена метка " + name + ", Координаты:" + " (" + x + ", " + y + ", " + z + "), Сервер: " + address, Formatting.GRAY);
        wayRepository.addWay(name, new BlockPos(x, y, z), address);
    }

    @Compile
    private void handleRemoveWay(IArgConsumer args) throws CommandException {
        args.requireMax(1);
        String name = args.getString();
        if (wayRepository.hasWay(name)) {
            wayRepository.deleteWay(name);
            logDirect(Formatting.GREEN + "Метка " + Formatting.RED + name + Formatting.GREEN + " была успешна удалена!");
        } else logDirect("Метка с названием '" + name + "' не найдена!");
    }

    @Compile
    private void handleListWays(String label, IArgConsumer args) throws CommandException {
        args.requireMax(1);
        Paginator.paginate(args, new Paginator<>(wayRepository.wayList),
                () -> logDirect("Список меток:"),
                way -> Text.literal(Formatting.GRAY + "Название: " + Formatting.RED + way.name())
                        .append(Text.literal(Formatting.GRAY + " Координаты: " + Formatting.WHITE + " (" + way.pos().getX() + ", " + way.pos().getY() + ", " + way.pos().getZ() + ")")
                                .append(Text.literal(Formatting.GRAY + " Сервер: " + Formatting.WHITE + way.server()))), FORCE_COMMAND_PREFIX + label);
    }

    @Compile
    private void handleClearWays(IArgConsumer args) throws CommandException {
        args.requireMax(1);
        wayRepository.clearList();
        logDirect(Formatting.GREEN + "Все метки были удалены.");
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasAny()) {
            String arg = args.getString();
            if (arg.equalsIgnoreCase("remove")) {
                if (args.hasExactlyOne()) return args.tabCompleteDatatype(WayDataType.INSTANCE);
            } else if (arg.equalsIgnoreCase("add")) {
                String string = args.has(5) ? "" : args.has(4) ? "z" : args.has(3) ? "y" : args.has(2) ? "x" : "Название";
                return new TabCompleteHelper().sortAlphabetically().prepend(string).stream();
            } else {
                return new TabCompleteHelper().sortAlphabetically().prepend("add", "remove", "list", "clear").filterPrefix(arg).stream();
            }
        }
        return Stream.empty();
    }


    @Override
    public String getShortDesc() {
        return "Позволяет ставить метки в мире";
    }

    @Compile
    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "С помощью этой команды можно добавлять/удалять метки в мире",
                "",
                "Использование:",
                "> way add <name> <x> <y> <z> - Добавляет метку",
                "> way remove <name> - Удаляет метку",
                "> way list - Возвращает список меток",
                "> way clear - Очищает список меток."
        );
    }
}