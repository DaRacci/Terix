package me.racci.sylphia.events;

import me.racci.sylphia.origins.objects.Origin;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class OriginChangeEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private Player player;
	private Origin oldOrigin;
	private Origin newOrigin;
	private boolean cancelled;

	public OriginChangeEvent(Player player, Origin oldOrigin, Origin newOrigin) {
		super(true);
		this.player = player;
		this.oldOrigin = oldOrigin;
		this.newOrigin = newOrigin;
	}

	public Player getPlayer() {
		return this.player;
	}

	public Origin getOldOrigin() {
		return this.oldOrigin;
	}

	public Origin getNewOrigin() {
		return this.newOrigin;
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
