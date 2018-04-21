package SOMbot;
import battlecode.common.*;

public class BroadcastHandler{
	private RobotController rc;
	public static class Channel {
		public static final int SOLDIERCOUNT = 0;
		public static final int LUMBERJACKCOUNT = 1;
		public static final int GARDENERCOUNT = 2;
		public static final int TANKCOUNT = 3;
		public static final int ARCHONCOUNT = 4;
		public static final int ENEMYPRIORITYPOSITION = 5;
		public static final int TREECOUNT = 6;
		public static final int SCOUTCOUNT = 7;
		public static final int SPAWNSOLDIER = 8;
		public static final int SPAWNLUMBERJACK = 9;
		public static final int SPAWNGARDENER = 10;
		public static final int SPAWNSCOUT = 11;
		public static final int SPAWNTANK = 12;
		public static final int[] PRIORITYTREEBASE = {13, 14, 15, 16, 17};
		public static final int[] TREES = {18, 19, 20, 21, 22};
		public static final int PRIORITYGARDENER = 23; 
	}

	public int encode(MapLocation loc) throws GameActionException{
		int x = (int)(loc.x * 100f) << 16;
		int y = (int)(loc.y * 100f);
		return x | y;
	}

	public MapLocation decode(int code) throws GameActionException{
		int y = code & 0xFFFF;
		int x = (code & 0xFFFF0000) >> 16;
		float xf = (float)x/100f;
		float yf = (float)y/100f;
		return new MapLocation(xf,yf);
	}

	public BroadcastHandler(RobotController unit){
		this.rc = unit;
	}

	public void reportExistence() throws GameActionException{
		int count = 0;
		switch (rc.getType()) {
			case LUMBERJACK:
				count = rc.readBroadcast(Channel.LUMBERJACKCOUNT) + 1;
				rc.broadcast(Channel.LUMBERJACKCOUNT,count);
				break;

			case SOLDIER:
				count = rc.readBroadcast(Channel.SOLDIERCOUNT) + 1;
				rc.broadcast(Channel.SOLDIERCOUNT,count);
				break;

			case GARDENER:
				count = rc.readBroadcast(Channel.GARDENERCOUNT) + 1;
				rc.broadcast(Channel.GARDENERCOUNT,count);
				break;

			case ARCHON:
				count = rc.readBroadcast(Channel.ARCHONCOUNT) + 1;
				rc.broadcast(Channel.ARCHONCOUNT,count);
				break;

			case TANK:
				count = rc.readBroadcast(Channel.TANKCOUNT) + 1;
				rc.broadcast(Channel.TANKCOUNT,count);
				break;

			case SCOUT:
				count = rc.readBroadcast(Channel.SCOUTCOUNT) + 1;
				rc.broadcast(Channel.SCOUTCOUNT, count);
				break;
		}
	}

	public int getCount(RobotType type) throws GameActionException{
		int count = 0;
		switch (type) {
			case LUMBERJACK:
				count = rc.readBroadcast(Channel.LUMBERJACKCOUNT);
				break;

			case SOLDIER:
				count = rc.readBroadcast(Channel.SOLDIERCOUNT);
				break;

			case GARDENER:
				count = rc.readBroadcast(Channel.GARDENERCOUNT);
				break;

			case ARCHON:
				count = rc.readBroadcast(Channel.ARCHONCOUNT);
				break;

			case TANK:
				count = rc.readBroadcast(Channel.TANKCOUNT);
				break;

			case SCOUT:
				count = rc.readBroadcast(Channel.SCOUTCOUNT);
				break;
		}
		return count;
	}

	public void resetUnitCounts() throws GameActionException{
		reset(Channel.LUMBERJACKCOUNT);
		reset(Channel.SOLDIERCOUNT);
		reset(Channel.GARDENERCOUNT);
		reset(Channel.ARCHONCOUNT);
		reset(Channel.TANKCOUNT);
		reset(Channel.SCOUTCOUNT);

	}

	public void reset(int channel) throws GameActionException{
		rc.broadcast(channel,0);
	}

	public void spawn(RobotType type, boolean set) throws GameActionException{
		switch (type) {
			case LUMBERJACK:
				rc.broadcastBoolean(Channel.SPAWNLUMBERJACK, set);
				break;

			case SOLDIER:
				rc.broadcastBoolean(Channel.SPAWNSOLDIER, set);
				break;

			case GARDENER:
				rc.broadcastBoolean(Channel.SPAWNGARDENER, set);
				break;

			case TANK:
				rc.broadcastBoolean(Channel.SPAWNTANK, set);
				break;

			case SCOUT:
				rc.broadcastBoolean(Channel.SPAWNSCOUT, set);
				break;
		}
	}

	public boolean shouldSpawnRobot(RobotType type) throws GameActionException{
		boolean ret = false;
		switch (type) {
			case LUMBERJACK:
				ret = rc.readBroadcastBoolean(Channel.SPAWNLUMBERJACK);
				break;

			case SOLDIER:
				ret = rc.readBroadcastBoolean(Channel.SPAWNSOLDIER);
				break;

			case GARDENER:
				ret = rc.readBroadcastBoolean(Channel.SPAWNGARDENER);
				break;

			case TANK:
				ret = rc.readBroadcastBoolean(Channel.SPAWNTANK);
				break;

			case SCOUT:
				ret = rc.readBroadcastBoolean(Channel.SPAWNSCOUT);
				break;
		}
		return ret;
	}

	public void reportNestTree(MapLocation tree) throws GameActionException{
		int encoded = encode(tree);
		for (int channel : Channel.PRIORITYTREEBASE){
			int treeAt = rc.readBroadcast(channel);
			if(treeAt != 0){
				if(encoded == treeAt)
					return;
			}
			else{
				rc.broadcast(channel,encoded);
				return;
			}
		}
	}

	public void resetNestTree(MapLocation tree) throws GameActionException{
		int encoded = encode(tree);
		for(int channel : Channel.PRIORITYTREEBASE){
			if (decode(rc.readBroadcast(channel)).distanceTo(tree) < GameConstants.NEUTRAL_TREE_MIN_RADIUS){
				rc.broadcast(channel,0);
			}
		}
	}

	public MapLocation[] getNestPriorityTrees() throws GameActionException{
		MapLocation[] locations = new MapLocation[5];
		int i = 0;
		for(int channel : Channel.PRIORITYTREEBASE){
			int encoded = rc.readBroadcast(channel);
			if(encoded == 0)
				locations[i] = new MapLocation(-1f,-1f);
			else
				locations[i] = decode(encoded);
			i++;
		}
		return locations;
	}
}