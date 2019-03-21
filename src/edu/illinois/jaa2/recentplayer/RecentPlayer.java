package edu.illinois.jaa2.recentplayer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

public class RecentPlayer extends JavaPlugin {
	
	private PlayerListener playerListener;
	
	@Override
	public void onEnable() {
		// Load the description file to get the plugin's name and version
		PluginDescriptionFile pdfFile = this.getDescription();
		Bukkit.getLogger().info(pdfFile.getName() + " version " + pdfFile.getVersion() + " has been enabled!"); 
		
		Map<UUID, Long> lastPlayers = loadLastPlayers();
		playerListener = new PlayerListener(lastPlayers, getConfig());
		getServer().getPluginManager().registerEvents(playerListener, this);
	}
	
	@Override
	public void onDisable() {
		saveLastPlayers(playerListener.getLastPlayers());
		
		//Load the description file to get the plugin's name and version
		PluginDescriptionFile pdfFile = this.getDescription();
		Bukkit.getLogger().info(pdfFile.getName() + " version " + pdfFile.getVersion() + " has been disabled!");
	}
	
	/**
	 * Saves the players who were last online
	 * @param playerList list of players and their logout times
	 */
	public void saveLastPlayers(Map<UUID, Long> lastPlayers) {
		File lastPlayersFile = new File(getDataFolder(), "lastPlayers.yml");
		FileConfiguration lastPlayersConfig = YamlConfiguration.loadConfiguration(lastPlayersFile);
		
		lastPlayersConfig.set("lastPlayers", null);
		ConfigurationSection playersSection = lastPlayersConfig.createSection("lastPlayers");
		
		List<UUID> players = lastPlayers.keySet().stream().collect(Collectors.toList());
		for (int i = 0; i < players.size(); i++) {
			ConfigurationSection thisPlayerSection = playersSection.createSection(Integer.toString(i));
			thisPlayerSection.set("player", players.get(i).toString());
			thisPlayerSection.set("lastQuit", lastPlayers.get(players.get(i)));
		}
		
		try {
			lastPlayersConfig.save(lastPlayersFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		getLogger().log(Level.INFO, "Saved " + lastPlayers.size() + " last players");
	}
	
	/**
	 * Loads the players who were online last
	 * @return map of OfflinePlayer to the (Long) time he or she was online last
	 */
	public Map<UUID, Long> loadLastPlayers() {
		// Load the YML file with the last players
		File lastPlayersFile = new File(getDataFolder(), "lastPlayers.yml");
		YamlConfiguration lastPlayersConfig = YamlConfiguration.loadConfiguration(lastPlayersFile);

		Map<UUID, Long> lastPlayers = new HashMap<UUID, Long>();
		
		if (lastPlayersConfig.isSet("lastPlayers")) {
			ConfigurationSection lastPlayersSection = lastPlayersConfig.getConfigurationSection("lastPlayers");
			for (String key : lastPlayersSection.getKeys(false)) {
				ConfigurationSection playerSection = lastPlayersSection.getConfigurationSection(key);
				
				if (!playerSection.contains("player")) {
					getLogger().warning("'player' property not found in one of the entries!");
					continue;
				}
				
				try {
					UUID player = UUID.fromString(playerSection.getString("player"));
					Long lastQuitTime = playerSection.getLong("lastQuit", -1);
					
					if (lastQuitTime != -1 && player != null) {
						lastPlayers.put(player, lastQuitTime);
					} else {
						getLogger().info("Uh-oh: last quit time is " + lastQuitTime + " and offline player is " + player);
					}
				} catch (IllegalArgumentException e) {
					getLogger().warning("UUID is not formed properly: " + playerSection.getString("player"));
				}
			}
		} else {
			getLogger().info("Last players not set");
		}
		
		getLogger().log(Level.INFO, "Loaded " + lastPlayers.size() + " players who were on last.");
		return lastPlayers;
	}
}
