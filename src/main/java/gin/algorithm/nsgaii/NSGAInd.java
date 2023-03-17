package gin.algorithm.nsgaii;

import gin.Patch;

import java.util.ArrayList;

public abstract class NSGAInd {

    protected ArrayList<Long> fitnesses;
    private Patch patch;
    private int rank;

    public NSGAInd(Patch patch, ArrayList<Long> fitnesses) {
        this.patch = patch;
        this.fitnesses = fitnesses;
    }

    public NSGAInd(ArrayList<Long> fitnesses) {
        this.fitnesses = fitnesses;
    }

    public ArrayList<Long> getFitnesses() {
        return fitnesses;
    }

    public Patch getPatch() {
        return patch;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

}
