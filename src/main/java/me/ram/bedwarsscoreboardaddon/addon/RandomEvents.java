package me.ram.bedwarsscoreboardaddon.addon;

import lombok.Getter;
import org.bukkit.potion.PotionEffectType;

@Getter
public enum RandomEvents {

    PLAYERS_SPEED("全员速度提升", PotionEffectType.SPEED),
    PLAYERS_JUMP_BOOST("全员跳跃提升", PotionEffectType.JUMP),
    PLAYERS_STRENGTH("全员力量提升", PotionEffectType.INCREASE_DAMAGE);

    private final String eventName;
    private final PotionEffectType effectType;

    RandomEvents(String eventName, PotionEffectType effectType) {
        this.eventName = eventName;
        this.effectType = effectType;
    }

    public String getSubtitle() {
        return "§a§l" + eventName + "!";
    }
}
