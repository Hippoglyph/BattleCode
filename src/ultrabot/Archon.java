package ultrabot;
import battlecode.common.*;


import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;

class Archon extends Bot{
	Direction hidingDirection;
	Direction hiredDirection;
	boolean isSafe = false;
	boolean initalGardenerSpawnSuccess = false;
	boolean scoutSpawned = false;
	boolean donateAll = false;
	float pointGradient = Constants.POINT_GRADIENT;
	int donatingRounds = 0;
	float incomeRate = 0.0f;
	float previousIncome = 0.0f;
	// openHexaLocs contains location in the honeycomb ordered
	// by their distance to the Archon, closest first.
	List<MapLocation> openHexaLocs = new ArrayList<MapLocation>();
	List<MapLocation> closedHexaLocs = new ArrayList<MapLocation>();
	@Override
	protected void beforeStep() throws GameActionException{
		super.beforeStep();
		resetHexLoc();
		setAllArchonLocations();
		broadCastEnemyArchonLocations();
		broadCastArchonLocations();
		broadCastNumArchons();
		/**
		 * TODO: Archon never tries to hide and never spawns
		 * an inital gardener as per inital plan
		 */
		setHidingDirection();
		initalGardenerSpawnSuccess = trySpawnInitialGardener();
	}

	@Override
	protected void mainStep() throws GameActionException{
		super.mainStep();
		receiveHexaLoc();
		if(!isSafe){
			moveToHiding();
		}
		if(!initalGardenerSpawnSuccess) {
			trySpawnInitialGardener();
		}
		else if(scoutSpawned){
			if(!pauseSpawn())
			{
				trySpawnGardener();
			}
			if(debug){debugOpenHexaLocs();}
			//debugClosedHexaLocs();
			processGardenerHelpRequest();
		} else{
			receiveScoutSpawned();
		}
	}

	@Override
	protected void afterStep() throws GameActionException{
		super.afterStep();
		checkForWictory();
	}

	protected void setIncomeRate(){
		float currIncome = rc.getTeamBullets();
		incomeRate = currIncome - previousIncome;
		previousIncome = currIncome;
	}

	protected void checkForWictory() throws GameActionException{
		setIncomeRate();
		// If we can donate current income rate over MAX_FREEZE_ROUNDS
		if(!donateAll && rc.getRoundNum() > 100 && incomeRate > 0){
			float medianCost = computePointCostAtRound(rc.getRoundNum()+ Math.round(Strategy.MAX_FREEZE_ROUNDS/2)); // 15
			float missingIncome = 1000 - previousIncome/rc.getVictoryPointCost(); // 1000 - 100/15 ~ 990
			int neededRounds = Math.round(medianCost * missingIncome / incomeRate); // (15*990)/300 = 49.5
			if(Strategy.MAX_FREEZE_ROUNDS >= neededRounds){
				// Can win in 100 rounds, freeze everything and donate all
				donateAll = true;
				rc.broadcastBoolean(Constants.FREEZE, true);
			}
		}
		float teamBullets = rc.getTeamBullets();
		float teamScore = rc.getTeamVictoryPoints();
		// Simply check if we can donate all to reach 1000 points
		if(teamScore + teamBullets/rc.getVictoryPointCost() >= 1000 - rc.getTeamVictoryPoints()){
			try{
				rc.donate(teamBullets);
			} catch(GameActionException e){
				System.err.println("Archon failed while donating");
				e.printStackTrace();
			}
		}
	}
	
	protected float computePointCostAtRound(int round){
		return pointGradient*round + 7.5f;
	}

	protected void donateAll() throws GameActionException{
		rc.donate(rc.getTeamBullets());
	}

	protected void setAllArchonLocations(){
		Team them = rc.getTeam().opponent();
		enemyArchonLocations = Arrays.asList(rc.getInitialArchonLocations(them));
		archonLocations = Arrays.asList(rc.getInitialArchonLocations(rc.getTeam()));
	}

	protected void setHidingDirection(){
		MapLocation vectorSum = rc.getLocation();
		for(int i = 0; i<enemyArchonLocations.size(); i++) {
			float distSq = rc.getLocation().distanceSquaredTo(enemyArchonLocations.get(i));
			Direction dir = enemyArchonLocations.get(i).directionTo(rc.getLocation());
			if (distSq>0) {
				vectorSum = vectorSum.add(dir, 1/distSq);
			}			
		}
		hidingDirection = new Direction(rc.getLocation(),vectorSum);
		//hidingDirection = new Direction(enemyArchonLocations.get(0), rc.getLocation());
	}

	protected void moveToHiding() throws GameActionException{
		try{
			MapLocation prevLoc = rc.getLocation();
			tryMove(hidingDirection, 20, 13);
			if(rc.getRoundNum()>Strategy.ARCHON_TRY_HIDE_MAX_TURNS) {
				isSafe = true;
			}
		} catch(GameActionException e){
			System.err.println("Could not move Archon");
			e.printStackTrace();
		}
	}

	/**
	 * Tries to spawn a gardener in front of itself towards the enemy archon
	 */
	protected boolean trySpawnInitialGardener() throws GameActionException{	
		Direction spawnDirection;
		MapLocation myLocation = rc.getLocation();
		// LOCATION - FIX !!!
		spawnDirection = myLocation.directionTo(enemyArchonLocations.get(0));
		boolean hired = tryHireGardener(spawnDirection);
		MapLocation newGardenerHome = myLocation.add(hiredDirection, Strategy.INITIAL_GARD_HOME_DIST);
		broadcastNewGardenerHome(newGardenerHome);
		try {
			rc.broadcastBoolean(Constants.GARDENER_IS_FIRST, true);
		} catch(GameActionException e){
			System.err.println("Could not set that this is first gardener");
			e.printStackTrace();
		}
		if(debug){
			rc.setIndicatorDot(myLocation.add(hiredDirection, Strategy.INITIAL_GARD_HOME_DIST), 254, 254, 0);
		}
		return hired;
	}

	protected void receiveScoutSpawned() throws GameActionException{
		try{
			if(rc.readBroadcastBoolean(Constants.SCOUT_SPAWNED)){
				scoutSpawned = true;
			}
		} catch(GameActionException e){
			System.err.println("Could not receive scout spawn indicator");
			e.printStackTrace();
		}
	}
	
	/**
	 * Sets the home location for the newly hired gardener.
	 * The gardener plans to reach its new home
	 */
	protected void broadcastNewGardenerHome(MapLocation home) {
		try{
			rc.broadcastFloat(Constants.NEW_GARDENER_HOME_X, home.x);
			rc.broadcastFloat(Constants.NEW_GARDENER_HOME_Y, home.y);
			rc.broadcastBoolean(Constants.NEW_GARDENER_HOME_SET, true);
			rc.setIndicatorDot(home, 254, 254, 0);
		} catch(GameActionException e){
			System.err.println("Could not set new gardener's home");
			e.printStackTrace();
		}
	}
	
	protected void processGardenerHelpRequest() {
		try{
			if(!openHexaLocs.isEmpty() && rc.readBroadcastBoolean(Constants.HELP_GARDENER_REQUESTED)) {
				float x = rc.readBroadcastFloat(Constants.HELP_GARDENER_X);
				float y = rc.readBroadcastFloat(Constants.HELP_GARDENER_Y);
				MapLocation newHexa = getClosestOpenHexaLocs(new MapLocation(x,y));
				
				rc.broadcastFloat(Constants.HELP_GARDENER_X, newHexa.x);
				rc.broadcastFloat(Constants.HELP_GARDENER_Y, newHexa.y);
				rc.broadcastBoolean(Constants.HELP_GARDENER_REQUESTED,false);
			}
		} catch(GameActionException e){
			System.err.println("Could not set process gardener help request");
			e.printStackTrace();
		}
	}

	protected void receiveHexaLoc() throws GameActionException {
		float newX, newY;
		MapLocation newHexaLoc;
		try{
			if(rc.readBroadcastBoolean(Constants.OPEN_HEXA_LOC_SET)){
				newX = rc.readBroadcastFloat(Constants.OPEN_HEXA_LOC_X);
				newY = rc.readBroadcastFloat(Constants.OPEN_HEXA_LOC_Y);
				newHexaLoc = new MapLocation(newX, newY);
				if(!openHexaLocs.contains(newHexaLoc) && isHexaLocationValid(rc, newHexaLoc) && !closedHexaLocs.contains(newHexaLoc)){
					openHexaLocs.add(newHexaLoc);
					sortOpenHexaLocs();
					//System.out.println("Received a free Hex Location");

				}
				resetHexLoc();
				//rc.broadcastBoolean(Constants.OPEN_HEXA_LOC_SET, false);
			}
		} catch(GameActionException e){
			System.err.println("Error while receiving hexagon locations in Archon");
			e.printStackTrace();
		}
	}

	protected void sortOpenHexaLocs(){
		MapLocation currentLoc = rc.getLocation();
		Collections.sort(openHexaLocs,
			(m1, m2) -> Float.compare(
				currentLoc.distanceTo(m1), currentLoc.distanceTo(m2)));
	}
	
	protected MapLocation getClosestOpenHexaLocs(MapLocation fromLocation){
		MapLocation closestLoc = openHexaLocs.get(0);
		float minDist = closestLoc.distanceTo(fromLocation);
		for(MapLocation location: openHexaLocs) {
			if (location.distanceTo(fromLocation)<minDist){
				closestLoc = location;
			}
		}
		return closestLoc;
	}
	
	protected boolean trySpawnGardener() throws GameActionException{
		if(!openHexaLocs.isEmpty()){
			MapLocation newGardenerHome = openHexaLocs.get(0);
			boolean hired = tryHireGardener(rc.getLocation().directionTo(newGardenerHome));
			rc.setIndicatorLine(rc.getLocation(), newGardenerHome, 0, 0, 255);
			if (hired) {
				broadcastNewGardenerHome(newGardenerHome);
				openHexaLocs.remove(0);
				closedHexaLocs.add(newGardenerHome);
			}
			if(debug){
				rc.setIndicatorDot(newGardenerHome, 254, 254, 0);
			}
			return true;
		} else{
			// Find your own hex, you bum!
		}
		return false;
	}
	
	protected boolean pauseSpawn() throws GameActionException{
		int holdRequestNum = processGardenerHoldRequest();
		if ((holdRequestNum*100)>rc.getTeamBullets()) {
			//System.out.println("Number of hold requests = "+holdRequestNum);
			return true;
		}
		
		return false;
	}
	
	protected int processGardenerHoldRequest() throws GameActionException{
		int numRequests = 0;
		try {
			numRequests = rc.readBroadcast(Constants.ARCHON_HOLD_SPAWN);
		} catch(GameActionException e){
			System.err.println("Could not read hold request from gardeners");
			e.printStackTrace();
		}
		return numRequests;
	}

	protected void broadCastArchonLocations() throws GameActionException{
		List<Integer[]> channels = getFriendlyArchonChannels(); 
		try{
			for(int i = 0; i<archonLocations.size(); i++){
				rc.broadcastFloat(channels.get(i)[0], archonLocations.get(i).x);
				rc.broadcastFloat(channels.get(i)[1], archonLocations.get(i).y);
			}
			
		} catch(GameActionException e){
			System.err.println("Could not broadcast location in Archon");
			e.printStackTrace();
		}
	}

	protected void broadCastEnemyArchonLocations() throws GameActionException{
		List<Integer[]> channels = getEnemyArchonChannels(); 
		try{
			for(int i = 0; i<enemyArchonLocations.size(); i++){
				rc.broadcastFloat(channels.get(i)[0], enemyArchonLocations.get(i).x);
				rc.broadcastFloat(channels.get(i)[1], enemyArchonLocations.get(i).y);
			}
			
		} catch(GameActionException e){
			System.err.println("Could not broadcast location in Archon");
			e.printStackTrace();
		}
	}

	protected void broadCastNumArchons() throws GameActionException{
		try{
			rc.broadcastInt(Constants.NUM_ARCHONS, archonLocations.size());
		} catch(GameActionException e){
			System.err.println("Could not broadcast number of archons");
			e.printStackTrace();
		}
	}
	
	protected void debugOpenHexaLocs() throws GameActionException{
		for (int i = 0; i < openHexaLocs.size(); i++) {
			rc.setIndicatorDot(openHexaLocs.get(i), 254, 0, 0);
		}
	}
	protected void debugClosedHexaLocs() throws GameActionException{
		for (int i = 0; i < closedHexaLocs.size(); i++) {
			rc.setIndicatorDot(closedHexaLocs.get(i), 254, 0, 0);
		}
	}
	
	/**
	 * Reset the data in channels to default values
	 */
	protected void resetHexLoc(){
		try {
			rc.broadcastFloat(Constants.OPEN_HEXA_LOC_X,0);
			rc.broadcastFloat(Constants.OPEN_HEXA_LOC_Y,0);
			rc.broadcastBoolean(Constants.OPEN_HEXA_LOC_SET, false);
		} catch (GameActionException e) {
			System.err.println("Archon: Could not reset channel data");
			e.printStackTrace();
		}
	}
	
	protected boolean tryHireGardener(Direction dir) {
		try {
			// First, try intended direction
		    if (rc.canHireGardener(dir)) {
		        rc.hireGardener(dir);
		        hiredDirection = dir;
		        return true;
		    }
		
		    // Now try a bunch of similar angles
		    int currentCheck = 1;
		    Direction newDir = dir;
		    while(currentCheck<=Strategy.CHECKS_PER_SIDE) {
		    	
		    	// Try the offset of the left side
		    	newDir = newDir.rotateLeftDegrees(Strategy.DEGREE_OFFSET_STEP*currentCheck);
		        if(rc.canHireGardener(newDir)) {
		            rc.hireGardener(newDir);
		            hiredDirection = newDir;
		            return true;
		        }
		        // Try the offset on the right side
		        newDir = newDir.rotateRightDegrees(Strategy.DEGREE_OFFSET_STEP*currentCheck);
		        if(rc.canHireGardener(newDir)) {
		            rc.hireGardener(newDir);
		            hiredDirection = newDir;
		            return true;
		        }
		        // No hire performed, try slightly further
		        currentCheck++;
		    }
		    
		} catch (GameActionException e) {
			System.err.println("Could not hire gardener");
			e.printStackTrace();
		}
		
		// A hire never happened, so return false.
        return false;
	}
} 