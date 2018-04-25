package group9;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.lang.StringBuilder;
import java.lang.Math;
import battlecode.common.*;
public strictfp class ActionNode extends BehaviorTreeNode {
	public int actionType;
	public static final int numActions = 14;
	public static final int numMemory = 9;
	int index;
	private static Random random = new Random();
	Direction dir;
	MapLocation target;
	float radians;
	float x;
	float y;
	public ActionNode(int actionType, int index) {
		this.actionType = actionType;
		this.index = index;
	}
	
	public static ActionNode randActionNode() {
		return new ActionNode(random.nextInt(numActions), random.nextInt(numMemory));
	}
	public boolean evaluate(RobotController rc, ArrayList<Float> memory, ArrayList<MapLocation> mapMemory) {
		float param1 = memory.get(index);
		Direction paramDirection = new Direction(param1);
		try {
			switch(actionType) {
				case 0: //move randomly
					for (int i = 0; i < 5; i++) {
						dir = RobotPlayer.randomDirection();
						if (rc.canMove(dir)) {
							rc.move(dir);
							System.out.println("Moved in direction" + dir.radians);
							return true;
						}
						else {
							System.out.println("Robot " + rc.getID() + " Could not move in direction" + dir.radians);
						}
					}
					return false;
				case 1: //move towards			
					target = mapMemory.get(index);
					if (rc.canMove(target)) {
						rc.move(target);
						return true;
					}
					else {
						return false;
					}
				case 2: //move away
					target = mapMemory.get(index);
					y = rc.getLocation().y - target.y;
					x = rc.getLocation().x - target.x;
					radians = (float)(Math.atan2(y,x));
					dir = new Direction(radians);
					if (rc.canMove(dir)) {
						rc.move(dir);
						return true;
					}
					else {
						return false;
					}
				case 3: //fire
					target = mapMemory.get(index);
					y = target.y - rc.getLocation().y;
					x = target.x - rc.getLocation().x;
					radians = (float)(Math.atan2(y,x));
					dir = new Direction(radians);
					if (rc.canFireSingleShot()) {
						rc.fireSingleShot(paramDirection);
						return true;
					}
					else {
						return false;
					}
				case 4: //plantTree
					if (rc.canPlantTree(paramDirection)) {
						rc.plantTree(paramDirection);
						return true;
					}
					else {
						return false;
					}
				case 5: //water tree
					
					if (rc.canWater()) {
						TreeInfo[] trees = rc.senseNearbyTrees(1.5f, rc.getTeam());
						
						if (trees.length > 0 && rc.canWater(trees[0].getLocation())) {
							try {
								rc.water(trees[0].getLocation());
								System.out.println("WATERED TREE");
								return true;
							} catch (GameActionException e) {
							
							}
						}
					} else {				
						return false;
					}
				case 6: //chop tree
					if (rc.canChop(rc.getLocation())) {
						rc.chop(rc.getLocation());
						return true;
					}
					else {
						return false;
					}
				case 7: //shake tree
					if (rc.canShake(rc.getLocation())) {
						rc.shake(rc.getLocation());
						return true;
					}
					else {
						return false;
					}
				case 8: //hire gardener
					if (rc.canHireGardener(paramDirection)) {
						rc.hireGardener(paramDirection);
						System.out.println("Gardener hired at " + param1);
						return true;
					}
					else {
						return false;
					}
					case 9: //hire lumberjack
					if (rc.canBuildRobot(RobotType.LUMBERJACK, paramDirection)) {
						rc.buildRobot(RobotType.LUMBERJACK, paramDirection);
						System.out.println("Lumberjack hired at " + param1);
						return true;
					}
					else {
						return false;
					}
				case 10: //hire soldier
					if (rc.canBuildRobot(RobotType.SOLDIER, paramDirection)) {
						rc.buildRobot(RobotType.SOLDIER, paramDirection);
						return true;
					}
					else {
						return false;
					}
				case 11: //hire tank
					if (rc.canBuildRobot(RobotType.TANK, paramDirection)) {
						rc.buildRobot(RobotType.TANK, paramDirection);
						return true;
					}
					else {
						return false;
					}
				case 12: //hire scout
					if (rc.canBuildRobot(RobotType.SCOUT, paramDirection)) {
						rc.buildRobot(RobotType.SCOUT, paramDirection);
						return true;
					}
					else {
						return false;
					}
					//ideas: broadcast or read broadcast, sense nearby robots into mapLocation
				case 13: //strike
					if (rc.canStrike()) {
						rc.strike();
						return true;
					}
					else {
						return false;
					}
				case 14: //move direction
					if (rc.canMove(paramDirection)) {
						rc.move(paramDirection, 0.1f);
						return true;
					}
					else {
						System.out.println("Robot " + rc.getID() + " Could not move in direction" + paramDirection.radians);
						return false;
					}
				default:
					throw new RuntimeException("Unknown action type");
			}
		}
		catch (GameActionException e) {
			return false;
		}
		//throw new RuntimeException("Somehow escaped try in ActionNode");
	}
	
	public void toGenome(StringBuilder builder) {
		builder.append("<" + actionType + " " +  index +">");
	}
	
	public void addNodesToList(ArrayList<BehaviorTreeNode> list) {
		list.add(this);
	}
}