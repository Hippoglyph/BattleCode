package ultrabot;
import battlecode.common.*;

public strictfp class RobotPlayer {
    public static void run(RobotController rc){
        try{
            // start the bot
            Bot.init(rc);
            Bot bot = Bot.getBotByType(rc.getType());
            bot.startRun();

        } catch(Exception e){
            System.err.println("Could not start bot type");
        }
    }
}