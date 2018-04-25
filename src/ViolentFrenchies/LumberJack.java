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

public class LumberJack extends Robot {

	private Direction dir;
	int counter;
	MapLocation enemyLocation;
	boolean helper = false;


	LumberJack(RobotController rc) throws GameActionException {
		super(rc);
		dir = randomDirection();
		counter = 0;
		enemyLocation = rc.getInitialArchonLocations(rc.getTeam().opponent())[0];
		currentState = readGlobalState();
		if(getRobotCount()<3){
			helper = true;
		}
	}
	
	boolean areFriendlyRobotsTooClose() {
		return rc.senseNearbyRobots(GameConstants.LUMBERJACK_STRIKE_RADIUS, rc.getTeam()).length != 0;
	}
	
	boolean strikeIfPossible(RobotInfo[] robots, boolean friendlyDmgAllowed) {
		if (robots.length > 0 && !rc.hasAttacked()) {
			if ((areFriendlyRobotsTooClose()==false || friendlyDmgAllowed) && rc.canStrike()) {
				try {
					rc.strike();
				} catch (GameActionException e) {
					e.printStackTrace();
				}
				return true;
			}
		}
		return false;

	}

	void moveRandomly() {
		if (!rc.canMove(dir, rc.getType().strideRadius*0.5f)) {
			dir = randFreeDirection();
		}
		if (!tryMove(dir)) {
			if (rc.canMove(dir, rc.getType().strideRadius*0.5f))
				try {
					rc.move(dir, rc.getType().strideRadius*0.5f);
				} catch (GameActionException e) {
					e.printStackTrace();
				}
		}
	}

	public MapLocation readReportedEnnemies() throws GameActionException {
		if(rc.readBroadcast(Constants.ENNEMIES_REPORTING_CHANNEL_STATUS)<=0)
			return null;
		int channel = Constants.ENNEMIES_REPORTING_CHANNEL_START;
		int ttl_channel = Constants.ENNEMIES_REPORTING_TTL_CHANNEL_START;
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
	
	@Override
	void robotBehavior() throws GameActionException {
		RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);
		if (!strikeIfPossible(robots, false)){
			TreeInfo[] trees = rc.senseNearbyTrees();
			
			
			if(rc.getRoundNum()<400 || helper){
				rc.setIndicatorDot(rc.getLocation(), 120, 240, 40);
				for (TreeInfo treeInfo : trees) {
					if (rc.canChop(treeInfo.ID) && treeInfo.team != rc.getTeam()) {
						rc.chop(treeInfo.ID);
						return;
					}
				}

				Optional<TreeInfo> interestingTree = Arrays.stream(trees).filter(tree -> tree.getTeam() != rc.getTeam()).findAny();
				if (interestingTree.isPresent()) {
					dir = rc.getLocation().directionTo(interestingTree.get().getLocation());
					if(tryMove(dir))
						return;
				}
			}
							
			Optional<TreeInfo> nearEnemyTree = Arrays.stream(trees).filter(tree -> tree.getTeam() == enemy).findFirst();
			if (nearEnemyTree.isPresent()) {
				if (rc.canChop(nearEnemyTree.get().ID)) {
					rc.chop(nearEnemyTree.get().ID);
					return;
				}
			}
			
			if(rc.getRobotCount()<600){
				for (TreeInfo treeInfo : trees) {
					if (rc.canChop(treeInfo.ID) && treeInfo.team != rc.getTeam()) {
						rc.chop(treeInfo.ID);
						return;
					}
				}

				Optional<TreeInfo> interestingTree = Arrays.stream(trees).filter(tree -> tree.getContainedRobot() != null)
						.findFirst();
				if (interestingTree.isPresent()) {
					dir = rc.getLocation().directionTo(interestingTree.get().getLocation());
					if(tryMove(dir))
						return;
				}

				Optional<RobotInfo> nearRobot = Arrays.stream(rc.senseNearbyRobots(-1, enemy))
						.filter(robot -> robot.getType() != RobotType.SOLDIER && robot.getType() != RobotType.TANK).findFirst();
				if (nearRobot.isPresent()) {
					MapLocation enemyLocation = nearRobot.get().getLocation();
					Direction toEnemy = rc.getLocation().directionTo(enemyLocation);
					if (tryMove(toEnemy))
						return;
				}		
			}
			else{
				MapLocation selectedPos = readReportedEnnemies();				
				if (tryMove( rc.getLocation().directionTo(selectedPos)))
					return;
			}
			
			if (rc.hasMoved())
				return;
			if ( rc.getLocation().distanceTo(enemyLocation) < Constants.LUMBERJACK_ATTACK_RADIUS) {
				moveRandomly();
			} else {
				dir = rc.getLocation().directionTo(enemyLocation);
				tryMove(dir);
			}
			moveRandomly();			
		}
		else{
			return;
		}
	}


}
