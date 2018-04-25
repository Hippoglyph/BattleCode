package Varg;

import battlecode.common.*;

public class Lumberjack {
	
	RobotController rc;

	public void logic(RobotController RC) throws GameActionException {
		
		rc = RC;
		
		while (true) {
			try {
				
				Map.donate(rc);
				Map.dodge(rc);
				Map.resetArchons(rc);
				
				MapLocation myLocation = rc.getLocation();
				TreeInfo[] nearbyTrees = rc.senseNearbyTrees(-1, Team.NEUTRAL);
				TreeInfo tree = null;
				MapLocation[] archons = rc.getInitialArchonLocations(rc.getTeam().opponent());
				
				if (nearbyTrees.length > 0) {
					tree = nearbyTrees[0];			
				}
				
				// Sense enemies around
				RobotInfo[] enemies = Map.checkEnemies(rc);
							
				// Strike zone
				if (enemies.length > 0) {
					Map.dodge(rc, myLocation.directionTo(enemies[0].location));
					if (myLocation.distanceTo(enemies[0].location) < (float)3 && rc.canStrike()) {
						rc.strike();
					}
					else {
						if (!rc.hasMoved()) {					
							Map.tryMove(rc, myLocation.directionTo(enemies[0].location));
							chopClosestTree(tree, myLocation);
						}
					}
				}		
				else {				
					// Chop zone
					if (nearbyTrees.length > 0) {				
						if (!rc.hasMoved()) {
							Map.tryMove(rc, myLocation.directionTo(nearbyTrees[0].location));
							for (TreeInfo t : nearbyTrees) {
								if (rc.canChop(t.ID)) {
									chopClosestTree(t, myLocation);
								}
							}
						}				
					}		
					
					// Movement zone
					else {	
						if (!rc.hasMoved()) {
							Map.tryMove(rc, rc.getLocation().directionTo(archons[0]), 90, 10);
						}
					}         	
				}
				
				rc.broadcast(3, rc.readBroadcast(3) + 1);
				Clock.yield();
				
			} catch (Exception e) {
		        System.out.println("Lumberjack Exception");
		        e.printStackTrace();
			}
		}
	}
	
	private void chopClosestTree(TreeInfo tree, MapLocation myLocation) throws GameActionException {	
		try {
			if (tree.containedBullets > 0 && rc.canShake()) {
				rc.shake(tree.ID);
			}
			else {		
				if (rc.canChop(tree.ID)) {					
					rc.chop(tree.ID);
				}
			}
		}
		catch(Exception e) {} 
	}
}
