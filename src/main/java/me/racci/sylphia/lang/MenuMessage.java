package me.racci.sylphia.lang;

public enum MenuMessage implements MessageKey {

    //Common
    CLOSE(0),
    ABILITY_LEVELS(0),
    ABILITY_LEVEL_ENTRY(0),
    ABILITY_LEVEL_ENTRY_LOCKED(0),
    STATS_LEVELED(0),
    //Skills Menu
    SKILLS_MENU_TITLE(1),
    YOUR_SKILLS(1),
    YOUR_SKILLS_DESC(1),
    YOUR_SKILLS_HOVER(1),
    YOUR_SKILLS_CLICK(1),
    SKILL_CLICK(1),
    SKILL_LOCKED(1),
    //Stats Menu
    STATS_MENU_TITLE(3),
    PLAYER_STAT_ENTRY(3),
    SKILLS(3),
    ATTACK_DAMAGE(3),
    HP(3),
    LUCK(3),
    XP_GAIN(3),
    MAX_MANA(3),
    INCOMING_DAMAGE(3);
    
    private String path;

    String commonMenu = "menus.common.";
    String skillsMenu = "menus.skills_menu.";
    String levelMenu = "menus.level_progression_menu.";
    String statsMenu = "menus.stats_menu.";
    String unclaimedMenu = "menus.unclaimed_items.";

    MenuMessage(int section) {
        String key = this.name().toLowerCase();
        if (section == 0) {
            this.path = commonMenu + key;
        } else if (section == 1) {
            this.path = skillsMenu + key;
        } else if (section == 2) {
            this.path = levelMenu + key;
        } else if (section == 3) {
            this.path = statsMenu + key;
        } else if (section == 4) {
            this.path = unclaimedMenu + key;
        }
    }

    public String getPath() {
        return path;
    }
}
