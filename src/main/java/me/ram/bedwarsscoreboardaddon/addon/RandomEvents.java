package me.ram.bedwarsscoreboardaddon.addon;

import io.github.bedwarsrel.events.BedwarsGameOverEvent;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.GameState;
import lombok.Getter;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.utils.Utils;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class RandomEvents {

    public static final RandomEvents PLAYERS_SPEED = new RandomEvents("全员速度提升", PotionEffectType.SPEED, "§4§l全体90秒速度加成!");
    public static final RandomEvents PLAYERS_JUMP_BOOST = new RandomEvents("全员跳跃提升", PotionEffectType.JUMP, "§4§l全体90秒跳跃加成!");
    public static final RandomEvents PLAYERS_STRENGTH = new RandomEvents("全员力量提升", PotionEffectType.INCREASE_DAMAGE, "§4§l全体90秒力量加成!");

    public static List<RandomEvents> getAllEvents() {
        return Arrays.asList(PLAYERS_SPEED, PLAYERS_JUMP_BOOST, PLAYERS_STRENGTH);
    }

    @Getter
    private final Game game;
    @Getter
    private final Arena arena;

    // 事件属性
    @Getter
    private final String eventName;
    @Getter
    private final PotionEffectType effectType;
    @Getter
    private final String subtitle;

    @Getter
    private RandomEvents currentActiveEvent;
    @Getter
    private List<RandomEvents> currentGameEvents;

    private RandomEvents(String eventName, PotionEffectType effectType, String subtitle) {
        this.eventName = eventName;
        this.effectType = effectType;
        this.subtitle = subtitle;
        this.game = null;
        this.arena = null;
        this.currentGameEvents = null;
    }

    public RandomEvents(Arena arena) {
        this.arena = arena;
        this.game = arena.getGame();
        this.eventName = null;
        this.effectType = null;
        this.subtitle = null;
        this.currentActiveEvent = null;

        initializeEventList();
    }

    // 初始化事件列表
    private void initializeEventList() {
        currentGameEvents = new ArrayList<>(getAllEvents());
        Collections.shuffle(currentGameEvents);
        currentActiveEvent = null;
    }

    public boolean is(RandomEvents event) {
        if (event == null || this.eventName == null) return false;
        return this.eventName.equals(event.getEventName());
    }

    public boolean startEvent(RandomEvents event) {
        if (event == null) return false;

        // 结束当前事件
        endCurrentEvent();

        // 设置新事件
        currentActiveEvent = event;

        // 应用事件效果
        applyEventEffects(event);

        arena.addGameTask(new BukkitRunnable() {
            @Override
            public void run() {
                endCurrentEvent();
            }
        }.runTaskLater(Main.getInstance(), 90 * 20L));

        return true;
    }

    // 获取下一个事件
    public Optional<RandomEvents> getNextEvent() {
        if (currentGameEvents == null || currentGameEvents.isEmpty()) {
            return Optional.empty();
        }
        // 返回列表中的第一个事件（即将到来的事件）
        return Optional.of(currentGameEvents.get(0));
    }

    // 获取所有剩余事件
    public List<RandomEvents> getAllRemainingEvents() {
        if (currentGameEvents == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(currentGameEvents);
    }

    // 切换到下一个事件
    public void switchNextEvent() {
        if (currentGameEvents == null || currentGameEvents.isEmpty()) {
            endCurrentEvent();  // 结束当前事件
            return;
        }

        RandomEvents nextEvent = currentGameEvents.remove(0);
        startEvent(nextEvent);

    }

    private void applyEventEffects(RandomEvents event) {
        for (Player player : game.getPlayers()) {
            Utils.sendTitle(player, 0, 60, 0, "", subtitle);
            if (event.getEffectType() != null) {
                player.addPotionEffect(new PotionEffect(event.getEffectType(), 90 * 20, 0));
            }
        }
    }

    // 结束当前事件
    public void endCurrentEvent() {
        if (currentActiveEvent != null) {
            currentActiveEvent = null;
        }
    }

    // 重置所有事件
    public void resetEvents() {
        initializeEventList();
        endCurrentEvent();
    }

    // 根据名称获取事件
    public Optional<RandomEvents> fromName(String name) {
        for (RandomEvents event : getAllEvents()) {
            if (event.getEventName().equalsIgnoreCase(name)) {
                return Optional.of(event);
            }
        }
        return Optional.empty();
    }

    // 检查是否有事件
    public boolean hasActiveEvent() {
        return currentActiveEvent != null;
    }

    // 获取当前事件
    public Optional<RandomEvents> getCurrentEvent() {
        return Optional.ofNullable(currentActiveEvent);
    }

    // 获取当前事件名称
    public Optional<String> getCurrentEventName() {
        return getCurrentEvent().map(RandomEvents::getEventName);
    }

    // 获取当前事件SUBTITLE
    public Optional<String> getCurrentSubtitle() {
        return getCurrentEvent().map(RandomEvents::getSubtitle);
    }

    // 获取剩余事件数量
    public int getRemainingEventCount() {
        return currentGameEvents != null ? currentGameEvents.size() : 0;
    }

    // 获取所有可用事件
    public List<RandomEvents> getAvailableEvents() {
        return getAllEvents();
    }

    // 判断当前事件是否是指定类型
    public boolean isCurrentEvent(RandomEvents event) {
        return currentActiveEvent != null && currentActiveEvent.is(event);
    }

    // 添加自定义事件
    public void addCustomEvent(RandomEvents event) {
        if (currentGameEvents != null && event != null) {
            currentGameEvents.add(event);
        }
    }

    // 移除特定事件
    public boolean removeEvent(RandomEvents event) {
        if (currentGameEvents != null && event != null) {
            return currentGameEvents.remove(event);
        }
        return false;
    }
}