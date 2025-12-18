package me.ram.bedwarsscoreboardaddon.addon;

import lombok.Getter;
import org.bukkit.potion.PotionEffectType;

@Getter
public enum RandomEvents {

    PLAYERS_SPEED("全员速度提升", PotionEffectType.SPEED, "§c§l全体90秒速度加成!"),
    PLAYERS_JUMP_BOOST("全员跳跃提升", PotionEffectType.JUMP, "§c§l全体90秒跳跃加成!"),
    PLAYERS_STRENGTH("全员力量提升", PotionEffectType.INCREASE_DAMAGE, "§c§l全体90秒力量加成!");

    private final String eventName;
    private final PotionEffectType effectType;
    private final String subtitle;

    RandomEvents(String eventName, PotionEffectType effectType, String subtitle) {
        this.eventName = eventName;
        this.effectType = effectType;
        this.subtitle = subtitle;
    }
}
