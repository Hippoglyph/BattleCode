package SOMbot;
import battlecode.common.*;


public class Lumberjack extends Unit{
    public Lumberjack(RobotController rc){
        super(rc);
    }

    @Override 
    public void run() {
            Team enemy = rc.getTeam().opponent();

            // The code you want your robot to perform every round should be in this loop
            while (true) {

                // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
                try {
                    broadcastHandle.reportExistence();
                    TreeInfo[] trees = rc.senseNearbyTrees(-1,Team.NEUTRAL);
                    RobotInfo[] enemies = rc.senseNearbyRobots(-1,rc.getTeam().opponent());
                    RobotInfo[] friends = rc.senseNearbyRobots(-1,rc.getTeam());
                   
                    if(enemies.length > friends.length)
                        flee();
                    else if(trees.length == 0 && enemies.length > 0)
                        attack(enemies, friends);
                    else if(trees.length > 0)
                        handleClosestTrees(trees);
                    else
                        wanderingRumba();
                    
                    
                    

                    // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                    Clock.yield();

                } catch (Exception e) {
                    System.out.println("Lumberjack Exception");
                    e.printStackTrace();
                }
            }
        }

        void flee() throws GameActionException{
            pathTo(friendlySpawn);
        }

        void attack(RobotInfo[] enemies, RobotInfo[] friends) throws GameActionException{
            int enemiesInRange = 0;
            int friendsInRange = 0;
            for (RobotInfo enemy : enemies){
                if(enemy.getLocation().distanceTo(rc.getLocation()) < RobotType.LUMBERJACK.bodyRadius + GameConstants.LUMBERJACK_STRIKE_RADIUS)
                    enemiesInRange++;
            }
            for (RobotInfo friend : friends){
                if(friend.getLocation().distanceTo(rc.getLocation()) < RobotType.LUMBERJACK.bodyRadius + GameConstants.LUMBERJACK_STRIKE_RADIUS)
                    friendsInRange++;
            }
            if(rc.canStrike() && enemiesInRange > friendsInRange)
                rc.strike();

            pathTo(enemies[0].getLocation());
            return;
        }

        void handleClosestTrees(TreeInfo[] trees) throws GameActionException{
            RobotInfo[] strikeFriends = rc.senseNearbyRobots(RobotType.LUMBERJACK.bodyRadius + GameConstants.LUMBERJACK_STRIKE_RADIUS, rc.getTeam());
            int chopTrees = 0;
            for(TreeInfo tree : trees){
                if (tree.getLocation().distanceTo(rc.getLocation()) < RobotType.LUMBERJACK.bodyRadius + GameConstants.LUMBERJACK_STRIKE_RADIUS)
                    chopTrees++;
            }
            if(chopTrees > 0 && strikeFriends.length == 0 && rc.canStrike())
                rc.strike();
            if (trees.length > 0){
                for(TreeInfo tree : trees){
                    if(tree.containedBullets > 0 && rc.canShake(tree.ID))
                        rc.shake(tree.ID);
                }
                if(rc.canChop(trees[0].ID))
                    rc.chop(trees[0].ID);
                else if(rc.hasAttacked())
                    return;
                else
                    pathTo(trees[0].getLocation());
            }
            return;
        }
    }