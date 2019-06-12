package gin.util;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;

import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import org.pmw.tinylog.Logger;

import gin.Patch;
import gin.SourceFile;
import gin.edit.Edit.EditType;
import gin.test.UnitTest;
import gin.test.UnitTestResult;
import gin.test.UnitTestResultSet;


/**
 * Random sampler. 
 *
 * Creates patchNumber random method patches of random size 1:maxPatchSize
 */

public class RandomSampler extends Sampler {
    
    @Argument(alias = "et", description = "Edit type")
    protected EditType editType = EditType.LINE;

    @Argument(alias = "ps", description = "Max patch size")
    protected Integer maxPatchSize = 1;

    @Argument(alias = "pn", description = "Number of patches")
    protected Integer patchNumber = 10;

    @Argument(alias = "rm", description = "Random seed for method selection")
    protected Integer methodSeed = 123;

    @Argument(alias = "rp", description = "Random seed for patch size selection")
    protected Integer patchSeed = 123;


    public static void main(String[] args) {
        RandomSampler sampler = new RandomSampler(args);
        sampler.sampleMethods();
    }

    public RandomSampler(String[] args) {
        super(args);
        Args.parseOrExit(this, args);
        printAdditionalArguments();
    }

    // Constructor used for testing
    public RandomSampler(File projectDir, File methodFile) {
        super(projectDir, methodFile);
    }

    private void printAdditionalArguments() {
        Logger.info("Edit type: "+ editType);
        Logger.info("Max patch size: "+ maxPatchSize);
        Logger.info("Number of patches: "+ patchNumber);
        Logger.info("Random seed for method selection: "+ methodSeed);
        Logger.info("Random seed for patch size selection: "+ patchSeed);
    }

   protected void sampleMethods() {

        Random mrng = new Random(methodSeed); 
        Random prng = new Random(patchSeed);

        if (maxPatchSize > 0) {

            writeHeader();

            int size = methodData.size();

            Logger.info("Start applying and testing random patches..");

            for (int i = 0; i < patchNumber; i++) {

                // Pick a random method
                TargetMethod method = methodData.get(mrng.nextInt(size));
                Integer methodID = method.getMethodID(); 
                File source = method.getFileSource();

                // Setup SourceFile for patching
                SourceFile sourceFile = SourceFile.makeSourceFileForEditType(editType, source.getPath(), method.getMethodName());

                Patch patch = new Patch(sourceFile);
                int patchSize = prng.nextInt(maxPatchSize);
                for (int j = 0; j < patchSize + 1; j++) {
                    patch.addRandomEdit(prng, editType);
                }

                Logger.info("Testing random patch " + patch + " for method: " + method + " with ID " + methodID);

                // Test the patched source file
                UnitTestResultSet results = testPatch(method.getClassName(), method.getGinTests(), patch);
                writeResults(results, methodID);
            }

        Logger.info("Results saved to: " + outputFile);

        } else {
            Logger.info("Max patch size must be greater than 0.");
        }

   }


}
