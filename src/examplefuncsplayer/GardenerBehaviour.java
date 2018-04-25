package examplefuncsplayer;

import java.util.ArrayList;
import java.util.Random;

//import battlecode.common.RobotController;
import battlecode.common.*;

public class GardenerBehaviour extends RobotBehaviour {

	private static Random rand = new Random();
	
	//final RobotType OUR_TYPE = RobotType.GARDENER;
	//final float OUR_RADIUS = RobotType.GARDENER.bodyRadius;
	final int POSITION_SAMPLE_NUMBER = 30;
	//final float PI = (float)Math.PI;
	final int MAX_TREE_NUMBER = 5; // 5 trees because 6 get the gardener stuck in a circle
	//private int initTreeNumber = 2;
	//private RobotType 
	
	private int[] myTrees = new int[MAX_TREE_NUMBER];
	private int nextTreeToBuild = 0;
	
	private boolean allTreesExist = false;
	//private MapLocation settledPosition = null;
	private boolean settledPositionReached = false;
	
	
	private GardenerMessage message;
	
	private static final int CIRCLES_NUM = 6;
	private static final int MAX_CIRCLE_POINTS_NUM = 19; // target point - 9
	private static final int TARGET_POINT_INDEX = 9;
	private static final int PREV_POINTS_NUM = 4;
	
	private static float[][][] dists = new float[CIRCLES_NUM][MAX_CIRCLE_POINTS_NUM][PREV_POINTS_NUM];
	private static int[] circleSizes = new int[CIRCLES_NUM];
	private static float[] angles = new float[CIRCLES_NUM];
	
	private static final float outerCircleRadius = RobotType.GARDENER.sensorRadius-RobotType.GARDENER.bodyRadius;
	private static final float vertexDistance = outerCircleRadius / CIRCLES_NUM;
	
	private final float outerCircleGap;
	private final float outerCircleAngle;
	private final float targetPointAngle;
	
	
	public GardenerBehaviour(RobotController rc, RobotType ourType) throws GameActionException {
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

	private int[][] prevVertices = new int[CIRCLES_NUM][MAX_CIRCLE_POINTS_NUM];
	private float[][][] prevDists = new float[CIRCLES_NUM][MAX_CIRCLE_POINTS_NUM][PREV_POINTS_NUM];
	private float[][] vertexAccDist = new float[CIRCLES_NUM][MAX_CIRCLE_POINTS_NUM];
	
	
	private MapLocation localPosition = null;
	
	private Direction direction;
	
	
	private int numMoveAttempts = 0;
	private static final int MAX_NUM_MOVE_ATTEMPTS = 73; 

	public final float NEEDED_FARM_RADIUS = 2 * GameConstants.BULLET_TREE_RADIUS + RobotType.GARDENER.bodyRadius * 3;
	public final float GOOD_FARM_RADIUS = NEEDED_FARM_RADIUS + 0.3f;
	
	
	private float treeDirOffset = 0.0f;
	
	private MapLocation closestArchon = null;
	

	int treeBuiltTryCount = 0;
	
	@Override
	protected void turn() throws GameActionException {
		
		if (message == null && ourInfo.messageId != 0) {
			int messagePointer = ourInfo.messageId*MessageInfo.OBJECT_SIZE + MESSAGES_SEGMENT_START;
			message = new GardenerMessage(messagePointer, rc);
			rc.broadcastInt(messagePointer, 0);
			ourInfo.messageId = 0;
		}
		

		boolean treeBuilt = false;
		
		if (settledPositionReached) {
			
			if (!allTreesExist) {
				
				if (message == null || message.robotTypeCode == -1 || message.numInitialTrees > 0) {
					// build initial tree
					Direction dir = new Direction(nextTreeToBuild * PI / 3 + treeDirOffset);
					if (rc.canPlantTree(dir)) {
						// Build next tree in circle
						rc.plantTree(dir);
						treeBuilt = true;
						treeBuiltTryCount = 0;
						nextTreeToBuild++;
						if(nextTreeToBuild == MAX_TREE_NUMBER) {
							allTreesExist = true;
						}
						if (message != null) {
							--message.numInitialTrees;
						}
					} else {
						++treeBuiltTryCount;
						treeBuilt = false;
					}
				}
				
			}
			
			/*
			if (!treeBuilt) {
				float step = (float)Math.PI*2/30;
				if (message != null && message.robotTypeCode != -1) {
					RobotType newRobotType = AgentInfo.translateType(message.robotTypeCode);
					for (int i = 0; i < 30; i++) {
						Direction buildDir = new Direction(i * step);
						if (rc.canBuildRobot(newRobotType, buildDir)) {
							rc.buildRobot(newRobotType, buildDir);
							message.robotTypeCode = -1;
						}
					}
				} else {
					RobotType newRobotType = AgentInfo.translateType(AgentInfo.LUMBERJACK_CODE);
					for (int i = 0; i < 30; i++) {
						Direction buildDir = new Direction(i * step);
						if (rc.canBuildRobot(newRobotType, buildDir)) {
							rc.buildRobot(newRobotType, buildDir);
						}
					}
				}
			}
			*/
		} else {
			
			if (nextMovesPlan == null) {
				if (message != null && ourLocation.distanceTo(message.location) > ourType.sensorRadius) {
					direction = ourLocation.directionTo(message.location);
					nextMovesPlan = getPathPlan(direction, true);
				} else if (localPosition == null) {
					
					computePrevVertices(new Direction(PI/2), false);
					
					int best_i = -1;
					int best_j = -1;
					float lowestPenalty = Float.MAX_VALUE;
					float bestAngleGap = 0.0f;
					float bestAngle = 0.0f;
					

					float minClosArchon = Float.MAX_VALUE;
					
					for (int i = 0; i < CIRCLES_NUM; i++) {
						int circleSize = circleSizes[i];

						float angle = angles[i];
						float angleGap = (PI - (circleSize-1)*angle)/2;
						float currCircleRadius = vertexDistance*(i+1);
						
						float currPenalty;
						for (int j = 0; j < circleSize; j++) {

							float dist = vertexAccDist[i][j];
							if (dist < Float.MAX_VALUE) {

								float accAngle = angleGap + j*angle;
								float vertex_x = -(float)Math.cos(accAngle)*currCircleRadius;
								float vertex_y = (float)Math.sin(accAngle)*currCircleRadius;
								
								MapLocation thePlace = new MapLocation(ourLocation.x + vertex_x, ourLocation.y + vertex_y);
								
								float curOccupiedArea = 0.0f;
								
								for (TreeInfo tree : nearbyTrees) {
									float distToPlace = thePlace.distanceTo(tree.location);
									curOccupiedArea += ServiceStuff.getCircleIntersectArea(tree.radius, GOOD_FARM_RADIUS, distToPlace);
								}
								
								for (RobotInfo robot : nearbyRobots) {
									float distToPlace = thePlace.distanceTo(robot.location);
									curOccupiedArea += ServiceStuff.getCircleIntersectArea(robot.getRadius(), GOOD_FARM_RADIUS, distToPlace);
									if (robot.type == RobotType.ARCHON && ourLocation.distanceTo(robot.location) < minClosArchon) {
										closestArchon = robot.location;
									}
								}
								
								currPenalty = curOccupiedArea; //0.9f*curOccupiedArea + 0.1f*dist;
								if (currPenalty < lowestPenalty) {
									lowestPenalty = currPenalty;
									best_i = i;
									best_j = j;
									bestAngleGap = angleGap;
									bestAngle = angle;
								}
							}
							
						}
						
					}
					
					/*
					nextMovesPlan = new MapLocation[best_i + 1];
					
					float trueGoalAccAngle = bestAngleGap + best_j*bestAngle + angleOffset;
					float true_goal_x = ourLocation.x + (float)Math.cos(trueGoalAccAngle)* vertexDistance*(best_i+1);
					float true_goal_y = ourLocation.y + (float)Math.sin(trueGoalAccAngle)* vertexDistance*(best_i+1);
					nextMovesPlan[best_i] = new MapLocation(true_goal_x, true_goal_y);
					localPosition = nextMovesPlan[best_i];
					
					int currVertex = best_j;
					for (int i = best_i-1; i >= 0; --i) {

						currVertex = prevVertices[i+1][currVertex];
						
						float angle = angles[i];
						float angleGap = (PI - (circleSizes[i]-1)*angle)/2;
						float currCircleRadius = vertexDistance*(i+1);
						
						float accAngle = angleGap + currVertex*angle + angleOffset;
						float vertex_x = (float)Math.cos(accAngle)*currCircleRadius;
						float vertex_y = (float)Math.sin(accAngle)*currCircleRadius;
						nextMovesPlan[i] = new MapLocation(ourLocation.x + vertex_x, ourLocation.y + vertex_y);
					}
					*/
					
					computePrevVertices(new Direction(-PI/2), false);
					
					
					for (int i = 0; i < CIRCLES_NUM; i++) {
						int circleSize = circleSizes[i];

						float angle = angles[i];
						float angleGap = (PI - (circleSize-1)*angle)/2;
						float currCircleRadius = vertexDistance*(i+1);
						
						float currPenalty;
						for (int j = 0; j < circleSize; j++) {

							float dist = vertexAccDist[i][j];
							if (dist < Float.MAX_VALUE) {

								float accAngle = angleGap + j*angle;
								float vertex_x = -(float)Math.cos(accAngle)*currCircleRadius;
								float vertex_y = (float)Math.sin(accAngle)*currCircleRadius;
								
								MapLocation thePlace = new MapLocation(ourLocation.x + vertex_x, ourLocation.y + vertex_y);
								
								float curOccupiedArea = 0.0f;
								
								for (TreeInfo tree : nearbyTrees) {
									float distToPlace = thePlace.distanceTo(tree.location);
									curOccupiedArea += ServiceStuff.getCircleIntersectArea(tree.radius, GOOD_FARM_RADIUS, distToPlace);
								}
								
								for (RobotInfo robot : nearbyRobots) {
									float distToPlace = thePlace.distanceTo(robot.location);
									curOccupiedArea += ServiceStuff.getCircleIntersectArea(robot.getRadius(), GOOD_FARM_RADIUS, distToPlace);
									if (robot.type == RobotType.ARCHON && ourLocation.distanceTo(robot.location) < minClosArchon) {
										closestArchon = robot.location;
									}
								}
								
								currPenalty = curOccupiedArea; //0.9f*curOccupiedArea + 0.1f*dist;
								if (currPenalty < lowestPenalty) {
									lowestPenalty = currPenalty;
									best_i = i;
									best_j = j;
									bestAngleGap = angleGap;
									bestAngle = angle;
								}
							}
							
						}
						
					}
					
					if (best_j != -1 && best_i != -1) {
						
						nextMovesPlan = new MapLocation[best_i + 1];
						
						float trueGoalAccAngle = bestAngleGap + best_j*bestAngle + angleOffset;
						float true_goal_x = ourLocation.x + (float)Math.cos(trueGoalAccAngle)* vertexDistance*(best_i + 1);
						float true_goal_y = ourLocation.y + (float)Math.sin(trueGoalAccAngle)* vertexDistance*(best_i + 1);
						nextMovesPlan[best_i] = new MapLocation(true_goal_x, true_goal_y);
						localPosition = nextMovesPlan[best_i];
						
						int currVertex = best_j;
						for (int i = best_i-1; i >= 0; --i) {
							try {
								currVertex = prevVertices[i+1][currVertex];
							} catch (Exception e) {
								System.err.println("Gardener's shit: " + i+1 +  " " + currVertex);
								nextMovesPlan = null;
								break;
							}
							
							float angle = angles[i];
							float angleGap = (PI - (circleSizes[i]-1)*angle)/2;
							float currCircleRadius = vertexDistance*(i+1);
							
							float accAngle = angleGap + currVertex*angle + angleOffset;
							float vertex_x = (float)Math.cos(accAngle)*currCircleRadius;
							float vertex_y = (float)Math.sin(accAngle)*currCircleRadius;
							nextMovesPlan[i] = new MapLocation(ourLocation.x + vertex_x, ourLocation.y + vertex_y);
						}
					}
					
					
				} else {
					//settledPositionReached = true;
					if (++numMoveAttempts > MAX_NUM_MOVE_ATTEMPTS) {
						settledPositionReached = true;
					}
				}
			}
			
			
			if (!rc.hasMoved()) {
				
				boolean moved = false;
				
				if (nextMovesPlan != null) {
					MapLocation nextVertex = nextMovesPlan[nextMoveIndex];
					float dist = ourLocation.distanceTo(nextVertex);
					if (dist > RobotType.SCOUT.strideRadius) {
						dist = RobotType.SCOUT.strideRadius;
					} else {
						++nextMoveIndex;
					}
					
					direction = ourLocation.directionTo(nextVertex);
					if (rc.canMove(direction, dist)) {
						rc.move(direction, dist);
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
					
					if (closestArchon != null) {
						direction = new Direction(ourLocation.directionTo(closestArchon).radians + (float)Math.random());
					} else {
						direction = new Direction((float)Math.random() * 2 * (float)Math.PI);
					}
					
					if (rc.canMove(direction)) {
						rc.move(direction);
					}
					
					if (++numMoveAttempts > MAX_NUM_MOVE_ATTEMPTS) {
						settledPositionReached = true;
					}
					
				}
				
			}
			
		}
		
		
		if (!treeBuilt && treeBuiltTryCount > 3) {
			float step = (float)Math.PI*2/30;
			if (message != null && message.robotTypeCode != -1) {
				RobotType newRobotType = AgentInfo.translateType(message.robotTypeCode);
				for (int i = 0; i < 30; i++) {
					Direction buildDir = new Direction(i * step);
					if (rc.canBuildRobot(newRobotType, buildDir)) {
						rc.buildRobot(newRobotType, buildDir);
						message.robotTypeCode = -1;
					}
				}
			} else {
				
				int numLumb = rc.readBroadcast(LUMBERJACK_NUMBER_CHANNEL);
				if (numLumb < 5) {
					RobotType newRobotType = AgentInfo.translateType(AgentInfo.LUMBERJACK_CODE);
					for (int i = 0; i < 30; i++) {
						Direction buildDir = new Direction(i * step);
						if (rc.canBuildRobot(newRobotType, buildDir)) {
							rc.buildRobot(newRobotType, buildDir);
						}
					}
				}
			}
		}
		
		
		
		//rc.canmove
		
	}
	
	
	//boolean scoutCreated = false;
	
	@Override
	protected void tick() throws GameActionException {
		
		if (nextTreeToBuild > 0 && rc.canWater()) {
			// Water tree with minimum health
			int minId = -1;
			float minHealth = Float.MAX_VALUE;
			for (int i = 0; i < nearbyTrees.length; i++) {
				TreeInfo tree = nearbyTrees[i];
				if (tree.team == ourTeam) {
					if (tree.health < minHealth) {
						minHealth = tree.health;
						minId = tree.ID;
					}
				}
			}
			if (minId != -1 && rc.canWater(minId)) {
				rc.water(minId);
			}
		}
		
		/*
		
		if (rc.canWater()) {
			// Water tree with minimum health
			int minIdx = -1;
			float minHealth = Float.MAX_VALUE;
			for (int i = 0; i < nextTreeToBuild; i++) {
				int tree = myTrees[i];
				float health= rc.senseTree(tree).health;
				if (health < minHealth) {
					minHealth = health;
					minIdx = i;
				}
			}
			if (minIdx != -1) {
				rc.water(myTrees[minIdx]);
			}
		}
		
		if (allTreesExist) {
			
			Direction dir = new Direction((nextTreeToBuild+1) * PI / 3);
			if (!scoutCreated && rc.canBuildRobot(RobotType.SCOUT, dir)) {
				rc.buildRobot(RobotType.SCOUT, dir);
				scoutCreated = true;
			}
			
			return;
		}
		
		MapLocation ourPosition = rc.getLocation();
		if (settledPositionReached) {
			// If the gardener has no building cool-down
			Direction dir = new Direction(nextTreeToBuild * PI / 3);
			if (rc.canPlantTree(dir)) {
				// Build next tree in circle
				rc.plantTree(dir);
				myTrees[nextTreeToBuild] = rc.senseTreeAtLocation(ourPosition.add(dir, OUR_RADIUS+GameConstants.BULLET_TREE_RADIUS)).ID;
				nextTreeToBuild++;
				if(nextTreeToBuild == MAX_TREE_NUMBER) {
					allTreesExist = true;
				}
			}
			return;
		}
		
		
		if (settledPosition == null) {
			//RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
			//TreeInfo[] nearbyTrees = rc.senseNearbyTrees();
			float neededRadius = 2*GameConstants.BULLET_TREE_RADIUS + OUR_RADIUS;
			float samplingRadius = OUR_TYPE.sensorRadius-neededRadius;
			
			float direction;
			float length;
			for (int i = 0; i < POSITION_SAMPLE_NUMBER; i++) {
				
				length = rand.nextFloat()*samplingRadius;
				direction = rand.nextFloat()*2*PI;
				
				settledPosition = new MapLocation(ourPosition.x + length*(float)Math.cos(direction),
						ourPosition.y + length*(float)Math.sin(direction));
				
				if (rc.isCircleOccupiedExceptByThisRobot(settledPosition, neededRadius) || !rc.onTheMap(settledPosition)) {
					settledPosition = null;
					continue;
				}
				
				break;
				
				//boolean positionFound = false;
				//for (RobotInfo robot : nearbyRobots) {
				//	MapLocation loc = robot.getLocation();
				//	
				//}
				
			}
			
			if (settledPosition == null) {
				// no empty
				return;
			}
			
		}
		
		// If we are not yet in the settledPosition for planting trees
		if(!settledPositionReached && !rc.hasMoved()) {
			float distToSettled = ourPosition.distanceTo(settledPosition);
			if (!(distToSettled > OUR_TYPE.strideRadius)) {
				rc.move(ourPosition.directionTo(settledPosition), distToSettled);
				settledPositionReached = true;
			} else {
				rc.move(ourPosition.directionTo(settledPosition));
			}
			return;
		}
		
		*/
		
	}
	
	
	public static class GardenerMessage {
		
		public int numInitialTrees;		// 3 bits
		public int robotTypeCode;		// 3 bits
		public int urgency;				// 7 bits
		
		public MapLocation location;	// 2 floats
		
		//public static final int MAX_NUM_INIT_TREES = 5;
		//public static final int MAX_ROBOT_TYPE_CODE = 5;
		//public static final int MAX_URGENCY = 127;

		// ---- ---- ---- ---- ---U UUUU UURR RTTT
		private static final int INIT_TREES_MASK = 7;
		private static final int ROBOT_TYPE_MASK = (7 << 3);
		private static final int URGENCY_MASK = (127 << 6);
		
		
		public GardenerMessage(int pointer, RobotController rc) throws GameActionException{
			int compoundCode = rc.readBroadcastInt(pointer);
			numInitialTrees = (INIT_TREES_MASK & compoundCode);
			robotTypeCode = ((ROBOT_TYPE_MASK & compoundCode) >>> 3);
			urgency = ((URGENCY_MASK & compoundCode) >>> 6);
			location = new MapLocation(rc.readBroadcastFloat(pointer+1), rc.readBroadcastFloat(pointer+2));
		}
		
		public GardenerMessage() { }
		
		public void write(int pointer, RobotController rc) throws GameActionException {
			int compound = urgency;
			compound = ((compound << 3) | robotTypeCode);
			compound = ((compound << 3) | numInitialTrees);
			rc.broadcastInt(pointer, compound);
		}
		
		public void write(int pointer, RobotController rc, MapLocation location) throws GameActionException {
			int compound = urgency;
			compound = ((compound << 3) | robotTypeCode);
			compound = ((compound << 3) | numInitialTrees);
			rc.broadcastInt(pointer, compound);
			rc.broadcastFloat(pointer+1, location.x);
			rc.broadcastFloat(pointer+2, location.y);
		}

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
	
	private MapLocation[] getPathPlan(Direction destination, boolean penalizeDestDistance) throws GameActionException {
		
		computePrevVertices(destination, penalizeDestDistance);
		
		int outerCircleVertex = -1;
		if (penalizeDestDistance && vertexAccDist[CIRCLES_NUM-1][TARGET_POINT_INDEX] < Float.MAX_VALUE) {
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
	
	private float outerAngle;
	private float outerAngleGap;
	private float outerRadius;
	private float goalAccAngle;
	private float goal_x;
	private float goal_y;
	
	private void computePrevVertices(Direction destination, boolean penalizeDestDistance) throws GameActionException {
		
		prevVertices = new int[CIRCLES_NUM][MAX_CIRCLE_POINTS_NUM];
		prevDists = new float[CIRCLES_NUM][MAX_CIRCLE_POINTS_NUM][PREV_POINTS_NUM];
		vertexAccDist = new float[CIRCLES_NUM][MAX_CIRCLE_POINTS_NUM];
		
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

		outerAngle = angles[CIRCLES_NUM-1];
		outerAngleGap = (PI - (circleSizes[CIRCLES_NUM-1]-1)*outerAngle)/2;
		outerRadius = vertexDistance*CIRCLES_NUM;
		goalAccAngle = outerAngleGap + TARGET_POINT_INDEX*outerAngle;
		goal_x = -(float)Math.cos(goalAccAngle)*outerRadius;
		goal_y = (float)Math.sin(goalAccAngle)*outerRadius;
		
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
				if (i == CIRCLES_NUM-1 && penalizeDestDistance) {
					float x_diff = vertex_x-goal_x;
					float y_diff = vertex_y-goal_y;
					// add distance to the goal
					vertexAccDist[i][j] = minDist + (float)Math.sqrt(x_diff*x_diff + y_diff*y_diff);
					continue;
				}
				vertexAccDist[i][j] = minDist;
			}
			
		}
	}
	
	static Direction randomDirection() {
        return new Direction((float)Math.random() * 2 * (float)Math.PI);
    }
	
}
