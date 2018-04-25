package ultrabot;

import battlecode.common.*;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;


/**
 * Basic priority of the lumberjack:
 * 	1) If I have a command, follow the command - DEPRECATED
 *  2) If I sense enemies within strike distance, strike
 *  3) If I know of any nearby trees, get to closest one 
 *  and start chopping
 * 	4) If I know of no trees and have no commands, go towards
 *  the enemy Archon and make a mess.
 * 
 * 	Every round I will also sense nearby trees and add them to my
 *  list of known trees
 */

class Lumberjack extends Bot{
	boolean checkForStuckness = true; // FLIP THIS SWITCH
	List<TreeInfo> knownTrees = new ArrayList<TreeInfo>();
	List<RobotInfo> knownEnemies = new ArrayList<RobotInfo>();
	boolean hasTargetEnemy;
	boolean hasTargetTree = false;
	RobotInfo targetEnemy;
	TreeInfo targetTree;
	RobotInfo enemyArchon;
	boolean foundArchon = false;
	boolean enemyArchonIsDead = false;
	boolean noNearByEnemies = false;
	MapLocation previousLocation;
	int tryMoves = 0;


	@Override
	protected void beforeStep() throws GameActionException{
		super.beforeStep();
		receiveArchonLocations();
		sortArchonLocations();
		receiveEnemyArchonLocations();
		sortEnemyArchonLocations();
		senseTrees();
		hasTargetTree = setTargetTree();
	}

	@Override
	protected void mainStep() throws GameActionException{
		super.mainStep();
		if(rc.senseNearbyRobots(2.0f, rc.getTeam().opponent()).length > 0){
			//Only strike if we deal more damage to the enemy
			rc.strike();
		} else if(hasTargetTree){
			dealWithTargetTree();
		} else{
			attackEnemyBase();
		}
		// Always sense trees		
		if(!hasTargetTree){
			senseTrees();
			hasTargetTree = setTargetTree();
		}
	}

	@Override
	protected void afterStep() throws GameActionException{
		super.afterStep();
		//Do something after each round
	}

	protected void sortEnemyArchonLocations(){
		Collections.sort(enemyArchonLocations,
			(m1, m2) -> Float.compare(
				distToLoc(m1), distToLoc(m2)));
	}

	protected void sortArchonLocations(){
		Collections.sort(archonLocations,
			(m1, m2) -> Float.compare(
				distToLoc(m1), distToLoc(m2)));
	}

	protected void dealWithTargetTree() throws GameActionException{
		try{
			if(rc.canInteractWithTree(targetTree.ID)){
				rc.shake(targetTree.ID);
				rc.chop(targetTree.ID);
				tryMoves=0;
			} else{
				if(tryMoves > 20 && findAChoppableTree()){
					//target tree will now have switched
					tryMoves = 0;
					rc.chop(targetTree.getID());
				}
				if(distToLoc(targetTree.location) <= RobotType.LUMBERJACK.sensorRadius 
					&& !rc.canSenseTree(targetTree.getID())){
						//Tree is probably dead
						hasTargetTree = setTargetTree();
				} else{
					tryMove(new Direction(rc.getLocation(), targetTree.location));
					tryMoves++;
				}	
			}
		} catch(GameActionException e){
			System.err.println("Lumberjack encountered an error while dealing with tree");
		} 
	}

	protected boolean findAChoppableTree() throws GameActionException{
		List<TreeInfo> nearbyTrees = new ArrayList<>(
			Arrays.asList(rc.senseNearbyTrees(RobotType.LUMBERJACK.sensorRadius, Team.NEUTRAL)));
		List<TreeInfo> enemyTrees = new ArrayList<>(
			Arrays.asList(rc.senseNearbyTrees(RobotType.LUMBERJACK.sensorRadius, rc.getTeam().opponent())));
		nearbyTrees.addAll(enemyTrees);
		if(nearbyTrees.size() > 0){
			Collections.sort(nearbyTrees,
				(t1, t2) -> Float.compare(distToLoc(t1.location), (distToLoc(t2.location))));
			if(rc.canInteractWithTree(nearbyTrees.get(0).getID())){
				knownTrees.remove(targetTree);
				knownTrees.add(targetTree);
				targetTree = nearbyTrees.get(0);
				return true;
			} else{
				return false;
			}
		} else{
			return false;
		}
	}

	protected void attackEnemyBase() throws GameActionException{
		if(!foundArchon){
			if(distToLoc(enemyArchonLocations.get(0)) < rc.getType().sensorRadius){
				scanForEnemyArchon();
			} else{
				tryMove(enemyArchonLocations.get(0));
			}
		} else if(!enemyArchonIsDead){
			attackEnemyArchon();
		} else if(!noNearByEnemies){
			strikeNearByEnemies();
		} else{
			//headToHomeBase(); ???
			// Use kill enemies from the scout class
		}
	}
	
	protected void scanForEnemyArchon() {
		RobotInfo[] closeRobots =  rc.senseNearbyRobots();
		for(int i=0; i<closeRobots.length; i++){
			if(closeRobots[i].getType().equals(RobotType.ARCHON) 
			&& closeRobots[i].getTeam().equals(rc.getTeam().opponent())){
				//found enemy archon
				enemyArchon = closeRobots[i];
				// LOCATION FIX !!!!
				enemyArchonLocations.add(0,enemyArchon.getLocation());
				foundArchon = true;
				break;
			}
		}
		if(!foundArchon){
			foundArchon = true;
			enemyArchonIsDead = true;
		}
	}

	protected void attackEnemyArchon() throws GameActionException{
		// LOCATION FIX !!!!
		if(distToLoc(enemyArchonLocations.get(0)) < 2.0f){
			if(rc.canSenseRobot(enemyArchon.getID())){
				rc.strike();
			} else{
				enemyArchonIsDead = true;
			}
		} else{
			// LOCATION FIX !!!!
			tryMove(enemyArchonLocations.get(0));
		}
	}

	protected void strikeNearByEnemies() throws GameActionException{
		if(knownEnemies.size() > 0){
			if(hasTargetEnemy){
				attackTargetEnemy();
			} else{
				hasTargetEnemy = true;
				targetEnemy = knownEnemies.remove(0);
			}
		} else{
			List<RobotInfo> newRobots = Arrays.asList(rc.senseNearbyRobots(rc.getType().sensorRadius, 
			rc.getTeam().opponent()));
			if(newRobots.size() == 0){
				noNearByEnemies = true;
			} else{
				knownEnemies.removeAll(newRobots);
				knownEnemies.addAll(newRobots);
				sortKnownEnemies();
			}
		}
	}

	protected void attackTargetEnemy() throws GameActionException{
		if(distToLoc(targetEnemy.getLocation()) <= 2.0f){
			if(rc.canSenseRobot(targetEnemy.getID())){
				rc.strike();
			} else{
				//Probably dead
				hasTargetEnemy = false;
			}
		} else{
			tryMove(targetEnemy.getLocation());
		}
	}

	protected void sortKnownEnemies(){
		Collections.sort(knownEnemies,
				(t1, t2) -> Float.compare(distToLoc(t1.location), (distToLoc(t2.location))));
	}
	
	protected void senseTrees(){
		List<TreeInfo> newTrees = new ArrayList<>(
			Arrays.asList(rc.senseNearbyTrees(RobotType.LUMBERJACK.sensorRadius, Team.NEUTRAL)));
		List<TreeInfo> enemyTrees = new ArrayList<>(
			Arrays.asList(rc.senseNearbyTrees(RobotType.LUMBERJACK.sensorRadius, rc.getTeam().opponent())));
		
		newTrees.addAll(enemyTrees);
		// removve target tree
		if(hasTargetTree){
			newTrees.remove(targetTree);
		}
		//Quick and dirty way of removing duplicates		
		if(newTrees.size()>0){
			knownTrees.removeAll(newTrees);
			knownTrees.addAll(newTrees);
			sortKnownTrees();
		} 
	}

	protected void sortKnownTrees(){
		/**
		 * Does not work that well
		 * Collections.sort(knownTrees,
				(t1, t2) -> Float.compare(
					distToLoc(t1.location)+t1.location.distanceTo(archonLocation), 
					distToLoc(t2.location)+t2.location.distanceTo(archonLocation)));
		 */
		Collections.sort(knownTrees,
				(t1, t2) -> Float.compare(
					distToLoc(t1.location)+t1.location.distanceTo(archonLocations.get(0)), 
					distToLoc(t2.location)+t2.location.distanceTo(archonLocations.get(0))));
	}

	protected boolean setTargetTree(){
		if(!knownTrees.isEmpty()){
			targetTree = knownTrees.remove(0);
			return true;
		} else{
			return false;
		}
	}
}