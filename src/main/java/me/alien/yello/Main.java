/*
 * MIT License
 *
 * Copyright (c) 2022. Zacharias Zellén
 */

package me.alien.yello;

import me.alien.yello.custome.combat.Base;
import me.alien.yello.events.RandomEvent;
import me.alien.yello.util.Factorys;
import me.alien.yello.util.ValueComparator;
import com.github.philippheuer.credentialmanager.CredentialManager;
import com.github.philippheuer.credentialmanager.CredentialManagerBuilder;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.auth.providers.TwitchIdentityProvider;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.common.events.domain.EventUser;
import com.github.twitch4j.helix.domain.User;
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent;

import me.despical.commandframework.CommandFramework;
import me.limeglass.streamelements.api.StreamElements;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import javax.swing.Timer;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;

public final class Main extends JavaPlugin {

    public static Map<String, Boolean> setting = new HashMap<>();
    private CommandFramework commandFramework;
    public static final JSONObject credentials = new JSONObject(Loader.loadFile(System.getProperty("user.dir")+"/data/credentials.json", "", Main.class.getResource("/credentials.json.old").getFile()));
    public static final JSONObject redemptions = new JSONObject(Loader.loadFile(Main.class.getResource("/redemtions.json").getFile()));
    public static final JSONObject TOOLS = new JSONObject(Loader.loadFile(Main.class.getResource("/tools.json").getFile()));
    public static final ArrayList<Pair<TwitchClient, String>> twitchClients = new ArrayList<>();
    public static final ArrayList<Pair<String, StreamElements>> SEInterfaces = new ArrayList<>();
    public static final OAuth2Credential credential = new OAuth2Credential("twitch", credentials.getString("user_ID"));
    public static final CredentialManager credentialManager = CredentialManagerBuilder.builder().build();
    //public static final JSONObject mysql = credentials.getJSONObject("mysql");
    public final ArrayList<String> readmeEventAction = new ArrayList<>();
    public static boolean isConnected = false;
    public static boolean connectChatTwitch = false;
    public boolean connectChatMinecraft = false;
    public Level minecraftChat = Level.ALL;
    public ArrayList<PotionEffectType> potionEffectTypes = new ArrayList<>();
    public int time = 0;
    public Timer timer;
    public boolean disableShit = false;
    public Timer looper;
    public Timer pointAccumulation;
    public FileConfiguration config = getConfig();
    public static final Map<String, Integer> viewerPoints = new HashMap<>();
    public ArrayList<String> spawnedHelpers = new ArrayList<>();
    public Team friendlyTeam;
    public Player host;
    public Connection conn = null;
    public static boolean grace = false;
    public static int graceTime = 0;
    public static int graceTimeOrig = 0;
    private static final boolean[] timersNotFinished = new boolean[12];
    private static final int[] timersDelay = new int[12];
    private Timer timers;
    public String channelId = null;
    public RandomEvent randomEvent;

    public static Main plugin;
    public static boolean combat = false;
    public static Random rand = new Random();
    public static ArrayList<Pair<UUID, Map<String, Integer>>> stats = new ArrayList<>();
    public static Logger logger;

    public static void main(String[] args) {
        Main main = new Main();
        main.onEnable();
    }

    @Override
    public void onEnable() {
        plugin = this;
        logger = getLogger();

        randomEvent = new RandomEvent(this);

        credentialManager.registerIdentityProvider(new TwitchIdentityProvider(credentials.getString("bot_ID"), credentials.getString("bot_Secreat"), ""));

        Arrays.fill(timersNotFinished, false);
        Arrays.fill(timersDelay, 0);

        getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
            for(Player p : getServer().getOnlinePlayers()){
                p.addPotionEffect(PotionEffectType.HUNGER.createEffect(1, 2));
            }
        });

        setting.put("redemptions", true);
        setting.put("crafting", false);
        setting.put("shared_damage", false);
        setting.put("explosive_bed", false);
        setting.put("dnd_combat", true);
        setting.put("hard_mobs", false);
        setting.put("custom_combat", false);
        setting.put("chat_read", false);


        commandFramework = new CommandFramework(this);
        commandFramework.registerCommands(new Commands());
        /*commandFramework.setAnyMatch(arguments -> {
            if(arguments.isArgumentsEmpty()) return;

            String label = arguments.getLabel(), arg = arguments.getArgument(0);

            List<StringMatcher.Match> matches = StringMatcher.match(arg, commandFramework.getCommands().stream().map(cmd -> cmd.name().replace(label+".","")).collect(Collectors.toList()));

            if(!matches.isEmpty()){
                arguments.sendMessage("Did you mean %command%?".replace("%command%", label + " " + matches.get(0).getMatch()));
            }

        });*/


        /*try {
            Class.forName("org.mariadb.jdbc.Driver").newInstance();
            String connection = "jdbc:mariadb://"+mysql.getString("ip")+"/"+mysql.getString("database")+"?user="+ mysql.getString("username")+"&password="+ mysql.getString("password");
            getServer().getLogger().info(connection);
            //conn = DriverManager.getConnection(connection );
            String con2 = "jdbc:mariadb://"+mysql.getString("ip")+"/"+mysql.getString("database");
            conn = DriverManager.getConnection(con2, "twitch_bot", "Twitch_bot");
        } catch (Exception ex) {
            /*getServer().getLogger().warning("Cant connect to sql server, defaulting to json backup may be out of date");
            try {
                JSONObject points = new JSONObject(Loader.loadFile(new FileInputStream(System.getProperty("user.dir")+"/data/backup.json")));
                for(String key : points.keySet()){
                    viewerPoints.put(key, points.getInt(key));
                }
            } catch (FileNotFoundException e) {
                getServer().getLogger().warning("Cant find file "+System.getProperty("user.dir")+"/data/backup.json"+" this file be be created");
                File file = new File(System.getProperty("user.dir")+"/data/backup.json");
                try {
                    new File(file.getParent()).mkdirs();
                    file.createNewFile();
                } catch (IOException exc) {
                    exc.printStackTrace();
                }
            }
            //ex.printStackTrace();
        }*/

        try{
            File file = new File(System.getProperty("user.dir")+"/data/redemtions");
            if(!file.exists()){
                file.mkdirs();
                for(File f : new File(getClass().getResource("/redemtions").getFile()).listFiles()){
                    if(Arrays.stream(file.listFiles()).filter((file1 -> file1.getName().equalsIgnoreCase(f.getName()))).toList().size() > 0){
                        File f1 = new File(file, f.getName());
                        f1.createNewFile();
                        BufferedWriter out = new BufferedWriter(new FileWriter(f1));
                        out.write(Loader.loadFile(f.getPath(), "\n"));
                        out.flush();
                        out.close();
                    }
                }
            }
            for(File f : file.listFiles()){
                if(!Loader.loadFile(f.getPath()).equals(Loader.loadFile(getClass().getResource("/redemtions/"+f.getName()).getFile()))){
                    BufferedWriter out = new BufferedWriter(new FileWriter(f));
                    out.write(Loader.loadFile(f.getPath(), "\n"));
                    out.flush();
                    out.close();
                }
            }
            readmeEventAction.addAll(Arrays.asList((Objects.requireNonNull(file.list((dir, name) -> !name.equals("shared.py"))))));
        }catch (Exception ignored){
            ignored.printStackTrace();
        }

        /*if(conn != null) {
            try {
                Statement stmt = conn.createStatement();

                ResultSet rs = stmt.executeQuery("select * from mcpoits.aniki");

                for (int row = 1; row <= rs.getRow(); row++) {
                    rs.absolute(row);
                    viewerPoints.put(rs.getString("id"), rs.getInt("points"));
                }
            } catch (SQLException ex) {
                // handle any errors
                System.out.println("SQLException: " + ex.getMessage());
                System.out.println("SQLState: " + ex.getSQLState());
                System.out.println("VendorError: " + ex.getErrorCode());
            }
        }*/

        getServer().getPluginManager().registerEvents(new MyListener(this), this);



        config.addDefault("ChargedCreeperOdds", 5);
        config.addDefault("CreeperOdds", 40);
        config.addDefault("BalloonPopOdds", 50);
        config.addDefault("KnockKnockOdds", 100);
        config.addDefault("KnockKnockBabyOdds", 20);
        config.addDefault("NutOdds", 20);
        config.addDefault("BooOdds", 70);
        config.addDefault("MissionFailedOdds60s", 40);
        config.addDefault("MissionFailedOdds30s", 100);
        config.addDefault("DropItOdds", 20);
        config.addDefault("NameGenOdds", 100);
        config.addDefault("AraAraOdds", 50);
        config.addDefault("HydrateOdds", 100);
        config.addDefault("Debug", false);
        config.options().copyDefaults(true);
        saveConfig();

        potionEffectTypes.add(PotionEffectType.BLINDNESS);
        potionEffectTypes.add(PotionEffectType.POISON);
        potionEffectTypes.add(PotionEffectType.BAD_OMEN);
        potionEffectTypes.add(PotionEffectType.WITHER);

        timer = new Timer(1000, e -> {
            if(time > 0){
                time--;
            }else
            if(time == 0 && disableShit){
                disableShit = false;
            }

            if(graceTime > 0){
                graceTime--;
            }else
            if(graceTime == 0 && grace){
                grace = false;
                getServer().sendMessage(Component.text("<a_twitch_bot_> grace period is now over"));
            }

            if(graceTime == graceTimeOrig/2 && grace){
                getServer().sendMessage(Component.text("<a_twitch_bot_> "+(graceTimeOrig/2/60)+" min left on grace period"));
            }else if(graceTime == 10 && grace){
                getServer().sendMessage(Component.text("<a_twitch_bot_> 10 seconds left on grace period"));
            }else if(graceTime == 5 && grace){
                getServer().sendMessage(Component.text("<a_twitch_bot_> 5 seconds left on grace period"));
            }
        });

        timer.start();

        looper = new Timer(20*1000*60, e -> {
            for(Pair<TwitchClient, String> twitchPair: twitchClients){
                for (String key : viewerPoints.keySet()) {
                    TwitchClient twitchClient = twitchPair.key;
                    //points.put(key, viewerPoints.get(key));
                    User user = twitchClient.getHelix().getUsers(credential.getAccessToken(), null, Collections.singletonList(key)).execute().getUsers().get(0);
                    //instance.setCurrentUserPoints(user.getLogin(), key);
                }
            }
        });

        //looper.start();

        pointAccumulation = new Timer(5*1000*60, e -> {
            try {
                if (!twitchClients.isEmpty()) {
                    for(Pair<TwitchClient, String> twitchPair : twitchClients) {
                        TwitchClient twitchClient = twitchPair.key;
                        String chat = twitchPair.value;
                        List<User> users = twitchClient.getHelix().getUsers(credential.getAccessToken(), null, twitchClient.getMessagingInterface().getChatters(chat).execute().getAllViewers()).execute().getUsers();
                        for (User key : users) {
                            synchronized (viewerPoints) {
                                Integer points = viewerPoints.get(key.getId());
                                points = (points == null ? 0 : points);
                                viewerPoints.put(key.getId(), points + 50);
                                //getServer().getLogger().info("add " + 50 + " to " + key.getDisplayName() + " now has " + (points + 50));
                            }
                        }
                    }
                }
            }catch (Exception ignored){}
        });

        //pointAccumulation.start();

        timers = new Timer(1000, e -> {
            for(int i = 0; i < timersDelay.length; i++){
                if(timersDelay[i] > 0){
                    timersDelay[i]--;
                } else if (timersNotFinished[i]) {
                    timersNotFinished[i] = false;
                }
            }
        });

        timers.start();

        boolean teamExist = false;

        for(Team team : getServer().getScoreboardManager().getMainScoreboard().getTeams()){
            if(team.getName().equalsIgnoreCase("friendly")) {
                teamExist = true;
                break;
            }
        }

        if(!teamExist) {
            friendlyTeam = getServer().getScoreboardManager().getMainScoreboard().registerNewTeam("friendly");

            friendlyTeam.setAllowFriendlyFire(false);
            friendlyTeam.color(NamedTextColor.AQUA);
        }else{
            friendlyTeam = getServer().getScoreboardManager().getMainScoreboard().getTeam("friendly");
        }
    }

    @Override
    public void onDisable() {
        for(Pair<TwitchClient, String> twitchPair : twitchClients) {
            TwitchClient twitchClient = twitchPair.key;
            String chat = twitchPair.value;
            if (chat != null) {
                if (!chat.equalsIgnoreCase("")) {
                    twitchClient.getChat().sendMessage(chat, "I was told to leave now so bye!");
                }
            }
        }
        /*if(conn != null) {
            StringBuilder data = new StringBuilder("insert into aniki(id, points) values ");
            Set<String> set = viewerPoints.keySet();
            int i = 0;
            for (String key : set) {

                data.append("(").append(key).append(", ").append(viewerPoints.get(key)).append(")").append((i < set.size() ? ", " : ";"));
                i++;
            }
            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(data.toString());
            } catch (SQLException ex) {
                // handle any errors
                System.out.println("SQLException: " + ex.getMessage());
                System.out.println("SQLState: " + ex.getSQLState());
                System.out.println("VendorError: " + ex.getErrorCode());
            }
        }else{
            JSONObject points = new JSONObject();
            for(String key : viewerPoints.keySet()){
                points.put(key, viewerPoints.get(key));
            }
            File file = new File(System.getProperty("user.dir")+"/data/backup.json");
            try{
                BufferedWriter out = new BufferedWriter(new FileWriter(file));
                out.write(points.toString(4));
                out.flush();
            }catch (Exception ignored){}
        }*/
    }

    static void onRedemption(RewardRedeemedEvent event, Pair<TwitchClient, String> twitchPair) {
        Redemption r = new Redemption(event, twitchPair, plugin);
        r.start();
    }

    static void onChatMessage(ChannelMessageEvent event, Pair<TwitchClient, String> twitchPair) {
        String command = event.getMessage();
        String[] args = new String[0];
        TwitchClient twitchClient = twitchPair.key;
        String chat = twitchPair.value;
        StreamElements instance = null;
        List<Pair<String, StreamElements>> list = SEInterfaces.stream().filter(pair -> pair.key.equals(chat)).toList();
        if(!list.isEmpty()){
            instance = list.get(0).value;
        }

        try{
            command = event.getMessage().substring(0, event.getMessage().indexOf(' '));
        }catch (Exception ignored){

        }
        try {
            args = event.getMessage().substring(event.getMessage().indexOf(' ')).split(" ");
            ArrayList<String> tmp = new ArrayList<>();
            for(int i = 1; i < args.length; i++){
                tmp.add(args[i]);
            }
            if(tmp.size() > 0) {
                args = tmp.toArray(new String[0]);
            }
        }catch (Exception ignored){}

        EventUser user = event.getUser();
        if(command.equalsIgnoreCase("-add") && false){
            viewerPoints.put(user.getId(), 10);
        }else

        if (command.equalsIgnoreCase("-test")) {
            //event.reply(twitchClient.getChat(), "yes i work!");
            twitchClient.getChat().sendMessage(chat, "yes i work @" + user.getName());
        }
        else if(command.equalsIgnoreCase("-source")){
            twitchClient.getChat().sendMessage(chat, "I'm a bot made by @AlienFromDia and my source code is located at https://github.com/zaze06/Twitch");
        }
        else if(command.equalsIgnoreCase("-points")){
            if(instance != null) {
                if (args.length > 0) {
                    User user1 = null;
                    String uname = args[0];
                    try {
                        uname = args[0].split("@")[1];
                    } catch (Exception ignored) {
                    }
                    try {

                        user1 = twitchClient.getHelix().getUsers(credential.getAccessToken(), null, Collections.singletonList(uname)).execute().getUsers().get(0);
                    } catch (Exception e) {
                        plugin.getServer().getLogger().warning(e.getCause().toString());
                    }
                    if (user1 != null) {
                        Long points = instance.getUserPoints(user.getName()).getCurrentPoints();
                        twitchClient.getChat().sendMessage(chat, user1.getDisplayName() + " currently have " + (points != null ? points : 0) + " Soul of the lost");
                    } else {
                    /*List<User> users = twitchClient.getHelix().getUsers(credential.getAccessToken(), null, Collections.singletonList(uname)).execute().getUsers();
                    StringBuilder data = new StringBuilder();
                    for(User user1 : users){
                        data.append(user1.getDisplayName());
                    }
                    getServer().getLogger().info(data.toString());*/
                        twitchClient.getChat().sendMessage(chat, "Sorry @" + user.getName() + " i cant find " + uname + " :(");
                    }
                } else {
                    Long points = instance.getUserPoints(user.getName()).getCurrentPoints();
                    twitchClient.getChat().sendMessage(chat, "@" + user.getName() + " you currently have " + (points != null ? points : 0) + " Soul of the lost");
                }
            }else{
                twitchClient.getChat().sendMessage(chat, "Sorry but "+chat+" don't have StreamElements connected so this feature is disabled");
            }
        }
        else if(command.equalsIgnoreCase("-products")){
            twitchClient.getChat().sendMessage(chat, "@"+ user.getName()+" you have the production list hear https://docs.google.com/spreadsheets/d/1irumyoV5YYpgm9GQeObTmKXLtBU5vXMnBXgybnwufeI/ you may need to change to the products page(bottom of the screen)");
        }
        else if(command.startsWith("-buy")){
            if(instance != null) {
                if (args.length > 0) {
                    Player[] players = null;

                    try {
                        players = plugin.getServer().getOnlinePlayers().toArray(new Player[0]);
                    } catch (NullPointerException ignored) {
                    }

                    if (players == null) {
                        return;
                    }
                    if (chat == null) {
                        return;
                    }

                    long points = instance.getUserPoints(user.getName()).getCurrentPoints();
                    long orgPoints = points;
                    boolean removedPoints = false;

                    for (Player player : players) {

                        String redemption = args[0];
                        if (redemption.equalsIgnoreCase("0") || redemption.equalsIgnoreCase("heal")) {
                            if (points >= 100 && !timersNotFinished[0]) {
                                plugin.getServer().getScheduler().runTask(plugin, () -> player.setHealth(20));
                                if (!removedPoints) {
                                    points -= 100;
                                    removedPoints = true;
                                }
                                timersDelay[0] = 3 * 60;
                            } else if (timersNotFinished[0]) {
                                twitchClient.getChat().sendMessage(chat, "Sorry " + user.getName() + " it's " + timersDelay[0] + " sec left");
                            } else {
                                twitchClient.getChat().sendMessage(chat, user.getName() + " you don't have enough soul of the lost");
                            }
                        } else if (redemption.equalsIgnoreCase("1") || redemption.equalsIgnoreCase("feed")) {
                            if (points >= 100 && !timersNotFinished[1]) {
                                plugin.getServer().getScheduler().runTask(plugin, () -> player.setFoodLevel(20));
                                if (!removedPoints) {
                                    points -= 100;
                                    removedPoints = true;
                                }
                                timersDelay[1] = 3 * 60;
                            } else if (timersNotFinished[1]) {
                                twitchClient.getChat().sendMessage(chat, "Sorry " + user.getName() + " it's " + timersDelay[1] + " sec left");
                            } else {
                                twitchClient.getChat().sendMessage(chat, user.getName() + " you don't have enough soul of the lost");
                            }
                        } else if (redemption.equalsIgnoreCase("2") || redemption.equalsIgnoreCase("grace")) {
                            if (points >= 500 && !timersNotFinished[2]) {
                                grace = true;
                                graceTime = 60 * 5;
                                graceTimeOrig = graceTime;
                                points -= 500;
                                timersDelay[2] = 2 * 60;
                                break;
                            } else if (timersNotFinished[2]) {
                                twitchClient.getChat().sendMessage(chat, "Sorry " + user.getName() + " it's " + timersDelay[2] + " sec left");
                            } else {
                                twitchClient.getChat().sendMessage(chat, user.getName() + " you don't have enough soul of the lost");
                            }
                        } else if (redemption.equalsIgnoreCase("3") || redemption.equalsIgnoreCase("teleport") && !grace) {
                            if (points >= 500 && !timersNotFinished[3]) {
                                Location loc = player.getLocation();
                                int x = (int) (Main.rand.nextDouble()* ((loc.getBlockX() + 1000) - (loc.getBlockX() - 1000)) + (loc.getBlockX() - 1000)), y = (int) (Math.random() * ((loc.getBlockY() + 1000) - (loc.getBlockY() - 1000)) + (loc.getBlockY() - 1000)), z = (int) (Math.random() * ((loc.getBlockZ() + 1000) - (loc.getBlockZ() - 1000)) + (loc.getBlockZ() - 1000));
                                for (int i = 0; i < 200; i++) {
                                    if (player.getWorld().getBlockAt(x, y, z).getType().isAir() && player.getWorld().getBlockAt(x, y + 1, z).getType().isAir() && y > -60 && y < 360)
                                        break;
                                    x = (int) (Main.rand.nextDouble() * ((loc.getBlockX() + 1000) - (loc.getBlockX() - 1000)) + (loc.getBlockX() - 1000));
                                    y = (int) (Main.rand.nextDouble() * ((loc.getBlockY() + 1000) - (loc.getBlockY() - 1000)) + (loc.getBlockY() - 1000));
                                    z = (int) (Main.rand.nextDouble() * ((loc.getBlockZ() + 1000) - (loc.getBlockZ() - 1000)) + (loc.getBlockZ() - 1000));
                                }
                                int finalX = x;
                                int finalY = y;
                                int finalZ = z;
                                plugin.getServer().getScheduler().runTask(plugin, () -> {
                                    if (player.getWorld().getBlockAt(finalX, finalY, finalZ).getType().isAir() && player.getWorld().getBlockAt(finalX, finalY + 1, finalZ).getType().isAir()) {
                                        player.teleport(new Location(player.getWorld(), finalX, finalY, finalZ));
                                    }
                                });
                                if (!removedPoints) {
                                    points -= 500;
                                    removedPoints = true;
                                }
                                timersDelay[3] = 4 * 60;
                            } else if (timersNotFinished[3]) {
                                twitchClient.getChat().sendMessage(chat, "Sorry " + user.getName() + " it's " + timersDelay[3] + " sec left");
                            } else {
                                twitchClient.getChat().sendMessage(chat, user.getName() + " you don't have enough soul of the lost");
                            }
                        } else if (redemption.equalsIgnoreCase("4") || redemption.equalsIgnoreCase("hydrate")) {
                            if (points >= 300 && !timersNotFinished[4]) {
                                twitchClient.getEventManager().publish(new RewardRedeemedEvent(Instant.now(), Factorys.redemptionFactory(user, plugin, redemptions.getString("Hydrate"))));
                                if (!removedPoints) {
                                    points -= 300;
                                    removedPoints = true;
                                }
                                break;
                            } else if (timersNotFinished[4]) {
                                twitchClient.getChat().sendMessage(chat, "Sorry " + user.getName() + " it's " + timersDelay[4] + " sec left");
                            } else {
                                twitchClient.getChat().sendMessage(chat, user.getName() + " you don't have enough soul of the lost");
                            }
                        } else if (redemption.equalsIgnoreCase("5") || redemption.equalsIgnoreCase("hiss")) {
                            if (points >= 50 && !timersNotFinished[5]) {
                                twitchClient.getEventManager().publish(new RewardRedeemedEvent(Instant.now(), Factorys.redemptionFactory(user, plugin, redemptions.getString("hiss"))));
                                if (!removedPoints) {
                                    points -= 50;
                                    removedPoints = true;
                                }
                                break;
                            } else if (timersNotFinished[5]) {
                                twitchClient.getChat().sendMessage(chat, "Sorry " + user.getName() + " it's " + timersDelay[5] + " sec left");
                            } else {
                                twitchClient.getChat().sendMessage(chat, user.getName() + " you don't have enough soul of the lost");
                            }
                        } else if (redemption.equalsIgnoreCase("6") || redemption.equalsIgnoreCase("nut")) {
                            if (points >= 50 && !timersNotFinished[6]) {
                                twitchClient.getEventManager().publish(new RewardRedeemedEvent(Instant.now(), Factorys.redemptionFactory(user, plugin, redemptions.getString("nut"))));
                                if (!removedPoints) {
                                    points -= 50;
                                    removedPoints = true;
                                }
                                break;
                            } else if (timersNotFinished[6]) {
                                twitchClient.getChat().sendMessage(chat, "Sorry " + user.getName() + " it's " + timersDelay[6] + " sec left");
                            } else {
                                twitchClient.getChat().sendMessage(chat, user.getName() + " you don't have enough soul of the lost");
                            }
                        } else if (redemption.equalsIgnoreCase("7") || redemption.equalsIgnoreCase("drop-it")) {
                            if (points >= 100 && !timersNotFinished[7]) {
                                twitchClient.getEventManager().publish(new RewardRedeemedEvent(Instant.now(), Factorys.redemptionFactory(user, plugin, redemptions.getString("Drop it"))));
                                if (!removedPoints) {
                                    points -= 100;
                                    removedPoints = true;
                                }
                                break;
                            } else if (timersNotFinished[7]) {
                                twitchClient.getChat().sendMessage(chat, "Sorry " + user.getName() + " it's " + timersDelay[7] + " sec left");
                            } else {
                                twitchClient.getChat().sendMessage(chat, user.getName() + " you don't have enough soul of the lost");
                            }
                        } else if (redemption.equalsIgnoreCase("8") || redemption.equalsIgnoreCase("mission-failed")) {
                            if (points >= 300 && !timersNotFinished[8]) {
                                twitchClient.getEventManager().publish(new RewardRedeemedEvent(Instant.now(), Factorys.redemptionFactory(user, plugin, redemptions.getString("Mission Failed"))));
                                if (!removedPoints) {
                                    points -= 300;
                                    removedPoints = true;
                                }
                                break;
                            } else if (timersNotFinished[8]) {
                                twitchClient.getChat().sendMessage(chat, "Sorry " + user.getName() + " it's " + timersDelay[8] + " sec left");
                            } else {
                                twitchClient.getChat().sendMessage(chat, user.getName() + " you don't have enough soul of the lost");
                            }
                        }
                    }

                    for (int i = 0; i < timersDelay.length; i++) {
                        if (!timersNotFinished[i] && timersDelay[i] > 0) {
                            timersNotFinished[i] = true;
                        }
                    }

                    instance.subtractPoints(instance.getUserPoints(user.getName()).getUser(), (orgPoints - points));
                }
            }else {
                twitchClient.getChat().sendMessage(chat, "Sorry but "+chat+" don't have StreamElements connected so this feature is disabled");
            }
        }
        else if(command.startsWith("-welcome")){
            if(args.length > 0){
                twitchClient.getChat().sendMessage(chat, "Hello "+args[0]+" This is Yello a plugin made by AlienFromDia, Its made for suffering of the streamer and" +
                        " a sort of player vs streamer. So a basic run thru, Twitch redemptions can d shit, and if the streamer have streamelements connected you can explore the -buy command. I have heard -buy 3 is fun :)");
            }
        }
        else if(command.startsWith("-simping")){
            twitchClient.getChat().sendMessage(chat, "I'm sorry i'm based on luck. but i will change my seed and we can hope for some more fair activity");
            rand.setSeed(System.currentTimeMillis());
        }
        else {
            if (connectChatTwitch && !user.getName().equalsIgnoreCase("StreamElements")) {
                plugin.getServer().sendMessage(MiniMessage.miniMessage().deserialize("<<"+event.getMessageEvent().getTagValue("color").orElse("null")+">"+user.getName() + "<reset>> " + event.getMessage()));
            }
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(label.equalsIgnoreCase("spawn")) {
            if (sender instanceof Player p) {
                int pro = 3;
                if (args.length > 0) {
                    try {
                        pro = Integer.parseInt(args[0]);
                    } catch (Exception ignored) {
                    }
                }
                host = p;
                int finalPro = pro;
                getServer().getScheduler().runTask(this, () -> {
                    Skeleton e = p.getWorld().spawn(p.getLocation(), Skeleton.class, CreatureSpawnEvent.SpawnReason.CUSTOM);
                    SkeletonGoal ai = new SkeletonGoal(this, e);
                    if (!Bukkit.getMobGoals().hasGoal(e, ai.getKey())) {
                        Bukkit.getMobGoals().addGoal(e, finalPro, ai);
                    }
                    friendlyTeam.addEntities(e);
                });
            }
        }
        else if(label.equalsIgnoreCase("run")&&false){
            EventUser user = new EventUser("426779304", "AlienFromDia");
            int i = 0;
            for(String key : plugin.redemptions.keySet()){
                //plugin.twitchClient.getEventManager().publish(new RewardRedeemedEvent(Instant.now().plusSeconds(i), Factorys.redemptionFactory(user, plugin, key)));
                i++;
            }
        }
        /*else if(label.equalsIgnoreCase("connect")) {
            if (args.length > 0) {
                if(sender instanceof Player p || true) {
                    final String chat = args[0];
                    if (twitchClients.stream().filter(pair -> pair.value.equals(chat)).toList().isEmpty()) {
                        TwitchClient twitchClient = TwitchClientBuilder.builder()
                                .withEnableChat(true)
                                .withEnablePubSub(true)
                                .withEnableHelix(true)
                                .withEnableTMI(true)
                                .withChatAccount(credential)
                                .withCredentialManager(credentialManager)
                                .build();

                        User user = twitchClient.getHelix().getUsers(credential.getAccessToken(), null, List.of(chat)).execute().getUsers().get(0);
                        if(user == null){
                            sender.sendMessage("<a_twitch_bot_> I'm sorry but "+chat+" is not a valid twitch user name please make sure you spelled correctly.");
                            return true;
                        }
                        channelId = user.getId();

                        twitchClient.getPubSub().listenForChannelPointsRedemptionEvents(credential, user.getId());
                        twitchClient.getChat().joinChannel(chat);
                        twitchClient.getChat().sendMessage(chat, "I was told to come hear by " + sender.getName() + " treat me well.");
                        twitchClient.getEventManager().onEvent(ChannelMessageEvent.class, e -> onChatMessage(e, new Pair<>(twitchClient, chat)));
                        twitchClient.getEventManager().onEvent(RewardRedeemedEvent.class, e -> onRedemption(e, new Pair<>(twitchClient, chat)));
                        sender.getServer().sendMessage(Component.text("<a_twitch_bot_> Bot connected to " + chat + " stream."));
                        isConnected = true;
                        //host = p;

                        if(credentials.getJSONObject("SE").has(chat.toLowerCase())) {
                            StreamElements instance = new StreamElementsBuilder()
                                    .withAccountID(credentials.getJSONObject("SE").getString(chat.toLowerCase()))
                                    .withToken(credentials.getJSONObject("SE").getString("token"))
                                    .withConnectionTimeout(10000)
                                    .build();
                            SEInterfaces.add(new Pair<>(chat, instance));
                        }
                        if(credentials.getJSONObject("SL").has(chat.toLowerCase())){
                            //StreamlabsApi api = StreamlabsApiBuilder.builder()
                            //        .withClientId("")
                            //        .build();
                        }

                        twitchClients.add(new Pair<>(twitchClient, chat));
                    } else {
                        sender.sendMessage("<a_twitch_bot_> I'm already connected to "+chat+"'s stream. Use /disconnect first.");
                    }
                }else{
                    sender.sendMessage("<a_twitch_bot_> I'm Sorry but i require that this command should be run by a player.");
                }
                return true;
            }
        }*/
        /*else if(label.equalsIgnoreCase("disconnect")) {
            if(args.length > 0){
                final String chat = args[0];
                List<Pair<TwitchClient, String>> pairList = twitchClients.stream().filter(pair -> pair.value.equals(chat)).toList();
                if(!pairList.isEmpty()){
                    Pair<TwitchClient, String> pair = pairList.get(0);
                    TwitchClient twitchClient = pair.key;
                    twitchClient.getChat().sendMessage(chat, "I was told to leave now so bye!");
                    twitchClient.getChat().disconnect();
                    twitchClient.close();
                }
            }else{
                for(Pair<TwitchClient, String> pair : twitchClients){
                    pair.key.getChat().sendMessage(pair.value, "I was told to leave now so bye!");
                    pair.key.close();
                }
            }
            return true;
        }*/
        /*else if(label.equalsIgnoreCase("send")) {
            if (args.length > 0) {
                if(!twitchClients.isEmpty()) {
                    for (Pair<TwitchClient, String> twitchPair : twitchClients) {
                        TwitchClient twitchClient = twitchPair.key;
                        String chat = twitchPair.value;
                        StringBuilder message = new StringBuilder();


                        for (String arg : args) {
                            message.append(arg).append(" ");
                        }

                        twitchClient.getChat().sendMessage(chat, "<" + sender.getName() + "> " + message);
                        getServer().sendMessage(Component.text("<" + sender.getName() + "> " + message));
                    }
                }else{
                    sender.sendMessage(Component.text("<a_twitch_bot_> ").append(MiniMessage.miniMessage().deserialize("<red><bold>Sorry but I'm not connected to a Twitch chat use /connect <twitch streamer>")));
                }
            }

            getServer().sendMessage(Component.text("<a_twitch_bot_> missing parameters usage /send <text to send>"));

            return true;
        }*/
        /*else if(label.equalsIgnoreCase("chat")) {
            if(args.length > 0) {
                if (!twitchClients.isEmpty()) {
                    for (Pair<TwitchClient, String> twitchPair : twitchClients) {
                        TwitchClient twitchClient = twitchPair.key;
                        String chat = twitchPair.value;
                        if (args[0].equalsIgnoreCase("twitch")) {
                            connectChatTwitch = !connectChatTwitch;
                            if (connectChatTwitch) {
                                getServer().sendMessage(Component.text("<a_twitch_bot_> the twitch chat is now connected to minecraft chat"));
                                twitchClient.getChat().sendMessage(chat, "twitch chat is now connected to minecraft chat");
                            } else {
                                getServer().sendMessage(Component.text("<a_twitch_bot_> the twitch chat is now disconnected to minecraft chat"));
                                twitchClient.getChat().sendMessage(chat, "twitch chat is now disconnected to minecraft chat");
                            }
                        } else if (args[0].equalsIgnoreCase("minecraft")) {
                            connectChatMinecraft = !connectChatMinecraft;
                            if (connectChatMinecraft) {
                                getServer().sendMessage(Component.text("<a_twitch_bot_> the minecraft chat is now connected to twitch chat"));
                                twitchClient.getChat().sendMessage(chat, "minecraft chat is now connected to twitch chat");
                            } else {
                                getServer().sendMessage(Component.text("<a_twitch_bot_> the minecraft chat is now disconnected to minetwitchcraft chat"));
                                twitchClient.getChat().sendMessage(chat, "minecraft chat is now disconnected to twitch chat");
                            }
                            if (args.length > 1) {
                                if (args[1].equalsIgnoreCase("all")) {
                                    minecraftChat = Level.valueOf(args[1].toUpperCase());
                                } else if (args[1].equalsIgnoreCase("info")) {
                                    minecraftChat = Level.valueOf(args[1].toUpperCase());
                                } else if (args[1].equalsIgnoreCase("chat")) {
                                    minecraftChat = Level.valueOf(args[1].toUpperCase());
                                }
                            }
                        }
                    }
                }
            }
            else{
                sender.sendMessage("<a_twitch_bot_> you ar missing parameters use /chat <twitch/minecraft> [all/info/chat note only works for mc chat and it defaults to all]");
            }

            return true;
        }*/
        /*else if(label.equalsIgnoreCase("grace")) {
            if(isConnected){
                if(args.length > 0){
                    try {
                        graceTime = Integer.parseInt(args[0])*60;
                        graceTimeOrig = graceTime;
                        grace = true;
                    }catch (Exception e){
                        sender.sendMessage("<a_twitch_bot_> first argument was not an int pleas provide <time in min> for the first argument");
                    }
                }else{
                    sender.sendMessage("<a_twitch_bot_> first argument was not an found pleas provide <time in min> for the first argument");
                }
            }
            return true;
        }*/
        else if(label.equalsIgnoreCase("points") && false){
            try {
                URL url = new URL("https://api.streamelements.com/kappa/v2/points/"+credentials.getJSONObject("SE").getString("channel")+"/top/");
                URLConnection con = url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuilder builder = new StringBuilder();
                String tmp = "";
                while ((tmp = reader.readLine()) != null) {
                    builder.append(tmp);
                }
                JSONObject response = new JSONObject(builder.toString());
                Map<String, Long> map = new HashMap<>();
                for (Object obj : response.getJSONArray("users").toList()) {
                    if (obj instanceof HashMap user) {
                        map.put((String) user.get("username"), ((Integer)user.get("points")).longValue());
                    } else {
                        System.out.println(obj.getClass());
                    }
                }
                TreeMap<String, Long> viewerPointsSorted = new TreeMap<>(new ValueComparator(map));
                viewerPointsSorted.putAll(map);
                ArrayList<Pair<String, Long>> topViewerPoints = new ArrayList<>();
                int i = 0;
                for(String key : viewerPointsSorted.keySet()){
                    if(i > 9) break;
                    //topViewerPoints.add(i, new Pair<>(key, instance.getUserPoints(key).getCurrentPoints()));
                    i++;
                }
                StringBuilder str = new StringBuilder();
                str.append("Top ").append(i).append(" soul of the lost user").append((i>1?"s":""));
                i = 0;
                for(Pair<String, Long> pair : topViewerPoints){
                    Long points = pair.getValue();
                    String user = pair.getKey();
                    str.append("\n").append("#").append(i+1).append(" ").append(user).append(" ").append((points==null?"null":points)).append(" Soul").append(((points==null?0:points)>1?"s":"")).append(" of the lost");
                    i++;
                }
                sender.sendMessage("<a_twitch_bot_> "+str.toString());
            }catch (Exception e1){
                e1.printStackTrace();
            }
            /*
            TreeMap<String, Integer> viewerPointsSorted = new TreeMap<>(new ValueComparator(viewerPoints));
            viewerPointsSorted.putAll(viewerPoints);
            ArrayList<Pair<String, Integer>> topViewerPoints = new ArrayList<>();
            int i = 0;
            for(String key : viewerPointsSorted.keySet()){
                if(i > 9) break;
                User user = twitchClient.getHelix().getUsers(credential.getAccessToken(), Arrays.asList(key), null).execute().getUsers().get(0);
                if(user == null) break;
                topViewerPoints.add(i, new Pair<>(user.getDisplayName(), viewerPoints.get(key)));
                i++;
            }
            StringBuilder str = new StringBuilder();
            if(topViewerPoints.size() > 0) {
                str.append("Top ").append(topViewerPoints.size()).append(" soul of the lost user").append((topViewerPoints.size()>1?"s":""));
                for(i = topViewerPoints.size()-1; i >= 0; i--){
                    Pair<String, Integer> user = topViewerPoints.get(i);
                    str.append("\n").append(user.getKey()).append(" have ").append((user.getValue()==null?"null":user.getValue())).append(" Soul").append(((user.getValue()==null?0:user.getValue())>1?"s":"")).append(" of the lost");
                }
            }
            sender.sendMessage("<a_twitch_bot_> "+str.toString());
            */
        }
        else if(label.equalsIgnoreCase("start-events")){
            randomEvent.start();
            return true;
        }
        else if(label.equalsIgnoreCase("force-event")) {
            synchronized (randomEvent){
                if(args.length > 0){
                    randomEvent.forceEvent = Integer.parseInt(args[0]);
                }
                randomEvent.notify();
            }
            return true;
        }
        else if(label.equalsIgnoreCase("combat")){
            combat = !combat;
            sender.sendMessage("Custom combat is now "+(combat?"enabled":"disable"));
            return true;
        }
        else if(label.equalsIgnoreCase("test")){
            if(sender instanceof Player p){
                try{
                    int i = Integer.parseInt(args[0]);
                    ItemStack stack = new ItemStack(Material.DIAMOND_SWORD);
                    Base.handle(stack, i);
                    p.getInventory().addItem(stack);
                }catch (Exception ignored){}
            }
        }
        /*else if(label.equalsIgnoreCase("stats")){
            if(sender instanceof Player p){
                List<Pair<UUID, Map<String, Integer>>> stats = new ArrayList<>(Main.stats.stream().filter((pair) -> pair.key.equals(p.getUniqueId())).toList());
                if(stats.isEmpty()){
                    Map<String, Integer> stats1 = new HashMap<>();
                    stats1.put("str", toMod(rand.nextInt(20)));
                    stats1.put("dex", toMod(rand.nextInt(20)));
                    stats1.put("int", toMod(rand.nextInt(20)));
                    stats1.put("con", toMod(rand.nextInt(20)));
                    stats1.put("cha", toMod(rand.nextInt(20)));
                    stats1.put("wiz", toMod(rand.nextInt(20)));
                    Pair<UUID, Map<String, Integer>> pair = new Pair<>(p.getUniqueId(), stats1);
                    Main.stats.add(pair);
                    stats.add(pair);
                }
                PrintHandler out = p::sendMessage;
                for(Map.Entry<String, Integer> pair : stats.get(0).value.entrySet()){
                    out.print(pair.getKey() + ": " + pair.getValue());
                }
            }
        }*/
        return false;
    }
}