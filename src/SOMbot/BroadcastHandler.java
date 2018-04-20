package SOMbot;
import battlecode.common.*;

public class BroadcastHandler{
	private RobotController rc;
	public class Channel {
		public static final int SOLDIERCOUNT = 407;
		public static final int LUMBERJACKCOUNT = 507;
		public static final int GARDENERCOUNT = 590;
		public static final int TANKCOUNT = 3096;
		public static final int ARCHONCOUNT = 2477;
		public static final int ENEMYPRIORITYPOSITION = 5789;
		public static final int TREECOUNT = 6254;
		public static final int SCOUTCOUNT = 1748;
		public static final int SPAWNSOLDIER = 888;
		public static final int SPAWNLUMBERJACK = 186;
		public static final int SPAWNGARDENER = 8174;
		public static final int SPAWNSCOUT = 9062;
		public static final int SPAWNTANK = 2076;
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
				System.out.println(set);
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
}