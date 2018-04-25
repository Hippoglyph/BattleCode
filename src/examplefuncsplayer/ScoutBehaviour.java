package examplefuncsplayer;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TreeInfo;

public class ScoutBehaviour extends RobotBehaviour {
	
	//private static final int SCOUT_MAP_SIZE = 120;
	private static int[][] scoutHistory = new int[MAP_SIDE_SQUARES_NUM][MAP_SIDE_SQUARES_NUM];
	
	//private final int CIRCLE_SAMP_NUM = (22+19+16+13+10+7+4);
	
	private static final int CIRCLES_NUM = 6;
	private static final int MAX_CIRCLE_POINTS_NUM = 19; // target point - 9
	private static final int TARGET_POINT_INDEX = 9;
	private static final int PREV_POINTS_NUM = 4;
	
	private static float[][][] dists = new float[CIRCLES_NUM][MAX_CIRCLE_POINTS_NUM][PREV_POINTS_NUM];
	private static int[] circleSizes = new int[CIRCLES_NUM];
	private static float[] angles = new float[CIRCLES_NUM];
	
	private static final float outerCircleRadius = RobotType.SCOUT.sensorRadius-RobotType.SCOUT.bodyRadius;
	private static final float vertexDistance = outerCircleRadius / CIRCLES_NUM;
	
	private final float outerCircleGap;
	private final float outerCircleAngle;
	private final float targetPointAngle;
	
	
	public ScoutBehaviour(RobotController rc, RobotType ourType) throws GameActionException {
		super(rc, ourType);
		
		angles[0] = 1.0472f;//60.0f;
		angles[1] = 0.505360503793f;//28.955024f;
		angles[2] = 0.33489615052f; //19.188136f;
		angles[3] = 0.250655669964f; //14.361512f;
		angles[4] = 0.200334843116f; //11.478341f;
		angles[5] = 0.16686017855f; //9.560384f;
		//angles[6] = 0.14297890821f;//8.192088f;
		
		circleSizes[0] = 4;
		circleSizes[1] = 7;
		circleSizes[2] = 10;
		circleSizes[3] = 13;
		circleSizes[4] = 16;
		circleSizes[5] = 19;
		//circleSizes[6] = 22;
		
		outerCircleAngle = angles[CIRCLES_NUM-1];
		outerCircleGap = (PI - (circleSizes[CIRCLES_NUM-1]-1)*outerCircleAngle)/2;
		targetPointAngle = outerCircleGap + TARGET_POINT_INDEX*outerCircleAngle;
		
		for (int i = 1; i < CIRCLES_NUM; i++) {
			float angle = angles[i];
			float angleGap = (PI - (circleSizes[i]-1)*angle)/2;
			float currCircleRadius = vertexDistance*(i+1);
			float prevCircleRadius = vertexDistance*i;
			float innerAngle = angles[i-1];
			float innerGap = (PI - (circleSizes[i-1]-1)*innerAngle)/2;
			
			for (int j = 0; j < circleSizes[i]; j++) {
				float accAngle = angleGap + j*angle;
				
				float[] currDists = dists[i][j];
				
				float vertex_x = -(float)Math.cos(accAngle)*currCircleRadius;
				float vertex_y = (float)Math.sin(accAngle)*currCircleRadius;
				
				float relativeAngle = accAngle-innerGap;
				int prevCircleVertexInd;
				if (relativeAngle < 0) {
					prevCircleVertexInd = -1;
				} else {
					prevCircleVertexInd = (int)(relativeAngle / innerAngle);
				}
				
				float angle1 = innerAngle*(prevCircleVertexInd-1) + innerGap;
				float x_diff1 = -(float)Math.cos(angle1)*prevCircleRadius - vertex_x;
				float y_diff1 = (float)Math.sin(angle1)*prevCircleRadius - vertex_y;
				currDists[0] = (float)Math.sqrt(x_diff1*x_diff1 + y_diff1*y_diff1);
				
				float angle2 = innerAngle*(prevCircleVertexInd) + innerGap;
				float x_diff2 = -(float)Math.cos(angle2)*prevCircleRadius - vertex_x;
				float y_diff2 = (float)Math.sin(angle2)*prevCircleRadius - vertex_y;
				currDists[1] = (float)Math.sqrt(x_diff2*x_diff2 + y_diff2*y_diff2);
				
				float angle3 = innerAngle*(prevCircleVertexInd+1) + innerGap;
				float x_diff3 = -(float)Math.cos(angle3)*prevCircleRadius - vertex_x;
				float y_diff3 = (float)Math.sin(angle3)*prevCircleRadius - vertex_y;
				currDists[2] = (float)Math.sqrt(x_diff3*x_diff3 + y_diff3*y_diff3);
				
				float angle4 = innerAngle*(prevCircleVertexInd+2) + innerGap;
				float x_diff4 = -(float)Math.cos(angle4)*prevCircleRadius - vertex_x;
				float y_diff4 = (float)Math.sin(angle4)*prevCircleRadius - vertex_y;
				currDists[3] = (float)Math.sqrt(x_diff4*x_diff4 + y_diff4*y_diff4);
				
				
			}
			
		}
		
	}
	
	private int nextMoveIndex = 0;
	private MapLocation[] nextMovesPlan = null;
	
	private int prevMapUpdateRound = -1;

	private Direction plannedDirectionToBorder = null;

	private Direction plannedDirection = null;
	
	
	private int[][] prevVertices = new int[CIRCLES_NUM][MAX_CIRCLE_POINTS_NUM];
	private float[][][] prevDists = new float[CIRCLES_NUM][MAX_CIRCLE_POINTS_NUM][PREV_POINTS_NUM];
	private float[][] vertexAccDist = new float[CIRCLES_NUM][MAX_CIRCLE_POINTS_NUM];
	
	@Override
	protected void turn() throws GameActionException {
		
		updateBorders();
		
		boolean mapUpdated = false;
		if (nextMovesPlan == null) {
			if (!(minX > Float.MIN_VALUE && minY > Float.MIN_VALUE)) {
				
				if (!rc.onTheMap(new MapLocation(ourLocation.x - ourType.sensorRadius, ourLocation.y))) {
					
					if (theirArchonsCenter.y - ourLocation.y > 0) {
						plannedDirectionToBorder = new Direction(-PI/2);
					} else {
						plannedDirectionToBorder = new Direction(PI/2);
					}
					
				} else if (!rc.onTheMap(new MapLocation(ourLocation.x, ourLocation.y - ourType.sensorRadius))) {
					
					if (theirArchonsCenter.x - ourLocation.x > 0) {
						plannedDirectionToBorder = new Direction(PI);
					} else {
						plannedDirectionToBorder = new Direction(0);
					}
					
				} else if (!rc.onTheMap(new MapLocation(ourLocation.x + ourType.sensorRadius, ourLocation.y))) {
					
					if (theirArchonsCenter.y - ourLocation.y > 0) {
						plannedDirectionToBorder = new Direction(-PI/2);
					} else {
						plannedDirectionToBorder = new Direction(PI/2);
					}
					
				} else if (!rc.onTheMap(new MapLocation(ourLocation.x, ourLocation.y + ourType.sensorRadius))) {
					
					if (theirArchonsCenter.x - ourLocation.x > 0) {
						plannedDirectionToBorder = new Direction(PI);
					} else {
						plannedDirectionToBorder = new Direction(0);
					}
					
				} else {
					plannedDirectionToBorder = theirArchonsCenter.directionTo(middlePoint);
				}
				
				nextMovesPlan = getPathPlan(plannedDirectionToBorder);
				
			}
			
			if (mapInitialized) {
				// update map with planning
				// nextMovesPlan(some direction according to the global heuristic)
				mapUpdated = true;
			}
		}
		
		if (rc.hasMoved()) {
			return;
		}
		
		boolean moved = false;
		
		if (nextMovesPlan != null) {
			MapLocation nextVertex = nextMovesPlan[nextMoveIndex];
			float dist = ourLocation.distanceTo(nextVertex);
			if (dist > RobotType.SCOUT.strideRadius) {
				dist = RobotType.SCOUT.strideRadius;
			} else {
				++nextMoveIndex;
			}
			
			plannedDirection = ourLocation.directionTo(nextVertex);
			if (rc.canMove(plannedDirection, dist)) {
				rc.move(plannedDirection, dist);
				moved = true;
				if (nextMoveIndex == nextMovesPlan.length) {
					nextMovesPlan = null;
					nextMoveIndex = 0;
				}
				
			}
		}
		
		if (!moved) {
			nextMovesPlan = null;
			nextMoveIndex = 0;
			
			plannedDirection = new Direction((float)Math.random() * 2 * (float)Math.PI);
			if (rc.canMove(plannedDirection)) {
				rc.move(plannedDirection);
			}
		}
		
		if (!mapUpdated && currRound - prevMapUpdateRound > 1) {
			ourInfo.opponentsNum = updateMap();
			prevMapUpdateRound = currRound;
		}
		
		
	}

	@Override
	protected void tick() throws GameActionException {
		
		// Recreate the data structures to save computations in path planning
		if (prevVertices == null) {
			prevVertices = new int[CIRCLES_NUM][MAX_CIRCLE_POINTS_NUM];
		}
		if (prevDists == null) {
			prevDists = new float[CIRCLES_NUM][MAX_CIRCLE_POINTS_NUM][PREV_POINTS_NUM];
		}
		if (vertexAccDist == null) {
			vertexAccDist = new float[CIRCLES_NUM][MAX_CIRCLE_POINTS_NUM];
		}
		
		
		/*
		// if cannot move to next, replan
		if (nextMovesPlan != null) {
			MapLocation nextVertex = nextMovesPlan[nextMoveIndex];
			ourLocation = rc.getLocation();
			float dist = ourLocation.distanceTo(nextVertex);
			if (dist > RobotType.SCOUT.strideRadius) {
				dist = RobotType.SCOUT.strideRadius;
			}
			if (rc.canMove(ourLocation.directionTo(nextVertex), dist)) {
				
			}
		}
		*/
		
		
		
	}
	
	
	private Square square;
	private int maxI;
	private int minI;
	private int maxJ;
	private int minJ;
	
	private Square[][] squares;
	
	// Returns nearby opponents number
	private int updateMap() throws GameActionException {
		
		if (!mapInitialized) {
			return -1;
		}
		
		// Get valid index range in X and Y axis that can be possibly sensed by our agent
		maxI = (int) ((ourInfo.location.y + ourType.sensorRadius - minY)/squareSideY) + 1;
		minI = (int)Math.floor((ourInfo.location.y - ourType.sensorRadius - minY)/squareSideY);
		maxJ = (int) ((ourInfo.location.x + ourType.sensorRadius - minX)/squareSideX) + 1;
		minJ = (int)Math.floor((ourInfo.location.x - ourType.sensorRadius - minX)/squareSideX);
		
		int innerVertexNumY = maxI - minI - 1;
		int innerVertexNumX = maxJ - minJ - 1;
		float y_coord;
		boolean[][] innerVertices = new boolean[(innerVertexNumY)][(innerVertexNumX)];
		for (int i = 0; i < innerVertexNumY; i++) {
			y_coord = minY + squareSideY*(minI + i + 1);
			for (int j = 0; j < innerVertexNumX; j++) {
				innerVertices[i][j] = rc.canSenseLocation(new MapLocation(minX + squareSideX*(minJ + j + 1), y_coord));
			}
		}

		int squareRows = maxI-minI;
		int squareCols = maxJ-minJ;
		squares = new Square[squareRows][squareCols];
		
		int min_i = minI;
		int min_j = minJ;
		if (minI < 0) {minI = 0;}
		if (minJ < 0) {minJ = 0;}
		if (maxI > MAP_SIDE_SQUARES_NUM) { maxI = MAP_SIDE_SQUARES_NUM; }
		if (maxJ > MAP_SIDE_SQUARES_NUM) { maxJ = MAP_SIDE_SQUARES_NUM; }
		
		int prev_i;
		int prev_j;
		for (int row = minI; row < maxI; row++) {
			int i = row - min_i;
			prev_i = i - 1;
			int j;
			for (int column = minJ; column < maxJ; column++) {
				j = column - min_j;
				prev_j = j - 1;
				
				boolean allInside = true;
				boolean nonInside = true;
				
				if (prev_i >= 0) {
					// bottom
					if (prev_j >= 0 && innerVertices[prev_i][prev_j]) {
						// left
						nonInside = false;
					} else {
						allInside = false;
					}
					if (j < innerVertexNumX && innerVertices[prev_i][j]) {
						// right
						nonInside = false;
					} else {
						allInside = false;
					}
				} else {
					allInside = false;
				}
				
				if (i < innerVertexNumY) {
					// upper
					if (prev_j >= 0 && innerVertices[i][prev_j]) {
						// left
						nonInside = false;
					} else {
						allInside = false;
					}
					if (j < innerVertexNumX && innerVertices[i][j]) {
						// right
						nonInside = false;
					} else {
						allInside = false;
					}
				} else {
					allInside = false;
				}
				
				if (!nonInside) {
					squares[i][j] = new Square(allInside);
					if (allInside) {
						scoutHistory[row][column] = currRound;
					}
				}
			}
		}
		
		int square_i;
		int square_j;
		// Loop among the trees, accumulating the info
		for (TreeInfo tree : nearbyTrees) {
			// Get row and column of tree's square in square matrix
			square_i = (int) Math.floor((tree.location.y - minY) / squareSideY) - min_i;
			if (square_i < 0 || square_i >= squareRows) {
				continue;
			}
			square_j = (int) Math.floor((tree.location.x - minX) / squareSideX) - min_j;
			if (square_j < 0 || square_j >= squareCols) {
				continue;
			}
			
			square = squares[square_i][square_j];
			if (square != null) {
				if (tree.team == opponentTeam) {
					++square.numOppTrees;
				} else if (tree.team == Team.NEUTRAL) {
					// Check if tree has robot inside
					RobotType containedType = tree.getContainedRobot();
					// If the tree contains a robot
					if (containedType != null) {
						// Update corresponding robot type count
						switch (containedType) {
							case ARCHON:
								square.prizeUnitsUtility += MapSquareInfo.ARCHONS_COEF;
								break;
							case GARDENER:
								square.prizeUnitsUtility += MapSquareInfo.GARDENERS_COEF;
								break;
							case SOLDIER:
								square.prizeUnitsUtility += MapSquareInfo.SOLDIERS_COEF;
								break;
							case TANK:
								square.prizeUnitsUtility += MapSquareInfo.TANKS_COEF;
								break;
							case SCOUT:
								square.prizeUnitsUtility += MapSquareInfo.SCOUTS_COEF;
								break;
							default:
								square.prizeUnitsUtility += MapSquareInfo.LUMBERJACKS_COEF;
								break;
						}
					} 
					
				}
				square.occupiedAreaPercent += (tree.radius*tree.radius);
			}
		}
		
		int result = 0;
		for (RobotInfo robot : nearbyRobots) {
			// Get row and column of tree's square in square matrix
			square_i = (int) Math.floor((robot.location.y - minY) / squareSideY) - min_i;
			if (square_i < 0 || square_i >= squareRows) {
				continue;
			}
			square_j = (int) Math.floor((robot.location.x - minX) / squareSideX) - min_j;
			if (square_j < 0 || square_j >= squareCols) {
				continue;
			}
			square = squares[square_i][square_j];
			if (square != null) {
				if (robot.team == opponentTeam) {
					switch (robot.type) {
						case ARCHON:
							++square.numOppArchons;				
							break;
						case GARDENER:
							++square.numOppGardeners;
							break;
						case SOLDIER:
							square.oppFightersHealth += robot.health*MapSquareInfo.SOLDIER_HEALTH_COST;
							break;
						case LUMBERJACK:
							square.oppFightersHealth += robot.health*MapSquareInfo.LUMBERJACK_HEALTH_COST;
							break;
						case TANK:
							square.oppFightersHealth += robot.health*MapSquareInfo.TANK_HEALTH_COST;
							break;
						default:
							square.oppFightersHealth += robot.health*MapSquareInfo.SCOUT_HEALTH_COST;
							break;
					}
					++result;
				}
				square.occupiedAreaPercent += (robot.getRadius()*robot.getRadius());
			}
			
		}
		
		MapSquareInfo storedSquare;
		boolean needWrite;
		for (int row = minI; row < maxI; row++) {
			int i = row - min_i;
			int j;
			for (int column = minJ; column < maxJ; column++) {
				j = column - min_j;
				square = squares[i][j];
				if (square != null) {
					int pointer = MAP_INFO_SEGMENT_START + row*MAP_SIDE_SQUARES_NUM + column;
					needWrite = false;
					square.occupiedAreaPercent *= (PI*100/squareArea);
					if (square.fullyInside) {
						needWrite = true;
					} else {
						storedSquare = new MapSquareInfo(pointer, rc);
						
						if (square.numOppArchons > storedSquare.numOppArchons) {
							needWrite = true;
						} else {
							square.numOppArchons = storedSquare.numOppArchons;
						}
						
						if (square.numOppGardeners > storedSquare.numOppGardeners) {
							needWrite = true;
						} else {
							square.numOppGardeners = storedSquare.numOppGardeners;
						}
						
						if (square.numOppTrees > storedSquare.numOppTrees) {
							needWrite = true;
						} else {
							square.numOppTrees = storedSquare.numOppTrees;
						}
						
						if (square.occupiedAreaPercent > storedSquare.occupiedAreaPercent) {
							needWrite = true;
						} else {
							square.occupiedAreaPercent = storedSquare.occupiedAreaPercent;
						}
						
						if (square.oppFightersHealth > storedSquare.oppFightersHealth) {
							needWrite = true;
						} else {
							square.oppFightersHealth = storedSquare.oppFightersHealth;
						}
						
						if (square.prizeUnitsUtility > storedSquare.prizeUnitsUtility) {
							needWrite = true;
						} else {
							square.prizeUnitsUtility = storedSquare.prizeUnitsUtility;
						}
						
					}
					
					if (needWrite) {
						square.write(pointer, rc);
					}
				}
			}
		}
		
		return result;
	}
	
	
	
	
	
	private float angleOffset;
	private MapLocation target;
	private float robotAngle;
	private float distToRobot;
	private float robotRadius;
	private MapLocation robotRelativeLocation;
	
	private float currCircleRadius;
	private int currCircleSize;
	private float angle;
	private float angleGap;

	private float prevCircleRadius;
	private int prevCircleSize;
	private float innerAngle;
	private float innerGap;
	
	private int nearestVertexInd;
	private int currVertIndex;
	
	private MapLocation[] getPathPlan(Direction destination) throws GameActionException {
		
		angleOffset = destination.radians - targetPointAngle;
		target = ourLocation.add(destination, outerCircleRadius);
		
		ourInfo.opponentsNum = 0;
		for (RobotInfo robot : nearbyRobots) {
			
			robotAngle = ourLocation.directionTo(robot.location).radians - angleOffset;
			distToRobot = ourLocation.distanceTo(robot.location);
			robotRadius = robot.getRadius();
			robotRelativeLocation = new MapLocation(robot.location.x - ourLocation.x, robot.location.y - ourLocation.y);
			
			for (int i = 0; i < CIRCLES_NUM; i++) {
				
				currCircleRadius = vertexDistance*(i+1);
				if ((distToRobot - robotRadius - ourType.bodyRadius) > currCircleRadius) {
					// never collide with robot
					continue;
				}
				
				currCircleSize = circleSizes[i];
				angle = angles[i];
				angleGap = (PI - (currCircleSize-1)*angle)/2;

				prevCircleRadius = vertexDistance*i;
				if (i==0) {
					innerAngle = 0;
					innerGap = 0;
				} else {
					prevCircleSize = circleSizes[i-1];
					innerAngle = angles[i-1];
					innerGap = (PI - (-1)*innerAngle)/2;
				}
				
				nearestVertexInd = (int)Math.floor((robotAngle-angleGap) / angle);
				
				currVertIndex = nearestVertexInd-1;
				checkVertex(robot, i);
				
				currVertIndex = nearestVertexInd;
				checkVertex(robot, i);
				
				currVertIndex = nearestVertexInd + 1;
				checkVertex(robot, i);
				
				currVertIndex = nearestVertexInd + 2;
				checkVertex(robot, i);
			}
			
			if (robot.team == opponentTeam) {
				++ourInfo.opponentsNum;
			}
		}
		
		
		for (int j = 0; j < circleSizes[0]; j++) {
			if (prevVertices[0][j] < 0) {
				vertexAccDist[0][j] = Float.MAX_VALUE;
				continue;
			}
			vertexAccDist[0][j] = vertexDistance;
		}

		float outerAngle = angles[CIRCLES_NUM-1];
		float outerAngleGap = (PI - (circleSizes[CIRCLES_NUM-1]-1)*outerAngle)/2;
		float outerRadius = vertexDistance*CIRCLES_NUM;
		float goalAccAngle = outerAngleGap + TARGET_POINT_INDEX*outerAngle;
		float goal_x = -(float)Math.cos(goalAccAngle)*outerRadius;
		float goal_y = (float)Math.sin(goalAccAngle)*outerRadius;
		
		for (int i = 1; i < CIRCLES_NUM; i++) {
			
			float angle = angles[i];
			float angleGap = (PI - (circleSizes[i]-1)*angle)/2;
			float innerAngle = angles[i-1];
			float innerGap = (PI - (circleSizes[i-1]-1)*innerAngle)/2;
			float currCircleRadius = vertexDistance*(i+1);
			
			for (int j = 0; j < circleSizes[i]; j++) {
				
				float accAngle = angleGap + j*angle;
				float vertex_x = -(float)Math.cos(accAngle)*currCircleRadius;
				float vertex_y = (float)Math.sin(accAngle)*currCircleRadius;
				
				float relativeAngle = accAngle-innerGap;
				int prevCircleVertexInd;
				if (relativeAngle < 0) {
					prevCircleVertexInd = -1;
				} else {
					prevCircleVertexInd = (int)(relativeAngle / innerAngle);
				}
				
				if (prevVertices[i][j] < 0) {
					vertexAccDist[i][j] = Float.MAX_VALUE;
					continue;
				}
				
				float minDist = Float.MAX_VALUE;
				int prevClosestVert = -1;
				
				if (prevCircleVertexInd-1 >= 0 && prevCircleVertexInd-1 < circleSizes[i-1] && prevDists[i][j][0] < Float.MAX_VALUE) {
					float accDist = vertexAccDist[i-1][prevCircleVertexInd-1] + dists[i][j][0];
					if (accDist < minDist) {
						minDist = accDist;
						prevClosestVert = prevCircleVertexInd-1;
					}
				}
				
				if (prevCircleVertexInd >= 0 && prevCircleVertexInd < circleSizes[i-1] && prevDists[i][j][1] < Float.MAX_VALUE) {
					float accDist = vertexAccDist[i-1][prevCircleVertexInd] + dists[i][j][1];
					if (accDist < minDist) {
						minDist = accDist;
						prevClosestVert = prevCircleVertexInd;
					}
				}
				
				int nextInd = prevCircleVertexInd+1;
				if (nextInd >= 0 && nextInd < circleSizes[i-1] && prevDists[i][j][2] < Float.MAX_VALUE) {
					float accDist = vertexAccDist[i-1][nextInd] + dists[i][j][2];
					if (accDist < minDist) {
						minDist = accDist;
						prevClosestVert = nextInd;
					}
				}
				
				int nextNextInd = prevCircleVertexInd+2;
				if (nextNextInd < circleSizes[i-1] && prevDists[i][j][3] < Float.MAX_VALUE) {
					float accDist = vertexAccDist[i-1][nextNextInd] + dists[i][j][3];
					if (accDist < minDist) {
						minDist = accDist;
						prevClosestVert = nextNextInd;
					}
				}
				
				prevVertices[i][j] = prevClosestVert;
				if (i == CIRCLES_NUM-1) {
					float x_diff = vertex_x-goal_x;
					float y_diff = vertex_y-goal_y;
					// add distance to the goal
					vertexAccDist[i][j] = minDist + (float)Math.sqrt(x_diff*x_diff + y_diff*y_diff);
					continue;
				}
				vertexAccDist[i][j] = minDist;
			}
			
		}
		
		int outerCircleVertex = -1;
		if (vertexAccDist[CIRCLES_NUM-1][TARGET_POINT_INDEX] < Float.MAX_VALUE) {
			outerCircleVertex = TARGET_POINT_INDEX;
		} else {
			float minTargetDist = Float.MAX_VALUE;
			for (int j = 0; j < circleSizes[CIRCLES_NUM-1]; j++) {
				float dist = vertexAccDist[CIRCLES_NUM-1][j];
				if (dist < minTargetDist) {
					minTargetDist = vertexAccDist[CIRCLES_NUM-1][j];
					outerCircleVertex = j;
				}
			}
		}
		
		if (outerCircleVertex < 0) {
			prevVertices = null;
			prevDists = null;
			vertexAccDist = null;
			return null;
		}
		
		nextMovesPlan = new MapLocation[CIRCLES_NUM];
		
		float trueGoalAccAngle = outerAngleGap + outerCircleVertex*outerAngle + angleOffset;
		float true_goal_x = ourLocation.x + (float)Math.cos(trueGoalAccAngle)*outerRadius;
		float true_goal_y = ourLocation.y + (float)Math.sin(trueGoalAccAngle)*outerRadius;
		
		nextMovesPlan[CIRCLES_NUM-1] = new MapLocation(true_goal_x, true_goal_y);
		
		int currVertex = outerCircleVertex;
		for (int i = CIRCLES_NUM-2; i >= 0; --i) {

			currVertex = prevVertices[i+1][currVertex];
			
			float angle = angles[i];
			float angleGap = (PI - (circleSizes[i]-1)*angle)/2;
			float currCircleRadius = vertexDistance*(i+1);
			
			float accAngle = angleGap + currVertex*angle + angleOffset;
			float vertex_x = (float)Math.cos(accAngle)*currCircleRadius;
			float vertex_y = (float)Math.sin(accAngle)*currCircleRadius;
			nextMovesPlan[i] = new MapLocation(ourLocation.x + vertex_x, ourLocation.y + vertex_y);
		}
		
		prevVertices = null;
		prevDists = null;
		vertexAccDist = null;
		
		return nextMovesPlan;
	}
	
	
	private void checkVertex(RobotInfo robot, int i) throws GameActionException {
		
		int prevVertIndex;
		if (currVertIndex >= 0 && currVertIndex < currCircleSize) {
			float vertex1_angle = angle*(currVertIndex) + angleGap;
			float vertex1_angle_plus_offset = vertex1_angle + angleOffset;
			MapLocation trueVertexLocation = new MapLocation(ourLocation.x + (float)Math.cos(vertex1_angle_plus_offset)*currCircleRadius,
					ourLocation.y + (float)Math.sin(vertex1_angle_plus_offset)*currCircleRadius);
			
			MapLocation vertexLocation = new MapLocation(-(float)Math.cos(vertex1_angle)*currCircleRadius,
					(float)Math.sin(vertex1_angle)*currCircleRadius);
			
			if (/*!(rc.onTheMap(new MapLocation(trueVertexLocation.x + ourType.bodyRadius, trueVertexLocation.y)) &&
					rc.onTheMap(new MapLocation(trueVertexLocation.x - ourType.bodyRadius, trueVertexLocation.y)) &&
					rc.onTheMap(new MapLocation(trueVertexLocation.x, trueVertexLocation.y + ourType.bodyRadius)) &&
					rc.onTheMap(new MapLocation(trueVertexLocation.x, trueVertexLocation.y - ourType.bodyRadius)))
						||*/ trueVertexLocation.distanceTo(robot.location) < ourType.bodyRadius + robotRadius) {
				// collision
				prevVertices[i][currVertIndex] = -1;
			} else {
				
				float relativeAngle = vertex1_angle - innerGap;
				int prevCircleVertexInd = 0;
				if (relativeAngle < 0) {
					prevCircleVertexInd = -1;
				} else if (i != 0) {
					prevCircleVertexInd = (int)(relativeAngle / innerAngle);
				}
				
				if (i == 0) {
					
					float vector_x = (vertexLocation.x)/vertexDistance;
					float vector_y = (vertexLocation.y)/vertexDistance;
					float obstacle_rel_x = robotRelativeLocation.x;
					float obstacle_rel_y = robotRelativeLocation.y;
					
					float dotProd = vector_x*obstacle_rel_x + vector_y*obstacle_rel_y;
					boolean collides = false;
					if (dotProd > 0 && dotProd < vertexDistance) {
						// Checking distance to the edge
						float dist = Math.abs(-vector_y*obstacle_rel_x + vector_x*obstacle_rel_y);
						if (dist < robotRadius + ourType.bodyRadius) {
							collides = true;
							prevVertices[i][currVertIndex] = -1;
						}
					}
					
				} else {
					
					prevVertIndex = prevCircleVertexInd - 1;
					if (prevVertIndex >= 0 && prevVertIndex < prevCircleSize && prevVertices[i-1][prevVertIndex] > 0) {
						float angle1 = innerAngle*(prevVertIndex) + innerGap;
						float prev_point_x = -(float)Math.cos(angle1)*prevCircleRadius;
						float prev_point_y = (float)Math.sin(angle1)*prevCircleRadius;
						
						float vectorDist = dists[i][currVertIndex][prevVertIndex];
						float vector_x = (vertexLocation.x - prev_point_x)/vectorDist;
						float vector_y = (vertexLocation.y - prev_point_y)/vectorDist;
						float obstacle_rel_x = robotRelativeLocation.x - prev_point_x;
						float obstacle_rel_y = robotRelativeLocation.y - prev_point_y;
						
						float dotProd = vector_x*obstacle_rel_x + vector_y*obstacle_rel_y;
						boolean collides = false;
						if (dotProd > 0 && dotProd < vectorDist) {
							// Checking distance to the edge
							float dist = Math.abs(-vector_y*obstacle_rel_x + vector_x*obstacle_rel_y);
							if (dist < robotRadius + ourType.bodyRadius) {
								collides = true;
								prevDists[i][currVertIndex][0] = Float.MAX_VALUE;
							}
						}
						
						// else if (...) {  add negative utility to edges inside vision (if soldier/tank) or close enough (lumberjack) }
						
					} else {
						prevDists[i][currVertIndex][0] = Float.MAX_VALUE;
					}
					
					prevVertIndex = prevCircleVertexInd;
					if (prevVertIndex >= 0 && prevVertIndex < prevCircleSize && prevVertices[i-1][prevVertIndex] > 0) {
						float angle1 = innerAngle*(prevVertIndex) + innerGap;
						float prev_point_x = -(float)Math.cos(angle1)*prevCircleRadius;
						float prev_point_y = (float)Math.sin(angle1)*prevCircleRadius;
						
						float vectorDist = dists[i][currVertIndex][prevVertIndex];
						float vector_x = (vertexLocation.x - prev_point_x)/vectorDist;
						float vector_y = (vertexLocation.y - prev_point_y)/vectorDist;
						float obstacle_rel_x = robotRelativeLocation.x - prev_point_x;
						float obstacle_rel_y = robotRelativeLocation.y - prev_point_y;
						
						float dotProd = vector_x*obstacle_rel_x + vector_y*obstacle_rel_y;
						boolean collides = false;
						if (dotProd > 0 && dotProd < vectorDist) {
							// Checking distance to the edge
							float dist = Math.abs(-vector_y*obstacle_rel_x + vector_x*obstacle_rel_y);
							if (dist < robotRadius + ourType.bodyRadius) {
								collides = true;
								prevDists[i][currVertIndex][1] = Float.MAX_VALUE;
							}
						}
						
						// else if (...) {  add negative utility to edges inside vision (if soldier/tank) or close enough (lumberjack) }
						
					} else {
						prevDists[i][currVertIndex][1] = Float.MAX_VALUE;
					}
					
					prevVertIndex = prevCircleVertexInd + 1;
					if (prevVertIndex >= 0 && prevVertIndex < prevCircleSize && prevVertices[i-1][prevVertIndex] > 0) {
						float angle1 = innerAngle*(prevVertIndex) + innerGap;
						float prev_point_x = -(float)Math.cos(angle1)*prevCircleRadius;
						float prev_point_y = (float)Math.sin(angle1)*prevCircleRadius;
						
						float vectorDist = dists[i][currVertIndex][prevVertIndex];
						float vector_x = (vertexLocation.x - prev_point_x)/vectorDist;
						float vector_y = (vertexLocation.y - prev_point_y)/vectorDist;
						float obstacle_rel_x = robotRelativeLocation.x - prev_point_x;
						float obstacle_rel_y = robotRelativeLocation.y - prev_point_y;
						
						float dotProd = vector_x*obstacle_rel_x + vector_y*obstacle_rel_y;
						boolean collides = false;
						if (dotProd > 0 && dotProd < vectorDist) {
							// Checking distance to the edge
							float dist = Math.abs(-vector_y*obstacle_rel_x + vector_x*obstacle_rel_y);
							if (dist < robotRadius + ourType.bodyRadius) {
								collides = true;
								prevDists[i][currVertIndex][2] = Float.MAX_VALUE;
							}
						}
						
						// else if (...) {  add negative utility to edges inside vision (if soldier/tank) or close enough (lumberjack) }
						
					} else {
						prevDists[i][currVertIndex][2] = Float.MAX_VALUE;
					}
					
					prevVertIndex = prevCircleVertexInd+2;
					if (prevVertIndex >= 0 && prevVertIndex < prevCircleSize && prevVertices[i-1][prevVertIndex] > 0) {
						float angle1 = innerAngle*(prevVertIndex) + innerGap;
						float prev_point_x = -(float)Math.cos(angle1)*prevCircleRadius;
						float prev_point_y = (float)Math.sin(angle1)*prevCircleRadius;
						
						float vectorDist = dists[i][currVertIndex][prevVertIndex];
						float vector_x = (vertexLocation.x - prev_point_x)/vectorDist;
						float vector_y = (vertexLocation.y - prev_point_y)/vectorDist;
						float obstacle_rel_x = robotRelativeLocation.x - prev_point_x;
						float obstacle_rel_y = robotRelativeLocation.y - prev_point_y;
						
						float dotProd = vector_x*obstacle_rel_x + vector_y*obstacle_rel_y;
						boolean collides = false;
						if (dotProd > 0 && dotProd < vectorDist) {
							// Checking distance to the edge
							float dist = Math.abs(-vector_y*obstacle_rel_x + vector_x*obstacle_rel_y);
							if (dist < robotRadius + ourType.bodyRadius) {
								collides = true;
								prevDists[i][currVertIndex][3] = Float.MAX_VALUE;
							}
						}
						
						// else if (...) {  add negative utility to edges inside vision (if soldier/tank) or close enough (lumberjack) }
						
					} else {
						prevDists[i][currVertIndex][3] = Float.MAX_VALUE;
					}
				}
			
			}
		}
	}
	
	
}
