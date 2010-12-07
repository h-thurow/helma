package helma.objectmodel.db;

import helma.objectmodel.INode;

import java.util.ArrayList;
import java.util.Iterator;

public class Transaction {

	ArrayList<Node> insertedNodes = new ArrayList<Node>();
	ArrayList<Node> modifiedNodes = new ArrayList<Node>();
	ArrayList<Node> deletedNodes = new ArrayList<Node>();
	ArrayList<Node> updatedParentNodes = new ArrayList<Node>();
	ArrayList<Node> dirtyNodes = new ArrayList<Node>();
	
	public Transaction() {	
	}
	
	public void addInsertedNode(Node insertedNode) {
		insertedNodes.add(insertedNode);
		dirtyNodes.add(insertedNode);
	}
	
	public void addModifiedNode(Node modifiedNode) {
		modifiedNodes.add(modifiedNode);
		dirtyNodes.add(modifiedNode);
	}
	
	public void addDeletedNode(Node deletedNode) {
		deletedNodes.add(deletedNode);
		dirtyNodes.add(deletedNode);
	}
	
	public void addUpdatedParentNode(Node updatedParentNode) {
		updatedParentNodes.add(updatedParentNode);
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<Node> getInsertedNodes() {
		return (ArrayList<Node>) insertedNodes.clone();
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<Node> getModifiedNodes() {
		return (ArrayList<Node>) modifiedNodes.clone();
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<Node> getDeletedNodes() {
		return (ArrayList<Node>) deletedNodes.clone();
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<Node> getUpdatedParentNodes() {
		return (ArrayList<Node>) updatedParentNodes.clone();
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<Node> getDirtyNodes() {
		return (ArrayList<Node>) dirtyNodes.clone();
	}
	
	public int getNumberOfInsertedNodes() {
		return insertedNodes.size();
	}
	
	public int getNumberOfModifiedNodes() {
		return modifiedNodes.size();
	}
	
	public int getNumberOfDeletedNodes() {
		return deletedNodes.size();
	}
	
	public int getNumberOfDirtyNodes() {
		return dirtyNodes.size();
	}
	
	public int getNumberOfUpdatedParentNodes() {
		return updatedParentNodes.size();
	}
	
}