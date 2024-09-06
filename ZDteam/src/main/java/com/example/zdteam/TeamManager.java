package com.example.zdteam;

import org.bukkit.configuration.file.FileConfiguration;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeamManager {

    private FileConfiguration config;

    public TeamManager(FileConfiguration config) {
        this.config = config;
    }

    public void loadConfig() {
        // 获取默认队伍人数上限
        int defaultTeamSize = config.getInt("default-team-size");

        // 获取每个队伍的人数上限配置
        Map<String, Integer> teamSizeConfig = new HashMap<>();

        ConfigurationSection teamSizeSection = config.getConfigurationSection("team-size");
        if (teamSizeSection != null) {
            for (String key : teamSizeSection.getKeys(false)) {
                int size = teamSizeSection.getInt(key, defaultTeamSize);
                teamSizeConfig.put(key, size);
            }
        } else {
            // 如果没有配置 "team-size" 部分，则使用默认值
            teamSizeConfig.put("default", defaultTeamSize);
        }

        // 示例：如何使用队伍名称获取最大人数
        String teamName = "TeamName"; // 示例队伍名
        UUID leaderUUID = UUID.fromString("player-uuid"); // 示例
        int maxSize = teamSizeConfig.getOrDefault(teamName, defaultTeamSize);

        // 创建队伍实例
        Team team = new Team(teamName, leaderUUID, maxSize);

        // 进一步的初始化代码
    }
}