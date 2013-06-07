package edu.mines.acmX.exhibit.stdlib.graphics;


/**
 * This associates a uniquely-identified hand to its 3-dimensional position. 
 * @author Aakash Shah
 * @author Ryan Stauffer
 *
 */
public class HandPosition {
	private int id;
	private Coordinate3D position;
	
	public HandPosition(int id, Coordinate3D pos) {
		this.id = id;
		this.position = pos;
	}
	
	public int getId() {
		return id;
	}

	public Coordinate3D getPosition() {
		return position;
	}
}
