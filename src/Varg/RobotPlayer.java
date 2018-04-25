package Varg;
import battlecode.common.*;


// Channels:

// 0-2: Attacked position (need to change when there are more than 1)
// 3-5: Lumberjack count / Lumberjack limit
// 6-7: Scout count / Scout limit
// 8-179: Soldier count
// 9-10: Location of the next tree to gather by scout.
// 11: There is a scout in the game
// 12-61: Assigned cluster builder IDs (Hopefully we won't build more than 50 clusters lol)
// 62-161: Cluster positions of our team
// 162: ALL-IN! (Stop any production, just attack and donate)
// 163-167: Last unit measures.
// 168-169: Scout next point to explore.
// 170: Soldier flag (stop building them)
// 171-173: Gardener count / Gardener limit
// 174-178: Idle gardeners 
// 175: Lumberjack rush
// 176: Soldier rush
// 177: Reset done
// 180: First gardener
// 181: Being rushed


public strictfp class RobotPlayer {
    static RobotController rc;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        // Here, we've separated the controls into a different method for each RobotType.
        // You can add the missing ones or rewrite this into your own control structure.
        switch (rc.getType()) {
            case ARCHON:
                new Archon().logic(rc);
                break;
            case GARDENER:
            	new Gardener().logic(rc);
                break;
            case SOLDIER:
            	new Soldier().logic(rc);
                break;
            case LUMBERJACK:
            	new Lumberjack().logic(rc);
                break;
			case SCOUT:
				new Scout().logic(rc);
				break;
			case TANK:
				new Tank().logic(rc);
				break;
			default:
				break;
        }
	}

    static void runLumberjack() throws GameActionException {
        System.out.println("I'm a lumberjack!");
        Team enemy = rc.getTeam().opponent();

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
                RobotInfo[] robots = rc.senseNearbyRobots(RobotType.LUMBERJACK.bodyRadius+GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);

                if(robots.length > 0 && !rc.hasAttacked()) {
                    // Use strike() to hit all nearby robots!
                    rc.strike();
                } else {
                    // No close robots, so search for robots within sight radius
                    robots = rc.senseNearbyRobots(-1,enemy);

                    // If there is a robot, move towards it
                    if(robots.length > 0) {
                        MapLocation myLocation = rc.getLocation();
                        MapLocation enemyLocation = robots[0].getLocation();
                        Direction toEnemy = myLocation.directionTo(enemyLocation);

                        Map.tryMove(rc, toEnemy);
                    } else {
                        // Move Randomly
                        Map.tryMove(rc, Map.randomDirection());
                    }
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Lumberjack Exception");
                e.printStackTrace();
            }
        }
    }



   
}
