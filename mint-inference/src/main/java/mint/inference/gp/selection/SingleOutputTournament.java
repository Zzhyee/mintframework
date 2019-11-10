package mint.inference.gp.selection;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.apache.commons.collections4.MultiValuedMap;

import mint.inference.evo.Chromosome;
import mint.inference.gp.fitness.Fitness;
import mint.inference.gp.fitness.latentVariable.IntegerFitness;
import mint.inference.gp.fitness.latentVariable.LatentVariableFitness;
import mint.inference.gp.fitness.latentVariable.StringFitness;
import mint.inference.gp.fitness.singleOutput.SingleOutputFitness;
import mint.inference.gp.tree.Node;
import mint.inference.gp.tree.NodeComparator;
import mint.tracedata.types.VariableAssignment;

/**
 * Created by neilwalkinshaw on 25/06/15.
 */
public class SingleOutputTournament extends IOTournamentSelection<VariableAssignment<?>> {

	protected Map<Node<?>, List<Double>> distances = null;
	boolean mem_dist = false;

	public SingleOutputTournament(MultiValuedMap<List<VariableAssignment<?>>, VariableAssignment<?>> evals,
			List<Chromosome> totalPopulation, int maxDepth, boolean mem_dist) {
		super(evals, totalPopulation, maxDepth);
		distances = new HashMap<Node<?>, List<Double>>();
		this.mem_dist = mem_dist;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public LatentVariableFitness<?> getFitness(Chromosome toEvaluateC) {
		Node<?> toEvaluate = (Node<?>) toEvaluateC;
		if (toEvaluate.getType().equals("string"))
			return new StringFitness(evals, (Node<VariableAssignment<String>>) toEvaluate, maxDepth);
		else {
			assert (toEvaluate.getType().equals("integer"));
			return new IntegerFitness(evals, (Node<VariableAssignment<Integer>>) toEvaluate, maxDepth);
		}
	}

	@Override
	protected Comparator<Chromosome> getComparator() {
		return new NodeComparator(this);
	}

	@Override
	protected void processResult(Map<Future<Double>, Chromosome> solMap, Future<Double> sol, double score,
			Fitness fitness) {
		super.processResult(solMap, sol, score, fitness);
		SingleOutputFitness<?> sof = (SingleOutputFitness<?>) fitness;
		if (mem_dist)
			distances.put(sof.getIndividual(), sof.getDistances());
	}

	public Map<Node<?>, List<Double>> getDistances() {
		return distances;
	}
}
