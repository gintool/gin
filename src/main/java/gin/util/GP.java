package gin.util;

import java.io.IOException;
import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Random;

import com.opencsv.CSVWriter;
import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import org.pmw.tinylog.Logger;

import gin.Patch;
import gin.SourceFile;
import gin.edit.Edit.EditType;
import gin.test.UnitTest;
import gin.test.UnitTestResultSet;


/**
 * Method-based General GP search.
 *
 */

public abstract class GP extends Sampler {
    
    @Argument(alias = "et", description = "Edit type")
    protected EditType editType = EditType.MATCHED_STATEMENT;

    @Argument(alias = "gn", description = "Number of generations")
    protected Integer genNumber = 1;

    @Argument(alias = "in", description = "Number of individuals")
    protected Integer indNumber = 10; 

    @Argument(alias = "ms", description = "Random seed for mutation selection")
    protected Integer mutationSeed = 123;

    @Argument(alias = "is", description = "Random seed for individual selection")
    protected Integer individualSeed = 123;

    protected Random mutationRng;
    protected Random individualRng;
    
       public GP(String[] args) {
        super(args);
        Args.parseOrExit(this, args);
        printAdditionalArguments();
        setup();
    }

    // Constructor used for testing
    public GP(File projectDir, File methodFile) {
        super(projectDir, methodFile);
    }

    private void printAdditionalArguments() {
        Logger.info("Edit type: "+ editType);
        Logger.info("Number of generations: "+ genNumber);
        Logger.info("Number of individuals: "+ indNumber);
        Logger.info("Random seed for mutation selection: "+ mutationSeed);
        Logger.info("Random seed for individual selection: "+ individualSeed);
    }

    private void setup() {
        mutationRng = new Random(mutationSeed);
        individualRng = new Random(individualSeed);
    }

    // Implementation of the abstract method
    protected void sampleMethods() {

        writeNewHeader();

        for (TargetMethod method : methodData) {

            Logger.info("Running GP on method " + method);

            // Setup SourceFile for patching
            SourceFile sourceFile = SourceFile.makeSourceFileForEditType(editType, method.getFileSource().getPath(), method.getMethodName());

            search(method.getClassName(), method.getGinTests(), sourceFile);

        }

    }

       /*============== Abstract methods  ==============*/

    // Simple patch selection
    protected abstract Patch select(List<Patch> patches);

    // Mutation operator
    protected abstract Patch mutate(Patch oldPatch);

    // Crossover operator
    protected abstract List<Patch> createCrossoverPatches(List<Patch> patches, SourceFile sourceFile);

    // Calculate fitness
    protected abstract long fitness(UnitTestResultSet results);

    // Calculate fitness threshold, for selection to the next generation
    protected abstract boolean fitnessThreshold(UnitTestResultSet results, long originalFitness);

    // GP search strategy
    protected abstract void search(String className, List<UnitTest> tests, SourceFile sourceFile);

    // Compare two fitness values
    protected abstract long compareFitness(long newFitness, long oldFitness);

    /*============== Helper methods  ==============*/

    protected void writeNewHeader() {
        String[] entry = {"ClassName"
                        , "Patch"
                        , "Compiled"
                        , "AllTestsPassed"
                        , "TotalExecutionTime(ms)"
                        , "Fitness"
                        , "FitnessImprovement"
                        };
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(outputFile));
            writer.writeNext(entry);
            writer.close();
        } catch (IOException e) {
            Logger.error(e, "Exception writing results to the output file: " + outputFile.getAbsolutePath());
            Logger.trace(e);
            System.exit(-1);
        }
    }

    protected void writePatch(UnitTestResultSet results, String className, long fitness, long improvement) {
        String[] entry = {className
                        , results.getPatch().toString()
                        , Boolean.toString(results.getCleanCompile())
                        , Boolean.toString(results.allTestsSuccessful())
                        , Float.toString(results.totalExecutionTime() / 1000000.0f)
                        , Long.toString(fitness)
                        , Long.toString(improvement)
                        };
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(outputFile, true));
            writer.writeNext(entry);
            writer.close();
        } catch (IOException e) {
            Logger.error(e, "Exception writing results to the output file: " + outputFile.getAbsolutePath());
            Logger.trace(e);
            System.exit(-1);
        }
    }

}
