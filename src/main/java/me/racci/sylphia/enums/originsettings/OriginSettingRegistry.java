package me.racci.sylphia.enums.originsettings;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("unused")
public class OriginSettingRegistry {

	public final Map<String, OriginSetting> originSettings;

	public OriginSettingRegistry() {
		this.originSettings = new HashMap<>();
	}

	public void register(String key, OriginSetting originSetting) {
		this.originSettings.put(key.toLowerCase(Locale.ROOT), originSetting);
	}

	public Collection<OriginSetting> getOriginSettings() {
		return originSettings.values();
	}

	@Nullable
	public OriginSetting getOriginSetting(String key) {
		return this.originSettings.get(key.toLowerCase(Locale.ROOT));
	}
}
