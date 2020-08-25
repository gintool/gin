package gin.util;

import java.io.File;
import java.io.FileReader;
import org.apache.commons.io.FileUtils;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.List;
import java.util.ArrayList;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeoutException;
import java.text.ParseException;

import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.CSVWriter;
import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import org.pmw.tinylog.Logger;
import org.zeroturnaround.exec.ProcessExecutor;

import gin.Patch;
import gin.SourceFileLine;
import gin.SourceFileTree;
import gin.edit.Edit;
import gin.edit.Edit.EditType;
import gin.test.UnitTestResult;
import gin.test.UnitTestResultSet;
import java.io.FileWriter;
import org.apache.commons.lang3.SystemUtils;

public class PatchTesterNew extends Sampler {

    @Argument(alias = "patchFile", description = "File with a list of patches")
    protected File patchFile;

    @Argument(alias = "sf", description = "Skip failing patches? If there is patch execution information, then skips the ones that failed.")
    public boolean skipFailingPatches = true;

    protected List<Entry<String, Integer>> patchData = new ArrayList<>();

    public static void main(String[] args) {
        PatchTesterNew sampler = new PatchTesterNew(args);
        sampler.sampleMethods();
    }

    public PatchTesterNew(String[] args) {
        super(args);
        Args.parseOrExit(this, args);
        printAdditionalArguments();
        this.patchData = processPatchFile();
        if (patchData.isEmpty()) {
            Logger.info("No patches to process.");
            System.exit(0);
        }
    }

    // Constructor used for testing
    public PatchTesterNew(File projectDir, File methodFile, File patchFile) {
        super(projectDir, methodFile);
        this.patchFile = patchFile;
    }

    private void printAdditionalArguments() {
        Logger.info("Patch file: " + patchFile);
    }

    protected void sampleMethodsHook() {

        double original = 0;
        writeNewHeader();
        for (Entry<String, Integer> entry : patchData) {

            String patchText = entry.getKey();
            Integer methodID = entry.getValue();

            TargetMethod method = null;

            for (TargetMethod m : methodData) {
                if (m.getMethodID().equals(methodID)) {
                    method = m;
                    break;
                }
            }

            if (method == null) {

                Logger.info("Method with ID: " + methodID.toString() + " not found for patch " + patchText);

            } else {

                // Get method location
                File source = method.getFileSource();
                String className = method.getClassName();

                // Create source files for edits for the  method
                SourceFileLine sourceFileLine = new SourceFileLine(source.getPath(), null);
                SourceFileTree sourceFileTree = new SourceFileTree(source.getPath(), null);

                // Parse patch
                Patch patch = parsePatch(patchText, sourceFileLine, sourceFileTree);

                Logger.info("Running tests on patch: " + patch.toString());

                // Run all project tests (example sourceFile and className needed for TestRunner setup)
                //UnitTestResultSet results = testPatch(className, super.testData, patch);
                UnitTestResultSet results = testPatch(className, method.getGinTests(), patch);

                double fitness = (double) (results.totalExecutionTime() / 1000000);
                if (patch.toString().trim().equals("|")) {
                    original = fitness;
                }

                writePatch(results, method.getMethodName(), method.getMethodID().toString(), fitness, original - fitness);

                Logger.info("All tests successful: " + results.allTestsSuccessful());
            }
        }

        Logger.info("Results saved to: " + super.outputFile.getAbsolutePath());
    }

    private List<Entry<String, Integer>> processPatchFile() {

        try {
            CSVReaderHeaderAware reader = new CSVReaderHeaderAware(new FileReader(patchFile));
            Map<String, String> data = reader.readMap();
            if ((!data.containsKey("Patch")) || (!data.containsKey("MethodIndex"))) {
                throw new ParseException("Both \"Patch\" and \"MethodIndex\"  fields are required in the patch file.", 0);
            }

            List<Entry<String, Integer>> patches = new ArrayList<>();

            while (data != null) {

                String patch = data.get("Patch");
                Integer methodID = Integer.valueOf(data.get("MethodIndex"));
                if (skipFailingPatches
                        && data.containsKey("Compiled")
                        && data.containsKey("AllTestsPassed")) {
                    boolean compiled = Boolean.parseBoolean(data.get("Compiled"));
                    boolean passed = Boolean.parseBoolean(data.get("AllTestsPassed"));
                    if (!compiled || !passed) {
                        data = reader.readMap();
                        continue;
                    }
                }
                patches.add(new SimpleEntry(patch, methodID));
                data = reader.readMap();
            }
            reader.close();

            return patches;

        } catch (ParseException e) {
            Logger.error(e.getMessage());
            Logger.trace(e);
        } catch (IOException e) {
            Logger.error("Error reading patch file: " + patchFile);
            Logger.trace(e);
        }
        return new ArrayList<>();

    }

    private Patch parsePatch(String patchText, SourceFileLine sourceFileLine, SourceFileTree sourceFileTree) {

        List<Edit> editInstances = new ArrayList<>();
        String patchTrim = patchText.trim();
        String cleanPatch = patchTrim;

        if (patchTrim.equals("|")) {
            return new Patch(sourceFileLine);
        } else if (patchTrim.startsWith("|")) {
            cleanPatch = patchText.replaceFirst("\\|", "").trim();
        }

        String[] editStrings = cleanPatch.trim().split("\\|");

        boolean allLineEdits = true;
        boolean allStatementEdits = true;

        for (String editString : editStrings) {

            String[] tokens = editString.trim().split("\\s+");
            String editAction = tokens[0];
            Class<?> clazz = null;

            try {
                clazz = Class.forName(editAction);
            } catch (ClassNotFoundException e) {
                Logger.error("Patch edit type unrecognised: " + editAction);
                Logger.trace(e);
                System.exit(-1);
            }

            Method parserMethod = null;
            try {
                parserMethod = clazz.getMethod("fromString", String.class);
            } catch (NoSuchMethodException e) {
                Logger.error("Patch edit type has no fromString method: " + clazz.getCanonicalName());
                Logger.trace(e);
                System.exit(-1);
            }

            Edit editInstance = null;
            try {
                editInstance = (Edit) parserMethod.invoke(null, editString.trim());
            } catch (IllegalAccessException e) {
                Logger.error("Cannot parse patch: access error invoking edit class.");
                Logger.trace(e);
                System.exit(-1);
            } catch (InvocationTargetException e) {
                Logger.error("Cannot parse patch: invocation error invoking edit class.");
                Logger.trace(e);
                System.exit(-1);
            }

            allLineEdits &= editInstance.getEditType() == EditType.LINE;
            allStatementEdits &= editInstance.getEditType() != EditType.LINE;
            editInstances.add(editInstance);

        }

        if (!allLineEdits && !allStatementEdits) {
            Logger.error("Cannot proceed: mixed line/statement edit types found in patch");
            System.exit(-1);
        }

        Patch patch = new Patch(allLineEdits ? sourceFileLine : sourceFileTree);

        String original = patch.getSourceFile().toString();
        try {
            FileUtils.writeStringToFile(new File("source.original"), original, Charset.defaultCharset());
        } catch (IOException e) {
            Logger.error("Could not write original source.");
            Logger.trace(e);
            System.exit(-1);
        }

        for (Edit e : editInstances) {
            patch.add(e);
        }
        String patched = patch.apply();

        try {
            FileUtils.writeStringToFile(new File("source.patched"), patched, Charset.defaultCharset());
        } catch (IOException ex) {
            Logger.error("Could not write patched source.");
            Logger.trace(ex);
            System.exit(-1);
        }

        Logger.info("diff between original and patch");
        try {
            if (SystemUtils.IS_OS_UNIX) {
                String output = new ProcessExecutor().command("diff", "source.original", "source.patched")
                        .readOutput(true).execute()
                        .outputUTF8();
                Logger.info(output);
            }
        } catch (IOException ex) {
            Logger.error(ex);
            System.exit(-1);
        } catch (InterruptedException ex) {
            Logger.error(ex);
            System.exit(-1);
        } catch (TimeoutException ex) {
            Logger.error(ex);
            System.exit(-1);
        }

        return patch;
    }

    /*============== Helper methods  ==============*/
    protected void writeNewHeader() {
        String[] entry = {"MethodName",
            "MethodIndex",
            "Patch",
            "Compiled",
            "AllTestsPassed",
            "TotalExecutionTime(ms)",
            "Fitness",
            "FitnessImprovement",
            "TimeStamp",
            "NTests",
            "NPassed",
            "NFailed"
        };
        try {
            File parentFile = this.outputFile.getParentFile();
            if (parentFile != null) {
                parentFile.mkdirs();
            }
            outputFileWriter = new CSVWriter(new FileWriter(outputFile));
            outputFileWriter.writeNext(entry);
        } catch (IOException e) {
            Logger.error(e, "Exception writing results to the output file: " + outputFile.getAbsolutePath());
            Logger.trace(e);
            System.exit(-1);
        }
    }

    protected void writePatch(UnitTestResultSet results, String methodName, String methodIndex, double fitness, double improvement) {
        List<UnitTestResult> testResults = results.getResults();
        long nTests = testResults.size();
        long nPassed = testResults.stream()
                .filter(test -> test.getPassed())
                .count();
        long nFailed = nTests - nPassed;
        
        String[] entry = {methodName,
            methodIndex,
            results.getPatch().toString(),
            Boolean.toString(results.getCleanCompile()),
            Boolean.toString(results.allTestsSuccessful()),
            Float.toString(results.totalExecutionTime() / 1000000.0f),
            Double.toString(fitness),
            Double.toString(improvement),
            Long.toString(System.currentTimeMillis()),
            Long.toString(nTests),
            Long.toString(nPassed),
            Long.toString(nFailed)
        };
        outputFileWriter.writeNext(entry);
    }

}
