package SOMbot;
import battlecode.common.*;

public class Gardener extends Unit{
	private boolean foundNest;
	private MapLocation nestPos;
	private float offsetAngle;

	public Gardener(RobotController rc){
        super(rc);
        foundNest = false;
        calculateOffsetAngle();
    }


    @Override
	public void run()  {

        // Try/catch blocks stop unhandled exceptions, which cause your robot to explode         
        try {
        // The code you want your robot to perform every round should be in this loop
	        while (true) {
                // Listen for home archon's location
                // Generate a random direction

                if (!foundNest){
                	foundNest = trySpawnNest();
                	tryMove(randomDirection());
                	
                }
                else if(foundNest){
                	buildNest();
                	waterTrees();
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
    	float dist = type.bodyRadius*3 + GameConstants.BULLET_TREE_RADIUS*2;
    	MapLocation myPos = rc.getLocation();
    	nestPos = myPos;
    	drawDebugCircle(myPos,dist);
    	return (rc.isCircleOccupiedExceptByThisRobot(myPos, dist) && rc.onTheMap(myPos, dist));
    }

    private void buildNest() throws GameActionException{
    	Direction dir = new Direction(nestPos, initArchonLocation);
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

}