package com.github.raiccoon;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.permission.Role;

import java.awt.*;
import java.util.*;
import java.util.List;

import static java.lang.String.join;

public class Main {
    public static Map<String, String[]> botmap = new HashMap<>(){{
        put("dishabot", new String[] {"h!", "140","158","255", "NzI4MzgwMTk3MjM3MTYxOTg2.Xv5i6w.pJ4pwW5FDWdOF5WNeCnIyvcmb84", "728380197237161986"});
    }};

    /*public static String botname = "eddy";

    public static String[] botvar = botmap.get(botname);

    public static String prefix = botvar[0];

    public static Color color = new Color(Integer.parseInt(botvar[1]), Integer.parseInt(botvar[2]), Integer.parseInt(botvar[3])); // ed.dy

    public static String token = botvar[4];

    public static long botID = Long.parseLong(botvar[5]);

    public static DiscordApi api = new DiscordApiBuilder().setToken(token).login().join();*/

    // variables for stopwatch
    public static ArrayList<Stopwatch> watches = new ArrayList<>();
    public static ArrayList<Long> watchIds = new ArrayList<>();

    // variable for breakout room
    public static ArrayList<BreakoutRooms> breakoutRoomList = new ArrayList<>();

    // variables for games
    public static ArrayList<Game> gamesList = new ArrayList<>();
    public static ArrayList<Game> liveGamesList = new ArrayList<>();

    public static Map<String, String> aliases = new HashMap<>(){{
        put("calc","calculate");
        put("del_selfrole", "delete_selfrole");
        put("dy", "about");
        put("games","game");
        put("q", "question");
        put("rr", "reaction_role");
        put("remind", "remindme");
        put("sc", "shortcut");
        put("welcmsg", "welcomemsg");
    }};

    public static Map<String, msgCommand> msgCommands = new HashMap<>(){{
        put("about", new msgAbout());
        put("add_selfrole", new msgAddSelfRole());
        put("announce", new msgAnnouncement());
        put("breakout", new msgBreakoutRooms());
        put("calculate", new msgCalculate());
        put("create", new msgCreate());
        put("define", new msgDefine());
        put("delete", new msgDelete());
        put("delete_selfrole", new msgDeleteSelfRole());
        put("disable", new msgDisable());
        put("enable", new msgEnable());
        put("game", new msgGame());
        put("help", new msgHelp());
        put("ping", new msgPing());
        put("poll", new msgPoll());
        put("question", new msgQuestion());
        put("reaction_role", new msgReactionRole());
        put("remindme", new msgRemind());
        put("selfrole", new msgSelfRole());
        put("selfroles", new msgSelfRoles());
        put("shortcut", new msgShortcut());
        put("stat", new msgStat());
        put("stopwatch", new msgStopwatch());
        put("suggest", new msgSuggest());
        put("welcomemsg", new msgWelcomeMsg());
        put("when2meet", new msgWhen2Meet());
    }};

    public static Map<String, reactCommand> reactCommands = new HashMap<>(){{
        put("stopwatch", new reactStopwatch());
        put("breakoutrooms",new reactBreakoutRooms());
        put("games", new reactGames());
    }};

    public static Map<String, removeReactCommand> removeReactCommands = new HashMap<>(){{
        put("breakoutrooms", new removeReactBreakoutRooms());
    }};

    public static void main(String[] args) {
        // create eddys databases, only needs to be done once
        // dbSetUp.createDatabase(api);
        // dbSetUp.addColumn("COMMANDS", "'command name'");

        api.updateActivity(prefix + "help");

        api.addMessageCreateListener(event -> {
            Message message = event.getMessage();
            if (message.getAuthor().isBotUser()) return;
            if (message.getAuthor().isWebhook()) return;

            // increase message count in user database
            String userid = message.getServer().get().getIdAsString() + ", "+ message.getUserAuthor().get().getIdAsString();
            Integer msgcount = dbUser.queryInt(userid, "MSGNUM");
            if (msgcount < 0) {
                dbUser.insertData(userid, 0, new ArrayList<String>());
                msgcount = 0;
            }
            dbUser.editData(userid, "MSGNUM", msgcount + 1);

            // check if interacting with bot
            if (!message.getContent().toLowerCase().startsWith(prefix)) return;

            String content = message.getContent();
            String[] strings = content.substring(prefix.length()).trim().split("\\s+");

            // games handler
            boolean ignoreGame = true;
            if(!liveGamesList.isEmpty()) {
                for (Game game : liveGamesList) {
                    if (game.originalmsg.getChannel().equals(message.getChannel())){
                        if (game.activePlayers.contains(message.getUserAuthor().get())) {
                            game.newMsg(message);
                            ignoreGame = false;
                        }
                        else if (game.gameType.equals(msgGame.GameType.MULTIPLAYER)
                                && message.getContent().substring(prefix.length()).toLowerCase().trim().equals("play")){
                            game.activePlayers.add(message.getUserAuthor().get());
                            game.newMsg(message);
                            ignoreGame = false;
                        }
                    }
                }
            }
            if(!ignoreGame)
                return;

            // if shortcut, alter message content to match new command
            if ((strings[0].contentEquals("shortcut") || strings[0].contentEquals("sc")) && strings.length >= 2){
                if(helper.isInteger(strings[1])){
                    content = msgShortcut.shortcut(message, Integer.parseInt(strings[1]));
                    message.getChannel().sendMessage("Executing shortcut " + strings[1] + ": " + content);

                    strings = content.substring(prefix.length()).trim().split(" ");
                }
            }

            String commandName = strings[0].toLowerCase();

            // check if commandName is alias
            if (aliases.get(commandName) != null){
                commandName = aliases.get(commandName);
            }

            // if command disabled, return
            if (dbCommands.queryString(message.getServer().get().getIdAsString(), commandName) != null) {
                if (dbCommands.queryString(message.getServer().get().getIdAsString(), commandName).contentEquals("disabled")){
                    System.out.println("command disabled");
                    return;
                }
            }

            String[] arguments = Arrays.copyOfRange(strings, 1, strings.length);
            String string = join(" ", arguments);

            if (msgCommands.get(commandName) == null) {
                message.getChannel().sendMessage("This is not a valid command.");
                return;
            }

            msgCommands.get(commandName).command(message, arguments, string);

        });

        // reactions on messages from bot & contains command phrase -> run react command
        api.addReactionAddListener(event -> {
            if(event.getMessage().isEmpty()) return;
            Message message = event.getMessage().get();
            if(event.getUser().isBot()) return;

            if(message.getContent().contains("stopwatch")) {
                reactCommands.get("stopwatch").react(event);
            }
                reactCommands.get("breakoutrooms").react(event);

                reactCommands.get("games").react(event);

        });

       // removal event reactions on messages from bot & contains command phrase -> run react command
        api.addReactionRemoveListener(event -> {
            for(BreakoutRooms rooms : breakoutRoomList) {
                if (rooms.getIsSchedulingRoom()) {
                    removeReactCommands.get("breakoutrooms").react(event);
                }
            }
        });



        // new server -> adds entry to server table
        api.addServerJoinListener(event -> {
            dbServer.insertData(event.getServer().getIdAsString(), new ArrayList<>());
        });

        // new user -> checks if server has a welcome message
        api.addServerMemberJoinListener(event -> {
            String server_id = event.getServer().getIdAsString();
            String welc_channel = dbServer.queryString(server_id, "WELCCHANNEL");
            String welc_message = dbServer.queryString(server_id, "WELCMSG");

            if (welc_message == null || welc_channel == null) return;

            event.getServer().getTextChannelById(welc_channel).get().sendMessage(event.getUser().getMentionTag() + ", " + welc_message);


        });

        // role deletion -> removes role from selfroles
        api.addRoleDeleteListener(event -> {
            String serverid = event.getServer().getIdAsString();
            Role role = event.getRole();

            List<String> self_role_list = dbServer.queryList(serverid,"SELFROLEIDS");

            if(self_role_list.contains(role.getIdAsString())){
                self_role_list.remove(role.getIdAsString());
                dbServer.editData(serverid, "SELFROLEIDS", self_role_list);
            }
        });

        // Print the invite url of your bot
        System.out.println("Bot invite url: " + api.createBotInvite(Permissions.fromBitmask(2080828528)));

    }

}
