package SOMbot;
import battlecode.common.*;


public class Lumberjack extends Unit{
    int giveUpOnPrioInit = 10;
    int giveUpOnPriority;
    boolean ignorePrio = false;
    public Lumberjack(RobotController rc){
        super(rc);
        giveUpOnPriority = giveUpOnPrioInit;
    }

    @Override 
    public void run() {
            Team enemy = rc.getTeam().opponent();

            // The code you want your robot to perform every round should be in this loop
            while (true) {

                // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
                try {
                    broadcastHandle.reportExistence();
                    MapLocation[] nestTrees = broadcastHandle.getNestPriorityTrees();
                    int nestTreesCount = nestTreesCount(nestTrees);
                    TreeInfo[] trees = rc.senseNearbyTrees(-1,Team.NEUTRAL);
                    RobotInfo[] enemies = rc.senseNearbyRobots(-1,rc.getTeam().opponent());
                    RobotInfo[] friends = rc.senseNearbyRobots(-1,rc.getTeam());

                   
                    if(enemies.length > friends.length)
                        flee();
                    else if(trees.length == 0 && enemies.length > 0)
                        attack(enemies, friends);
                    else if(nestTreesCount > 0 && !ignorePrio)
                        handleNestTrees(nestTrees);
                    if(trees.length > 0)
                        handleClosestTrees(trees);
                    else
                        wanderingRumba();
                    
                    
                    if(ignorePrio)
                        reducePriority();

                    // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                    Clock.yield();

                } catch (Exception e) {
                    System.out.println("Lumberjack Exception");
                    e.printStackTrace();
                }
            }
        }

        void handleNestTrees(MapLocation[] trees) throws GameActionException{
            MapLocation closestTree = new MapLocation(-1f,-1f);
            float currMin = Float.MAX_VALUE;
            for (MapLocation tree : trees){
                if (tree.x != -1f && tree.y != -1f && rc.getLocation().distanceTo(tree) < currMin){
                    closestTree = tree;
                    currMin = rc.getLocation().distanceTo(tree);
                }
            }
            if(closestTree.x == -1f && closestTree.y == -1f)
                return;
            if(rc.canShake(closestTree) && rc.senseTreeAtLocation(closestTree).containedBullets > 0 )
                rc.shake(closestTree);

            //rc.setIndicatorDot(closestTree,0,255,0);
            
            if(rc.canChop(closestTree)){
                giveUpOnPriority = giveUpOnPrioInit;
                rc.chop(closestTree);
                
            }
            else if(rc.hasAttacked())
                return;
            else{
                if(rc.getLocation().distanceTo(closestTree) < type.bodyRadius * 10)
                    reducePriority();
                pathTo(closestTree);
            }
            if(rc.canSenseAllOfCircle(closestTree, GameConstants.NEUTRAL_TREE_MIN_RADIUS)){
                TreeInfo[] choppedTree = rc.senseNearbyTrees(closestTree, GameConstants.NEUTRAL_TREE_MIN_RADIUS, Team.NEUTRAL);
                if(choppedTree.length == 0){
                    broadcastHandle.resetNestTree(closestTree);
                }
            }

        


        }

        void reducePriority(){
            giveUpOnPriority--;
            if(giveUpOnPriority < 0){
                giveUpOnPriority = giveUpOnPrioInit;
                ignorePrio = !ignorePrio;
            }
        }

        int nestTreesCount(MapLocation[] nestTrees){
            int count = 0;
            for(MapLocation tree : nestTrees){
                if (tree.x != -1f && tree.y != -1f)
                    count++;
            }
            return count;

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