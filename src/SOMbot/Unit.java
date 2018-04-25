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
    //Leader stuff
    boolean isLeader = false;
    boolean spawnSoldiers = false;
    boolean spawnLumberjacks = false;
    boolean spawnGardeners = false;
    boolean spawnScouts = false;
    boolean spawnTanks = false;
    boolean neverLeader = false;
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

    boolean willCollideWithLocation(MapLocation location, MapLocation bullet, Direction bulletDir){
        MapLocation myLocation = location;

        // Get relevant bullet information
        Direction propagationDirection = bulletDir;
        MapLocation bulletLocation = bullet;

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

    void safeMove() throws GameActionException{
        if(rc.hasMoved())
            return;
        BulletInfo[] bullets = rc.senseNearbyBullets();
        for(BulletInfo bullet : bullets){
            if(willCollideWithMe(bullet)){
                if(tryMove(new Direction(rc.getLocation(), bullet.location).rotateRightRads((float)Math.PI/2)) || tryMove(new Direction(rc.getLocation(), bullet.location).rotateLeftRads((float)Math.PI)))
                    break;
                
            }
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

    void resetInvalidNest() throws GameActionException{
        MapLocation nest = broadcastHandle.getNestLocation();
        if(!isValid(nest))
            return;

        if(!rc.canSenseAllOfCircle(nest,nestRange))
            return;

        TreeInfo[] friendlyTrees = rc.senseNearbyTrees(nest,nestRange,rc.getTeam());
        if(friendlyTrees.length > 0)
            broadcastHandle.resetNestLocation();
    }

    void doLeaderStuff() throws GameActionException{
        int soldierCount = broadcastHandle.getCount(RobotType.SOLDIER);
        int lumberjackCount = broadcastHandle.getCount(RobotType.LUMBERJACK);
        int gardenerCount = broadcastHandle.getCount(RobotType.GARDENER);
        int scoutCount = broadcastHandle.getCount(RobotType.SCOUT);
        int tankCount = broadcastHandle.getCount(RobotType.TANK);
        int notFoundNest = broadcastHandle.getNotFoundNest();
        int trees = broadcastHandle.getTrees();
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(-1,rc.getTeam().opponent());
        int unitCount = soldierCount + lumberjackCount + gardenerCount + scoutCount + tankCount;
        if(unitCount == 0)
            unitCount = 1;
        float treeUnitRatio = (float)trees/unitCount;
        float bullets = rc.getTeamBullets();

        if(bullets / rc.getVictoryPointCost() >= GameConstants.VICTORY_POINTS_TO_WIN - rc.getTeamVictoryPoints()){
            rc.donate(bullets);
            return;
        }

        if(notFoundNest < 1 && (bullets >= 350) || gardenerCount < 1){
            if(!spawnGardeners){
                broadcastHandle.spawn(RobotType.GARDENER,true);
                spawnGardeners = true;
            }
        }
        else{
            //do something smart
            if(spawnGardeners){
                broadcastHandle.spawn(RobotType.GARDENER,false);
                spawnGardeners = false;
            }
        }

        if((soldierCount < gardenerCount) || (nearbyEnemies.length > 0 && soldierCount < 3*gardenerCount)) {
            if(!spawnSoldiers){
                broadcastHandle.spawn(RobotType.SOLDIER, true);
                spawnSoldiers = true;
            }
            
        }
        else {
           // System.out.println(soldierCount);
            if(spawnSoldiers){
                broadcastHandle.spawn(RobotType.SOLDIER,false);
                spawnSoldiers = false;
            }
            
        }



        if(((treeUnitRatio > 5f && 0.35f > (float)lumberjackCount/unitCount) || (lumberjackCount < 1 && trees > 0))){ //&& (soldierCount != 0 && nearbyEnemies.length == 0)){
            if(!spawnLumberjacks){
                broadcastHandle.spawn(RobotType.LUMBERJACK, true);
                spawnLumberjacks = true;
            }
        }
        else {
            if(spawnLumberjacks){
                broadcastHandle.spawn(RobotType.LUMBERJACK,false);
                spawnLumberjacks = false;  
            }
            
        }

        if(gardenerCount > 0 && bullets > gardenerCount*RobotType.SOLDIER.bulletCost*2 && !spawnLumberjacks && !spawnSoldiers && bullets > 1000){
            float bulletsPerPoint = rc.getVictoryPointCost();
            float maxBullets = 100;
            int donation = (int)(maxBullets/bulletsPerPoint);
            rc.donate(donation * bulletsPerPoint);
        }


        broadcastHandle.resetUnitCounts();
    }

    void reportEmptyArea() throws GameActionException{
        resetInvalidNest();
        if(type.sensorRadius < nestRange)
            return;
        MapLocation best = broadcastHandle.getNestLocation();
        float curDist = Float.MAX_VALUE;
        boolean foundBetter = false;
        boolean prior = true;
        if(isValid(best))
            curDist = best.distanceTo(friendlySpawn);
        else
            prior = false;

        for(int i = 0; i < 20; i++){
            float angle = (float)(Math.random() * Math.PI * 2);
            float dist = (float)Math.random() * (type.sensorRadius - nestRange);
            MapLocation sample = rc.getLocation().add(new Direction(angle),dist);
            TreeInfo[] trees = rc.senseNearbyTrees(sample,nestRange,rc.getTeam());
            RobotInfo[] guardeners = rc.senseNearbyRobots(sample,nestRange,rc.getTeam());
            int gardeners = 0;
            for(RobotInfo gard : guardeners){
                if(gard.getType() == RobotType.GARDENER)
                    gardeners++;
            }
            if(trees.length == 0 && gardeners == 0 && rc.onTheMap(sample,nestRange) && curDist > sample.distanceTo(friendlySpawn) ){
                best = sample;
                curDist = sample.distanceTo(friendlySpawn);
                foundBetter = true;
            }
        }
        if(foundBetter){
            if(prior)
                broadcastHandle.resetNestLocation();
            broadcastHandle.reportNestLocation(best);
        }
    }

    void giveUpLeader() throws GameActionException{
        if(rc.getHealth()/type.maxHealth < 0.2)
            neverLeader = true;
        if(!isLeader)
            return;
        else if(neverLeader){
            broadcastHandle.giveUpLeader();
            isLeader = false;
        }
    }

    void takeUpLeader() throws GameActionException{
        if(neverLeader || isLeader)
            return;
        boolean shouldTakeUp = broadcastHandle.takeUpLeader();
        if(shouldTakeUp){
            isLeader = true;
            spawnSoldiers = true;
        }
    }

}