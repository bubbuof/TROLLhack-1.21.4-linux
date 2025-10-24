package ru.zenith.implement.screens.altmanager;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Alt {
    private String username;
    private String email;
    private boolean premium;
    
    public Alt(String username) {
        this.username = username;
        this.email = "";
        this.premium = false;
    }
    
    public String getDisplayName() {
        return username + (premium ? " §a[Premium]" : " §7[Cracked]");
    }
}
