package violentfrenchies;

import java.util.Arrays;
import java.util.Optional;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.TreeInfo;
import violentfrenchies.Constants;

public class Soldier extends Robot {

	private Direction dir;
	private int squad = 0;
	
	Soldier(RobotController rc) throws GameActionException {
		super(rc);
		dir = randomDirection();
		squad = rand.nextInt(Constants.SQUAD_NUNBER);
		currentState = readGlobalState();
	}

	private boolean shouldFireTriad(RobotInfo victim) {
		Direction dir = rc.getLocation().directionTo(victim.getLocation());
		float offset = GameConstants.TRIAD_SPREAD_DEGREES;
		return haveEnoughBullets() && isEnemy(nearestInDirection(dir.rotateLeftDegrees(offset)))
				&& isEnemy(nearestInDirection(dir.rotateRightDegrees(offset)));
	}

	private boolean haveEnoughBullets() {
		return rc.canFireTriadShot() && rc.getTeamBullets() > Constants.MINIMUM_BULLETS_TO_SAVE_BY_SOLDIER;
	}
	
	private Direction getDirToEnemyArchonInitLoc() {
		MapLocation[] archons = rc.getInitialArchonLocations(enemy);
		MapLocation rndArchon = archons[(int) Math.floor(Math.random() * archons.length)];
		Direction directionToArchonInit = rc.getLocation().directionTo(rndArchon);
		return directionToArchonInit;
	}

	private void changeMoveDirection() {
		if (Math.random() < Constants.SOLDIER_RANDOM_MOVE_PROB) {
			dir = randFreeDirection();
		} else {
			dir = getDirToEnemyArchonInitLoc();
		}
	}
	
	private RobotInfo getTarget() {
		RobotInfo[] enemies = rc.senseNearbyRobots(-1, enemy);
		MapLocation myLoc = rc.getLocation();
		if (enemies.length == 0)
			return null;
		RobotInfo[] friends = rc.senseNearbyRobots(-1, rc.getTeam());
		TreeInfo[] myTrees = rc.senseNearbyTrees(-1, rc.getTeam());
		for (RobotInfo enemy : enemies) {
			float distanceToEnemy = enemy.getLocation().distanceTo(myLoc);
			Direction dirToEnemy = myLoc.directionTo(enemy.getLocation());
			if (!willIHitSomething(dirToEnemy, friends, distanceToEnemy)
					&& !willIHitSomething(dirToEnemy, myTrees, distanceToEnemy))
				return enemy;
		}
		return null;
	}
	
	private void updateReportedCount() throws GameActionException {				
		int channel = Constants.ENNEMIES_REPORTING_CHANNEL_STATUS+2*squad;		
		int current = rc.readBroadcast(channel);
		rc.broadcast(channel, current + 1);
	}

	
	private int checkReportedEnemiesCount(){
		try {
			return rc.readBroadcast(Constants.ENNEMIES_REPORTING_CHANNEL_STATUS+2*squad);
		} catch (GameActionException e) {
			//e.printStackTrace();
		}
		return 0;
	}
	
	public MapLocation readReportedEnnemies() {
		int count = 0;
		count = checkReportedEnemiesCount();
		if(count == 0){
			return null;
		}		
		int channel = Constants.ENNEMIES_REPORTING_CHANNEL_START+2*squad;
		int ttl_channel = Constants.ENNEMIES_REPORTING_TTL_CHANNEL_START+2*squad;
		MapLocation allyArchonPos = rc.getInitialArchonLocations(rc.getTeam())[0];
		float dist_base_enemy = Float.MAX_VALUE;
		MapLocation selectedPos = null;
		try {
			float x = rc.readBroadcastFloat(channel);
			float y = rc.readBroadcastFloat(channel+1);
			MapLocation ePos = new MapLocation(x, y);
			int ttl = rc.readBroadcast(ttl_channel);
			if( (rc.getRoundNum()-ttl) > Constants.ENNEMIES_REPORTING_TTL_MAX || ttl==0){
				return null;
			}
			float distance = allyArchonPos.distanceTo(ePos);
			if(distance < dist_base_enemy){
				dist_base_enemy = distance;
				selectedPos = ePos;
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		return selectedPos;
	}
	
	public void reportEnnemy(MapLocation enemy2report) {
		int channel = Constants.ENNEMIES_REPORTING_CHANNEL_START+2*squad;
		int ttl_channel = Constants.ENNEMIES_REPORTING_TTL_CHANNEL_START+2*squad;
		try {
			int roundAdded = rc.readBroadcast(ttl_channel);
			if( (rc.getRoundNum()-roundAdded) > Constants.ENNEMIES_REPORTING_TTL_MAX ){
				rc.broadcastFloat(channel, enemy2report.x);
				rc.broadcastFloat(channel+1, enemy2report.y);
				rc.broadcast(ttl_channel, rc.getRoundNum());
				updateReportedCount();
				rc.setIndicatorDot(enemy2report, 255, 192, 203);
				return;
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	void robotBehavior() throws GameActionException {
		RobotInfo target = getTarget();
		MapLocation targetLocation = null;
		if(target != null ){
			targetLocation = target.getLocation();
		}
		
		if(rc.hasMoved() == false){
			RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.LUMBERJACK_STRIKE_RADIUS*2.5f);
			Optional<RobotInfo> nearEnemyLb = Arrays.stream(robots)
					.filter(robot -> robot.getTeam() == enemy)
					.filter(robot -> robot.getType() == RobotType.LUMBERJACK)
					.findFirst();

			int nbReports = checkReportedEnemiesCount();
			if(nbReports>0){
				MapLocation reported = readReportedEnnemies();
				if(reported != null){
					dir = rc.getLocation().directionTo(reported);
					/*if(squad == 0)
						rc.setIndicatorLine(rc.getLocation(), reported, 0, 255, 0);
					if(squad == 1)
						rc.setIndicatorLine(rc.getLocation(), reported, 0, 0, 255);
					if(squad == 2)
						rc.setIndicatorLine(rc.getLocation(), reported, 255, 0, 0);
					if(squad == 3)
						rc.setIndicatorLine(rc.getLocation(), reported, 255, 255, 0);
					if(squad == 4)
						rc.setIndicatorLine(rc.getLocation(), reported, 0, 255, 255);*/
				}
			}
			// hit and run
			if(nearEnemyLb.isPresent()){
				rc.setIndicatorDot(rc.getLocation(), 100, 0, 100);
				dir = rc.getLocation().directionTo(nearEnemyLb.get().getLocation()).opposite();
				tryMove(dir);
			}
						
			if (target != null && (target.getType()!=RobotType.TANK || rc.getLocation().distanceTo(targetLocation) > Constants.MAX_SOLDIER_TO_SOLDIER_DISTANCE  )) {
				// Move in the direction of the enemy
				dir = rc.getLocation().directionTo(targetLocation);
			}
			else{
				if (!rc.canMove(dir)) {
					// Move randomly
					changeMoveDirection();
				}
			}
			tryMove(dir);
		}
		if (target != null) {
			rc.setIndicatorDot(targetLocation, 0, 192, 203);
			reportEnnemy(targetLocation);	
			double r = rand.nextDouble();	
			if (shouldFireTriad(target)) {
				double rnd = rand.nextDouble();
				if (rnd < 0.8)
					rc.firePentadShot(rc.getLocation().directionTo(targetLocation));
				else
					rc.fireTriadShot(rc.getLocation().directionTo(targetLocation));
				return;
			} else if (rc.canFireTriadShot()) {
				rc.fireTriadShot(rc.getLocation().directionTo(targetLocation));
				return;
			}
		}
	}


}
