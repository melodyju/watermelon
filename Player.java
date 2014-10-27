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

	private final int generationSize = 10;
	private final int numGenerations = 3;

	enum Region {
		TOP, BOTTOM, LEFT, RIGHT
	}

	public void init() {

	}

	static double distance(seed tmp, Pair pair) {
		return Math.sqrt((tmp.x - pair.x) * (tmp.x - pair.x) + (tmp.y - pair.y) * (tmp.y - pair.y));
	}

	// Return: the next position
	// my position: dogs[id-1]
	static double distance(seed a, Point b) {
		return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y));
	}

	@Override
	public ArrayList<seed> move(ArrayList<Pair> treelist, double width, double length, double s) {
		// TODO Auto-generated method stub
		this.boardWidth = width;
		this.boardHeight = length;
		this.s = s;

		return bestProgeny(treelist, width, length, s);
	}

	public ArrayList<seed> bestProgeny(ArrayList<Pair> treelist, double width, double length, double s) {
		//Initialize generation 0 (k = 0)
		int k = 0;
		ArrayList<ArrayList<seed>> parents = new ArrayList<ArrayList<seed>>();
		for (int i = 0; i < generationSize; i++) {
			parents.add(i, generateRandomBoard(treelist, width, length, s));
		}

		boolean condition = false;
		ArrayList<ArrayList<seed>> children = null;
		while (condition == false) {
			System.out.println("Generation " + k);
			//Create generation k + 1
			children = spawnChildren(parents);
			// System.out.println("Spawn children returned");

			k++;
			if (k > numGenerations) {
				condition = true;
			}
			parents = children;
		}

		//now return the fittest person in the most current generation
		return fittestIndividual(children);
	}

	public ArrayList<seed> fittestIndividual(ArrayList<ArrayList<seed>> generation) {
		double maxFitness = 0;
		ArrayList<seed> fittestIndividual = null;
		for (ArrayList<seed> individual : generation) {
			double fitness = calculateFitness(individual);
			if (fitness > maxFitness) {
				maxFitness = fitness;
				fittestIndividual = individual;
			}
		}
		return fittestIndividual;
	}

	public ArrayList<ArrayList<seed>> spawnChildren(ArrayList<ArrayList<seed>> parents) {
		ArrayList<ArrayList<seed>> children = new ArrayList<ArrayList<seed>>();

		//calculate total fitness of parent generation
		double[] parentFitness = new double[generationSize];
		double totalFitness = 0;
		for (int i = 0; i < generationSize; i++) {
			double fitness = calculateFitness(parents.get(i));
			parentFitness[i] = fitness;
			totalFitness += fitness;
		}

		//fill in the child population
		ArrayList<seed> parent1; //parent 1 of children[i]
		ArrayList<seed> parent2; //parent 2 of children[i]
		ArrayList<ArrayList<seed>> allChildren; //all children from these 2 parents
		ArrayList<seed> fittestChild; //fittest child from these 2 parents -> children[i]

		for (int i = 0; i < generationSize; i++) {
			//select first parent by roulette wheel selection
			parent1 = rouletteWheelSelection(parents, parentFitness, totalFitness);
			//select second parent by roulette wheel selection
			parent2 = rouletteWheelSelection(parents, parentFitness, totalFitness);
			//parents reproduce 
			allChildren = getChildrenFromParentBoards(parent1, parent2);
			//add their fittest child to the list of children
			fittestChild = fittestIndividual(allChildren);
			children.add(i, fittestChild);
		}

		return children;
	}

	public double totalFitness(ArrayList<ArrayList<seed>> generation) {
		double totalFitness = 0;
		for (ArrayList<seed> individual : generation) {
			totalFitness += calculateFitness(individual);
		}
		return totalFitness;
	}

	public ArrayList<seed> rouletteWheelSelection(ArrayList<ArrayList<seed>> parents, double[] parentFitness, double totalFitness) {
		Random random = new Random();
		double rnd = random.nextDouble() * totalFitness;
		int j;
		for (j = 0; j < generationSize && rnd > 0; j++) {
			rnd -= parentFitness[j];
		}
		return parents.get(j-1);
	}

	public ArrayList<seed> generateRandomBoard(ArrayList<Pair> treelist, double width, double length, double s) {
		ArrayList<seed> seedlist = new ArrayList<seed>();
		for (double i = distowall; i < width - distowall; i = i + distoseed) {
			for (double j = distowall; j < length - distowall; j = j + distoseed) {
				Random random = new Random();
				seed tmp;
				if (random.nextInt(2) == 0)
					tmp = new seed(i, j, false);
				else
					tmp = new seed(i, j, true);
				boolean add = true;
				for (int f = 0; f < treelist.size(); f++) {
					if (distance(tmp, treelist.get(f)) < distotree) {
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
			int identity = random.nextInt(3);
			switch (identity) {
				case 0:
					//don't fill this position 
					break;
				case 1:
					seedlist.add(new seed(x, y, true));
					break;
				case 2:
					seedlist.add(new seed(x, y, false));
					break;
				default:
					break;
			}
		}
		return seedlist;
	}

	public ArrayList<ArrayList<seed>> getChildrenFromParentBoards(ArrayList<seed> board1, ArrayList<seed> board2) {
		ArrayList<ArrayList<seed>> childrenBoards = new ArrayList<ArrayList<seed>>();
		
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
		
		childrenBoards.add(child1);
		childrenBoards.add(child2);
		childrenBoards.add(child3);
		childrenBoards.add(child4);
		
		return childrenBoards;
	}
	
	public ArrayList<seed> getSeedsInRegion(ArrayList<seed> board, Region region) {
		ArrayList<seed> seedsInRegion = new ArrayList<seed>();
		for (seed nextSeed : board) {
			switch(region) {
				case TOP:
					if (nextSeed.y <= (this.boardHeight - SEED_RADIUS) / 2.0) {
						seedsInRegion.add(nextSeed);
					}
					break;
				case BOTTOM:
					if (nextSeed.y >= (this.boardHeight + SEED_RADIUS) / 2.0) {
						seedsInRegion.add(nextSeed);
					}
					break;
				case LEFT:
					if (nextSeed.x <= (this.boardWidth - SEED_RADIUS) / 2.0) {
						seedsInRegion.add(nextSeed);
					}
					break;
				case RIGHT:
					if (nextSeed.x >= (this.boardWidth + SEED_RADIUS) / 2.0) {
						seedsInRegion.add(nextSeed);
					}
					break;
				default:
					System.out.println("getSeedsInRegion is broken :/");
					break;
					
			}
		}
		return seedsInRegion;
	}

	public double calculateFitness(ArrayList<seed> seedlist) {
		double total = 0;
		for (int i = 0; i < seedlist.size(); i++) {
			double score;
			double chance = 0.0;
			double totaldis = 0.0;
			double difdis = 0.0;
			for (int j = 0; j < seedlist.size(); j++) {
				if (j != i) {
					totaldis = totaldis
							+ Math.pow(
									distanceseed(seedlist.get(i),
											seedlist.get(j)), -2);
				}
			}
			for (int j = 0; j < seedlist.size(); j++) {
				if (j != i
						&& ((seedlist.get(i).tetraploid && !seedlist.get(j).tetraploid) || (!seedlist
								.get(i).tetraploid && seedlist.get(j).tetraploid))) {
					difdis = difdis
							+ Math.pow(
									distanceseed(seedlist.get(i),
											seedlist.get(j)), -2);
				}
			}
			chance = difdis / totaldis;
			score = chance + (1 - chance) * s;
			total = total + score;
		}
		return total;
	}
	
	static double distanceseed(seed a, seed b) {
		return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y));
	}

}