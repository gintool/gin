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
    
    @Argument(alias = "i", description = "Number of iterations for evolutionary search")
    protected Integer numIterations = 50;
    
    @Argument(alias = "p", description = "Population size for evolutionary search")
    protected Integer populationSize = 100;	//TODO set populationSize as relational to size of input program
    
    @Argument(alias = "e", description = "Number of elites for evolutionary search")
    protected Integer eliteSize = 10;
    
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
    	
    	List<Patch> firstPopulation = getFirstPopulation(fitterPatches);
    	//Logger.info(String.format("First Generation. found: %d Patches",firstPopulation.size()));
    	List<Patch> population = EvolutionarySearch(firstPopulation);
    	//Logger.info(String.format("Finished. found: %d Patches",population.size()));
    	
    	
    }
    
    //Evolutionary Algorithm for building new population based on given parent population
    private List<Patch> EvolutionarySearch(List<Patch> parentPatches){
    	//get testresult for patches and make list of UnitTestResultSets from only successful Patches
    	List<UnitTestResultSet> testResultSets = new ArrayList<UnitTestResultSet>();
    	for(Patch parentPatch : parentPatches) {
    		UnitTestResultSet testResultSet = testRunner.runTests(parentPatch, 1);
    		if(testResultSet.getValidPatch() && testResultSet.getCleanCompile() && testResultSet.allTestsSuccessful()) {
    			testResultSets.add(testResultSet);
    		}
    	}
    	//Logger.info(String.format("After deleting : %d HMOs", testResultSets.size()));
    	
    	//sort the beginning of the list to finnd k elites (k is parameter eliteSite)
    	kthSmallest(testResultSets,0,testResultSets.size()-1,testResultSets.size()>eliteSize?eliteSize:testResultSets.size());
    	
    	//add kth best Patches (elites) to childPopulation
    	List<Patch> childGeneration=new ArrayList<Patch>();
    	for (int i=0;i<(testResultSets.size()<eliteSize?testResultSets.size():eliteSize);++i) {
    		childGeneration.add(testResultSets.get(i).getPatch());
    	}
    	
    	//TODO generate more mutants by Crossover and Poitmutation
    	//TODO recursively call EvolutionarySearch, decrease numIterations by 1 each Recursion, Exit & print result when numIteraions hits 0
    	
    	return childGeneration;
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
    
  //merge multiple Patches to one Patch
    private Patch mergePatches(List<Patch> patches)
    {
    	if(patches==null || patches.isEmpty()){
    		return new Patch(this.sourceFile);
        }
        else if(patches.size()==1){
        	return patches.get(0);
        }
        else{
        	Patch newPatch = new Patch(this.sourceFile);
        	for(Patch patch : patches){
        		List<Edit> edits = patch.getEdits();
        		for(Edit edit : edits){
            		newPatch.add(edit);
            	}
        	}
          return newPatch;
        }
     }
    //create the first HOM-Population by Combining FOM-Patches
    private List<Patch> getFirstPopulation(List<Patch> FOMPatches)
    {
    	List<Patch> firstPopulation = new ArrayList<Patch>();
    	Random rand = new Random();
    	
    	for(int i=0; i<populationSize; ++i) {
    		List<Patch> HOMPatches = new ArrayList<Patch>();
    		
    		if(FOMPatches.size()<=1)	return FOMPatches;
    		//select multiple (minimum 2) FOM-Patches to merge into a HOM Patch 
    		for (int j=0; j<rand.nextInt((FOMPatches.size()-1))+1; j++) {
                int randomIndex = rand.nextInt(FOMPatches.size());
                Patch randomPatch = FOMPatches.get(randomIndex);
                HOMPatches.add(randomPatch);
            }
    		Patch mergedHOMPatch = mergePatches(HOMPatches);
    		firstPopulation.add(mergedHOMPatch);
    	}
    	return firstPopulation;
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
    
    
    //QUICKSELECT METHODS
    //Quickselect algorithm similar to Quicksort
    
    /**
     * @param testResults List of UnitTestResults
     * @param low lowest position of list (0 for calling)
     * @param high highest position of list (list.size()-1)
     * @param k element until which alues should be sorted (between 1 and list.size()) 
     */
    public int kthSmallest(List<UnitTestResultSet> testResults, int low, int high, int k)
    {
        int partition = partition(testResults, low, high);
 

        if (partition == k - 1)
            return 0;
 
        else if (partition < k - 1)
            return kthSmallest(testResults, partition + 1, high, k);
 
        else
            return kthSmallest(testResults, low, partition - 1, k);
    }
    public int partition(List<UnitTestResultSet> testResults, int low, int high)
    {
        long pivot = testResults.get(high).totalExecutionTime();
        int pivotlocation = low;
        for (int i = low; i <= high; i++) {
            // inserting testSets with lower runtime to the left
            if (testResults.get(i).totalExecutionTime() < pivot) {
                UnitTestResultSet temp = testResults.get(i);
                testResults.set(i,testResults.get(pivotlocation));
                testResults.set(pivotlocation,temp);
                pivotlocation++;
            }
        }
 
        // swapping pivot to the final pivot location
        UnitTestResultSet temp = testResults.get(high);
        testResults.set(high,testResults.get(pivotlocation));
        testResults.set(pivotlocation,temp);
 
        return pivotlocation;
    }
}
