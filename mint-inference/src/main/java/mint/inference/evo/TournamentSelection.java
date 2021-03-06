package mint.inference.evo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mint.inference.gp.fitness.Fitness;

/**
 *
 * Implements the Tournament Selection strategy for GP. Partition the population
 * of individuals into groups (of a given size). For each group, select the best
 * ones.
 *
 * Created by neilwalkinshaw on 05/03/15.
 */

public abstract class TournamentSelection implements Selection {

	protected Map<Chromosome, Double> fitnessCache;
	protected Map<Chromosome, String> summaryCache;
	protected List<Chromosome> totalPopulation;
	protected List<Chromosome> elite;
	protected int eliteSize;
	protected double bestFitness;
	protected int maxDepth;
	protected Random rand;

	public List<Chromosome> getElite() {
		return elite;
	}

	public TournamentSelection(List<Chromosome> totalPopulation, int maxDepth, Random rand) {
		this.summaryCache = new HashMap<Chromosome, String>();
		eliteSize = 10;
		this.totalPopulation = totalPopulation;
		this.bestFitness = Double.MAX_VALUE;
		this.maxDepth = maxDepth;
		this.fitnessCache = new HashMap<Chromosome, Double>();
		this.rand = rand;
	}

	@Override
	public double getBestFitness() {
		return bestFitness;
	}

	@Override
	public List<Chromosome> select(GPConfiguration config, int number) {
		List<List<Chromosome>> partitions = partition(config.getTournamentSize(), number);
		List<Chromosome> best = bestIndividuals(partitions);
		bestScoresAndElites(best);
		return best;
	}

	public double computeFitness(Chromosome toEvaluate) throws InterruptedException {
		if (fitnessCache.containsKey(toEvaluate))
			return fitnessCache.get(toEvaluate);
		else {
			Fitness f = getFitness(toEvaluate);
			double fitness = f.call();
			fitnessCache.put(toEvaluate, fitness);
			summaryCache.put(toEvaluate, f.getFitnessSummary());
			return fitness;
		}
	}

	public abstract Fitness getFitness(Chromosome toEvaluate);

	protected List<List<Chromosome>> partition(int tournamentSize, int number) {
		List<List<Chromosome>> best = new ArrayList<List<Chromosome>>();
		while (best.size() < number) {
			List<Chromosome> pop = new ArrayList<Chromosome>();
			for (int i = pop.size(); i < tournamentSize; i++) {
				pop.add(totalPopulation.get(rand.nextInt(totalPopulation.size())).copy());
			}
			best.add(pop);
		}
		return best;
	}

	protected List<Chromosome> bestIndividuals(List<List<Chromosome>> partitions) {
		List<Chromosome> bestIndividuals = new ArrayList<Chromosome>();
		for (List<Chromosome> p : partitions) {
			bestIndividuals.add(evaluatePopulation(p));
		}
		return bestIndividuals;
	}

	protected abstract Comparator<Chromosome> getComparator();

	protected void bestScoresAndElites(List<Chromosome> population) {
		Collections.sort(population, getComparator());
		if (population.isEmpty())
			return;
		bestFitness = fitnessCache.get(population.get(0));
		elite = new ArrayList<>();
		for (int i = 0; (i < eliteSize && i < population.size()); i++) {
			elite.add(population.get(i));
		}
		/*
		 * elite.clear(); elite.add(population.get(0)); for(int i =1; i<
		 * population.size() && elite.size()<maxElite; i++){
		 * if(!elite.contains(population.get(i))) { elite.add(population.get(i)); } }
		 */
	}

	protected Chromosome evaluatePopulation(Collection<Chromosome> population) {
		assert (!population.isEmpty());
		double bestScore = Double.MAX_VALUE;
		Chromosome best = null;
		Map<Double, Chromosome> solMap = new HashMap<Double, Chromosome>();
		Set<Double> set = new HashSet<Double>();
		ExecutorService pool = Executors.newFixedThreadPool(4);
		Fitness fitness = null;
		try {
			for (Chromosome node : population) {
				fitness = getFitness(node);
				Double f = fitness.call();
//				Future<Double> future = pool.submit(fitness);
				solMap.put(f, node);
				set.add(f);
			}
			for (Double sol : set) {
				double score = 0D;
				try {
					processResult(solMap, sol, score, fitness);
				} catch (Exception ex) {
					ex.printStackTrace();
					score = 1000D;
					processResult(solMap, sol, score, fitness);
				}
				if (score < bestScore) {
					bestScore = score;
					best = solMap.get(sol);
				} else if (best == null)
					best = solMap.get(sol);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			pool.shutdownNow();
		}
		return best.copy();
	}

	protected void processResult(Map<Double, Chromosome> solMap, Double sol, double score, Fitness fitness) {
		fitnessCache.put(solMap.get(sol), score);
	}

}
