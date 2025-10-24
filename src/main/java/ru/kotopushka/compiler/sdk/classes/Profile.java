package ru.kotopushka.compiler.sdk.classes;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import ru.kotopushka.compiler.sdk.annotations.Exclude;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.kotopushka.compiler.sdk.annotations.VMProtect;
import ru.kotopushka.compiler.sdk.enums.VMProtectType;

@UtilityClass
@VMProtect(type = VMProtectType.ULTRA)
public class Profile {
    @Getter
    public String username = System.getenv("username");
    @Getter
    public int uid = 1;
    @Getter
    public String expire = "2038-06-06";
    @Getter
    public String role = "Разработчик";
}