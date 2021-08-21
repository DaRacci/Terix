package me.racci.sylphia.leaderboard;

import me.racci.sylphia.enums.origins.Origin;

import java.util.*;

public class LeaderboardManager {

    private final Map<Origin, List<SkillValue>> skillLeaderboards;
    private List<SkillValue> powerLeaderboard;
    private List<SkillValue> averageLeaderboard;
    private volatile boolean sorting = false;

    public LeaderboardManager() {
        this.skillLeaderboards = new HashMap<>();
        this.powerLeaderboard = new ArrayList<>();
        this.averageLeaderboard = new ArrayList<>();
    }

    public List<SkillValue> getLeaderboard(Origin origin) {
        return skillLeaderboards.get(origin);
    }

    public void setLeaderboard(Origin origin, List<SkillValue> leaderboard) {
        this.skillLeaderboards.put(origin, leaderboard);
    }

    public List<SkillValue> getLeaderboard(Origin origin, int page, int numPerPage) {
        List<SkillValue> leaderboard = skillLeaderboards.get(origin);
        int from = (Math.max(page, 1) - 1) * numPerPage;
        int to = from + numPerPage;
        return leaderboard.subList(Math.min(from, leaderboard.size()), Math.min(to, leaderboard.size()));
    }

    public List<SkillValue> getPowerLeaderboard() {
        return powerLeaderboard;
    }

    public List<SkillValue> getPowerLeaderboard(int page, int numPerPage) {
        int from = (Math.max(page, 1) - 1) * numPerPage;
        int to = from + numPerPage;
        return powerLeaderboard.subList(Math.min(from, powerLeaderboard.size()), Math.min(to, powerLeaderboard.size()));
    }

    public void setPowerLeaderboard(List<SkillValue> leaderboard) {
        this.powerLeaderboard = leaderboard;
    }

    public List<SkillValue> getAverageLeaderboard() {
        return averageLeaderboard;
    }

    public List<SkillValue> getAverageLeaderboard(int page, int numPerPage) {
        int from = (Math.max(page, 1) - 1) * numPerPage;
        int to = from + numPerPage;
        return averageLeaderboard.subList(Math.min(from, averageLeaderboard.size()), Math.min(to, averageLeaderboard.size()));
    }

    public void setAverageLeaderboard(List<SkillValue> leaderboard) {
        this.averageLeaderboard = leaderboard;
    }

    public int getSkillRank(Origin origin, UUID id) {
        List<SkillValue> leaderboard = skillLeaderboards.get(origin);
        for (SkillValue skillValue : leaderboard) {
            if (skillValue.getId().equals(id)) {
                return leaderboard.indexOf(skillValue) + 1;
            }
        }
        return 0;
    }

    public int getPowerRank(UUID id) {
        for (SkillValue skillValue : powerLeaderboard) {
            if (skillValue.getId().equals(id)) {
                return powerLeaderboard.indexOf(skillValue) + 1;
            }
        }
        return 0;
    }

    public int getAverageRank(UUID id) {
        for (SkillValue skillValue : averageLeaderboard) {
            if (skillValue.getId().equals(id)) {
                return averageLeaderboard.indexOf(skillValue) + 1;
            }
        }
        return 0;
    }

    public boolean isNotSorting() {
        return !sorting;
    }

    public void setSorting(boolean sorting) {
        this.sorting = sorting;
    }

}
