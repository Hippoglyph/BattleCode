package group9;

import java.lang.StringBuilder;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.io.*;
import battlecode.common.*;

public strictfp class BehaviorTree implements Serializable {
	private BehaviorTreeNode root;
	public float score;
	public static String defaultTree = "[[<8 0>][{(0 40 10)[<14 2><5 2>]}(0 40 10){(0 40 10)<3 2>}<10 0><4 2><0 0>]]";
	
	public void readObject(ObjectInputStream aInputStream) throws ClassNotFoundException, IOException {
		score = aInputStream.readFloat();
		int length = aInputStream.readInt();
		String genome = "";
		for (int i = 0; i < length ;i++) { 
			genome += aInputStream.readChar();
		}
		Scanner scanner = new Scanner(genome);
		root = parseNode(scanner);
	}
	
	public void writeObject(ObjectOutputStream aOutputStream) throws IOException {
		String genome = toGenome();
		aOutputStream.writeFloat(score);
		aOutputStream.writeInt(genome.length());
		aOutputStream.writeChars(genome);
	}
	
	public BehaviorTreeNode parseNode(Scanner scanner) {
		char readChar = scanner.nextChar();
		try {
			switch(readChar) {
				case '<':
					ActionNode node1 = new ActionNode(scanner.nextInt(), scanner.nextInt());
					if (scanner.nextChar() != '>') {
						throw new InputMismatchException("Invalid genome (action ended improperly)");
					}
					return node1;
				case '(':
					ConditionalNode node2 = new ConditionalNode(scanner.nextInt(), scanner.nextInt(), scanner.nextInt());
					if (scanner.nextChar() != ')') {
						throw new InputMismatchException("Invalid genome (conditional ended improperly)");
					}
					return node2;
				case '[':
					SwitchNode node3 = new SwitchNode();
					while (!scanner.hasNextChar(']')) {
						node3.addChild(parseNode(scanner));
					}
					scanner.nextChar();
					return node3;
				case '{':
					SequenceNode node4 = new SequenceNode();
					while (!scanner.hasNextChar('}')) {
						node4.addChild(parseNode(scanner));
					}
					scanner.nextChar();
					return node4;
				default:
					throw new InputMismatchException("Invalid genome (unrecognized node type)");
			}
		}
		catch (NoSuchElementException e) {
			System.out.println("Invalid genome");
			return null;
		}
	}
	
	public BehaviorTree(String genome) {
		System.out.println("Genome is \"" + genome + "\"");
		Scanner scanner = new Scanner(genome);
		root = parseNode(scanner);
	}
	
	public String toGenome() {
		StringBuilder builder = new StringBuilder(500);
		root.toGenome(builder);
		return builder.toString();
	}
	
	public void runTree(RobotController rc, ArrayList<Float> memory, ArrayList<MapLocation> mapMemory) {
		root.evaluate(rc, memory, mapMemory);
	}
	
	private ArrayList<BehaviorTreeNode> getNodes() {
		ArrayList<BehaviorTreeNode> ret = new ArrayList<BehaviorTreeNode>();
		root.addNodesToList(ret);
		return ret;
	}
	
	public static BehaviorTree mutate(BehaviorTree old) {
		Random random = new Random();
		ArrayList<BehaviorTreeNode> nodes = old.getNodes();
		BehaviorTreeNode selected = nodes.get(random.nextInt(nodes.size()));
		float rand = new Random().nextFloat();
		if (selected instanceof ActionNode) {
			if (rand < 0.5) {
				((ActionNode)selected).index = random.nextInt(ActionNode.numMemory);
			}
			else {
				((ActionNode)selected).actionType = random.nextInt(ActionNode.numActions);
			}
		}
		else if (selected instanceof ConditionalNode) {
			if (rand < 0.33) {
				((ConditionalNode)selected).condition = ConditionalNode.Condition.values()[random.nextInt(ConditionalNode.numConditions)];
			}
			else if (rand < 0.66) {
				((ConditionalNode)selected).index1 = random.nextInt(ConditionalNode.numMemory);
			}
			else {
				((ConditionalNode)selected).index2 = random.nextInt(ConditionalNode.numMemory);
			}
		}
		else if (selected instanceof SwitchNode || selected instanceof SequenceNode) {
			ArrayList<BehaviorTreeNode> children;
			if (selected instanceof SwitchNode) {
				children = ((SwitchNode)selected).children;
			}
			else {
				children = ((SequenceNode)selected).children;
			}
			if (rand < 0.1 && children.size() > 0) {
				children.remove(random.nextInt(children.size()));
			}
			else if (rand < 0.3) {
				children.add(new SwitchNode());
			}
			else if (rand < 0.5) {
				children.add(new SequenceNode());
			}
			else if (rand < 0.7) {
				children.add(ConditionalNode.randConditionalNode());
			}
			else {
				children.add(ActionNode.randActionNode());
			}
		}
		return new BehaviorTree(defaultTree);
	}
}