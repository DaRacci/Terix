package me.racci.sylphia.enums.punishments;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("unused")
public class PunishmentsRegistry {

    public final Map<String, Punishment> punishments;

    public PunishmentsRegistry() {
        this.punishments = new HashMap<>();
    }

    public void register(String key, Punishment punishments) {
        this.punishments.put(key.toLowerCase(Locale.ROOT), punishments);
    }

    public Collection<Punishment> getPunishments() {
        return punishments.values();
    }

    @Nullable
    public Punishment getPunishment(String key) {
        return this.punishments.get(key.toLowerCase(Locale.ROOT));
    }

}
