package gin.algorithm.nsgaii;

import gin.Patch;

import java.util.ArrayList;

public class NSGAIIInd extends NSGAInd {
    private double crowding;

    public NSGAIIInd(Patch patch, ArrayList<Long> fitnesses) {
        super(patch, fitnesses);


        this.crowding = 0;
    }

    public NSGAIIInd(ArrayList<Long> fitnesses) {
        super(fitnesses);
        this.crowding = 0;
    }

    public double getCrowding() {
        return crowding;
    }

    public void setCrowding(double crowding) {
        this.crowding = crowding;
    }

}
