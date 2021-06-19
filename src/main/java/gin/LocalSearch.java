package gin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.rng.simple.JDKRandomBridge;
import org.apache.commons.rng.simple.RandomSource;
import org.pmw.tinylog.Logger;

import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;

import gin.edit.Edit;
import gin.edit.Edit.EditType;
import gin.test.InternalTestRunner;
import gin.test.UnitTestResult;
import gin.test.UnitTestResultSet;

/**
 * Simple local search. Takes a source filename and a method signature, optimises it.
 * Assumes the existence of accompanying Test Class.
 * The class must be in the top level package, if classPath not provided.
 */
public class LocalSearch {

    private static final int WARMUP_REPS = 10;

    @Argument(alias = "f", description = "Required: Source filename", required=true)
    protected File filename = null;

    @Argument(alias = "m", description = "Required: Method signature including arguments." +
                                         "For example, \"classifyTriangle(int,int,int)\"", required=true)
    protected String methodSignature = "";

    @Argument(alias = "s", description = "Seed")
    protected Integer seed = 123;

    @Argument(alias = "n", description = "Number of steps")
    protected Integer numSteps = 100;

    @Argument(alias = "d", description = "Top directory")
    protected File packageDir;

    @Argument(alias = "c", description = "Class name")
    protected String className;

    @Argument(alias = "cp", description = "Classpath")
    protected String classPath;

    @Argument(alias = "t", description = "Test class name")
    protected String testClassName;
    
    @Argument(alias = "et", description = "Edit type: this can be a member of the EditType enum (LINE,STATEMENT,MATCHED_STATEMENT,MODIFY_STATEMENT); the fully qualified name of a class that extends gin.edit.Edit, or a comma separated list of both")
    protected String editType = EditType.LINE.toString();
    
    @Argument(alias = "hom", description = "Way of search. When argument is set, it will be searched for Higher-Order-Mutations")
    protected boolean homEnabled;
    
    /**allowed edit types for sampling: parsed from editType*/
    protected List<Class<? extends Edit>> editTypes;
    
    @Argument(alias = "ff", description = "Fail fast. "
            + "If set to true, the tests will stop at the first failure and the next patch will be executed. "
            + "You probably don't want to set this to true for Automatic Program Repair.")
    protected Boolean failFast = false;

    protected SourceFile sourceFile;
    InternalTestRunner testRunner;
    protected Random rng;

    // Instantiate a class and call search
    public static void main(String[] args) {
        LocalSearch simpleLocalSearch = new LocalSearch(args);
        simpleLocalSearch.search();
    }

    // Constructor parses arguments
    LocalSearch(String[] args) {

        Args.parseOrExit(this, args);
        editTypes = Edit.parseEditClassesFromString(editType);

        this.sourceFile = SourceFile.makeSourceFileForEditTypes(editTypes, this.filename.toString(), Collections.singletonList(this.methodSignature));
        
        this.rng = new JDKRandomBridge(RandomSource.MT, Long.valueOf(seed));
        if (this.packageDir == null) {
            this.packageDir = (this.filename.getParentFile()!=null) ? this.filename.getParentFile().getAbsoluteFile() : new File(System.getProperty("user.dir"));
        }
        if (this.classPath == null) {
            this.classPath = this.packageDir.getAbsolutePath();
        }
        if (this.className == null) {
            this.className = FilenameUtils.removeExtension(this.filename.getName());
        }
        if (this.testClassName == null) {
            this.testClassName = this.className + "Test";
        }
        this.testRunner = new InternalTestRunner(className, classPath, testClassName, failFast);

    }

    // Apply empty patch and return execution time
    private long timeOriginalCode() {

        Patch emptyPatch = new Patch(this.sourceFile);
        UnitTestResultSet resultSet = testRunner.runTests(emptyPatch, WARMUP_REPS);

        if (!resultSet.allTestsSuccessful()) {

            if (!resultSet.getCleanCompile()) {

                Logger.error("Original code failed to compile");

            } else {

                Logger.error("Original code failed to pass unit tests");

                for (UnitTestResult testResult: resultSet.getResults()) {
                    Logger.error(testResult);
                }

            }

            System.exit(0);

        }

        return resultSet.totalExecutionTime() / WARMUP_REPS;

    }
    
    private void search()
    {
    	if(homEnabled) {
    		searchWithHOM();
    	}
    	else {
    		searchWithoutHOM();
    	}
    }

    private void searchWithHOM()
    {
    	List<Patch> fitterPatches = getFitterPatches();
    	Set<Integer> modifiedLines = getModifiedLines(fitterPatches);
    	
    	//TODO HOM to be implemented
    	
    }
    
    private List<Patch> getFitterPatches()
    {
    	List<Patch> fitterPatches = new ArrayList<Patch>();
    	
        Logger.info(String.format("Localsearch on file: %s method: %s with HOM", filename, methodSignature));

        // Time original code
        long origTime = timeOriginalCode();
        Logger.info("Original execution time: " + origTime + "ns");

        // Start with empty patch
        Patch origPatch = new Patch(this.sourceFile);

        for (int step = 1; step <= numSteps; step++) {

            Patch neighbour = neighbour(origPatch);
            UnitTestResultSet testResultSet = testRunner.runTests(neighbour, 1);

            StringBuilder msg = new StringBuilder();
            
            if(checkIfPatchIsFitter(neighbour, testResultSet, origTime, msg)) {
                fitterPatches.add(neighbour);
                msg.append("Found fitter Patch with time: ").append(testResultSet.totalExecutionTime()).append("(ns)");
            }

            Logger.info(String.format("Step: %d, Patch: %s, %s ", step, neighbour, msg));

        }
    	
    	return fitterPatches;
    }
    
    private Set<Integer> getModifiedLines(List<Patch> patches)
    {
    	return patches.parallelStream().map(p -> p.getEditedLines()).flatMap(l -> l.stream()).collect(Collectors.toSet());
    }
    
    // Simple local search
    private void searchWithoutHOM() {

        Logger.info(String.format("Localsearch on file: %s method: %s", filename, methodSignature));

        // Time original code
        long origTime = timeOriginalCode();
        Logger.info("Original execution time: " + origTime + "ns");

        // Start with empty patch
        Patch bestPatch = new Patch(this.sourceFile);
        long bestTime = origTime;

        for (int step = 1; step <= numSteps; step++) {

            Patch neighbour = neighbour(bestPatch);
            UnitTestResultSet testResultSet = testRunner.runTests(neighbour, 1);

            StringBuilder msg = new StringBuilder();
            
            if(checkIfPatchIsFitter(neighbour, testResultSet, bestTime, msg)) {
                bestPatch = neighbour;
                bestTime = testResultSet.totalExecutionTime();
                msg.append("New best time: ").append(bestTime).append("(ns)");
            }

            Logger.info(String.format("Step: %d, Patch: %s, %s ", step, neighbour, msg));

        }

        Logger.info(String.format("Finished. Best time: %d (ns), Speedup (%%): %.2f, Patch: %s",
                                    bestTime,
                                    100.0f *((origTime - bestTime)/(1.0f * origTime)),
                                    bestPatch));

        bestPatch.writePatchedSourceToFile(sourceFile.getFilename() + ".optimised");

    }
    
    private boolean checkIfPatchIsFitter(Patch patch, UnitTestResultSet testResultSet, Long origTime, StringBuilder msg) {

        if (!testResultSet.getValidPatch()) {
            msg.append("Patch invalid");
        } else if (!testResultSet.getCleanCompile()) {
            msg.append("Failed to compile");
        } else if (!testResultSet.allTestsSuccessful()) {
            msg.append("Failed to pass all tests");
        } else if (testResultSet.totalExecutionTime() >= origTime) {
            msg.append("Time: ").append(testResultSet.totalExecutionTime()).append("ns");
        } else {
        	return true;
        }
        
        return false;
        
    }


    /**
     * Generate a neighbouring patch, by either deleting an edit, or adding a new one.
     * @param patch Generate a neighbour of this patch.
     * @return A neighbouring patch.
     */
    Patch neighbour(Patch patch) {

        Patch neighbour = patch.clone();

        if (neighbour.size() > 0 && rng.nextFloat() > 0.5) {
            neighbour.remove(rng.nextInt(neighbour.size()));
        } else {
            neighbour.addRandomEditOfClasses(rng, editTypes);
        }
        
        return neighbour;

    }
}
