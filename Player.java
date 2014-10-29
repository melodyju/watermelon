package watermelon.group2;

import java.util.*;

import watermelon.sim.Pair;
import watermelon.sim.Point;
import watermelon.sim.seed;

public class Player extends watermelon.sim.Player {
	static double distowall = 2.1;
	static double distotree = 2.2;
	static double distoseed = 1.01;
	static double SEED_RADIUS = 1.0;

	static double boardWidth;
	static double boardHeight;
	static double s;
	static ArrayList<Pair> treeList;

	static int k;

	private static final int generationSize = 20;
	private static final int numGenerations = 5;

	/////////////////////////////////////////////////////////////////////////////////////
	// Generation and Individual classes

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

		ArrayList<Individual> reproduce(Individual parent2) {
			ArrayList<Individual> children = new ArrayList<Individual>();

			ArrayList<seed> board1 = this.board;
			ArrayList<seed> board2 = parent2.board;

			ArrayList<seed> child1 = new ArrayList<seed>();
			child1.addAll(getSeedsInRegion(board1, Region.TOP));
			child1.addAll(getSeedsInRegion(board2, Region.BOTTOM));
			
			ArrayList<seed> child2 = new ArrayList<seed>();
			child2.addAll(getSeedsInRegion(board1, Region.BOTTOM));
			child2.addAll(getSeedsInRegion(board2, Region.TOP));
			
			ArrayList<seed> child3 = new ArrayList<seed>();
			child3.addAll(getSeedsInRegion(board1, Region.LEFT));
			child3.addAll(getSeedsInRegion(board2, Region.RIGHT));
			
			ArrayList<seed> child4 = new ArrayList<seed>();
			child4.addAll(getSeedsInRegion(board1, Region.RIGHT));
			child4.addAll(getSeedsInRegion(board2, Region.LEFT));
			
			children.add(new Individual(child1));
			children.add(new Individual(child2));
			children.add(new Individual(child3));
			children.add(new Individual (child4));
			
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
			// Create generation k + 1
			children = new Generation(parents);

			k++;
			// Children become the parents of the next generation
			parents = children;
		}

		// Return the fittest person in the most current generation
		return children.fittestIndividual(children.population);
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
		for (double i = distowall; i < boardWidth - distowall; i = i + distoseed) {
			for (double j = distowall; j < boardHeight - distowall; j = j + distoseed) {
				Random random = new Random();
				seed tmp;
				if (random.nextInt(2) == 0)
					tmp = new seed(i, j, false);
				else
					tmp = new seed(i, j, true);
				boolean add = true;
				for (int f = 0; f < treeList.size(); f++) {
					if (distance(tmp, treeList.get(f)) < distotree) {
						add = false;
						break;
					}
				}
				if (add) {
					seedlist.add(tmp);
				}
			}
		}

		return generateRandomBoardFromPositions(seedlist);
	}

	public ArrayList<seed> generateRandomBoardFromPositions(ArrayList<seed> positions) {
		ArrayList<seed> seedlist = new ArrayList<seed>();
		Random random = new Random();
		for (seed position : positions) {
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

	static double distance(seed a, Point b) {
		return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y));
	}

}
