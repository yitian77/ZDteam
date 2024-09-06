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

    // 构造函数
    public Team(String name, UUID leader, List<UUID> members, int maxSize) {
        this.name = name;
        this.leader = leader;
        this.members = members;
        this.maxSize = maxSize;
        this.viceLeaders = new ArrayList<>();
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

    public boolean isLeader(UUID playerId) {
        return leader.equals(playerId);
    }

    public boolean isViceLeader(UUID playerId) {
        return viceLeaders.contains(playerId);
    }

    public void addMember(UUID playerId) {
        if (!members.contains(playerId)) {
            members.add(playerId);
        }
    }

    public void removeMember(UUID playerId) {
        members.remove(playerId);
    }

    public void promoteToViceLeader(UUID playerId) {
        if (!viceLeaders.contains(playerId)) {
            viceLeaders.add(playerId);
        }
    }

    public void demoteFromViceLeader(UUID playerId) {
        viceLeaders.remove(playerId);
    }

    public void transferLeadership(UUID newLeaderId) {
        leader = newLeaderId;
    }
}
