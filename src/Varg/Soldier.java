package Varg;

import battlecode.common.*;

public class Soldier {

RobotController rc;
	
	public void logic(RobotController RC) throws GameActionException {
		
		rc = RC;
		
		while (true) {
			try {
				
				// Every 20 rounds, resets attacked position.
				if (rc.getRoundNum() % 20 == 0) {
					rc.broadcast(0, 0);
					rc.broadcast(1, 0);
					rc.broadcast(2, 0);
					rc.broadcast(181, 0);
				}
				
				if (rc.getRoundNum() > 500) {
					if (rc.readBroadcast(181) == 1) {
						rc.broadcast(181, 0);
					}
				}
				
				Map.donate(rc);
				Map.dodge(rc);
				Map.resetArchons(rc);
				
				MapLocation myLocation = rc.getLocation();
				RobotInfo[] enemies = Map.checkEnemies(rc);			
				MapLocation[] archons = rc.getInitialArchonLocations(rc.getTeam().opponent());
				
				// If there are enemies nearby...
				if (enemies.length > 0) {
					if (!rc.hasMoved()) {
						/*
						if (allies.length > enemies.length) {
							Map.dodge(rc, myLocation.directionTo(enemies[0].location));			
						}
						else {
							Map.dodge(rc, myLocation.directionTo(enemies[0].location).opposite());
						} */
						Map.dodge(rc, myLocation.directionTo(enemies[0].location));
					}		
										
					Pair<Direction, Boolean> killzone = Map.shoot(rc, enemies);				
					Direction dir = killzone.getLeft();
					boolean fire = killzone.getRight();

					if (fire) {
						if (rc.getRoundNum() < 300 && enemies[0].type != RobotType.SCOUT) {
							rc.broadcast(181, 1);
						}
						if (!rc.hasMoved()) {
							Map.tryMove(rc, dir);
						}
						float dist = myLocation.distanceTo(enemies[0].location);

						if (dist < 3) {
							if (rc.canFirePentadShot()) {
								rc.firePentadShot(dir);
							}	
						}						
						else if (dist >= 3) {
							if (rc.canFireTriadShot()) {
								rc.fireTriadShot(dir);
							}	
						}						
					} 
					
					else {
						if (!rc.hasMoved()) {
							Map.tryMove(rc, dir, 90, 10);
						}
					}
				}
				
				// If we don't sense any enemy...
				else {
					Map.dodge(rc);
					// If not, we check if there is an attacked position.
					if (!rc.hasMoved()) {
						MapLocation attacked_pos = new MapLocation(rc.readBroadcast(0), rc.readBroadcast(1));
						
						// If there isn't any, just explore.
						if (attacked_pos.x == 0 && attacked_pos.y == 0) {
							if (myLocation.distanceTo(archons[0]) > 10  && rc.readBroadcast(179) > 2) {
								Map.tryMove(rc, rc.getLocation().directionTo(archons[0]), 90, 10);
							}
							else {
								Map.tryMove(rc, Map.randomDirection());
							}
						}
						// If there is an attacked position, go there
						else {
							Map.tryMove(rc, rc.getLocation().directionTo(attacked_pos), 90, 10);
						}
					}
				}
				
				rc.broadcast(8, rc.readBroadcast(8) + 1);
				Clock.yield();
				
			} catch (Exception e) {
		        System.out.println("Soldier Exception");
		        e.printStackTrace();
			}
		}
	}
	
	public void explore() throws GameActionException {
		Team team = rc.getTeam();
		Direction initial_dir = rc.getLocation().directionTo(rc.getInitialArchonLocations(team.opponent())[0]);
		for (int k = 0; k < Gardener.LOCATIONS_TO_CHECK_WHEN_BUILDING; k++) {				
			Direction dir = initial_dir.rotateRightDegrees(k*(360/Gardener.LOCATIONS_TO_CHECK_WHEN_BUILDING));				
			MapLocation exploringLocation = rc.getLocation().add(dir, 4);

			if (rc.senseNearbyRobots(exploringLocation, (float)2.5, team).length == 0) {
				Map.tryMove(rc, rc.getLocation().directionTo(exploringLocation));
				return;
			}
		}
		Map.tryMove(rc, rc.getLocation().directionTo(rc.getInitialArchonLocations(team.opponent())[0]));
	}
	

	

}
