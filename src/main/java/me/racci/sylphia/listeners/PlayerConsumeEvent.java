package me.racci.sylphia.listeners;

import me.racci.sylphia.Sylphia;
import me.racci.sylphia.origins.OriginHandler;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public class PlayerConsumeEvent implements Listener {

	Sylphia plugin;
	OriginHandler originHandler;

	public PlayerConsumeEvent(Sylphia plugin) {
		this.plugin = plugin;
		this.originHandler = plugin.getOriginHandler();
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void OnConsume(PlayerItemConsumeEvent event) {
		if(event.isCancelled()) {
			return;
		}

		Player player = event.getPlayer();
		if(event.getItem().getType() == Material.MILK_BUCKET) {
			Sylphia.newChain().delay(20).execute();
			originHandler.setTest(player);
		}
	}
}
