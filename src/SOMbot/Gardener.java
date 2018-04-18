package SOMbot;
import battlecode.common.*;

public class Gardener extends Unit{
	private boolean foundNest;
	private MapLocation nestPos;
	private float offsetAngle;
    private float nestRange;

	public Gardener(RobotController rc){
        super(rc);
        foundNest = false;
        calculateOffsetAngle();
        nestRange = type.bodyRadius*3 + GameConstants.BULLET_TREE_RADIUS*2;
    }


    @Override
	public void run()  {
        boolean spawned = false;
        // Try/catch blocks stop unhandled exceptions, which cause your robot to explode         
        try {
        // The code you want your robot to perform every round should be in this loop
	        while (true) {
                // Listen for home archon's location
                // Generate a random direction

                if (!foundNest){
                	foundNest = trySpawnNest();
                	//tryMove(randomDirection());
                    if(rc.getRoundNum() - birthday > 40)
                        wanderingRumba();
                    else
                	   moveForNest();

                }
                else if(foundNest){
                	buildNest();
                	waterTrees();
                }

                float soldierSpawn = (float)Math.random();
                float lumberJackSpawn = (float)Math.random();

                if (rc.getTeamBullets() > 400 && rc.canBuildRobot(RobotType.SOLDIER,new Direction(enemySpawn,nestPos)) && soldierSpawn < 0.0)
                    rc.buildRobot(RobotType.SOLDIER,new Direction(enemySpawn,nestPos));
                if (rc.getTeamBullets() > 400 && rc.canBuildRobot(RobotType.LUMBERJACK,new Direction(enemySpawn,nestPos)) && lumberJackSpawn < 1.0 && !spawned){
                    spawned = true;
                    rc.buildRobot(RobotType.LUMBERJACK,new Direction(enemySpawn,nestPos));
                }
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();
	        } 
        }
        catch (Exception e) {
            System.out.println("Gardener Exception");
            e.printStackTrace();
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
            if (robots[i].getID() != rc.getID()){
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

}