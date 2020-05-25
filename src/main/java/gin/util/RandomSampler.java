package gin.util;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.commons.rng.simple.JDKRandomBridge;
import org.apache.commons.rng.simple.RandomSource;
import org.pmw.tinylog.Logger;

import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;

import gin.Patch;
import gin.SourceFile;
import gin.edit.Edit;
import gin.edit.Edit.EditType;
import gin.test.UnitTestResultSet;


/**
 * Random sampler. 
 *
 * Creates patchNumber random method patches of size 1:patchSize
 */

public class RandomSampler extends Sampler {
    
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

    /**allowed edit types for sampling: parsed from editType*/
    protected List<Class<? extends Edit>> editTypes;

    public static void main(String[] args) {
        RandomSampler sampler = new RandomSampler(args);
        sampler.sampleMethods();
    }

    public RandomSampler(String[] args) {
        super(args);
        Args.parseOrExit(this, args);
        editTypes = Edit.parseEditClassesFromString(editType);
        printAdditionalArguments();
    }

    // Constructor used for testing
    public RandomSampler(File projectDir, File methodFile) {
        super(projectDir, methodFile);
        editTypes = Edit.parseEditClassesFromString(editType);
    }

    private void printAdditionalArguments() {
        Logger.info("Edit types: "+ editTypes);
        Logger.info("Number of edits per patch: "+ patchSize);
        Logger.info("Number of patches: "+ patchNumber);
        Logger.info("Random seed for method selection: "+ methodSeed);
        Logger.info("Random seed for edit type selection: "+ patchSeed);
    }

   protected void sampleMethodsHook() {

        Random mrng = new JDKRandomBridge(RandomSource.MT, Long.valueOf(methodSeed)); 
        
        if (patchSize > 0) {

            writeHeader();

            int size = methodData.size();

            Logger.info("Start applying and testing random patches..");

            for (int i = 0; i < patchNumber; i++) {
                Random prng = new JDKRandomBridge(RandomSource.MT, Long.valueOf(patchSeed + (100000 * i)));
                
                // Pick a random method
                TargetMethod method = methodData.get(mrng.nextInt(size));
                Integer methodID = method.getMethodID(); 
                File source = method.getFileSource();

                // Setup SourceFile for patching
                SourceFile sourceFile = SourceFile.makeSourceFileForEditTypes(editTypes, source.getPath(), Collections.singletonList(method.getMethodName()));

                Patch patch = new Patch(sourceFile);
                for (int j = 0; j < patchSize; j++) {
                    patch.addRandomEditOfClasses(prng, editTypes);
                }

                Logger.info("Testing random patch " + patch + " for method: " + method + " with ID " + methodID);

                // Test the patched source file
                UnitTestResultSet results = testPatch(method.getClassName(), method.getGinTests(), patch);
                writeResults(results, methodID);
            }

        Logger.info("Results saved to: " + outputFile);

        } else {
            Logger.info("Number of edits  must be greater than 0.");
        }

   }


}
