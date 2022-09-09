package gin.util;

import gin.Patch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public abstract class NSGAInd {

    private Patch patch;



    private int rank;
    protected ArrayList<Long> fitnesses;

    public NSGAInd(Patch patch, ArrayList<Long> fitnesses){
        this.patch = patch;
        this.fitnesses = fitnesses;
    }
    public NSGAInd(ArrayList<Long> fitnesses){
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
