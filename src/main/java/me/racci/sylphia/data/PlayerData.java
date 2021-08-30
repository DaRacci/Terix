package me.racci.sylphia.data;

import me.racci.sylphia.Sylphia;
import me.racci.sylphia.origins.enums.specials.Special;
import me.racci.sylphia.enums.punishments.Punishment;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class PlayerData {

	private final Player player;
	private final Sylphia plugin;

	private String origin;
	private String lastOrigin;

	private final Map<Special, Integer> originSettings;

	private final Map<Punishment, Integer> punishments;
	private Boolean unban;

	private Boolean silentmode;

	private boolean saving;
	private boolean shouldSave;

	public PlayerData(Player player, Sylphia plugin) {
		this.player = player;
		this.plugin = plugin;
		this.originSettings = new HashMap<>();
		this.punishments = new HashMap<>();
		this.saving = false;
		this.shouldSave = true;
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

	public Map<Special, Integer> getOriginSettingMap() {
		return originSettings;
	}

	public Boolean isSilentmode() {
		return silentmode;
	}

	public void setSilentmode(Boolean value) {
		this.silentmode = value;
	}

	public Integer getPunishment(Punishment punishment) {
		return punishments.get(punishment);
	}

	public void setPunishments(Punishment punishment, Integer data) {
		punishments.put(punishment, data);
	}

	public Map<Punishment, Integer> getPunishmentsMap() {
		return punishments;
	}

	public Boolean isUnban() {
		return this.unban;
	}

	public void setUnban(Boolean value) {
		this.unban = value;
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
