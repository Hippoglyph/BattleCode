package SOMbot;
import battlecode.common.*;


public class Soldier extends Unit{
    boolean harasser;
    boolean reachedEnemySpawn;
    MapLocation previousLocation;

    int cutTreeCounter;
    int cutTreeCounterInit = 30;
    public Soldier(RobotController rc){
        super(rc);
        harasser = probIs((float)1.0);
        reachedEnemySpawn = false;
        cutTreeCounter = cutTreeCounterInit;
        previousLocation = rc.getLocation();
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
                if(!neverLeader)
                    broadcastHandle.reportExistence();
                MapLocation[] prioTargets = broadcastHandle.getPriorityTargets();
                MapLocation myLocation = rc.getLocation();
                MapLocation closestPrio = nullMap();
                float curDist = Float.MAX_VALUE;
                for(MapLocation prio : prioTargets){
                    if(isValid(prio) && myLocation.distanceTo(prio) < curDist){
                        closestPrio = prio;
                        curDist = myLocation.distanceTo(prio);
                    }
                    if(rc.canSenseAllOfCircle(prio,rc.getType().bodyRadius)){
                        RobotInfo[] sensed = rc.senseNearbyRobots(prio,rc.getType().bodyRadius,enemy);
                        if(sensed.length == 0)
                            broadcastHandle.resetEnemy(prio);
                    }
                }

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
                    if(!rc.hasMoved())
                        tryMove(new Direction(rc.getLocation(), closestRobot.location));
                    broadcastHandle.reportEnemy(closestRobot.location);
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
                    MapLocation goTo = nullMap();
                    if(!reachedEnemySpawn)
                        goTo = enemySpawn;
                    if(isValid(closestPrio))
                        goTo = closestPrio;


                    if(!reachedEnemySpawn && rc.getLocation().distanceTo(enemySpawn) < rc.getType().bodyRadius*10)
                        reachedEnemySpawn = true;
                    else if(isValid(goTo)){
                        cutTreeCounter--;
                        if(cutTreeCounter < 0){
                            if(previousLocation.distanceTo(goTo) - rc.getLocation().distanceTo(goTo) < rc.getType().bodyRadius*2){
                                reportClosestTree();
                            }
                            else
                                previousLocation = rc.getLocation();
                            cutTreeCounter=cutTreeCounterInit;
                        }
                        pathTo(goTo);
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

    void reportClosestTree() throws GameActionException{
        TreeInfo[] trees = rc.senseNearbyTrees(-1,Team.NEUTRAL);
        if(trees.length != 0){
            broadcastHandle.reportTree(trees[0].location);  
        }
    }



}