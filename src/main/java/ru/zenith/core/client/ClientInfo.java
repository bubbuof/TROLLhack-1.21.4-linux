package ru.zenith.core.client;

import ru.kotopushka.compiler.sdk.classes.Profile;
import ru.zenith.common.util.other.StringUtil;

import java.io.File;

public record ClientInfo(String clientName, String userName, String role, File clientDir, File filesDir, File configsDir) implements ClientInfoProvider {

    @Override
    public String getFullInfo() {
        return String.format("Welcome! Client: %s Version: %s Branch: %s", clientName, Profile.getUsername(), StringUtil.getUserRole());
    }
}