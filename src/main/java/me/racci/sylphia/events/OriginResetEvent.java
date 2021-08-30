package me.racci.sylphia.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class OriginResetEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private final Player player;
	private boolean cancelled;

	public OriginResetEvent(Player player) {
		super(true);
		this.player = player;
	}

	public Player getPlayer() {
		return this.player;
	}

	@NotNull
	public HandlerList getHandlers() {
		return handlers;
	}

	public boolean isCancelled() {
		return this.cancelled;
	}

	public void setCancelled(boolean value) {
		this.cancelled = value;
	}
}