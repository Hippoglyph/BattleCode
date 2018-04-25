package group9;
import java.util.ArrayList;
import java.lang.StringBuilder;
import battlecode.common.*;
public abstract strictfp class BehaviorTreeNode {
	public boolean success;
	protected ArrayList<BehaviorTreeNode> children;
	
	public abstract boolean evaluate(RobotController rc, ArrayList<Float> memory, ArrayList<MapLocation> mapMemory);
	
	public abstract void toGenome(StringBuilder builder);
	
	public abstract void addNodesToList(ArrayList<BehaviorTreeNode> list);
}