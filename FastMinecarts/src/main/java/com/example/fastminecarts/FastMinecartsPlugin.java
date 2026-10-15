package com.example.fastminecarts;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.data.Rail;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class FastMinecartsPlugin extends JavaPlugin implements Listener {

    private double maxSpeed;
    private double horseSpeedMultiplier;
    private final double DEFAULT_SPEED = 0.4D; // 8 blocks per second

    private NamespacedKey horseSpeedModifierKey;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        FileConfiguration config = this.getConfig();

        maxSpeed = config.getDouble("max-speed", 1.6);
        horseSpeedMultiplier = config.getDouble("horse-speed-multiplier", 2.0);

        horseSpeedModifierKey = new NamespacedKey(this, "horse_speed_modifier");

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("FastMinecarts has been enabled! Max minecart speed set to " + maxSpeed + ", Horse multiplier set to " + horseSpeedMultiplier);

        // Apply modifier to currently loaded horses in case of reload
        for (World world : getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof AbstractHorse) {
                    applyHorseSpeedModifier((AbstractHorse) entity);
                }
            }
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("FastMinecarts has been disabled!");
    }

    private void applyHorseSpeedModifier(AbstractHorse horse) {
        AttributeInstance speedAttribute = horse.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttribute != null) {
            speedAttribute.removeModifier(horseSpeedModifierKey);

            if (horseSpeedMultiplier != 1.0) {
                double amount = horseSpeedMultiplier - 1.0;
                AttributeModifier modifier = new AttributeModifier(
                    horseSpeedModifierKey,
                    amount,
                    AttributeModifier.Operation.ADD_SCALAR
                );
                speedAttribute.addModifier(modifier);
            }
        }
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof AbstractHorse) {
            applyHorseSpeedModifier((AbstractHorse) event.getEntity());
        }
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof AbstractHorse) {
                applyHorseSpeedModifier((AbstractHorse) entity);
            }
        }
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

    // Fix for minecarts flying off tracks on corners at high speed
    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        if (!(event.getVehicle() instanceof Minecart)) return;
        Minecart cart = (Minecart) event.getVehicle();

        // Only apply corner-slowing logic to ridden carts with boosted max speed
        if (cart.getPassengers().isEmpty() || !(cart.getPassengers().get(0) instanceof Player)) return;

        if (isApproachingCorner(cart)) {
            // Temporarily set to safe default speed for the corner
            cart.setMaxSpeed(DEFAULT_SPEED);
        } else {
            // Restore high speed if straight track
            cart.setMaxSpeed(maxSpeed);
        }
    }

    private boolean isCorner(Rail.Shape shape) {
        return shape == Rail.Shape.SOUTH_EAST ||
               shape == Rail.Shape.SOUTH_WEST ||
               shape == Rail.Shape.NORTH_WEST ||
               shape == Rail.Shape.NORTH_EAST;
    }

    private boolean isApproachingCorner(Minecart cart) {
        Location loc = cart.getLocation();
        Vector vel = cart.getVelocity();
        double speed = vel.length();

        if (speed < 0.1) return false;

        Vector dir = vel.clone().normalize();

        // Check blocks ahead based on max speed to ensure we catch corners before hitting them.
        // We look ahead a bit further than the current max speed blocks/tick.
        int checkDistance = (int) Math.ceil(maxSpeed) + 2;

        for (int i = 0; i <= checkDistance; i++) {
            Location checkLoc = loc.clone().add(dir.clone().multiply(i));

            // Check the block at cart level
            Block b = checkLoc.getBlock();
            if (b.getBlockData() instanceof Rail) {
                if (isCorner(((Rail) b.getBlockData()).getShape())) return true;
            } else {
                // Also check block below (for descending slopes)
                Block below = checkLoc.clone().subtract(0, 1, 0).getBlock();
                if (below.getBlockData() instanceof Rail) {
                    if (isCorner(((Rail) below.getBlockData()).getShape())) return true;
                }
            }
        }
        return false;
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
        } else if (command.getName().equalsIgnoreCase("fasthorses")) {
            if (!sender.hasPermission("fasthorses.admin")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return true;
            }

            if (args.length != 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /fasthorses <multiplier>");
                return true;
            }

            try {
                double newMultiplier = Double.parseDouble(args[0]);
                if (newMultiplier < 0) {
                    sender.sendMessage(ChatColor.RED + "Multiplier must be positive.");
                    return true;
                }

                horseSpeedMultiplier = newMultiplier;
                getConfig().set("horse-speed-multiplier", horseSpeedMultiplier);
                saveConfig();

                sender.sendMessage(ChatColor.GREEN + "Horse speed multiplier has been set to " + horseSpeedMultiplier);

                for (World world : getServer().getWorlds()) {
                    for (Entity entity : world.getEntities()) {
                        if (entity instanceof AbstractHorse) {
                            applyHorseSpeedModifier((AbstractHorse) entity);
                        }
                    }
                }

                return true;
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Please enter a valid number for multiplier.");
                return true;
            }
        }
        return false;
    }
}
