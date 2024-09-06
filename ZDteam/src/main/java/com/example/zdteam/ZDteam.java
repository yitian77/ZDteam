package com.example.zdteam;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ZDteam extends JavaPlugin implements TabCompleter {

    private Map<String, Team> teams = new HashMap<>();
    private Map<UUID, String> playerTeams = new HashMap<>();
    private boolean friendlyFireEnabled = false;
    private int maxTeamSize = 8;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadTeams();
        getCommand("zd").setExecutor(this);
        getCommand("zd").setTabCompleter(this);
        Bukkit.getScheduler().runTask(this, this::updatePlayerTags);
    }

    @Override
    public void onDisable() {
        saveTeams();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "请提供指令！");
            return false;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                handleCreateCommand((Player) sender, args);
                break;
            case "invite":
                handleInviteCommand((Player) sender, args);
                break;
            case "disband":
                handleDisbandCommand((Player) sender);
                break;
            case "kick":
                handleKickCommand((Player) sender, args);
                break;
            case "promote":
                handlePromoteCommand((Player) sender, args);
                break;
            case "demote":
                handleDemoteCommand((Player) sender, args);
                break;
            case "transfer":
                handleTransferCommand((Player) sender, args);
                break;
            case "friendlyfire":
                handleFriendlyFireCommand((Player) sender, args);
                break;
            case "reload":
                handleReloadCommand(sender);
                break;
            case "hp":
                showHelp(sender);
                break;
            case "ck":
                handleCheckCommand((Player) sender);
                break;
            case "exit":
                handleExitCommand((Player) sender);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "无效的指令！");
                return false;
        }

        return true;
    }

    private void handleCreateCommand(Player player, String[] args) {
        if (playerTeams.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "你已经在队伍中，无法创建新的队伍！");
            return;
        }

        if (args.length != 2) {
            player.sendMessage(ChatColor.RED + "请提供队伍名称！");
            return;
        }

        String teamName = args[1];
        if (teams.containsKey(teamName)) {
            player.sendMessage(ChatColor.RED + "队伍名已存在！");
            return;
        }

        int maxSize = getConfig().getInt("team-size." + teamName, getConfig().getInt("default-team-size"));
        Team newTeam = new Team(teamName, player.getUniqueId(), maxSize);
        teams.put(teamName, newTeam);
        playerTeams.put(player.getUniqueId(), teamName);
        player.sendMessage(ChatColor.GREEN + "队伍 " + teamName + " 已成功创建！");

        // 创建队伍时生成配置文件
        File teamFile = new File(getDataFolder(), "teams/" + teamName + ".yml");
        if (!teamFile.exists()) {
            try {
                teamFile.getParentFile().mkdirs();
                teamFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                player.sendMessage(ChatColor.RED + "创建队伍配置文件失败！");
            }
        }
    }


    private void handleInviteCommand(Player player, String[] args) {
        if (!playerTeams.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "你没有队伍，无法邀请玩家！");
            return;
        }

        if (args.length != 2) {
            player.sendMessage(ChatColor.RED + "请指定玩家名！");
            return;
        }

        String teamName = playerTeams.get(player.getUniqueId());
        Team team = teams.get(teamName);
        if (team.isFull()) {
            player.sendMessage(ChatColor.RED + "队伍已满！");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "玩家不在线！");
            return;
        }

        if (playerTeams.containsKey(target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "该玩家已经有队伍了！");
            return;
        }

        team.addMember(target.getUniqueId());
        playerTeams.put(target.getUniqueId(), teamName);
        player.sendMessage(ChatColor.GREEN + "邀请已发送给 " + target.getName() + "！");
        target.sendMessage(ChatColor.GREEN + "你已被邀请加入队伍 " + teamName + "！");
    }

    private void handleDisbandCommand(Player player) {
        if (!playerTeams.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "你没有队伍，无法解散队伍！");
            return;
        }

        String teamName = playerTeams.get(player.getUniqueId());
        Team team = teams.get(teamName);
        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "只有队长才能解散队伍！");
            return;
        }

        for (UUID memberId : team.getMembers()) {
            playerTeams.remove(memberId);
        }
        teams.remove(teamName);
        player.sendMessage(ChatColor.GREEN + "队伍 " + teamName + " 已成功解散！");
    }

    private void handleKickCommand(Player player, String[] args) {
        if (!playerTeams.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "你没有队伍，无法踢出成员！");
            return;
        }

        if (args.length != 2) {
            player.sendMessage(ChatColor.RED + "请指定玩家名！");
            return;
        }

        String teamName = playerTeams.get(player.getUniqueId());
        Team team = teams.get(teamName);
        if (!team.isLeader(player.getUniqueId()) && !team.isViceLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "只有队长或副队长可以踢出成员！");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !team.getMembers().contains(target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "玩家不在你的队伍中！");
            return;
        }

        team.removeMember(target.getUniqueId());
        playerTeams.remove(target.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "玩家 " + target.getName() + " 已被踢出队伍！");
        target.sendMessage(ChatColor.RED + "你已被踢出队伍！");
    }

    private void handlePromoteCommand(Player player, String[] args) {
        if (!playerTeams.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "你没有队伍，无法提升副队长！");
            return;
        }

        if (args.length != 2) {
            player.sendMessage(ChatColor.RED + "请指定玩家名！");
            return;
        }

        String teamName = playerTeams.get(player.getUniqueId());
        Team team = teams.get(teamName);
        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "只有队长才能提升副队长！");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !team.getMembers().contains(target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "玩家不在你的队伍中！");
            return;
        }

        team.promoteToViceLeader(target.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "玩家 " + target.getName() + " 已被提升为副队长！");
        target.sendMessage(ChatColor.GREEN + "你已被提升为副队长！");
    }

    private void handleDemoteCommand(Player player, String[] args) {
        if (!playerTeams.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "你没有队伍，无法降职副队长！");
            return;
        }

        if (args.length != 2) {
            player.sendMessage(ChatColor.RED + "请指定玩家名！");
            return;
        }

        String teamName = playerTeams.get(player.getUniqueId());
        Team team = teams.get(teamName);
        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "只有队长才能降职副队长！");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !team.getViceLeaders().contains(target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "玩家不是副队长！");
            return;
        }

        team.demoteFromViceLeader(target.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "玩家 " + target.getName() + " 已被降职！");
        target.sendMessage(ChatColor.GREEN + "你已被降职！");
    }

    private void handleTransferCommand(Player player, String[] args) {
        if (!playerTeams.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "你没有队伍，无法转让队长！");
            return;
        }

        if (args.length != 2) {
            player.sendMessage(ChatColor.RED + "请指定玩家名！");
            return;
        }

        String teamName = playerTeams.get(player.getUniqueId());
        Team team = teams.get(teamName);
        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "只有队长才能转让队长！");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !team.getMembers().contains(target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "玩家不在你的队伍中！");
            return;
        }

        team.transferLeadership(target.getUniqueId());
        playerTeams.put(target.getUniqueId(), teamName);
        player.sendMessage(ChatColor.GREEN + "队长已成功转让给 " + target.getName() + "！");
        target.sendMessage(ChatColor.GREEN + "你已成为队长！");
    }

    private void handleFriendlyFireCommand(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(ChatColor.RED + "请指定开启或关闭！");
            return;
        }

        if (args[1].equalsIgnoreCase("开启")) {
            friendlyFireEnabled = true;
            player.sendMessage(ChatColor.GREEN + "队伍间友方伤害已开启！");
        } else if (args[1].equalsIgnoreCase("关闭")) {
            friendlyFireEnabled = false;
            player.sendMessage(ChatColor.GREEN + "队伍间友方伤害已关闭！");
        } else {
            player.sendMessage(ChatColor.RED + "无效的参数！");
        }
    }

    private void handleReloadCommand(CommandSender sender) {
        if (!(sender instanceof Player) || sender.hasPermission("zdteam.reload")) {
            reloadConfig();
            loadTeams();
            sender.sendMessage(ChatColor.GREEN + "插件配置已重载！");
        } else {
            sender.sendMessage(ChatColor.RED + "此指令仅限管理员使用！");
        }
    }

    private void handleCheckCommand(Player player) {
        if (!playerTeams.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "你没有队伍！");
            return;
        }

        String teamName = playerTeams.get(player.getUniqueId());
        Team team = teams.get(teamName);

        player.sendMessage(ChatColor.GREEN + "队伍名称: " + teamName);
        player.sendMessage(ChatColor.GREEN + "队伍人数: " + team.getMembers().size() + "/" + team.getMaxSize());
        player.sendMessage(ChatColor.GREEN + "副队长: " + team.getViceLeaders().toString());
    }

    private void handleExitCommand(Player player) {
        if (!playerTeams.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "你不在任何队伍中！");
            return;
        }

        String teamName = playerTeams.get(player.getUniqueId());
        Team team = teams.get(teamName);
        team.removeMember(player.getUniqueId());
        playerTeams.remove(player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "你已退出队伍 " + teamName + "！");
    }

    private void loadTeams() {
        File teamsDir = new File(getDataFolder(), "teams");
        if (!teamsDir.exists()) {
            teamsDir.mkdirs();
            return;
        }

        File[] files = teamsDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            String name = config.getString("name");
            UUID leader = UUID.fromString(config.getString("leader"));
            List<UUID> members = new ArrayList<>();
            List<String> memberStrings = config.getStringList("members");
            for (String member : memberStrings) {
                members.add(UUID.fromString(member));
            }
            int maxSize = config.getInt("maxSize");
            Team team = new Team(name, leader, members, maxSize);
            teams.put(name, team);
            for (UUID member : members) {
                playerTeams.put(member, name);
            }
        }
    }

    private void saveTeams() {
        for (Team team : teams.values()) {
            File teamFile = new File(getDataFolder(), "teams/" + team.getName() + ".yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(teamFile);
            config.set("name", team.getName());
            config.set("leader", team.getLeader().toString());
            List<String> members = new ArrayList<>();
            for (UUID member : team.getMembers()) {
                members.add(member.toString());
            }
            config.set("members", members);
            config.set("maxSize", team.getMaxSize());

            try {
                config.save(teamFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void updatePlayerTags() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (playerTeams.containsKey(player.getUniqueId())) {
                String teamName = playerTeams.get(player.getUniqueId());
                player.setCustomName(ChatColor.GREEN + "[" + teamName + "] " + player.getName());
                player.setCustomNameVisible(true);
            } else {
                player.setCustomName(player.getName());
                player.setCustomNameVisible(true);
            }
        }
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "队伍插件指令帮助：");
        sender.sendMessage(ChatColor.GREEN + "/zd create <队伍名称> <最大成员数> - 创建队伍");
        sender.sendMessage(ChatColor.GREEN + "/zd invite <玩家名> - 邀请玩家");
        sender.sendMessage(ChatColor.GREEN + "/zd disband - 解散队伍");
        sender.sendMessage(ChatColor.GREEN + "/zd kick <玩家名> - 踢出玩家");
        sender.sendMessage(ChatColor.GREEN + "/zd promote <玩家名> - 晋升副队长");
        sender.sendMessage(ChatColor.GREEN + "/zd demote <玩家名> - 降职副队长");
        sender.sendMessage(ChatColor.GREEN + "/zd transfer <玩家名> - 转让队长");
        sender.sendMessage(ChatColor.GREEN + "/zd friendlyfire <开启/关闭> - 设置队伍间友方伤害");
        sender.sendMessage(ChatColor.GREEN + "/zd reload - 重载插件配置");
        sender.sendMessage(ChatColor.GREEN + "/zd hp - 显示帮助信息");
        sender.sendMessage(ChatColor.GREEN + "/zd ck - 查看队伍信息");
        sender.sendMessage(ChatColor.GREEN + "/zd exit - 退出队伍");
    }
    public void loadConfig() {
        FileConfiguration config = getConfig();
        int defaultTeamSize = config.getInt("default-team-size");

        // 从配置中获取队伍人数上限配置，并转换为 Map<String, Integer>
        ConfigurationSection teamSizeSection = config.getConfigurationSection("team-size");
        Map<String, Integer> teamSizeConfig = new HashMap<>();

        if (teamSizeSection != null) {
            teamSizeConfig = teamSizeSection.getKeys(false).stream()
                    .collect(Collectors.toMap(
                            key -> key,
                            key -> teamSizeSection.getInt(key)
                    ));
        }

        // 示例：创建队伍实例
        String teamName = "TeamName"; // 示例
        UUID leaderUUID = UUID.fromString("player-uuid"); // 示例
        int maxSize = teamSizeConfig.getOrDefault(teamName, defaultTeamSize);

        Team team = new Team(teamName, leaderUUID, maxSize);
        // 进一步的初始化代码
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "invite", "disband", "kick", "promote", "demote", "transfer", "friendlyfire", "reload", "hp", "ck", "exit");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("kick") || args[0].equalsIgnoreCase("promote") || args[0].equalsIgnoreCase("demote") || args[0].equalsIgnoreCase("transfer")) {
                List<String> playerNames = new ArrayList<>();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    playerNames.add(player.getName());
                }
                return playerNames;
            } else if (args[0].equalsIgnoreCase("friendlyfire")) {
                return Arrays.asList("开启", "关闭");
            }
        }
        return Collections.emptyList();
    }
}
