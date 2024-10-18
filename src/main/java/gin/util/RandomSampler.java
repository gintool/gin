package gin.util;

import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import gin.Patch;
import gin.SourceFile;
import gin.edit.Edit;
import gin.edit.Edit.EditType;
import gin.edit.llm.LLMMaskedStatement;
import gin.edit.llm.LLMReplaceStatement;
import gin.test.UnitTestResultSet;

import org.apache.commons.rng.simple.JDKRandomBridge;
import org.apache.commons.rng.simple.RandomSource;
import org.pmw.tinylog.Logger;

import java.io.File;
import java.io.Serial;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;



/**
 * Random sampler.
 * <p>
 * Creates patchNumber random method patches of size 1:patchSize
 */

public class RandomSampler extends Sampler {

    @Serial
    private static final long serialVersionUID = 5754760811598365140L;

    @Argument(alias = "et", description = "Edit type: this can be a member of the EditType enum (LINE,STATEMENT,MATCHED_STATEMENT,MODIFY_STATEMENT); the fully qualified name of a class that extends gin.edit.Edit, or a comma separated list of both")
    protected String editType = EditType.LINE.toString();

    @Argument(alias = "ps", description = "Number of edits per patch")
    protected Integer patchSize = 1;

    @Argument(alias = "pn", description = "Number of patches")
    protected Integer patchNumber = 10;

    @Argument(alias = "rm", description = "Random seed for method selection")
    protected Integer methodSeed = 123;

    @Argument(alias = "rp", description = "Random seed for edit type selection")
    protected Integer patchSeed = 123;

    @Argument(alias = "pb", description = "Probablity of combiend")
    protected Double combinedProbablity = 0.5;


    // Whether to use LLM edits
    private boolean ifLLM = false;

    private Class <? extends Edit> LLMedit = null;

    private List<Class <? extends Edit>> NoneLLMedit = new ArrayList<>();

    private Random mutationRng;

    /**
     * allowed edit types for sampling: parsed from editType
     */
    protected List<Class<? extends Edit>> editTypes;

    public RandomSampler(String[] args) {
        super(args);
        Args.parseOrExit(this, args);
        editTypes = Edit.parseEditClassesFromString(editType);
        Setup();
        printAdditionalArguments();
    }



    private void Setup () {

        mutationRng = new JDKRandomBridge(RandomSource.MT, Long.valueOf(patchSeed));

        if (editTypes.contains(LLMMaskedStatement.class) || editTypes.contains(LLMReplaceStatement.class)) {
            ifLLM = true;
            if (editTypes.contains(LLMMaskedStatement.class)) {
                LLMedit = LLMMaskedStatement.class;
            } else if (editTypes.contains(LLMReplaceStatement.class)) {
                LLMedit = LLMReplaceStatement.class;
            }

            for (Class <? extends Edit> edit : editTypes) {
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


    // Constructor used for testing
    public RandomSampler(File projectDir, File methodFile) {
        super(projectDir, methodFile);
        editTypes = Edit.parseEditClassesFromString(editType);
        Setup();
    }

    public static void main(String[] args) {
        RandomSampler sampler = new RandomSampler(args);
        sampler.sampleMethods();
    }

    private void printAdditionalArguments() {
        Logger.info("Edit types: " + editTypes);
        Logger.info("Number of edits per patch: " + patchSize);
        Logger.info("Number of patches: " + patchNumber);
        Logger.info("Random seed for method selection: " + methodSeed);
        Logger.info("Random seed for edit type selection: " + patchSeed);
    }

     /**
     * Generate a neighbouring patch, by either deleting an edit, or adding a new one.
     *
     * @param patch Generate a neighbour of this patch.
     * @return A neighbouring patch.
     */
    Patch neighbour(Patch patch) {

        Patch neighbour = patch.clone();


        if(ifLLM && NoneLLMedit.size() > 0){
            if (mutationRng.nextFloat() > combinedProbablity) {

                neighbour.addRandomEditOfClasses(mutationRng, Arrays.asList(LLMedit));
            } 
            else {
                neighbour.addRandomEditOfClasses(mutationRng, NoneLLMedit);
            }
        } else {
            neighbour.addRandomEditOfClasses(mutationRng, editTypes);
        }

        return neighbour;
    }


    protected void sampleMethodsHook() {

        Random mrng = new JDKRandomBridge(RandomSource.MT, Long.valueOf(methodSeed));

        if (patchSize > 0) {

            writeHeader();

            int size = methodData.size();

            Logger.info("Start applying and testing random patches..");
            
            Logger.info("Number of patch: " + patchNumber);

            for (int i = 0; i < patchNumber; i++) {
                Random prng = new JDKRandomBridge(RandomSource.MT, patchSeed + (100000L * i));

                // Pick a random method
                TargetMethod method = methodData.get(mrng.nextInt(size));
                Integer methodID = method.getMethodID();
                File source = method.getFileSource();

                // Setup SourceFile for patching
                SourceFile sourceFile = SourceFile.makeSourceFileForEditTypes(editTypes, source.getPath(), Collections.singletonList(method.getMethodName()));

                Patch patch = new Patch(sourceFile);
                for (int j = 0; j < patchSize; j++) {
                    // patch.addRandomEditOfClasses(prng, editTypes, combinedProbablity);
                    patch = neighbour(patch);
                }

                Logger.info("Testing random patch " + patch + " for method: " + method + " with ID " + methodID);

                // Test the patched source file
                UnitTestResultSet results = testPatch(method.getClassName(), method.getGinTests(), patch, null);
                writeResults(results, methodID);
            }
            Logger.info("Results saved to: " + outputFile);

        } else {
            Logger.info("Number of edits  must be greater than 0.");
        }
    }

}
