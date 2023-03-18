package com.github.pregrafer.RegionInformationReload.Task;

import com.github.pregrafer.RegionInformationReload.Manager.DataManager;
import com.github.pregrafer.RegionInformationReload.Manager.InfoManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BiomeTask extends BukkitRunnable {
    Player player;
    String biome, oldBiome, BCN, oldBCN;
    HashMap<String, String> biomes;
    List<String> biomeInfos;

    public BiomeTask(Player player) {
        this.player = player;
        this.biome = player.getLocation().getBlock().getBiome().name();
        this.biomeInfos = DataManager.getBiomeInfos();
        this.biomes = DataManager.getBiomes();
        this.BCN = this.biomes.get(biome);
    }

    private void updateBiome() {
        oldBiome = biome;
        oldBCN = BCN;
        biome = player.getLocation().getBlock().getBiome().name();
        BCN = biomes.get(biome);
    }

    @Override
    public void run() {
        updateBiome();
        if (DataManager.isBiomeHighAccuracy()) {
            if (biome.equals(oldBiome)) {
                return;
            }
        } else {
            if (biome.equals(oldBiome) || biome.contains(oldBiome) || oldBiome.contains(biome)) {
                return;
            }
        }

        biomes.keySet().forEach(b -> {
            if (b.equalsIgnoreCase(biome)) {
                List<String> biomeInfos = new ArrayList<>(this.biomeInfos);
                biomeInfos.replaceAll(s -> ChatColor.translateAlternateColorCodes('&',
                        s.replace("%newBiome%", BCN).replace("%oldBiome%", oldBCN)));
                new InfoManager(player, biomeInfos).sendInfos();
            }
        });
    }
}
