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
	
	public static ArrayList<Position> rectilinear(ArrayList<Position> trees, double width, double height) {
		double x = SEED_RADIUS;
		double y = SEED_RADIUS;
		ArrayList<Position> Positions = new ArrayList<Position>();
		
		while (y <= height - SEED_RADIUS) {
			x = SEED_RADIUS;
			
			while (x <= width -  SEED_RADIUS) {
				if (!closeToTree(x, y, trees))
					Positions.add(new Position(x, y));
				
				x += 2* SEED_RADIUS;
			}
			
			y += 2* SEED_RADIUS;
		}
		
		return Positions;
	}
	
	public static ArrayList<Position> hexagonal(ArrayList<Position> trees, double width, double height) {
		double x =  SEED_RADIUS;
		double y =  SEED_RADIUS;
		boolean offset = false;
		
		ArrayList<Position> Positions = new ArrayList<Position>();
		
		while (y <= height -  SEED_RADIUS) {
			x =  SEED_RADIUS + (offset ?  SEED_RADIUS : 0);
			
			while (x <= width -  SEED_RADIUS) {
				if (!closeToTree(x, y, trees))
					Positions.add(new Position(x, y));
				
				x += 2* SEED_RADIUS;
			}
			
			y +=  SQRT_3* SEED_RADIUS;
			offset = !offset;
		}
		
		return Positions;
	}
}