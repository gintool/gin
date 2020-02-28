package gin.util;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;

import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;

import org.apache.commons.rng.simple.JDKRandomBridge;
import org.apache.commons.rng.simple.RandomSource;
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
 * Creates patchNumber random method patches of size 1:patchSize
 */

public class RandomSampler extends Sampler {
    
    @Argument(alias = "et", description = "Edit type")
    protected EditType editType = EditType.LINE;

    @Argument(alias = "ps", description = "Number of edits per patch")
    protected Integer patchSize = 1;

    @Argument(alias = "pn", description = "Number of patches")
    protected Integer patchNumber = 10;

    @Argument(alias = "rm", description = "Random seed for method selection")
    protected Integer methodSeed = 123;

    @Argument(alias = "rp", description = "Random seed for edit type selection")
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
        Logger.info("Number of edits per patch: "+ patchSize);
        Logger.info("Number of patches: "+ patchNumber);
        Logger.info("Random seed for method selection: "+ methodSeed);
        Logger.info("Random seed for edit type selection: "+ patchSeed);
    }

   protected void sampleMethods() {

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
                SourceFile sourceFile = SourceFile.makeSourceFileForEditType(editType, source.getPath(), method.getMethodName());

                Patch patch = new Patch(sourceFile);
                for (int j = 0; j < patchSize; j++) {
                    patch.addRandomEdit(prng, editType);
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
