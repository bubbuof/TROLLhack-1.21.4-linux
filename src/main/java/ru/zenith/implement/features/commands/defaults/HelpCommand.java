package ru.zenith.implement.features.commands.defaults;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.zenith.core.Main;
import ru.zenith.api.feature.command.Command;
import ru.zenith.api.feature.command.ICommand;
import ru.zenith.api.feature.command.argument.IArgConsumer;
import ru.zenith.api.feature.command.exception.CommandException;
import ru.zenith.api.feature.command.exception.CommandNotFoundException;
import ru.zenith.api.feature.command.helpers.Paginator;
import ru.zenith.api.feature.command.helpers.TabCompleteHelper;
import ru.zenith.implement.features.commands.manager.CommandRepository;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.zenith.api.feature.command.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

public class HelpCommand extends Command {
    Main main;

    protected HelpCommand(Main main) {
        super("help");
        this.main = main;
    }

    @Compile
    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMax(1);

        CommandRepository commandRepository = main.getCommandRepository();
        if (!args.hasAny() || args.is(Integer.class)) {
            Paginator.paginate(
                    args, new Paginator<>(
                            commandRepository.getRegistry().descendingStream()
                                    .filter(command -> !command.hiddenFromHelp())
                                    .collect(Collectors.toList())
                    ),
                    () -> logDirect("Доступные команды в чите:"),
                    command -> {
                        String names = String.join("/", command.getNames());
                        String name = command.getNames().get(0);
                        MutableText shortDescComponent = Text.literal(" - " + command.getShortDesc());
                        shortDescComponent.setStyle(shortDescComponent.getStyle().withColor(Formatting.DARK_GRAY));
                        MutableText namesComponent = Text.literal(names);
                        namesComponent.setStyle(namesComponent.getStyle().withColor(Formatting.WHITE));
                        MutableText hoverComponent = Text.literal("");
                        hoverComponent.setStyle(hoverComponent.getStyle().withColor(Formatting.GRAY));
                        hoverComponent.append(namesComponent);
                        hoverComponent.append("\n" + command.getShortDesc());
                        hoverComponent.append("\n\nНажмите, чтобы просмотреть полную справку о команде");
                        String clickCommand = FORCE_COMMAND_PREFIX + String.format("%s %s", label, command.getNames().get(0));
                        MutableText component = Text.literal(name);
                        component.setStyle(component.getStyle().withColor(Formatting.GRAY));
                        component.append(shortDescComponent);
                        component.setStyle(component.getStyle()
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverComponent))
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, clickCommand)));
                        return component;
                    },
                    FORCE_COMMAND_PREFIX + label
            );
        } else {
            String commandName = args.getString().toLowerCase();
            ICommand command = commandRepository.getCommand(commandName);
            if (command == null) {
                throw new CommandNotFoundException(commandName);
            }
            logDirect("");
            command.getLongDesc().forEach(this::logDirect);
            logDirect("");
            MutableText returnComponent = Text.literal("Нажмите что бы вернуться обратно в меню");
            returnComponent.setStyle(returnComponent.getStyle().withClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    FORCE_COMMAND_PREFIX + label
            )));
            logDirect(returnComponent);
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasExactlyOne()) {
            return new TabCompleteHelper()
                    .addCommands(Main.getInstance().getCommandRepository())
                    .filterPrefix(args.getString())
                    .stream();
        }
        return Stream.empty();
    }

    @Compile
    @Override
    public String getShortDesc() {
        return "Просмотр всех доступных команд";
    }

    @Compile
    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "С помощью этой команды можно просмотреть подробную справочную информацию о том, как использовать определенные команды",
                "",
                "Использование:",
                "> help - Перечисляет все команды и их краткие описания.",
                "> help <command> - Отображение справочной информации по конкретной команде."
        );
    }
}
