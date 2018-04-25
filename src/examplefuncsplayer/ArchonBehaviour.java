package examplefuncsplayer;

import java.util.ArrayList;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TreeInfo;

public class ArchonBehaviour extends RobotBehaviour {

	final int NUM_CIRCLE_PARTS = 30;
	final float NEEDED_FARM_RADIUS = 2 * GameConstants.BULLET_TREE_RADIUS + RobotType.GARDENER.bodyRadius * 3;
	final float GOOD_FARM_RADIUS = NEEDED_FARM_RADIUS + 1;
	
	public ArchonBehaviour(RobotController rc, RobotType ourType) throws GameActionException {
		super(rc, ourType);
	}

	private boolean gardenerBuilt = false;
	
	private ArrayList<AgentInfo> archons;
	private ArrayList<AgentInfo> gardeners;
	private ArrayList<AgentInfo> soldiers;
	private ArrayList<AgentInfo> tanks;
	private ArrayList<AgentInfo> scouts;
	private ArrayList<AgentInfo> lumberjacks;
	
	//MAP_INITIALIZED_CHANNEL
	
	private int lastId;
	private AgentInfo agent;
	
	private int lastBuildStep = -1000;
	
	private int numCloseRobots;
	final static int NUM_CLOSE_ROBOTS_THRESHOLD = 2;
	final static float CLOSE_DIST_THRESHOLD = RobotType.ARCHON.bodyRadius + 1;
	
	final static float MONEY_FOR_VICTORY_POINTS_PURCHASE = 500;
	
	@Override
	protected void turn() throws GameActionException {
		
		// Read all the positions of the team, read the map
		
		archons = new ArrayList<AgentInfo>();
		archons.add(ourInfo);
		gardeners = new ArrayList<AgentInfo>();
		soldiers = new ArrayList<AgentInfo>();
		tanks = new ArrayList<AgentInfo>();
		scouts = new ArrayList<AgentInfo>();
		lumberjacks = new ArrayList<AgentInfo>();
		
		lastId = rc.readBroadcastInt(ROBOTS_LAST_ID_CHANNEL);
		if (lastId >= MAX_ALIVE_ROBOTS) {
			lastId = MAX_ALIVE_ROBOTS-1;
		}
		
		numCloseRobots = 0;
		for (int i = 0; i <= lastId; i++) {
			int compoundCode = rc.readBroadcastInt(i*AgentInfo.OBJECT_SIZE);
			if (compoundCode == 0) {
				continue;
			}
			agent = AgentInfo.createSimple(compoundCode);
			if (agent.statusCode == AgentInfo.DEAD) {
				continue;
			}
			
			if (agent.statusCode == AgentInfo.CHECKED) {
				agent.statusCode = AgentInfo.DEAD;
				
				int typeChannel = -1;
				switch (agent.typeCode) {
					case AgentInfo.ARCHON_CODE:
						typeChannel = ARCHON_NUMBER_CHANNEL;
						break;
					case AgentInfo.GARDENER_CODE:
						typeChannel = GARDENER_NUMBER_CHANNEL;
						break;
					case AgentInfo.SOLDIER_CODE:
						typeChannel = SOLDIER_NUMBER_CHANNEL;
						break;
					case AgentInfo.TANK_CODE:
						typeChannel = TANK_NUMBER_CHANNEL;
						break;
					case AgentInfo.SCOUT_CODE:
						typeChannel = SCOUT_NUMBER_CHANNEL;
						break;
					default:
						typeChannel = LUMBERJACK_NUMBER_CHANNEL;
						break;
				}

				// Increase count of your class so that class percentages can be measured afterwards
				int currTypeNumber = rc.readBroadcastInt(typeChannel);
				rc.broadcastInt(typeChannel, currTypeNumber+1);
				
			} else {
				switch (agent.typeCode) {
					case AgentInfo.ARCHON_CODE:
						archons.add(agent);
						break;
					case AgentInfo.GARDENER_CODE:
						gardeners.add(agent);
						break;
					case AgentInfo.SOLDIER_CODE:
						soldiers.add(agent);
						break;
					case AgentInfo.TANK_CODE:
						tanks.add(agent);
						break;
					case AgentInfo.SCOUT_CODE:
						scouts.add(agent);
						break;
					default:
						lumberjacks.add(agent);
						break;
				}
				agent.statusCode = AgentInfo.CHECKED;
				agent.initLocation(i*AgentInfo.OBJECT_SIZE, rc);
				if (ourLocation.distanceTo(agent.location) < CLOSE_DIST_THRESHOLD) {
					++numCloseRobots;
				}
			}
			
			AgentInfo.write(i*AgentInfo.OBJECT_SIZE, rc, agent);
		}
		
		// Calculate the proportion of types of agents
		
		updateBorders();
		
		if (!mapInitialized) {
			if (minX > Float.MIN_VALUE && minY > Float.MIN_VALUE) {
				mapInitialized = true;
				rc.broadcastBoolean(MAP_INITIALIZED_CHANNEL, true);
			} else {
				mapInitialized = rc.readBroadcastBoolean(MAP_INITIALIZED_CHANNEL);
				if (!mapInitialized) {
					minX = rc.readBroadcastFloat(MIN_X_CHANNEL);
					if (minX > Float.MIN_VALUE) {
						minY = rc.readBroadcastFloat(MIN_Y_CHANNEL);
						if (minY > Float.MIN_VALUE) {
							//float squareSideX = (maxX-minX)/MAP_SIDE_SQUARES_NUM;
							//float squareSideY = (maxY-minY)/MAP_SIDE_SQUARES_NUM;
							mapInitialized = true;
							rc.broadcastBoolean(MAP_INITIALIZED_CHANNEL, true);
							maxX = rc.readBroadcastFloat(MAX_X_CHANNEL);
							maxY = rc.readBroadcastFloat(MAX_Y_CHANNEL);
						}
					}
				}
			}
		}
		
		ourInfo.opponentsNum = updateMap();
		
		if (currRound-lastBuildStep > 30 && numCloseRobots <= NUM_CLOSE_ROBOTS_THRESHOLD && (gardeners.size() < 2 || lumberjacks.size() > 2)  && rc.getBuildCooldownTurns() == 0) {
			float step = (float)Math.PI*2/NUM_CIRCLE_PARTS;
			float minOccupiedArea = Float.MAX_VALUE;
			float curOccupiedArea; // utility
			MapLocation farmCenter;
			float farmingDistance = NEEDED_FARM_RADIUS + FLOAT_TOLERANCE + ourType.bodyRadius;
			float minDirection = 0.0f;
			
			float bestFarmingDistance = farmingDistance;
			

			for (float farmDist = farmingDistance; farmDist < MAX_GARDEN_PLANNING_DIST; farmDist += PLANNING_DIST_STEP) {
				
				// add penalty for distance
				
				for (int i = 0; i < NUM_CIRCLE_PARTS; i++) {
					
					curOccupiedArea = 0.0f;
					float dist;
					if (farmDist < ourType.sensorRadius) {
						for (TreeInfo tree : nearbyTrees) {
							farmCenter =  ourLocation.add(i * step, farmDist);
							dist = farmCenter.distanceTo(tree.location);
							curOccupiedArea += ServiceStuff.getCircleIntersectArea(tree.radius, GOOD_FARM_RADIUS + FLOAT_TOLERANCE, dist);
						}
					} // otherwise: TODO: read the map from broadcast
					
					for (AgentInfo gardener : gardeners) {
						// if gardener is inside the circle that we consider -- add penalty
						farmCenter =  ourLocation.add(i * step, farmDist);
						dist = farmCenter.distanceTo(gardener.location);
						curOccupiedArea += ServiceStuff.getCircleIntersectArea(RobotType.GARDENER.bodyRadius, GOOD_FARM_RADIUS + FLOAT_TOLERANCE, dist);
					}
					
					for (AgentInfo archon : archons) {
						// if archon is inside the circle that we consider -- add penalty
						farmCenter =  ourLocation.add(i * step, farmDist);
						dist = farmCenter.distanceTo(archon.location);
						curOccupiedArea += ServiceStuff.getCircleIntersectArea(RobotType.ARCHON.bodyRadius, GOOD_FARM_RADIUS + FLOAT_TOLERANCE, dist);
					}
					
					curOccupiedArea += farmDist*DISTANCE_FROM_BASE_PENALTY_FACTOR;
					
					if (curOccupiedArea < minOccupiedArea) {
						minOccupiedArea = curOccupiedArea;
						minDirection = i * step;
						bestFarmingDistance = farmDist;
					}
				}
				
				if (minOccupiedArea < PERFECT_OCCUPIED_AREA_THRESHOLD) {
					break;
				}
				
			}
			
			
			for (int i = 0; i < NUM_CIRCLE_PARTS; i++) {
				Direction direction = new Direction(i * step);
				if (rc.canBuildRobot(RobotType.GARDENER, direction)) {
					
					GardenerBehaviour.GardenerMessage message = new GardenerBehaviour.GardenerMessage();
					message.location = ourLocation.add(new Direction(minDirection), bestFarmingDistance);
					message.numInitialTrees = 2;
					if (scouts.size() == 0) {
						message.robotTypeCode = AgentInfo.SCOUT_CODE;
						message.urgency = 1;
					}
					
					spawnGardener(direction, message);
					gardenerBuilt = true;
					break;
				}
			}
			
		}
		
		float ourBullets = rc.getTeamBullets();
		if (ourBullets > MONEY_FOR_VICTORY_POINTS_PURCHASE) {
			float costPerOne = rc.getVictoryPointCost();
			int numToBuy = (int) ((ourBullets - MONEY_FOR_VICTORY_POINTS_PURCHASE)/costPerOne);
			rc.donate(numToBuy*costPerOne);
		}
		
	}
	
	public final float MAX_GARDEN_PLANNING_DIST = 70.0f;
	public final float PLANNING_DIST_STEP = 10.0f;
	public final float PERFECT_OCCUPIED_AREA_THRESHOLD = 10.0f + NEEDED_FARM_RADIUS + ourType.bodyRadius;
	public final float DISTANCE_FROM_BASE_PENALTY_FACTOR = 0.3f;
	
	@Override
	protected void tick() throws GameActionException {
		
	}

	private void spawnGardener(Direction direction, GardenerBehaviour.GardenerMessage message) throws GameActionException {
		
		int compound = 1;
		int i = 0;
		while (compound != 0) {
			++i;
			compound = rc.readBroadcastInt(MESSAGES_SEGMENT_START + i*MessageInfo.OBJECT_SIZE);
		}
		
		int pointer = MESSAGES_SEGMENT_START + i*MessageInfo.OBJECT_SIZE;
		message.write(pointer, rc, message.location);
		
		int newId = rc.readBroadcastInt(ROBOTS_LAST_FUTURE_ID_CHANNEL);
		rc.broadcastInt(ROBOTS_LAST_FUTURE_ID_CHANNEL, newId + 1);
		AgentInfo.write(newId*AgentInfo.OBJECT_SIZE, rc, AgentInfo.GARDENER_CODE, AgentInfo.ALIVE,
				AgentInfo.MAX_HEALTH_LEVEL, i, 0);
		
		try {
			rc.buildRobot(RobotType.GARDENER, direction);
			lastBuildStep = currRound;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	

	
	private float squareSideX;
	private float squareSideY;
	private float squareArea;
	
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
		
		squareSideX = (maxX-minX)/MAP_SIDE_SQUARES_NUM;
		squareSideY = (maxY-minY)/MAP_SIDE_SQUARES_NUM;
		squareArea = squareSideX*squareSideY;
		
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
				/*if (delta_x*delta_x + delta_y*delta_y < sensorRadius*sensorRadius) {
					//ourInfo.location
					//innerVertices[i][j]
				}*/
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
				}
			}
		}
		
		int square_i;
		int square_j;
		
		// Loop among the trees, accumulating the info
		for (TreeInfo tree : nearbyTrees) {
			// Get row and column of tree's square in square matrix
			square_i = (int) ((tree.location.y - minY) / squareSideY) - min_i;
			if (square_i < 0 || square_i >= squareRows) {
				continue;
			}
			square_j = (int) ((tree.location.x - minX) / squareSideX) - min_j;
			if (square_j < 0 || square_j >= squareCols) {
				continue;
			}
			
			square = squares[square_i][square_j];
			if (square == null) {
				continue;
			}
			
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
		
		int result = 0;
		for (RobotInfo robot : nearbyRobots) {
			square_i = (int) ((robot.location.y - minY) / squareSideY) - min_i;
			if (square_i < 0 || square_i >= squareRows) {
				continue;
			}
			square_j = (int) ((robot.location.x - minX) / squareSideX) - min_j;
			if (square_j < 0 || square_j >= squareCols) {
				continue;
			}
			square = squares[square_i][square_j];
			if (square == null) {
				continue;
			}
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
	
	
	
}
