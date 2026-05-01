package gin.algorithm.nsgaii;

import com.opencsv.CSVWriter;
import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import gin.Patch;
import gin.SourceFile;
import gin.edit.Edit;
import gin.test.UnitTest;
import gin.test.UnitTestResultSet;
import gin.util.Sampler;
import org.apache.commons.rng.simple.JDKRandomBridge;
import org.apache.commons.rng.simple.RandomSource;
import org.pmw.tinylog.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class NSGAII extends Sampler {

    @Serial
    private static final long serialVersionUID = 8547883760400442899L;

    @Argument(alias = "et", description = "Edit type: this can be a member of the EditType enum (LINE,STATEMENT,MATCHED_STATEMENT,MODIFY_STATEMENT); the fully qualified name of a class that extends gin.edit.Edit, or a comma separated list of both")
    protected String editType = Edit.EditType.STATEMENT.toString();

    @Argument(alias = "gn", description = "Number of generations")
    protected Integer genNumber = 1;

    @Argument(alias = "in", description = "Number of individuals")
    protected Integer indNumber = 10;

    @Argument(alias = "ms", description = "Random seed for mutation operator selection")
    protected Integer mutationSeed = 123;

    @Argument(alias = "is", description = "Random seed for individual selection")
    protected Integer individualSeed = 123;

    // Allowed edit types for sampling: parsed from editType
    protected List<Class<? extends Edit>> editTypes;

    protected Random mutationRng;
    protected Random individualRng;
    protected String methodName;
    private String className;
    private float initTime;
    private long initMem;
    private List<UnitTest> tests;

    public NSGAII(String[] args) {
        super(args);
        Args.parseOrExit(this, args);
        setup();
        printAdditionalArguments();
    }

    // Constructor used for testing
    public NSGAII(File projectDir, File methodFile) {
        super(projectDir, methodFile);
        setup();
    }


    public static void main(String[] args) {
        NSGAII sampler = new NSGAII(args);
        sampler.sampleMethods();
    }


    private void printAdditionalArguments() {
        Logger.info("Edit types: " + editTypes);
        Logger.info("Number of generations: " + genNumber);
        Logger.info("Number of individuals: " + indNumber);
        Logger.info("Random seed for mutation operator selection: " + mutationSeed);
        Logger.info("Random seed for individual selection: " + individualSeed);
    }

    private void setup() {
        mutationRng = new JDKRandomBridge(RandomSource.MT, Long.valueOf(mutationSeed));
        individualRng = new JDKRandomBridge(RandomSource.MT, Long.valueOf(individualSeed));
        editTypes = Edit.parseEditClassesFromString(editType);
    }

    // Implementation of gin.util.Sampler's abstract method
    protected void sampleMethodsHook() {

        if ((indNumber < 1) || (genNumber < 1)) {
            Logger.info("Please enter a positive number of generations and individuals.");
        } else {

            writeNewHeader();

            for (TargetMethod method : methodData) {

                Logger.info("Running NSGAII on method " + method);

                // Setup SourceFile for patching
                SourceFile sourceFile = SourceFile.makeSourceFileForEditTypes(editTypes, method.getFileSource().getPath(), Collections.singletonList(method.getMethodName()));

                search(method, new Patch(sourceFile));

            }
        }

    }

    private void search(TargetMethod method, Patch origPatch) {


        className = method.getClassName();
        methodName = method.toString();
        tests = method.getGinTests();

        // Run original code
        UnitTestResultSet initRes = initFitness(className, tests, origPatch);
        initMem = initRes.totalMemoryUsage();
        initTime = initRes.totalExecutionTime() / 1000000.0f;
        writePatch(initRes, methodName);

        ArrayList<Integer> dirs = new ArrayList<>();
        dirs.add(-1);
        dirs.add(-1);
        NSGAIIPop P = new NSGAIIPop(2, dirs);
        UnitTestResultSet resultSet;
        Logger.info("Generating initial generation");
        for (int i = 0; i < indNumber; i++) {
            Patch patch = mutate(origPatch);
            resultSet = testPatch(className, tests, patch, null);
            writePatch(resultSet, methodName);
            ArrayList<Long> fitnesses = new ArrayList<>();
            if (resultSet.allTestsSuccessful()) {
                fitnesses.add(resultSet.totalExecutionTime());
                fitnesses.add(resultSet.totalMemoryUsage());
            } else {
                fitnesses.add(Long.MAX_VALUE);
                fitnesses.add(Long.MAX_VALUE);
            }
            P.addInd(patch, fitnesses);

        }
        for (int g = 0; g < genNumber; g++) {
            Logger.info("Generating generation " + g);
            NSGAIIPop Q = NSGAIIOffspring(P, origPatch);
            NSGAIIPop R = new NSGAIIPop(P, Q);
            Logger.info("getting next generation");
            ArrayList<Patch> patches = R.getNextGen(indNumber);
            P = new NSGAIIPop(2, dirs);
            for (Patch patch : patches) {
                Logger.info("Testing patch: " + patch);
                resultSet = testPatch(className, tests, patch, null);

                writePatch(resultSet, methodName);
                ArrayList<Long> fitnesses = new ArrayList<>();
                if (resultSet.allTestsSuccessful()) {
                    fitnesses.add(resultSet.totalExecutionTime());
                    fitnesses.add(resultSet.totalMemoryUsage());
                } else {
                    fitnesses.add(Long.MAX_VALUE);
                    fitnesses.add(Long.MAX_VALUE);
                }
                P.addInd(patch, fitnesses);
            }
        }
    }

    public NSGAIIPop NSGAIIOffspring(NSGAIIPop pop, Patch origpatch) {
        Logger.info("Generating offspring");
        ArrayList<NSGAInd> population = pop.getPopulation();
        List<Patch> oldPatches = new ArrayList<>();
        for (NSGAInd ind : population) {
            oldPatches.add(ind.getPatch());
        }
        List<Patch> patches = new ArrayList<>();
        //selection
        for (int i = 0; i < population.size(); i++) {
            NSGAInd ind1 = population.get(individualRng.nextInt(population.size()));
            NSGAInd ind2 = population.get(individualRng.nextInt(population.size()));
            if (ind1.getRank() < ind2.getRank()) {
                patches.add(ind1.getPatch().clone());
            }
            if (ind1.getRank() > ind2.getRank()) {
                patches.add(ind2.getPatch().clone());
            } else {
                float coinFlip = mutationRng.nextFloat();
                if (coinFlip < 0.5) {
                    patches.add(ind1.getPatch().clone());
                } else {
                    patches.add(ind2.getPatch().clone());
                }
            }
        }
        //crossover
        patches = crossover(patches, origpatch);
        //mutation
        List<Patch> mutatedPatches = new ArrayList<>();
        for (Patch patch : patches) {
            if (mutationRng.nextFloat() < 0.5) {
                mutatedPatches.add(mutate(patch));
            }
        }
        patches = mutatedPatches;
        ArrayList<Integer> dirs = new ArrayList<>();
        dirs.add(-1);
        dirs.add(-1);
        NSGAIIPop Q = new NSGAIIPop(2, dirs);
        //fitness
        for (Patch patch : patches) {
            UnitTestResultSet resultSet;
            resultSet = testPatch(className, tests, patch, null);

            writePatch(resultSet, methodName);
            ArrayList<Long> fitnesses = new ArrayList<>();
            if (resultSet.allTestsSuccessful()) {
                fitnesses.add(resultSet.totalExecutionTime());
                fitnesses.add(resultSet.totalMemoryUsage());
            } else {
                fitnesses.add(Long.MAX_VALUE);
                fitnesses.add(Long.MAX_VALUE);
            }
            Q.addInd(patch, fitnesses);
        }

        return Q;
    }

    protected UnitTestResultSet initFitness(String className, List<UnitTest> tests, Patch origPatch) {

        return testPatch(className, tests, origPatch, null);
    }

    protected Patch mutate(Patch oldPatch) {
        Patch patch = oldPatch.clone();
        patch.addRandomEditOfClasses(mutationRng, editTypes);
        return patch;
    }


    protected List<Patch> crossover(List<Patch> patches, Patch origPatch) {

        List<Patch> crossedPatches = new ArrayList<>();

        Collections.shuffle(patches, mutationRng);
        int half = patches.size() / 2;
        for (int i = 0; i < half; i++) {

            Patch parent1 = patches.get(i);
            Patch parent2 = patches.get(i + half);
            List<Edit> list1 = parent1.getEdits();
            List<Edit> list2 = parent2.getEdits();

            Patch child1 = origPatch.clone();
            Patch child2 = origPatch.clone();

            for (Edit edit : list1) {
                if (mutationRng.nextFloat() > 0.5) {
                    child1.add(edit);
                }
            }
            for (Edit edit : list2) {
                if (mutationRng.nextFloat() > 0.5) {
                    child1.add(edit);
                }
                if (mutationRng.nextFloat() > 0.5) {
                    child2.add(edit);
                }
            }
            for (Edit edit : list1) {
                if (mutationRng.nextFloat() > 0.5) {
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



    /*============== Helper methods  ==============*/

    protected void writeNewHeader() {
        String[] entry = {"MethodName"
                , "Patch"
                , "Compiled"
                , "AllTestsPassed"
                , "TotalExecutionTime(ms)"
                , "ExecutionTimeFitness"
                , "ExecutionTimeFitnessImprovement"
                , "MemoryFitness"
                , "MemoryFitnessImprovement"
        };
        try {
            outputFileWriter = new CSVWriter(new FileWriter(outputFile));
            outputFileWriter.writeNext(entry);
        } catch (IOException e) {
            Logger.error(e, "Exception writing results to the output file: " + outputFile.getAbsolutePath());
            Logger.trace(e);
            System.exit(-1);
        }
    }

    protected void writePatch(UnitTestResultSet results, String methodName, double fitnessTime, double improvementTime, double fitnessMem, double improvementMem) {
        String[] entry = {methodName
                , results.getPatch().toString()
                , Boolean.toString(results.getCleanCompile())
                , Boolean.toString(results.allTestsSuccessful())
                , Float.toString(results.totalExecutionTime() / 1000000.0f)
                , Double.toString(fitnessTime)
                , Double.toString(improvementTime)
                , Double.toString(fitnessMem)
                , Double.toString(improvementMem)
        };
        outputFileWriter.writeNext(entry);
    }

    protected void writePatch(UnitTestResultSet resultSet, String methodName) {
        float execTime = resultSet.totalExecutionTime() / 1000000.0f;
        if (execTime == 0 || !resultSet.allTestsSuccessful()) execTime = Float.MAX_VALUE;
        long memoryUsage = resultSet.totalMemoryUsage();
        if (memoryUsage == 0 || !resultSet.allTestsSuccessful()) memoryUsage = Long.MAX_VALUE;
        writePatch(resultSet, methodName, execTime, initTime - execTime, memoryUsage, initMem - memoryUsage);
    }


}
