package me.ram.bedwarsscoreboardaddon.addon;

import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.events.BedwarsResourceSpawnEvent;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.ResourceSpawner;
import lombok.Getter;
import me.ram.bedwarsscoreboardaddon.Main;
import me.ram.bedwarsscoreboardaddon.arena.Arena;
import me.ram.bedwarsscoreboardaddon.config.Config;
import me.ram.bedwarsscoreboardaddon.events.BoardAddonResourceUpgradeEvent;
import me.ram.bedwarsscoreboardaddon.utils.ColorUtil;
import me.ram.bedwarsscoreboardaddon.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class ResourceUpgrade {

    @Getter
    private final Game game;
    @Getter
    private final Arena arena;
    private final Map<Material, Integer> interval;
    private final Map<Material, Integer> spawn_time;
    private final Map<String, String> upg_time;
    private final Map<Material, String> levels;
    @Getter
    private final Map<String, Boolean> resourcePointFirstSpawn = new HashMap<>();
    private final Set<String> executedUpgradeStages = new HashSet<>();

    public ResourceUpgrade(Arena arena) {
        this.arena = arena;
        this.game = arena.getGame();
        this.interval = new HashMap<>();
        this.spawn_time = new HashMap<>();
        this.upg_time = new HashMap<>();
        this.levels = new HashMap<>();

        // 初始化每个资源点的等级和初始间隔
        for (ResourceSpawner spawner : game.getResourceSpawners()) {
            for (ItemStack itemStack : spawner.getResources()) {
                levels.put(itemStack.getType(), "I");
                interval.put(itemStack.getType(), spawner.getInterval() / 50);
            }
            Location sloc = spawner.getLocation();
            for (ItemStack itemStack : spawner.getResources()) {
                arena.addGameTask(new BukkitRunnable() {
                    final Location loc = new Location(sloc.getWorld(), sloc.getX(), sloc.getY(), sloc.getZ());
                    int i = 0;

                    @Override
                    public void run() {
                        spawn_time.put(itemStack.getType(), ((i / 20) + 1));
                        if (i <= 0) {
                            i = interval.get(itemStack.getType());
                            int es = 0;
                            for (Entity entity : loc.getWorld().getNearbyEntities(loc, 2, 2, 2)) {
                                if (entity instanceof Item) {
                                    Item item = (Item) entity;
                                    if (item.getItemStack().getType().equals(itemStack.getType())) {
                                        es += item.getItemStack().getAmount();
                                    }
                                }
                            }
                            boolean drop = true;
                            if (Config.resourcelimit_enabled) {
                                for (String[] rl : Config.resourcelimit_limit) {
                                    if (rl[0].equals(itemStack.getType().name()) && es >= Integer.parseInt(rl[1])) {
                                        drop = false;
                                    }
                                }
                            }
                            Block block = loc.getBlock();
                            boolean inchest = block.getType().equals(Material.CHEST) && BedwarsRel.getInstance().getBooleanConfig("spawn-resources-in-chest", true);
                            if (drop || inchest) {
                                BedwarsResourceSpawnEvent event = new BedwarsResourceSpawnEvent(game, loc, itemStack.clone());
                                Bukkit.getPluginManager().callEvent(event);
                                if (!event.isCancelled()) {
                                    if (inchest && spawner.canContainItem(((Chest) block.getState()).getInventory(), itemStack)) {
                                        ((Chest) block.getState()).getInventory().addItem(itemStack.clone());
                                    } else if (drop) {
                                        ConfigurationSection config = Main.getInstance().getConfig().getConfigurationSection("holographic.resource.resources");
                                        String res_name = getResourceName(itemStack.getTypeId());
                                        double i = res_name == null || !config.getBoolean(res_name + ".drop", false) ? 0 : config.getDouble(res_name + ".height", 0.0);
                                        i = i > 0 ? i : 0.325;
                                        Item item = loc.getWorld().dropItem(loc.clone().add(0, i, 0), itemStack);
                                        item.setPickupDelay(0);
                                        Vector vector = item.getVelocity();
                                        vector.multiply(spawner.getSpread());
                                        vector.setY(0);
                                        item.setVelocity(vector);
                                    }
                                }
                            }
                        }
                        i--;
                    }
                }.runTaskTimer(Main.getInstance(), 0L, 1L));
            }
        }
    }

    public void checkResourceUpgrade() {
        FileConfiguration config = Main.getInstance().getConfig();
        for (String rs : config.getConfigurationSection("resourceupgrade").getKeys(false)) {
            if (rs == null || rs.isEmpty()) {
                continue;
            }

            if (executedUpgradeStages.contains(rs)) {
                continue;
            }

            final int gametime = config.getInt("resourceupgrade." + rs + ".gametime");
            final int timeLeft = game.getTimeLeft();

            int remtime = timeLeft - gametime;
            String formatremtime = remtime / 60 + ":" + ((remtime % 60 < 10) ? ("0" + remtime % 60) : (remtime % 60));
            upg_time.put(rs, formatremtime);

            if (timeLeft <= gametime) {
                executedUpgradeStages.add(rs);

                final List<String> upgrade = config.getStringList("resourceupgrade." + rs + ".upgrade");
                final String message = config.getString("resourceupgrade." + rs + ".message");
                final String title = config.getString("resourceupgrade." + rs + ".title");
                final String subtitle = config.getString("resourceupgrade." + rs + ".subtitle");

                BoardAddonResourceUpgradeEvent resourceUpgradeEvent = new BoardAddonResourceUpgradeEvent(game, upgrade);
                Bukkit.getPluginManager().callEvent(resourceUpgradeEvent);
                if (resourceUpgradeEvent.isCancelled()) {
                    continue;
                }

                for (String upg : resourceUpgradeEvent.getUpgrade()) {
                    String[] ary = upg.split(",");
                    if (levels.containsKey(Material.valueOf(ary[0]))) {
                        levels.put(Material.valueOf(ary[0]), getLevel(levels.get(Material.valueOf(ary[0]))));
                        interval.put(Material.valueOf(ary[0]), Integer.valueOf(ary[1]));
                    }
                }

                if (message != null && !message.isEmpty()) {
                    for (Player player : game.getPlayers()) {
                        player.sendMessage(ColorUtil.color(message));
                    }
                }
                if (title != null && !title.isEmpty() || subtitle != null && !subtitle.isEmpty()) {
                    for (Player player : game.getPlayers()) {
                        Utils.sendTitle(player, 0, 60, 20, ColorUtil.color(title), ColorUtil.color(subtitle));
                    }
                }

                PlaySound.playSound(game, Config.play_sound_sound_upgrade);
            }
        }
    }

    public Map<String, String> getUpgTime() {
        return upg_time;
    }

    public Map<Material, Integer> getSpawnTime() {
        return spawn_time;
    }

    public Map<Material, String> getLevel() {
        return levels;
    }

    private String getResourceName(int id) {
        FileConfiguration config = Main.getInstance().getConfig();
        for (String r : Config.holographic_resource) {
            if (id == config.getInt("holographic.resource.resources." + r + ".item", 0)) {
                return r;
            }
        }
        return null;
    }

    private String getLevel(String level) {
        switch (level) {
            case "I":
                return "II";
            case "II":
                return "III";
            case "III":
                return "IV";
            case "IV":
                return "V";
            case "V":
                return "VI";
            case "VI":
                return "VII";
            case "VII":
                return "VIII";
            case "VIII":
                return "IX";
            case "IX":
            case "X":
                return "X";
            default:
                return "I";
        }
    }
}
