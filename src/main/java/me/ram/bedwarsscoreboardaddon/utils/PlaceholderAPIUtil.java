package me.ram.bedwarsscoreboardaddon.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderAPIUtil {

    public static String setPlaceholders(Player player, String text) {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return PlaceholderAPI.setPlaceholders(player, text);
        }
        return text;
    }

    // PAPI
    public static String replacePlaceholdersWithPrefix(String message, String placeholderPrefix, Player targetPlayer) {
        Pattern pattern = Pattern.compile(Pattern.quote(placeholderPrefix) + "([^}]+)}");
        Matcher matcher = pattern.matcher(message);

        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String placeholderContent = matcher.group(1);
            String replacementValue = PlaceholderAPIUtil.setPlaceholders(targetPlayer, placeholderContent);
            matcher.appendReplacement(result, replacementValue);
        }
        matcher.appendTail(result);

        return result.toString();
    }
}
