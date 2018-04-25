package SOMbot;
import battlecode.common.*;

public class Archon extends Unit{
    
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
                giveUpLeader();
                if(isLeader)
                    doLeaderStuff();
                else
                    takeUpLeader();
                reportClosestEnemy();
                broadcastHandle.reportExistence();
                reportTrees();
                spawnGardener();
                reportEmptyArea();
                // Move randomly
                RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
                TreeInfo[] friendlyTrees = rc.senseNearbyTrees(-1, rc.getTeam());
                
                if(enemies.length > 0){
                    float x = 0f;
                    float y = 0f;
                    for(RobotInfo robot : enemies){
                        float normalizer = (type.sensorRadius - robot.getLocation().distanceTo(rc.getLocation())) / type.sensorRadius;
                        float distance = robot.location.distanceTo(rc.getLocation());
                        x -= (robot.location.x - rc.getLocation().x)*normalizer;
                        y -= (robot.location.y - rc.getLocation().y)*normalizer;
                    }
                    Direction dir = new Direction(x,y);

                    tryMove(dir);

                }
                else if(friendlyTrees.length == 0 ){
                    pathTo(friendlySpawn);
                    wanderingDir = randomDirection();
                }
                else{
                    wanderingRumba();
                }
                
                /*
                MapLocation nest = broadcastHandle.getNestLocation();
                if (isValid(nest))
                    rc.setIndicatorDot(nest, 0, 255, 0);
                

                
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