package ru.zenith.implement.screens.altmanager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
public class AltManager {
    private static AltManager instance;
    private final List<Alt> alts = new ArrayList<>();
    private final Path altsFile;
    private final Gson gson;
    
    private AltManager() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.altsFile = Paths.get("zenith", "alts.json");
        load();
    }
    
    public static AltManager getInstance() {
        if (instance == null) {
            instance = new AltManager();
        }
        return instance;
    }
    
    public void addAlt(Alt alt) {
        if (!alts.contains(alt)) {
            alts.add(alt);
            save();
        }
    }
    
    public void removeAlt(Alt alt) {
        alts.remove(alt);
        save();
    }
    
    public void login(Alt alt) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            // Create session with username and UUID
            Session session = new Session(
                alt.getUsername(),
                java.util.UUID.randomUUID(),
                "",
                Optional.empty(),
                Optional.empty(),
                alt.isPremium() ? Session.AccountType.MSA : Session.AccountType.LEGACY
            );
            
            setSession(mc, session);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void setSession(MinecraftClient mc, Session session) {
        try {
            Field sessionField = MinecraftClient.class.getDeclaredField("session");
            sessionField.setAccessible(true);
            sessionField.set(mc, session);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public String getCurrentUsername() {
        return MinecraftClient.getInstance().getSession().getUsername();
    }
    
    private void load() {
        try {
            if (Files.exists(altsFile)) {
                Reader reader = Files.newBufferedReader(altsFile);
                List<Alt> loadedAlts = gson.fromJson(reader, new TypeToken<List<Alt>>(){}.getType());
                if (loadedAlts != null) {
                    alts.addAll(loadedAlts);
                }
                reader.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void save() {
        try {
            Files.createDirectories(altsFile.getParent());
            Writer writer = Files.newBufferedWriter(altsFile);
            gson.toJson(alts, writer);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
