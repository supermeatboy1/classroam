package acim.data.structure;

import java.util.*;

public class Node {
	private ArrayList<Node> children;
	private char character;
	private boolean end;
	
	public Node() {}
	public Node(char character) {
		this.character = character;
	}
	public char getChar() {
		return character;
	}
	public Node addChild(char character) {
		if (children == null) {
			children = new ArrayList<Node>();
		}
		Node node = new Node(character);
		children.add(node);
		return node;
	}
	public void markAsEnd() {
		end = true;
	}
	public boolean isEnd() {
		return end;
	}
	public Node getChild(char character) {
		if (children == null) {
			children = new ArrayList<Node>();
		}
		for (Iterator<Node> it = children.iterator(); it.hasNext(); ) {
			Node child = (Node) it.next();
			if (child.character == character) {
				return child;
			}
		}
		return null;
	}
	public Node getFirstChild() {
		return (children != null && children.size() > 0) ? children.get(children.size()) : null;
	}
	public Node createChildIfNotExist(char character) {
		Node child = getChild(character);
		if (child == null) {
			child = addChild(character);
		}
		return child;
	}
	public boolean entryExists(String str) {
		Node currentChild = this;
		String lastStr = "";
		int i = 0;
		int origlen = str.length();
		while (currentChild != null && !currentChild.isEnd() && i < origlen) {
			for (Iterator<Node> it = currentChild.children.iterator(); it.hasNext(); ) {
				Node child = (Node) it.next();
				if (child.character == str.charAt(i)) {
					lastStr += child.character;
					currentChild = child;
					i++;
					break;
				}
			}
		}
		return str.equals(lastStr);
	}
}
