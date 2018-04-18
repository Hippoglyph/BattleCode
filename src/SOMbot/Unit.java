package SOMbot;
import battlecode.common.*;

public abstract class Unit{
	RobotController rc;
	RobotType type;
	MapLocation enemySpawn;
    Direction lastDirection;
    boolean useLastDirection;
    int hugDirection = -1;
    MapLocation friendlySpawn;
    Direction wanderingDir;
    int birthday;
	public Unit(RobotController rc){
		this.rc = rc;
		this.type = rc.getType();
		this.enemySpawn = rc.getInitialArchonLocations(rc.getTeam().opponent())[0];
        this.friendlySpawn = rc.getInitialArchonLocations(rc.getTeam())[0];
        this.useLastDirection = false;
        float leftyrighty = (float)Math.random();
        if (leftyrighty < 0.5)
            this.hugDirection = 1;
        this.wanderingDir = randomDirection();
        this.birthday = rc.getRoundNum();
    
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

    void pathTo(MapLocation to) throws GameActionException{
        if(rc.hasMoved())
            return;
        MapLocation myLocation = rc.getLocation();
        Direction dir = new Direction(myLocation, to);
        if(useLastDirection && rc.canMove(lastDirection)){
            rc.move(lastDirection);
            useLastDirection = false;
            return;
        }
        else if(!useLastDirection && rc.canMove(dir)){
            rc.move(dir);
            return;
        }
        float maxAngle = (float) Math.PI*2;
        for(float angle = (float)Math.PI/8; angle <= maxAngle; angle+=(float)Math.PI/7){
           // rc.setIndicatorLine(myLocation, myLocation.add(dir.rotateLeftDegrees(angle),1),0,0,255);

            if(rc.canMove(dir.rotateLeftRads(hugDirection*angle))){
                rc.move(dir.rotateLeftRads(hugDirection*angle));
                useLastDirection = true;
                lastDirection = dir.rotateLeftRads(hugDirection*angle);

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
}