package SOMbot;
import battlecode.common.*;


public class Lumberjack extends Unit{
    int giveUpOnPrioInit = 15;
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
                    giveUpLeader();
                    if(isLeader)
                        doLeaderStuff();
                    else
                        takeUpLeader();
                    reportTrees();
                    broadcastHandle.reportExistence();
                    MapLocation[] nestTrees = broadcastHandle.getPriorityTrees();
                    int nestTreesCount = nestTreesCount(nestTrees);
                    TreeInfo[] trees = rc.senseNearbyTrees(-1,Team.NEUTRAL);
                    RobotInfo[] enemies = rc.senseNearbyRobots(-1,rc.getTeam().opponent());
                    RobotInfo[] friends = rc.senseNearbyRobots(-1,rc.getTeam());

                   
                    if(enemies.length > friends.length+1)
                        flee();
                    else if(enemies.length > 0)
                        attack(enemies, friends);
                    else if(nestTreesCount > 0 && !ignorePrio)
                        handleNestTrees(nestTrees);
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

        void handleNestTrees(MapLocation[] trees) throws GameActionException{
            TreeInfo[] normalTrees = rc.senseNearbyTrees(-1,Team.NEUTRAL);
            MapLocation closestTree = nullMap();
            float currMin = Float.MAX_VALUE;
            for (MapLocation tree : trees){
                if (isValid(tree) && rc.getLocation().distanceTo(tree) < currMin){
                    closestTree = tree;
                    currMin = rc.getLocation().distanceTo(tree);
                }
            }
            if(!isValid(closestTree))
                return;
            if(rc.canShake(closestTree) && rc.canSenseLocation(closestTree) && rc.senseTreeAtLocation(closestTree).containedBullets > 0 )
                rc.shake(closestTree);

            
            if(rc.canChop(closestTree)){
                giveUpOnPriority = giveUpOnPrioInit;
                rc.chop(closestTree);
                
            }
            else if(rc.hasAttacked())
                return;
            else{
                reducePriority(); //Maybe add range to this
                if(normalTrees.length > 0 && rc.canChop(normalTrees[0].ID))
                    rc.chop(normalTrees[0].ID);
                pathTo(closestTree);
            }
            if(rc.canSenseAllOfCircle(closestTree, GameConstants.NEUTRAL_TREE_MIN_RADIUS/2)){
                TreeInfo[] choppedTree = rc.senseNearbyTrees(closestTree, GameConstants.NEUTRAL_TREE_MIN_RADIUS/2, Team.NEUTRAL);
                if(choppedTree.length == 0){
                    broadcastHandle.resetTree(closestTree);
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
                if (tree.x > 0f && tree.y > 0f)
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
            RobotInfo[] strikeFriends = rc.senseNearbyRobots( GameConstants.LUMBERJACK_STRIKE_RADIUS, rc.getTeam());
            int chopTrees = 0;
            for(TreeInfo tree : trees){
                if (tree.getLocation().distanceTo(rc.getLocation()) <  GameConstants.LUMBERJACK_STRIKE_RADIUS)
                    chopTrees++;
            }
            if(chopTrees > 3 && strikeFriends.length == 0 && rc.canStrike()){
                rc.strike();
            }
            if (trees.length > 0){
                for(TreeInfo tree : trees){
                    if(tree.containedBullets > 0 && rc.canShake(tree.ID))
                        rc.shake(tree.ID);
                }
                if(rc.canChop(trees[0].ID)){
                    MapLocation chopped = trees[0].location;
                    rc.chop(trees[0].ID);
                    TreeInfo[] choppedTree = rc.senseNearbyTrees(chopped, GameConstants.NEUTRAL_TREE_MIN_RADIUS/2, Team.NEUTRAL);
                    if(choppedTree.length == 0 ){
                        ignorePrio = false;
                    }
                    
                }
                else if(rc.canInteractWithTree(trees[0].ID))
                    return;
                else{
                    pathTo(trees[0].getLocation());
                }

            }
            return;
        }
    }