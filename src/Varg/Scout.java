package Varg;

import battlecode.common.*;

public class Scout {
	
	RobotController rc;
	
	public void logic(RobotController RC) throws GameActionException {
		
		rc = RC;
		
		while (true) {
			try {
				
				Map.donate(rc);			
				Map.dodge(rc);
				Map.resetArchons(rc);

				MapLocation myLocation = rc.getLocation();					
				RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());			
				TreeInfo[] trees = RC.senseNearbyTrees(-1, Team.NEUTRAL);

				// Scan for enemies
				if (enemies.length > 0) {
					if (!rc.hasMoved()) {
						Map.dodge(rc, myLocation.directionTo(enemies[0].location).opposite());
					}
				}
				
				// Scan for trees
				if (trees.length > 0) {	
					
					for (TreeInfo tree : trees) {
						if (tree.containedBullets > 0) {
							if (rc.canShake(tree.ID)) {	
								rc.shake(tree.ID);
								break;
							}
							else {
								if (!rc.hasMoved()) {
									Map.tryMove(rc, myLocation.directionTo(tree.location));
								}
							}
						}
					}
					
					if (rc.readBroadcast(4) == 0) {
						if (rc.getRoundNum() < 100) {
							rc.broadcast(175, 1);
						}
						rc.broadcast(4, 5);		
					}
				}
				

				
				if (!rc.hasMoved()) {
					moveRandomly(myLocation);
				}
				
				// I'm alive
				rc.broadcast(6, 1);
				
				Clock.yield();		
				
			} catch (Exception e) {
		        System.out.println("Scout Exception");
		        e.printStackTrace();
			}
		}
	}
	
	private void moveRandomly(MapLocation myLocation) throws GameActionException {
		MapLocation target = pickTarget();
		Map.tryMove(rc, myLocation.directionTo(target));
	}
	
    private MapLocation pickTarget() throws GameActionException {
        RobotType type = rc.getType();
        MapLocation target = new MapLocation(-1, -1);
        while (target.x == -1 || !rc.onTheMap(target)) {
            Direction dir = Map.randomDirection();
        	target = rc.getLocation().add(dir, type.sensorRadius - 1f);
        }
        return target;
    }
}
