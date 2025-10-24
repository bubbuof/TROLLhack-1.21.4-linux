package ru.zenith.api.system.discord;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.Identifier;
import ru.kotopushka.compiler.sdk.classes.Profile;
import ru.zenith.api.system.discord.utils.*;
import ru.zenith.common.QuickImports;
import ru.zenith.common.util.other.BufferUtil;
import ru.zenith.common.util.other.StringUtil;
import ru.zenith.core.Main;

import java.io.IOException;

@Setter
@Getter
public class DiscordManager implements QuickImports {
    private final DiscordDaemonThread discordDaemonThread = new DiscordDaemonThread();
    private boolean running = true;
    private DiscordInfo info = new DiscordInfo("Unknown","","");
    private Identifier avatarId;

    public void init() {
        DiscordEventHandlers handlers = new DiscordEventHandlers.Builder().ready((user) -> {
            Main.getInstance().getDiscordManager().setInfo(new DiscordInfo(user.username,"https://cdn.discordapp.com/avatars/" + user.userId + "/" + user.avatar + ".png",user.userId));
            DiscordRichPresence richPresence = new DiscordRichPresence.Builder()
                    .setStartTimestamp(System.currentTimeMillis() / 1000)
                    .setDetails("Role: " + StringUtil.getUserRole())
                    .setLargeImage("https://s14.gifyu.com/images/bKm4m.jpg", "UID: " + Profile.getUid())
                    .setButtons(RPCButton.create("Телеграм", "t.me/zovclient"), RPCButton.create("Дискорд", "https://discord.gg/TPdfGKs7B3")).build();
            DiscordRPC.INSTANCE.Discord_UpdatePresence(richPresence);
        }).build();

        DiscordRPC.INSTANCE.Discord_Initialize("1396101952143757353", handlers, true, "");
        discordDaemonThread.start();
    }

    public void stopRPC() {
        DiscordRPC.INSTANCE.Discord_Shutdown();
        this.running = false;
    }

    public void load() throws IOException {
        if (avatarId == null && !info.avatarUrl.isEmpty()) {
            avatarId = BufferUtil.registerDynamicTexture("avatar-", BufferUtil.getHeadFromURL(info.avatarUrl));
        }
    }

    private class DiscordDaemonThread extends Thread {
        @Override
        public void run() {
            this.setName("Discord-RPC");

            try {
                while (Main.getInstance().getDiscordManager().isRunning()) {
                    DiscordRPC.INSTANCE.Discord_RunCallbacks();
                    load();
                    Thread.sleep(15000);
                }
            } catch (Exception exception) {
                stopRPC();
            }
            super.run();
        }
    }


    public record DiscordInfo(String userName, String avatarUrl, String userId) {}
}