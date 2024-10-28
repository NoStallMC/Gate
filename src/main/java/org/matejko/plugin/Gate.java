package main.java.org.matejko.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Gate extends JavaPlugin implements Listener {

    private Location corner1;
    private Location corner2;
    private Location gateDestination;
    private String gateId = "default";  // Default Gate ID, will be updated when /tohere is run

    private List<GateData> gates = new ArrayList<>();
    private Logger logger;

    @Override
    public void onEnable() {
        this.logger = Logger.getLogger("Gate");
        this.logger.info("Gate enabled!");
        Bukkit.getPluginManager().registerEvents(this, this);
        getDataFolder().mkdirs();
        loadGatesFromFile();
        logger.info("Gates loaded: " + gates.size());
    }

    @Override
    public void onDisable() {
        this.logger.info("Gate disabled.");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInHand();
        if (itemInHand.getTypeId() == 290) {  // Wooden Hoe ID is 290
            if (event.getAction().toString().contains("LEFT_CLICK")) {
                if (corner1 == null) {
                    Location clickedBlockLocation = event.getClickedBlock().getLocation();
                    corner1 = clickedBlockLocation;
                    player.sendMessage("Corner 1 set at: " + formatLocation(corner1));
                } else {
                    Location clickedBlockLocation = event.getClickedBlock().getLocation();
                    corner1 = clickedBlockLocation;
                    player.sendMessage("Corner 1 overwritten at: " + formatLocation(corner1));
                }
            } else if (event.getAction().toString().contains("RIGHT_CLICK")) {
                if (corner2 == null) {
                    Location clickedBlockLocation = event.getClickedBlock().getLocation();
                    corner2 = clickedBlockLocation;
                    player.sendMessage("Corner 2 set at: " + formatLocation(corner2));
                } else {
                    Location clickedBlockLocation = event.getClickedBlock().getLocation();
                    corner2 = clickedBlockLocation;
                    player.sendMessage("Corner 2 overwritten at: " + formatLocation(corner2));
                }
            }
        }
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("tohere")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command.");
                return false;
            }

            Player player = (Player) sender;
            if (!player.isOp()) {
                player.sendMessage("You do not have permission to use this command.");
                return false;
            }

            if (args.length > 0) {
                gateId = args[0];
            } else {
                player.sendMessage("Please provide a name for the gate. Usage: /tohere <name>");
                return false;
            }

            if (corner1 == null || corner2 == null) {
                sender.sendMessage("Both corners must be set first using the wooden hoe.");
                return false;
            }

            if (isGateIdDuplicate(gateId)) {
                sender.sendMessage(ChatColor.RED + "Error: Gate with ID \"" + gateId + "\" already exists!");
                return false;
            }

            gateDestination = player.getLocation();
            logger.info("Gate destination set at: " + formatLocation(gateDestination));

            player.sendMessage("Gate destination set at: " + formatLocation(gateDestination) + " with Gate ID: " + gateId);

            gates.add(new GateData(gateId, corner1, corner2, gateDestination));
            saveGatesToFile();
            resetGateData();

            return true;
        } else if (cmd.getName().equalsIgnoreCase("delgate")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command.");
                return false;
            }

            Player player = (Player) sender;
            if (!player.isOp()) {
                player.sendMessage("You do not have permission to use this command.");
                return false;
            }

            if (args.length == 0) {
                player.sendMessage("Please provide a Gate ID to delete. Usage: /delgate <gateId>");
                return false;
            }

            String gateIdToDelete = args[0];
            GateData gateToDelete = getGateById(gateIdToDelete);
            if (gateToDelete == null) {
                player.sendMessage(ChatColor.RED + "Error: Gate with ID \"" + gateIdToDelete + "\" does not exist.");
                return false;
            }

            gates.remove(gateToDelete);
            saveGatesToFile();

            player.sendMessage(ChatColor.GREEN + "Successfully deleted gate with ID \"" + gateIdToDelete + "\".");

            return true;
        }

        return false;
    }

    private boolean isGateIdDuplicate(String gateId) {
        File gateFile = new File(getDataFolder(), "gates.txt");
        if (!gateFile.exists()) {
            return false;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(gateFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Gate ID: " + gateId)) {
                    return true;
                }
            }
        } catch (IOException e) {
            logger.warning("Failed to read gates.txt to check for duplicate Gate ID.");
        }

        return false;
    }

    private GateData getGateById(String gateId) {
        for (GateData gate : gates) {
            if (gate.getGateId().equalsIgnoreCase(gateId)) {
                return gate;
            }
        }
        return null;
    }

    private void resetGateData() {
        corner1 = null;
        corner2 = null;
        gateDestination = null;
    }

    private void saveGatesToFile() {
        File gateFile = new File(getDataFolder(), "gates.txt");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(gateFile))) {
            for (GateData gate : gates) {
                writer.write("Gate ID: " + gate.getGateId());
                writer.newLine();
                writer.write("Corner 1: " + formatLocation(gate.getCorner1()));
                writer.newLine();
                writer.write("Corner 2: " + formatLocation(gate.getCorner2()));
                writer.newLine();
                writer.write("Gate Destination: " + formatLocation(gate.getGateDestination()));
                writer.newLine();
                writer.write("-----------");
                writer.newLine();
            }
        } catch (IOException e) {
            logger.warning("Failed to save all gates to gates.txt.");
        }
    }

    private void loadGatesFromFile() {
        File gateFile = new File(getDataFolder(), "gates.txt");
        if (!gateFile.exists()) {
            logger.info("No gates.txt file found.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(gateFile))) {
            String line;
            String gateId = null;
            Location corner1 = null;
            Location corner2 = null;
            Location gateDestination = null;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Gate ID: ")) {
                    gateId = line.split(": ")[1];
                } else if (line.startsWith("Corner 1: ")) {
                    corner1 = parseLocation(line.split(": ")[1]);
                } else if (line.startsWith("Corner 2: ")) {
                    corner2 = parseLocation(line.split(": ")[1]);
                } else if (line.startsWith("Gate Destination: ")) {
                    gateDestination = parseLocation(line.split(": ")[1]);
                } else if (line.equals("-----------")) {
                    if (gateId != null && corner1 != null && corner2 != null && gateDestination != null) {
                        gates.add(new GateData(gateId, corner1, corner2, gateDestination));
                    }
                    gateId = null;
                    corner1 = null;
                    corner2 = null;
                    gateDestination = null;
                }
            }
        } catch (IOException e) {
            logger.warning("Failed to load gates from file.");
        }
    }

    private Location parseLocation(String locString) {
        String[] parts = locString.split(", ");
        return new Location(Bukkit.getWorld(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
    }

    private String formatLocation(Location loc) {
        return loc.getWorld().getName() + ", " + loc.getX() + ", " + loc.getY() + ", " + loc.getZ();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        for (GateData gate : gates) {
            if (isInsideGateArea(player.getLocation(), gate)) {
                player.sendMessage("Teleporting to: " + gate.getGateId());
                player.teleport(gate.getGateDestination());
                return;
            }
        }
    }

    private boolean isInsideGateArea(Location playerLocation, GateData gate) {
        return playerLocation.getX() >= Math.min(gate.getCorner1().getX(), gate.getCorner2().getX()) &&
                playerLocation.getX() <= Math.max(gate.getCorner1().getX(), gate.getCorner2().getX()) &&
                playerLocation.getY() >= Math.min(gate.getCorner1().getY(), gate.getCorner2().getY()) &&
                playerLocation.getY() <= Math.max(gate.getCorner1().getY(), gate.getCorner2().getY()) &&
                playerLocation.getZ() >= Math.min(gate.getCorner1().getZ(), gate.getCorner2().getZ()) &&
                playerLocation.getZ() <= Math.max(gate.getCorner1().getZ(), gate.getCorner2().getZ());
    }
}
