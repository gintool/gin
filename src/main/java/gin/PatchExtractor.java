package gin;

import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.exceptions.CsvValidationException;
import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import gin.edit.Edit;
import gin.edit.Edit.EditType;
import gin.test.InternalTestRunner;
import gin.test.UnitTestResult;
import gin.test.UnitTestResultSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.pmw.tinylog.Logger;

import java.io.*;
import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.text.ParseException;

/**
 * A handy utility for extracting patches from sampler_results.csv file and parsing through PatchAnalyser.
 */
public class PatchExtractor {

    @Argument(alias = "pl", description = "Patch list. Processes sampler_results.csv file to extract patches.", required = true)
    protected File patchFile;

    PatchExtractor(String[] args) {

        Args.parseOrExit(this, args);
	Map <String,List<String>> patches = processSamplerResults(patchFile);
        analysePatches(patches);
    }

    public static void main(String[] args) {
        PatchExtractor analyser = new PatchExtractor(args);
    }

    private Map<String,List<String>> processSamplerResults(File patchFile) {

        try {
            CSVReaderHeaderAware reader = new CSVReaderHeaderAware(new FileReader(patchFile));
            Map<String, String> data = reader.readMap();
            if ((!data.containsKey("Patch")) || (!data.containsKey("UnitTest"))) {
                throw new ParseException("Both \"Patch\" and \"UnitTest\" fields are required in the input file.", 0);
            }

            Map<String,List<String>> patches = new HashMap<>();

            int idx = 0;

            while (data != null) {

                String patch = data.get("Patch");
                String test = data.get("UnitTest");
		List<String> tests = patches.get(patch);
		if (tests == null) {
			tests = new ArrayList<>();
 		}
		tests.add(test);
		patches.put(patch,tests);
                data = reader.readMap();
	    }
            reader.close();
	    return patches;

        } catch (IOException e) {
            e.printStackTrace();
	    return null;
        } catch (ParseException e) {
            e.printStackTrace();
	    return null;
        } catch (CsvValidationException e) {
            e.printStackTrace();
	    return null;
        } 
    }
    private void analysePatches(Map<String,List<String>> patches) {
        Integer patchID = 0;
	for (String patch : patches.keySet()){
	    String[] findpath = patch.split("\"");
	    String filepath = "";
	    for (String txt : findpath){
		if (txt.contains(".java")) {
		    filepath = txt;
		    break;
		}
	    }
	    String outputDir = "patch" + patchID.toString();
	    Logger.info(outputDir);
	    String[] args = new String[]{"-p", patch, "-nr", "-f", filepath , "-od", outputDir};
	    //for (String arg : args){Logger.info(arg);}
            PatchAnalyser pa = new PatchAnalyser(args);
            pa.analyse();
	    //for (String test : patches.get(patch)) {
	        //Logger.info(test);
	    //}
	    patchID = patchID + 1;
	}
    }

}
