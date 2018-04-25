package Varg;

import battlecode.common.*;


public class Map {
	
    /**
     * Returns a random Direction
     * @return a random Direction
     */
    static Direction randomDirection() {
        return new Direction((float)Math.random() * 2 * (float)Math.PI);
    }
    
    /**
     * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(RobotController rc, Direction dir) throws GameActionException {
        return tryMove(rc, dir,20,3);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
     *
     * @param dir The intended direction of movement
     * @param degreeOffset Spacing between checked directions (degrees)
     * @param checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(RobotController rc, Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

        // First, try intended direction
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }

        // Now try a bunch of similar angles
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            if(rc.canMove(dir.rotateLeftDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset*currentCheck));
                return true;
            }
            // Try the offset on the right side
            if(rc.canMove(dir.rotateRightDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateRightDegrees(degreeOffset*currentCheck));
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    }

    /**
     * A slightly more complicated example function, this returns true if the given bullet is on a collision
     * course with the current robot. Doesn't take into account objects between the bullet and this robot.
     *
     * @param bullet The bullet in question
     * @return True if the line of the bullet's path intersects with this robot's current position.
     */
    static boolean willCollideWithMe(RobotController rc, BulletInfo bullet, MapLocation myLocation) {

        // Get relevant bullet information
        Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;

        // Calculate bullet relations to this robot
        Direction directionToRobot = bulletLocation.directionTo(myLocation);
        float distToRobot = bulletLocation.distanceTo(myLocation);
        float theta = propagationDirection.radiansBetween(directionToRobot);

        // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
        if (Math.abs(theta) > Math.PI/2) {
            return false;
        }

        // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
        // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
        // This corresponds to the smallest radius circle centered at our location that would intersect with the
        // line that is the path of the bullet.
        float perpendicularDist = (float)Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

        return (perpendicularDist <= rc.getType().bodyRadius);
    }
    
    static void dodge(RobotController rc) throws GameActionException {
    	dodge(rc, Direction.EAST);
    }
    
    static void dodge(RobotController rc, Direction dir) throws GameActionException {
    	if (!rc.hasMoved()) {
			BulletInfo[] bullets = rc.senseNearbyBullets();		
			MapLocation myLocation = rc.getLocation();
			boolean danger = false;
			
			if (bullets.length > 0) {
				for (int i = 0; i < bullets.length; i++) {
					if (willCollideWithMe(rc, bullets[i], myLocation) && isUnitInSight(rc, myLocation, bullets[i].location)) {
						danger = true;
						break;
					}
				}
				if (danger) {
					
					MapLocation possible_loc = new MapLocation(0, 0);
					for (int k = 0; k < Gardener.LOCATIONS_TO_CHECK_WHEN_BUILDING; k++) {				
						Direction dir1 = dir.rotateRightDegrees(k*(180/Gardener.LOCATIONS_TO_CHECK_WHEN_BUILDING));
						Direction dir2 = dir.rotateLeftDegrees(k*(180/Gardener.LOCATIONS_TO_CHECK_WHEN_BUILDING));	
				
						MapLocation possible_loc1 = myLocation.add(dir1, rc.getType().strideRadius);
						MapLocation possible_loc2 = myLocation.add(dir2, rc.getType().strideRadius);
						
						for (int l = 0; l < 2; l++) {
							if (l == 0) {
								possible_loc = possible_loc1;
							}
							else {
								possible_loc = possible_loc2;
							}
						
							for (int j = 0; j < bullets.length; j++) {
								if (!willCollideWithMe(rc, bullets[j], possible_loc)) {
									danger = false; // Actually, there is danger lol.
									break;
								}
								else {
									continue;
								}
							}
							
							if (!danger) {
								Map.tryMove(rc, myLocation.directionTo(possible_loc));
								return;
							}
						}
					}
				}
			}
    	}
    }
    
	static Pair<Direction, Boolean> shoot(RobotController rc, RobotInfo[] enemies) throws GameActionException {
		
		MapLocation myLocation = rc.getLocation();
		
		for (int i = 0; i < enemies.length; i++) {
			MapLocation enemy_loc = enemies[i].location;
			if (Map.isUnitInSight(rc, rc.getLocation(), enemy_loc) && (enemies[i].getType() != RobotType.ARCHON || (enemies[i].getType() == RobotType.ARCHON && (rc.getRoundNum() >= 500) || enemies.length == 1))) {
				return new Pair<Direction, Boolean>(myLocation.directionTo(enemy_loc), true);
			}
		}
		
		return new Pair<Direction, Boolean>(Map.randomDirection(), false);
		
	}
    
    static void resetArchons(RobotController rc) throws GameActionException {
		if (rc.readBroadcast(177) == 1) {
			rc.broadcast(177, 0);
		}
    }
    
	// Rings the alarm if the gardener senses enemies nearby.
	static RobotInfo[] checkEnemies(RobotController rc) throws GameActionException {
		RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
		int most_enemies = rc.readBroadcast(2);

		// If there are enemies close, write the attacked location in the message array.
		if (enemies.length > most_enemies && rc.readBroadcast(0) == 0 && rc.readBroadcast(1) == 0) {	
			rc.broadcast(0, (int)enemies[0].location.x);
			rc.broadcast(1, (int)enemies[0].location.y);
			rc.broadcast(2, enemies.length);
		}
		
		return enemies;
	}
	
	static boolean isUnitInSight(RobotController rc, MapLocation myLocation, MapLocation unitLocation) throws GameActionException {
		Direction dir = new Direction(myLocation, unitLocation);
		float dist = myLocation.distanceTo(unitLocation);
		try {
			for (float i = 1; i < dist - 1; i++) {		
				MapLocation loc = myLocation.add(dir, i);
				if (rc.isLocationOccupiedByRobot(loc)) {
					RobotInfo[] robots = rc.senseNearbyRobots(loc, (float)0.5, rc.getTeam());
					if (robots.length > 0) {
						return false;
					}
				}
			}
		}
		catch(Exception e) {} 
		return true;
	}
	
	static Direction getBestRallyPoint(RobotController rc, MapLocation myLocation, RobotType type) throws GameActionException {
		Direction dir = myLocation.directionTo(rc.getInitialArchonLocations(rc.getTeam().opponent())[0]);
		Direction possible_dir = null;
		
		for (int k = 0; k < Gardener.LOCATIONS_TO_CHECK_WHEN_BUILDING; k++) {		
			Direction dir1 = dir.rotateRightDegrees(k*(180/Gardener.LOCATIONS_TO_CHECK_WHEN_BUILDING));
			Direction dir2 = dir.rotateLeftDegrees(k*(180/Gardener.LOCATIONS_TO_CHECK_WHEN_BUILDING));	
			
			for (int l = 0; l < 2; l++) {
				if (l == 0) { possible_dir = dir1; }
				else { possible_dir = dir2; }
			
				if (rc.canBuildRobot(type, possible_dir)) {
					return possible_dir;
				}
			}
		}
		return possible_dir;
	}
	
	static void donate(RobotController rc) throws GameActionException {
		
		int teamPoints = rc.getTeamVictoryPoints();
		int opponentPoints = rc.getOpponentVictoryPoints();
		int diff = teamPoints - opponentPoints;
		
		float nrBullets = rc.getTeamBullets();
		float pointCost = rc.getVictoryPointCost();
		
		// If we win by donating everything, donate everything
		if (teamPoints+nrBullets/pointCost >= 1000) {
			rc.donate(nrBullets);
		}
		
		//Else, try to stay ahead of opponent, if opponent isn't too far ahead.
		if (diff <= 0 && diff >-100 && -(diff*pointCost) < nrBullets) {
			rc.donate(-(diff*pointCost));
		}
		
		// Also, if we have a good economy, donate
		// (good economy = we can buy at least 50 points)
		if (nrBullets > pointCost*50) {
			rc.donate(pointCost);
		}
		
		if (rc.getRoundNum() > 2990) {
			rc.donate(nrBullets);
		}
		
	}
	
    static float randomWithRange(float min, float max) {
    	float range = (max - min) + 1;     
    	return (float)((Math.random() * range) + min);
    }
    
    static void reset(RobotController rc) throws GameActionException {
    	if (rc.readBroadcast(177) == 0) {

			// Stop rush mode
			if (rc.getRoundNum() > 100 && rc.readBroadcast(175) != 0) {
				rc.broadcast(175, 0);				
			}
			// Stop rush mode
			if (rc.getRoundNum() > 500 && rc.readBroadcast(176) != 0) {
				rc.broadcast(176, 0);				
			}
			
			
			// Every 20 rounds, resets attacked position.
			if (rc.getRoundNum() % 20 == 0) {
				rc.broadcast(0, 0);
				rc.broadcast(1, 0);
				rc.broadcast(2, 0);
			}
			
			// Reset lumberjack count
			if (rc.readBroadcast(4) > 0) {				
				rc.broadcast(5, rc.readBroadcast(3));
				rc.broadcast(3, 0);
			}
			
			// Reset scout count
			if (rc.readBroadcast(6) > 0) {
				rc.broadcast(7, 10*rc.getInitialArchonLocations(rc.getTeam()).length);
			}
			else { rc.broadcast(7, rc.readBroadcast(7) - 1); }
			
			
			// Reset soldier count
			if (rc.readBroadcast(8) > 100) {
				if (rc.readBroadcast(170) == 0) {
					rc.broadcast(170, 1);
				}
			}
			else {
				if (rc.readBroadcast(8) != -1 && rc.readBroadcast(170) == 1) {
					rc.broadcast(170, 0);
				}
			}
			rc.broadcast(179, rc.readBroadcast(8));
			rc.broadcast(8, -1);
			
			// Reset gardener count
			if (rc.readBroadcast(171) > 100) {
				if (rc.readBroadcast(172) == 0) {
					rc.broadcast(172, 1);
				}
			}
			else {
				if (rc.readBroadcast(171) != -1 && rc.readBroadcast(172) == 1) {
					rc.broadcast(172, 0);
				}
			}
			rc.broadcast(171, -1);

			// Reset idle gardeners count
			rc.broadcast(178, rc.readBroadcast(174));
			rc.broadcast(174, 0);
			
			rc.broadcast(177, 1);
		}
    }
    
    
}
