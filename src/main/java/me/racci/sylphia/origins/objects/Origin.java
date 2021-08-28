package me.racci.sylphia.origins.objects;

import me.racci.sylphia.utils.Logger;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.lang.reflect.Field;
import java.util.*;

@SuppressWarnings("unused")
public class Origin {



	private static final Map<String, Origin> originMap = new LinkedHashMap<>();

	static {
		loadClassData();
	}

	private static void loadClassData() {
		Arrays.stream(Origin.class.getDeclaredFields())
				.filter(declaredField -> declaredField.getType() == Origin.class)
				.forEach(Origin::putInMap);
	}

	private static void putInMap(Field declaredField) {
		try {
			originMap.putIfAbsent(declaredField.getName(), (Origin) declaredField.get(null));
		} catch (IllegalAccessException e) {
			Logger.log(Logger.LogLevel.ERROR, "Could not initialize Colour Map value: " + declaredField.getName() + " " + e);
		}
	}

	private final LinkedHashMap<OriginValue, String> nameMap = new LinkedHashMap<>();
	private final LinkedHashMap<OriginValue, Sound> soundMap = new LinkedHashMap<>();
	private final LinkedHashMap<OriginValue, String> timeMessageMap = new LinkedHashMap<>();
	private final LinkedHashMap<OriginValue, List<String>> permissionMap = new LinkedHashMap<>();
	private final LinkedHashMap<OriginValue, Boolean> effectEnableMap = new LinkedHashMap<>();
	private final LinkedHashMap<OriginValue, Integer> specialMap = new LinkedHashMap<>();
	private final LinkedHashMap<OriginValue, List<PotionEffect>> effectMap = new LinkedHashMap<>();
	private final LinkedHashMap<OriginValue, List<OriginAttribute>> attributeMap = new LinkedHashMap<>();
	private final LinkedHashMap<OriginValue, Boolean> damageEnableMap = new LinkedHashMap<>();
	private final LinkedHashMap<OriginValue, Integer> damageAmountMap = new LinkedHashMap<>();
	private final ItemStack GUIItem;


	public Origin(Map<OriginValue, String> nameMap,
	              Map<OriginValue, Sound> soundMap,
	              Map<OriginValue, String> timeMessageMap,
	              Map<OriginValue, List<String>> permissionMap,
	              Map<OriginValue, Boolean> effectEnableMap,
	              Map<OriginValue, Integer> specialMap,
	              Map<OriginValue, List<PotionEffect>> effectMap,
	              Map<OriginValue, List<OriginAttribute>> attributeMap,
	              Map<OriginValue, Boolean> damageEnableMap,
	              Map<OriginValue, Integer> damageAmountMap,
	              ItemStack GUIItem)    {

		this.nameMap.putAll(nameMap);
		this.soundMap.putAll(soundMap);
		this.timeMessageMap.putAll(timeMessageMap);
		this.permissionMap.putAll(permissionMap);
		this.effectEnableMap.putAll(effectEnableMap);
		this.specialMap.putAll(specialMap);
		this.effectMap.putAll(effectMap);
		this.attributeMap.putAll(attributeMap);
		this.damageEnableMap.putAll(damageEnableMap);
		this.damageAmountMap.putAll(damageAmountMap);
		this.GUIItem = GUIItem;
	}

	public String getName() {
		return nameMap.get(OriginValue.NAME);
	}
	public String getColour() {
		return nameMap.get(OriginValue.COLOUR);
	}
	public String getDisplayName() {
		return nameMap.get(OriginValue.DISPLAY_NAME);
	}
	public String getBracketName() {
		return nameMap.get(OriginValue.BRACKET_NAME);
	}

	public Sound getHurtSound() {
		return soundMap.get(OriginValue.HURT_SOUND);
	}
	public Sound getDeathSound() {
		return soundMap.get(OriginValue.DEATH_SOUND);
	}

	public String getDayMessage() {
		return timeMessageMap.get(OriginValue.DAY_MESSAGE);
	}
	public String getNightMessage() {
		return timeMessageMap.get(OriginValue.NIGHT_MESSAGE);
	}

	public List<String> getRequiredPermission() {
		return permissionMap.get(OriginValue.PERMISSION_REQUIRED);
	}
	public List<String> getGivenPermission() {
		return permissionMap.get(OriginValue.PERMISSION_GIVEN);
	}

	public Boolean isGeneralEffects() {
		return effectEnableMap.get(OriginValue.GENERAL_ENABLED);
	}
	public Boolean isTimeEffects() {
		return effectEnableMap.get(OriginValue.TIME_ENABLED);
	}
	public Boolean isLiquidEffects() {
		return effectEnableMap.get(OriginValue.LIQUID_ENABLED);
	}

	public Boolean isSlowFalling() {
		return specialMap.get(OriginValue.SPECIAL_SLOWFALLING) == 1;
	}
	public Boolean isNightVision() {
		return specialMap.get(OriginValue.SPECIAL_NIGHTVISION) == 1;
	}
	public Boolean isJumpBoost() {
		return specialMap.get(OriginValue.SPECIAL_JUMPBOOST) == 1;
	}
	public Integer getJumpBoost() {
		return specialMap.get(OriginValue.SPECIAL_JUMPBOOST);
	}

	public List<PotionEffect> getEffects() {
		return effectMap.get(OriginValue.GENERAL_EFFECTS);
	}
	public List<PotionEffect> getDayEffects() {
		return effectMap.get(OriginValue.DAY_EFFECTS);
	}
	public List<PotionEffect> getNightEffects() {
		return effectMap.get(OriginValue.NIGHT_EFFECTS);
	}
	public List<PotionEffect> getWaterEffects() {
		return effectMap.get(OriginValue.WATER_EFFECTS);
	}
	public List<PotionEffect> getLavaEffects() {
		return effectMap.get(OriginValue.LAVA_EFFECTS);
	}

	public List<OriginAttribute> getAttributes() {
		return attributeMap.get(OriginValue.GENERAL_ATTRIBUTES);
	}
	public List<OriginAttribute> getDayAttributes() {
		return attributeMap.get(OriginValue.DAY_ATTRIBUTES);
	}
	public List<OriginAttribute> getNightAttributes() {
		return attributeMap.get(OriginValue.NIGHT_ATTRIBUTES);
	}
	public List<OriginAttribute> getWaterAttributes() {
		return attributeMap.get(OriginValue.WATER_ATTRIBUTES);
	}
	public List<OriginAttribute> getLavaAttributes() {
		return attributeMap.get(OriginValue.LAVA_ATTRIBUTES);
	}

	public Boolean isDamage() {
		return damageEnableMap.get(OriginValue.DAMAGE_ENABLED);
	}
	public Boolean isSun() {
		return damageEnableMap.get(OriginValue.SUN_ENABLED);
	}
	public Boolean isFall() {
		return damageEnableMap.get(OriginValue.FALL_ENABLED);
	}
	public Boolean isRain() {
		return damageEnableMap.get(OriginValue.RAIN_ENABLED);
	}
	public Boolean isWater() {
		return damageEnableMap.get(OriginValue.WATER_ENABLED);
	}
	public Boolean isLava() {
		return damageEnableMap.get(OriginValue.LAVA_ENABLED);
	}

	public Integer getSun() {
		return damageAmountMap.get(OriginValue.SUN_AMOUNT);
	}
	public Integer getFall() {
		return damageAmountMap.get(OriginValue.FALL_AMOUNT);
	}
	public Integer getRain() {
		return damageAmountMap.get(OriginValue.RAIN_AMOUNT);
	}
	public Integer getWater() {
		return damageAmountMap.get(OriginValue.WATER_AMOUNT);
	}
	public Integer getLava() {
		return damageAmountMap.get(OriginValue.LAVA_AMOUNT);
	}

	public ItemStack getItem() {
		return GUIItem;
	}




	public static Origin valueOf(String name) {
		Origin origin = originMap.get(name);
		if (origin == null) {
			throw new IllegalArgumentException("No Origin by the name " + name + " found");
		}
		return origin;
	}

	public static Origin[] values() {
		return originMap.values().toArray(Origin[]::new).clone();
	}

	public Map<OriginValue, String> getNameMap() {
		return nameMap;
	}

	public Map<OriginValue, Sound> getSoundMap() {
		return soundMap;
	}

	public Map<OriginValue, String> getTimeMessageMap() {
		return timeMessageMap;
	}

	public Map<OriginValue, List<String>> getPermissionMap() {
		return permissionMap;
	}

	public Map<OriginValue, Boolean> getEffectEnableMap() {
		return effectEnableMap;
	}

	public Map<OriginValue, Integer> getSpecialMap() {
		return specialMap;
	}

	public Map<OriginValue, List<PotionEffect>> getEffectMap() {
		return effectMap;
	}

	public Map<OriginValue, List<OriginAttribute>> getAttributeMap() {
		return attributeMap;
	}

	public Map<OriginValue, Boolean> getDamageEnableMap() {
		return damageEnableMap;
	}

	public Map<OriginValue, Integer> getDamageAmountMap() {
		return damageAmountMap;
	}

	public enum OriginValue {
		NAME,
		COLOUR,
		DISPLAY_NAME,
		BRACKET_NAME,
		HURT_SOUND,
		DEATH_SOUND,
		DAY_MESSAGE,
		NIGHT_MESSAGE,
		PERMISSION_REQUIRED,
		PERMISSION_GIVEN,
		GENERAL_ENABLED,
		TIME_ENABLED,
		LIQUID_ENABLED,
		GENERAL_EFFECTS,
		GENERAL_ATTRIBUTES,
		DAY_EFFECTS,
		DAY_ATTRIBUTES,
		NIGHT_EFFECTS,
		NIGHT_ATTRIBUTES,
		WATER_EFFECTS,
		WATER_ATTRIBUTES,
		LAVA_EFFECTS,
		LAVA_ATTRIBUTES,
		SPECIAL_SLOWFALLING,
		SPECIAL_NIGHTVISION,
		SPECIAL_JUMPBOOST,
		DAMAGE_ENABLED,
		SUN_ENABLED,
		FALL_ENABLED,
		RAIN_ENABLED,
		WATER_ENABLED,
		LAVA_ENABLED,
		SUN_AMOUNT,
		FALL_AMOUNT,
		RAIN_AMOUNT,
		WATER_AMOUNT,
		LAVA_AMOUNT,
	}

}