package ru.zenith.api.system.sound;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.kotopushka.compiler.sdk.annotations.Initialization;
import ru.kotopushka.compiler.sdk.annotations.VMProtect;
import ru.kotopushka.compiler.sdk.enums.VMProtectType;
import ru.zenith.common.QuickImports;
import ru.zenith.common.util.entity.PlayerIntersectionUtil;

@Setter
@Getter
@UtilityClass
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SoundManager implements QuickImports {
    public SoundEvent OPEN_GUI = SoundEvent.of(Identifier.of("minecraft:gui_open"));
    public SoundEvent CLOSE_GUI = SoundEvent.of(Identifier.of("minecraft:gui_close"));
    public SoundEvent ENABLE_MODULE = SoundEvent.of(Identifier.of("minecraft:module_enable"));
    public SoundEvent DISABLE_MODULE = SoundEvent.of(Identifier.of("minecraft:module_disable"));

    @Compile
    public void init() {
        Registry.register(Registries.SOUND_EVENT, OPEN_GUI.id(), OPEN_GUI);
        Registry.register(Registries.SOUND_EVENT, CLOSE_GUI.id(), CLOSE_GUI);
        Registry.register(Registries.SOUND_EVENT, ENABLE_MODULE.id(), ENABLE_MODULE);
        Registry.register(Registries.SOUND_EVENT, DISABLE_MODULE.id(), DISABLE_MODULE);
    }

    public void playSound(SoundEvent sound) {
        playSound(sound, 1, 1);
    }

    public void playSound(SoundEvent sound, float volume, float pitch) {
        if (!PlayerIntersectionUtil.nullCheck()) {
            mc.world.playSound(mc.player, mc.player.getBlockPos(), sound, SoundCategory.BLOCKS, volume, pitch);
        }
    }
}
