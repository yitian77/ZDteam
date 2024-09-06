package com.example.zdteam;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Team {
    private String name;
    private UUID leader;
    private List<UUID> members;
    private List<UUID> viceLeaders;
    private int maxSize;

    // 构造函数: 只使用名称和队长的 UUID
    public Team(String name, UUID leader, int maxSize) {
        this.name = name;
        this.leader = leader;
        this.members = new ArrayList<>();
        this.viceLeaders = new ArrayList<>();
        this.maxSize = maxSize;
        this.members.add(leader); // 将队长添加为成员
    }

    // 构造函数: 使用名称、队长 UUID、副队长列表和最大人数
    public Team(String name, UUID leader, List<UUID> viceLeaders, int maxSize) {
        this(name, leader, maxSize); // 调用上面的构造函数
        this.viceLeaders = viceLeaders != null ? viceLeaders : new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public UUID getLeader() {
        return leader;
    }

    public List<UUID> getMembers() {
        return members;
    }

    public List<UUID> getViceLeaders() {
        return viceLeaders;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public boolean isFull() {
        return members.size() >= maxSize;
    }

    public void addMember(UUID member) {
        if (!isFull()) {
            members.add(member);
        }
    }

    public void removeMember(UUID member) {
        members.remove(member);
    }

    public boolean isLeader(UUID player) {
        return leader.equals(player);
    }

    public boolean isViceLeader(UUID player) {
        return viceLeaders.contains(player);
    }

    public void promoteToViceLeader(UUID player) {
        if (members.contains(player) && !viceLeaders.contains(player)) {
            viceLeaders.add(player);
        }
    }

    public void demoteFromViceLeader(UUID player) {
        viceLeaders.remove(player);
    }

    public void transferLeadership(UUID newLeader) {
        if (members.contains(newLeader)) {
            leader = newLeader;
        }
    }
}
