package ultrabot;

public class Strategy {

	// --- MAIN STRATEGY --- //
	
	static final int WALL_GARDENER_LIMIT = 30;
	static final int LUMBERJACK_LIMIT = 3;
	static final int SPAWN_GARDENER_LIMIT = 5;
	static final int SPAWN_SOLDIER_LIMIT = 1;
	
	
	// --- SPAWN PARAMETERS --- //
	static final float INITIAL_GARD_HOME_DIST = 5.2f;  // Min 3.1f

	// --- MOVEMENT PARAMETERS --- //
	
	static final int DEGREE_OFFSET_STEP = 10; // Turning step if move not possible
	static final int CHECKS_PER_SIDE = 18; // Maximum turning directions allowed
	static final int LOCATION_HISTORY_LIMIT = 10; // Maximum number of previous locations to store.
	static final int REROUTE_TURN_MAX = 5;
	static final float GREEDINESS = 0.75f;
	static final float ARCHON_TRY_HIDE_MAX_TURNS = 100;
	
	// --- COLONY PARAMETERS --- //
	static final float HONEYCOMB_SPACING = 7.5f;//4.2f; //(float) (1 + Math.sqrt(2) + Math.sqrt(3));
	static final float ARCHON_BUFFER_RADIUS = 7.5f; 
	
	// --- BUILD PARAMETERS --- //
	static final float SPAWN_DOOR = 0.9272f;
	static final int NUM_TREE_WALLS = 6;

	// --- MEMORY RELATED --- //
	static final int LOCATION_MEMORY_SIZE = 15;
	static final float STUCK_TOLERANCE = 0.5f;
	static final int STUCK_STEPS = 5;

	// --- VICTORY RELATED --- //
	static final int MAX_FREEZE_ROUNDS = 100;
}
