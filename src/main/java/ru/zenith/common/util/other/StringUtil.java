package ru.zenith.common.util.other;

import lombok.experimental.UtilityClass;
import net.minecraft.client.util.InputUtil;
import ru.kotopushka.compiler.sdk.classes.Profile;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.common.util.entity.PlayerIntersectionUtil;

import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static net.minecraft.client.util.InputUtil.Type.*;

@UtilityClass
public class StringUtil {
    public String randomString(int length) {
        return IntStream.range(0, length)
                .mapToObj(operand -> String.valueOf((char) new Random().nextInt('a', 'z' + 1)))
                .collect(Collectors.joining());
    }

    public String getBindName(int key) {
        if (key < 0) return "N/A";
        return PlayerIntersectionUtil.getKeyType(key).createFromCode(key).getTranslationKey().replace("key.keyboard.", "")
                .replace("key.mouse.", "mouse ").replace(".", " ").toUpperCase();
    }

    public String wrap(String input, int width, int size) {
        String[] words = input.split(" ");
        StringBuilder output = new StringBuilder();
        float lineWidth = 0;
        for (String word : words) {
            float wordWidth = Fonts.getSize(size).getStringWidth(word);
            if (lineWidth + wordWidth > width) {
                output.append("\n");
                lineWidth = 0;
            } else if (lineWidth > 0) {
                output.append(" ");
                lineWidth += Fonts.getSize(size).getStringWidth(" ");
            }
            output.append(word);
            lineWidth += wordWidth;
        }
        return output.toString();
    }

    public String getUserRole() {
        return switch (Profile.getRole()) {
            case "Разработчик" -> "DEVELOPER";
            case "Администратор" -> "ADMIN";
            default -> "USER";
        };
    }

    public String getDuration(int time) {
        int mins = time / 60;
        String sec = String.format("%02d", time % 60);
        return mins + ":" + sec;
    }
}
