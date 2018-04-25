package group9;
import java.util.ArrayList;
import java.lang.StringBuilder;
import battlecode.common.*;

public strictfp class SequenceNode extends BehaviorTreeNode {
	public SequenceNode() {
		children = new ArrayList<BehaviorTreeNode>();
	}
	
	public void addChild(BehaviorTreeNode node) {
		children.add(node);
	}
	public boolean evaluate(RobotController rc, ArrayList<Float> memory, ArrayList<MapLocation> mapMemory) {
		for (BehaviorTreeNode node : children) {
			boolean ret = node.evaluate(rc, memory, mapMemory);
			if (!ret) {
				return false;
			}
		}
		return true;
	}
	
	public void toGenome(StringBuilder builder) {
		builder.append('{');
		for (BehaviorTreeNode node : children) {
			node.toGenome(builder);
		}
		builder.append('}');
	}
	
	public void addNodesToList(ArrayList<BehaviorTreeNode> list) {
		list.add(this);
		for (BehaviorTreeNode node : children) {
			node.addNodesToList(list);
		}
	}
}