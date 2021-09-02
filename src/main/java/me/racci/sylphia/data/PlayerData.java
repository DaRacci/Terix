package me.racci.sylphia.data;

import me.racci.sylphia.Sylphia;
import me.racci.sylphia.origins.enums.specials.Special;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class PlayerData {

	private final Player player;
	private final Sylphia plugin;

	private String origin;
	private String lastOrigin;

	private final HashMap<Special, Integer> originSettings;
	private final HashMap<String, Long> cooldownMap;

	private boolean saving;
	private boolean shouldSave;

	public PlayerData(Player player, Sylphia plugin) {
		this.player = player;
		this.plugin = plugin;
		this.cooldownMap = new HashMap<>();
		this.originSettings = new HashMap<>();
		this.saving = false;
		this.shouldSave = true;
	}

	public void createCooldown(String cooldown, Integer length) {
		cooldownMap.put(cooldown + ".start", System.currentTimeMillis() / 50);
		cooldownMap.put(cooldown + ".time", (long) length);
	}
	public void removeCooldown(String cooldown) {
		cooldownMap.remove("cooldown." + cooldown);
	}
	public long getStart(String cooldown) {
		return cooldownMap.getOrDefault(cooldown + ".start", (long) -1);
	}
	public double getCooldown(String cooldown) {
		return cooldownMap.getOrDefault(cooldown + ".time", (long) -1).doubleValue();
	}
	public long getTimeToExpire(String cooldown) {
		return (long) (getStart(cooldown) != -1 ? (getStart(cooldown) - System.currentTimeMillis() / (double)50) + getCooldown(cooldown) : -1);

	}

	public Player getPlayer() {
		return player;
	}
	public Sylphia getPlugin() {
		return plugin;
	}

	public String getOrigin() {
		return this.origin;
	}
	public String getLastOrigin() {
		return this.lastOrigin;
	}
	public void setOrigin(String origin) {
		this.origin = origin;
	}
	public void setLastOrigin(String origin) {
		this.lastOrigin = origin;
	}

	public Integer getOriginSetting(Special originSetting) {
		return originSettings.getOrDefault(originSetting, 1);
	}
	public void setOriginSetting(Special originSetting, Integer value) {
		originSettings.put(originSetting, value);
	}

	public boolean isSaving() {
		return saving;
	}
	public void setSaving(boolean saving) {
		this.saving = saving;
	}
	public boolean shouldNotSave() {
		return !shouldSave;
	}
	public void setShouldSave(boolean shouldSave) {
		this.shouldSave = shouldSave;
	}

}
