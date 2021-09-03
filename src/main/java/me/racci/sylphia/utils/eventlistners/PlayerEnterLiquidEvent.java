package me.racci.sylphia.utils.eventlistners;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerEnterLiquidEvent extends Event {
	/* HandlerList */
	private static final HandlerList HANDLERS = new HandlerList();

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	@NotNull
	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

	/* Event */
	private final Player player;
	private final Integer liquidType;
	private final Block from;
	private final Block to;

	public PlayerEnterLiquidEvent(Player player, Integer liquidType, Block from, Block to) {
		this.player = player;
		this.liquidType = liquidType;
		this.from = from;
		this.to = to;
	}

	public Player getPlayer() {
		return player;
	}
	public Integer getLiquidType() {
		return liquidType;
	}
	public Block getFrom() {
		return from;
	}
	public Block getTo() {
		return to;
	}

}

