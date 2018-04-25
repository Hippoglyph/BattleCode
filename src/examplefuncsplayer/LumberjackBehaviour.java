package examplefuncsplayer;
import java.util.Arrays;

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
import examplefuncsplayer.RobotBehaviour.AgentInfo;

public class LumberjackBehaviour extends RobotBehaviour {

	public LumberjackBehaviour(RobotController rc, RobotType ourType) throws GameActionException {
		super(rc, ourType);

		
		isLeftUnit = Math.random() > .5;
		needToSetDirection = true;
		allyTeam = rc.getTeam();
		enemyTeam = rc.getTeam().opponent();
		allyArchonLocations = rc.getInitialArchonLocations(allyTeam);
		enemyArchonLocations = rc.getInitialArchonLocations(enemyTeam);
		type = rc.getType();
		

		moveDir = randomDirection();
		// TODO: Replace with actual logic
		respondToBroadcast = (rc.getRobotCount() - 2 == allyArchonLocations.length) || Math.random() > .3;
	}
	
	@Override
	protected void tick() throws GameActionException {
		
	}

	
	private static int ARCHON_IGNORE_ROUND = 200;

	private static final float NO_INTERSECT = Float.NEGATIVE_INFINITY;

	private static boolean isLeftUnit;

	private static MapLocation destination;

	private static float distanceToDestination;

	protected static final float DISTANCE_TO_CLEAR_DESTINATION = 2;

	private static Direction lastDirection;

	private static boolean needToSetDirection;

	private Team allyTeam;
	private Team enemyTeam;

	private MapLocation[] allyArchonLocations;
	private MapLocation[] enemyArchonLocations;

	private RobotType type;



	private MapLocation getRandomEnemyInitialArchonLocation() {
		return enemyArchonLocations[(int) (enemyArchonLocations.length * (Math.random()))];
	}

	/*
	 * This method takes in a direction and attempts to move in this direction.
	 * If it cannot, then new directions are tried. By the end of this method,
	 * either the robot has moved in the returned angle, or no movable angle was
	 * found.
	 */
	private Direction moveWithRandomBounce(Direction move) throws GameActionException {
		if (smartCanMove(move)) {
			move(move);
		} else {
			for (int count = 0; count < 20 && !smartCanMove(move); count++) {
				move = randomDirection();
			}
			if (smartCanMove(move)) {
				move(move);
			}
		}
		return move;
	}
	
	private boolean smartCanMove(Direction toMove) throws GameActionException {
		//rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(toMove), 0, 0, 255);
		if (rc.getType() != RobotType.TANK) {
			return rc.canMove(toMove);
		} else if (rc.canMove(toMove)) {
			// System.out.println("We can move, but we are a tank.");
			if (rc.isCircleOccupiedExceptByThisRobot(rc.getLocation().add(toMove, rc.getType().strideRadius),
					rc.getType().bodyRadius)) {
				// System.out.println("The circle is occupied");
				TreeInfo[] possibleHitAllyTrees = rc
						.senseNearbyTrees(rc.getType().strideRadius + rc.getType().bodyRadius, rc.getTeam());
				for (TreeInfo t : possibleHitAllyTrees) {
					if (Math.abs(toMove.degreesBetween(rc.getLocation().directionTo(t.location))) < 90) {
						return false;
					}
				}
				return true;
			} else {
				return true;
			}
		}
		return false;
	}

	/*
	 * Sets the destination to the specified location. Used with
	 * tryToMoveToDestination for path finding.
	 * 
	 * Passing in null clears the destination.
	 */
	private void setDestination(MapLocation newDesination) {
		needToSetDirection = true;
		if (newDesination != null) {
			destination = newDesination;
			distanceToDestination = Float.MAX_VALUE;
			lastDirection = rc.getLocation().directionTo(destination);
		} else {
			destination = null;
			distanceToDestination = 0;
			lastDirection = null;
		}
	}

	private MapLocation getDestination() {
		return destination;
	}

	private boolean findBestDirection(MapLocation destination) throws GameActionException {
		Direction toMove = rc.getLocation().directionTo(destination);
		for (int count = 0; count < 180; count += 5) {
			if (smartCanMove(toMove.rotateLeftDegrees(count))) {
				return true;
			} else if (smartCanMove(toMove.rotateRightDegrees(count))) {
				return false;
			}
		}
		// System.out.println("Reached. This should never happen if a unit is
		// not trapped.");
		return false;
	}

	private boolean move(Direction direction) throws GameActionException {
		if (direction == null) {
			return false;
		}
		if (!rc.hasMoved() && smartCanMove(direction)) {
			rc.move(direction);
			return true;
		}
		return false;
	}


	private void endTurn() throws GameActionException {
		tryAndShakeATree();
		econWinIfPossible();

		// Dump all bullets if game about to end to get tiebreaker
		if (rc.getRoundLimit() - rc.getRoundNum() < 2) {
			float bulletCount = rc.getTeamBullets();
			bulletCount /= rc.getVictoryPointCost();
			int donateCount = (int) bulletCount;
			donateCount *= rc.getVictoryPointCost();
			rc.donate(donateCount);
		} /*
			 * else { float bullets = rc.getTeamBullets(); if (bullets > 250) {
			 * int bulletCount = (int) ((bullets - 250) /
			 * rc.getVictoryPointCost()); bulletCount *=
			 * rc.getVictoryPointCost(); rc.donate(bulletCount); } }
			 */
		// drawDots();
		TreeInfo[] nearbyTrees = rc.senseNearbyTrees(5);

		if (nearbyTrees.length == 0 && rc.getType() != RobotType.GARDENER) {
			//BroadcastManager.saveLocation(rc, rc.getLocation(), LocationInfoType.GOOD_SPOT);
		}
		if (Clock.getBytecodesLeft() > 120) {
			//BroadcastManager.broadcastSpam(rc);
		}
//		try {
//			if (this instanceof CombatUnitLogic) {
//				if (!rc.hasAttacked()) {
//					MapLocation toAttack = BroadcastManager.getHashedLocationToFire(rc);
//					if (toAttack != null) {
//						System.out.println("Target at: " + toAttack);
//						rc.setIndicatorDot(toAttack, 255, 0, 0);
//						rc.fireSingleShot(rc.getLocation().directionTo(toAttack));
//						BroadcastManager.clearHashedLocation(rc);
//					}
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

		Clock.yield();
	}


	/*
	 * If it is possible to move towards the target, then this method returns
	 * the best angle to do so with. Otherwise, null is returned. The method
	 * will also disallow angles that will result in the robot getting hit by a
	 * bullet.
	 */
	private Direction getDirectionTowards(MapLocation destination) throws GameActionException {
		if (!rc.getLocation().equals(destination)) {
			Direction toMove = rc.getLocation().directionTo(destination);
			return getDirectionTowards(toMove);
		}
		return null;
	}

	/*
	 * If it is possible to move towards the specified direction, then this
	 * method returns the best angle to do so with. Otherwise, null is returned.
	 * The method will also disallow angles that will result in the robot
	 * getting hit by a bullet.
	 */
	private Direction getDirectionTowards(Direction toMove) throws GameActionException {
		if (smartCanMove(toMove)) {
			return toMove;
		} else {
			BulletInfo[] bullets = rc.senseNearbyBullets();
			for (int deltaAngle = 0; deltaAngle < 360; deltaAngle += 5) {
				if (isLeftUnit) {
					Direction leftDir = toMove.rotateLeftDegrees(deltaAngle);
					if (smartCanMove(leftDir)
							&& !willGetHitByABullet(rc.getLocation().add(leftDir, type.strideRadius), bullets)) {
						return leftDir;
					}
				} else {
					Direction rightDir = toMove.rotateRightDegrees(deltaAngle);
					if (smartCanMove(rightDir)
							&& !willGetHitByABullet(rc.getLocation().add(rightDir, type.strideRadius), bullets)) {
						return rightDir;
					}
				}
			}
		}
		return null;
	}

	/*
	 * This method should be called at the end of each run statement, if
	 * bytecode can be afforded. It will find a tree that can be shaked and
	 * shake it.
	 */
	protected void tryAndShakeATree() throws GameActionException {
		if (rc.canShake()) {
			TreeInfo[] trees = rc.senseNearbyTrees();
			for (TreeInfo t : trees) {
				if (t.containedBullets > 0 && rc.canShake(t.ID)) {
					rc.shake(t.ID);
					//System.out.println("We shook a tree");
					return;
				}
			}
		}
		//System.out.println("We did not shake a tree");
	}

	private BodyInfo getFirstHitTargetAprox(MapLocation location, Direction direction) throws GameActionException {
		MapLocation testLocation = location.add(direction, type.bodyRadius + GameConstants.BULLET_SPAWN_OFFSET);
		while (rc.canSenseLocation(testLocation)) {
			if (rc.isLocationOccupied(testLocation)) {
				TreeInfo targetTree = rc.senseTreeAtLocation(testLocation);
				if (targetTree != null) {
					return targetTree;
				}
				RobotInfo targetRobot = rc.senseRobotAtLocation(testLocation);
				if (targetRobot != null) {
					return targetRobot;
				} else {
					return null;
				}
			} else {
				float DELTA_BULLET_DISTANCE = .49f;
				testLocation = testLocation.add(direction, DELTA_BULLET_DISTANCE);
			}
		}
		return null;
	}

	/*
	 * This method returns a bullet that will hit the player in its current
	 * position. If multiple bullets will hit the target, only the closest
	 * bullet is returned. Returns null if no bullet will hit the target.
	 */
	protected BulletInfo getTargetingBullet(BulletInfo[] bullets) {
		RobotInfo player = new RobotInfo(-1, null, type, rc.getLocation(), 1, 1, 1);

		float minDistance = Float.MAX_VALUE;
		BulletInfo closestBullet = null;

		for (BulletInfo bullet : bullets) {
			float distance = rc.getLocation().distanceTo(bullet.getLocation());
			if (distance < minDistance
					&& getIntersectionDistance(bullet.location, bullet.dir, player) != NO_INTERSECT) {
				closestBullet = bullet;
				minDistance = distance;
			}
		}
		return closestBullet;
	}

	/**
	 * Returns whether a bullet will hit a player at a specific location.
	 */
	protected boolean willHit(BulletInfo bullet, RobotInfo player) {
		return getIntersectionDistance(bullet.location, bullet.dir, player) != NO_INTERSECT;
	}

	/*
	 * This method determines if a character will be hit by any bullet if it
	 * moves to a particular location. The method takes in the location that the
	 * player wants to move to and an array representing all sensed bullets.
	 *
	 * The method returns true iff a bullet will hit the player in the next
	 * round, given that the player moves to the specified location
	 */
	protected boolean willGetHitByABullet(MapLocation playerLocation, BulletInfo[] bullets) {
		RobotInfo player = new RobotInfo(-1, null, type, playerLocation, 1, 1, 1);
		for (BulletInfo bullet : bullets) {
			if (getIntersectionDistance(bullet.location, bullet.dir, player) != NO_INTERSECT) {
				return true;
			}
		}
		return false;
	}

	/*
	 * This method takes the location and direction of a bullet, as well as the
	 * BodyInfo of the target to be intersected.
	 *
	 * The method returns the distance at which the target will be intersected.
	 * If the target is never intersected, this method returns
	 * Float.NEGATIVE_INFINITY.
	 */
	private float getIntersectionDistance(MapLocation location, Direction direction, BodyInfo target) {

		float targetRadius = target.getRadius();

		// The x and y coordinates of the center of the target.
		float xTarget = target.getLocation().x;
		float yTarget = target.getLocation().y;

		// The x and y coordinates of the bullet's starting point.
		float xStart = location.x;
		float yStart = location.y;

		float dx = xTarget - xStart;
		float dy = yTarget - yStart;
		// Compute the shortest distance between the bullet and the center of
		// the target
		float angle = direction.radians;
		float dist = (float) Math.abs(Math.sin(angle) * (dx) - Math.cos(angle) * dy);

		// If the shortest distance is too large, the bullet won't ever
		// intersect the target
		if (dist > targetRadius) {
			return NO_INTERSECT;
		}

		if (Math.abs(location.directionTo(rc.getLocation()).degreesBetween(direction)) > 90) {
			return NO_INTERSECT;
		}

		// Compute the distance the bullet travels to get to the point of
		// closest approach
		float distSquared = dist * dist;
		float lengthToClosestApproach = (float) Math.sqrt((dx) * (dx) + (dy) * (dy) - distSquared);

		// Compute the distance the bullet travels from the intersection point
		// to the closest approach
		float excessDistance = (float) Math.sqrt(targetRadius * targetRadius - distSquared);

		return lengthToClosestApproach - excessDistance;
	}


	/*
	 * Code used to find the highest priority target. If no sutable targets are
	 * found, null is returned. This method only returns a target if it can be
	 * fired at from the robot's current position.
	 */
	RobotInfo getHighestPriorityTarget(RobotInfo[] enemies, boolean hitTrees) throws GameActionException {
		if (enemies.length == 0) {
			return null;
		}

		int maxIndex = -1;
		double maxPriority = -1;

		for (int index = 0; index < enemies.length; index++) {

			double priority = 0;

			if (enemies[index].getType().canAttack()) {
				priority = enemies[index].getType().attackPower / (Math.max(enemies[index].health, 1)
						* rc.getLocation().distanceTo(enemies[index].getLocation()));
			}

			// System.out.println("Priority is: " + priority);

			// TODO: Refactor
			if ((priority > maxPriority || (maxPriority == 0 && enemies[index].health < enemies[maxIndex].health))) {

				// Don't attack archons at the start of the game.
				if (enemies[index].type == RobotType.ARCHON && rc.getRoundNum() < ARCHON_IGNORE_ROUND) {
					continue;
				}

				Direction toEnemy = rc.getLocation().directionTo(enemies[index].location);
				float spawnOffset = type.bodyRadius + GameConstants.BULLET_SPAWN_OFFSET;

				MapLocation bulletSpawnPoint = rc.getLocation().add(toEnemy, spawnOffset);

				// Only attack if we will hit an enemy.
				BodyInfo firstHitObject = getFirstHitTargetAprox(bulletSpawnPoint, toEnemy);
				if ((firstHitObject instanceof RobotInfo && ((RobotInfo) firstHitObject).getTeam().equals(enemyTeam))
						|| (firstHitObject instanceof TreeInfo
								&& ((TreeInfo) firstHitObject).getTeam().equals(enemyTeam) && hitTrees)) {
					if (firstHitObject instanceof TreeInfo) {
						maxIndex = index;
						maxPriority = priority;
					} else if (firstHitObject instanceof RobotInfo
							&& firstHitObject.getID() == enemies[index].getID()) {
						maxIndex = index;
						maxPriority = priority;
					}
					// if (getFirstHitTeamAprox(bulletSpawnPoint, toEnemy,
					// hitTrees) == enemyTeam) {
					// System.out.println("We have found a new target");
				}
			}
		}

		if (maxIndex >= 0) {
			return enemies[maxIndex];
		}
		// TODO: actually handle
		return null;
	}

	/*
	 * Returns the body that is closest to the robot tha calls the method. The
	 * passed in array should contain all bodies for analysis.
	 */
	protected BodyInfo getClosestBody(BodyInfo[] foes) {
		if (foes.length == 0) {
			return null;
		}

		BodyInfo closestEnemy = null;
		float closestDistance = Float.MAX_VALUE;

		for (BodyInfo enemy : foes) {
			// Ignore enemy archons at the start of the game.
			if (enemy instanceof RobotInfo) {

				RobotInfo robot = (RobotInfo) enemy;
				if (robot.type == RobotType.ARCHON && rc.getRoundNum() < ARCHON_IGNORE_ROUND) {
					continue;
				}
			}

			float dist = rc.getLocation().distanceTo(enemy.getLocation());
			if (dist < closestDistance) {
				closestEnemy = enemy;
				closestDistance = dist;
			}
		}

		return closestEnemy;
	}

	protected void econWinIfPossible() throws GameActionException {
		if (rc.getTeamBullets() >= (GameConstants.VICTORY_POINTS_TO_WIN - rc.getTeamVictoryPoints())
				* rc.getVictoryPointCost()) {
			rc.donate(rc.getTeamBullets());
		}
	}

	private Direction findDensestDirection(BulletInfo[] bullets) {
		float avgX = 0, avgY = 0;
		MapLocation currLocation = rc.getLocation();

		for (BulletInfo bullet : bullets) {
			Direction d = currLocation.directionTo(bullet.location);
			avgX += d.getDeltaX(1);
			avgY += d.getDeltaY(1);
		}

		avgX /= bullets.length;
		avgY /= bullets.length;

		return new Direction(avgX, avgY);
	}

	private final float ANGLE_EPSILON = 0.01f;

	protected boolean incomingBullet(BulletInfo[] bullets) {
		for (BulletInfo bullet : bullets) {
			float angleTolerance = (float) (Math.abs(
					Math.asin(type.bodyRadius / bullet.getLocation().distanceTo(rc.getLocation()))) + ANGLE_EPSILON);
			// System.out.println(angleTolerance);
			if (Math.abs(bullet.location.directionTo(rc.getLocation()).radiansBetween(bullet.dir)) < angleTolerance) {
				/*
				 * System.out.println("incoming bullet " +
				 * Math.abs(bullet.location.directionTo(rc.getLocation()).
				 * radiansBetween(bullet.dir)));
				 */
				//rc.setIndicatorDot(bullet.location, 0, 255, 0);
				return true;
			}
		}

		return false;
	}

	protected void moveAndDodge(MapLocation enemy, BulletInfo[] bullets) throws GameActionException {
		MapLocation currLocation = rc.getLocation();
		Direction toEnemy = currLocation.directionTo(enemy);

		float minDamage = rc.getHealth();
		int bestAngle = -40;
		for (int angle = -40; angle < 40; angle += 10) {
			MapLocation expectedLocation = currLocation.add(toEnemy.rotateLeftDegrees(angle), type.strideRadius);
			float damage = expectedDamage(bullets, expectedLocation);

			if (damage < minDamage) {
				bestAngle = angle;
				minDamage = damage;
			}
		}

		move(toEnemy.rotateLeftDegrees(bestAngle));
	}

	protected void dodge(BulletInfo[] bullets) throws GameActionException {
		// bullets = getAllIncomingBullets(bullets, rc.getLocation(), 20);
		BulletInfo[] predictNext = Arrays.stream(bullets).map(b -> new BulletInfo(b.getID(),
				b.location.add(b.getDir(), b.speed), b.getDir(), b.getSpeed(), b.getDamage()))
				.toArray(BulletInfo[]::new);
		Direction densestDirection = findDensestDirection(bullets);
		MapLocation currentLocation = rc.getLocation();

		float stationaryImminentDanger = getImminentDanger(bullets, currentLocation);

		int safestAngle = 0;
		float leastDanger = stationaryImminentDanger;
		for (int angle = 90; angle < 270; angle += 10) {

			float expectedDanger = getImminentDanger(predictNext,
					currentLocation.add(densestDirection.rotateLeftDegrees(angle)));

			if (expectedDanger < leastDanger && smartCanMove(densestDirection.rotateLeftDegrees(angle))) {
				leastDanger = expectedDanger;
				safestAngle = angle;
			}
		}

		Direction toMove = densestDirection.rotateLeftDegrees(safestAngle);

		if (smartCanMove(toMove)) {
			move(toMove);
		}
	}

	private float getImminentDanger(BulletInfo[] bullets, MapLocation loc) {
		float danger = 0;
		RobotInfo player = new RobotInfo(-1, null, type, loc, 1, 1, 1);
		float totalDamage = 0;
		for (BulletInfo bullet : bullets) {
			danger += bullet.getDamage() / loc.distanceTo(bullet.getLocation());
			if (willHit(bullet, player)) {
				totalDamage += bullet.damage;
			}
		}

		return danger * totalDamage;
	}

	protected boolean inDanger() {
		RobotInfo[] foes = rc.senseNearbyRobots(-1, this.enemyTeam);
		for (RobotInfo enemy : foes) {
			if (enemy.getType() != RobotType.ARCHON && enemy.getType() != RobotType.GARDENER) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This method returns the expected damage which a specific location will
	 * get in the next round, based on the current bullet information
	 *
	 * @param bullets
	 * @return
	 */
	protected float expectedDamage(BulletInfo[] bullets, MapLocation loc) {
		RobotInfo player = new RobotInfo(-1, null, type, loc, 1, 1, 1);
		float totalDamage = 0;
		for (BulletInfo bullet : bullets) {
			if (willHit(bullet, player)) {
				totalDamage += bullet.damage;
			}
		}

		return totalDamage;
	}

	private boolean tryToMoveToDestinationTwo() throws GameActionException {
		if (destination == null) {
			return false;
		}
		//rc.setIndicatorLine(rc.getLocation(), destination, 0, 0, 40);
		MapLocation currentLocation = rc.getLocation();
		/*Direction toMove = new Direction(
				(float) (((int) (rc.getLocation().directionTo(destination).getAngleDegrees() / 5) * 5) * Math.PI
						/ 180));*/
		Direction toMove = rc.getLocation().directionTo(destination);
		float currentDistance = currentLocation.distanceTo(destination);
		/*
		 * System.out.println("Current distance is: " + currentDistance +
		 * " closest distance is : " + distanceToDestination +
		 * " canMove returns " + rc.canMove(toMove) +
		 * "If the next statement is true, then this unit leans left " +
		 * isLeftUnit);
		 */
		if (currentDistance <= distanceToDestination && smartCanMove(toMove)) {
			move(toMove);
			distanceToDestination = currentDistance;
			needToSetDirection = true;
			lastDirection = toMove;
			return true;
		} else if (smartCanMove(lastDirection)) {
			toMove = findAngleThatBringsYouClosestToAnObstruction(lastDirection);
			if (smartCanMove(toMove)) {
				lastDirection = toMove;
				move(toMove);
				distanceToDestination = Math.min(distanceToDestination, currentDistance);
				return true;
			} else {
				toMove = getDirectionTowards(toMove);
				if(toMove != null) {
					move(toMove);
					distanceToDestination = Math.min(distanceToDestination, currentDistance);
				}
				return false;
			}
		} else {
			if (needToSetDirection) {
				isLeftUnit = findBestDirection(destination);
				needToSetDirection = false;
			}
			toMove = getDirectionTowards(lastDirection);
			if (toMove != null) {
				lastDirection = toMove;
				move(toMove);
				return true;
			} else {
				return false;
			}
		}
	}

	private Direction findAngleThatBringsYouClosestToAnObstruction(Direction lastDirection2)
			throws GameActionException {
		Direction testAngle = lastDirection2;
		int directionMultiplyer;
		if (isLeftUnit) {
			directionMultiplyer = 1;
		} else {
			directionMultiplyer = -1;
		}
		for (int deltaAngle = 0; deltaAngle < 360 && smartCanMove(testAngle); deltaAngle += 5) {
			testAngle = lastDirection.rotateRightDegrees(deltaAngle * directionMultiplyer);
		}
		while (!smartCanMove(testAngle)) {
			testAngle = testAngle.rotateLeftDegrees(5 * directionMultiplyer);
		}
		return testAngle;
	}

	

	private Direction moveDir;
	private boolean respondToBroadcast;
	private final int TREE_TOO_FAR_AWAY_DISTANCE = 15;
	private final int ROUNDS_TO_TRY_TO_HELP = 200;


	public void run() throws GameActionException {
		int birthRound = rc.getRoundNum();
		while (true) {
			
			// TODO: Sensing the bullets, dodging
			prevRound = currRound;
			currRound = rc.getRoundNum();
			if (currRound != prevRound) {
				
				ourInfo = AgentInfo.createSimple(myPointer, rc);
				ourInfo.location = rc.getLocation();
				ourHealth = rc.getHealth();
				ourInfo.healthLevel = (int) (AgentInfo.MAX_HEALTH_LEVEL * ourHealth / ourType.maxHealth);
				ourInfo.statusCode = AgentInfo.ALIVE;
				
				nearbyRobots = rc.senseNearbyRobots();
				nearbyTrees = rc.senseNearbyTrees();
				
				ourLocation = ourInfo.location;
				
				ourInfo.opponentsNum = -1;
				
				turn();
				
				if (ourInfo.opponentsNum == -1) {
					ourInfo.opponentsNum = 0;
					for (RobotInfo robot : nearbyRobots) {
						if (robot.team == opponentTeam) {
							++ourInfo.opponentsNum;
						}
					}
				}
				
				
				// Update our state
				AgentInfo.write(myPointer, rc, ourInfo, ourInfo.location);
			}
			
			try {

				// First priority: attack enemy
				boolean foundEnemyToTarget = false;
				RobotInfo[] enemyRobots = rc.senseNearbyRobots(3.5f, enemyTeam);
				for (RobotInfo enemy : enemyRobots) {
					if (/* enemy.type != RobotType.SOLDIER && */enemy.type != RobotType.TANK) {
						foundEnemyToTarget = true;
						break;
					}
				}
				if (foundEnemyToTarget) {
					attackEnemy(enemyRobots);
					endTurn();
					continue;
				}

				// Tree cutting mode
				TreeInfo[] enemyTrees = rc.senseNearbyTrees(-1, enemyTeam);
				if (enemyTrees.length > 0) {
					moveTowardsAndChop(enemyTrees);
					endTurn();
					continue;
				}

				TreeInfo[] neutralTrees = rc.senseNearbyTrees(-1, Team.NEUTRAL);
				TreeInfo toChop = getTargetTree(neutralTrees);

				if(rc.getRoundNum() - birthRound < ROUNDS_TO_TRY_TO_HELP) {
					if (respondToBroadcast) {
						if (toChop != null && toChop.containedRobot != null
								&& (this.getDestination() == null || !getDestination().equals(toChop.getLocation()))) {
							this.setDestination(toChop.location);
						}

						if (getDestination() != null
								&& rc.getLocation().distanceTo(getDestination()) < TREE_TOO_FAR_AWAY_DISTANCE) {
							moveTowardsAndChop(getDestination());
							if (rc.getLocation().distanceTo(getDestination()) < DISTANCE_TO_CLEAR_DESTINATION) {
								setDestination(null);
							}
							endTurn();
							continue;
						} else {
							//MapLocation destination = BroadcastManager.getRecentLocation(rc,
							//		LocationInfoType.LUMBERJACK_GET_HELP);
							//setDestination(destination);
							//BroadcastManager.invalidateLocation(rc, LocationInfoType.LUMBERJACK_GET_HELP);
						}
					}
				}

				// Neutral tree cutting mode
				if (neutralTrees.length > 0) {
					moveTowardsAndChop(neutralTrees);
					endTurn();
					continue;
				}

				// Move randomly
				MapLocation archonLocation = getRandomEnemyInitialArchonLocation();
				Direction towardsEnemy = rc.getLocation().directionTo(archonLocation);
				towardsEnemy = getDirectionTowards(towardsEnemy);
				if (towardsEnemy != null) {
					move(towardsEnemy);
					if (rc.canSenseLocation(archonLocation)) {
						respondToBroadcast = true;
					}
				} else {
					this.moveDir = this.moveWithRandomBounce(moveDir);
				}
				endTurn();

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void moveTowardsAndChop(TreeInfo[] trees) throws GameActionException {
		TreeInfo toChop = getTargetTree(trees);
		if (toChop != null) {
			//rc.setIndicatorLine(rc.getLocation(), toChop.location, 80, 80, 0);
			TreeInfo treeInFront = getClosestTreeThatCanBeChopped(trees, null);
			if (rc.canChop(toChop.getID())) {
				System.out.println("sandwich");
				rc.chop(toChop.getID());
			} else if (treeInFront != null && rc.canChop(treeInFront.ID)) {
				System.out.println("read");
				rc.chop(treeInFront.ID);
			} else {
				Direction toMove = getDirectionTowards(toChop.getLocation());
				if (toMove != null) {
					move(toMove);
				}
			}
		} 
	}

	private void moveTowardsAndChop(MapLocation destination) throws GameActionException {
		//rc.setIndicatorLine(rc.getLocation(), destination, 80, 80, 0);
		TreeInfo[] trees = rc.senseNearbyTrees(-1, Team.NEUTRAL);
		TreeInfo treeInFront = getClosestTreeThatCanBeChopped(trees, rc.getLocation().directionTo(destination));
		if (treeInFront != null && rc.canChop(treeInFront.ID)) {
			System.out.println("Trying to chop");
			rc.chop(treeInFront.ID);
		} else {
			this.tryToMoveToDestinationTwo();
		}
	}

	/*
	 * Passed direction is the direction that you want to move.
	 */
	private TreeInfo getClosestTreeThatCanBeChopped(TreeInfo[] trees, Direction directionTo) {
		TreeInfo toChop = null;
		float closestDistance = Float.MAX_VALUE;
		for (TreeInfo tree : trees) {
			float distance = rc.getLocation().distanceTo(tree.location);
			if (directionTo != null) {
				if (Math.abs(rc.getLocation().directionTo(tree.location).degreesBetween(directionTo)) < 90
						&& tree.team != allyTeam && distance < closestDistance) {
					closestDistance = distance;
					toChop = tree;
				}
			} else if (tree.team != allyTeam && distance < closestDistance) {
				closestDistance = distance;
				toChop = tree;
			}
		}
		return toChop;
	}

	private TreeInfo getTargetTree(TreeInfo[] trees) {
		boolean foundTreeWithGoodies = false;
		float distanceToClosestTree = Float.MAX_VALUE;

		TreeInfo target = null;
		for (TreeInfo tree : trees) {
			float distance = rc.getLocation().distanceTo(tree.location);
			if (foundTreeWithGoodies && distance < distanceToClosestTree) {
				target = tree;
				distanceToClosestTree = distance;
			} else if (!foundTreeWithGoodies && tree.containedRobot != null) {
				target = tree;
				distanceToClosestTree = distance;
				foundTreeWithGoodies = true;
			} else if (!foundTreeWithGoodies && distance < distanceToClosestTree) {
				target = tree;
				distanceToClosestTree = distance;
			}
		}
		return target;
	}

	private void attackEnemy(RobotInfo[] enemyRobots) throws GameActionException {
		RobotInfo target = getTarget(enemyRobots);
		Direction toMove = getDirectionTowards(target.location);
		if (toMove != null) {
			move(toMove);
		}
		if ((rc.getLocation().distanceTo(target.location) < GameConstants.LUMBERJACK_STRIKE_RADIUS
				+ target.getType().bodyRadius) && rc.canStrike()) {
			rc.strike();
		} else {
			TreeInfo[] nearbyTrees = rc.senseNearbyTrees();
			TreeInfo toChop = (TreeInfo) getClosestBody(nearbyTrees);
			if (rc.canChop(toChop.ID)) {
				rc.chop(toChop.ID);
			}
		}
	}

	private RobotInfo getTarget(RobotInfo[] enemyRobots) {
		RobotInfo closestEnemy = null;
		float closestDistance = Float.MAX_VALUE;
		for (RobotInfo enemy : enemyRobots) {
			float distance = rc.getLocation().distanceTo(enemy.location);
			if (distance < closestDistance && type != RobotType.SOLDIER && type != RobotType.TANK) {
				closestDistance = distance;
				closestEnemy = enemy;
			}
		}
		return closestEnemy;
	}
	
    public static MapLocation getAvgArchonLocations(RobotController rc, Team team) {
        float avgX = 0;
        float avgY = 0;
        MapLocation[] initialLocs = rc.getInitialArchonLocations(team);
        for(MapLocation loc : initialLocs) {
            avgX += loc.x;
            avgY += loc.y;
        }
        return new MapLocation(avgX / initialLocs.length, avgY / initialLocs.length);
    }

    public static Direction randomDirection() {
        return new Direction((float) Math.random() * 2 * (float) Math.PI);
    }

    public static Direction diagonalDirection() {
        int[] diagonals = new int[] {0, 45, 90, 135, 180, 225, 270, 315, 360};
        return new Direction(diagonals[(int) (Math.random() * diagonals.length)]);
    }

}
