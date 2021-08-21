package me.racci.sylphia.events;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.racci.sylphia.Sylphia;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@SuppressWarnings("unused")
public class PlayerChatEvent implements Listener {
	Sylphia plugin;

	public PlayerChatEvent(Sylphia plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void Mention(AsyncChatEvent e) {
		Player player = e.getPlayer();
	}
}
