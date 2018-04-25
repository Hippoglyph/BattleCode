package examplefuncsplayer;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TreeInfo;


public abstract class RobotBehaviour {
	
	public static final float PI = (float)Math.PI;
	
	// ============ Special info segment ==================
	
	public static final int SPECIAL_INFO_SEGMENT_START = 9969;

	public static final int ROBOTS_LAST_ID_CHANNEL = SPECIAL_INFO_SEGMENT_START;
	//public static final int FIRST_POINTER_CHANNEL = SPECIAL_INFO_SEGMENT_START+1;
	//public static final int LAST_POINTER_CHANNEL = SPECIAL_INFO_SEGMENT_START+2;

	public static final int ARCHON_NUMBER_CHANNEL = SPECIAL_INFO_SEGMENT_START+3;
	public static final int GARDENER_NUMBER_CHANNEL = SPECIAL_INFO_SEGMENT_START+4;
	public static final int SOLDIER_NUMBER_CHANNEL = SPECIAL_INFO_SEGMENT_START+5;
	public static final int TANK_NUMBER_CHANNEL = SPECIAL_INFO_SEGMENT_START+6;
	public static final int SCOUT_NUMBER_CHANNEL = SPECIAL_INFO_SEGMENT_START+7;
	public static final int LUMBERJACK_NUMBER_CHANNEL = SPECIAL_INFO_SEGMENT_START+8;
	public static final int ROBOTS_LAST_FUTURE_ID_CHANNEL = SPECIAL_INFO_SEGMENT_START+9;
	
	public static final int MIN_X_CHANNEL = SPECIAL_INFO_SEGMENT_START+10;
	public static final int MAX_X_CHANNEL = SPECIAL_INFO_SEGMENT_START+11;
	public static final int MIN_Y_CHANNEL = SPECIAL_INFO_SEGMENT_START+12;
	public static final int MAX_Y_CHANNEL = SPECIAL_INFO_SEGMENT_START+13;
	public static final int SYMMETRY_CHANNEL = SPECIAL_INFO_SEGMENT_START+14; // 0 - horizontal, 1 - vertical, 2 - rotational
	public static final int MIDDLE_POINT_X_CHANNEL = SPECIAL_INFO_SEGMENT_START+15;
	public static final int MIDDLE_POINT_Y_CHANNEL = SPECIAL_INFO_SEGMENT_START+16;
	public static final int THEIR_ARCHONS_CENTER_X_CHANNEL = SPECIAL_INFO_SEGMENT_START+17;
	public static final int THEIR_ARCHONS_CENTER_Y_CHANNEL = SPECIAL_INFO_SEGMENT_START+18;
	public static final int MAP_INITIALIZED_CHANNEL = SPECIAL_INFO_SEGMENT_START+19;
	
	// ====================================================
	
	
	public static final int ROBOTS_INFO_SEGMENT_START = 0;
	public static final int MAX_ALIVE_ROBOTS = 500;
	
	public static final int MAX_MESSAGES_NUM = 255;
	public static final int MESSAGES_SEGMENT_START = SPECIAL_INFO_SEGMENT_START - (MAX_MESSAGES_NUM*MessageInfo.OBJECT_SIZE);
	
	public static final int MAP_INFO_SEGMENT_START = (ROBOTS_INFO_SEGMENT_START + MAX_ALIVE_ROBOTS*(AgentInfo.OBJECT_SIZE));
	// ~7480
	// 50*50=2500 squares => 3 broadcast cells per square
	public static final int MAP_INFO_SEGMENT_LENGTH = (MESSAGES_SEGMENT_START - MAP_INFO_SEGMENT_START);
	
	public static final int MAP_SIDE_SQUARES_NUM = 14; // So, the map is 14x14
	
	
	public static final double TOLERANCE = 1e-9;
	public static final float FLOAT_TOLERANCE = 1e-7f;
	
	
	protected RobotController rc;
	protected int myID;
	//protected int lastPointer = 0;
	//protected int firstPointer = 0;
	protected final RobotType ourType;
	protected final int ourTypeCode;
	protected float ourHealth;
	protected int messageId = 0;
	protected int opponentsNum = 0;
	protected Team ourTeam; 
	protected Team opponentTeam;
	protected AgentInfo ourInfo = null;
	
	
	protected int symmetryType; // 0 - horizontal, 1 - vertical, 2 - rotational
	protected MapLocation middlePoint;
	protected MapLocation theirArchonsCenter;
	protected MapLocation ourArchonsCenter;
	
	MapLocation[] ourArchons = null;
	MapLocation[] theirArchons = null;

	protected boolean mapInitialized = false;
	
	protected int myPointer;
	
	protected MapLocation ourLocation;
	
	
	public RobotBehaviour(RobotController rc, RobotType ourType) throws GameActionException {
		
		this.rc = rc;
		this.ourType = ourType;
		ourTeam = rc.getTeam();
		opponentTeam = ourTeam.opponent();
		ourLocation = rc.getLocation();
		int typeNumberChannel;
		switch (ourType) {
			case ARCHON:
				ourTypeCode = AgentInfo.ARCHON_CODE;
				typeNumberChannel = ARCHON_NUMBER_CHANNEL;
				break;
			case GARDENER:
				ourTypeCode = AgentInfo.GARDENER_CODE;
				typeNumberChannel = GARDENER_NUMBER_CHANNEL;
				break;
			case SOLDIER:
				ourTypeCode = AgentInfo.SOLDIER_CODE;
				typeNumberChannel = SOLDIER_NUMBER_CHANNEL;
				break;
			case TANK:
				ourTypeCode = AgentInfo.TANK_CODE;
				typeNumberChannel = TANK_NUMBER_CHANNEL;
				break;
			case SCOUT:
				ourTypeCode = AgentInfo.SCOUT_CODE;
				typeNumberChannel = SCOUT_NUMBER_CHANNEL;
				break;
			default:
				ourTypeCode = AgentInfo.LUMBERJACK_CODE;
				typeNumberChannel = LUMBERJACK_NUMBER_CHANNEL;
				break;
		}

		myID = rc.readBroadcastInt(ROBOTS_LAST_ID_CHANNEL);
		myPointer = myID*AgentInfo.OBJECT_SIZE;
		
		boolean createdManually = false;
		int compoundCode = rc.readBroadcastInt(myPointer);
		if (compoundCode != 0) {
			ourInfo = AgentInfo.createSimple(compoundCode);
			if (ourInfo.typeCode == ourTypeCode) {
				createdManually = true;
				rc.broadcast(ROBOTS_LAST_ID_CHANNEL, myID+1);
			} else {
				myID = rc.readBroadcastInt(ROBOTS_LAST_FUTURE_ID_CHANNEL);
				rc.broadcast(ROBOTS_LAST_FUTURE_ID_CHANNEL, myID+1);
			}
		} else {
			rc.broadcast(ROBOTS_LAST_ID_CHANNEL, myID+1);
			rc.broadcast(ROBOTS_LAST_FUTURE_ID_CHANNEL, myID+1);
		}
		
		//rc.broadcast(ROBOTS_LAST_FUTURE_ID_CHANNEL, myID+1);
		
		ourArchons = rc.getInitialArchonLocations(ourTeam);
		float ourCenterX = 0.0f;
		float ourCenterY = 0.0f;
		for (MapLocation ourArchon : ourArchons) {
			ourCenterX += ourArchon.x;
			ourCenterY += ourArchon.y;
		}
		ourCenterX /= ourArchons.length;
		ourCenterY /= ourArchons.length;
		ourArchonsCenter = new MapLocation(ourCenterX, ourCenterY);
		
		if (myID >= MAX_ALIVE_ROBOTS) {
			int cellId = myID%MAX_ALIVE_ROBOTS;
			// find the first cell on the right that has myID
			// read the message
			// update the cell with our true information
			throw new UnsupportedOperationException();
		} else {
			if (!createdManually) {
				if (myID == 0) {
					// Initialize the map range
					rc.broadcastFloat(MIN_X_CHANNEL, Float.MIN_VALUE);
					rc.broadcastFloat(MAX_X_CHANNEL, Float.MAX_VALUE);
					rc.broadcastFloat(MIN_Y_CHANNEL, Float.MIN_VALUE);
					rc.broadcastFloat(MAX_Y_CHANNEL, Float.MAX_VALUE);
					
					theirArchons = rc.getInitialArchonLocations(opponentTeam);
					float theirCenterX = 0.0f;
					float theirCenterY = 0.0f;
					for (MapLocation theirArchon : theirArchons) {
						theirCenterX += theirArchon.x;
						theirCenterY += theirArchon.y;
					}
					theirCenterX /= theirArchons.length;
					theirCenterY /= theirArchons.length;
					theirArchonsCenter = new MapLocation(theirCenterX, theirCenterY);
					
					rc.broadcastFloat(THEIR_ARCHONS_CENTER_X_CHANNEL, theirArchonsCenter.x);
					rc.broadcastFloat(THEIR_ARCHONS_CENTER_Y_CHANNEL, theirArchonsCenter.y);
					
					if (Math.abs(ourCenterX - theirArchonsCenter.x) < TOLERANCE) {
						symmetryType = 0; // horizontal
					} else if (Math.abs(ourCenterY - theirArchonsCenter.y) < TOLERANCE) {
						symmetryType = 1; // vertical
					} else {
						symmetryType = 2; // rotational
					}

					rc.broadcastInt(SYMMETRY_CHANNEL, symmetryType);
					middlePoint = new MapLocation((theirArchonsCenter.x+ourCenterX)/2, ((theirArchonsCenter.y+ourCenterY)/2));
					rc.broadcastFloat(MIDDLE_POINT_X_CHANNEL, middlePoint.x);
					rc.broadcastFloat(MIDDLE_POINT_Y_CHANNEL, middlePoint.y);
					
				} else {
					//lastPointer = rc.readBroadcastInt(LAST_POINTER_CHANNEL);
					// Updating the head's pointer
					//firstPointer = rc.readBroadcastInt(FIRST_POINTER_CHANNEL);
					//AgentInfo head = AgentInfo.createSimple(firstPointer*AgentInfo.OBJECT_SIZE, rc);
					//head.leftPointer = myID;
					//AgentInfo.write(firstPointer*AgentInfo.OBJECT_SIZE, rc, head);
					// Update the last pointer
					//rc.broadcast(LAST_POINTER_CHANNEL, myID);
					
					symmetryType = rc.readBroadcastInt(SYMMETRY_CHANNEL);
					middlePoint = new MapLocation(rc.readBroadcastFloat(MIDDLE_POINT_X_CHANNEL), rc.readBroadcastFloat(MIDDLE_POINT_Y_CHANNEL));
					theirArchonsCenter = new MapLocation(rc.readBroadcastFloat(THEIR_ARCHONS_CENTER_X_CHANNEL),
							rc.readBroadcastFloat(THEIR_ARCHONS_CENTER_Y_CHANNEL));
				}
				
				AgentInfo.write(myPointer, rc, ourTypeCode, AgentInfo.ALIVE, AgentInfo.MAX_HEALTH_LEVEL, messageId, opponentsNum, ourLocation);
				
			} else {
				ourInfo = AgentInfo.createSimple(compoundCode);
				symmetryType = rc.readBroadcastInt(SYMMETRY_CHANNEL);
				middlePoint = new MapLocation(rc.readBroadcastFloat(MIDDLE_POINT_X_CHANNEL), rc.readBroadcastFloat(MIDDLE_POINT_Y_CHANNEL));
				theirArchonsCenter = new MapLocation(rc.readBroadcastFloat(THEIR_ARCHONS_CENTER_X_CHANNEL),
						rc.readBroadcastFloat(THEIR_ARCHONS_CENTER_Y_CHANNEL));
			}
		}
		
		// Increase count of your class so that class percentages can be measured afterwards
		int currTypeNumber = rc.readBroadcastInt(typeNumberChannel);
		rc.broadcastInt(typeNumberChannel, currTypeNumber+1);
		
	}
	
	protected abstract void tick() throws GameActionException;
	
	protected void turn() throws GameActionException { }
	
	
	//protected int prevBytecodeNum = 0;
	//protected AgentInfo prevGuy;
	protected RobotInfo[] nearbyRobots;
	protected TreeInfo[] nearbyTrees;
	protected int prevRound;
	protected int currRound = 0;
	
	public final void execute() throws GameActionException {
		
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
			//prevBytecodeNum = Clock.getBytecodeNum();
			
			
			// Agent-dependent stuff (should not take more than bytecode limit)
			tick();
		}
		
	}
	
	
	public static class AgentInfo {
		
		public static final int OBJECT_SIZE = 3;

		public final int typeCode;	// 3 bit
		public int statusCode;		// 2 bits
		//public int leftPointer;		// 9 bits
		public int healthLevel;		// 4 bits
		public int messageId;		// 8 bits
		public int opponentsNum;	// 6 bits
		
		public MapLocation location; // 8 bytes
		
		public static final int MAX_HEALTH_LEVEL = 15;
		public static final int MAX_OPPONENTS = 63;
		// OOOO OOMM  MMMM MMHH  HHPP PPPP  PPPS STTT
		private static final int TYPE_MASK = 7;
		private static final int STATUS_MASK = (3 << 3);
		private static final int POINTER_MASK = (511 << 5);
		private static final int HEALTH_MASK = (MAX_HEALTH_LEVEL << 14);
		private static final int MESSAGE_MASK = (255 << 18);
		private static final int OPPONENTS_MASK = (63 << 26);
		

		// Robot status codes
		public static final int ALIVE = 0;
		public static final int CHECKED = 1;
		public static final int DEAD = 2;
		//public static final int HEAVY_COMPUTATION = 3;
		
		// Robot type codes
		public static final int ARCHON_CODE = 0;
		public static final int GARDENER_CODE = 1;
		public static final int SOLDIER_CODE = 2;
		public static final int TANK_CODE = 3;
		public static final int SCOUT_CODE = 4;
		public static final int LUMBERJACK_CODE = 5;
		
		public AgentInfo(int pointer, RobotController rc) throws GameActionException {
			int compoundCode = rc.readBroadcastInt(pointer);
			typeCode = (TYPE_MASK & compoundCode);
			statusCode = ((STATUS_MASK & compoundCode) >>> 3);
			//leftPointer = ((POINTER_MASK & compoundCode) >>> 5);
			healthLevel = ((HEALTH_MASK & compoundCode) >>> 14);
			messageId = ((MESSAGE_MASK & compoundCode) >>> 18);
			opponentsNum = ((OPPONENTS_MASK & compoundCode) >>> 26);
			location = new MapLocation(rc.readBroadcastFloat(pointer+1), rc.readBroadcastFloat(pointer+2));
		}
		
		public AgentInfo(int typeCode, int statusCode, int healthLevel, int messageId, int opponentsNum) throws GameActionException {
			this.typeCode = typeCode;
			this.statusCode = statusCode;
			//this.leftPointer = leftPointer;
			this.healthLevel = healthLevel;
			this.messageId = messageId;
			this.opponentsNum = opponentsNum;
		}
		
		
		public static AgentInfo createSimple(int pointer, RobotController rc) throws GameActionException {
			int compoundCode = rc.readBroadcastInt(pointer);
			return new AgentInfo((TYPE_MASK & compoundCode),
					((STATUS_MASK & compoundCode) >>> 3),
					//((POINTER_MASK & compoundCode) >>> 5),
					((HEALTH_MASK & compoundCode) >>> 14),
					((MESSAGE_MASK & compoundCode) >>> 18),
					((OPPONENTS_MASK & compoundCode) >>> 26));
		}
		
		public static AgentInfo createSimple(int compoundCode) throws GameActionException {
			return new AgentInfo((TYPE_MASK & compoundCode),
					((STATUS_MASK & compoundCode) >>> 3),
					//((POINTER_MASK & compoundCode) >>> 5),
					((HEALTH_MASK & compoundCode) >>> 14),
					((MESSAGE_MASK & compoundCode) >>> 18),
					((OPPONENTS_MASK & compoundCode) >>> 26));
		}
		
		public void initLocation(int pointer, RobotController rc) throws GameActionException {
			location = new MapLocation(rc.readBroadcastFloat(pointer+1), rc.readBroadcastFloat(pointer+2));
		}
		
		public static void write(int pointer, RobotController rc,
				int typeCode, int statusCode, int healthLevel, int messageId, int opponentsNum) throws GameActionException {
			int compound =  opponentsNum;
			if (compound > MAX_OPPONENTS) {
				compound = MAX_OPPONENTS;
			}
			compound = ((compound << 8) | messageId);
			compound = ((compound << 4) | healthLevel);
			compound = ((compound << 9) | 0);
			compound = ((compound << 2) | statusCode);
			compound = ((compound << 3) | typeCode);
			rc.broadcastInt(pointer, compound);
		}
		
		public static void write(int pointer, RobotController rc,
				int typeCode, int statusCode, int healthLevel, int messageId, int opponentsNum, MapLocation location)
						throws GameActionException {
			int compound =  opponentsNum;
			if (compound > MAX_OPPONENTS) {
				compound = MAX_OPPONENTS;
			}
			compound = ((compound << 8) | messageId);
			compound = ((compound << 4) | healthLevel);
			compound = ((compound << 9) | 0);
			compound = ((compound << 2) | statusCode);
			compound = ((compound << 3) | typeCode);
			rc.broadcastInt(pointer, compound);
			rc.broadcastFloat(pointer+1, location.x);
			rc.broadcastFloat(pointer+2, location.y);
		}
		
		public static void write(int pointer, RobotController rc,
				AgentInfo agentInfo) throws GameActionException {
			int compound =  agentInfo.opponentsNum;
			if (compound > MAX_OPPONENTS) {
				compound = MAX_OPPONENTS;
			}
			compound = ((compound << 8) | agentInfo.messageId);
			compound = ((compound << 4) | agentInfo.healthLevel);
			compound = ((compound << 9) | 0);
			compound = ((compound << 2) | agentInfo.statusCode);
			compound = ((compound << 3) | agentInfo.typeCode);
			rc.broadcastInt(pointer, compound);
		}
		
		public static void write(int pointer, RobotController rc,
				AgentInfo agentInfo, MapLocation location) throws GameActionException {
			int compound =  agentInfo.opponentsNum;
			if (compound > MAX_OPPONENTS) {
				compound = MAX_OPPONENTS;
			}
			compound = ((compound << 8) | agentInfo.messageId);
			compound = ((compound << 4) | agentInfo.healthLevel);
			compound = ((compound << 9) | 0);
			compound = ((compound << 2) | agentInfo.statusCode);
			compound = ((compound << 3) | agentInfo.typeCode);
			rc.broadcastInt(pointer, compound);
			rc.broadcastFloat(pointer+1, location.x);
			rc.broadcastFloat(pointer+2, location.y);
		}
		
		
		public static RobotType translateType(int typeCode) {
			switch (typeCode) {
				case ARCHON_CODE:
					return RobotType.ARCHON;
				case GARDENER_CODE:
					return RobotType.GARDENER;
				case SOLDIER_CODE:
					return RobotType.SOLDIER;
				case TANK_CODE:
					return RobotType.TANK;
				case SCOUT_CODE:
					return RobotType.SCOUT;
				default:
					return RobotType.LUMBERJACK;
			}
		}
		
	}
	
	public static class MessageInfo {
		public static final int OBJECT_SIZE = 3;
		
		public int flags;
		public float x;
		public float y;
	}
	
	class MapSquareInfo {
		
		public static final float ARCHONS_COEF = 3.0f;
		public static final float TANKS_COEF = 2.0f;
		public static final float SOLDIERS_COEF = 1.0f;
		public static final float LUMBERJACKS_COEF = 1.0f;
		public static final float GARDENERS_COEF = 1.0f;
		public static final float SCOUTS_COEF = 0.7f;
		
		public static final float SOLDIER_HEALTH_COST = 2.0f;
		public static final float TANK_HEALTH_COST = 2.0f;
		public static final float LUMBERJACK_HEALTH_COST = 1.0f;
		public static final float SCOUT_HEALTH_COST = 0.3f;
		
		// 6 bits
		public float prizeUnitsUtility; // num_archons*3 + num_tanks*2 + num_soldiers + num_lumberjacks + num_gardeners + 0.5*num_scouts
		public float occupiedAreaPercent;	// 7 bits
		public int numOppTrees;			// 4 bits
		public int numOppGardeners;		// 3 bits
		public int numOppArchons;		// 2 bits
		public float oppFightersHealth;	// 10 bits
		

		public static final int MAX_PRIZE_UNITS = 63;
		public static final int MAX_OPP_TREES = 15;
		public static final int MAX_OPP_GARDENERS = 7;
		public static final int MAX_OPP_ARCHONS = 3;
		public static final int MAX_OPP_FIGHT_HEALTH = 1023;
		
		// FFFF FFFF FFAA GGGT TTTA AAAA AAUU UUUU
		private static final int PRIZE_UNITS_MASK = MAX_PRIZE_UNITS;
		private static final int OCCUPIED_AREA_MASK = (127 << 6);
		private static final int OPP_TREES_MASK = (15 << 13);
		private static final int OPP_GARDENERS_MASK = (7 << 17);
		private static final int OPP_ARCHONS_MASK = (3 << 20);
		private static final int OPP_FIGHT_MASK = (1023 << 22);
		
		public MapSquareInfo(int pointer, RobotController rc) throws GameActionException{
			int compoundCode = rc.readBroadcastInt(pointer);
			prizeUnitsUtility = (PRIZE_UNITS_MASK & compoundCode);
			occupiedAreaPercent = ((OCCUPIED_AREA_MASK & compoundCode) >>> 6);
			numOppTrees = ((OPP_TREES_MASK & compoundCode) >>> 13);
			numOppGardeners = ((OPP_GARDENERS_MASK & compoundCode) >>> 17);
			numOppArchons = ((OPP_ARCHONS_MASK & compoundCode) >>> 20);
			oppFightersHealth = ((OPP_FIGHT_MASK & compoundCode) >>> 22);
		}
		
		public MapSquareInfo() { }
		
		
		public void write(int pointer, RobotController rc) throws GameActionException {
			
			if (oppFightersHealth > MAX_OPP_FIGHT_HEALTH) {
				oppFightersHealth = MAX_OPP_FIGHT_HEALTH;
			}
			if (numOppArchons > MAX_OPP_ARCHONS) {
				numOppArchons = MAX_OPP_ARCHONS; 
			}
			if (numOppGardeners > MAX_OPP_GARDENERS) {
				numOppGardeners = MAX_OPP_GARDENERS;
			}
			if (numOppTrees > MAX_OPP_TREES) {
				numOppTrees = MAX_OPP_TREES;
			}
			if (prizeUnitsUtility > MAX_PRIZE_UNITS) {
				prizeUnitsUtility = MAX_PRIZE_UNITS;
			}
			
			int compound =  (int)oppFightersHealth;
			compound = ((compound << 2) | numOppArchons);
			compound = ((compound << 3) | numOppGardeners);
			compound = ((compound << 4) | numOppTrees);
			compound = ((compound << 7) | (int)occupiedAreaPercent);
			compound = ((compound << 6) | (int)prizeUnitsUtility);
			rc.broadcastInt(pointer, compound);
		}
		
	}
	
 
	protected float minX = Float.MIN_VALUE;
	protected float minY = Float.MIN_VALUE;
	protected float maxX = Float.MAX_VALUE;
	protected float maxY = Float.MAX_VALUE;
	
	protected float squareSideX;
	protected float squareSideY;
	protected float squareArea;
	
	protected void updateBorders() throws GameActionException {
		
		if (mapInitialized) {
			return;
		}
		mapInitialized = rc.readBroadcastBoolean(MAP_INITIALIZED_CHANNEL);
		if (mapInitialized) {
			// Initialize the borders
			minX = rc.readBroadcastFloat(MIN_X_CHANNEL);
			maxX = rc.readBroadcastFloat(MAX_X_CHANNEL);
			minY = rc.readBroadcastFloat(MIN_Y_CHANNEL);
			maxY = rc.readBroadcastFloat(MAX_Y_CHANNEL);
			squareSideX = (maxX-minX)/MAP_SIDE_SQUARES_NUM;
			squareSideY = (maxY-minY)/MAP_SIDE_SQUARES_NUM;
			squareArea = squareSideX*squareSideY;
			return;
		}

		ourLocation = rc.getLocation();
		
		try {
			
			if (!rc.onTheMap(ourLocation, ourType.sensorRadius)) {
				float tempMinX = ourLocation.x-ourType.sensorRadius+FLOAT_TOLERANCE;
				float tempMaxX = ourLocation.x+ourType.sensorRadius-FLOAT_TOLERANCE;
				if (tempMinX > minX && !rc.onTheMap(new MapLocation(tempMinX, ourLocation.y))) {
					minX = tempMinX;
					float storedMinX = rc.readBroadcastFloat(MIN_X_CHANNEL);
					if (minX > storedMinX) {
						rc.broadcastFloat(MIN_X_CHANNEL, minX);
						if (symmetryType != 0) {
							maxX = (middlePoint.x + (middlePoint.x-minX));
							rc.broadcastFloat(MAX_X_CHANNEL, maxX);
						} else {
							tempMaxX = minX + (GameConstants.MAP_MAX_WIDTH + ourType.sensorRadius);
							if (tempMaxX < maxX) {
								float storedMaxX = rc.readBroadcastFloat(MAX_X_CHANNEL);
								if (tempMaxX < storedMaxX) {
									maxX = tempMaxX;
									rc.broadcastFloat(MAX_X_CHANNEL, maxX);
								} else {
									maxX = storedMaxX;
								}
							}
						}
					} else {
						minX = storedMinX;
					}
				} else if (tempMaxX < maxX && !rc.onTheMap(new MapLocation(tempMaxX, ourLocation.y))) {
					maxX = tempMaxX;
					float storedMaxX = rc.readBroadcastFloat(MAX_X_CHANNEL);
					if (maxX < storedMaxX) {
						rc.broadcastFloat(MAX_X_CHANNEL, maxX);
						if (symmetryType != 0) {
							minX = (middlePoint.x - (maxX-middlePoint.x));
							rc.broadcastFloat(MIN_X_CHANNEL, minX);
						} else {
							tempMinX = maxX - (GameConstants.MAP_MAX_WIDTH + ourType.sensorRadius);
							if (tempMinX > minX) {
								float storedMinX = rc.readBroadcastFloat(MIN_X_CHANNEL);
								if (tempMinX > storedMinX) {
									minX = tempMinX;
									rc.broadcastFloat(MIN_X_CHANNEL, minX);
								} else {
									minX = storedMinX;
								}
							}
						}
					} else {
						maxX = storedMaxX;
					}
				}
				float tempMinY = ourLocation.y-ourType.sensorRadius+FLOAT_TOLERANCE;
				float tempMaxY = ourLocation.y+ourType.sensorRadius-FLOAT_TOLERANCE;
				if (tempMinY > minY && !rc.onTheMap(new MapLocation(ourLocation.x, tempMinY))) {
					minY = tempMinY;
					float storedMinY = rc.readBroadcastFloat(MIN_Y_CHANNEL);
					if (minY > storedMinY) {
						rc.broadcastFloat(MIN_Y_CHANNEL, minY);
						if (symmetryType != 1) {
							maxY = (middlePoint.y + (middlePoint.y-minY));
							rc.broadcastFloat(MAX_Y_CHANNEL, maxY);
						} else {
							tempMaxY = minY + (GameConstants.MAP_MAX_WIDTH + ourType.sensorRadius);
							if (tempMaxY < maxY) {
								float storedMaxY = rc.readBroadcastFloat(MAX_Y_CHANNEL);
								if (tempMaxY < storedMaxY) {
									maxY = tempMaxY;
									rc.broadcastFloat(MAX_Y_CHANNEL, maxY);
								} else {
									maxY = storedMaxY;
								}
							}
						}
					} else {
						minY = storedMinY;
					}
				} else if (tempMaxY < maxY && !rc.onTheMap(new MapLocation(ourLocation.x, tempMaxY))) {
					maxY = tempMaxY;
					float storedMaxY = rc.readBroadcastFloat(MAX_Y_CHANNEL);
					if (maxY < storedMaxY) {
						rc.broadcastFloat(MAX_Y_CHANNEL, maxY);
						if (symmetryType != 1) {
							minY = (middlePoint.y - (maxY-middlePoint.y));
							rc.broadcastFloat(MIN_Y_CHANNEL, minY);
						} else {
							tempMinY = maxY - (GameConstants.MAP_MAX_WIDTH + ourType.sensorRadius);
							if (tempMinY > minY) {
								float storedMinY = rc.readBroadcastFloat(MIN_Y_CHANNEL);
								if (tempMinY > storedMinY) {
									minY = tempMinY;
									rc.broadcastFloat(MIN_Y_CHANNEL, minY);
								} else {
									minY = storedMinY;
								}
							}
						}
					} else {
						maxY = storedMaxY;
					}
				}
				
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	class Square extends MapSquareInfo {
		public boolean fullyInside;
		public Square(boolean fullyInside) { this.fullyInside = fullyInside; }
	}
	
}
