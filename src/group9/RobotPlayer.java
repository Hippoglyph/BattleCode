package group9;
import battlecode.common.*;
import java.util.ArrayList;
import java.util.Random;
import java.lang.Math;
import java.lang.Math;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.StreamCorruptedException;
import java.io.OptionalDataException;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;

public strictfp class RobotPlayer {
    static RobotController rc;
	static boolean hasWritten = false;
	static volatile boolean startedInit = false;
	static volatile boolean completedInit = false;
	private static int numRobots[] = new int[6]; 
	private static int lastRound = 0;
	static private int roundLimit;
	static int whichTree;
	private static ArrayList<BehaviorTree> genomes = null;
	static Random random = new Random();

	public static byte[] hexStringtoByteArray(String s) {
		byte[] data = new byte[s.length() / 2];
		for (int i = 0; i < s.length(); i+= 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
							+ Character.digit(s.charAt(i+1), 16));
		}
		return data;
	}
	
	public static void initializeMemory(RobotController rc, ArrayList<Float> memory, ArrayList<MapLocation>mapMemory) {

		memory.clear();
		mapMemory.clear();
		MapLocation loc = rc.getLocation();
		mapMemory.add(loc);
		mapMemory.add(new MapLocation(loc.x + 5, loc.y    )); //0
		mapMemory.add(new MapLocation(loc.x + 3, loc.y + 3)); //1
		mapMemory.add(new MapLocation(loc.x    , loc.y + 5)); //2
		mapMemory.add(new MapLocation(loc.x - 3, loc.y + 3)); //3
		mapMemory.add(new MapLocation(loc.x - 5, loc.y    )); //4
		mapMemory.add(new MapLocation(loc.x - 3, loc.y - 3)); //5
		mapMemory.add(new MapLocation(loc.x    , loc.y - 5)); //6
		mapMemory.add(new MapLocation(loc.x + 3, loc.y - 3)); //7
		int randIndex = random.nextInt(7) + 1;
		MapLocation chosen = mapMemory.get(randIndex);
		BulletInfo[] info = rc.senseNearbyBullets();
		RobotInfo[] rinfo = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
		memory.add((float)(random.nextFloat() * 2 * Math.PI));
		for (int i = -4; i < 4; i++) {
			//TODO: split direction memory
			memory.add((float)(i * Math.PI / 4));
		}
		memory.add(0f);
		memory.add(1f);
		System.out.println("Boolean one is at index " + memory.size());
		memory.add((float)boolToInt(rc.canChop(loc)));
		memory.add((float)boolToInt(rc.canFireSingleShot()));
		memory.add((float)boolToInt(rc.canShake(loc)));
		memory.add((float)boolToInt(rc.canWater()));
		memory.add((float)boolToInt(rc.getHealth() < 400 * 0.1f));
		//memory.add(rc.getTeamBullets());
		boolean[] isInSector = new boolean[9];
		boolean[] isInSector2 = new boolean[9];
		for (int i = 0; i < info.length; i++) {
			if (squaredist(loc, info[i].location) < 16) {
				isInSector[0] = true;
			}
			else {
				isInSector[binAngle(loc, info[i].location) + 1] = true;
			}
		}
		for (int i = 0; i < 9; i++) {
			memory.add((float)boolToInt(isInSector[i]));
			isInSector[i] = false;
		}
		for (int i = 0; i < rinfo.length; i++) {
			if (squaredist(loc, rinfo[i].location) < 16) {
				isInSector[0] = true;
			}
			else {
				isInSector[binAngle(loc, rinfo[i].location) + 1] = true;
			}
		}
		for (int i = 0; i < 9; i++) {
			memory.add((float)boolToInt(isInSector[i]));
			isInSector[i] = false;
		}

		switch (rc.getType()) {

			case ARCHON:
			case SOLDIER:
			case TANK:
			case SCOUT:
				break;
			case GARDENER:
			case LUMBERJACK:
				Team ourTeam = rc.getTeam();
				TreeInfo[] tinfo = rc.senseNearbyTrees();
				for (int i = 0; i < tinfo.length; i++) {
					if (squaredist(loc, tinfo[i].location) <= 4) {
						isInSector[0] = true;
						if (tinfo[i].team != ourTeam) {
							isInSector2[0] = true;
						}
					}
					else {
						int binnedAngle = binAngle(loc, tinfo[i].location) + 1;
						isInSector[binnedAngle] = true;
						if (tinfo[i].team != ourTeam) {
							isInSector2[binnedAngle] = true;
						}
					}
				}
				for (int i = 0; i < 9; i++) {
					memory.add((float)boolToInt(isInSector[i]));
					System.out.println("Tree memory is at " + (memory.size() - 1) + ", is " + isInSector[i]);
				}
				for (int i = 0; i < 9; i++) {
					memory.add((float)boolToInt(isInSector2[i]));
				}
				break;
		}
	}

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;
		String hackData = HackFile.read();
		if (!startedInit) {
			startedInit = true;
			roundLimit = rc.getRoundLimit();
			try {
				genomes = new ArrayList<BehaviorTree>();
				int index = 0;
				float score;
				int size = Integer.parseInt(hackData.substring(index, index + 4), 16);
				index += 4;
				int length;
				for (int i = 0; i < size; i++) {
					score = Float.intBitsToFloat(Integer.parseInt(hackData.substring(index, index + 8), 16));
					index += 8;
					length = Integer.parseInt(hackData.substring(index, index + 4), 16);
					index += 4;
					String genome = "";
					for (int j = 0; j < length ;j++) {
						genome += (char)(Integer.parseInt(hackData.substring(index, index + 4), 16));
						index += 4;
					}
					genomes.add(new BehaviorTree(genome));
					genomes.get(genomes.size() - 1).score = score;
				}				
				
				int bestIndex = 0;
				int worstIndex = 0;
				float bestScore = 0;
				float worstScore = 1e30f;
				for (int i = 0; i < genomes.size(); i++) {
					score = genomes.get(i).score;
					if (score > bestScore) {
						bestIndex = i;
						bestScore = score;
					}
					if (score < worstScore) {
						worstIndex = i;
						worstScore = score;
					}
				}
				if (genomes.size() > 10) {
					genomes.remove(worstIndex);
					if (bestIndex > worstIndex) {
						bestIndex--;
					}
				}
				genomes.add(BehaviorTree.mutate(genomes.get(bestIndex)));
			}
			catch (IndexOutOfBoundsException e) {
				System.out.println("Genome list data terminated unexpectedly");
			}
			if (genomes.size() == 0) {
				genomes = new ArrayList<BehaviorTree>();
				System.out.println("Default tree genome is \"" + BehaviorTree.defaultTree + "\"");
				genomes.add(new BehaviorTree(BehaviorTree.defaultTree));
				genomes.get(0).score = 0;
				
			}
			whichTree = genomes.size() - 1;
			
			completedInit = true;
		}
		

        // Here, we've separated the controls into a different method for each RobotType.
        // You can add the missing ones or rewrite this into your own control structure.
		while (!completedInit) {
			if (rc.getType() == RobotType.GARDENER) {
				System.out.println("Gardener waiting for archon:\r\n");
			}
			Clock.yield();
		}
        switch (rc.getType()) {
            case ARCHON:
                runArchon(rc);
                break;
            case GARDENER:
                runGardener(rc);
                break;
            case SOLDIER:
                runSoldier(rc);
                break;
            case LUMBERJACK:
                runLumberjack(rc);
                break;
			case SCOUT:
				runScout(rc);
				break;
			case TANK:
				runTank(rc);
				break;
        }
	}
	
	static int boolToInt(boolean b) {
		return b ? 1 : 0;
	}
	
	static float squaredist(MapLocation a, MapLocation b) {
		float xdiff = a.x - b.x;
		float ydiff = a.y - b.y;
		return xdiff * xdiff + ydiff * ydiff;
	}
	
	static int binAngle(MapLocation a, MapLocation b) {
		float theta = (float)Math.atan2(b.y - a.y, b.x - a.x);
		if (theta < -7*Math.PI/8 || theta > 7*Math.PI/8) {
			return 4;
		}
		else if (theta < -5*Math.PI/8) {
			return 5;
		}
		else if (theta < -3*Math.PI/8) {
			return 6;
		}
		else if (theta < -1*Math.PI/8) {
			return 7;
		}
		else if (theta < 1*Math.PI/8) {
			return 0;
		}
		else if (theta < 3*Math.PI/8) {
			return 1;
		}
		else if (theta < 5*Math.PI/8) {
			return 2;
		}
		else {
			return 3;
		}
	}

    static void runArchon(RobotController rc) throws GameActionException {
        System.out.println("I'm an archon!");
		ArrayList<Float> memory = new ArrayList<Float>();
		ArrayList<MapLocation> mapMemory = new ArrayList<MapLocation>();

        // The code you want your robot to perform every round should be in this loop
        while (true) {
			initializeMemory(rc, memory, mapMemory);
			if (rc.getRoundNum() > lastRound) {
				lastRound = rc.getRoundNum();
				for (int i = 0; i < 6; i++) {
					 numRobots[i] = 0;
				}
			}
			numRobots[0]++;
			if (rc.getRoundNum() > roundLimit - 5 && rc.readBroadcast(0) == 0) {
				rc.broadcast(0, 1);
				writePerformance(rc);
			}
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            //try {
				genomes.get(whichTree).runTree(rc, memory, mapMemory);
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();
            /*} catch (Exception e) {
                System.out.println("Archon Exception");
                e.printStackTrace();
            }*/
        }
    }

	static void runGardener(RobotController rc) throws GameActionException {
        System.out.println("I'm a gardener!");
		ArrayList<Float> memory = new ArrayList<Float>();
		ArrayList<MapLocation> mapMemory = new ArrayList<MapLocation>();

        // The code you want your robot to perform every round should be in this loop
        while (true) {
			initializeMemory(rc, memory, mapMemory);
			numRobots[1]++;
			if (rc.getRoundNum() > roundLimit - 5 && rc.readBroadcast(0) == 0) {
				rc.broadcast(0, 1);
				writePerformance(rc);
			}

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
				genomes.get(whichTree).runTree(rc, memory, mapMemory);
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            }
        }
    }

    static void runSoldier(RobotController rc) throws GameActionException {
        System.out.println("I'm an soldier!");
		ArrayList<Float> memory = new ArrayList<Float>();
		ArrayList<MapLocation> mapMemory = new ArrayList<MapLocation>();

        // The code you want your robot to perform every round should be in this loop
        while (true) {
			initializeMemory(rc, memory, mapMemory);
			numRobots[2]++;
			if (rc.getRoundNum() > roundLimit - 5 && rc.readBroadcast(0) == 0) {
				rc.broadcast(0, 1);
				writePerformance(rc);
			}
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
				genomes.get(whichTree).runTree(rc, memory, mapMemory);
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Soldier Exception");
                e.printStackTrace();
            }
        }
    }

	static void runTank(RobotController rc) throws GameActionException {
	    System.out.println("I'm a tank!");
		ArrayList<Float> memory = new ArrayList<Float>();
		ArrayList<MapLocation> mapMemory = new ArrayList<MapLocation>();
        Team enemy = rc.getTeam().opponent();

        // The code you want your robot to perform every round should be in this loop
        while (true) {
			initializeMemory(rc, memory, mapMemory);
			numRobots[3]++;
			if (rc.getRoundNum() > roundLimit - 5 && rc.readBroadcast(0) == 0) {
				rc.broadcast(0, 1);
				writePerformance(rc);
			}

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
				genomes.get(whichTree).runTree(rc, memory, mapMemory);
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Tank Exception");
                e.printStackTrace();
            }
        }
	}
	
	static void runScout(RobotController rc) throws GameActionException {
		System.out.println("I'm a scout!");
		ArrayList<Float> memory = new ArrayList<Float>();
		ArrayList<MapLocation> mapMemory = new ArrayList<MapLocation>();
        Team enemy = rc.getTeam().opponent();

        // The code you want your robot to perform every round should be in this loop
        while (true) {
			initializeMemory(rc, memory, mapMemory);
			numRobots[4]++;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
				genomes.get(whichTree).runTree(rc, memory, mapMemory);
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Scout Exception");
                e.printStackTrace();
            }
        }
	}
	
	static void runLumberjack(RobotController rc) throws GameActionException {
        System.out.println("I'm a lumberjack!");
		ArrayList<Float> memory = new ArrayList<Float>();
		ArrayList<MapLocation> mapMemory = new ArrayList<MapLocation>();
        Team enemy = rc.getTeam().opponent();

        // The code you want your robot to perform every round should be in this loop
        while (true) {
			initializeMemory(rc, memory, mapMemory);
			numRobots[5]++;
			if (rc.getRoundNum() > roundLimit - 5 && rc.readBroadcast(0) == 0) {
				rc.broadcast(0, 1);
				writePerformance(rc);
			}

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
				genomes.get(whichTree).runTree(rc, memory, mapMemory);
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Lumberjack Exception");
                e.printStackTrace();
            }
        }
    }

	static void writePerformance(RobotController rc) {
		float performance = getPerformance(rc);
		genomes.get(whichTree).score = performance;
		String outString = String.format("%4s", Integer.toHexString(genomes.size())).replace(' ', '0');
		for (BehaviorTree tree : genomes) {
			String genome = tree.toGenome();
			outString += String.format("%8s", Integer.toHexString(Float.floatToRawIntBits(tree.score))).replace(' ', '0');
			outString += String.format("%4s", Integer.toHexString(genome.length())).replace(' ', '0');
			for (int i = 0; i < genome.length(); i++) {
				outString += String.format("%4s", Integer.toHexString(genome.charAt(i))).replace(' ', '0');
			}
		}
		try {
			HackFile.write(outString);
		}
		catch (IOException e) {
			System.out.println("Failed to write to hackfile");
		}
	}
	
	static float getPerformance(RobotController rc) {
		//HACK: assume all victory points worth 10 bullets
		return rc.getTeamVictoryPoints() * 10.0f + rc.getTeamBullets()
			+ numRobots[0] * 100f + numRobots[1] * 10f + numRobots[2] * 10f + numRobots[3] * 30f + numRobots[4] * 8f
				+ numRobots[5] * 10f;
	}
    /**
     * Returns a random Direction
     * @return a random Direction
     */
    static Direction randomDirection() {
        return new Direction((float)Math.random() * 2 * (float)Math.PI);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        return tryMove(dir,20,3);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
     *
     * @param dir The intended direction of movement
     * @param degreeOffset Spacing between checked directions (degrees)
     * @param checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

        // First, try intended direction
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }

        // Now try a bunch of similar angles
        boolean moved = false;
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            if(rc.canMove(dir.rotateLeftDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset*currentCheck));
                return true;
            }
            // Try the offset on the right side
            if(rc.canMove(dir.rotateRightDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateRightDegrees(degreeOffset*currentCheck));
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    }

    /**
     * A slightly more complicated example function, this returns true if the given bullet is on a collision
     * course with the current robot. Doesn't take into account objects between the bullet and this robot.
     *
     * @param bullet The bullet in question
     * @return True if the line of the bullet's path intersects with this robot's current position.
     */
    static boolean willCollideWithMe(BulletInfo bullet) {
        MapLocation myLocation = rc.getLocation();

        // Get relevant bullet information
        Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;

        // Calculate bullet relations to this robot
        Direction directionToRobot = bulletLocation.directionTo(myLocation);
        float distToRobot = bulletLocation.distanceTo(myLocation);
        float theta = propagationDirection.radiansBetween(directionToRobot);

        // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
        if (Math.abs(theta) > Math.PI/2) {
            return false;
        }

        // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
        // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
        // This corresponds to the smallest radius circle centered at our location that would intersect with the
        // line that is the path of the bullet.
        float perpendicularDist = (float)Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

        return (perpendicularDist <= rc.getType().bodyRadius);
    }
}
