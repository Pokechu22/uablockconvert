package com.wasteofplastic.uablockconverter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import com.evilmidget38.UUIDFetcher;

public class UABlockConverter extends JavaPlugin implements Listener {
    File plugins;
    File aSkyBlockConfig;
    File uSkyBlockConfig;
    FileConfiguration aSkyBlockConf;
    FileConfiguration uSkyBlockConf;
    List<String> playerNames = new ArrayList<String>();
    CaseInsensitiveMap players = new CaseInsensitiveMap();
    boolean UUIDflag;
    BukkitTask check;

    Map<String, UUID> response = null;

    @Override
    public void onEnable() {
	// Check to see if USkyBlock is active or ASkyblock
	if (getServer().getPluginManager().isPluginEnabled("uSkyBlock")) {
	    getLogger().severe("uSkyBlock is active - please remove uskyblock.jar from plugins before running this converter.");
	    getServer().getPluginManager().disablePlugin(this);
	}
	if (getServer().getPluginManager().isPluginEnabled("ASkyBlock")) {
	    getLogger().severe("A SkyBlock is active - please remove askyblock.jar from plugins before running this converter.");
	    getServer().getPluginManager().disablePlugin(this);
	}
	// Check that directories exist
	plugins = getDataFolder().getParentFile();
	uSkyBlockConfig = new File(plugins.getPath() + File.separator + "uSkyBlock" + File.separator + "config.yml");
	if (!uSkyBlockConfig.exists()) {
	    getLogger().severe("There appears to be no uSkyBlock folder of config in the plugins folder!");
	    getServer().getPluginManager().disablePlugin(this);
	} else {
	    getLogger().info("Found uSkyBlock config.");
	}
	aSkyBlockConfig = new File(plugins.getPath() + File.separator + "ASkyBlock" + File.separator + "config.yml");
	if (!aSkyBlockConfig.exists()) {
	    getLogger().severe("There appears to be no ASkyBlock folder or config in the plugins folder!");
	    getServer().getPluginManager().disablePlugin(this);
	} else {
	    getLogger().info("Found ASkyBlock config in the plugins folder.");
	}
    }
    @Override
    public void onDisable() {
	getLogger().info("uSkyblock to A Skyblock converter disabled");
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
	// Just do it
	sender.sendMessage(ChatColor.GREEN + "Starting conversion...");
	// Set up configs first
	aSkyBlockConf = YamlConfiguration.loadConfiguration(aSkyBlockConfig);
	uSkyBlockConf = YamlConfiguration.loadConfiguration(uSkyBlockConfig);
	/*  USkyblock config:
	 * options:
  general:
    maxPartySize: 4
    worldName: skyworld
    spawnSize: 150
    cooldownInfo: 30
    cooldownRestart: 600
    biomeChange: 3600
  island:
    schematicName: yourschematicname
    distance: 110
    removeCreaturesByTeleport: false
    height: 150
    chestItems: 79:2 360:1 81:1 327:1 40:1 39:1 361:1 338:1 323:1
    addExtraItems: true
    extraPermissions:
      smallbonus: 4:16 320:5
      mediumbonus: 50:16 327:1
      largebonus: 3:5 12:5
      giantbonus: 2:1 110:1
      extremebonus: 352:8 263:4
      donorbonus: 261:1 262:32 272:1
    protectWithWorldGuard: false
    protectionRange: 105
    allowPvP: deny
    allowIslandLock: true
    useOldIslands: false
    useIslandLevel: true
    useTopTen: true

	 */
	// Chest items
	String chestItems = uSkyBlockConf.getString("options.island.chestItems","");
	getLogger().info("uSkyBlock: Chest items = " + chestItems);
	String aChestItems = "";
	if (!chestItems.isEmpty()) {
	    // Parse
	    String[] items = chestItems.split(" ");
	    for (String item : items){
		//getLogger().info("DEBUG: parsing = " + item);
		String[] split = item.split(":");
		Material material = Material.getMaterial(Integer.valueOf(split[0]));
		if (material != null) {
		    if (aChestItems.isEmpty()) {
			aChestItems = material.toString() + ":" + split[1]; 
		    } else {
			aChestItems = aChestItems + " " + material.toString() + ":" + split[1]; 
		    }
		}
	    }
	    getLogger().info("ASkyBlock: Chest items = " + aChestItems);
	    aSkyBlockConf.set("island.chestItems", aChestItems);
	}
	// World name
	String world = uSkyBlockConf.getString("options.general.worldName","skyworld");
	aSkyBlockConf.set("general.worldName", world );
	// reset wait
	aSkyBlockConf.set("general.resetwait", uSkyBlockConf.getInt("options.general.cooldownRestart",600));
	// distance
	aSkyBlockConf.set("island.distance", uSkyBlockConf.getInt("options.island.distance",110));
	aSkyBlockConf.set("island.protectionRange", uSkyBlockConf.getInt("options.island.protectionRange",105));
	// Height
	int height = uSkyBlockConf.getInt("options.island.height",150);
	aSkyBlockConf.set("general.islandlevel", height);
	// PVP
	aSkyBlockConf.set("island.allowPVP", uSkyBlockConf.getString("options.island.allowPVP","deny"));
	// Teleport mob removal
	aSkyBlockConf.set("general.islandremovemobs", uSkyBlockConf.getBoolean("options.island.removeCreaturesByTeleport",false));
	aSkyBlockConf.set("general.loginremovemobs", uSkyBlockConf.getBoolean("options.island.removeCreaturesByTeleport",false));
	// Max team size
	aSkyBlockConf.set("island.maxteamsize", uSkyBlockConf.getInt("options.general.maxPartySize",4));
	try {
	    aSkyBlockConf.save(aSkyBlockConfig);
	} catch (IOException e1) {
	    // TODO Auto-generated catch block
	    e1.printStackTrace();
	}
	sender.sendMessage(ChatColor.GREEN + "Completed config.yml transfer");

	// Go to the islands folder and see how many there are
	File islandDir = new File(plugins.getPath() + File.separator + "uSkyBlock" + File.separator + "islands");
	if (!islandDir.exists()) {
	    sender.sendMessage(ChatColor.RED + "There is no islands folder in uSkyBlock!");
	    return true;
	}
	int total = islandDir.listFiles().length-2;
	sender.sendMessage("There are " + total + " islands to convert");
	int count = 1;
	// General idea - load all the data, do the name lookups then create the new files

	for (File island : islandDir.listFiles()) {
	    // Ignore the null filename
	    if (!island.getName().equalsIgnoreCase("null.yml") && island.getName().endsWith(".yml") && !island.getName().equalsIgnoreCase(".yml")) {
		String islandName = island.getName().substring(0, island.getName().length() -4);
		if (!islandName.isEmpty()) {
		    sender.sendMessage("Loading island #" + (count++) + " of " + total + " at location " + islandName);
		    // Find out who the owners are of this island
		    YamlConfiguration config = new YamlConfiguration();
		    try {
			config.load(island);
			// Get island info
			// Location
			String[] split = islandName.split(",");
			String islandLocation = world + ":" + split[0] + ":" + height + ":" + split[1];
			// Get island level
			int level = config.getInt("general.level",0);
			// Get the island leader
			String leaderName = config.getString("party.leader","");
			if (!leaderName.isEmpty()) {
			    getLogger().info("Leader is :"+leaderName);
			    // Create this player
			    Players leader = new Players(this,leaderName);
			    leader.setHasIsland(true);
			    leader.setIslandLocation(islandLocation);

			    // Problem - will be recalculated
			    leader.setIslandLevel(level);
			    playerNames.add(leaderName);
			    players.put(leaderName,leader);
			    ConfigurationSection party = config.getConfigurationSection("party.members");
			    // Step through the names on this island
			    for (String name : party.getKeys(false)) {
				//getLogger().info("DEBUG: name in file = " + name);
				if (!name.equals(leaderName) && !name.isEmpty()) {
				    // Team member
				    Players teamMember = new Players(this,name);
				    leader.addTeamMember(name);
				    leader.addTeamMember(leaderName);
				    leader.setTeamLeaderName(leaderName);
				    leader.setTeamIslandLocation(islandLocation);
				    leader.setInTeam(true);
				    teamMember.setTeamLeaderName(leaderName);
				    teamMember.setTeamIslandLocation(islandLocation);
				    teamMember.setInTeam(true);
				    players.put(name,teamMember);
				    playerNames.add(name);
				} 
			    }
			}
		    } catch (FileNotFoundException e) {
			sender.sendMessage(islandName + " suddenly disappeared! Skipping...");
			// TODO Auto-generated catch block
			e.printStackTrace();
		    } catch (IOException e) {
			sender.sendMessage(islandName + " problem! Skipping...");
			// TODO Auto-generated catch block
			e.printStackTrace();
		    } catch (InvalidConfigurationException e) {
			sender.sendMessage(islandName + " YAML is corrupted! Skipping...");
			// TODO Auto-generated catch block
			e.printStackTrace();
		    } catch (Exception e) {
			sender.sendMessage(islandName + " problem! Skipping...");
			// TODO Auto-generated catch block
			e.printStackTrace();
		    }

		}
	    }
	}
	sender.sendMessage(ChatColor.GREEN + "Loaded islands. Now loading home locations of players...");
	// We now have a full list of all player's names and have build player objects and teams. 
	// Next we need to grab all the home locations
	// Go to the islands folder and see how many there are
	File playerDir = new File(plugins.getPath() + File.separator + "uSkyBlock" + File.separator + "players");
	if (!playerDir.exists()) {
	    sender.sendMessage(ChatColor.RED + "There is no players folder in uSkyBlock!");
	    return true;
	}
	int totalPlayers = playerDir.listFiles().length-1;
	sender.sendMessage(ChatColor.DARK_BLUE + "There are " + totalPlayers + " player files to convert");
	int playerCount = 1;
	for (File playerFile : playerDir.listFiles()) {
	    // Only grab yml files
	    String playerFileName = playerFile.getName();
	    if (playerFileName.endsWith(".yml")) {
		String playerName = playerFileName.substring(0, playerFileName.length()-4);
		sender.sendMessage("Loading home for " + playerName + ", player " + (playerCount++) + " of " + totalPlayers);
		if (playerNames.contains(playerName)) {
		    Players thisPlayer = players.get(playerName);
		    YamlConfiguration p = new YamlConfiguration();
		    try {
			p.load(playerFile);
			String hl = world + ":" + p.getInt("player.homeX") + ":" + p.getInt("player.homeY") + ":" + p.getInt("player.homeZ");
			thisPlayer.setHL(hl);
		    } catch (Exception e) {
			sender.sendMessage("Problem with " + playerName + " skipping");
		    }
		}
	    }
	}
	// Now get the UUID's
	sender.sendMessage(ChatColor.GREEN + "Now contacting Mojang to obtain UUID's for players. This could take a while, see console and please wait...");
	// Check for any blank or null names
	if (playerNames.contains(null)) {
	    sender.sendMessage(ChatColor.RED + "null player name found - deleting");
	    playerNames.remove(null);
	}
	final UUIDFetcher fetcher = new UUIDFetcher(playerNames,true);
	UUIDflag = false;
	// Kick off an async task and grab the UUIDs.
	getServer().getScheduler().runTaskAsynchronously(this, new Runnable(){

	    @Override
	    public void run() {
		// Fetch UUID's
		try {
		    response = fetcher.call();
		} catch (Exception e) {
		    getLogger().warning("Exception while running UUIDFetcher");
		    e.printStackTrace();
		}
		UUIDflag = true;
	    }});

	// Kick of a scheduler to check if the UUID results are in yet
	check = getServer().getScheduler().runTaskTimer(this, new Runnable(){
	    @Override
	    public void run() {
		getLogger().info("Checking for name to UUID results");
		// Check to see if UUID has returned
		if (UUIDflag) {
		    getLogger().info("Received!");
		    finish();
		} else {
		    getLogger().info("Waiting...");
		}
	    }}, 20L, 20L);
	return true;
    }
    protected void finish() {
	check.cancel();
	// finishes the conversion
	getLogger().info("Received " + response.size() + " UUID's");
	// Now complete the player objects
	for (String name : response.keySet()) {
	    getLogger().info("Set UUID for " + name);
		players.get(name).setUUID(response.get(name));
	}
	File playerDir = new File(plugins.getPath() + File.separator + "aSkyBlock" + File.separator + "players");
	if (!playerDir.exists()) {
	    playerDir.mkdir();
	}
	// Now save all the players
	for (String name : players.keySet()) {
	    if (players.get(name).getUUID() != null) {
		players.get(name).save(playerDir);
	    } else {
		getLogger().warning(name + " has no UUID. Cannot save this player!");
	    }  
	}
	getLogger().info("***** All Done! *****");
	getLogger().info("Stop server and check that config.yml in askyblock folder is okay");
	getLogger().info("Then copy askyblock.jar to /plugins folder. Remove uaconv.jar and then restart server.");
    }

}