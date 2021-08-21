package me.racci.sylphia.objects;

import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.List;

@SuppressWarnings("unused")
public class Origin {

	String originName;
	String displayName;
	String bracketName;
	String originColour;

	Sound originHurtSound;
	Sound originDeathSound;

	List<String> originRequiredPermissions;
	List<String> originGivenPermissions;

	boolean originGeneralEnabled;
	boolean originNightDayEnabled;
	boolean originWaterLavaEnabled;

	List<PotionEffect> originEffects;
	List<PotionEffect> originWaterEffects;
	List<PotionEffect> originLavaEffects;
	List<PotionEffect> originDayEffects;
	List<PotionEffect> originNightEffects;

	List<OriginAttribute> originAttributes;
	List<OriginAttribute> originWaterAttributes;
	List<OriginAttribute> originLavaAttributes;
	List<OriginAttribute> originDayAttributes;
	List<OriginAttribute> originNightAttributes;

	boolean originSpecialSlowFalling;
	boolean originSpecialNightVision;
	int originSpecialJumpBoost;

	boolean originDamageEnabled;
	boolean originSunDamageEnabled;
	boolean originFallDamageEnabled;
	boolean originRainDamageEnabled;
	boolean originWaterDamageEnabled;
	boolean originFireDamageEnabled;

	int originSunAmount;
	int originFallAmount;
	int originRainAmount;
	int originWaterAmount;
	int originFireAmount;

	ItemStack originGUIItem;
	String dayMessage;
	String nightMessage;

	public Origin(String var1, String var2, String var3, String var4, Sound var5, Sound var6, List<String> var7, List<String> var8, boolean var9, boolean var10, boolean var11, List<PotionEffect> var12, List<PotionEffect> var13, List<PotionEffect> var14, List<PotionEffect> var15, List<PotionEffect> var16, List<OriginAttribute> var17, List<OriginAttribute> var18, List<OriginAttribute> var19, List<OriginAttribute> var20, List<OriginAttribute> var21, boolean var22, boolean var23, int var24, boolean var25, boolean var26, boolean var27, boolean var28, boolean var29, boolean var30, int var31, int var32, int var33, int var34, int var35, ItemStack var36, String var37, String var38) {
		this.originName = var1;
		this.displayName = var2;
		this.bracketName = var3;
		this.originColour = var4;
		this.originHurtSound = var5;
		this.originDeathSound = var6;
		this.originRequiredPermissions = var7;
		this.originGivenPermissions = var8;
		this.originGeneralEnabled = var9;
		this.originNightDayEnabled = var10;
		this.originWaterLavaEnabled = var11;
		this.originEffects = var12;
		this.originWaterEffects = var13;
		this.originLavaEffects = var14;
		this.originDayEffects = var15;
		this.originNightEffects = var16;
		this.originAttributes = var17;
		this.originWaterAttributes = var18;
		this.originLavaAttributes = var19;
		this.originDayAttributes = var20;
		this.originNightAttributes = var21;
		this.originSpecialSlowFalling = var22;
		this.originSpecialNightVision = var23;
		this.originSpecialJumpBoost = var24;
		this.originDamageEnabled = var25;
		this.originSunDamageEnabled = var26;
		this.originFallDamageEnabled = var27;
		this.originRainDamageEnabled = var28;
		this.originWaterDamageEnabled = var29;
		this.originFireDamageEnabled = var30;
		this.originSunAmount = var31;
		this.originFallAmount = var32;
		this.originRainAmount = var33;
		this.originWaterAmount = var34;
		this.originFireAmount = var35;
		this.originGUIItem = var36;
		this.dayMessage = var37;
		this.nightMessage = var38;
	}

	public String getName() {
		return this.originName;
	}

	public String getDisplayName() {
		return this.displayName;
	}

	public String getBracketName() {
		return this.bracketName;
	}

	public String getColour() {
		return this.originColour;
	}

	public Sound getHurtSound() {
		return this.originHurtSound;
	}

	public Sound getDeathSound() {
		return this.originDeathSound;
	}

	public Boolean isGeneralEnabled() {
		return this.originGeneralEnabled;
	}

	public Boolean isNightDayEnabled() {
		return this.originNightDayEnabled;
	}

	public Boolean isWaterLavaEnabled() {
		return this.originWaterLavaEnabled;
	}

	public List<OriginAttribute> getGeneralAttributes() {
		return this.originAttributes;
	}

	public List<OriginAttribute> getDayAttributes() {
		return this.originDayAttributes;
	}

	public List<OriginAttribute> getNightAttributes() {
		return this.originNightAttributes;
	}

	public List<OriginAttribute> getWaterAttributes() {
		return this.originWaterAttributes;
	}

	public List<OriginAttribute> getLavaAttributes() {
		return this.originLavaAttributes;
	}

	public List<PotionEffect> getGeneralEffects() {
		return this.originEffects;
	}

	public List<PotionEffect> getDayEffects() {
		return this.originDayEffects;
	}

	public List<PotionEffect> getNightEffects() {
		return this.originNightEffects;
	}

	public List<PotionEffect> getWaterEffects() {
		return this.originWaterEffects;
	}

	public List<PotionEffect> getLavaEffects() {
		return this.originLavaEffects;
	}

	public Boolean getSlowFalling() {
		return this.originSpecialSlowFalling;
	}

	public Boolean getNightVision() {
		return this.originSpecialNightVision;
	}

	public Integer getJumpBoost() {
		return this.originSpecialJumpBoost;
	}

	public Boolean isDamageEnabled() {
		return this.originDamageEnabled;
	}

	public Boolean isSunDamageEnabled() {
		return this.originSunDamageEnabled;
	}

	public Boolean isFallDamageEnabled() {
		return this.originFallDamageEnabled;
	}

	public Boolean isRainDamageEnabled() {
		return this.originRainDamageEnabled;
	}

	public Boolean isWaterDamageEnabled() {
		return this.originWaterDamageEnabled;
	}

	public Boolean isLavaDamageEnabled() {
		return this.originFireDamageEnabled;
	}

	public Integer getSunDamage() {
		return this.originSunAmount;
	}

	public Integer getFallDamage() {
		return this.originFallAmount;
	}

	public Integer getRainDamage() {
		return this.originRainAmount;
	}

	public Integer getWaterDamage() {
		return this.originWaterAmount;
	}

	public Integer getLavaDamage() {
		return this.originFireAmount;
	}

	public ItemStack getItem() {
		return this.originGUIItem;
	}

	public String getDayMessage() {
		return this.dayMessage;
	}

	public String getNightMessage() {
		return this.nightMessage;
	}



	public void setName(String var1) {
		this.originName = var1;
	}

	public void setDisplayName(String var1) {
		this.displayName = var1;
	}

	public void setBracketName(String var1) {
		this.bracketName = var1;
	}

	public void setColour(String var1) {
		this.originColour = var1;
	}

	public void setHurtSound(Sound var1) {
		this.originHurtSound = var1;
	}

	public void setDeathSound(Sound var1) {
		this.originDeathSound = var1;
	}

	public void setGeneralEnabled(Boolean var1) {
		this.originGeneralEnabled = var1;
	}

	public void setNightDayEnabled(Boolean var1) {
		this.originNightDayEnabled = var1;
	}

	public void setWaterLavaEnabled(Boolean var1) {
		this.originWaterLavaEnabled = var1;
	}

	public void setGeneralAttributes(List<OriginAttribute> var1) {
		this.originAttributes = var1;
	}

	public void setDayAttributes(List<OriginAttribute> var1) {
		this.originDayAttributes = var1;
	}

	public void setNightAttributes(List<OriginAttribute> var1) {
		this.originNightAttributes = var1;
	}

	public void setWaterAttributes(List<OriginAttribute> var1) {
		this.originWaterAttributes = var1;
	}

	public void setLavaAttributes(List<OriginAttribute> var1) {
		this.originLavaAttributes = var1;
	}

	public void setGeneralEffects(List<PotionEffect> var1) {
		this.originEffects = var1;
	}

	public void setDayEffects(List<PotionEffect> var1) {
		this.originDayEffects = var1;
	}

	public void setNightEffects(List<PotionEffect> var1) {
		this.originNightEffects = var1;
	}

	public void setWaterEffects(List<PotionEffect> var1) {
		this.originWaterEffects = var1;
	}

	public void setLavaEffects(List<PotionEffect> var1) {
		this.originLavaEffects = var1;
	}

	public void setSlowFalling(Boolean var1) {
		this.originSpecialSlowFalling = var1;
	}

	public void setNightVision(Boolean var1) {
		this.originSpecialNightVision = var1;
	}

	public void setJumpBoost(Integer var1) {
		this.originSpecialJumpBoost = var1;
	}

	public void setDamageEnabled(Boolean var1) {
		this.originDamageEnabled = var1;
	}

	public void setSunDamageEnabled(Boolean var1) {
		this.originSunDamageEnabled = var1;
	}

	public void setFallDamageEnabled(Boolean var1) {
		this.originFallDamageEnabled = var1;
	}

	public void setRainDamageEnabled(Boolean var1) {
		this.originRainDamageEnabled = var1;
	}

	public void setWaterDamageEnabled(Boolean var1) {
		this.originWaterDamageEnabled = var1;
	}

	public void setLavaDamageEnabled(Boolean var1) {
		this.originFireDamageEnabled = var1;
	}

	public void setSunDamage(Integer var1) {
		this.originSunAmount = var1;
	}

	public void setFallDamage(Integer var1) {
		this.originFallAmount = var1;
	}

	public void setRainDamage(Integer var1) {
		this.originRainAmount = var1;
	}

	public void setWaterDamage(Integer var1) {
		this.originWaterAmount = var1;
	}

	public void setLavaDamage(Integer var1) {
		this.originFireAmount = var1;
	}

	public void setItem(ItemStack var1) {
		this.originGUIItem = var1;
	}

	public void setDayMessage(String var1) {
		this.dayMessage = var1;
	}

	public void setNightMessage(String var1) {
		this.nightMessage = var1;
	}



















}