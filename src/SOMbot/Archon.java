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
                broadcastHandle.reportExistence();
                reportTrees();
                spawnGardener();
                // Move randomly
                tryMove(randomDirection());
                reportClosestEnemy();
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

}