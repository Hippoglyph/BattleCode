package Varg;

import battlecode.common.*;

public class Archon {
	
	RobotController rc;
	
	public void logic(RobotController RC) {
		
		rc = RC;
		
		while (true) {
			
			try {
				
				Map.donate(rc);	
				Map.dodge(rc);				
				RobotInfo[] enemies = Map.checkEnemies(rc);
				MapLocation myLocation = rc.getLocation();
				int round = rc.getRoundNum();
				
				if (enemies.length > 0 && rc.readBroadcast(179) > 0) {
					if (round < 300 && rc.readBroadcast(176) != 1) {
						rc.broadcast(176, 1);
					}	
					Map.dodge(rc, myLocation.directionTo(enemies[0].location).opposite());
				}
				else {
					if (rc.readBroadcast(176) != 0) {
						rc.broadcast(176, 0);
					}
				}
				
				if (rc.readBroadcast(162) == 0) {
					rc.broadcast(62, (int)myLocation.x);
					rc.broadcast(63, (int)myLocation.y);
					rc.broadcast(12, -1);
				}


				// Always hire a gardener if possible and if there isn't many units around.
				if (rc.readBroadcast(172) == 0 && rc.readBroadcast(178) < 2 && (rc.readBroadcast(180) == 0 || rc.readBroadcast(180) > 0 && round > 50)) {	
					Direction gardenerRallyPoint = Map.getBestRallyPoint(rc, myLocation, RobotType.GARDENER);
					hireGardener(gardenerRallyPoint);
					rc.broadcast(180, rc.readBroadcast(180) + 1);
				}
				
				Map.reset(rc);
				
	            // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
	            Clock.yield();
	            
			} catch (Exception e) {
		            System.out.println("Archon Exception");
		            e.printStackTrace();
		    }
		}
	}
	
	// TODO: Remove this function when we update getGardenerHiringDirection().
	public void hireGardener(Direction rallyPoint) throws GameActionException {		
        // Attempt to build a gardener in this direction
        if (rc.canHireGardener(rallyPoint)) {
            rc.hireGardener(rallyPoint);
        }
	}
	
	// TODO: Get the position closer to the point where we are going to build the next cluster
	public Direction getGardenerHiringDirection() {
		// Right now we leave it south.
		Direction dir = Map.randomDirection();
		
		return dir; 
	}
	

}
