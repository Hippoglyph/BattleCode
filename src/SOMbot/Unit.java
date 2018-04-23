package SOMbot;
import battlecode.common.*;

public abstract class Unit{
	RobotController rc;
	RobotType type;
	MapLocation enemySpawn;
    Direction lastDirection;
    boolean useLastDirection;
    boolean hugRight;
    MapLocation friendlySpawn;
    Direction wanderingDir;
    int birthday;
    float nestRange;
    BroadcastHandler broadcastHandle;
    int initPatience = 25;
    int patience;
	public Unit(RobotController rc){
		this.rc = rc;
        nestRange = RobotType.GARDENER.bodyRadius*3 + GameConstants.BULLET_TREE_RADIUS*2 + RobotType.GARDENER.bodyRadius/4;
		this.type = rc.getType();
		this.enemySpawn = rc.getInitialArchonLocations(rc.getTeam().opponent())[0];
        this.friendlySpawn = rc.getInitialArchonLocations(rc.getTeam())[0];
        this.useLastDirection = false;
        hugRight = probIs(0.5f);
        //if(hugRight)
        //    System.out.println("hugging right");
        //else
        //    System.out.println("hugging left");
        this.wanderingDir = randomDirection();
        this.birthday = rc.getRoundNum();
        this.broadcastHandle = new BroadcastHandler(rc);
        this.patience = this.initPatience;
    
	}

	public abstract void run();

	 /**
     * Returns a random Direction
     * @return a random Direction
     */
    Direction randomDirection() {
        return new Direction((float)Math.random() * 2 * (float)Math.PI);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    boolean tryMove(Direction dir) throws GameActionException {
        return tryMove(dir,20,3);
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
    boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

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

    /**
     * A slightly more complicated example function, this returns true if the given bullet is on a collision
     * course with the current robot. Doesn't take into account objects between the bullet and this robot.
     *
     * @param bullet The bullet in question
     * @return True if the line of the bullet's path intersects with this robot's current position.
     */
    boolean willCollideWithMe(BulletInfo bullet) {
        MapLocation myLocation = rc.getLocation();

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

    boolean willBeAtPoint(BulletInfo bullet, MapLocation location) {
        MapLocation myLocation = location;

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

    boolean probIs(float p){
        return ((float)Math.random() < p);
    }

    void loosePatience(){
        patience--;
        if(patience < 0){
            patience = initPatience;
            hugRight= !hugRight;
        }
    }

    void resetPatience(){
        patience = initPatience;
    }

    void pathTo(MapLocation to) throws GameActionException{
        if(rc.hasMoved())
            return;
        MapLocation myLocation = rc.getLocation();
        Direction dir = new Direction(myLocation, to);
        if(rc.canMove(dir)){
            rc.move(dir);
            hugRight = !hugRight;
            return;
        }

        Direction newDir;
        float maxAngle = (float) Math.PI*2;
        for(float angle = (float)Math.PI/8; angle <= maxAngle; angle+=(float)Math.PI/7){
            if(hugRight)
                newDir = dir.rotateLeftRads(angle);
            else
                newDir = dir.rotateRightRads(angle);
            if(rc.canMove(newDir)){
                loosePatience();
                rc.move(newDir);
                lastDirection = newDir;
                return;

            }
            
        }
    }

    void wanderingRumba() throws GameActionException{
        if(rc.hasMoved())
            return;
        if (rc.canMove(wanderingDir))
            rc.move(wanderingDir);
        else
            wanderingDir = randomDirection();
        
    }


    void drawDebugCircle(MapLocation center, float radius){
    	float res =  (float)Math.PI/6;
    	for(float i = 0; i < 2*Math.PI; i+=res){
    		MapLocation from = new MapLocation(center.x + radius * (float)Math.cos(i), center.y + radius*(float)Math.sin(i));
    		MapLocation to = new MapLocation(center.x + radius * (float)Math.cos(i+res), center.y + radius*(float)Math.sin(i+res));
    		rc.setIndicatorLine(from, to, 250,0,0);
    	}
    }

    MapLocation sampleWithinStride(){
        float angle = (float)(Math.random() * Math.PI * 2);
        float dist = rc.getType().strideRadius;//(float)Math.random() * rc.getType().strideRadius;
        Direction dir = new Direction(angle);
        return rc.getLocation().add(dir,dist);
    }

    float getSafetyScore(MapLocation location, BulletInfo[] bullets, RobotInfo[] enemies){
        float lumberjackDamage = 0;
        float hittingBullets = 0;
        for(RobotInfo robot : enemies){
            if(robot.getType() == RobotType.LUMBERJACK && robot.getLocation().distanceTo(rc.getLocation()) < RobotType.LUMBERJACK.bodyRadius + GameConstants.LUMBERJACK_STRIKE_RADIUS){
                lumberjackDamage += robot.getType().attackPower;
            }
        }
        for(BulletInfo bullet : bullets){
            if(willBeAtPoint(bullet,location)){
                Direction bulletDir = bullet.dir;
                Direction towardsSample = new Direction(bullet.location, location);
                float bulletAngle = (float)Math.PI/2 - towardsSample.radiansBetween(bulletDir);
                hittingBullets += bullet.getDamage()*bulletAngle/(float)(Math.PI/2);
            }
        }

        return lumberjackDamage + hittingBullets;
    }

    void safeMove() throws GameActionException{
        if(rc.hasMoved())
            return;
        BulletInfo[] bullets = rc.senseNearbyBullets();
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        float bestScore = getSafetyScore(rc.getLocation(),bullets,enemies);
        MapLocation bestLocation = rc.getLocation();
        for(int i = 0; i < 20; i++){
            MapLocation sample = sampleWithinStride();
            float sampleScore = getSafetyScore(sample, bullets, enemies);
            if(sampleScore <= bestScore && rc.canMove(sample)){
                //System.out.println("FOUND BETTER ");
                bestScore = sampleScore;
                bestLocation = sample;
            }
        }

        if(!bestLocation.equals(rc.getLocation()) && rc.canMove(bestLocation)){
            rc.move(bestLocation);
        }
        return;
    }

    void reportTrees() throws GameActionException{
        TreeInfo[] trees = rc.senseNearbyTrees(-1, Team.NEUTRAL);
        if(trees.length > 0)
            broadcastHandle.reportTrees(trees.length);
    }

    boolean isValid(MapLocation loc) throws GameActionException{
        return (loc.x > 0f && loc.y > 0f);
    }

    MapLocation nullMap() throws GameActionException{
        return new MapLocation(-1f,-1f);
    }

    void reportClosestEnemy() throws GameActionException{
        RobotInfo[] enemies = rc.senseNearbyRobots(-1,rc.getTeam().opponent());
        if(enemies.length > 0)
            broadcastHandle.reportEnemy(enemies[0].location);
    }
}