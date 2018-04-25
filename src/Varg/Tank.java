package Varg;

import battlecode.common.*;

public class Tank {
	
	RobotController rc;
	
	public void logic(RobotController RC) throws GameActionException {
		
		rc = RC;
		
		while (true) {
			try {
				
				Map.donate(rc);
				Map.dodge(rc);
				Map.resetArchons(rc);
				
				MapLocation myLocation = rc.getLocation();
				RobotInfo[] enemies = Map.checkEnemies(rc);			
				RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
				
				// If there are enemies nearby...
				if (enemies.length > 0) {
					if (!rc.hasMoved()) {
						if (allies.length > enemies.length) {
							Map.dodge(rc, myLocation.directionTo(enemies[0].location));			
						}
						else {
							Map.dodge(rc, myLocation.directionTo(enemies[0].location).opposite());
						}
					}		
										
					Pair<Direction, Boolean> killzone = Map.shoot(rc, enemies);				
					Direction dir = killzone.getLeft();
					boolean fire = killzone.getRight();

					if (fire) {						
						float dist = myLocation.distanceTo(enemies[0].location);

						if (dist < 3) {
							if (rc.canFirePentadShot()) {
								rc.firePentadShot(dir);
							}	
						}						
						else if (dist > 3 && dist < 5) {
							if (rc.canFireTriadShot()) {
								rc.fireTriadShot(dir);
							}	
						}						
						else if (dist > 5) {
							if (rc.canFireSingleShot()) {
								rc.fireSingleShot(dir);
							}	
						}
					} 
					
					else {
						if (!rc.hasMoved()) {
							Map.tryMove(rc, dir);
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
							Map.tryMove(rc, Map.randomDirection());
						}
						// If there is an attacked position, go there
						else {
							Direction dir = rc.getLocation().directionTo(attacked_pos);
							try {
								if (rc.senseTreeAtLocation(myLocation.add(dir, 3)).team == rc.getTeam()) {
									Map.tryMove(rc, Map.randomDirection());
								}
								else {
									Map.tryMove(rc, dir, 90, 10);
								}
							}
							catch(Exception e) {
								Map.tryMove(rc, dir, 90, 10);
							}
						}
					}
				}

				Clock.yield();
				
			} catch (Exception e) {
		        System.out.println("Tank Exception");
		        e.printStackTrace();
			}
		}
	}
}
