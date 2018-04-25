package violentfrenchies;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TreeInfo;
import violentfrenchies.Constants;

public class Gardener extends Robot{

	float currentHealth = rc.getType().getStartingHealth();
	int roundCounter = 0;
	int roundSpotCounter = 0;
	int initRound = 0;
	int gardenSize = 0;
	int lumberjackBuilt = 0;
	int scoutBuilt = 0;
	boolean hasGarden = false;
	Direction currentDirection;
	Direction gardenEntrance;
	GardenerState state = GardenerState.INIT;
	
	Gardener(RobotController rc) throws GameActionException {
		super(rc);
	}

	enum GardenerState {
		INIT, FINDING_GARDEN, BUILDING_GARDEN, MASS_SOLDIER
	}
	
	void setGardenEntrance() {
		gardenEntrance = rc.getLocation().directionTo(rc.getInitialArchonLocations(enemy)[0]);
		gardenEntrance = Direction.EAST.rotateRightDegrees(((int) gardenEntrance.getAngleDegrees() / 60) * 60).opposite();
	}

	private boolean findPlace2Be() {		
		RobotInfo[] robots = rc.senseNearbyRobots(Constants.REQUIRED_SPOT_RADIUS);
		TreeInfo[] trees = rc.senseNearbyTrees(Constants.REQUIRED_SPOT_RADIUS);
		
		if(roundSpotCounter>50 || roundSpotCounter==0){
			double rnd = rand.nextDouble();
			if(rnd >0.3){
				currentDirection= rc.getLocation().directionTo(rc.getInitialArchonLocations(rc.getTeam())[0]).opposite();
			}
			else if(robots.length>0){
				currentDirection= rc.getLocation().directionTo(robots[0].getLocation()).opposite();
			}
			currentDirection = randomFreeDirection(currentDirection, 190f);
			roundSpotCounter = 1;
		}
		roundSpotCounter++;
		
		try {
			if(robots.length<2 && trees.length==0 && rc.onTheMap(rc.getLocation(), Constants.REQUIRED_SPOT_RADIUS)){
				return true;
			}
			else{				
				tryMove(currentDirection);
			}
		} catch (GameActionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}

	boolean buildGarden() {
		boolean ret = false;
		if (gardenEntrance == null)
			setGardenEntrance();
		Direction dir = gardenEntrance;
		dir = dir.rotateRightDegrees(60);
		for (int i = 0; i < gardenSize; i++) {
			try {
				if (rc.canPlantTree(dir)) {
					rc.plantTree(dir);
					ret = true;
				}
			} catch (GameActionException e) {
			}
			dir = dir.rotateRightDegrees(60);
		}
		return ret;
	}
	
	void waterGarden() {
		if (gardenEntrance == null)
			setGardenEntrance();
		TreeInfo[] trees = rc.senseNearbyTrees(1.5f, rc.getTeam());
		for (int i = 0; i < trees.length; i++) {
			if (trees[i].getHealth() < trees[i].getMaxHealth()*0.6) {
				if (rc.canWater(trees[i].getLocation())) {
					try {
						rc.water(trees[i].getLocation());
					} catch (GameActionException e) {
					}
				}
				Clock.yield();
			}
		}
		for (int i = 0; i < trees.length; i++) {
			if (rc.canWater(trees[i].getLocation())) {
				try {
					rc.water(trees[i].getLocation());
				} catch (GameActionException e) {
				}
			}
			Clock.yield();
		}
	}

	void checkHealth() {
		float x = GameConstants.MAP_MAX_HEIGHT;
	}

	public void reportUnderAttack(MapLocation enemy2report) {
		int channel = Constants.ENNEMIES_REPORTING_CHANNEL_START;
		int ttl_channel = Constants.ENNEMIES_REPORTING_TTL_CHANNEL_START;
		try {
			for(int i=0; i<Constants.SQUAD_NUNBER; i++){
				int roundAdded = rc.readBroadcast(ttl_channel+i);
				if( (rc.getRoundNum()-roundAdded) > Constants.ENNEMIES_REPORTING_TTL_MAX ){
					rc.broadcastFloat(channel+2*i, enemy2report.x);
					rc.broadcastFloat(channel+2*i+1, enemy2report.y);
					rc.broadcast(ttl_channel+i, rc.getRoundNum());
					int current = rc.readBroadcast(Constants.ENNEMIES_REPORTING_CHANNEL_STATUS);
					rc.broadcast(Constants.ENNEMIES_REPORTING_CHANNEL_STATUS, current + 1);
					//rc.setIndicatorDot(enemy2report, 255, 0, 0);
					return;
				}
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}
	
	private void CheckIfAttacked() {
		if (rc.getHealth() < currentHealth) {
			MapLocation gardenerPos = rc.getLocation();
			reportUnderAttack(gardenerPos);
			currentHealth = rc.getHealth();
		}
		
	}
	
	boolean isNearTrees() {
		TreeInfo[] nearbyTrees = rc.senseNearbyTrees(Constants.GARDENERS_DEFAULT_FREE_SPOT_RADIUS, Team.NEUTRAL);
		if(nearbyTrees.length>=Constants.GARDENER_NUM_OF_TREES_TO_BUILD_LUMBER){
			return true;
		}		
		return false;
	}

	boolean isNearLotsTrees() {
		TreeInfo[] nearbyTrees = rc.senseNearbyTrees(Constants.GARDENERS_DEFAULT_FREE_SPOT_RADIUS, Team.NEUTRAL);
		if(nearbyTrees.length>=Constants.GARDENER_NUM_OF_TREES_TO_BUILD_LUMBER_ALOT){
			return true;
		}		
		return false;
	}
	
	
	boolean isEnemyArchonNear() {
		RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
		for (RobotInfo robot : nearbyRobots) {
			if (robot.getType() == RobotType.ARCHON && robot.getTeam() == enemy)
				return true;
		}
		return false;
	}

	boolean buildRobot(RobotType robotType){
		Direction randomDir = randomDirection();
		boolean ret = false;
		
		double r = rand.nextDouble();
		try {
			if(rc.getTeamBullets()<150 && getRobotCount(RobotType.GARDENER)<5 && getRobotCount(RobotType.LUMBERJACK)>2  )
				return ret;
		} catch (GameActionException e1) {
			e1.printStackTrace();
		}
		
		if(rc.canBuildRobot(robotType, gardenEntrance)){
			try {
				rc.buildRobot(robotType, gardenEntrance);
				ret = true;
			} catch (GameActionException e) {
			}
		}
		else if(rc.canBuildRobot(robotType, randomDir)){
			try {
				rc.buildRobot(robotType, randomDir);
				ret = true;
			} catch (GameActionException e) {
			}
		}
		return ret;
	}

	int RoundSinceNeedLumberjack = 0;
	
	@Override
	void robotBehavior() throws GameActionException {
		roundCounter++;
		CheckIfAttacked();
		int maxRound = 0;	
		switch (state) {
			case INIT:
				initRound = rc.getRoundNum();
				currentState = readGlobalState();
				switch (currentState) {
					case ECONOMY:	
						maxRound = Constants.NUMBER_OF_TOUR_BEFORE_GIVINGUP_GARDEN_SPOT_FINDING_ECO;
						break;
					case BALANCED:						
						maxRound = Constants.NUMBER_OF_TOUR_BEFORE_GIVINGUP_GARDEN_SPOT_FINDING_NORMAL;
						break;
					case MILITARY_RUSH:				
						maxRound = Constants.NUMBER_OF_TOUR_BEFORE_GIVINGUP_GARDEN_SPOT_FINDING_RUSH;		
						break;						
				}	
				if(currentState == GlobalState.MILITARY_RUSH){
					rc.setIndicatorDot(rc.getLocation(), 255, 0, 0);
				}
				else if(currentState == GlobalState.ECONOMY){
					rc.setIndicatorDot(rc.getLocation(), 0, 255, 0);
				}
				else{
					rc.setIndicatorDot(rc.getLocation(), 0, 0, 255);
				}						
				setGardenEntrance();
				if(    lumberjackBuilt<Constants.MAX_LUMBERJACK_PER_GARDENER && isNearLotsTrees()){
					rc.setIndicatorDot(rc.getLocation(), 0, 0, 0);
					if(buildRobot(RobotType.LUMBERJACK)){
						lumberjackBuilt++;
					}
					RoundSinceNeedLumberjack = rc.getRoundNum();
				}
				double r = rand.nextDouble();
				if(r<0.5 && currentState == GlobalState.MILITARY_RUSH || r<0.1 && currentState == GlobalState.ECONOMY || r<0.15 && currentState == GlobalState.BALANCED){
					buildRobot(RobotType.SOLDIER);
				}
				if(r<0.5 && currentState == GlobalState.MILITARY_RUSH || r<0.2 && currentState == GlobalState.ECONOMY || r<0.3 && currentState == GlobalState.BALANCED){
					buildRobot(RobotType.SCOUT);
				}				double rt = rand.nextDouble();
				if(rt<Constants.TANK_SOLDIER_GARDEN_RATIO){
					gardenSize = 4;
				}
				else{
					gardenSize = 5;
				}			
				if( (isEnemyArchonNear() || currentState == GlobalState.MILITARY_RUSH) && rc.canBuildRobot(RobotType.SOLDIER, gardenEntrance) ){
					state = GardenerState.MASS_SOLDIER;
				}
				else{
					state = GardenerState.FINDING_GARDEN;
				}
				break;		
			case FINDING_GARDEN:
				//rc.setIndicatorDot(rc.getLocation(), 255,255, 255);
				if (filterByType(rc.senseNearbyRobots(Constants.GARDENERS_DEFAULT_FREE_SPOT_RADIUS, enemy),
						RobotType.SOLDIER).size() > 0)
					buildRobot(RobotType.SOLDIER);
				
				if(hasGarden){
					state = GardenerState.BUILDING_GARDEN;
					break;
				}				
				else if((roundCounter-initRound)> maxRound){
					state = GardenerState.BUILDING_GARDEN;
					break;
				}
				r = rand.nextDouble();
				if(r<0.5 && currentState == GlobalState.MILITARY_RUSH || r<0.1 && currentState == GlobalState.ECONOMY || r<0.15 && currentState == GlobalState.BALANCED){
					if(getRobotCount(RobotType.SOLDIER)< Constants.MAX_NUMBER_SOLDIERS){
						buildRobot(RobotType.SOLDIER);
					}
				}	
				
				boolean foundPlace2Be = findPlace2Be();				
				if(    lumberjackBuilt<Constants.MAX_LUMBERJACK_PER_GARDENER && isNearTrees() && rc.getRoundNum()<400 ){
						//rc.setIndicatorDot(rc.getLocation(), 0, 0, 0);
						if(buildRobot(RobotType.LUMBERJACK)){
							lumberjackBuilt++;
						}
					RoundSinceNeedLumberjack = rc.getRoundNum();
					break;
				}
				
				if(r<0.2 && currentState == GlobalState.MILITARY_RUSH || r<0.4 && currentState == GlobalState.ECONOMY || r<0.3 && currentState == GlobalState.BALANCED){
					if(scoutBuilt!=0 && getRobotCount(RobotType.SCOUT)< Constants.MAX_NUMBER_SCOUTS){
						buildRobot(RobotType.SCOUT);
						scoutBuilt++;
					}
				}		
				if(foundPlace2Be){
					hasGarden = true;
					state = GardenerState.BUILDING_GARDEN;
				}
				break;		
			case BUILDING_GARDEN:
				buildGarden();		
				waterGarden();	
				if(isEnemyArchonNear() || isEnemyNear() || getRobotCount(RobotType.SOLDIER)< Constants.MAX_NUMBER_SOLDIERS){
					state = GardenerState.MASS_SOLDIER;
					buildRobot(RobotType.SOLDIER);					
				}
				if(getRobotCount(RobotType.SCOUT) < Constants.MAX_NUMBER_SCOUTS && scoutBuilt<1){
					buildRobot(RobotType.SCOUT);
					scoutBuilt++;
				}
				r = rand.nextDouble();
				if ( r<0.4 && lumberjackBuilt<Constants.MAX_LUMBERJACK_PER_GARDENER 
						&& getRobotCount(RobotType.LUMBERJACK) < Constants.MAX_NUMBER_LUMBERJACKS){
					buildRobot(RobotType.LUMBERJACK);
				}
				break;
			case MASS_SOLDIER:
				if(gardenSize == 4){ // TANK
					if(buildRobot(RobotType.TANK) == false){
						state = GardenerState.FINDING_GARDEN;
					}
				}
				else{
					if(buildRobot(RobotType.SOLDIER) == false){
						state = GardenerState.FINDING_GARDEN;
					}
				}
				break;
				
		}
	}

}
