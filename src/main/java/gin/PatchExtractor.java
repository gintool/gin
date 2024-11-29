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
			CSVReaderHeaderAware reader = new CSVReaderHeaderAware(new FileReader(patchFile));
			Map<String, String> data = reader.readMap();
			if ((!data.containsKey("Patch")) || (!data.containsKey("UnitTest"))) {
				throw new ParseException("Both \"Patch\" and \"UnitTest\" fields are required in the input file.", 0);
			}

			patches = new HashMap<>();
			ids = new HashMap<>();

			int idx = 0;

			while (data != null) {

				String patch = data.get("Patch");
				String test = data.get("UnitTest");
				String id = data.getOrDefault("PatchIndex", Integer.toString(idx));
				List<String> tests = patches.get(patch);
				if (tests == null) {
					tests = new ArrayList<>();
					idx++;
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
		for (String patch : patches.keySet()){
			String[] findpath = patch.split("\"");
			String filepath = "";
			for (String txt : findpath){
				if (txt.contains(".java")) {
					filepath = txt;
					break;
				}
			}
			String outputDir = patchFile.getName() + "_patch_" + ids.get(patch);
			Logger.info(outputDir);
			String[] args = new String[]{"-nr", "-f", filepath , "-od", outputDir, "-p", patch};
			//for (String arg : args){Logger.info(arg);}
			PatchAnalyser pa = new PatchAnalyser(args);
			pa.analyse();
			//for (String test : patches.get(patch)) {
			//Logger.info(test);
			//}
		}
	}

}
