package com.example.fastminecarts;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class FastMinecartsPlugin extends JavaPlugin implements Listener {

    private double maxSpeed;
    private final double DEFAULT_SPEED = 0.4D; // 8 blocks per second

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        FileConfiguration config = this.getConfig();

        // 32 blocks per second is 1.6 max speed in Spigot/Paper
        // (default is 0.4 which is 8 blocks per second)
        maxSpeed = config.getDouble("max-speed", 1.6);

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("FastMinecarts has been enabled! Max speed set to " + maxSpeed);
    }

    @Override
    public void onDisable() {
        getLogger().info("FastMinecarts has been disabled!");
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (event.getVehicle() instanceof Minecart && event.getEntered() instanceof Player) {
            Minecart minecart = (Minecart) event.getVehicle();
            minecart.setMaxSpeed(maxSpeed);
        }
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        if (event.getVehicle() instanceof Minecart && event.getExited() instanceof Player) {
            Minecart minecart = (Minecart) event.getVehicle();
            minecart.setMaxSpeed(DEFAULT_SPEED);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("fastminecarts")) {
            if (!sender.hasPermission("fastminecarts.admin")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return true;
            }

            if (args.length != 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /fastminecarts <speed>");
                return true;
            }

            try {
                double newSpeed = Double.parseDouble(args[0]);
                if (newSpeed < 0) {
                    sender.sendMessage(ChatColor.RED + "Speed must be positive.");
                    return true;
                }

                maxSpeed = newSpeed;
                getConfig().set("max-speed", maxSpeed);
                saveConfig();

                sender.sendMessage(ChatColor.GREEN + "Minecart max speed has been set to " + maxSpeed);

                // Also update any currently ridden minecarts online
                for (Player p : getServer().getOnlinePlayers()) {
                    if (p.isInsideVehicle() && p.getVehicle() instanceof Minecart) {
                        Minecart cart = (Minecart) p.getVehicle();
                        cart.setMaxSpeed(maxSpeed);
                    }
                }

                return true;
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Please enter a valid number for speed.");
                return true;
            }
        }
        return false;
    }
}
