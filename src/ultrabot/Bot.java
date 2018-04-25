package ultrabot;
import battlecode.common.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;

abstract class Bot{
	static RobotController rc;
	static RobotType type;
	static List<MapLocation> archonLocations = new ArrayList<MapLocation>();
	static List<MapLocation> enemyArchonLocations = new ArrayList<MapLocation>();;
	
	
	/** ACTIVATE FOR DEBUGGING */
 	boolean debug = false;

	static void init(RobotController rController){
		// Select type of bot
		rc = rController;
		type = rc.getType();
	}

	static Bot getBotByType(RobotType type) {
		Bot bot;
		switch (type) {
			case ARCHON:
	            bot = new Archon();
				break;
			case GARDENER:
				bot = new Gardener();
				break;
			case LUMBERJACK:
				bot = new Lumberjack();
				break;
			case SCOUT:
				bot = new Scout();
				break;
			case SOLDIER:
				bot = new Soldier();
				break;
			case TANK:
				bot = new Tank();
				break;
	        default:
	        	throw new RuntimeException("Unknown type specified: " + type);
		}
		return bot;
	}


	// Note: Everything here is executed for each bot, regardless of type
	// but can be implemented in the classes for each bot
	final void startRun() throws GameActionException{
		beforeStep();
		while(true){
			mainStep();
			afterStep();
			Clock.yield();
		}
	}

	protected void beforeStep() throws GameActionException{
		// Do something when the bot is spawned
		// Only activated once for each bot !
	}

	protected void mainStep() throws GameActionException{
		//if(previousLocations.size() > Strategy.LOCATION_MEMORY_SIZE){
		//		previousLocations.remove(previousLocations.size() - 1);
		//}
		//previousLocations.add(0,rc.getLocation());
		// Do something every turn
	}

	protected void afterStep() throws GameActionException{
		// Do something after each turn
	}


	protected float distToLoc(MapLocation loc){
		return rc.getLocation().distanceTo(loc);
	}
	
	protected MapLocation getClosestHexaLocation(MapLocation originHexaLoc)
	{
		float dist = rc.getLocation().distanceTo(originHexaLoc);
		Direction dir = originHexaLoc.directionTo(rc.getLocation());
		if (dist>0) {
			float hexWidth = Strategy.HONEYCOMB_SPACING*(float)Math.sqrt(3)/2;
			float hexHeight = Strategy.HONEYCOMB_SPACING;
			//float x = dir.getDeltaX(dist);
			//float y = dir.getDeltaY(dist);
			float x = rc.getLocation().x - originHexaLoc.x;
			float y = rc.getLocation().y - originHexaLoc.y;
			System.out.println("dx = "+x+", dy = "+y);
			// Find the row and column of the box that the point falls in.
			float column,row;
		    
		    column = (float) Math.round(x / hexWidth);	
		    float hexaX = originHexaLoc.x + (column*hexWidth);
		    float hexaY;
		    
		    boolean columnIsOdd = column % 2 == 1;		    	    
		    
		    // Is the row an odd number?
		    if (columnIsOdd) {	// Yes: Offset x to match the indent of the row
		    	if(y>0) {
			        row = (float) Math.round((y - hexHeight/2) / hexHeight);
			        hexaY = originHexaLoc.y + (row*hexHeight) + hexHeight/2;
		    	}
		    	else {
		    		row = (float) Math.round((y + hexHeight/2) / hexHeight);
		    		hexaY = originHexaLoc.y + (row*hexHeight) - hexHeight/2;
		    	}
		    }
		    else { // No: Calculate normally
		        row = (float) Math.round(y / hexHeight);
	    		hexaY = originHexaLoc.y + (row*hexHeight);
		    }
		    if(debug) {
			    rc.setIndicatorLine(rc.getLocation(), originHexaLoc, 255, 255, 255);
			    rc.setIndicatorLine(rc.getLocation(), new MapLocation(hexaX, hexaY), 0, 0, 0);
		    }
		    return new MapLocation(hexaX, hexaY);
		    
		}
		return rc.getLocation();
	}
	
	protected MapLocation getClosestOpenNodeLocation(MapLocation closestHexa, MapLocation goalLocation) {	
		MapLocation openNode;
		MapLocation closestOpenNode = goalLocation;
		float minDist = Float.MAX_VALUE;
		float dist;
		for(int i = 0; i<6 ; i++) {
			openNode = closestHexa.add(i*Constants.PI/3, Strategy.HONEYCOMB_SPACING/(float)Math.sqrt(3));
			dist = (1-Strategy.GREEDINESS)*rc.getLocation().distanceTo(openNode)+Strategy.GREEDINESS*goalLocation.distanceTo(openNode);
			if (dist<minDist) {
				closestOpenNode = openNode;
				minDist = dist;
			}
			//rc.setIndicatorDot(openNode, 0, 0, 0);
		}
		//rc.setIndicatorLine(rc.getLocation(), closestOpenNode, 255, 255, 255);
		return closestOpenNode;
	}
	

	protected void tryMove(MapLocation loc) throws GameActionException{
		try{
			if( rc.getLocation().distanceTo(loc) <= rc.getType().strideRadius){
				if(rc.canMove(loc)){
					rc.move(loc);
				}
			} else{
				tryMove(rc.getLocation().directionTo(loc));
			}
		} catch(GameActionException e){
			System.err.println("Failure while trying to move");
			e.printStackTrace();
		}
	}

	/**
     * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    protected boolean tryMove(Direction dir) throws GameActionException {
        return tryMove(dir,Strategy.DEGREE_OFFSET_STEP,Strategy.CHECKS_PER_SIDE);
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
    protected boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

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
	
	Direction randomDirection() {
        return new Direction((float)Math.random() * 2 * (float)Math.PI);
	}


	/**
	 * Pursuit is a method used for following other robots, while
	 * trying to avoid damage from other robots
	 */
	protected void pursuit(RobotInfo victim) throws GameActionException{
		//We should try to stay within half of sensing distance from the enemy
		float dist = distToLoc(victim.getLocation());
		float dX, dY;
		if(dist > 0.5*rc.getType().sensorRadius){
			//move directly towards it
			tryMove(victim.getLocation());
		} else{
			tryMove(randomDirection());
		}
	}

	  /**
     * A slightly more complicated example function, this returns true if the given bullet is on a collision
     * course with the current robot. Doesn't take into account objects between the bullet and this robot.
     *
     * @param bullet The bullet in question
     * @return True if the line of the bullet's path intersects with this robot's current position.
     */
    static boolean willCollideWithMe(BulletInfo bullet, MapLocation myLocation) {
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

	protected void avoidBullets(RobotInfo target) throws GameActionException{
		List<BulletInfo> nearbyBullets = new ArrayList<>(
			Arrays.asList(rc.senseNearbyBullets(RobotType.LUMBERJACK.sensorRadius)));
		if(nearbyBullets.size() > 0 ){
			//nearbyBullets = sortBullets(nearbyBullets);
			//tryMove(rc.getLocation().add(nearbyBullets.get(0).getDir().rotateLeftDegrees(90.0f), rc.getType().strideRadius));
			Direction tryDir;		
			int bestAvoidance = nearbyBullets.size();
			int collideCounter;
			int maxBulletChecks = 10;
			Direction startDirection = new Direction(rc.getLocation(), target.getLocation()).rotateLeftDegrees(90);
			Direction bestDir = startDirection;
			float strideRadius = rc.getType().strideRadius;
			MapLocation currentLocation = rc.getLocation();
			for(int i=1; i<=180; i+=2){
				collideCounter = 0;
				tryDir =  startDirection.rotateLeftDegrees(i);
				for(int j=0; j<Math.min(maxBulletChecks,nearbyBullets.size()); j++){
					if(willCollideWithMe(nearbyBullets.get(j),currentLocation.add(tryDir, strideRadius))){
						collideCounter++;
					}
				}
				if(collideCounter < bestAvoidance){
					bestDir = tryDir;
					bestAvoidance = collideCounter;
				}
			}
			if(debug){debugBestAvoidance(bestDir);}
			if(rc.canMove(bestDir)){
				tryMove(bestDir);
			}
		} else{
			// Do nothing
		}
	}

	public List<BulletInfo> sortBullets(List<BulletInfo> bullets){
		Collections.sort(bullets,
			(t1, t2) -> Float.compare(distToLoc(bulletFuturePos(t1)), distToLoc((bulletFuturePos(t2)))));
		return bullets;
	}

	

	protected MapLocation bulletFuturePos(BulletInfo b){
		return b.getLocation().add(b.getDir(), b.getSpeed());
	}

	protected void receiveEnemyArchonLocations() throws GameActionException{
		List<Integer[]> channels = getEnemyArchonChannels(); 
		float x, y;
		try{
			int numArchons = rc.readBroadcastInt(Constants.NUM_ARCHONS);
			for(int i = 0; i<numArchons; i++){
				x = rc.readBroadcastFloat(channels.get(i)[0]);
				y = rc.readBroadcastFloat(channels.get(i)[1]);
				enemyArchonLocations.add(new MapLocation(x, y));
			}
			
		} catch(GameActionException e){
			System.err.println("Could not receive enemy archon location");
			e.printStackTrace();
		}
	}
	
	protected void receiveArchonLocations() throws GameActionException{
		List<Integer[]> channels = getFriendlyArchonChannels(); 
		float x, y;
		try{
			int numArchons = rc.readBroadcastInt(Constants.NUM_ARCHONS);
			System.out.println(numArchons);
			for(int i = 0; i<numArchons; i++){
				x = rc.readBroadcastFloat(channels.get(i)[0]);
				y = rc.readBroadcastFloat(channels.get(i)[1]);
				System.out.println(x);
				System.out.println(y);
				archonLocations.add(new MapLocation(x, y));
				System.out.println("Finished adding");
			}
		} catch(GameActionException e){
			System.err.println("Could not receive enemy archon location");
			e.printStackTrace();
		}
	}

	protected List<Integer[]> getEnemyArchonChannels(){
		List<Integer[]> channels = new ArrayList<Integer[]>();
		channels.add(new Integer[]{Constants.ARCHON_ENEMY_LOC_X_1, Constants.ARCHON_ENEMY_LOC_Y_1});
		channels.add(new Integer[]{Constants.ARCHON_ENEMY_LOC_X_2, Constants.ARCHON_ENEMY_LOC_Y_2});
		channels.add(new Integer[]{Constants.ARCHON_ENEMY_LOC_X_3, Constants.ARCHON_ENEMY_LOC_Y_3});
		return channels;
	}

	protected List<Integer[]> getFriendlyArchonChannels(){
		List<Integer[]> channels = new ArrayList<Integer[]>();
		channels.add(new Integer[]{Constants.ARCHON_LOC_X_1, Constants.ARCHON_LOC_Y_1});
		channels.add(new Integer[]{Constants.ARCHON_LOC_X_2, Constants.ARCHON_LOC_Y_2});
		channels.add(new Integer[]{Constants.ARCHON_LOC_X_3, Constants.ARCHON_LOC_Y_3});
		return channels;
	}

	
	protected List<Integer[]> getHexaLocChannels(){
		List<Integer[]> channels = new ArrayList<Integer[]>();
		channels.add(new Integer[]{Constants.OPEN_HEXA_LOC_X_1, Constants.OPEN_HEXA_LOC_X_1});
		channels.add(new Integer[]{Constants.OPEN_HEXA_LOC_X_2, Constants.OPEN_HEXA_LOC_X_2});
		channels.add(new Integer[]{Constants.OPEN_HEXA_LOC_X_3, Constants.OPEN_HEXA_LOC_X_3});
		return channels;
	}

	//Checks if the hexa location is valid for a gardener to get placed
    static boolean isHexaLocationValid(RobotController rc, MapLocation hexaLocation) throws GameActionException{
    	try {
	    	if (rc.canSenseAllOfCircle(hexaLocation, RobotType.GARDENER.bodyRadius)) {
				if (rc.onTheMap(hexaLocation, RobotType.GARDENER.bodyRadius)) {
					if (rc.isLocationOccupiedByRobot(hexaLocation)) {
						if(rc.isCircleOccupiedExceptByThisRobot(hexaLocation, RobotType.GARDENER.bodyRadius)) {
							if (rc.senseRobotAtLocation(hexaLocation).type==RobotType.GARDENER) {
								return false;
							}
						}
					}
				}
				else {
					//System.out.println("Location is outside map"+hexaLocation.x+", "+hexaLocation.y);
					return false;
				}
			}
	    	if(rc.getType()==RobotType.ARCHON) {
	    		if(rc.getLocation().distanceTo(hexaLocation)<Strategy.ARCHON_BUFFER_RADIUS) {
	    			return false;
	    		}
	    	}
    	} catch (GameActionException e) {
			System.err.println("Failed to validate hexa location");
			e.printStackTrace();
		}
		return true;
	}

	static boolean almostReachedLocation(RobotController rc, MapLocation location) {
    	return rc.getLocation().isWithinDistance(location, rc.getType().bodyRadius);
	}
	
	static void debugBestAvoidance(Direction dir){
		rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(dir, 5.0f), 254, 0, 0);
	}

}
