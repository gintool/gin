package gin.algorithm.nsgaii;

import gin.Patch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public abstract class NSGAPop {

    protected ArrayList<NSGAInd> population;
    protected int noObj;
    protected Map<Integer, ArrayList<NSGAInd>> fronts;
    protected ArrayList<Integer> fitnessDirs = new ArrayList<>();

    public NSGAPop(int noObj) {
        this.population = new ArrayList<>();
        this.noObj = noObj;
        for (int i = 0; i < noObj; i++) {
            fitnessDirs.add(1);
        }
    }

    public NSGAPop(int noObj, ArrayList<Integer> fitnessDirs) {
        this.population = new ArrayList<>();
        this.noObj = noObj;
        this.fitnessDirs = fitnessDirs;
    }

    public NSGAPop(NSGAPop p, NSGAPop q) {
        if (p.noObj != q.noObj) {
            throw new IllegalArgumentException("No objectives dont match");
        }
        noObj = p.noObj;
        if (p.fitnessDirs.equals(q.fitnessDirs)) {
            fitnessDirs = p.fitnessDirs;

        } else {
            throw new IllegalArgumentException("Fitness Directions do not match");
        }
        this.population = new ArrayList<>();
        for (NSGAInd ind : p.getPopulation()) {
            addInd(ind);
        }
        for (NSGAInd ind : q.getPopulation()) {
            addInd(ind);
        }
    }

    public abstract void addInd(Patch patch, ArrayList<Long> fitnesses);

    public void addInd(NSGAInd ind) {
        population.add(ind);
    }

    protected void nonDominatedSort() {
        Map<NSGAInd, ArrayList<NSGAInd>> dominationSets = new HashMap<>();
        Map<NSGAInd, Integer> dominationScores = new HashMap<>();
        fronts = new HashMap<>();
        fronts.put(1, new ArrayList<>());
        for (NSGAInd p : population) {
            ArrayList<NSGAInd> dominationSet = new ArrayList<>();
            int dominationScore = 0;
            for (NSGAInd q : population) {
                if (dominates(p, q)) {
                    dominationSet.add(q);
                } else if (dominates(q, p)) {
                    dominationScore += 1;
                }
            }
            dominationScores.put(p, dominationScore);
            dominationSets.put(p, dominationSet);
            if (dominationScore == 0) {
                fronts.get(1).add(p);
            }
        }
        int i = 1;
        while (true) {
            ArrayList<NSGAInd> currentFront = fronts.get(i);
            ArrayList<NSGAInd> nextFront = new ArrayList<>();
            for (NSGAInd p : currentFront) {
                for (NSGAInd q : dominationSets.get(p)) {
                    Integer score = dominationScores.get(q);
                    score -= 1;
                    dominationScores.put(q, score);
                    if (score == 0) {
                        q.setRank(i + 1);
                        nextFront.add(q);
                    }
                }
            }
            i += 1;
            if (nextFront.size() == 0) {
                break;
            } else {
                fronts.put(i, nextFront);
            }
        }
    }


    public boolean dominates(NSGAInd p, NSGAInd q) {
        boolean better = false;
        for (int i = 0; i < noObj; i++) {
            float dir = fitnessDirs.get(i);
            if (p.getFitnesses().get(i) * dir < q.getFitnesses().get(i) * dir) {
                return false;
            }
            if (p.getFitnesses().get(i) * dir > q.getFitnesses().get(i) * dir) {
                better = true;
            }
        }
        return better;
    }

    public void sortByObj(int index) {
        population.sort(Comparator.comparing((NSGAInd ind) -> ind.getFitnesses().get(index)));
    }

    public ArrayList<NSGAInd> getPopulation() {
        return population;
    }

    public abstract ArrayList<Patch> getNextGen(int popSize);
}
