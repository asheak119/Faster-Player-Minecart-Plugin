package com.example.fastminecarts;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class FastMinecartsPlugin extends JavaPlugin implements Listener {

    private double horseSpeedMultiplier;
    private NamespacedKey horseSpeedModifierKey;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        FileConfiguration config = this.getConfig();

        horseSpeedMultiplier = config.getDouble("horse-speed-multiplier", 2.0);

        horseSpeedModifierKey = new NamespacedKey(this, "horse_speed_modifier");

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("FastMinecarts has been enabled! Horse multiplier set to " + horseSpeedMultiplier);

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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("fasthorses")) {
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
