package me.racci.sylphia.utils.eventlistners;

import co.aikar.taskchain.TaskChain;
import me.racci.sylphia.Sylphia;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class PlayerMoveFullListener implements Listener {
	Sylphia plugin;

	public PlayerMoveFullListener(Sylphia plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void PlayerMoveFullEvent(final PlayerMoveFullXYZEvent event) {
		String eventName = "event";
		TaskChain<?> chain = Sylphia.newSharedChain("AsyncLiquidEnterExitEvent");
		chain
			.async(() -> {
				Block blockFrom = event.getFrom().getBlock();
				Block blockTo = event.getTo().getBlock();

				if (isLiquid(blockFrom) == 0) {
					chain.setTaskData(eventName, switch (isLiquid(blockTo)) {
						case 1 -> new PlayerEnterLiquidEvent(event.getPlayer(), 1, event.getFrom().getBlock(), event.getTo().getBlock());
						case 2 -> new PlayerEnterLiquidEvent(event.getPlayer(), 2, event.getFrom().getBlock(), event.getTo().getBlock());
						default -> null;
					});
				} else if (isLiquid(blockTo) != 1) {
					chain.setTaskData(eventName, switch (isLiquid(blockFrom)) {
						case 1 -> new PlayerExitLiquidEvent(event.getPlayer(), 1,  event.getFrom().getBlock(), event.getTo().getBlock());
						case 2 -> new PlayerExitLiquidEvent(event.getPlayer(), 2,  event.getFrom().getBlock(), event.getTo().getBlock());
						default -> null;
					});
				}
			})
			.sync(() -> {
				Event newEvent = chain.getTaskData(eventName);
				if(newEvent != null) {
					Bukkit.getPluginManager().callEvent(newEvent);
				}
			}).execute();
	}

	private Integer isLiquid(Block block) {
		if(block.getType() == Material.WATER) {
			return 1;
		} else if(block.getType() == Material.LAVA) {
			return 2;
		} else return block.getBlockData() instanceof Waterlogged var1 && var1.isWaterlogged() ? 1:0;
	}
}
