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
    protected Integer populationSize = 50;	//TODO set populationSize as relational to size of input program
    
    @Argument(alias = "e", description = "Number of elites for evolutionary search")
    protected Integer eliteSize = 10;
    
    @Argument(alias = "ts", description = "Tournament size for evolutionary search")
    protected Integer tournamentSize = 5;
    
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
    	List<Patch> finalPopulation = EvolutionarySearch(firstPopulation, numIterations);
    	//find Patch with lowest runtime
    	List<UnitTestResultSet> finalTestResultSets = new ArrayList<UnitTestResultSet>();
    	for(Patch patch : finalPopulation) {
    		UnitTestResultSet testResultSet = testRunner.runTests(patch, 1);
    		finalTestResultSets.add(testResultSet);
    	}
    	//order list to have best element on index 0
    	kthSmallest(finalTestResultSets, 0,finalTestResultSets.size()-1,1);
    	Patch finalPatch = finalTestResultSets.get(0).getPatch();
    	long origTime = timeOriginalCode();
    	long bestTime = finalTestResultSets.get(0).totalExecutionTime();
    	Logger.info(String.format("Finished Evolutionary Search. Best time: %d (ns), Speedup (%%): %.2f, Patch: %s",
                bestTime, 100.0f *((origTime - bestTime)/(1.0f * origTime)), finalPatch));
    	finalPatch.writePatchedSourceToFile(sourceFile.getFilename() + ".optimised");
    }
    
    //Evolutionary Algorithm for building new population based on given parent population
    private List<Patch> EvolutionarySearch(List<Patch> parentPatches, int iterationCounter){
    	//get testResultSet for patches and make list of UnitTestResultSets from only successful Patches
    	List<UnitTestResultSet> testResultSets = new ArrayList<UnitTestResultSet>();
    	for(Patch parentPatch : parentPatches) {
    		UnitTestResultSet testResultSet = testRunner.runTests(parentPatch, 1);
    		if(testResultSet.getValidPatch() && testResultSet.getCleanCompile() && testResultSet.allTestsSuccessful()) {
    			testResultSets.add(testResultSet);
    		}
    	}
    	
    	//TODO what happens if list is empty because every HOM-Patch failed
    	//current soltion: return list with just original Patch
    	if(testResultSets.size()==0) {
    		List<Patch> originalPatch = new ArrayList<Patch>();
    		originalPatch.add(new Patch(this.sourceFile));
    		return originalPatch;
    	}
    	//if iteration counter hits 0: leave recursion, return current population (after sorting out invalid patches above)
    	if(iterationCounter==0) {
    		List<Patch> finalPatches = new ArrayList<Patch>();
    		for(UnitTestResultSet testResultSet : testResultSets) {
    			finalPatches.add(testResultSet.getPatch());
    		}
    		return finalPatches;
    	}
    	
    	//sort the beginning of the list to find k elites (k is parameter eliteSite)
    	kthSmallest(testResultSets,0,testResultSets.size()-1,testResultSets.size()>eliteSize?eliteSize:testResultSets.size());
    	
    	//add kth best Patches (elites) to childPopulation
    	List<Patch> childGeneration=new ArrayList<Patch>();
    	for (int i=0;i<(testResultSets.size()<eliteSize?testResultSets.size():eliteSize);++i) {
    		childGeneration.add(testResultSets.get(i).getPatch());
    	}
    	
    	//Crossover probability is set to 0.8
    	//Tournament selection for selecting crossovers:
    	for(int i=0; i<0.8*(populationSize-eliteSize); ++i) {
    		List<UnitTestResultSet> tournamentList = new ArrayList<UnitTestResultSet>();
	    	for(int j=0; j<tournamentSize;++j) {
	    		tournamentList.add(testResultSets.get(rng.nextInt(testResultSets.size())));
	    	}
	    	//order list for two elements with best fitness, perform crossover on these two patches
	    	kthSmallest(tournamentList,0,tournamentSize-1,2);
	    	childGeneration.add(crossover(tournamentList.get(0).getPatch(),tournamentList.get(1).getPatch()));
    	}
    	//Mutation Probability is 0.2
    	//find random Patches, perform Pointmutations and add them in the childGeneraion
    	for(int i=0; i<0.2*(populationSize-eliteSize); ++i) {
    		Patch patchToMutate = testResultSets.get(rng.nextInt(testResultSets.size())).getPatch();
    		childGeneration.add(pointMutation(patchToMutate));
    	}
    	
    	return EvolutionarySearch(childGeneration, --iterationCounter);
    }
    
    //dummy for crossover method
    private Patch crossover(Patch p1, Patch p2) {
    	if(rng.nextBoolean())	return p1;
    	else	return p2;
    }
    //dummy for pointMutation method
    private Patch pointMutation(Patch patch) {
    	patch.addRandomEditOfClasses(rng, editTypes);
    	return patch;
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
    	List<UnitTestResultSet> testResultSets = new ArrayList<UnitTestResultSet>();
    	for(Patch FOMPatch : FOMPatches) {
    		UnitTestResultSet testResultSet = testRunner.runTests(FOMPatch, 1);
    		if(testResultSet.getValidPatch() && testResultSet.getCleanCompile() && testResultSet.allTestsSuccessful()) {
    			testResultSets.add(testResultSet);
    		}
    	}
    	//TODO what to do if no patch is valid
    	if(testResultSets.size()==0) {
    		Logger.info(String.format("Found no valid FOM Patches"));
    		return FOMPatches;
    	}
    	//sort the beginning of the list to find k elites (k is parameter eliteSite)
    	kthSmallest(testResultSets,0,testResultSets.size()-1,testResultSets.size()>eliteSize?eliteSize:testResultSets.size());
    	
    	List<Patch> firstPopulation = new ArrayList<Patch>();
    	
    	//add kth best FOM-Patches (elites) to firstPopulation
    	for (int i=0;i<(testResultSets.size()<eliteSize?testResultSets.size():eliteSize);++i) {
    		firstPopulation.add(testResultSets.get(i).getPatch());
    	}
    	
    	for(int i=0; i<populationSize-eliteSize; ++i) {
    		List<Patch> HOMPatches = new ArrayList<Patch>();
    		
    		if(FOMPatches.size()<=1) {
    			Logger.info(String.format("Found no FOM Patches to merge"));
    			return FOMPatches;
    		}
    		//select multiple (minimum 1, maximum 5) FOM-Patches to merge into a HOM Patch 
    		for (int j=0; j<rng.nextInt(((FOMPatches.size()>5?5:FOMPatches.size())-1))+1; j++) {
                int randomIndex = rng.nextInt(FOMPatches.size());
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
    //changes order of list elements so that lowest k elements are in the lowest k inices
    
    /**
     * @param testResults List of UnitTestResults
     * @param low lowest position of list (0 for calling)
     * @param high highest position of list (list.size()-1)
     * @param k element until which alues should be sorted (between 1 and list.size()) 
     */
    public int kthSmallest(List<UnitTestResultSet> testResults, int low, int high, int k)
    {
        int partition = partition(testResults, low, high);

        if (partition == k-1)
            return 0;
 
        else if (partition < k-1)
            return kthSmallest(testResults, partition+1, high, k);
 
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
    
    //TODO methods for testing purpose, delete later!
    public void printUnitTestResults(List<UnitTestResultSet> testResults) {
    	long origTime = timeOriginalCode();
    	Logger.info(String.format("#################### Printing list ##########################"));
    	for(int i=0;i<testResults.size();++i) {
    		if(!(testResults.get(i).getValidPatch() && testResults.get(i).getCleanCompile() && testResults.get(i).allTestsSuccessful())) {
    			continue;
    		}
    		
    		long bestTime = testResults.get(i).totalExecutionTime();
        			
	    	Logger.info(String.format("List item %d. Best time: %d (ns), Speedup (%%): %.2f,",
	                i, bestTime,100.0f *((origTime - bestTime)/(1.0f * origTime))));
    	}
    	Logger.info(String.format("#################### Done Printing list ##########################"));
    }
    
    public void printPatches(List<Patch> patches, int k) {
    	if(patches.size()>k)	patches = patches.subList(0, k);
    	List<UnitTestResultSet> testResultSets = new ArrayList<UnitTestResultSet>();
    	for(Patch patch : patches) {
    		UnitTestResultSet testResultSet = testRunner.runTests(patch, 1);
    		testResultSets.add(testResultSet);
    	}
    	printUnitTestResults(testResultSets);
    }
}
