package group9;
import battlecode.common.*;
import java.util.ArrayList;
import java.util.Random;
import java.lang.StringBuilder;
import java.lang.Math;

public strictfp class ConditionalNode extends BehaviorTreeNode {
	public enum Condition {EQ, NE, LT, GT, AEQ, LE, GE}
	private static Random random = new Random();
	public Condition condition;
	public int index1;
	public int index2;
	private static final float epsilon = 0.5f;
	public static final int numMemory = 9*3 + 7;
	public static final int numConditions = 2;
	
	public ConditionalNode(int condition, int index1, int index2) {
		this.condition = Condition.values()[condition % Condition.values().length];
		this.index1 = index1;
		this.index2 = index2;
	}
	
	public static ConditionalNode randConditionalNode() {
		return new ConditionalNode(random.nextInt(numConditions), random.nextInt(numMemory), random.nextInt(numMemory));
	}
	
	
	public boolean evaluate(RobotController rc, ArrayList<Float> memory, ArrayList<MapLocation> mapMemory) {
		if (index1 >= memory.size() || index2 >= memory.size()) {
			return false;
		}
		float num1 = memory.get(index1);
		float num2 = memory.get(index2);
		switch (condition) {
			case LT:
				return num1 < num2;
			case NE:
				return num1 != num2;
			case GT:
				return num1 > num2;
			case EQ:
				System.out.println("Conditional " + condition.ordinal() + ", " + index1 + "," + index2 + ") is " +
					(new Boolean(num1 == num2)).toString());
				return num1 == num2;
			case AEQ:
				return Math.abs(num1 - num2) < epsilon;
			case LE:
				return num1 <= num2;
			case GE:
				return num1 >= num2;
			default:
				throw new RuntimeException("Unknown Conditional Type");
		}
	}
	
	public void toGenome(StringBuilder builder) {
		builder.append("(" + condition.ordinal() + " " +
			index1 + " " + 
			index2 + ")");
	}
	
	public void addNodesToList(ArrayList<BehaviorTreeNode> list) {
		list.add(this);
	}
}