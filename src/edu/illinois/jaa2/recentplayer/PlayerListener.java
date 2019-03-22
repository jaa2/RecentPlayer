package edu.illinois.jaa2.recentplayer;

import java.util.*;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.ocpsoft.prettytime.PrettyTime;

public class PlayerListener implements Listener {
	
	private Map<UUID, Long> playerList;
	private Double groupingWindow;
	private long messageMaxPlayers;
	private Locale locale;
	
	public PlayerListener(Map<UUID, Long> playerList, FileConfiguration config) {
		this.playerList = playerList;
		
		// Load config
		groupingWindow = config.getDouble("player-grouping-window", 10L);
		locale = new Locale(config.getString("locale", "en"));
		messageMaxPlayers = config.getInt("message-max-players", 1);
	}
	
	
	/**
	 * Gets the list of last players
	 * @return list of last players
	 */
	public Map<UUID, Long> getLastPlayers() {
		return playerList;
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		addPlayerToList(event.getPlayer());
		event.getPlayer().getServer().getLogger().info("Added " + event.getPlayer().getName() + " to the list");
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		filterPlayerList();
		if (event.getPlayer().getServer().getOnlinePlayers().size() <= messageMaxPlayers && playerList.size() > 0) {
			String playerListStr = "";
			List<String> playerNameList = playerList.keySet().stream()
					.map(playerUUID -> event.getPlayer().getServer().getOfflinePlayer(playerUUID))
                    .filter(player -> player != null)
					.map(OfflinePlayer::getName)
					.collect(Collectors.toList());
			
			if (playerNameList.size() == 1) {
				playerListStr = playerNameList.get(0) + " was";
			} else if (playerNameList.size() >= 2) {
				playerListStr = String.join(", ", playerNameList.subList(0, playerNameList.size() - 1))
						+ (playerNameList.size() > 2 ? "," : "") + " and " + playerNameList.get(playerNameList.size() - 1)
						+ " were";
			}
			
			OptionalLong latestTime = playerList.values().stream().mapToLong(date -> date).max();
			if (latestTime.isPresent()) {
				PrettyTime prettyTime = new PrettyTime(locale);
				event.getPlayer().sendMessage(ChatColor.GREEN + playerListStr + " last online "
                        + prettyTime.format(new Date(latestTime.getAsLong())));
			}
		}
	}
	
	public void addPlayerToList(OfflinePlayer player) {
		Date windowStart = new Date((long) (System.currentTimeMillis() - 1000 * 60 * groupingWindow));
		playerList.keySet().removeIf(offp -> (playerList.get(offp) < windowStart.getTime()));
		playerList.put(player.getUniqueId(), System.currentTimeMillis());
	}
	
	public void filterPlayerList() {
		playerList.keySet().removeIf(playerUUID -> Bukkit.getServer().getOfflinePlayer(playerUUID).isOnline());
	}
}
