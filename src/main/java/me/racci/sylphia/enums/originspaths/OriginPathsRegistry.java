package me.racci.sylphia.enums.originspaths;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("unused")
public class OriginPathsRegistry {

    public final Map<String, OriginPath> punishments;

    public OriginPathsRegistry() {
        this.punishments = new HashMap<>();
    }

    public void register(String key, OriginPath punishments) {
        this.punishments.put(key.toLowerCase(Locale.ROOT), punishments);
    }

    public Collection<OriginPath> getPunishments() {
        return punishments.values();
    }

    @Nullable
    public OriginPath getPunishment(String key) {
        return this.punishments.get(key.toLowerCase(Locale.ROOT));
    }

}
