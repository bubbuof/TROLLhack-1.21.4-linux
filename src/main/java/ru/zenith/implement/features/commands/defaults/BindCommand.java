package ru.zenith.implement.features.commands.defaults;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.kotopushka.compiler.sdk.annotations.VMProtect;
import ru.kotopushka.compiler.sdk.enums.VMProtectType;
import ru.zenith.core.Main;
import ru.zenith.api.feature.command.Command;
import ru.zenith.api.feature.command.argument.IArgConsumer;
import ru.zenith.api.feature.command.datatypes.KeyDataType;
import ru.zenith.api.feature.command.datatypes.ModuleDataType;
import ru.zenith.api.feature.command.exception.CommandException;
import ru.zenith.api.feature.command.helpers.Paginator;
import ru.zenith.api.feature.command.helpers.TabCompleteHelper;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleProvider;
import ru.zenith.api.feature.module.ModuleRepository;
import ru.zenith.common.util.other.StringUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static ru.zenith.api.feature.command.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BindCommand extends Command {
    ModuleProvider moduleProvider;
    ModuleRepository moduleRepository;

    public BindCommand(Main main) {
        super("bind");
        moduleRepository = main.getModuleRepository();
        moduleProvider = main.getModuleProvider();
    }

    @Compile
    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        String action = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";
        switch (action) {
            case "add":
                handleAddBind(args);
                break;
            case "remove":
                handleRemoveBind(args);
                break;
            case "list":
                handleListBinds(args, label);
                break;
            case "clear":
                handleClearBinds(args);
                break;
        }
    }

    @Compile
    @VMProtect(type = VMProtectType.MUTATION)
    private void handleAddBind(IArgConsumer args) throws CommandException {
        args.requireMin(2);
        String moduleName = args.getString();
        int key = args.getDatatypeFor(KeyDataType.INSTANCE).getValue();

        Module module = moduleProvider.get(moduleName);
        module.setKey(key);

        logDirect(Formatting.GREEN +
                "Модуль " + Formatting.RED
                + moduleName + Formatting.GREEN
                + " привязан к кнопке " + Formatting.RED
                + StringUtil.getBindName(key).toLowerCase());
    }

    @Compile
    private void handleRemoveBind(IArgConsumer args) throws CommandException {
        args.requireMax(1);
        String moduleName = args.getString();
        Module module = moduleProvider.get(moduleName);
        module.setKey(GLFW.GLFW_KEY_UNKNOWN);
        logDirect(Formatting.GREEN + "Бинд для модуля " + Formatting.RED + moduleName + Formatting.GREEN + " был успешно удален!");
    }

    @Compile
    private void handleListBinds(IArgConsumer args, String label) throws CommandException {
        args.requireMax(1);
        List<Module> filtredList = moduleRepository.modules()
                .stream()
                .filter(module -> module.getKey() != -1)
                .toList();

        Paginator.paginate(
                args, new Paginator<>(filtredList),
                () -> logDirect("Список модулей:"),
                module -> {
                    String names = module.getName();
                    String keys = StringUtil.getBindName(module.getKey()).toLowerCase();
                    return Text.literal(Formatting.GRAY + "Название: " + Formatting.WHITE + names)
                            .append(Text.literal(Formatting.GRAY + " Клавиша: " + Formatting.WHITE + keys));
                },
                FORCE_COMMAND_PREFIX + label);
    }

    @Compile
    private void handleClearBinds(IArgConsumer args) throws CommandException {
        args.requireMax(1);
        moduleRepository.modules().forEach(function -> function.setKey(GLFW.GLFW_KEY_UNKNOWN));
        logDirect("Все бинды модулей были удалены.", Formatting.GREEN);
    }

    // TODO: Починить табкомплит, не показывает KeyDataType.
    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasExactlyOne()) {
            return new TabCompleteHelper()
                    .sortAlphabetically()
                    .prepend("add", "remove", "list", "clear")
                    .filterPrefix(args.getString())
                    .stream();
        } else {
            String arg = args.getString();
            if (arg.equalsIgnoreCase("add")) {
                if (args.hasExactly(1)) {
                    return args.tabCompleteDatatype(ModuleDataType.INSTANCE);
                } else if (args.hasExactly(2)) {
                    return args.tabCompleteDatatype(KeyDataType.INSTANCE);
                }
            }
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Управление биндами для модулей.";
    }


    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Эта команда позволяет управлять биндами для модулей, которые будут активироваться при нажатии определённых клавиш.",
                "",
                "Использование:",
                "> bind add <module> <key> - Привязывает модуль к указанной клавише.",
                "> bind remove <module> - Удаляет привязку модуля.",
                "> bind list - Показывает список всех текущих биндов модулей.",
                "> bind clear - Удаляет все бинды модулей."
        );
    }
}

