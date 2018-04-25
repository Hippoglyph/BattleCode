package ultrabot;

import battlecode.common.*;


import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

class Gardener extends Bot{
	
	// Broadcast Related
	MapLocation hexaLocation;
	boolean isFirst = false;
	boolean helpRequestSent = false;
	boolean holdSpawnRequested = false;
	boolean freeze = false;
	List<MapLocation> connectedHexaLocs = new ArrayList<MapLocation>();
	
	// Movement Related
	boolean atHexaLocation = false;
	List<MapLocation> locationHistory = new ArrayList<MapLocation>();
	boolean gotStuck = false;
	Direction rerouteDirection = randomDirection();

	
	// Building tree / Spawning Units related
	int numTrees = 0;
	int numLumberjacks = 0;
	int numSoldiers = 0;
	boolean initialScoutSpawned = false;
	boolean isFrontLine = false;
	
	// Tree related
	int wateringInd = 0;
	List<TreeInfo> surroundingTrees = new ArrayList<TreeInfo>();	
	Direction wallBeginDir = new Direction((float)(Math.floor(Math.random()*6)*Math.PI/3));

	@Override
	protected void beforeStep() throws GameActionException{
		super.beforeStep();
		receiveArchonLocations();
		sortArchonLocations();
		receiveHexaLocation();
		receiveIsFirst();

		if(isFirst){
			boolean spawnLumberjackSuccess = trySpawnInitialLumberJack();
		}	
	}

	@Override
	protected void mainStep() throws GameActionException{
		super.mainStep();
		if(rc.readBroadcastBoolean(Constants.SCOUT_DYING) && rc.getTeamBullets()>100){
			initialScoutSpawned = trySpawnInitialScout();
			rc.broadcastBoolean(Constants.SCOUT_DYING, false);
		}
		if(isFirst && !initialScoutSpawned && rc.getTeamBullets()>100){
			initialScoutSpawned = trySpawnInitialScout();
		}
		if (isLumberjackNeeded()) {
			trySpawnInitialLumberJack();
		}
		if(!atHexaLocation && !rc.hasMoved()){
			moveToHexaLocation();
			if(atHexaLocation) {
				setConnectedHexaLocations();
				validateConnectedHexaLocations();
				sortConnectedHexaLocations();
				if(debug){debugConnectedHexaLocations();}
			}
		}
		else{
			if(!isWalled()){
				if(isSoldierNeeded()) {
					if(!rc.hasRobotBuildRequirements(RobotType.SOLDIER) && !holdSpawnRequested){
						askArchonToHoldSpawn();
					}
					else {
						trySpawnSoldier();
					}
				}
				else {
					wallWithTree();
				}
			}
			if (connectedHexaLocs.size() > 0) {
				broadCastConnectedHexLoc();
			}
			if(surroundingTrees.size() > 0){
				waterTree();
			};
			if(debug){
				debugConnectedHexaLocations();
			}
		}
	}
	
	@Override
	protected void afterStep() throws GameActionException{
		super.afterStep();
		//getClosestOpenNodeLocation(hexaLocation);
		//saveLocationHistory();
	}

	// SPAWN METHODS

	protected boolean trySpawnSoldier() throws GameActionException{
		Direction spawnDir;
		for(float rads = 0; rads <2*Math.PI; rads +=Math.PI/6){
			spawnDir = new Direction(rads);
			if(rc.canBuildRobot(RobotType.SOLDIER, spawnDir)){
				rc.buildRobot(RobotType.SOLDIER, spawnDir);
				numSoldiers++;
				if(holdSpawnRequested) {
					removeSpawnRequestToArchon();
					//System.out.println("Removed spawn request");
				}
				holdSpawnRequested=false;
				return true;
			}
		}
		return false;
	}


	/**
	 * Tries to spawn the initial lumberjack, returns success
	 */
	protected boolean trySpawnInitialLumberJack() throws GameActionException{
		Direction spawnDir;
		for(float rads = 0; rads <2*Math.PI; rads +=Math.PI/6){
			spawnDir = new Direction(rads);
			if(rc.canBuildRobot(RobotType.LUMBERJACK, spawnDir)){
				rc.buildRobot(RobotType.LUMBERJACK, spawnDir);
				numLumberjacks++;
				return true;
			}
		}
		return false;
	}

	/**
	 * Tries to spawn the initial scout, returns success
	 */
	protected boolean trySpawnInitialScout() throws GameActionException{
		Direction spawnDir;
		for(float rads = 0; rads <2*Math.PI; rads +=Math.PI/6){
			spawnDir = new Direction(rads);
			if(rc.canBuildRobot(RobotType.SCOUT, spawnDir)){
				rc.buildRobot(RobotType.SCOUT, spawnDir);
				broadCastScoutSpawned();
				return true;
			}
		}
		return false;
	}


	// GARDENER SPECIFICS

	protected void sortArchonLocations(){
		Collections.sort(archonLocations,
			(m1, m2) -> Float.compare(
				distToLoc(m1), distToLoc(m2)));
	}

	protected void moveToHexaLocation() throws GameActionException{
		if(canReachHexaLocation() && rc.canMove(hexaLocation)){
			//System.out.println("I reached my home");
			rc.move(hexaLocation);
			atHexaLocation = true;
		} else {
			if(!isHexaLocationValid(rc, hexaLocation)) {
				receiveNewHexaLocation();
			}
			else {
				if(rc.getLocation().isWithinDistance(hexaLocation, Constants.GARDENER_SIGHT_RAD)) {
					tryMove(rc.getLocation().directionTo(hexaLocation));
				}
				else {
					MapLocation closestHexa = getClosestHexaLocation(hexaLocation);
					MapLocation openNode = getClosestOpenNodeLocation(closestHexa,hexaLocation);
					if(almostReachedLocation(rc, openNode)) {
						tryMove(rc.getLocation().directionTo(hexaLocation));
					} else {
						tryMove(rc.getLocation().directionTo(openNode));

					}
				}
			}
		}
		rc.setIndicatorDot(hexaLocation, 255, 255, 0);
	}

	protected boolean canReachHexaLocation(){
		return rc.getLocation().distanceTo(hexaLocation) <= rc.getType().strideRadius;
	}

	protected void setConnectedHexaLocations(){
		for(int i = 0; i<6 ; i++) {
			connectedHexaLocs.add(hexaLocation.add((2*i+1)*Constants.PI/6, Strategy.HONEYCOMB_SPACING));
		}
	}

	protected void validateConnectedHexaLocations(){
		// Could check for possible collisions here
	}
	
	protected void sortConnectedHexaLocations(){
		Collections.sort(connectedHexaLocs,
			(m1, m2) -> Float.compare(
				archonLocations.get(0).distanceTo(m1), archonLocations.get(0).distanceTo(m2)));
	}

	protected void waterTree(){
        int weakTreeId = surroundingTrees.get(0).getID();
        float minHealth = surroundingTrees.get(0).getHealth();
		TreeInfo wateringTree = surroundingTrees.get(wateringInd % surroundingTrees.size());
		if(rc.canWater(wateringTree.getID())){
			try{
				rc.water(wateringTree.getID());
				wateringInd++;
			} catch (GameActionException e) {
				System.err.println("Gardener failed to water");
				e.printStackTrace();
			}
		}
	}

	protected void wallWithTree() throws GameActionException{
		try{
			for (int i = 0; i < Strategy.NUM_TREE_WALLS; i++){
				Direction dir = new Direction((i*Constants.PI/3)+wallBeginDir.radians);
				if (rc.canPlantTree(dir)) {
					rc.plantTree(dir);
					surroundingTrees.add(rc.senseTreeAtLocation(rc.getLocation().add(dir, 2.0f)));
					numTrees += 1;
				}
			}
		} catch(GameActionException e){
			System.err.println("Gardener failed while planting a tree");
			e.printStackTrace();
		}
	}
	
	protected boolean isWalled(){
		//return numTrees == Strategy.NUM_TREE_WALLS;
		int numWalls = 0;
		for (int i = 0; i < surroundingTrees.size(); i++){
			if(rc.canSenseTree(surroundingTrees.get(i).getID())) {
				numWalls++;
			}
		}
		return numWalls==Strategy.NUM_TREE_WALLS;
	}
	
	protected boolean isLumberjackNeeded() throws GameActionException {
		if(numLumberjacks < Strategy.LUMBERJACK_LIMIT) {
			/*
			if(rc.canSenseAllOfCircle(hexaLocation, RobotType.GARDENER.bodyRadius)) {
				if(rc.isLocationOccupiedByTree(hexaLocation)) {
					if(!rc.senseTreeAtLocation(hexaLocation).team.equals(rc.getTeam())) {
						return true;
					}
				}
			}*/
			if(rc.senseNearbyTrees(rc.getType().sensorRadius, Team.NEUTRAL).length>0){
				return true;
				
			}
		}
		return false;
	}
	
	protected boolean isSoldierNeeded() throws GameActionException {
		if(isFrontLine) {  //need to add the code for this in archon
			return true;
		}
		else if (numSoldiers<Strategy.SPAWN_SOLDIER_LIMIT) {
			return true;
		}
		RobotInfo[] closeEnemies = rc.senseNearbyRobots(Constants.GARDENER_SIGHT_RAD, rc.getTeam().opponent());
		if(closeEnemies.length!=0) {
			return true;
		}
		return false;
	}

	// BROADCAST AND RECEIVE METHODS

	protected void broadCastConnectedHexLoc() throws GameActionException{
		// NEED TO ITERATE HERE OVER ALL CHANNELS TO FIND ONE AVAILABLE AND CHOOSE THAT ONE
		if(!rc.readBroadcastBoolean(Constants.OPEN_HEXA_LOC_SET)) {
			try{
				MapLocation connectedLoc = connectedHexaLocs.remove(0);
				//System.out.println("Trying to submit location: " + connectedLoc.x +", "+ connectedLoc.y);
				rc.broadcastFloat(Constants.OPEN_HEXA_LOC_X, connectedLoc.x);
				rc.broadcastFloat(Constants.OPEN_HEXA_LOC_Y, connectedLoc.y);
				rc.broadcastBoolean(Constants.OPEN_HEXA_LOC_SET, true);
				//System.out.println("Submitted Location");
			} catch(GameActionException e){
				System.err.println("Could not broadcast connected hexalocation");
				e.printStackTrace();
			}
		}
	}

	protected void broadCastScoutSpawned() throws GameActionException{
		try{
			rc.broadcastBoolean(Constants.SCOUT_SPAWNED, true);
		} catch(GameActionException e){
			System.err.println("Could not broadcast scout spawn indicator");
		}
	}

	protected void receiveHexaLocation() throws GameActionException{
		float x = 0;
		float y = 0;
		try{
			x = rc.readBroadcastFloat(Constants.NEW_GARDENER_HOME_X);
			y = rc.readBroadcastFloat(Constants.NEW_GARDENER_HOME_Y);
		} catch(GameActionException e){
			System.err.println("Receiving Home location failed");
			e.printStackTrace();
		}
		hexaLocation = new MapLocation(x, y); 
	}
	
	protected void receiveNewHexaLocation() throws GameActionException{
		float x = 0;
		float y = 0;
		try{
			if(!rc.readBroadcastBoolean(Constants.HELP_GARDENER_REQUESTED)) {
				if (!helpRequestSent) {
					sendHelpRequest();
					helpRequestSent = true;
					//System.out.println("Help request Sent");
				}
				else {
					x = rc.readBroadcastFloat(Constants.HELP_GARDENER_X);
					y = rc.readBroadcastFloat(Constants.HELP_GARDENER_Y);
					hexaLocation = new MapLocation(x,y);
					helpRequestSent = false;
					//System.out.println("Archon helped me find new hex");
					if(debug){rc.setIndicatorLine(rc.getLocation(), hexaLocation, 255, 255, 0);}
				}
			}
				
		} catch(GameActionException e){
			System.err.println("Receiving Home location failed");
			e.printStackTrace();
		} 
	}

	/**
	 * protected void broadCastConnectedHexLoc() throws GameActionException{ 
		if(!rc.readBroadcastBoolean(Constants.OPEN_HEXA_LOC_SET)) {
			try{
				MapLocation connectedLoc = connectedHexaLocs.remove(0);
				//System.out.println("Trying to submit location: " + connectedLoc.x +", "+ connectedLoc.y);
				rc.broadcastFloat(channelClosestX, connectedLoc.x);
				rc.broadcastFloat(channelClosestY, connectedLoc.y);
				rc.broadcastBoolean(Constants.OPEN_HEXA_LOC_SET, true);
				//System.out.println("Submitted Location");
			} catch(GameActionException e){
				System.err.println("Could not broadcast connected hexalocation");
				e.printStackTrace();
			}
		}
	}

	 */

	protected void receiveFreeze() throws GameActionException{
		try{
			freeze = rc.readBroadcastBoolean(Constants.FREEZE);
		} catch(GameActionException e){
			System.err.println("Could not receive freeze indicator");
			e.printStackTrace();
		}
	}
	
	protected void sendHelpRequest() {
		try {
			rc.broadcastBoolean(Constants.HELP_GARDENER_REQUESTED,true);
			rc.broadcastFloat(Constants.HELP_GARDENER_X, rc.getLocation().x);
			rc.broadcastFloat(Constants.HELP_GARDENER_Y, rc.getLocation().y);
		} catch(GameActionException e){
			System.err.println("Could not request for help");
			e.printStackTrace();
		}
	}
	
	protected void askArchonToHoldSpawn() {
		try {
			int holdSpawnRequest = rc.readBroadcast(Constants.ARCHON_HOLD_SPAWN)+1;
			rc.broadcast(Constants.ARCHON_HOLD_SPAWN,holdSpawnRequest);
			holdSpawnRequested = true;
			//rc.broadcastFloat(Constants.HELP_GARDENER_X, rc.getLocation().x);
			//rc.broadcastFloat(Constants.HELP_GARDENER_Y, rc.getLocation().y);
		} catch(GameActionException e){
			System.err.println("Could not broadcast to archon to hold spawn");
			e.printStackTrace();
		}
	}
	
	protected void removeSpawnRequestToArchon() {
		try {
			int holdSpawnRequest = rc.readBroadcast(Constants.ARCHON_HOLD_SPAWN)-1;
			rc.broadcast(Constants.ARCHON_HOLD_SPAWN,holdSpawnRequest);
			holdSpawnRequested = true;
			//rc.broadcastFloat(Constants.HELP_GARDENER_X, rc.getLocation().x);
			//rc.broadcastFloat(Constants.HELP_GARDENER_Y, rc.getLocation().y);
		} catch(GameActionException e){
			System.err.println("Could not broadcast to archon to hold spawn");
			e.printStackTrace();
		}
	}

	protected void receiveIsFirst() throws GameActionException{
		try{
			isFirst = rc.readBroadcastBoolean(Constants.GARDENER_IS_FIRST);
			rc.broadcastBoolean(Constants.GARDENER_IS_FIRST, false);
		} catch(GameActionException e){
			System.err.println("Could not receive isFirst indicator");
			e.printStackTrace();
		}
	}

	protected void debugConnectedHexaLocations() {
		for(MapLocation m: connectedHexaLocs){
			rc.setIndicatorDot(m, 0, 254, 0);
		}
	}
	
}