package gin.util;

import gin.Patch;
import gin.edit.Edit;
import gin.edit.line.CopyLine;
import gin.edit.line.DeleteLine;
import gin.edit.line.LineEdit;
import gin.edit.llm.LLMMaskedStatement;
import gin.edit.llm.LLMReplaceStatement;
import gin.test.UnitTest;
import gin.test.UnitTestResultSet;
import org.pmw.tinylog.Logger;

import com.fasterxml.jackson.annotation.JsonTypeInfo.None;
import com.sampullara.cli.Argument;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;


/**
 * Method-based LocalSearchSimple search.
 */

public abstract class LocalSearchSimple extends GP {

    // Percentage of population size to be selected during tournament selection
    private static final double TOURNAMENT_PERCENTAGE = 0.2;
    // Probability of adding an edit during uniform crossover
    private static final double MUTATE_PROBABILITY = 0.5;

    // Whether to use LLM edits
    private boolean ifLLM = false;

    private Class <? extends Edit> LLMedit = null;

    private List<Class <? extends Edit>> NoneLLMedit = new ArrayList<>();

    public LocalSearchSimple(String[] args) {
        super(args);
        SetLLMedits();
    }

    // Constructor used for testing
    public LocalSearchSimple(File projectDir, File methodFile) {
        super(projectDir, methodFile); 
        SetLLMedits();
    }

    private void SetLLMedits () {
        if (super.editTypes.contains(LLMMaskedStatement.class) || super.editTypes.contains(LLMReplaceStatement.class)) {
            ifLLM = true;
            if (super.editTypes.contains(LLMMaskedStatement.class)) {
                LLMedit = LLMMaskedStatement.class;
            } else if (super.editTypes.contains(LLMReplaceStatement.class)) {
                LLMedit = LLMReplaceStatement.class;
            }

            for (Class <? extends Edit> edit : super.editTypes) {
                if (edit != LLMedit) {
                    NoneLLMedit.add(edit);
                }
            }
        }


        Logger.info("=== LocalSearchSimple ===");
        Logger.info("LLM edits: " + ifLLM);
        Logger.info("None LLM edits: " + NoneLLMedit.toString());
        Logger.info("LLM edit: " + LLMedit);
        Logger.info("=====================================");


    }

    // Whatever initialisation needs to be done for fitness calculations
    @Override
    protected abstract UnitTestResultSet initFitness(String className, List<UnitTest> tests, Patch origPatch);

    // Calculate fitness
    @Override
    protected abstract double fitness(UnitTestResultSet results);

    // Calculate fitness threshold, for selection to the next generation
    @Override
    protected abstract boolean fitnessThreshold(UnitTestResultSet results, double orig);

    /*============== Implementation of abstract methods  ==============*/

    /*====== Search ======*/

    // Simple GP search (based on Simple)
    protected void search(TargetMethod method, Patch origPatch) {

        Logger.info("Runnning best-first local search.");

        String className = method.getClassName();
        String methodName = method.toString();
        List<UnitTest> tests = method.getGinTests();

        // Run original code
        UnitTestResultSet results = initFitness(className, tests, origPatch);

        // Calculate fitness and record result, including fitness improvement (currently 0)
        double orig = fitness(results);
        super.writePatch(-1, 0, results, methodName, orig, 0);

        // Keep best 
        double best = orig;
        Patch bestPatch = origPatch;

        for (int i = 1; i < indNumber; i++) {

            // Add a mutation
            Patch patch = neighbour(bestPatch);

            // Calculate fitness
            results = testPatch(className, tests, patch, null);
            double newFitness = fitness(results);
            super.writePatch(i, i, results, methodName, newFitness, compareFitness(newFitness, orig));

            // Check if better
            if (compareFitness(newFitness, best) > 0) {
                best = newFitness;
                bestPatch = patch;
            }
        }
    }

    /*====== GP Operators ======*/

    /**
     * Generate a neighbouring patch, by either deleting an edit, or adding a new one.
     *
     * @param patch Generate a neighbour of this patch.
     * @return A neighbouring patch.
     */
    Patch neighbour(Patch patch) {

        Patch neighbour = patch.clone();

        if(ifLLM && NoneLLMedit.size() > 0){
            Logger.info("LLM edit" + super.combinedProbablity);
            if (neighbour.size() > 0 && super.mutationRng.nextFloat() > super.combinedProbablity) {
                neighbour.addRandomEditOfClasses(super.mutationRng, Arrays.asList(LLMedit));
            } 
            else {
                neighbour.addRandomEditOfClasses(super.mutationRng, NoneLLMedit);
            }
        } else {
            neighbour.addRandomEditOfClasses(super.mutationRng, super.editTypes);
        }


        return neighbour;

    }


    // Adds a random edit of the given type with equal probability among allowed types
    // TODO: This is a bit of a hack, as it assumes that if only put one edit type, it is LLMReplaceStatement or LLMMaskedStatement
    protected Patch mutate(Patch oldPatch) {
        Patch patch = oldPatch.clone();
        patch.addRandomEditOfClasses(super.mutationRng, super.editTypes);

        return patch;
    }

    // Tournament selection for patches
    protected List<Patch> select(Map<Patch, Double> population, Patch origPatch, double origFitness) {

        List<Patch> patches = new ArrayList<>(population.keySet());
        if (patches.size() < super.indNumber) {
            population.put(origPatch, origFitness);
            while (patches.size() < super.indNumber) {
                patches.add(origPatch);
            }
        }
        List<Patch> selectedPatches = new ArrayList<>();

        // Pick half of the population size
        for (int i = 0; i < super.indNumber / 2; i++) {

            Collections.shuffle(patches, super.individualRng);

            // Best patch from x% randomly selected patches picked each time
            Patch bestPatch = patches.get(0);
            double best = population.get(bestPatch);
            for (int j = 1; j < (super.indNumber * TOURNAMENT_PERCENTAGE); j++) {
                Patch patch = patches.get(j);
                double fitness = population.get(patch);

                if (compareFitness(fitness, best) > 0) {
                    bestPatch = patch;
                    best = fitness;
                }
            }

            selectedPatches.add(bestPatch.clone());

        }
        return selectedPatches;
    }

    // Uniform crossover: patch1patch2 and patch2patch1 created, each edit added with x% probability
    protected List<Patch> crossover(List<Patch> patches, Patch origPatch) {

        List<Patch> crossedPatches = new ArrayList<>();

        Collections.shuffle(patches, super.individualRng);
        int half = patches.size() / 2;
        for (int i = 0; i < half; i++) {

            Patch parent1 = patches.get(i);
            Patch parent2 = patches.get(i + half);
            List<Edit> list1 = parent1.getEdits();
            List<Edit> list2 = parent2.getEdits();

            Patch child1 = origPatch.clone();
            Patch child2 = origPatch.clone();

            for (Edit edit : list1) {
                if (super.mutationRng.nextFloat() > MUTATE_PROBABILITY) {
                    child1.add(edit);
                }
            }
            for (Edit edit : list2) {
                if (super.mutationRng.nextFloat() > MUTATE_PROBABILITY) {
                    child1.add(edit);
                }
                if (super.mutationRng.nextFloat() > MUTATE_PROBABILITY) {
                    child2.add(edit);
                }
            }
            for (Edit edit : list1) {
                if (super.mutationRng.nextFloat() > MUTATE_PROBABILITY) {
                    child2.add(edit);
                }
            }

            crossedPatches.add(parent1);
            crossedPatches.add(parent2);
            crossedPatches.add(child1);
            crossedPatches.add(child2);
        }

        return crossedPatches;
    }

}

