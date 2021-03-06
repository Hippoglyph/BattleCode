package SOMbot;
import battlecode.common.*;

public class Gardener extends Unit{
	private boolean foundNest;
    private boolean clearNeutral;
	private MapLocation nestPos;
	private float offsetAngle;
    private boolean hasNestTrees;
    private boolean targetAquired;
    private MapLocation targetLocation;
    private MapLocation previousLocation;
    private int nestPatience;
    private int nestPatienceInit = 30;
    private boolean ignoreTarget = false;
	public Gardener(RobotController rc){
        super(rc);
        foundNest = false;
        calculateOffsetAngle();
        
        clearNeutral = true;
        hasNestTrees = true;
        Direction toEnemy = new Direction(rc.getLocation(), enemySpawn);
        if(hugRight)
            toEnemy = toEnemy.rotateLeftRads((float)Math.PI/4);
        else
            toEnemy = toEnemy.rotateRightRads((float)Math.PI/4);
        wanderingDir = toEnemy;
        targetAquired = false;
        previousLocation = rc.getLocation();
        nestPatience = nestPatienceInit;
    }


    @Override
	public void run()  {
        
        // Try/catch blocks stop unhandled exceptions, which cause your robot to explode         
        try {
            targetLocation = nullMap();
            
        // The code you want your robot to perform every round should be in this loop
	        while (true) {
                giveUpLeader();
                if(isLeader)
                    doLeaderStuff();
                else
                    takeUpLeader();
                broadcastHandle.reportExistence();

                // Listen for home archon's location
                // Generate a random direction
                shakeNeutralTrees();
                reportTrees();
                reportClosestEnemy();
                if(!targetAquired)
                    aquireTarget();
                //else if(!foundNest){
                   // if(ignoreTarget)
                        //rc.setIndicatorLine(rc.getLocation(),targetLocation,255,0,0);
                    //else
                        //rc.setIndicatorLine(rc.getLocation(),targetLocation,0,0,255);
               // }


                if (!foundNest){
                	//tryMove(randomDirection());
                    if(targetAquired && !ignoreTarget){
                        if(nestPatience < 0){
                            if(previousLocation.distanceTo(targetLocation) - rc.getLocation().distanceTo(targetLocation) < rc.getType().bodyRadius*3){
                                ignoreTarget = true;
                            }
                            else{
                                previousLocation=rc.getLocation();
                                nestPatience = nestPatienceInit;
                            }
                        }
                        else
                            nestPatience--;
                        pathTo(targetLocation);
                    }
                    else if(rc.getRoundNum() - birthday > 40)
                        wanderingRumba();
                    else
                	   moveForNest();
                    foundNest = trySpawnNest();
                    broadcastHandle.reportNotFoundNest();

                }
                else if(foundNest){
                    reportNeutralTreesInNest();
                	buildNest();
                	waterTrees();
                }
                spawnDudes();
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();
	        } 
        }
        catch (Exception e) {
            System.out.println("Gardener Exception");
            e.printStackTrace();
        }
    }

    private void aquireTarget() throws GameActionException{
        MapLocation target = broadcastHandle.getNestLocation();
        if(isValid(target)){
            broadcastHandle.resetNestLocation();
            targetAquired=true;
            targetLocation = target;
        }
    }

    private void reportNeutralTreesInNest() throws GameActionException{
        if(hasNestTrees){
            TreeInfo[] trees = rc.senseNearbyTrees(nestRange, Team.NEUTRAL);
            if(trees.length == 0){
                hasNestTrees = false;
            }
            for(TreeInfo tree : trees){
                broadcastHandle.reportNestTree(tree.location);
            }
        }
    }

    private boolean trySpawnNest() throws GameActionException{
    	MapLocation myPos = rc.getLocation();
    	nestPos = myPos;
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(nestRange);
        for(RobotInfo ri : nearbyRobots){
            if (ri.type == RobotType.GARDENER && ri.ID != rc.getID()){
                return false;
            }
        }
        TreeInfo[] nearbyTrees = rc.senseNearbyTrees(nestRange, rc.getTeam());
        if (nearbyTrees.length > 0)
            return false;
        
    	return rc.onTheMap(myPos, nestRange);
    }

    private void buildNest() throws GameActionException{
    	Direction dir = new Direction(nestPos, enemySpawn);
    	if(rc.canPlantTree(dir)){
    		rc.plantTree(dir);
    		return;
    	}
    	if(rc.canPlantTree(dir.rotateLeftRads(offsetAngle))){
    		rc.plantTree(dir.rotateLeftRads(offsetAngle));
    		return;
    	}
    	if(rc.canPlantTree(dir.rotateRightRads(offsetAngle))){
    		rc.plantTree(dir.rotateRightRads(offsetAngle));
    		return;
    	}
    	if(rc.canPlantTree(dir.rotateLeftRads(2*offsetAngle))){
    		rc.plantTree(dir.rotateLeftRads(2*offsetAngle));
    		return;
    	}
    	if(rc.canPlantTree(dir.rotateRightRads(2*offsetAngle))){
    		rc.plantTree(dir.rotateRightRads(2*offsetAngle));
    		return;
    	}

    }

    private void waterTrees() throws GameActionException{
    	TreeInfo[] trees = rc.senseNearbyTrees(type.bodyRadius + GameConstants.BULLET_TREE_RADIUS, rc.getTeam());
    	float minHealth = Float.MAX_VALUE;
    	int treeId = -1;
    	for(int i = 0; i < trees.length; i++){
    		if (trees[i].health < minHealth && rc.canWater(trees[i].ID)){
    			minHealth = trees[i].health;
    			treeId = i;
    		}
    	}
    	if (treeId != -1){ //&& trees[treeId].maxHealth - GameConstants.WATER_HEALTH_REGEN_RATE >= trees[treeId].health ){
    			rc.water(trees[treeId].ID);
    	}

    }

    private void shakeNeutralTrees() throws GameActionException{
        if(clearNeutral){
           TreeInfo[] interactableTrees = rc.senseNearbyTrees(rc.getType().bodyRadius + GameConstants.INTERACTION_DIST_FROM_EDGE, Team.NEUTRAL);
            if(interactableTrees.length > 0 && rc.canShake(interactableTrees[0].ID))
                rc.shake(interactableTrees[0].ID); 
            if(foundNest && interactableTrees.length == 0)
                clearNeutral = false;
        }
    }

    private void calculateOffsetAngle(){
    	double b = type.bodyRadius + GameConstants.BULLET_TREE_RADIUS;
    	double a = GameConstants.BULLET_TREE_RADIUS * 2;
    	double cosA = (2*Math.pow(b,2) - Math.pow(a,2))/(2*Math.pow(b,2));
    	offsetAngle = (float)Math.acos(cosA);
    }

    private void moveForNest() throws GameActionException{
        float x = 0.f;
        float y = 0.f;
        
        RobotInfo[] robots = rc.senseNearbyRobots(nestRange*2);

        for(int i = 0; i < robots.length; i++){
            float distance = rc.getLocation().distanceTo(robots[i].getLocation());
            if (robots[i].getID() != rc.getID() && robots[i].getType() == RobotType.GARDENER){
                x += ((-robots[i].getLocation().x + rc.getLocation().x)*(nestRange*2-distance))/(nestRange*2);
                y += ((-robots[i].getLocation().y + rc.getLocation().y)*(nestRange*2-distance))/(nestRange*2);
            }
        }
        
        TreeInfo[] trees = rc.senseNearbyTrees(nestRange, rc.getTeam());
        for(int i = 0; i < trees.length; i++){
            float distance = rc.getLocation().distanceTo(trees[i].getLocation());
            x += ((-trees[i].getLocation().x + rc.getLocation().x)*(nestRange-distance))/(nestRange);
            y += ((-trees[i].getLocation().y + rc.getLocation().y)*(nestRange-distance))/(nestRange);
        }

        if(!rc.onTheMap(rc.getLocation().add(Direction.NORTH, nestRange)))
            y -= 1;
        if(!rc.onTheMap(rc.getLocation().add(Direction.SOUTH, nestRange)))
            y += 1;
        if(!rc.onTheMap(rc.getLocation().add(Direction.WEST, nestRange)))
            x += 1;
        if(!rc.onTheMap(rc.getLocation().add(Direction.EAST, nestRange)))
            x -= 1;

        if (Math.pow(x,2) + Math.pow(y,2) < 0.1){
            x = enemySpawn.x - rc.getLocation().x;
            y = enemySpawn.y - rc.getLocation().y;
        }


        float angle = (float)Math.random() * 2 * (float)Math.PI;
        x += Math.cos(angle);
        y += Math.sin(angle);
        tryMove(new Direction(x,y));
        
    }

    private void spawnDudes() throws GameActionException{
        boolean spawnSoldier = broadcastHandle.shouldSpawnRobot(RobotType.SOLDIER);
        boolean spawnLumberjack = broadcastHandle.shouldSpawnRobot(RobotType.LUMBERJACK);

        if(spawnSoldier)
            spawnRobot(RobotType.SOLDIER);  
        if(spawnLumberjack)
            spawnRobot(RobotType.LUMBERJACK);
             
    }

    private void spawnRobot(RobotType type) throws GameActionException{
        if(rc.canBuildRobot(type, new Direction(enemySpawn, nestPos))){
            rc.buildRobot(type,new Direction(enemySpawn,nestPos));
            return;
        }
        for(float i = 0; i < (float)Math.PI*2; i+=(float)Math.PI/8){
            if (rc.canBuildRobot(type,new Direction(i)) ){
                rc.buildRobot(type,new Direction(i));
                break;
            }
        }
            
    }

}