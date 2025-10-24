package ru.zenith.mixins;

import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zenith.common.util.world.ServerUtil;

@Mixin(Scoreboard.class)
public class ScoreboardMixin {

    @Inject(method = "removeScoreHolderFromTeam", at = @At("HEAD"), cancellable = true)
    private void onRemoveScoreHolderFromTeam(String playerName, Team team, CallbackInfo ci) {
        Scoreboard scoreboard = (Scoreboard) (Object) this;
        Team currentTeam = scoreboard.getScoreHolderTeam(playerName);
        if (currentTeam != team) {
            ci.cancel();
        }
    }
}