package SOMbot;
import battlecode.common.*;


public class Soldier extends Unit{
    boolean harasser;
    boolean reachedEnemySpawn;
    public Soldier(RobotController rc){
        super(rc);
        harasser = probIs((float)1.0);
        reachedEnemySpawn = false;
    }

    @Override
    public void run() {
        Team enemy = rc.getTeam().opponent();

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                broadcastHandle.reportExistence();
                MapLocation myLocation = rc.getLocation();

                // See if there are any nearby enemy robots
                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
                RobotInfo[] friendlies = rc.senseNearbyRobots(-1, rc.getTeam());

                // If there are some...
                if (robots.length > 0) {
                    safeMove();
                    RobotInfo closestRobot = robots[0];
                    for (RobotInfo robot : robots){
                        //check if friend is in way of robot
                        float distanceRobot = myLocation.distanceTo(robot.location);
                        Direction robotDirection = new Direction(myLocation, robot.location);
                        boolean friendInWay = false;
                        for(RobotInfo friend : friendlies){
                            if(myLocation.distanceTo(friend.location) < distanceRobot && robotDirection.equals(new Direction(myLocation, friend.location))){
                                friendInWay = true;
                                break;
                            }
                        }

                        if(!friendInWay){
                            if (robot.type == RobotType.GARDENER){
                                if(closestRobot.type != RobotType.GARDENER && robot.getLocation().distanceTo(myLocation) < closestRobot.getLocation().distanceTo(myLocation))
                                    closestRobot = robot;
                            }
                            else if(robot.type == RobotType.SOLDIER){
                                if (closestRobot.type != RobotType.GARDENER && robot.getLocation().distanceTo(myLocation) < closestRobot.getLocation().distanceTo(myLocation))
                                    closestRobot = robot;
                            }
                        }
                        
                    }
                    float distanceToTarget = rc.getLocation().distanceTo(closestRobot.getLocation());
                    if(distanceToTarget < rc.getType().bodyRadius*6 && rc.canFirePentadShot())
                        rc.firePentadShot(rc.getLocation().directionTo(closestRobot.location));

                    if(distanceToTarget < rc.getType().bodyRadius*8 && rc.canFireTriadShot())
                        rc.fireTriadShot(rc.getLocation().directionTo(closestRobot.location));

                    if(rc.canFireSingleShot()){
                        rc.fireSingleShot(rc.getLocation().directionTo(closestRobot.location));
                    }

                    
                    
                    
                    
                    
                }
                else{

                    if(harasser){
                        if(!reachedEnemySpawn && rc.getLocation().distanceTo(enemySpawn) < rc.getType().bodyRadius*10)
                            reachedEnemySpawn = true;
                        else if(!reachedEnemySpawn)
                            pathTo(enemySpawn);
                    }
                    if(reachedEnemySpawn)
                        wanderingRumba();
                    
                }



                // Move randomly
                //tryMove(randomDirection());

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Soldier Exception");
                e.printStackTrace();
            }
        }
    }



}