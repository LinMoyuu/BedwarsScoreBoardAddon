package me.ram.bedwarsscoreboardaddon.addon.teamshop.upgrades;

import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.Team;

public interface Upgrade {

    UpgradeType getType();

    String getName();

    Game getGame();

    Team getTeam();

    int getLevel();

    void setLevel(int level);

    String getBuyer();

    void setBuyer(String buyer);

    void runUpgrade();
}
