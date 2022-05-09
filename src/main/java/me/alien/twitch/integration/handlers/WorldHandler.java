package me.alien.twitch.integration.handlers;

import me.alien.twitch.integration.Main;
import me.alien.twitch.integration.util.Vector2I;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class WorldHandler {
    final World world;
    final Main plugin;
    public WorldHandler(World world, Main plugin){
        this.world = world;
        this.plugin = plugin;
    }

    public boolean setBlock(Vector2I pos, String block){
        Material mat;
        try{
            mat = Material.valueOf(block);
        }catch (Exception e){
            return false;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            this.world.setType(pos.getX(), pos.getY(), pos.getZ(), mat);
        });
        return true;
    }
    public boolean setBlock(Vector2I pos, String block, String... replace){
        ArrayList<Material> replaceList = new ArrayList<>();

        for(String string : replace){
            Material mat;
            try{
                mat = Material.valueOf(block);
            }catch (Exception e){
                continue;
            }
            replaceList.add(mat);
        }

        if(replaceList.isEmpty()) return false;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if(!replaceList.contains(this.world.getType(pos.getX(), pos.getY(), pos.getZ()))){
                this.world.setType(pos.getX(), pos.getY(), pos.getZ(), Material.valueOf(block));
            }
        });

        return true;
    }

    public Entity spawnEntity(Vector2I pos, String name){
        AtomicReference<Entity> e = new AtomicReference<>();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Location loc = new Location(world, pos.getX(), pos.getY(), pos.getZ());
            e.set(this.world.spawnEntity(loc, EntityType.valueOf(name)));
        });
        return e.get();
    }

    public Entity getRandomEntityInWorld(){
        AtomicReference<Entity> e = new AtomicReference<>();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            e.set(world.getLivingEntities().get((int) (Math.random() * world.getLivingEntities().size())));
        });
        return e.get();
    }

    public String getType(Entity e){
        AtomicReference<String> name = new AtomicReference<>("");
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            name.set(e.getType().name());
        });
        return name.get();
    }

    public int getMaxHeight(){
        AtomicInteger max = new AtomicInteger();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            max.set(world.getMaxHeight());
        });
        return max.get();
    }

    public int getMinHeight(){
        return world.getMaxHeight();
    }
}