package watermelon.group2;

import java.util.*;

import watermelon.group2.Position;



public class Packing {
	public static final double SEED_RADIUS = 1.0;
	public static final double TREE_RADIUS = 1.0;
	public static final double SQRT_3 = 1.732051;
	
	private static boolean closeToTree(double x, double y, ArrayList<Position> trees) {
		for (Position tree : trees) {
			if ( (tree.x - x)*(tree.x - x) + (tree.y - y)*(tree.y - y) < (( SEED_RADIUS +  TREE_RADIUS) * ( SEED_RADIUS +  TREE_RADIUS)))
				return true;
		}
		
		return false;
	}
	
	private static boolean closeToTree(Position position, ArrayList<Position> trees) {
		return closeToTree(position.x, position.y, trees);
	}
	
	public static ArrayList<Position> rectilinear(ArrayList<Position> trees, double width, double height) {
		double x =  SEED_RADIUS;
		double y =  SEED_RADIUS;
		ArrayList<Position> locations = new ArrayList<Position>();
		
		while (y <= height -  SEED_RADIUS) {
			x =  SEED_RADIUS;
			
			while (x <= width -  SEED_RADIUS) {
				if (!closeToTree(x, y, trees))
					locations.add(new Position(x, y));
				
				x += 2* SEED_RADIUS;
			}
			
			y += 2* SEED_RADIUS;
		}
		
		return locations;
	}
	
	public static ArrayList<Position> hexagonal(ArrayList<Position> trees, double width, double height) {
		double x =  SEED_RADIUS;
		double y =  SEED_RADIUS;
		boolean offset = false;
		
		ArrayList<Position> locations = new ArrayList<Position>();
		
		while (y <= height -  SEED_RADIUS) {
			x =  SEED_RADIUS + (offset ?  SEED_RADIUS : 0);
			
			while (x <= width -  SEED_RADIUS) {
				if (!closeToTree(x, y, trees))
					locations.add(new Position(x, y));
				
				x += 2* SEED_RADIUS;
			}
			
			y +=  SQRT_3* SEED_RADIUS;
			offset = !offset;
		}
		
		return locations;
	}
	
	public static void treeExpansion(ArrayList<Position> trees, Position start, double width, double height, ArrayList<Position> board) {
		
		if (start == null) {
			ArrayList<Position> treesCopy = new ArrayList<Position>(trees);
			Collections.shuffle(treesCopy);
			
			for (Position treePosition : treesCopy) {
				for (Position position : surroundingPositionsForPosition(treePosition)) {
					if (positionIsInsideBoard(position, width, height) && !closeToTree(position, board) && !closeToTree(position, trees)) {
						board.add(position);
						treeExpansion(trees, position, width, height, board);
					}
				}
			}
		} else {
			for (Position position : surroundingPositionsForPosition(start)) {
				if (positionIsInsideBoard(position, width, height) && !closeToTree(position, board) && !closeToTree(position, trees)) {
					board.add(position);
					treeExpansion(trees, position, width, height, board);
				}
			}
		}
	}
	
	public static void treeExpansionBreadthFirst(ArrayList<Position> trees, Position start, double width, double height, ArrayList<Position> board) {
		Queue<Position> queue = new LinkedList<Position>();
		for (Position treePosition : trees) {
			queue.addAll(surroundingPositionsForPosition(treePosition));
		}
		
		while (queue.peek() != null) {
			Position position = queue.remove();
			
			if (positionIsInsideBoard(position, width, height) && !closeToTree(position, board) && !closeToTree(position, trees)) {
				board.add(position);
				queue.addAll(surroundingPositionsForPosition(position));
			}
		}
	}

	
	public static boolean positionIsInsideBoard(Position position, double width, double height) {
		return (position.x >= SEED_RADIUS && position.x <= width - SEED_RADIUS && position.y >= SEED_RADIUS && position.y <= height - SEED_RADIUS);
	}
	
	public static ArrayList<Position> surroundingPositionsForPosition(Position position) {
		ArrayList<Position> positions = new ArrayList<Position>();
		
		double longEdge = SQRT_3 * SEED_RADIUS;
		double shortEdge = SEED_RADIUS;
		
		double x = position.x;
		double y = position.y;
		
		positions.add(new Position(x, y + 2 * SEED_RADIUS));
		positions.add(new Position(x + longEdge, y + shortEdge));
		positions.add(new Position(x + longEdge, y - shortEdge));
		positions.add(new Position(x, y - 2 * SEED_RADIUS));
		positions.add(new Position(x - longEdge, y - shortEdge));
		positions.add(new Position(x - longEdge, y + shortEdge));
		
		return positions;
	}
}
