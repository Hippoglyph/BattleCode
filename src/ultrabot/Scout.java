package ultrabot;
import battlecode.common.*;
import java.util.ArrayList;
import java.util.List;

import java.util.Collections;
import java.util.Arrays;

class Scout extends Bot{
	/**
	 * Basic priority of the scout:
	 *  1) If I know about the enemy scout, try to kill it!
	 *  2) If I know about trees, shake them for bullets
	 *  3) If I don't, explore map to determine cordinates of edges
	 *  4) Perhaps we could make it go find the Archon or shoot enemies
	 */
	List<TreeInfo> knownTrees = new ArrayList<TreeInfo>();
	List<TreeInfo> doneTrees = new ArrayList<TreeInfo>();
	List<RobotInfo> knownRobots = new ArrayList<RobotInfo>();
	boolean hasTargetTree = false;
	boolean followingEnemyScout = false;
	boolean hasFoundEnemyScout = false;
	boolean hasAskedForNewScout = false;
	boolean missionFinished = false;

	boolean hasTargetRobot = false;
	RobotInfo targetRobot;
	Direction currentDirection = randomDirection();
	RobotInfo enemyScout;
	TreeInfo targetTree;
	float earning = 0.0f;

	@Override
	protected void beforeStep() throws GameActionException{
		super.beforeStep();
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
			if(debug){debugTreeLoc();}
			dealWithTargetTree();
		} else{
			missionFinished = true;
			killEnemyRobots();
		}

		if(!hasAskedForNewScout && !missionFinished){
			if(likelyToDie()){
				broadCastSpawnNewScout();
				hasAskedForNewScout = true;
			}
		}
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

	protected void killEnemyRobots() throws GameActionException{
		if(hasTargetRobot){
			killTargetRobot();
		} else{
			List<RobotInfo> nearbyRobots = 
				Arrays.asList(rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam().opponent()));
			if(nearbyRobots.size() > 0){
				hasTargetRobot = true;
				targetRobot = nearbyRobots.get(0);
			} else{
				moveRandomly();
			}
		}
	}

	protected void killTargetRobot() throws GameActionException{
		if(rc.canSenseRobot(targetRobot.ID)){
			MapLocation targetLoc = rc.senseRobot(targetRobot.getID()).getLocation();
			if(rc.canFireSingleShot()){
				try{
					rc.fireSingleShot(new Direction(rc.getLocation(), targetLoc));
				} catch(GameActionException e){
					System.err.println("Scout failed while shooting");
					e.printStackTrace();
				}
			}	
			pursuit(targetRobot);
		} else{
			// Hopefully the enemy scout is dead, but maybe
			// we just lost track of him
			hasTargetRobot = false;
		}
	}

	protected void moveRandomly() throws GameActionException{
		if(!rc.onTheMap(rc.getLocation().add(currentDirection, rc.getType().sensorRadius - 0.1f))){
			currentDirection = randomDirection();
			tryMove(currentDirection);
		} else{
			tryMove(currentDirection);
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

	protected void debugTreeLoc(){
		rc.setIndicatorLine(rc.getLocation(), targetTree.getLocation(), 0, 0, 254);
	}
}

