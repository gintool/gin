package gin.algorithm.nsgaii;

import gin.Patch;

import java.util.ArrayList;
import java.util.Comparator;

public class NSGAIIPop extends NSGAPop {


    public NSGAIIPop(int noObj) {
        super(noObj);
    }

    public NSGAIIPop(int noObj, ArrayList<Integer> fitnessDirs) {
        super(noObj, fitnessDirs);
    }

    public NSGAIIPop(NSGAIIPop p, NSGAIIPop q) {
        super(p, q);
    }

    public void addInd(Patch patch, ArrayList<Long> fitnesses) {
        if (fitnesses.size() != noObj) {
            throw new IllegalArgumentException("Incorrect number of fitnesses");
        }
        population.add(new NSGAIIInd(patch, fitnesses));
    }

    public void setCrowding() {
        for (int m = 0; m < noObj; m++) {
            sortByObj(m);
            ((NSGAIIInd) population.get(0)).setCrowding(Double.MAX_VALUE);
            ((NSGAIIInd) population.get(population.size() - 1)).setCrowding(Double.MAX_VALUE);
            double max = population.get(population.size() - 1).getFitnesses().get(m);
            double min = population.get(0).getFitnesses().get(m);
            double denom = max - min;
            for (int i = 1; i < population.size() - 1; i++) {
                double currentDist = ((NSGAIIInd) population.get(i)).getCrowding();
                double num = population.get(i + 1).getFitnesses().get(m) - population.get(i - 1).getFitnesses().get(m);
                ((NSGAIIInd) population.get(i)).setCrowding(currentDist + (num / denom));
            }
        }

    }


    public ArrayList<Patch> getNextGen(int popSize) {
        ArrayList<Patch> out = new ArrayList<>();
        nonDominatedSort();
        setCrowding();
        for (int front = 1; front <= fronts.size(); front++) {
            if (fronts.get(front).size() < popSize - out.size()) {
                for (NSGAInd ind : fronts.get(front)) {
                    out.add(ind.getPatch().clone());
                }
            } else {
                fronts.get(front).sort(Comparator.comparing((NSGAInd ind) -> ((NSGAIIInd) ind).getCrowding()));
                int frontInd = 0;
                while (out.size() < popSize) {
                    out.add(fronts.get(front).get(frontInd).getPatch().clone());
                    frontInd++;
                }

            }
        }
        return out;
    }
}
