package me.racci.sylphia.listeners;

import me.racci.sylphia.Sylphia;
import org.bukkit.event.Listener;

@SuppressWarnings("unused")
public class PlayerChatEvent implements Listener {
	Sylphia plugin;

	public PlayerChatEvent(Sylphia plugin) {
		this.plugin = plugin;
	}

//	@EventHandler
//	public void Mention(AsyncChatEvent e) {
//
//	}
}
