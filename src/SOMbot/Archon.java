package SOMbot;
import battlecode.common.*;

public class Archon extends Unit{
    boolean isLeader = false;
    boolean spawnSoldiers = false;
    boolean spawnLumberjacks = false;
    boolean spawnGardeners = false;
    boolean spawnScouts = false;
    boolean spawnTanks = false;
	public Archon(RobotController rc){
        super(rc);
        float closestDist = Float.MAX_VALUE;
        int closestIndex = 0;
        MapLocation[] archonLocations = rc.getInitialArchonLocations(rc.getTeam());
        for(int i =0; i < archonLocations.length; i++){
            if(rc.getLocation().distanceTo(archonLocations[i]) < closestDist ){
                closestIndex = i;
                closestDist = rc.getLocation().distanceTo(archonLocations[i]);
            }
        }
        if(closestIndex == 0){
            isLeader = true;
        }


    }


    @Override
	public void run()  {
         try {
        // The code you want your robot to perform every round should be in this loop
            while (true) {
                // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
                if(isLeader){
                    doLeaderStuff();
                }
                reportClosestEnemy();
                broadcastHandle.reportExistence();
                reportTrees();
                spawnGardener();
                reportEmptyArea();
                // Move randomly
                RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
                TreeInfo[] friendlyTrees = rc.senseNearbyTrees(-1, rc.getTeam());
                if(friendlyTrees.length == 0 || enemies.length > 0){
                    pathTo(friendlySpawn);
                    wanderingDir = randomDirection();
                }
                else{
                    wanderingRumba();
                }
                /*
                MapLocation nest = broadcastHandle.getNestLocation();
                if (isValid(nest))
                    rc.setIndicatorDot(nest, 255, 0, 0);
                */

                /*
                MapLocation[] prioTrees = broadcastHandle.getPriorityTrees();
                for (MapLocation tree : prioTrees){
                    if (isValid(tree))
                        rc.setIndicatorDot(tree, 255, 0, 0);
                }
                

                MapLocation[] prioTargets = broadcastHandle.getPriorityTargets();
                int count = 0;
                for (MapLocation target : prioTargets){
                    if (isValid(target)){
                        count++;
                        rc.setIndicatorDot(target, count*255/5, 0, 0);
                    }
                }
                 */
                //System.out.println(count);

                // Broadcast archon's location for other robots on the team to know
                

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } 
        }
        catch (Exception e) {
            System.out.println("Archon Exception");
            e.printStackTrace();
        }
    }


    private void doLeaderStuff() throws GameActionException{
        int soldierCount = broadcastHandle.getCount(RobotType.SOLDIER);
        int lumberjackCount = broadcastHandle.getCount(RobotType.LUMBERJACK);
        int gardenerCount = broadcastHandle.getCount(RobotType.GARDENER);
        int scoutCount = broadcastHandle.getCount(RobotType.SCOUT);
        int tankCount = broadcastHandle.getCount(RobotType.TANK);
        int notFoundNest = broadcastHandle.getNotFoundNest();
        int trees = broadcastHandle.getTrees();
        int unitCount = soldierCount + lumberjackCount + gardenerCount + scoutCount + tankCount;
        if(unitCount == 0)
            unitCount = 1;
        float treeUnitRatio = (float)trees/unitCount;
        float bullets = rc.getTeamBullets();

        if(bullets / rc.getVictoryPointCost() >= GameConstants.VICTORY_POINTS_TO_WIN - rc.getTeamVictoryPoints()){
            rc.donate(bullets);
            return;
        }

        if(notFoundNest < 1 && (bullets >= 350) || gardenerCount < 1){
            if(!spawnGardeners){
                broadcastHandle.spawn(RobotType.GARDENER,true);
                spawnGardeners = true;
            }
        }
        else{
            //do something smart
            if(spawnGardeners){
                broadcastHandle.spawn(RobotType.GARDENER,false);
                spawnGardeners = false;
            }
        }

        if(soldierCount < gardenerCount){
            if(!spawnSoldiers){
                broadcastHandle.spawn(RobotType.SOLDIER, true);
                spawnSoldiers = true;
            }
            
        }
        else {
           // System.out.println(soldierCount);
            if(spawnSoldiers){
                broadcastHandle.spawn(RobotType.SOLDIER,false);
                spawnSoldiers = false;
            }
            
        }



        if((treeUnitRatio > 5f && 0.6f > (float)lumberjackCount/unitCount)|| (lumberjackCount < 1 && trees > 0)){
            if(!spawnLumberjacks){
                broadcastHandle.spawn(RobotType.LUMBERJACK, true);
                spawnLumberjacks = true;
            }
        }
        else {
            if(spawnLumberjacks){
                broadcastHandle.spawn(RobotType.LUMBERJACK,false);
                spawnLumberjacks = false;  
            }
            
        }

        if(gardenerCount > 0 && bullets > gardenerCount*RobotType.SOLDIER.bulletCost*2 && !spawnLumberjacks && !spawnSoldiers && bullets > 1000){
            float bulletsPerPoint = rc.getVictoryPointCost();
            float maxBullets = 100;
            int donation = (int)(maxBullets/bulletsPerPoint);
            rc.donate(donation * bulletsPerPoint);
        }


        broadcastHandle.resetUnitCounts();
    }

    private void spawnGardener() throws GameActionException{
        boolean shouldSpawnGardener = broadcastHandle.shouldSpawnRobot(RobotType.GARDENER);
        if(shouldSpawnGardener){
            for (float i = 0; i < (float)Math.PI*2; i+=Math.PI/4){
                Direction dir = new Direction(i);
                if (rc.canHireGardener(dir)) {
                    rc.hireGardener(dir);
                    break;
                }
            }

        }
    }

    void reportEmptyArea() throws GameActionException{
        resetInvalidNest();
        MapLocation best = broadcastHandle.getNestLocation();
        float curDist = Float.MAX_VALUE;
        boolean foundBetter = false;
        boolean prior = true;
        if(isValid(best))
            curDist = best.distanceTo(friendlySpawn);
        else
            prior = false;

        for(int i = 0; i < 20; i++){
            float angle = (float)(Math.random() * Math.PI * 2);
            float dist = (float)Math.random() * (type.sensorRadius - nestRange);
            MapLocation sample = rc.getLocation().add(new Direction(angle),dist);
            TreeInfo[] trees = rc.senseNearbyTrees(sample,nestRange,rc.getTeam());
            RobotInfo[] guardeners = rc.senseNearbyRobots(sample,nestRange,rc.getTeam());
            int gardeners = 0;
            for(RobotInfo gard : guardeners){
                if(gard.getType() == RobotType.GARDENER)
                    gardeners++;
            }
            if(trees.length == 0 && gardeners == 0 && rc.onTheMap(sample,nestRange) && curDist > sample.distanceTo(friendlySpawn) ){
                best = sample;
                curDist = sample.distanceTo(friendlySpawn);
                foundBetter = true;
            }
        }
        if(foundBetter){
            if(prior)
                broadcastHandle.resetNestLocation();
            broadcastHandle.reportNestLocation(best);
        }
    }

}