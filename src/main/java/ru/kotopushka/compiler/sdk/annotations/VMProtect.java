package ru.kotopushka.compiler.sdk.annotations;

import ru.kotopushka.compiler.sdk.enums.VMProtectType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface VMProtect {
    VMProtectType type();
}
