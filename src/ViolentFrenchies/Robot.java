package violentfrenchies;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;

import battlecode.common.BodyInfo;
import battlecode.common.BulletInfo;
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

abstract public class Robot {
	RobotController rc;
	Team enemy;
	Random rand;
	private boolean alive;
	GlobalState currentState;
	
	Robot(RobotController rc) throws GameActionException {
		this.rc = rc;
		enemy = rc.getTeam().opponent();
		rand = new Random();
		newRobotBorn();
	}

	public GlobalState readGlobalState() {
		int channel = Constants.GLOBAL_MODE_CHANNEL;
		try {
			int current = rc.readBroadcast(channel);
			return GlobalState.values()[current];
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		return GlobalState.values()[0];
	}
	
	public enum GlobalState {
		BALANCED(0), MILITARY_RUSH(1), ECONOMY(2);
	    private final int value;
	    GlobalState(int value) {
	        this.value = value;
	    }
	    
	    public int getValue() {
	        return value;
	    }
	}
	
	/**
	 * MAIN BEHAVIOR
	 */
	void run() {
		while (true) {
			try {
				checkStatus();
				buyVictoryPoints();
				dodgeBullets();
				robotBehavior();
				Clock.yield();
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}
	}
	
	abstract void robotBehavior() throws GameActionException;

	private void checkStatus() throws GameActionException {
		if (!alive) {
			return;
		}

		if (rc.getHealth() < rc.getType().getStartingHealth() * Constants.MINIMUM_HEALTH_PERCENTAGE
				|| rc.getHealth() < Constants.MINIMUM_HEALTH) {
			updateRobotCount(-1);
			alive = false;
			reportDeath();
		}
	}
	
	void buyVictoryPoints() throws GameActionException {
		float bullets = rc.getTeamBullets();
		// donate all bullets if we can win immediately
		if (rc.getTeamBullets() / rc.getVictoryPointCost()	+ rc.getTeamVictoryPoints()	
			>= GameConstants.VICTORY_POINTS_TO_WIN) {
			rc.donate(bullets);
			return;
		}
		// don't donate before NUMBER_OF_TOUR_BEFORE_BUYING_VP
		if (rc.getRoundNum() < Constants.NUMBER_OF_TOUR_BEFORE_BUYING_VP){
			return;
		}	
		// donate only the surplus of bullet
		if (bullets > Constants.MAXIMUM_BULLETS_TO_SAVE) {
			float bulletsToDonate = bullets - Constants.MINIMUM_BULLETS_TO_SAVE;
			// round bullets to integers. Only integers donations are taken into account
			bulletsToDonate -= bulletsToDonate % rc.getVictoryPointCost();
			rc.donate(bulletsToDonate);
			return;
		}
	}

	void dodgeBullets() {
		BulletInfo[] bullets = rc.senseNearbyBullets();
		Arrays.stream(bullets).filter(x -> willCollideWithMe(x)).forEach(x -> tryMoveSideways(x));
	}
	
	boolean isEnemyNear() {
		RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
		for (RobotInfo robot : nearbyRobots) {
			if (robot.getTeam() == enemy)
				return true;
		}
		return false;
	}


	private void newRobotBorn() throws GameActionException {
		alive = true;
		updateRobotCount(1);
	}

	protected void reportDeath() {
		//combatLocations.reportLocation(rc.getLocation());
	}	

	private void updateRobotCount(int i) throws GameActionException {
		int channel = Constants.ROBOT_COUNTERS_BEGIN + rc.getType().ordinal();		
		int current = rc.readBroadcast(channel);
		rc.broadcast(channel, current + i);
	}

	int getRobotCount(RobotType type) throws GameActionException {
		int channel = Constants.ROBOT_COUNTERS_BEGIN + type.ordinal();
		int count = rc.readBroadcast(channel);
		return count;
	}

	int getRobotCount() throws GameActionException {
		return getRobotCount(rc.getType());
	}

	/** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** **/
	/** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** **/
	
	Direction randomDirection() {
		return new Direction((float) Math.random() * 2 * (float) Math.PI);
	}

	public Direction randFreeDirection() {
		Direction rndDir = null;
		for (int i = 0; i < Constants.GENERATING_DIR_MAX_TRIES_LIMIT; i++) {
			rndDir = randomDirection();
			if (rc.canMove(rndDir, rc.getType().strideRadius / 2))
				return rndDir;
		}
		return rndDir;
	}

	public Direction randomFreeDirection(Direction dir, float range) {
		for (int i = 0; i < Constants.GENERATING_DIR_MAX_TRIES_LIMIT; i++) {
			float angle = rand.nextFloat() * range - (range / 2);
			Direction rndDir = dir.rotateRightDegrees(angle);
			if (rc.canMove(rndDir))
				return rndDir;
		}
		return randFreeDirection();
	}

	boolean tryMove(Direction dir) {
		return tryMove(dir, 25, 4);
	}

	boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) {
		try {
			if (dir == null){
				return false;
			}
			if (rc.canMove(dir)) {
				rc.move(dir);
				return true;
			}
			
			int i = 1;

			while (i <= checksPerSide) {
				if (rc.canMove(dir.rotateLeftDegrees(degreeOffset * i))) {
					rc.move(dir.rotateLeftDegrees(degreeOffset * i));
					return true;
				}
				if (rc.canMove(dir.rotateRightDegrees(degreeOffset * i))) {
					rc.move(dir.rotateRightDegrees(degreeOffset * i));
					return true;
				}
				i++;
			}
			return false;
		} catch (GameActionException e) {
			return false;
		}
	}

	boolean willCollideWithMe(BulletInfo bullet) {
		MapLocation myLocation = rc.getLocation();

		Direction propagationDirection = bullet.dir;
		MapLocation bulletLocation = bullet.location;

		Direction directionToRobot = bulletLocation.directionTo(myLocation);
		float distToRobot = bulletLocation.distanceTo(myLocation);
		float theta = propagationDirection.radiansBetween(directionToRobot);

		if (Math.abs(theta) > Math.PI / 2) {
			return false;
		}
		
		float perpendicularDist = (float) Math.abs(distToRobot * Math.sin(theta));
		return (perpendicularDist <= rc.getType().bodyRadius);
	}

	boolean willIHitBody(Direction shootingDirection, BodyInfo body) {
		MapLocation myLocation = rc.getLocation();
		MapLocation robotLocation = body.getLocation();

		Direction directionToRobot = myLocation.directionTo(robotLocation);
		float theta = shootingDirection.radiansBetween(directionToRobot);
		if (Math.abs(theta) > Math.PI / 2) {
			return false;
		}
		float distToRobot = myLocation.distanceTo(robotLocation);
		float perpendicularDist = (float) Math.abs(distToRobot * Math.sin(theta));
		return perpendicularDist <= body.getRadius();
	}

	boolean willIHitRobot(Direction shootingDirection, RobotInfo robot) {
		return willIHitBody(shootingDirection, robot);
	}

	<T extends BodyInfo> boolean willIHitSomething(Direction shootingDirection, T[] bodies, float maxRadius) {
		for (BodyInfo friend : bodies) {
			if (friend.getLocation().distanceTo(rc.getLocation()) > maxRadius)
				return false;
			if (willIHitBody(shootingDirection, friend))
				return true;
		}
		return false;
	}

	BodyInfo nearestInDirection(Direction dir) {
		RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
		BodyInfo nearest = null;
		float distance = Float.POSITIVE_INFINITY;
		for (RobotInfo robot : nearbyRobots) {
			if (willIHitRobot(dir, robot)) {
				nearest = robot;
				distance = rc.getLocation().distanceTo(nearest.getLocation());
				break;
			}
		}
		TreeInfo[] nearbyTrees = rc.senseNearbyTrees();
		for (TreeInfo tree : nearbyTrees) {
			if (willIHitBody(dir, tree)) {
				if (rc.getLocation().distanceTo(tree.getLocation()) < distance) {
					return tree;
				}
			}
		}
		return nearest;
	}

	boolean isEnemy(BodyInfo body) {
		if (body == null)
			return false;
		return (body.isTree() ? ((TreeInfo) body).getTeam() : ((RobotInfo) body).getTeam()) == enemy;
	}

	boolean isFriend(BodyInfo body) {
		if (body == null)
			return false;
		return (body.isTree() ? ((TreeInfo) body).getTeam() : ((RobotInfo) body).getTeam()) == rc.getTeam();
	}

	boolean tryMoveSideways(BulletInfo bullet) {
		Direction towards = bullet.getDir();
		return (tryMove(towards.rotateRightDegrees(90)) || tryMove(towards.rotateLeftDegrees(90)));
	}

	ArrayList<RobotInfo> filterByType(RobotInfo[] robots, RobotType type) {
		ArrayList<RobotInfo> res = new ArrayList<>();
		for (RobotInfo robot : robots) {
			if (robot.getType() == type)
				res.add(robot);
		}
		return res;
	}

	ArrayList<TreeInfo> filterTreeBy(TreeInfo[] trees, Predicate<TreeInfo> cond) {
		ArrayList<TreeInfo> res = new ArrayList<>();
		for (TreeInfo tree : trees) {
			if (cond.test(tree)) {
				res.add(tree);
			}
		}
		return res;
	}

	Optional<RobotInfo> getNearestRobot(RobotType type, float maxRadius) {
		RobotInfo[] close_enemies = rc.senseNearbyRobots(maxRadius, enemy);
		ArrayList<RobotInfo> robots = filterByType(close_enemies, type);
		if (robots.size() > 0) {
			rc.setIndicatorDot(robots.get(0).getLocation(), 0, 250, 0);
			return Optional.of(robots.get(0));
		}
		return Optional.empty();
	}

	ArrayList<TreeInfo> treesInDir(TreeInfo[] trees, Direction dir, float delta) {
		return filterTreeBy(trees, tree -> {
			Direction dirToTree = rc.getLocation().directionTo(tree.getLocation());
			return (Math.abs(dirToTree.degreesBetween(dir)) < delta);
		});
	}
}
