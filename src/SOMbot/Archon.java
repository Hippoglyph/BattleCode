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
                // Generate a random direction
                Direction dir = randomDirection();

                // Randomly attempt to build a gardener in this direction
                if (rc.canHireGardener(dir) && Math.random() < .01) {
                    rc.hireGardener(dir);
                }

                // Move randomly
                tryMove(randomDirection());

                // Broadcast archon's location for other robots on the team to know
                MapLocation myLocation = rc.getLocation();
                rc.broadcast(0,(int)myLocation.x);
                rc.broadcast(1,(int)myLocation.y);

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


        if(soldierCount < 5){
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



        if(lumberjackCount < 4 ){
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




        broadcastHandle.resetUnitCounts();
    }

}