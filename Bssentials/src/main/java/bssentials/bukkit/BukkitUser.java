package bssentials.bukkit;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import bssentials.Bssentials;
import bssentials.api.User;
import bssentials.configuration.BssConfiguration;
import bssentials.configuration.Configs;

public class BukkitUser implements User {

    public Player base;
    private static File folder;
    private File file;

    public boolean npc = false;
    public String lastAccountName;
    public BigDecimal money = new BigDecimal(100);
    public String nick = "_null_";

    public ArrayList<String> homes = new ArrayList<>();

    public FileConfiguration user = new YamlConfiguration();

    public BukkitUser(Player base) {
        this.base = base;
        if (null == folder)
            folder = new File(Bssentials.getInstance().getDataFolder(), "userdata");

        this.file = new File(folder, base.getUniqueId().toString() + ".yml");
        this.lastAccountName = base.getName();
        this.user = new YamlConfiguration();
        folder.mkdir();

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Unable to create file: " + folder.getAbsolutePath());
            }
            user.set("npc", false);
            user.set("lastAccountName", base.getName());
            user.set("money", 100.0); // Default
            save();
        }

        try {
            user.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }

        if (file.exists()) {
            npc = user.getBoolean("npc", false);
            lastAccountName = user.getString("lastAccountName");
            try {
                money = new BigDecimal((double) user.get("money"));
            } catch (Exception e) {
                money = BigDecimal.valueOf(Double.valueOf((int) user.get("money")));
            }
            nick = user.getString("nick");
        }
    }

    public boolean isPlayer() {
        return true;
    }

    public FileConfiguration getConfig() {
        return user;
    }

    public Location getHome(String home) {
        if (user.getConfigurationSection("homes." + home) == null)
            return null;

        World w = Bukkit.getServer().getWorld(user.getString("homes." + home + ".world"));
        double x = user.getDouble("homes." + home + ".x");
        double y = user.getDouble("homes." + home + ".y");
        double z = user.getDouble("homes." + home + ".z");
        return new Location(w, x, y, z);
    }

    public void save() {
        try {
            user.save(file);
            npc = user.getBoolean("npc", false);
            lastAccountName = user.getString("lastAccountName");
            try {
                money = new BigDecimal((double) user.get("money"));
            } catch (Exception e) {
                Object mon = user.get("money");
                money = mon instanceof BigDecimal ? (BigDecimal) mon : BigDecimal.valueOf(Double.valueOf((int) mon));
            }
            nick = user.getString("nick");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Unable to write to file: " + folder.getAbsolutePath());
        }
    }

    public BigDecimal getMoney() {
        return money;
    }

    public boolean isAuthorized(String string) {
        return base.hasPermission(string);
    }

    public void setMoney(BigDecimal balance) {
        user.set("money", balance.doubleValue());
        save();
    }

    public void setNick(String n) throws Exception {
        BssConfiguration config = Configs.MAIN;
        int maxLength = config.getInt("max-nick-length");

        String s = config.getBoolean("ignore-colors-in-max-nick-length", true) ? ChatColor.stripColor(n) : n;

        if (maxLength > 0 && s.length() > maxLength)
            throw new Exception("Nickname is too long");

        if (config.contains("nickname-prefix"))
            n = config.getString("nickname-prefix") + n;

        List<String> blackList = config.getStringList("nick-blacklist");
        for (String str : blackList) {
            String strip = ChatColor.stripColor(n);
            if ((strip.equalsIgnoreCase(str) || strip.contains(str)) && !isAuthorized("bssentials.nick.blacklist.bypass"))
                throw new Exception("Nick blacklisted");
        }

        user.set("nick", n);
        save();
        base.setDisplayName(n);
        if (Configs.MAIN.getBoolean("change-playerlist", true))
            base.setPlayerListName(base.getDisplayName());
    }

    public boolean isNPC() {
        return false; // TODO: should we have NPC support?
    }

    @Deprecated
    public static BukkitUser getByName(String name) {
        if (name == null) 
            throw new RuntimeException("Economy username cannot be null");
        return new BukkitUser(Bukkit.getPlayerExact(name));
    }

    public void setHome(String home, Location l) {
        user.set("homes." + home + ".world", l.getWorld().getName());
        user.set("homes." + home + ".x", l.getX());
        user.set("homes." + home + ".y", l.getY());
        user.set("homes." + home + ".z", l.getZ());
        save();
    }

    public void delHome(String home) {
        user.set("homes." + home + ".world", null);
        user.set("homes." + home + ".x", null);
        user.set("homes." + home + ".y", null);
        user.set("homes." + home + ".z", null);
        user.set("homes." + home, null);
        save();
    }

    public void sendMessage(String txt) {
        base.sendMessage(ChatColor.translateAlternateColorCodes('&', txt));
    }

    public Set<String> getHomes() {
        ConfigurationSection section = user.getConfigurationSection("homes");
        return section.getValues(false).keySet();
    }

    public UUID getUniqueId() {
        return base.getUniqueId();
    }

    public String getName(boolean custom) {
        return custom ? base.getDisplayName() : base.getName();
    }

    public Location getLocation() {
        return base.getLocation();
    }

    public Block getTargetBlock(Set<Material> transparent, int maxDistance) {
        return base.getTargetBlock(transparent, maxDistance);
    }

    public void clearInventory(String item) {
        if (null == item) {
            base.getInventory().clear();
        } else base.getInventory().remove(Material.valueOf(item));
    }

    public boolean isOnline() {
        return base.isOnline();
    }

    public void openEnderchest(User target) {
        base.openInventory(((Player)target.getBase()).getEnderChest());
    }

    public int getExpLevel() {
        return base.getLevel();
    }

    public void setExpLevel(int i) {
        base.setLevel(i);
    }

    public void setAllowFly(boolean allow) {
        base.setAllowFlight(allow);
        base.setFlying(allow);
    }

    public boolean isAllowedToFly() {
        return base.getAllowFlight();
    }

    public void setGameMode(GameMode mode) {
        base.setGameMode(mode);
    }

    @SuppressWarnings("deprecation")
    public int getItemInMainHand() {
        return base.getInventory().getItemInMainHand().getType().getId();
    }

    public void setHelmentToMainHandItem() {
        ItemStack itemHand = base.getInventory().getItemInMainHand();
        PlayerInventory inventory = base.getInventory();
        ItemStack itemHead = inventory.getHelmet();
        inventory.removeItem(new ItemStack[] { itemHand });
        inventory.setHelmet(itemHand);
        inventory.setItemInMainHand(itemHead);
    }

    public void setHealthAndFoodLevel(int h, int f) {
        if (h != -1) base.setHealth(h);
        if (f != -1) base.setFoodLevel(f);
    }

    public boolean isOp() {
        return base.isOp();
    }

    public boolean teleport(Location l) {
        return base.teleport(l);
    }

    public void openOtherUserInventory(User target) {
        base.openInventory(((Player)target.getBase()).getInventory());
    }

    @Override
    public Object getBase() {
        return base;
    }

    @Override
    public String getNick() {
        return nick;
    }

}