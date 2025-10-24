/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.zenith.implement.features.commands.defaults;

import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.kotopushka.compiler.sdk.annotations.Initialization;
import ru.kotopushka.compiler.sdk.annotations.VMProtect;
import ru.kotopushka.compiler.sdk.enums.VMProtectType;
import ru.zenith.core.Main;
import ru.zenith.api.feature.command.ICommand;

import java.util.*;

public final class DefaultCommands {


    @Compile
    @Initialization
    public static List<ICommand> createAll() {
        Main main = Main.getInstance();
        List<ICommand> commands = new ArrayList<>(Arrays.asList(
                new BoxESPCommand(main),
                new ConfigCommand(main),
                new MacroCommand(main),
                new HelpCommand(main),
                new BindCommand(main),
                new WayCommand(main),
                new RCTCommand(main),
                new FriendCommand(),
                new PrefixCommand(),
                new DebugCommand()
        ));
        return Collections.unmodifiableList(commands);
    }
}
