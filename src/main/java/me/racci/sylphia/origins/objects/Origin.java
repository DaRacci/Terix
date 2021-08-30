package me.racci.sylphia.origins.objects;

import me.racci.sylphia.utils.Logger;
import org.apache.commons.collections4.ListUtils;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Field;
import java.util.*;

public class Origin {

	private static final HashMap<Origin, String> requiredPermList = new HashMap<>();
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
			Logger.log(Logger.LogLevel.ERROR, "Could not initialize Origin Map value: " + declaredField.getName() + " " + e);
		}
	}

	private final LinkedHashMap<OriginValue, String> nameMap = new LinkedHashMap<>();
	private final LinkedHashMap<OriginValue, Sound> soundMap = new LinkedHashMap<>();
	private final LinkedHashMap<OriginValue, String> timeMessageMap = new LinkedHashMap<>();
	private final LinkedHashMap<OriginValue, List<String>> permissionMap = new LinkedHashMap<>();
	private final LinkedHashMap<OriginValue, Boolean> effectEnableMap = new LinkedHashMap<>();
	private final LinkedHashMap<OriginValue, Integer> specialMap = new LinkedHashMap<>();
	private final LinkedHashMap<OriginValue, List<PotionEffect>> effectMap = new LinkedHashMap<>();
	private final LinkedHashMap<OriginValue, List<PotionEffect>> conditionEffectMap = new LinkedHashMap<>();
	private final LinkedHashMap<OriginValue, List<OriginAttribute>> attributeMap = new LinkedHashMap<>();
	private final LinkedHashMap<OriginValue, List<OriginAttribute>> conditionAttributeMap = new LinkedHashMap<>();
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

		if(permissionMap.get(OriginValue.PERMISSION_REQUIRED).get(0) != null) {
			requiredPermList.putIfAbsent(this, permissionMap.get(OriginValue.PERMISSION_REQUIRED).get(0));
		}


		constructPotionMap(OriginValue.OD, effectMap.get(OriginValue.GENERAL), effectMap.get(OriginValue.OVERWORLD), effectMap.get(OriginValue.DAY), null);
		constructPotionMap(OriginValue.ON, effectMap.get(OriginValue.GENERAL), effectMap.get(OriginValue.OVERWORLD), effectMap.get(OriginValue.NIGHT), null);
		constructPotionMap(OriginValue.ODW, effectMap.get(OriginValue.GENERAL), effectMap.get(OriginValue.OVERWORLD), effectMap.get(OriginValue.DAY), effectMap.get(OriginValue.WATER));
		constructPotionMap(OriginValue.ODL, effectMap.get(OriginValue.GENERAL), effectMap.get(OriginValue.OVERWORLD), effectMap.get(OriginValue.DAY), effectMap.get(OriginValue.LAVA));
		constructPotionMap(OriginValue.ONW, effectMap.get(OriginValue.GENERAL), effectMap.get(OriginValue.OVERWORLD), effectMap.get(OriginValue.NIGHT), effectMap.get(OriginValue.WATER));
		constructPotionMap(OriginValue.ONL, effectMap.get(OriginValue.GENERAL), effectMap.get(OriginValue.OVERWORLD), effectMap.get(OriginValue.NIGHT), effectMap.get(OriginValue.LAVA));
		constructPotionMap(OriginValue.N, effectMap.get(OriginValue.GENERAL), effectMap.get(OriginValue.NETHER), null, null);
		constructPotionMap(OriginValue.NL, effectMap.get(OriginValue.GENERAL), effectMap.get(OriginValue.NETHER), null, effectMap.get(OriginValue.LAVA));
		constructPotionMap(OriginValue.E, effectMap.get(OriginValue.GENERAL), effectMap.get(OriginValue.END), null, null);
		constructPotionMap(OriginValue.EW, effectMap.get(OriginValue.GENERAL), effectMap.get(OriginValue.END), null, effectMap.get(OriginValue.WATER));
		constructPotionMap(OriginValue.EL, effectMap.get(OriginValue.GENERAL), effectMap.get(OriginValue.END), null, effectMap.get(OriginValue.LAVA));

		constructAttributeMap(OriginValue.OD, attributeMap.get(OriginValue.GENERAL), attributeMap.get(OriginValue.OVERWORLD), attributeMap.get(OriginValue.DAY), null);
		constructAttributeMap(OriginValue.ON, attributeMap.get(OriginValue.GENERAL), attributeMap.get(OriginValue.OVERWORLD), attributeMap.get(OriginValue.NIGHT), null);
		constructAttributeMap(OriginValue.ODW, attributeMap.get(OriginValue.GENERAL), attributeMap.get(OriginValue.OVERWORLD), attributeMap.get(OriginValue.DAY), attributeMap.get(OriginValue.WATER));
		constructAttributeMap(OriginValue.ODL, attributeMap.get(OriginValue.GENERAL), attributeMap.get(OriginValue.OVERWORLD), attributeMap.get(OriginValue.DAY), attributeMap.get(OriginValue.LAVA));
		constructAttributeMap(OriginValue.ONW, attributeMap.get(OriginValue.GENERAL), attributeMap.get(OriginValue.OVERWORLD), attributeMap.get(OriginValue.NIGHT), attributeMap.get(OriginValue.WATER));
		constructAttributeMap(OriginValue.ONL, attributeMap.get(OriginValue.GENERAL), attributeMap.get(OriginValue.OVERWORLD), attributeMap.get(OriginValue.NIGHT), attributeMap.get(OriginValue.LAVA));
		constructAttributeMap(OriginValue.N, attributeMap.get(OriginValue.GENERAL), attributeMap.get(OriginValue.NETHER), null, null);
		constructAttributeMap(OriginValue.NL, attributeMap.get(OriginValue.GENERAL), attributeMap.get(OriginValue.NETHER), null, attributeMap.get(OriginValue.LAVA));
		constructAttributeMap(OriginValue.E, attributeMap.get(OriginValue.GENERAL), attributeMap.get(OriginValue.END), null, null);
		constructAttributeMap(OriginValue.EW, attributeMap.get(OriginValue.GENERAL), attributeMap.get(OriginValue.END), null, attributeMap.get(OriginValue.WATER));
		constructAttributeMap(OriginValue.EL, attributeMap.get(OriginValue.GENERAL), attributeMap.get(OriginValue.END), null, attributeMap.get(OriginValue.LAVA));



	}

	private void constructPotionMap(OriginValue originValue, List<PotionEffect> general, List<PotionEffect> world, List<PotionEffect> time, List<PotionEffect> liquid) {
		List<PotionEffect> list = new ArrayList<>();
		for(PotionEffectType potion : PotionEffectType.values()) {
			Logger.log(Logger.LogLevel.INFO, potion.toString());
			PotionEffect finalPotion = new PotionEffect(Objects.requireNonNull(potion), 1, -1);
			finalPotion = Objects.requireNonNullElse(potionLoop(potion, general, finalPotion), finalPotion);
			finalPotion = Objects.requireNonNullElse(potionLoop(potion, world, finalPotion), finalPotion);
			finalPotion = Objects.requireNonNullElse(potionLoop(potion, time, finalPotion), finalPotion);
			finalPotion = Objects.requireNonNullElse(potionLoop(potion, liquid, finalPotion), finalPotion);
			Logger.log(Logger.LogLevel.INFO, originValue.name() + ": " + finalPotion);
			if(!(finalPotion.getDuration() == 1 && finalPotion.getAmplifier() == -1)) {
				Logger.log(Logger.LogLevel.INFO, "Final" + finalPotion);
				list.add(finalPotion);
			}
		}
		conditionEffectMap.put(originValue, list);
	}
	private void constructAttributeMap(OriginValue originValue, List<OriginAttribute> general, List<OriginAttribute> world, List<OriginAttribute> time, List<OriginAttribute> liquid) {
		List<OriginAttribute> list = new ArrayList<>();
		for(Attribute attribute : Attribute.values()) {
			OriginAttribute finalAttribute = new OriginAttribute(Attribute.GENERIC_FLYING_SPEED, 0.842);
			finalAttribute = Objects.requireNonNullElse(attributeLoop(attribute, general, finalAttribute), finalAttribute);
			finalAttribute = Objects.requireNonNullElse(attributeLoop(attribute, world, finalAttribute), finalAttribute);
			finalAttribute = Objects.requireNonNullElse(attributeLoop(attribute, time, finalAttribute), finalAttribute);
			finalAttribute = Objects.requireNonNullElse(attributeLoop(attribute, liquid, finalAttribute), finalAttribute);
			if(!(finalAttribute.getAttribute() == Attribute.GENERIC_FLYING_SPEED && finalAttribute.getValue() == 0.842)) {
				list.add(finalAttribute);
			}
		}
		conditionAttributeMap.put(originValue, list);
	}

	private PotionEffect potionLoop(PotionEffectType potionType, List<PotionEffect> potionEffects, PotionEffect finalPotion) {
		for(PotionEffect potion : ListUtils.emptyIfNull(potionEffects)) {
			if(potion.getType() == potionType && finalPotion.getAmplifier() < potion.getAmplifier()) {
				return potion;
			}
		}
		return null;
	}
	private OriginAttribute attributeLoop(Attribute attribute, List<OriginAttribute> attributes, OriginAttribute finalAttribute) {
		for(OriginAttribute var1 : ListUtils.emptyIfNull(attributes)) {
			if(var1.getAttribute() == attribute && finalAttribute.getValue() < var1.getValue()) {
				return var1;
			}
		}
		return null;
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

	public Sound getHurt() {
		return soundMap.get(OriginValue.HURT);
	}
	public Sound getDeath() {
		return soundMap.get(OriginValue.DEATH);
	}

	public String getDayTitle() {
		return timeMessageMap.get(OriginValue.DAY_TITLE);
	}
	public String getDaySubtitle() {
		return timeMessageMap.get(OriginValue.DAY_SUBTITLE);
	}
	public String getNightTitle() {
		return timeMessageMap.get(OriginValue.NIGHT_TITLE);
	}
	public String getNightSubtitle() {
		return timeMessageMap.get(OriginValue.NIGHT_SUBTITLE);
	}

	public List<String> getRequiredPermission() {
		return permissionMap.get(OriginValue.PERMISSION_REQUIRED);
	}
	public List<String> getGivenPermission() {
		return permissionMap.get(OriginValue.PERMISSION_GIVEN);
	}

	public Boolean isGeneralEffects() {
		return effectEnableMap.get(OriginValue.GENERAL);
	}
	public Boolean isTimeEffects() {
		return effectEnableMap.get(OriginValue.TIME);
	}
	public Boolean isLiquidEffects() {
		return effectEnableMap.get(OriginValue.LIQUID);
	}
	public Boolean isDimensionEffects() {
		return effectEnableMap.get(OriginValue.DIMENSION);
	}
	public Boolean isEffects(OriginValue originValue) {
		return effectEnableMap.get(originValue);
	}

	public Boolean isSlowFalling() {
		return specialMap.get(OriginValue.SPECIAL_SLOWFALLING) == 1;
	}
	public Boolean isNightVision() {
		return specialMap.get(OriginValue.SPECIAL_NIGHTVISION) == 1;
	}
	public Boolean isJumpBoost() {
		return specialMap.get(OriginValue.SPECIAL_JUMPBOOST) != 0;
	}
	public Boolean isSpecial(OriginValue originValue) {
		return specialMap.get(originValue) >= 1;
	}
	public Integer getJumpBoost() {
		return specialMap.get(OriginValue.SPECIAL_JUMPBOOST);
	}

	public List<PotionEffect> getEffects() {
		return effectMap.get(OriginValue.GENERAL);
	}
	public List<PotionEffect> getDayEffects() {
		return effectMap.get(OriginValue.DAY);
	}
	public List<PotionEffect> getNightEffects() {
		return effectMap.get(OriginValue.NIGHT);
	}
	public List<PotionEffect> getWaterEffects() {
		return effectMap.get(OriginValue.WATER);
	}
	public List<PotionEffect> getLavaEffects() {
		return effectMap.get(OriginValue.LAVA);
	}

	public List<OriginAttribute> getAttributes() {
		return attributeMap.get(OriginValue.GENERAL);
	}
	public List<OriginAttribute> getDayAttributes() {
		return attributeMap.get(OriginValue.DAY);
	}
	public List<OriginAttribute> getNightAttributes() {
		return attributeMap.get(OriginValue.NIGHT);
	}
	public List<OriginAttribute> getWaterAttributes() {
		return attributeMap.get(OriginValue.WATER);
	}
	public List<OriginAttribute> getLavaAttributes() {
		return attributeMap.get(OriginValue.LAVA);
	}

	public List<OriginAttribute> getAttributes(OriginValue originValue) {
		return conditionAttributeMap.get(originValue);
	}
	public List<PotionEffect> getPotions(OriginValue originValue) {
		return conditionEffectMap.get(originValue);
	}


	public Boolean isDamage() {
		return damageEnableMap.get(OriginValue.DAMAGE);
	}
	public Boolean isSun() {
		return damageEnableMap.get(OriginValue.SUN);
	}
	public Boolean isFall() {
		return damageEnableMap.get(OriginValue.FALL);
	}
	public Boolean isRain() {
		return damageEnableMap.get(OriginValue.RAIN);
	}
	public Boolean isWater() {
		return damageEnableMap.get(OriginValue.WATER);
	}
	public Boolean isLava() {
		return damageEnableMap.get(OriginValue.LAVA);
	}

	public Integer getSun() {
		return damageAmountMap.get(OriginValue.SUN);
	}
	public Integer getFall() {
		return damageAmountMap.get(OriginValue.FALL);
	}
	public Integer getRain() {
		return damageAmountMap.get(OriginValue.RAIN);
	}
	public Integer getWater() {
		return damageAmountMap.get(OriginValue.WATER);
	}
	public Integer getLava() {
		return damageAmountMap.get(OriginValue.LAVA);
	}

	public ItemStack getItem() {
		return GUIItem;
	}

	public static Map<Origin, String> originRequiredPermsList() {
		return requiredPermList;
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

	public enum OriginValue {
		NAME,
		COLOUR,
		DISPLAY_NAME,
		BRACKET_NAME,
		HURT,
		DEATH,
		DAY_TITLE,
		DAY_SUBTITLE,
		NIGHT_TITLE,
		NIGHT_SUBTITLE,
		PERMISSION_REQUIRED,
		PERMISSION_GIVEN,

		GENERAL,
		TIME,
		LIQUID,
		DIMENSION,
		DAY,
		NIGHT,
		WATER,
		LAVA,

		OVERWORLD,
		NETHER,
		END,
		SPECIAL_SLOWFALLING,
		SPECIAL_NIGHTVISION,
		SPECIAL_JUMPBOOST,
		DAMAGE,
		SUN,
		FALL,
		RAIN,

		/*
		O = OVERWORLD
		N = NETHER
		E = END
		D = DAY
		N = NIGHT
		W = WATER
		L = LAVA
		*/

		OD,
		ON,
		ODW,
		ODL,
		ONW,
		ONL,
		N,
		NL,
		E,
		EW,
		EL,
	}

}