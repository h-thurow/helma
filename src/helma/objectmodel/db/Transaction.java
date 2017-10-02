/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 2017 Daniel Ruthardt. All Rights Reserved.
 */

package helma.objectmodel.db;

import java.util.ArrayList;

public class Transaction {

	ArrayList<Node> insertedNodes = new ArrayList<Node>();
	ArrayList<Node> modifiedNodes = new ArrayList<Node>();
	ArrayList<Node> deletedNodes = new ArrayList<Node>();
	ArrayList<Node> updatedParentNodes = new ArrayList<Node>();
	ArrayList<Node> dirtyNodes = new ArrayList<Node>();

	public Transaction() {
	}

	public void addInsertedNode(Node insertedNode) {
		this.insertedNodes.add(insertedNode);
		this.dirtyNodes.add(insertedNode);
	}

	public void addModifiedNode(Node modifiedNode) {
		this.modifiedNodes.add(modifiedNode);
		this.dirtyNodes.add(modifiedNode);
	}

	public void addDeletedNode(Node deletedNode) {
		this.deletedNodes.add(deletedNode);
		this.dirtyNodes.add(deletedNode);
	}

	public void addUpdatedParentNode(Node updatedParentNode) {
		this.updatedParentNodes.add(updatedParentNode);
	}

	@SuppressWarnings("unchecked")
	public ArrayList<Node> getInsertedNodes() {
		return (ArrayList<Node>) this.insertedNodes.clone();
	}

	@SuppressWarnings("unchecked")
	public ArrayList<Node> getModifiedNodes() {
		return (ArrayList<Node>) this.modifiedNodes.clone();
	}

	@SuppressWarnings("unchecked")
	public ArrayList<Node> getDeletedNodes() {
		return (ArrayList<Node>) this.deletedNodes.clone();
	}

	@SuppressWarnings("unchecked")
	public ArrayList<Node> getUpdatedParentNodes() {
		return (ArrayList<Node>) this.updatedParentNodes.clone();
	}

	@SuppressWarnings("unchecked")
	public ArrayList<Node> getDirtyNodes() {
		return (ArrayList<Node>) this.dirtyNodes.clone();
	}

	public int getNumberOfInsertedNodes() {
		return this.insertedNodes.size();
	}

	public int getNumberOfModifiedNodes() {
		return this.modifiedNodes.size();
	}

	public int getNumberOfDeletedNodes() {
		return this.deletedNodes.size();
	}

	public int getNumberOfDirtyNodes() {
		return this.dirtyNodes.size();
	}

	public int getNumberOfUpdatedParentNodes() {
		return this.updatedParentNodes.size();
	}

}
