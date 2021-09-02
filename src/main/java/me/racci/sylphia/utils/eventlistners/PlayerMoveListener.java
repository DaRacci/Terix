package me.racci.sylphia.utils.eventlistners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMoveListener implements Listener {

	@EventHandler(priority = EventPriority.NORMAL)
	public void onMove(final PlayerMoveEvent event) {

		/* PlayerMoveFullXYZEvent */
		PlayerMoveFullXYZEvent playerMoveFullXYZEvent = null;

		if (
				event.getFrom().getBlockX() != event.getTo().getBlockX() ||
						event.getFrom().getBlockY() != event.getTo().getBlockY() ||
						event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
			playerMoveFullXYZEvent = new PlayerMoveFullXYZEvent(event.getPlayer(), event.getFrom(), event.getTo());

			Bukkit.getPluginManager().callEvent(playerMoveFullXYZEvent);

			playerMoveFullXYZEvent.setCancelled(event.isCancelled());

			if (playerMoveFullXYZEvent.getTo() != event.getTo()) {
				event.setTo(playerMoveFullXYZEvent.getTo());
			}
		}

		/* PlayerMoveXYZEvent */
		PlayerMoveXYZEvent playerMoveXYZEvent = null;

		if (
				event.getFrom().getX() != event.getTo().getX() ||
						event.getFrom().getY() != event.getTo().getY() ||
						event.getFrom().getZ() != event.getTo().getZ()) {
			playerMoveXYZEvent = new PlayerMoveXYZEvent(event.getPlayer(), event.getFrom(), event.getTo());

			Bukkit.getPluginManager().callEvent(playerMoveXYZEvent);

			playerMoveXYZEvent.setCancelled(event.isCancelled());

			if (playerMoveXYZEvent.getTo() != event.getTo()) {
				event.setTo(playerMoveXYZEvent.getTo());
			}
		}

		/* Cancellation */
		boolean isCancelled = event.isCancelled();

		if (playerMoveFullXYZEvent != null && playerMoveFullXYZEvent.isCancelled()) {
			isCancelled = true;
			if (playerMoveXYZEvent != null) playerMoveXYZEvent.setCancelled(true);
		}

		if (playerMoveXYZEvent != null && playerMoveXYZEvent.isCancelled()) {
			isCancelled = true;
			if (playerMoveFullXYZEvent != null) playerMoveFullXYZEvent.setCancelled(true);
		}

		event.setCancelled(isCancelled);
	}
}
