package Varg;

import battlecode.common.*;

public class Gardener {
	
	static int TREES_IN_CLUSTER = 6;
	static int MAX_CLUSTERS = 50;
	static int MAX_BULLETS_IN_TREE = 10;
	static double MIN_HEALTH_TO_WATER_TREE = 0.9;
	static int CLUSTER_BUILDER_STARTING_CHANNEL = 12;
	static int CLUSTER_POSITION_STARTING_CHANNEL = 62;
	static int LOCATIONS_TO_CHECK_WHEN_BUILDING = 180;	
	
	RobotController rc;

	public void logic(RobotController RC) {
		
		rc = RC;

        while (true) {

            try { 
            	
            	Map.donate(rc);
            	Map.dodge(rc);
				Map.resetArchons(rc);
            	
            	// Check for bullets to dodge
				MapLocation myLocation = rc.getLocation();
				RobotInfo[] enemies = Map.checkEnemies(rc);
				
				TreeInfo[] trees = RC.senseNearbyTrees(-1, Team.NEUTRAL);

				if (enemies.length > 0) {
					if (rc.getRoundNum() < 500 && rc.readBroadcast(176) != 1) {
						rc.broadcast(176, 1);
					}	
					Map.dodge(rc, myLocation.directionTo(enemies[0].location).opposite());
				}
            		
            	// If this is a gardener assigned to a cluster, let it know.
            	int builder = checkIfBuilder();
            	
            	if (builder == -1) {
            		
            		Direction rallyPoint = Map.getBestRallyPoint(rc, myLocation, RobotType.SCOUT);
            		
            		if (rc.readBroadcast(181) == 1 || rc.readBroadcast(0) != 0 || rc.getRoundNum() < 10) {
            			if (rc.canBuildRobot(RobotType.SOLDIER, rallyPoint)) {
            				buildSoldier(rallyPoint);
            			}
            			else {
            				rc.broadcast(174, rc.readBroadcast(174) + 1);	
            			}
            		}
            		
            		else if (getPositionForNextClusters() == false) {
     
    	            // TODO: If not a builder, either explore (we need pathfinding for this) to find new cluster locations or 
	            	// build an army based on the environment (lumberjacks if lots of trees, soldiers if open map...)    	            
    	            	
                		if (trees.length > 5 && rc.canBuildRobot(RobotType.LUMBERJACK, rallyPoint)) {
                			if (rc.canBuildRobot(RobotType.LUMBERJACK, rallyPoint)) {
                				buildLumberjack(rallyPoint);
                			}
                		}
                		
                		// Hire a scout instantly if there isn't any.
                		else if (rc.readBroadcast(7) <= 0 && rc.canBuildRobot(RobotType.SCOUT, rallyPoint)) { 
                			buildScout(rallyPoint);
                		}  
                		
                		else if (rc.canBuildRobot(RobotType.SOLDIER, rallyPoint) && rc.readBroadcast(170) == 0) {
                			buildSoldier(rallyPoint);
    	            	}   
                		
                		else {
                			rc.broadcast(174, rc.readBroadcast(174) + 1);
                		}

    	            }
            		
            		// Move randomly
	            	if (!rc.hasMoved() && rc.readBroadcast(0) != 0 && rc.getRoundNum() > 500 && rc.readBroadcast(181) == 0) {
	        			//Get trajectory towards enemy initial pos
	            		moveTowardsArchonRandom();
	            	}
	            	else {
	            		Map.tryMove(rc, Map.randomDirection());
	            	}
            	}

            	else {
            		goToOrBuildCluster(builder);
            	}       	           	

            	rc.broadcast(171, rc.readBroadcast(171) + 1);
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            }
        }
	}
	
	
	public void moveTowardsArchonRandom() throws GameActionException {
		MapLocation firstArchonLoc = rc.getInitialArchonLocations(rc.getTeam().opponent())[0];
		Direction towardsArchon = rc.getLocation().directionTo(firstArchonLoc);
		Direction towardsArchonRandom = towardsArchon.rotateLeftRads(Map.randomWithRange(-(float)(Math.PI/2), (float)(Math.PI/2)));        
		Map.tryMove(rc, towardsArchonRandom);
	}
	
	// Checks the message array to see if it's a builder, returns the relative position in the array.
	public int checkIfBuilder() throws GameActionException {
		for (int i = 0; i < MAX_CLUSTERS; i++) {
			int builderID = rc.readBroadcast(CLUSTER_BUILDER_STARTING_CHANNEL + i);
			if (builderID != rc.getID()) {
				if (builderID == 0) { break; }
				else { continue; }
			}
			else { return i; }
		}
		return -1;
	}
	
	// TODO: Change random rally points for intelligent rally points
	public void buildScout(Direction rallyPoint) throws GameActionException {		
		// We got one scout now!
		rc.broadcast(7, 15*rc.getInitialArchonLocations(rc.getTeam()).length);
		rc.buildRobot(RobotType.SCOUT, rallyPoint);
	}
	
	public void buildLumberjack(Direction rallyPoint) throws GameActionException {		
		// Notify that we built the lumberjack
		rc.broadcast(5, 11*rc.getInitialArchonLocations(rc.getTeam()).length);
		rc.buildRobot(RobotType.LUMBERJACK, rallyPoint);
	}
	
	public void buildSoldier(Direction rallyPoint) throws GameActionException {	
		rc.buildRobot(RobotType.SOLDIER, rallyPoint);
	}
	
	// Explores the nearby area clockwise, and picks the first open space it finds to build a cluster.
	// Additionally, it registers the following cluster location.
	// TODO: Possible heuristics: Protecting archon, far from enemies, not build if being attacked.
	public boolean getPositionForNextClusters() throws GameActionException {
		
		if (rc.readBroadcast(0) != 0 && rc.getLocation().distanceTo(new MapLocation(rc.readBroadcast(0), rc.readBroadcast(1))) < 7) {
			return false;
		}
		for (int k = 0; k < LOCATIONS_TO_CHECK_WHEN_BUILDING; k++) {				
			Direction dir = Direction.NORTH.rotateRightDegrees(k*(360/LOCATIONS_TO_CHECK_WHEN_BUILDING));				
			MapLocation clusterLocation = rc.getLocation().add(dir, 4);

			if (rc.onTheMap(clusterLocation, (float)2.5) && !rc.isCircleOccupied(clusterLocation, (float)2.5) && !collidesWithOtherClusters(clusterLocation)) {
				int cluster_pos = registerNewCluster(clusterLocation);
				if (cluster_pos != -1) {
					goToOrBuildCluster(cluster_pos);
				}
				return true;
			}
		}
		return false;
	}
	
	// Checks if a given location is 4 units away from every cluster we have.
	public boolean collidesWithOtherClusters(MapLocation loc) throws GameActionException {
		for (int i = 0; i < MAX_CLUSTERS; i++) {
			MapLocation cluster_loc = new MapLocation(rc.readBroadcast(CLUSTER_POSITION_STARTING_CHANNEL + 2*i), rc.readBroadcast(CLUSTER_POSITION_STARTING_CHANNEL + 2*i + 1));
			if (loc.distanceTo(cluster_loc) < 8) {
				return true;
			}
		}
		return false;
	}
	
	// Add the gardener ID and the cluster location to the respective reserved spaces in the message array. Returns -1 if the space is full.
	public int registerNewCluster(MapLocation loc) throws GameActionException {
		
		// Check until finding an empty space, then store builder ID and cluster position in their respective slots.
		for (int i = 0; i < MAX_CLUSTERS; i++) {
			if (rc.readBroadcast(CLUSTER_BUILDER_STARTING_CHANNEL + i) != 0) {
				continue;
			}
			else {
				rc.broadcast(CLUSTER_BUILDER_STARTING_CHANNEL + i, rc.getID());
				rc.broadcast(CLUSTER_POSITION_STARTING_CHANNEL + 2*i, (int)loc.x);
				rc.broadcast(CLUSTER_POSITION_STARTING_CHANNEL + 2*i + 1, (int)loc.y);
				return i;
			}
		}
		// This shouldn't happen.
		return -1;
	}
		
	// Go to the cluster building point or build it if already there.
	public void goToOrBuildCluster(int builderpos) throws GameActionException {
		
		int position = CLUSTER_POSITION_STARTING_CHANNEL + 2*builderpos;
		MapLocation current_loc = rc.getLocation();
		
    	// Move to the assigned cluster location to start building.
    	MapLocation bestLocation = new MapLocation(rc.readBroadcast(position), rc.readBroadcast(position+1));
    	
    	// If we are not there, move.
    	if (current_loc.distanceTo(bestLocation) > 1 && !rc.hasMoved()) {
			Map.tryMove(rc, new Direction(current_loc, bestLocation));
		}
    	
    	// If we are there, build or shake and water the cluster.
		else { 
			takeCareOfCluster();
		}	  
	}

	
	// Either build or shake and water the cluster the gardener is in.
	public void takeCareOfCluster() throws GameActionException {
		// Plant trees if there are empty spots in the cluster.
		if (rc.senseNearbyTrees(1).length != TREES_IN_CLUSTER) {
			buildCluster();  
    	}
		// Water any tree in the cluster with less than certain HP.
		waterCluster();  
		// Get the bullets from the mature trees in the cluster.
		shakeCluster();  
	}
	
	public void buildCluster() throws GameActionException {
		
		int offset = 0;
		
		while (offset < (float)2*Math.PI) {			
			Direction treePlacement = new Direction(2*(float)Math.PI*offset/6f);

			if (rc.canPlantTree(treePlacement)) {
				if (rc.readBroadcast(0) != 0) {
					rc.buildRobot(RobotType.SOLDIER, treePlacement);
				}
				else {
					rc.plantTree(treePlacement);
				}
			}
			else {
				offset += 1;
			}
		}		
	}
		
	public void shakeCluster() throws GameActionException {
		
		TreeInfo[] trees = rc.senseNearbyTrees((float)1.5, rc.getTeam());
		
		for (int i = 0; i < trees.length; i++) {
			TreeInfo tree = trees[i];
			if (tree.containedBullets > MAX_BULLETS_IN_TREE && rc.canShake(tree.getLocation())) {
				rc.shake(tree.getLocation());
				break;
			}
		}
	}
	
	public void waterCluster() throws GameActionException {		
		
		TreeInfo[] trees = rc.senseNearbyTrees(2, rc.getTeam());

		for (int i = 0; i < trees.length; i++) {
			TreeInfo tree = trees[i];
			if (tree.health < tree.maxHealth*MIN_HEALTH_TO_WATER_TREE && rc.canWater(tree.getLocation())) {
				rc.water(tree.getLocation());
				break;
			}
		}
	}
		
}
