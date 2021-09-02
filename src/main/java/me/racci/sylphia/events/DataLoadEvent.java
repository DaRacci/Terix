package me.racci.sylphia.events;

import me.racci.sylphia.data.PlayerData;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class DataLoadEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final PlayerData playerData;

    public DataLoadEvent(PlayerData playerData) {
        this.playerData = playerData;
    }

    public PlayerData getPlayerData() {
        return playerData;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
