package com.example.fastminecarts;

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

        // 40 blocks per second is 2.0 max speed in Spigot/Paper
        // (default is 0.4 which is 8 blocks per second)
        maxSpeed = config.getDouble("max-speed", 2.0);

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
}
