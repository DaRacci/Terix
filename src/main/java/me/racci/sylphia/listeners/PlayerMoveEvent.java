package me.racci.sylphia.listeners;

import me.racci.sylphia.Sylphia;
import me.racci.sylphia.origins.objects.Origin.OriginValue;
import me.racci.sylphia.utils.eventlistners.PlayerEnterLiquidEvent;
import me.racci.sylphia.utils.eventlistners.PlayerExitLiquidEvent;
import me.racci.sylphia.utils.minecraft.WorldTime;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PlayerMoveEvent implements Listener {
	Sylphia plugin;

	public PlayerMoveEvent(Sylphia plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onPlayerEnterLiquid(PlayerEnterLiquidEvent event) {
		Sylphia.newChain().async(() -> {
			Player player = event.getPlayer();
			Integer liquidType = event.getLiquidType();
			OriginValue condition = getCondition(player, liquidType);
			player.sendMessage(condition.toString());
		}).execute();
	}

	@EventHandler
	public void onPlayerExitLiquid(PlayerExitLiquidEvent event) {
		Sylphia.newChain().async(() -> {
			Player player = event.getPlayer();
			OriginValue condition = getCondition(player);
			player.sendMessage(condition.toString());
		}).execute();
	}

	private OriginValue getCondition(Player player) {
		return getCondition(player, 0);
	}

	private OriginValue getCondition(Player player, Integer var1) {
		String var2 = switch(player.getWorld().getEnvironment()) {
			case NORMAL -> "O";
			case NETHER -> "N";
			case THE_END -> "E";
			default -> throw new IllegalStateException("Unexpected value: " + player.getWorld().getEnvironment());
		};
		if(var2.equals("O")) {
			var2 = switch((WorldTime.isDay(player) ? 0 : 1)) {
				case 0 -> "OD";
				case 1 -> "ON";
				default -> throw new IllegalStateException("Unexpected value: " + (WorldTime.isDay(player) ? 0 : 1));
			};
		}
		var2 = switch(var1) {
			case 1 -> var2 + "W";
			case 2 -> var2 + "L";
			default -> var2;
		};
		return OriginValue.valueOf(var2);
	}
}
