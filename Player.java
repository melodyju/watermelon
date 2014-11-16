package watermelon.group8;

import java.util.*;

import watermelon.group2.Packing;
import watermelon.group2.Position;
import watermelon.sim.Pair;
import watermelon.sim.Point;
import watermelon.sim.seed;

public class Player extends watermelon.sim.Player {
	static double distowall = 2.1;
	static double distotree = 2.2;
	static double distoseed = 2.01;
	static double SEED_RADIUS = 1.0;
	static double distToWall= 1.0;
	static double distToSeed = 2.0000000000000001;
	static double distToTree = 2.0000000000000001;
	static ArrayList<seed> globalBoard;

	static double boardWidth;
	static double boardHeight;
	static double s;
	static ArrayList<Pair> treeList;

	static int k;
	static Individual best;

	private static final int generationSize = 4;
	private static final int numGenerations = 30;
	private static final int childPolicy = 15;

	enum Region {
		TOP, BOTTOM, LEFT, RIGHT
	}

	/////////////////////////////////////////////////////////////////////////////////////
	// Generation and Individual classes
	static boolean closeToTree(Pair p, ArrayList<Pair> treeLst){
		for(Pair q : treeLst){
			if(Math.sqrt((p.x - q.x) * (p.x - q.x) + (p.y - q.y) * (p.y - q.y)) < distToTree){
				return true;
			}
		}
		return false;
	}
	// Generation class 
	class Generation {
		public double totalFitness;
		public ArrayList<Individual> population;

		Generation(ArrayList<Individual> individuals) {
			this.population = individuals;
			totalFitness = calculateTotalFitness();
		}

		Generation(Generation parentGeneration) {
			this.population = spawnChildren(parentGeneration);
			totalFitness = calculateTotalFitness();
		}

		Generation() {
			this.population = initFirstGeneration();
			totalFitness = calculateTotalFitness();
		}

		Individual selectParent() {
			// return rouletteWheelSelection();
			return probabilisticSelection();
		}

		Individual rouletteWheelSelection() {
			Random random = new Random();
			double rnd = random.nextDouble() * this.totalFitness;
			int j;
			for (j = 0; j < generationSize && rnd > 0; j++) {
				rnd -= population.get(j).fitness;
			}
			return population.get(j-1);
		}

		Individual probabilisticSelection() {
			Individual chosenOne = null;
			Random random = new Random();
			while (chosenOne == null) {
				int candidateIndex = random.nextInt(generationSize);
				Individual candidate = population.get(candidateIndex);
				if (candidate.chosen(totalFitness)) {
					chosenOne = candidate;
				}
			}
			return chosenOne;
		}

		double calculateTotalFitness() {
			double total = 0;
			for (Individual individual : population) {
				total += individual.fitness;
			}
			return total;
		}

		ArrayList<Individual> initFirstGeneration() {
			ArrayList<Individual> individuals = new ArrayList<Individual>();
			for (int i = 0; i < generationSize; i++) {
				Individual individual = new Individual();
				individuals.add(individual);
			}
			return individuals;
		}

		ArrayList<Individual> spawnChildren(Generation parentGeneration) {
			ArrayList<Individual> children = new ArrayList<Individual>();

			Individual parent1; //parent 1 of children[i]
			Individual parent2; //parent 2 of children[i]
			ArrayList<Individual> allChildren; //all children from these 2 parents
			Individual fittestChild; //fittest child from these 2 parents -> children[i]

			for (int i = 0; i < generationSize; i++) {
				parent1 = parentGeneration.selectParent();
				parent2 = parentGeneration.selectParent();
				//parents reproduce 
				allChildren = parent1.reproduce(parent2);
				//add their fittest child to the list of children
				fittestChild = fittestIndividual(allChildren);
				children.add(i, fittestChild);
			}

			return children;
		}

		Individual fittestIndividual(ArrayList<Individual> population) {
			double maxFitness = 0;
			Individual fittestIndividual = null;
			for (Individual individual : population) {
				double fitness = individual.fitness;
				if (fitness > maxFitness) {
					maxFitness = fitness;
					fittestIndividual = individual;
				}
			}
			return fittestIndividual;
		}

	}

	// Individual class
	class Individual {
		public double fitness;
		public ArrayList<seed> board;

		
		Individual(ArrayList<seed> board) {
			this.board = board;
			fitness = calculateFitness();
		}

		Individual() {
			this.board = generateRandomBoard();
			fitness = calculateFitness();
		}

		double calculateFitness() {
			double total = 0;
			for (int i = 0; i < board.size(); i++) {
				double score;
				double chance = 0.0;
				double totaldis = 0.0;
				double difdis = 0.0;
				for (int j = 0; j < board.size(); j++) {
					if (j != i) {
						totaldis = totaldis + Math.pow(distanceBetweenSeeds(board.get(i), board.get(j)), -2);
					}
				}
				for (int j = 0; j < board.size(); j++) {
					if (j != i && ((board.get(i).tetraploid && !board.get(j).tetraploid) || 
						(!board.get(i).tetraploid && board.get(j).tetraploid))) {
						difdis = difdis + Math.pow(distanceBetweenSeeds(board.get(i), board.get(j)), -2);
					}
				}
				chance = difdis / totaldis;
				score = chance + (1 - chance) * s;
				total = total + score;
			}
			return total;
		}

		// void mutate(Individual parent1, Individual parent2) {
		// 	Random random = new Random();
		// 	int patches = 
		// }

		ArrayList<Individual> reproduce(Individual parent2) {
			ArrayList<Individual> children = new ArrayList<Individual>();

			ArrayList<seed> board1 = this.board;
			ArrayList<seed> board2 = parent2.board;

			Random random = new Random();

			for (int i = 0; i < childPolicy; i++) {
				//randomly select which parent is 1 and which parent is 2
				int swap = random.nextInt(2);
				if (swap == 1) {
					board1 = parent2.board;
					board2 = this.board;
				}

				int xBoundary = random.nextInt((int)boardWidth);
				int yBoundary = random.nextInt((int)boardHeight);
				ArrayList<seed> board = new ArrayList<seed>();
				board.addAll(getSeedsInRegion(board1, 0, xBoundary, 0, yBoundary));
				board.addAll(getSeedsInRegion(board2, xBoundary, boardWidth, 0, boardHeight));
				board.addAll(getSeedsInRegion(board2, 0, xBoundary, yBoundary, boardHeight));

				children.add(new Individual(board));
			}
			
			return children;
		}

		boolean chosen(double totalFitness) {
			double proportion = this.fitness / totalFitness;
			Random random = new Random();
			double d = random.nextDouble();
			if (d <= proportion) {
				return true;
			}
			else {
				return false;
			}
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////
	// Game stuff

	@Override
	public ArrayList<seed> move(ArrayList<Pair> treelist, double width, double length, double s) {
		// TODO Auto-generated method stub
		this.boardWidth = width;
		this.boardHeight = length;
		this.s = s;
		this.treeList = treelist;

		this.best = new Individual();

		Individual winner = runGeneticAlgorithm();
		return winner.board;
	}

	// Returns the final board from Genetic Algorithm 
	public Individual runGeneticAlgorithm() {
		Generation parents = null;
		Generation children = null;

		// Initialize Generation 0 of random boards
		k = 0;
		parents = new Generation();

		while (shouldTerminate() == false) {
			System.out.println("Generation " + k);
			System.out.println(parents.totalFitness);
			// Create generation k + 1
			children = new Generation(parents);

			k++;
			// Check to see if the fittest of this generation is fitter than the GOAT
			Individual fittestChild = children.fittestIndividual(children.population);
			if (fittestChild.fitness > best.fitness) {
				best = fittestChild;
			}
			// Children become the parents of the next generation
			parents = children;
		}

		// Return the fittest person in the most current generation
		return best;
	}

	public boolean shouldTerminate() {
		if (k > numGenerations) {
			return true;
		}
		return false;
	}

	/////////////////////////////////////////////////////////////////////////////////////
	// Helper methods

		public ArrayList<seed> generateRandomBoard() {
			ArrayList<seed> seedlist = new ArrayList<seed>();
			ArrayList<Position> trees = new ArrayList<Position>();

			for (Pair p : treeList)
				trees.add(new Position(p.x, p.y));

			ArrayList<Position> locations = new ArrayList<Position>();

			if (trees.size() > 0) {
				for (int i = 0; i < 1000; i++) {
					ArrayList<Position> workingLocations = new ArrayList<Position>();
					Packing.treeExpansion(trees, null, boardWidth, boardHeight, workingLocations);
					if (workingLocations.size() > locations.size()) {
						locations = new ArrayList<Position>(workingLocations);
					}
				}


				Packing.treeExpansionBreadthFirst(trees, null, boardWidth, boardHeight, locations);

				Packing.pullToTrees(locations, trees, boardWidth, boardHeight);
				fillInSpace(trees,boardWidth,boardHeight,locations);	

				ArrayList<Position> hexLocations = Packing.hexagonal(trees, boardWidth, boardHeight);
				Packing.treeExpansionBreadthFirst(trees, null, boardWidth, boardHeight, hexLocations);
				Packing.pullToTrees(hexLocations, trees, boardWidth, boardHeight);
				fillInSpace(trees, boardWidth, boardHeight, hexLocations);
				if (hexLocations.size() > locations.size()) {
	 			System.out.println("hex packing chosen");
					locations = hexLocations;
				}

				//ArrayList<Position> locations = Packing.hexagonal(trees, boardWidth, boardHeight);
				System.out.println("Board has " + locations.size() + " seeds.");
			}

			else {
				locations = Packing.hexagonal(trees, boardWidth, boardHeight);
				fillInSpace(trees,boardWidth,boardHeight,locations);
				System.out.println("Board has " + locations.size() + " seeds.");
			}

			
			return generateRandomBoardFromPositions(locations);
		}
	
	public static void fillInSpace(ArrayList<Position> trees, double width, double height, ArrayList<Position> board)
	{
		Position nextSeed = getNextAvailableSeed(board, treeList, 1,width,height);
		while(nextSeed != null){
			board.add(nextSeed);
			nextSeed = getNextAvailableSeed(board, treeList, 1,width,height);
		}

		nextSeed = getNextAvailableSeed(board, treeList, 0,width,height);
		while(nextSeed != null){
			board.add(nextSeed);
			nextSeed = getNextAvailableSeed(board, treeList, 0,width,height);
		}

		nextSeed = getNextAvailableSeed(board, treeList, 2,width,height);
		while(nextSeed != null){
			board.add(nextSeed);
			nextSeed = getNextAvailableSeed(board, treeList, 2,width,height);}
	}

	public static Position getNextAvailableSeed(ArrayList<Position> board, ArrayList<Pair> treeLst, int direction,double width, double height){
		if(board == null || board.size() == 0){
			System.out.println("empty List");
			return null;
		}
		Position minSeed = null; 
		for(Position s : board){
			Position temp = null;
			switch (direction){
			case 0:
				temp = getNewSeedDown(s, board, treeList,width,height);
				// curernt seed has avaiable neighboor
				if(temp != null){
					if(minSeed == null || minSeed.y < temp.y){
						minSeed = temp;
					}
				}
				break;
			case 1:
				temp = getNewSeedUp(s, board, treeList,width,height);
				// curernt seed has avaiable neighboor
				if(temp != null){
					if(minSeed == null || minSeed.y < temp.y){
						minSeed = temp;
					}
				}
				break;
			case 2:
				temp = getNewSeedLeft(s, board, treeList,width,height);
				// curernt seed has avaiable neighboor
				if(temp != null){
					if(minSeed == null || minSeed.y < temp.y){
						minSeed = temp;
					}
				}
				break;
			}
		}
		if(minSeed == null){
			System.out.println("minSeed is null");
		} else {
			System.out.println("minSeed: (" + minSeed.x + "):(" + minSeed.y + ")");
		}
		return minSeed;
	}

	public static Position getNewSeedDown(Position s2, ArrayList<Position> board, ArrayList<Pair> treeLst,double width, double height){
		//System.out.println(seedLst.size());
		// two set of angles
		double x, y;
		// check with increasing height order
		for(int i = 0; i <= 180; i += 1){
			x = s2.x + distToSeed * Math.cos(Math.toRadians((360 + 180 - i) % 360));
			y = s2.y + -distToSeed * Math.sin(Math.toRadians((360 + 180 - i) % 360));
			// if left node is valid
			if(x >= distToWall && x <= width - distToWall && y >= distToWall && y <= height - distToWall && !closeToTree(new Pair(x, y), treeLst)){
				boolean flag = false;
				for(Position s : board){
					if(distance(s, new Pair(x, y)) < 2.0){
						flag = true;
						break;
					}
				}
				if(!flag){
					return new Position(x, y);
				}        
			}
			// if right is valid
			x = s2.x + distToSeed * Math.cos(Math.toRadians(i));
			y = s2.y + -distToSeed * Math.sin(Math.toRadians(i));
			if(x >= distToWall && x <= width - distToWall && y >= distToWall && y <= height - distToWall && !closeToTree(new Pair(x, y), treeLst)){
				boolean flag = false;
				for(Position s : board){
					if(distance(s, new Pair(x, y)) < 2.0){
						flag = true;
						break;
					}
				}
				if(!flag){
					return new Position(x, y);
				}        
			}
		}
		return null;
	}

	public static Position getNewSeedUp(Position s, ArrayList<Position> board, ArrayList<Pair> treeLst,double width, double height){
		//System.out.println(seedLst.size());
		// two set of angles
		double x, y;
		// check with increasing height order
		for(int i = 0; i <= 180; i += 1){
			x = s.x + distToSeed * Math.cos(Math.toRadians((90 - i + 360) % 360));
			y = s.y + -distToSeed * Math.sin(Math.toRadians((90 - i + 360) % 360));
			// if right node is valid
			if(x >= distToWall && x <= width - distToWall && y >= distToWall && y <= height - distToWall && !closeToTree(new Pair(x, y), treeLst)){
				boolean flag = false;
				for(Position sd : board){
					if(distance(sd, new Pair(x, y)) < 2.0){
						flag = true;
						break;
					}
				}
				if(!flag){
					return new Position(x,y);//rnd.nextInt(2) == 0 ? true : false);
				}        
			}
			// if right is valid
			x = s.x + distToSeed * Math.cos(Math.toRadians((90 + i) % 360));
			y = s.y + -distToSeed * Math.sin(Math.toRadians((90 + i) % 360));
			if(x >= distToWall && x <= width - distToWall && y >= distToWall && y <= height - distToWall && !closeToTree(new Pair(x, y), treeLst)){
				boolean flag = false;
				for(Position sd : board){
					if(distance(sd, new Pair(x, y)) < 2.0){
						flag = true;
						break;
					}
				}
				if(!flag){
					return new Position(x,y);//rnd.nextInt(2) == 0 ? true : false);
				}        
			}
		}
		return null;
	}


	public static Position getNewSeedLeft(Position s, ArrayList<Position> board, ArrayList<Pair> treeLst,double width, double height){
		//System.out.println(seedLst.size());
		// two set of angles
		double x, y;
		// check with increasing height order
		for(int i = 0; i <= 180; i += 1){
			x = s.x + distToSeed * Math.cos(Math.toRadians(180-i));
			y = s.y + -distToSeed * Math.sin(Math.toRadians(180-i));
			// if left node is valid
			if(x >= distToWall && x <= width - distToWall && y >= distToWall && y <= height - distToWall && !closeToTree(new Pair(x, y), treeLst)){
				boolean flag = false;
				for(Position sd : board){
					if(distance(sd, new Pair(x, y)) < 2.0){
						flag = true;
						break;
					}
				}
				if(!flag){
					return new Position(x,y);//rnd.nextInt(2) == 0 ? true : false);
				}        
			}
			// if right is valid
			x = s.x + distToSeed * Math.cos(Math.toRadians(180+i));
			y = s.y + -distToSeed * Math.sin(Math.toRadians(180+i));
			if(x >= distToWall && x <= width - distToWall && y >= distToWall && y <= height - distToWall && !closeToTree(new Pair(x, y), treeLst)){
				boolean flag = false;
				for(Position sd : board){
					if(distance(sd, new Pair(x, y)) < 2.0){
						flag = true;
						break;
					}
				}
				if(!flag){
					return new Position(x,y);//rnd.nextInt(2) == 0 ? true : false);
				}        
			}
		}
		return null;
	}


	public static ArrayList<Position> hexagonal(ArrayList<Position> trees, double width, double height) {
		double x =  SEED_RADIUS;
		double y =  SEED_RADIUS;
		boolean offset = false;

		ArrayList<Position> locations = new ArrayList<Position>();

		while (y <= height -  SEED_RADIUS) {
			x =  SEED_RADIUS + (offset ?  SEED_RADIUS : 0);

			while (x <= width -  SEED_RADIUS) {
					locations.add(new Position(x, y));

				x += 2* SEED_RADIUS;
			}

			y +=  1.732051* SEED_RADIUS;
			offset = !offset;
		}

		return locations;
	}

	
	public ArrayList<seed> generateRandomBoardFromPositions(ArrayList<Position> positions) {
		ArrayList<seed> seedlist = new ArrayList<seed>();
		Random random = new Random();
		for (Position position : positions) {
			double x = position.x;
			double y = position.y;
			// int identity = random.nextInt(3);
			// switch (identity) {
			// 	case 0:
			// 		//don't fill this position 
			// 		break;
			// 	case 1:
			// 		seedlist.add(new seed(x, y, true));
			// 		break;
			// 	case 2:
			// 		seedlist.add(new seed(x, y, false));
			// 		break;
			// 	default:
			// 		break;
			// }
			int identity = random.nextInt(2);
			switch (identity) {
				case 0:
					seedlist.add(new seed(x, y, false));
					break;
				case 1:
					seedlist.add(new seed(x, y, true));
					break;
				default:
					break;
			}
		}
		return seedlist;
	}
	
	public ArrayList<seed> getSeedsInRegion(ArrayList<seed> board, double minX, double maxX, double minY, double maxY) {
		ArrayList<seed> seedsInRegion = new ArrayList<seed>();
		for (seed nextSeed : board) {
			if (nextSeed.x >= minX && nextSeed.x < maxX && nextSeed.y >= minY && nextSeed.y < maxY) {
				seedsInRegion.add(nextSeed);
			}
		}
		return seedsInRegion;
	}
	
	
	static double distanceBetweenSeeds(seed a, seed b) {
		return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y));
	}

	public void init() {
         
	}

	static double distance(seed tmp, Pair pair) {
		return Math.sqrt((tmp.x - pair.x) * (tmp.x - pair.x) + (tmp.y - pair.y) * (tmp.y - pair.y));
	}
	static double distance(Position tmp, Pair pair) {
		return Math.sqrt((tmp.x - pair.x) * (tmp.x - pair.x) + (tmp.y - pair.y) * (tmp.y - pair.y));
	}
	static double distance(seed a, Point b) {
		return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y));
	}

}
