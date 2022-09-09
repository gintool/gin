package gin.util;

import gin.Patch;

import java.util.ArrayList;

public class NSGAIIInd extends NSGAInd {
;
    private double crowding;

    private int rank;
    private ArrayList<Long> fitnesses;

    public NSGAIIInd(Patch patch, ArrayList<Long> fitnesses){
        super(patch,fitnesses);


        this.crowding = 0;
    }
    public NSGAIIInd(ArrayList<Long> fitnesses){
        super(fitnesses);
        this.crowding = 0;
    }

    public void setCrowding(double crowding) {
        this.crowding = crowding;
    }

    public double getCrowding() {
        return crowding;
    }

}
