package violentfrenchies;

public abstract interface Constants {

	public static final int SQUAD_NUNBER = 3;

	public static final int NUMBER_OF_TOUR_BEFORE_GIVINGUP_GARDEN_SPOT_FINDING_ECO = 175;
	public static final int NUMBER_OF_TOUR_BEFORE_GIVINGUP_GARDEN_SPOT_FINDING_RUSH = 100;
	public static final int NUMBER_OF_TOUR_BEFORE_GIVINGUP_GARDEN_SPOT_FINDING_NORMAL = 150;
	
	public static final int NUMBER_OF_TOUR_BEFORE_BUYING_VP = 500;
	public static final int MAX_LUMBERJACK_PER_GARDENER = 2;

	public static final int ARCHON_DISCOVERY_STATUS_CHANNEL = 9999;
	public static final int SCOUT_DISCOVERY_STATUS_CHANNEL = 9998;
	public static final int GLOBAL_MODE_CHANNEL = 9997;

	public static final int ENNEMIES_REPORTING_CHANNEL_STATUS = 9940;
	public static final int ENNEMIES_REPORTING_CHANNEL_START = 9950;
	public static final int ENNEMIES_REPORTING_TTL_CHANNEL_START = 9960;
	public static final int ENNEMIES_REPORTING_TTL_MAX = 2;

	public static final float REQUIRED_SPOT_RADIUS = 5.5f;
	public static final double TANK_SOLDIER_GARDEN_RATIO = 0.21;

	public static final int DISTANCE_TO_ENEMY_SMALL = 40;
	public static final int DISTANCE_TO_ENEMY_LARGE = 65;
	public static final int INITAL_MAX_NUMBER_TREES = 7;
	public static final int INITAL_MIN_NUMBER_TREES = 9;

	public static final int GARDENER_NUM_OF_TREES_TO_BUILD_LUMBER = 3;
	public static final int GARDENER_NUM_OF_TREES_TO_BUILD_LUMBER_ALOT = 8;
	
	public static final int DESIRED_NUMBER_OF_GARDENERS = 8;

	public static final int GENERATING_DIR_MAX_TRIES_LIMIT = 100;
	
	public static final double SOLDIER_RANDOM_MOVE_PROB = .35;
	
	public static final double MINIMUM_HEALTH_PERCENTAGE = 0.2;
	
	public static final int MINIMUM_HEALTH = 10;
	
	public static final int ROBOT_COUNTERS_BEGIN = 0;
	
	public static final int MINIMUM_BULLETS_TO_SAVE = 200;
	public static final int MAXIMUM_BULLETS_TO_SAVE = 350;

	public static final float GARDENERS_DEFAULT_FREE_SPOT_RADIUS = 6.5f;

	public static final int MAX_NUMBER_SOLDIERS = 30;
	public static final int MAX_NUMBER_LUMBERJACKS = 25;
	public static final int MAX_NUMBER_SCOUTS = 4;

	public static final int MINIMUM_BULLETS_TO_SAVE_BY_SOLDIER = 70;

	
	public static final int MAX_SOLDIER_TO_SOLDIER_DISTANCE = 4;
	
	public static final int COMBAT_LOCATIONS_FIRST_CHANNEL = 100;

	public static final float LUMBERJACK_ATTACK_RADIUS = 15;	
	
	public static final float SCOUT_AVOID_LUMBERJACK_RANGE = 45;
	public static final float SCOUT_MOVEMENT_BLOCKED_DIR_RANGE = 180;

}
