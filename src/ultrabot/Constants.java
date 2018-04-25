package ultrabot;

class Constants{
	//BROADCAST CHANNELS
	static int iter = 0;
	static final int ARCHON_LOC_X_1 = iter();
	static final int ARCHON_LOC_Y_1 = iter();
	static final int ARCHON_LOC_X_2 = iter();
	static final int ARCHON_LOC_Y_2 = iter();
	static final int ARCHON_LOC_X_3 = iter();
	static final int ARCHON_LOC_Y_3 = iter();
	static final int ARCHON_ENEMY_LOC_X_1 = iter();
	static final int ARCHON_ENEMY_LOC_Y_1 = iter();
	static final int ARCHON_ENEMY_LOC_X_2 = iter();
	static final int ARCHON_ENEMY_LOC_Y_2 = iter();
	static final int ARCHON_ENEMY_LOC_X_3 = iter();
	static final int ARCHON_ENEMY_LOC_Y_3 = iter();
	static final int NUM_ARCHONS = iter();
	static final int NEW_GARDENER_HOME_X = iter();
	static final int NEW_GARDENER_HOME_Y = iter();
	static final int NEW_GARDENER_HOME_SET = iter();
	static final int NEW_GARDENER_DIR = iter();
	static final int NEW_GARDENER_TYPE = iter();
	static final int HELP_GARDENER_X = iter();
	static final int HELP_GARDENER_Y = iter();
	static final int HELP_GARDENER_REQUESTED = iter();
	static final int OPEN_HEXA_LOC_X = iter();
	static final int OPEN_HEXA_LOC_Y = iter();
	static final int OPEN_HEXA_LOC_SET = iter();
	
	static final int OPEN_HEXA_LOC_X_1 = iter();
	static final int OPEN_HEXA_LOC_Y_1 = iter();
	static final int OPEN_HEXA_LOC_SET_1 = iter();
	static final int OPEN_HEXA_LOC_X_2 = iter();
	static final int OPEN_HEXA_LOC_Y_2 = iter();
	static final int OPEN_HEXA_LOC_SET_2 = iter();
	static final int OPEN_HEXA_LOC_X_3 = iter();
	static final int OPEN_HEXA_LOC_Y_3 = iter();
	static final int OPEN_HEXA_LOC_SET_3 = iter();

	static final int GARDENER_IS_FIRST = iter();
	static final int LUMBERJACK_COMMAND_SET = iter();
	static final int LUMBERJACK_LOC_X = iter();
	static final int LUMBERJACK_LOC_Y = iter();
	static final int SCOUT_SPAWNED = iter();
	static final int FOUND_BORDER = iter();
	static final int FREEZE = iter();
	static final int SCOUT_DYING = iter();
	static final int ARCHON_HOLD_SPAWN = iter(); 

	//IDENTIFIERS
	static final int SPAWN_GARDENER = iter();
	static final int WALL_GARDENER = iter();
	
	//ARBITRARY CONSTANTS
	static final float PI = (float)Math.PI;
	static final int DUMMYID = -1;
	static final int INVALID = -9999;
	
	//OTHER
	static final float POINT_GRADIENT = (20.0f-7.5f)/3000.0f;

	
	//GAME CONSTANTS
	// TODO: remove this ? 
	static final float ARHON_SIGHT_RAD = 10.0f;
	static final float ARCHON_STRIDE_RAD = 0.5f; 
	static final float GARDENER_SIGHT_RAD = 7.0f;
	static final float GARDENER_STRIDE_RAD = 0.5f;
	static final float LUMBERJACK_SIGHT_RAD = 7.0f;
	static final float LUMBERJACK_STRIDE_RAD = 0.75f;
	static final float SCOUT_SIGHT_RAD = 14.0f;
	static final float SCOUT_STRIDE_RAD = 1.25f;
	
	private static int iter(){
		return iter++;
	}
}