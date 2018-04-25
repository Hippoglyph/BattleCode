package violentfrenchies;

import java.util.ArrayList;
import java.util.Optional;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TreeInfo;

public class Scout extends Robot {

	Direction dir;
	Direction enemyDir;

	Scout(RobotController rc) throws GameActionException {
		super(rc);
		dir = rc.getLocation().directionTo(rc.getInitialArchonLocations(enemy)[0]);
		enemyDir = dir;
		currentState = readGlobalState();
	}

	Optional<TreeInfo> findBulletTree() {
		TreeInfo[] trees = rc.senseNearbyTrees(-1, Team.NEUTRAL);
		if (trees.length > 0) {
			for (TreeInfo tree : trees) {
				if (tree.containedBullets > 0) {
					if (rc.canMove(tree.getLocation())) {
						return Optional.of(tree);
					}
				}
			}
		}
		return Optional.empty();
	}

	boolean shakeTheTree(TreeInfo tree) {
		if (rc.canShake(tree.getLocation())) {
			try {
				rc.shake(tree.getLocation());
			} catch (GameActionException e) {
			}
			return true;
		}
		return false;
	}
	
	Optional<RobotInfo> nearGardenerWithoutTrees() {
		return filterByType(rc.senseNearbyRobots(-1, enemy), RobotType.GARDENER).stream().filter(gardener -> {
			MapLocation myLocation = rc.getLocation();
			MapLocation gardenerLocation = gardener.getLocation();
			Direction gardenerDir = myLocation.directionTo(gardenerLocation);
			TreeInfo[] trees = rc.senseNearbyTrees(myLocation.distanceTo(gardenerLocation), enemy);
			return treesInDir(trees, gardenerDir, 30).size() == 0;
		}).findFirst();
	}

	Optional<RobotInfo> nearArchonsWithoutTrees() {
		return filterByType(rc.senseNearbyRobots(-1, enemy), RobotType.ARCHON).stream().filter(archon -> {
			MapLocation myLocation = rc.getLocation();
			MapLocation archonLocation = archon.getLocation();
			Direction archonDir = myLocation.directionTo(archonLocation);
			TreeInfo[] trees = rc.senseNearbyTrees(myLocation.distanceTo(archonLocation), enemy);
			return treesInDir(trees, archonDir, 30).size() == 0;
		}).findFirst();
	}


	@Override
	void robotBehavior() {
		if (rc.hasMoved())
			return;
		Optional<RobotInfo> lumberjack = getNearestRobot(RobotType.LUMBERJACK, rc.getType().sensorRadius / 2f);
		if (lumberjack.isPresent()) {
			rc.setIndicatorDot(lumberjack.get().getLocation(), 0, 0, 255);
			dir = rc.getLocation().directionTo(lumberjack.get().getLocation()).opposite();
			dir = randomFreeDirection(dir, Constants.SCOUT_AVOID_LUMBERJACK_RANGE);
			try {
				rc.move(dir);
			} catch (GameActionException e) {
				e.printStackTrace();
			}
			return;
		}
		if (rc.hasMoved())
			return;

		try {
			Optional<TreeInfo> bulletTree = findBulletTree();
			if (bulletTree.isPresent()) {
				if (!shakeTheTree(bulletTree.get())) {
					// we need to get to this tree first
					MapLocation treeLoc = bulletTree.get().getLocation();
					if (rc.canMove(treeLoc) == true) {
						rc.move(treeLoc);
						return;
					}
				} else {
					rc.move(randFreeDirection());
					return;
				}
			}
		} catch (GameActionException e) {
			e.printStackTrace();
		}

		ArrayList<RobotInfo> gardeners = filterByType(rc.senseNearbyRobots(-1, enemy), RobotType.GARDENER);
		ArrayList<RobotInfo> archons = filterByType(rc.senseNearbyRobots(-1, enemy), RobotType.ARCHON);
		if (gardeners.size() > 0) {
			Optional<RobotInfo> gardener = nearGardenerWithoutTrees();
			if (gardener.isPresent()) {
				if (rc.getLocation().distanceTo(gardener.get().getLocation()) > rc.getType().sensorRadius / 4) {
					tryMove(rc.getLocation().directionTo(gardener.get().getLocation()));
				}
				if (rc.canFireSingleShot()) {
					try {
						rc.fireSingleShot(rc.getLocation().directionTo(gardener.get().getLocation()));
					} catch (GameActionException e) {
						e.printStackTrace();
					}
				}
				return;

			} else {
				tryMove(rc.getLocation().directionTo(gardeners.get(0).getLocation()));
				return;
			}
		}
		if (archons.size() > 0) {
			Optional<RobotInfo> archon = nearArchonsWithoutTrees();
			if (archon.isPresent()) {
				if (rc.getLocation().distanceTo(archon.get().getLocation()) > rc.getType().sensorRadius / 4) {
					tryMove(rc.getLocation().directionTo(archon.get().getLocation()));
				}
				if (rc.canFireSingleShot()) {
					try {
						rc.fireSingleShot(rc.getLocation().directionTo(archon.get().getLocation()));
					} catch (GameActionException e) {
						e.printStackTrace();
					}
				}
				return;

			} else {
				tryMove(rc.getLocation().directionTo(archons.get(0).getLocation()));
				return;
			}
		}
		if (!tryMove(dir, 20, 1))
			dir = randomFreeDirection(enemyDir, Constants.SCOUT_MOVEMENT_BLOCKED_DIR_RANGE);

	}
}
