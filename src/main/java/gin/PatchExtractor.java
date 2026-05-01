package gin;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pmw.tinylog.Logger;

import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.CSVReaderHeaderAwareBuilder;
import com.opencsv.RFC4180Parser;
import com.opencsv.RFC4180ParserBuilder;
import com.opencsv.exceptions.CsvValidationException;
import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;

/**
 * A handy utility for extracting patches from sampler_results.csv file and parsing through PatchAnalyser.
 */
public class PatchExtractor {

	@Argument(alias = "pl", description = "Patch list. Processes sampler_results.csv file to extract patches.", required = true)
	protected File patchFile;

	
	private Map<String,List<String>> patches = new HashMap<>();
	private Map<String, String> ids = new HashMap<>();
	
	PatchExtractor(String[] args) {

		Args.parseOrExit(this, args);
		processSamplerResults();
		analysePatches();
	}

	public static void main(String[] args) {
		PatchExtractor analyser = new PatchExtractor(args);
	}

	private void processSamplerResults() {

		try {
			//CSVReaderHeaderAware reader = new CSVReaderHeaderAware(new FileReader(patchFile));
			RFC4180Parser parser = new RFC4180ParserBuilder().build();
			CSVReaderHeaderAware reader = (CSVReaderHeaderAware)(new CSVReaderHeaderAwareBuilder(new FileReader(patchFile)).withCSVParser(parser).build());
            
			Map<String, String> data = reader.readMap();
			if ((!data.containsKey("Patch"))) { // || (!data.containsKey("UnitTest"))) {
				//throw new ParseException("Both \"Patch\" and \"UnitTest\" fields are required in the input file.", 0); // UnitTest only present in the RandomSampler rather than local search file. Not needed here anyway.
				throw new ParseException("\"Patch\" field is required in the input file.", 0);
			}

			patches = new HashMap<>();
			ids = new HashMap<>();

			int idx = 0;

			while (data != null) {

				String patch = data.get("Patch");
				String test = data.getOrDefault("UnitTest", "NO_TEST");
				String id = data.getOrDefault("PatchIndex", Integer.toString(idx));
				List<String> tests = patches.get(patch);
				if (tests == null) {
				    tests = new ArrayList<>();
				    idx++;
				} else {
	                            if (!data.containsKey("UnitTest")) { // we are here because we've seen the patch before, and we don't have UnitTests (so it's an aggr>
                                        Logger.info("Patch " + id + " was a duplicate of a previous: " + ids.get(patch));
        	                    }
                                }

				tests.add(test);
				patches.put(patch,tests);
				ids.put(patch, id);
				data = reader.readMap();
			}
			reader.close();

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (CsvValidationException e) {
			e.printStackTrace();
		} 
	}
	private void analysePatches() {
		for (String patch : patches.keySet()){ // NB these won't be processed in patch index order, but that doesn't matter
			Logger.info("Processing patch #" + ids.get(patch) + "...");
			Logger.info(patch);
			
			try {
				String[] findpath = patch.split("\"");
				String filepath = "";
				for (String txt : findpath){
					if (txt.contains(".java")) {
						filepath = txt;
						break;
					}
				}
				String outputDir = patchFile.getName() + "_patch_" + ids.get(patch);
				Logger.info("Writing to dir " + outputDir);
				String[] args = new String[]{"-nr", "-f", filepath , "-od", outputDir, "-p", patch};
				//for (String arg : args){Logger.info(arg);}
				PatchAnalyser pa = new PatchAnalyser(args);
				pa.analyse();
				//for (String test : patches.get(patch)) {
				//Logger.info(test);
				//}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
