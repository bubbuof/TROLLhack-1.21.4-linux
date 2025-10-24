package ru.zenith.api.system.logger.implement;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import ru.zenith.api.system.logger.Logger;
import ru.zenith.common.QuickImports;

import java.util.Arrays;

public class MinecraftLogger implements Logger, QuickImports {
    @Override
    public void log(Object message) {

    }

    @Override
    public void minecraftLog(Text... components) {
        if (mc.player != null) {
            MutableText component = Text.literal("");
            Arrays.asList(components).forEach(component::append);
            mc.inGameHud.getChatHud().addMessage(component);
        }
    }
}
