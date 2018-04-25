package violentfrenchies;


import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TreeInfo;
import violentfrenchies.Constants;

public class Archon extends Robot {

	Archon(RobotController rc) throws GameActionException {
		super(rc);
	}
	
	private boolean isDiscoveryOver() {
		int channel = Constants.ARCHON_DISCOVERY_STATUS_CHANNEL;
		try {
			int current = rc.readBroadcast(channel);
			return current == 1;
		} catch (GameActionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	private void broadcastDiscoveryOver(){
		int channel = Constants.ARCHON_DISCOVERY_STATUS_CHANNEL;
		try {
			rc.broadcast(channel,1);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	private void broadcastDiscovery(GlobalState s){
		int channel = Constants.GLOBAL_MODE_CHANNEL;
		try {
			rc.broadcast(channel,s.getValue());
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	int roundCounter = 0;
	Direction currentDir = randomDirection();
	int step = 0;
	
	@Override
	void robotBehavior() throws GameActionException {
		roundCounter++;
		
		if(isDiscoveryOver() == false){			
			broadcastDiscoveryOver();
			float InitialDistanceToEnemy = Float.MAX_VALUE;
			MapLocation[] EnemyArchons = rc.getInitialArchonLocations(enemy);
			for(MapLocation archon: EnemyArchons){
				if(rc.getLocation().distanceTo(archon)<InitialDistanceToEnemy ){
					InitialDistanceToEnemy = rc.getLocation().distanceTo(archon);
				}
			}
			TreeInfo[] nearbyTrees = rc.senseNearbyTrees(-1, Team.NEUTRAL);
			int nbTreesInRange = nearbyTrees.length;
			GlobalState mode;
			if(InitialDistanceToEnemy < Constants.DISTANCE_TO_ENEMY_SMALL && nbTreesInRange<Constants.INITAL_MIN_NUMBER_TREES){
				mode = GlobalState.MILITARY_RUSH;
				rc.setIndicatorDot(rc.getLocation(), 255, 0, 0);
			}
			else if( (InitialDistanceToEnemy > Constants.DISTANCE_TO_ENEMY_SMALL
						&& InitialDistanceToEnemy < Constants.DISTANCE_TO_ENEMY_LARGE && nbTreesInRange>Constants.INITAL_MAX_NUMBER_TREES)
					|| (InitialDistanceToEnemy > Constants.DISTANCE_TO_ENEMY_LARGE) ){
				mode = GlobalState.ECONOMY;
				rc.setIndicatorDot(rc.getLocation(), 0, 255, 0);
			}
			else{
				mode = GlobalState.BALANCED;
				rc.setIndicatorDot(rc.getLocation(), 0, 0, 255);
			}						
			broadcastDiscovery(mode);
		}
		if (roundCounter > 25) {
			roundCounter = 0;
			currentDir = randomDirection();
			while (Math.abs(currentDir.getAngleDegrees() - rc.getLocation()
					.directionTo(rc.getInitialArchonLocations(enemy)[0]).getAngleDegrees()) < 40)
				currentDir = randomDirection();

		}
		if (!rc.onTheMap(rc.getLocation(), 3)) {
			roundCounter += 5;
		}
		RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
		if (robots.length > 0) {
			Direction toEnemy = rc.getLocation().directionTo(robots[0].getLocation());
			tryMove(toEnemy.opposite());
		} else {
			tryMove(currentDir);
		}
		Direction dir = randomDirection();
		if (rc.getRoundNum() >= 0 && rc.getRoundNum() < 60 && step == 0) {
			if (rc.getTeamBullets()> 110) {
				rc.hireGardener(dir);
				step++;
			}
		} else if (rc.getRoundNum() >= 60 && rc.getRoundNum() < 120 && step == 1) {
			if (rc.getTeamBullets()> 110) {
				rc.hireGardener(dir);
				step++;
			}
		} else if (rc.getRoundNum() >= 120 && rc.getRoundNum() < 180 && step == 2) {
			if (rc.getTeamBullets()> 110) {
				rc.hireGardener(dir);
				step++;
			}
		} else {
			float maxBullets = (float)Constants.MINIMUM_BULLETS_TO_SAVE;
			if(currentState == GlobalState.ECONOMY){
				maxBullets = maxBullets*0.3f;
			}
			else if(currentState == GlobalState.BALANCED){
				maxBullets = maxBullets*0.5f;
			}
			if (rc.getTeamBullets() < maxBullets) {
				return;
			}
			if (rc.canHireGardener(dir) && Constants.DESIRED_NUMBER_OF_GARDENERS > getRobotCount(RobotType.GARDENER)) {
				rc.hireGardener(dir);
			}
		}
	}
}
