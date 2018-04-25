package ultrabot;
import battlecode.common.*;
import java.util.ArrayList;
import java.util.List;

import java.util.Collections;
import java.util.Arrays;

class Scout2 extends Bot{
	/**
	 * Basic priority of the scout:
	 *  1) If I know about the enemy scout, try to kill it!
	 *  2) If I know about trees, shake them for bullets
	 *  3) If I don't, explore map to determine cordinates of edges
	 *  4) Perhaps we could make it go find the Archon or shoot enemies
	 */
	List<TreeInfo> knownTrees = new ArrayList<TreeInfo>();
	List<TreeInfo> doneTrees = new ArrayList<TreeInfo>();
	List<MapLocation> exploreLocations = new ArrayList<MapLocation>();
	List<Direction> exploreDirections = new ArrayList<Direction>();
	List<MapLocation> edgeLocations = new ArrayList<MapLocation>();
	List<Direction> edgeDirections = new ArrayList<Direction>();
	List<MapLocation> borderLocations = new ArrayList<MapLocation>();
	List<RobotInfo> knownRobots = new ArrayList<RobotInfo>();
	boolean hasTargetTree = false;
	boolean foundBorder = false;
	boolean followingEnemyScout = false;
	boolean hasFoundEnemyScout = false;
	boolean hasAskedForNewScout = false;

	RobotInfo enemyScout;
	TreeInfo targetTree;
	float earning = 0.0f;
	float minX, minY, maxX, maxY;

	@Override
	protected void beforeStep() throws GameActionException{
		super.beforeStep();
		receiveArchonLocations();
		receiveEnemyArchonLocations();
		setExploreLocations();
		checkForTrees();
		checkForEnemyScout();
	}

	@Override
	protected void mainStep() throws GameActionException{
		super.mainStep();
		checkForTrees();
		checkForEnemyScout();
		if(followingEnemyScout){
			dealWithEnemyScout();
		} else if(hasTargetTree){
			debugTreeLoc();
			dealWithTargetTree();
		} else if(!foundBorder){
			explore();
		} else{
			// attack enemy archon
		}


		if(!hasAskedForNewScout){
			if(likelyToDie()){
				broadCastSpawnNewScout();
				hasAskedForNewScout = true;
			}
		}
		debugExploreLocs();
		//debugEdgeLocs();
	}

	@Override
	protected void afterStep() throws GameActionException{
		super.afterStep();
		//Do something after each round
	}

	protected boolean likelyToDie(){
		return rc.getHealth() < 5.0f && 
		rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam().opponent()).length > 0;
	}

	protected void broadCastSpawnNewScout() throws GameActionException{
		try{
			rc.broadcastBoolean(Constants.SCOUT_DYING, true);
		} catch(GameActionException e){
			System.err.println("Could not broadcast new scout indicator");
			e.printStackTrace();
		}
	}

	protected void checkForEnemyScout(){
		if(hasFoundEnemyScout){
			if(rc.canSenseRobot(enemyScout.getID())){
				followingEnemyScout = true;
			}
		} else{
			List<RobotInfo> newRobots = new ArrayList<>(
				Arrays.asList(rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam().opponent())));
			newRobots.removeAll(knownRobots);
			for(int i=0; i<newRobots.size(); i++){
				if(newRobots.get(i).getType() == RobotType.SCOUT){
					enemyScout = newRobots.remove(i);
					followingEnemyScout = true;
					hasFoundEnemyScout = true;
				}
			}
			knownRobots.addAll(newRobots);
		}
		
	}

	protected void dealWithEnemyScout() throws GameActionException{
		if(rc.canSenseRobot(enemyScout.ID)){
			MapLocation currentLoc = rc.senseRobot(enemyScout.getID()).getLocation();
			if(rc.canFireSingleShot()){
				try{
					rc.fireSingleShot(new Direction(rc.getLocation(), currentLoc));
				} catch(GameActionException e){
					System.err.println("Scout failed while shooting");
					e.printStackTrace();
				}
			}	
			pursuit(enemyScout);
		} else{
			// Hopefully the enemy scout is dead, but maybe
			// we just lost track of him
			followingEnemyScout = false;
		}
	}

	protected void checkForTrees(){
		senseTrees();
		if(!hasTargetTree && !knownTrees.isEmpty()){
			hasTargetTree = true;
			targetTree = knownTrees.remove(0);
		}
	}

	protected void senseTrees(){
		List<TreeInfo> newTrees = new ArrayList<>(
			Arrays.asList(rc.senseNearbyTrees(RobotType.LUMBERJACK.sensorRadius, Team.NEUTRAL)));
		//Quick and dirty way of removing duplicates		
		if(newTrees.size()>0){
			//Don't care for bullet-less trees
			newTrees.removeIf(t -> t.containedBullets == 0);
			newTrees.removeAll(doneTrees);
			knownTrees.removeAll(newTrees);
			knownTrees.addAll(newTrees);
			sortKnownTrees();
		} 
	}

	protected void sortKnownTrees(){
		Collections.sort(knownTrees,
				(t1, t2) -> Float.compare(distToLoc(t1.location), (distToLoc(t2.location))));
	}

	protected void dealWithTargetTree() throws GameActionException{
		try{
			if(rc.canInteractWithTree(targetTree.ID)){
				rc.shake(targetTree.ID);
				doneTrees.add(targetTree);
				hasTargetTree = false;
			} else{
				if(distToLoc(targetTree.getLocation()) < rc.getType().sensorRadius){
					if(!rc.canSenseTree(targetTree.getID())){
						// Tree was probably destroyed
						doneTrees.add(targetTree);
						hasTargetTree = false;
					}
				}
				tryMove(new Direction(rc.getLocation(), targetTree.location));
			}
		} catch(GameActionException e){
			System.err.println("Scout encountered an error while dealing with tree");
		} 
	}

	protected void setExploreLocations(){
		// First add the center point in the map
		float unitDist = rc.getType().sensorRadius;
		float addDist = unitDist;
		Direction[] dirs = {Direction.getWest(), Direction.getNorth(), 
			Direction.getEast(), Direction.getSouth()};
		exploreLocations.add(archonLocations.get(0).add(
			new Direction(archonLocations.get(0), enemyArchonLocations.get(0)),
			archonLocations.get(0).distanceTo(enemyArchonLocations.get(0))/2));
		exploreDirections.add(Direction.getNorth());
		MapLocation newLoc;
		Direction newDir;
		// i is just set to some dirty value to make it go longer
		for(int i=1; i<20; i++){
			if(i%2 == 0){
				addDist+=2*unitDist;
			}
			newDir = dirs[i%4];
			newLoc = exploreLocations.get(i-1).add(newDir, addDist);
			exploreLocations.add(newLoc);
			exploreDirections.add(newDir);
		}
	}

	protected void constructBorder() throws GameActionException{
		foundBorder = true;
		minX = edgeLocations.get(0).x;
		minY = edgeLocations.get(0).y;
		maxX = edgeLocations.get(0).x;
		maxY = edgeLocations.get(0).y;
		for(MapLocation loc: edgeLocations){
			if(loc.x < minX){
				minX = loc.x;
			} else if(loc.x > maxX){
				maxX = loc.x;
			}
			if(loc.y < minY){
				minY = loc.y;
			} else if(loc.y > maxY){
				maxY = loc.y;
			}
		}
		borderLocations.add(new MapLocation(minX, minY));
		borderLocations.add(new MapLocation(maxX, minY));
		borderLocations.add(new MapLocation(maxX, maxY));
		borderLocations.add(new MapLocation(minX, maxY));
		//reportBorder();
	}

	/** 
	protected void reportBorder() throws GameActionException{
		try{
			rc.broadcastBoolean(Constants.FOUND_BORDER, true);
			rc.broadcastFloat(Constants.BORDER_MIN_X, minX);
			rc.broadcastFloat(Constants.BORDER_MIN_Y, minY);
			rc.broadcastFloat(Constants.BORDER_MAX_X, maxX);
			rc.broadcastFloat(Constants.BORDER_MAX_Y, maxY);
		} catch(Exception e){
			System.err.println("Could not broadcast border");
			e.printStackTrace();
		}
	}*/

	protected void explore() throws GameActionException{
		if(!rc.onTheMap(rc.getLocation().add(exploreDirections.get(0), rc.getType().sensorRadius - 0.1f))){
			System.out.println("Not on the map");
			// Found an edge
			edgeLocations.add(
				rc.getLocation().add(exploreDirections.get(0), rc.getType().sensorRadius - 0.1f));
			edgeDirections.add(exploreDirections.get(0));
			exploreLocations.remove(0);
			exploreDirections.remove(0);
			if(edgeLocations.size() == 4){
				constructBorder();
			}
		}

		if(!foundBorder && exploreLocations.size() > 0){
			if(rc.getLocation() == exploreLocations.get(0)){
				exploreLocations.remove(0);
				exploreDirections.remove(0);
			}  else{
				System.out.println("Trying to move");
				tryMove(exploreLocations.get(0));
			}
		}
	}

	protected void debugExploreLocs(){
		for(int i=0; i<exploreLocations.size() - 1; i++){
			rc.setIndicatorLine(exploreLocations.get(i), exploreLocations.get(i+1), 0, 254, 0);
		}
		rc.setIndicatorLine(rc.getLocation(), exploreLocations.get(0), 254, 0, 0);
	}

	protected void debugEdgeLocs(){
		for(int i=0; i<edgeLocations.size(); i++){
			rc.setIndicatorDot(edgeLocations.get(i).add(edgeDirections.get(0).opposite(), 5.0f), 0, 254, 0);
		}
		if(edgeLocations.size() > 1){
			for(int i=0; i<edgeLocations.size() - 1; i++){
				rc.setIndicatorLine(edgeLocations.get(i), edgeLocations.get(i+1), 255, 0, 0);
			}
			rc.setIndicatorLine(edgeLocations.get(edgeLocations.size()-1), edgeLocations.get(0), 255, 0, 0);
		}
		if(borderLocations.size() > 0){
			for(int i=0; i<borderLocations.size() - 1; i++){
				rc.setIndicatorLine(borderLocations.get(i), borderLocations.get(i+1), 0, 255, 0);
			}
			rc.setIndicatorLine(borderLocations.get(borderLocations.size()-1), borderLocations.get(0), 255, 0, 0);
		}
	}

	protected void debugTreeLoc(){
		rc.setIndicatorLine(rc.getLocation(), targetTree.getLocation(), 0, 0, 254);
	}
}

