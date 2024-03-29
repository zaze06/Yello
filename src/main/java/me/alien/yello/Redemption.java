/*
 * MIT License
 *
 * Copyright (c) 2022. Zacharias Zellén
 */

package me.alien.yello;

import com.github.twitch4j.TwitchClient;
import me.alien.yello.handlers.Envierment;
import me.alien.yello.handlers.Handler;
import com.github.twitch4j.pubsub.domain.ChannelPointsUser;
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent;
import me.alien.yello.util.Vector3I;
import me.limeglass.streamelements.api.StreamElements;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.python.core.*;
import org.python.util.PythonInterpreter;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Redemption extends Thread {

    RewardRedeemedEvent event;
    Main plugin;
    Pair<TwitchClient, String> twitchPair;

    public Redemption(RewardRedeemedEvent event, Pair<TwitchClient, String> twitchPair, Main plugin){
        this.event = event;
        this.plugin = plugin;
        this.twitchPair = twitchPair;
    }

    @Override
    public void run() {
        String id = event.getRedemption().getReward().getId();
        long cost = event.getRedemption().getReward().getCost();
        String userName = event.getRedemption().getUser().getDisplayName();
        ChannelPointsUser user = event.getRedemption().getUser();

        TwitchClient twitchClient = twitchPair.key;
        String chat = twitchPair.value;





        Player[] players = null;

        try{
            players = plugin.getServer().getOnlinePlayers().toArray(new Player[0]);
        }catch (NullPointerException ignored){}

        if (players == null) {
            return;
        }

        for(Player player : players) {

            if(player.getGameMode() == GameMode.SPECTATOR) continue;

            final Player p = player;

            int pitch = (int) p.getLocation().getPitch();
            Location pos = p.getLocation().clone();
            if (pitch < 0) {
                pitch = 180 + (-pitch);
            }
            if (pitch == 360) {
                pitch = 0;
            }
            if (pitch >= -45 && pitch < 45) {
                pos.add(0, 0, -1);
            } else if (pitch >= 45 && pitch < 135) {
                pos.add(1, 0, 0);
            } else if (pitch >= 135 && pitch < 225) {
                pos.add(0, 0, 1);
            } else if (pitch >= 225 && pitch < 360) {
                pos.add(-1, 0, 0);
            }

            int pPosX = p.getLocation().getBlockX();
            int pPosY = p.getLocation().getBlockY();
            int pPosZ = p.getLocation().getBlockZ();

            int odds = (int) (Main.rand.nextDouble() * 100);

            System.out.println(odds + "");
            if (plugin.config.getBoolean("Debug")) {
                twitchClient.getChat().sendPrivateMessage("AlienFromDia", odds + "");
            }

            boolean failed = true;

            for(String redemtion : plugin.readmeEventAction) {
                try {
                    PythonInterpreter pi =  new PythonInterpreter();
                    //pi.compile(new FileReader(System.getProperty("user.dir") + "/data/redemtions/shared.py"));
                    //pi.set("shared", pi.eval(Loader.loadFile(new FileInputStream(System.getProperty("user.dir") + "/data/redemtions/shared.py"), "\n")));
                    //pi.exec(Loader.loadFile(new FileInputStream(System.getProperty("user.dir") + "/data/redemtions/shared.py"), "\n"));
                    //pi.eval(Loader.loadFile(new FileInputStream(System.getProperty("user.dir") + "/data/redemtions/shared.py"), "\n"));

                    imp.createFromCode("shared", pi.compile(Loader.loadFile(System.getProperty("user.dir") + "/data/redemtions/shared.py"), "\n"));
                    //imp.load(Loader.loadFile(new FileInputStream(System.getProperty("user.dir") + "/data/redemtions/shared.py"), "\n"));
                    pi.exec(Loader.loadFile(System.getProperty("user.dir") + "/data/redemtions/" + redemtion, "\n"));
                    AtomicBoolean redemtionID = new AtomicBoolean(false);
                    AtomicBoolean redemtionName = new AtomicBoolean(false);
                    try{
                        PyObject obj = pi.get("redemptionId");
                        if(obj == null){
                            throw new NullPointerException();
                        }
                        ArrayList<String> strs = new ArrayList<>(List.of((String[]) obj.__tojava__(String[].class)));
                        strs.forEach(str -> {
                            if(str.equals(id)){
                                redemtionID.set(true);
                            }
                        });
                    }catch (Exception ignored){}
                    String name = "";
                    try{
                        PyObject obj = pi.get("redemptionName");
                        if(obj == null){
                            throw new NullPointerException();
                        }
                        ArrayList<String> strs = new ArrayList<>(List.of((String[]) obj.__tojava__(String[].class)));
                        strs.forEach(str -> {
                            if(str.equals(event.getRedemption().getReward().getTitle())){
                                redemtionName.set(true);
                            }
                        });
                    }catch (Exception ignored){}

                    Envierment env = Envierment.valueOf((String) pi.get("env").__tojava__(String.class));
                    if((redemtionID.get() || redemtionName.get()) && (env == Envierment.PLUGIN || env == Envierment.BOTH) && !plugin.grace){
                        plugin.getLogger().info("Found "+redemtion);
                        pi.get("run").__call__(new PyObject[]{
                                new PyLong(cost),
                                new PyString(userName),
                                Py.java2py(user),
                                Py.java2py(new Handler(p, plugin)),
                                Py.java2py(new Vector3I(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ())),
                                new PyInteger(odds),
                                Py.java2py(event.getRedemption())
                        });
                        failed = false;
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
                /*
                try {
                    ScriptEngine ee = new ScriptEngineManager().getEngineByName("rhino");
                    ee.eval(Loader.loadFile(new FileInputStream(System.getProperty("user.dir") + "/data/redemtions/" + redemtion)));
                    Object var = ee.get("id");
                    if(var instanceof String){
                        if(((String)var).equalsIgnoreCase(id)){
                            Invocable file = (Invocable) ee;
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                try {
                                    file.invokeFunction("run", cost, userName, user, p, pos);
                                } catch (ScriptException e) {
                                    throw new RuntimeException(e);
                                } catch (NoSuchMethodException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                        }
                    }
                } catch (ScriptException e) {
                    throw new RuntimeException(e);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
                */
            }

            if(failed){
                if(id.equals("b184ce9e-1032-46bd-a475-1e93c60f4972")){
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        p.getWorld().spawn(pos, Arrow.class, CreatureSpawnEvent.SpawnReason.CUSTOM, e -> {
                            Vector voc = p.getVelocity().clone();
                            Vector velocity = new Vector(voc.getX()*2, voc.getY()*2, voc.getZ()*2);
                            e.setVelocity(velocity);
                        });
                    });
                }
                else if(id.equals("0f44c4f9-1000-4867-844e-ade303564e7c")){

                }
            }

            if(false) {
                if (id.equalsIgnoreCase(plugin.redemptions.getString("hiss")) && !plugin.grace) {
                    if (odds <= plugin.config.getInt("ChargedCreeperOdds")) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            p.getWorld().spawn(pos, Creeper.class, CreatureSpawnEvent.SpawnReason.CUSTOM, e -> {
                                e.setSilent(true);
                                e.setPowered(true);
                                e.customName(Component.text(event.getRedemption().getUser().getDisplayName()));
                            });
                        });

                    } else if (odds <= plugin.config.getInt("CreeperOdds")) {

                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            p.getWorld().spawn(pos, Creeper.class, CreatureSpawnEvent.SpawnReason.CUSTOM, e -> {
                                e.setSilent(true);
                                e.customName(Component.text(event.getRedemption().getUser().getDisplayName()));
                            });
                        });
                    }
                    //p.sendMessage(event.getRedemption().getUser().getDisplayName()+" redeemed hiss!");
                } else if (id.equalsIgnoreCase(plugin.redemptions.getString("balloonPop")) && !plugin.grace) {
                    if (odds <= plugin.config.getInt("BalloonPopOdds")) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            for (int x = -3; x < 4; x++) {
                                for (int y = -3; y < 4; y++) {
                                    for (int z = -3; z < 4; z++) {
                                        p.getWorld().setType(pPosX + x, pPosY + y, pPosZ + z, Material.AIR);
                                    }
                                }
                            }
                            int i = (int) (Main.rand.nextDouble() * plugin.potionEffectTypes.size());
                            p.addPotionEffect(plugin.potionEffectTypes.get(i).createEffect(40 * 20, 4));
                            i = (int) (Main.rand.nextDouble() * plugin.potionEffectTypes.size());
                            p.addPotionEffect(plugin.potionEffectTypes.get(i).createEffect(40 * 20, 4));
                        });
                        p.sendMessage(event.getRedemption().getUser().getDisplayName() + " redeemed BalloonPop!");
                    }
                } else if (id.equalsIgnoreCase(plugin.redemptions.getString("knock")) && !plugin.grace) {
                    if (odds <= plugin.config.getInt("KnockKnockBabyOdds")) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            p.getWorld().setType(pos, Material.CRIMSON_DOOR);
                            p.getWorld().spawn(pos, Zombie.class, e -> {
                                e.customName(Component.text(event.getRedemption().getUser().getDisplayName()));
                                e.setSilent(true);
                                if (odds <= 30) {
                                    e.setBaby();
                                }
                            });
                        });
                    }
                } else if (id.equalsIgnoreCase(plugin.redemptions.getString("nut")) && !plugin.grace) {
                    if (odds <= plugin.config.getInt("NutOdds")) {
                        //List of monsters to spawn
                        int pilliger = ((int) (Main.rand.nextDouble() * 2)) + 2;
                        int vindicators = ((int) (Main.rand.nextDouble() * 2)) + 5;
                        int witch = ((int) (Main.rand.nextDouble() * 2)) + 1;
                        int evoker = 2;
                        int RavagerVindicator = ((int) (Main.rand.nextDouble() * 1)) + 1;
                        int RavagerEvoker = 1;

                        int total = pilliger + vindicators + witch + evoker + RavagerEvoker + RavagerVindicator;

                        Location location = p.getLocation();
// redemtion nut, should spawn some monsters accoring to the list above
                        for (int i = 0; i < total; i++) {
                            int x = (int) (Main.rand.nextDouble() * ((location.getBlockX() + 30) - (location.getBlockX() - 30)) + (location.getBlockX() + 30));
                            int z = (int) (Main.rand.nextDouble() * ((location.getBlockZ() + 30) - (location.getBlockZ() - 30)) + (location.getBlockZ() + 30));
                            int y = p.getWorld().getMaxHeight();

                            while (p.getWorld().getBlockAt(x, y, z).getType().isAir() && y != p.getWorld().getMinHeight()) {
                                y--;
                            }
                            if (y == p.getWorld().getMinHeight()) {
                                y = pPosY;
                                for (int x1 = x - 1; x1 < x + 1; x1++) {
                                    for (int z1 = z - 1; z1 < z + 1; z1++) {
                                        Block blockAt = p.getWorld().getBlockAt(x1, y - 1, z1);
                                        if (blockAt.getType().isAir()) {
                                            blockAt.setType(Material.DIRT);
                                        }
                                    }
                                }
                            }

                            int finalY = y;

                            if (pilliger > 0) {
                                plugin.getServer().getScheduler().runTask(plugin, () -> {
                                    p.getWorld().spawn(new Location(p.getWorld(), x, finalY, z), Pillager.class, CreatureSpawnEvent.SpawnReason.CUSTOM, e -> {
                                        e.setSilent(true);
                                        e.setTarget(p);
                                        e.customName(Component.text(userName));
                                    });
                                });
                                pilliger--;
                            } else if (vindicators > 0) {
                                plugin.getServer().getScheduler().runTask(plugin, () -> {
                                    p.getWorld().spawn(new Location(p.getWorld(), x, finalY, z), Vindicator.class, CreatureSpawnEvent.SpawnReason.CUSTOM, e -> {
                                        e.setSilent(true);
                                        e.setTarget(p);
                                        e.customName(Component.text(userName));
                                    });
                                });
                                vindicators--;
                            } else if (witch > 0) {
                                plugin.getServer().getScheduler().runTask(plugin, () -> {
                                    p.getWorld().spawn(new Location(p.getWorld(), x, finalY, z), Witch.class, CreatureSpawnEvent.SpawnReason.CUSTOM, e -> {
                                        e.setSilent(true);
                                        e.setTarget(p);
                                        e.customName(Component.text(userName));
                                    });
                                });
                                witch--;
                            } else if (evoker > 0) {
                                plugin.getServer().getScheduler().runTask(plugin, () -> {
                                    p.getWorld().spawn(new Location(p.getWorld(), x, finalY, z), Evoker.class, CreatureSpawnEvent.SpawnReason.CUSTOM, e -> {
                                        e.setSilent(true);
                                        e.setTarget(p);
                                        e.customName(Component.text(userName));
                                    });
                                });
                                evoker--;
                            } else if (RavagerEvoker > 0) {
                                plugin.getServer().getScheduler().runTask(plugin, () -> {
                                    p.getWorld().spawn(new Location(p.getWorld(), x, finalY, z), Ravager.class, CreatureSpawnEvent.SpawnReason.CUSTOM, e -> {
                                        e.setSilent(true);
                                        e.setTarget(p);
                                        e.customName(Component.text(userName));
                                        e.addPassenger(p.getWorld().spawn(new Location(p.getWorld(), x, finalY, z), Evoker.class, CreatureSpawnEvent.SpawnReason.CUSTOM, e1 -> {
                                            e.customName(Component.text(userName));
                                            e.setTarget(p);
                                            e.setSilent(true);
                                        }));
                                    });
                                });
                                RavagerEvoker--;
                            } else if (RavagerVindicator > 0) {
                                plugin.getServer().getScheduler().runTask(plugin, () -> {
                                    p.getWorld().spawn(new Location(p.getWorld(), x, finalY, z), Ravager.class, CreatureSpawnEvent.SpawnReason.CUSTOM, e -> {
                                        e.setSilent(true);
                                        e.setTarget(p);
                                        e.customName(Component.text(userName));
                                        e.addPassenger(p.getWorld().spawn(new Location(p.getWorld(), x, finalY, z), Vindicator.class, CreatureSpawnEvent.SpawnReason.CUSTOM, e1 -> {
                                            e.customName(Component.text(userName));
                                            e.setTarget(p);
                                            e.setSilent(true);
                                        }));
                                    });
                                });
                                RavagerVindicator--;
                            }
                        }
                    }
                } else if (id.equalsIgnoreCase(plugin.redemptions.getString("boo")) && !plugin.grace) {
                    if (odds <= plugin.config.getInt("BooOdds")) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            p.addPotionEffect(PotionEffectType.BLINDNESS.createEffect(20 * 20, 3));
                        });
                    }
                } else if (id.equalsIgnoreCase(plugin.redemptions.getString("Mission Failed")) && !plugin.grace) {
                    if (odds <= plugin.config.getInt("MissionFailedOdds60s")) plugin.time = 60;
                    else if (odds <= plugin.config.getInt("MissionFailedOdds30s")) plugin.time = 30;
                    plugin.disableShit = true;
                } else if (id.equalsIgnoreCase(plugin.redemptions.getString("Drop it")) && !plugin.grace) {
                    if (odds <= plugin.config.getInt("DropItOdds")) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            for (int y = pPosY + 4; y >= -60; y--) {
                                for (int x = -2; x <= 2; x++) {
                                    for (int z = -2; z <= 2; z++) {
                                        p.getWorld().setType(pPosX + x, y, pPosZ + z, Material.AIR);
                                    }
                                }
                            }
                        });
                    }
                } else if (id.equalsIgnoreCase(plugin.redemptions.getString("Name Generator")) && !plugin.grace) {
                    if (odds <= plugin.config.getInt("NameGenOdds")) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            p.getWorld().spawn(pos, p.getWorld().getLivingEntities().get((int) (Main.rand.nextDouble() * p.getWorld().getLivingEntities().size())).getClass(), CreatureSpawnEvent.SpawnReason.CUSTOM, e -> {
                                e.customName(Component.text(event.getRedemption().getUserInput()));
                            });
                            p.getWorld().spawn(pos, p.getWorld().getLivingEntities().get((int) (Main.rand.nextDouble() * p.getWorld().getLivingEntities().size())).getClass(), CreatureSpawnEvent.SpawnReason.CUSTOM, e -> {
                                e.customName(Component.text(event.getRedemption().getUserInput()));
                            });
                        });
                    }
                } else if (id.equalsIgnoreCase(plugin.redemptions.getString("Ara Ara")) && !plugin.grace) {
                    if (odds <= plugin.config.getInt("AraAraOdds")) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            p.getWorld().spawn(pos, Evoker.class, e -> {
                                e.customName(Component.text(event.getRedemption().getUser().getDisplayName()));
                            });
                            p.getWorld().spawn(pos, Evoker.class, e -> {
                                e.customName(Component.text(event.getRedemption().getUser().getDisplayName()));
                            });
                            p.getWorld().spawn(pos, Vindicator.class, e -> {
                                e.customName(Component.text(event.getRedemption().getUser().getDisplayName()));
                            });
                            p.getWorld().spawn(pos, Vindicator.class, e -> {
                                e.customName(Component.text(event.getRedemption().getUser().getDisplayName()));
                            });
                            p.addPotionEffect(PotionEffectType.SLOW.createEffect(60 * 20, 2));
                        });
                    }
                } else if (id.equalsIgnoreCase(plugin.redemptions.getString("Hydrate")) && !plugin.grace) {
                    if (odds <= plugin.config.getInt("HydrateOdds")) {

                        Location loc = p.getLocation();
                        for (int x = loc.getBlockX() - 50; x < loc.getBlockX() + 50; x++) {
                            for (int y = loc.getBlockY() - 50; y < loc.getBlockY() + 50; y++) {
                                int z = loc.getBlockZ();
                                int finalY = y;
                                int finalX = x;
                                plugin.getServer().getScheduler().runTask(plugin, () -> {
                                    Block block = p.getWorld().getBlockAt(pPosX + finalX, pPosY + finalY, pPosZ + z);
                                    if (block.getType().isAir()) {
                                        p.getWorld().setType(pPosX + finalX, pPosY + finalY, pPosZ + z, Material.WATER);
                                    } else if (!(block instanceof Door) && block.getBlockData() instanceof Waterlogged w) {
                                        w.setWaterlogged(true);
                                    }
                                });
                            }
                        }
                        for (int x = pos.getBlockX() - 50; x < pos.getBlockX() + 50; x++) {
                            for (int y = pos.getBlockY() - 50; y < pos.getBlockY() + 50; y++) {
                                int z = loc.getBlockZ();
                                int finalX = x;
                                int finalY = y;
                                plugin.getServer().getScheduler().runTask(plugin, () -> {
                                    Block block = p.getWorld().getBlockAt(pPosX + finalX, pPosY + finalY, pPosZ + z);
                                    if (block.getType().isAir()) {
                                        p.getWorld().setType(pPosX + finalX, pPosY + finalY, pPosZ + z, Material.WATER);
                                    } else if (!(block instanceof Door) && block.getBlockData() instanceof Waterlogged w) {
                                        w.setWaterlogged(true);
                                    }
                                });
                            }
                        }

                        for (int z = pos.getBlockZ() - 50; z < pos.getBlockZ() + 50; z++) {
                            for (int y = pos.getBlockY() - 50; y < pos.getBlockY() + 50; y++) {
                                int x = loc.getBlockX();
                                int finalY = y;
                                int finalZ = z;
                                plugin.getServer().getScheduler().runTask(plugin, () -> {
                                    Block block = p.getWorld().getBlockAt(pPosX + x, pPosY + finalY, pPosZ + finalZ);
                                    if (block.getType().isAir()) {
                                        p.getWorld().setType(pPosX + x, pPosY + finalY, pPosZ + finalZ, Material.WATER);
                                    } else if (!(block instanceof Door) && block.getBlockData() instanceof Waterlogged w) {
                                        w.setWaterlogged(true);
                                    }
                                });
                            }
                        }
                        for (int z = pos.getBlockZ() - 50; z < pos.getBlockZ() + 50; z++) {
                            for (int y = pos.getBlockY() - 50; y < pos.getBlockY() + 50; y++) {
                                int x = loc.getBlockZ();
                                int finalY = y;
                                int finalZ = z;
                                plugin.getServer().getScheduler().runTask(plugin, () -> {
                                    Block block = p.getWorld().getBlockAt(pPosX + x, pPosY + finalY, pPosZ + finalZ);
                                    if (block.getType().isAir()) {
                                        p.getWorld().setType(pPosX + x, pPosY + finalY, pPosZ + finalZ, Material.WATER);
                                    } else if (!(block instanceof Door) && block.getBlockData() instanceof Waterlogged w) {
                                        w.setWaterlogged(true);
                                    }
                                });
                            }
                        }

                    }
                }

                if (cost >= 100) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        p.playSound(p, Sound.ENTITY_SKELETON_AMBIENT, 10, 10);
                        if (odds <= 70) {
                            p.getWorld().spawn(pos, Skeleton.class, CreatureSpawnEvent.SpawnReason.CUSTOM, e -> {
                                e.setSilent(true);
                                e.setTarget(p);
                                e.customName(Component.text(userName));
                            });
                        }
                    });
                }
            }
        }

        List<Pair<String, StreamElements>> pairList = plugin.SEInterfaces.stream().filter(pair -> pair.key.equals(chat)).toList();
        if(!pairList.isEmpty()) {
            Pair<String, StreamElements> pair = pairList.get(0);
            StreamElements instance = pair.value;

            instance.addPoints(userName, (long) (cost * 0.10));
        }
    }

    static class WaterCube extends Thread{

        final Main plugin;
        final Player p;
        final int xOff;
        final int pPosX;
        final int pPosY;
        final int pPosZ;

        WaterCube(Main plugin, Player p, int xOff, int pPosX, int pPosY, int pPosZ) {
            this.plugin = plugin;
            this.p = p;
            this.xOff = xOff;
            this.pPosX = pPosX;
            this.pPosY = pPosY;
            this.pPosZ = pPosZ;
        }


        @Override
        public void run() {
            for (int x1 = -50+xOff; x1 <= xOff; x1++) {
                for (int y1 = -50; y1 <= 50; y1++) {
                    for (int z1 = -50; z1 <= 50; z1++) {
                        int x = x1, y = y1, z = z1;
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            Block block = p.getWorld().getBlockAt(pPosX + x, pPosY + y, pPosZ + z);
                            if (block.getType().isAir()) {
                                p.getWorld().setType(pPosX + x, pPosY + y, pPosZ + z, Material.WATER);
                            } else if (!(block instanceof Door) && block.getBlockData() instanceof Waterlogged w) {
                                w.setWaterlogged(true);
                            }
                        });
                        try {
                            sleep(1);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
    }
}
