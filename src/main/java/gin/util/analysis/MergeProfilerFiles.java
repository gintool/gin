package gin.util.analysis;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.pmw.tinylog.Logger;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;


public class MergeProfilerFiles {

	@Argument(alias = "d", description = "Root directory for filename patterns; defaults to current dir", required = false)
    protected String dir = ".";
	
	@Argument(alias = "if", description = "Comma separated list of filename patterns, e.g. spark.Profiler_output_*.csv or spark.Profiler_output_1.csv,spark.Profiler_output_2.csv,spark.Profiler_output_3.csv", required = true)
    protected String inputFiles;

	@Argument(alias = "of", description = "Output: e.g. spark.Profiler_output.csv", required = true)
    protected File outputFile;
	
	@Argument(alias = "n", description = "Only methods in at least n repeats will be included; defaults to the number of files (so keep only the intersection of hot methods)", required = false)
    protected int n = -1;
	
	
	// this will 
	// only methods in at least n repeats will be included
	// count is total
	// rank is based on total of counts
	// tests include all seen over the files (union)
	
	public static void main(String[] args) {
		Logger.info("A tool to merge Gin profiler files.");
		Logger.info("Only methods in at least n repeats will be included");
		Logger.info("n defaults to the number of files (so keep only the intersection of hot methods)");
		Logger.info("In the output, 'count' is sum of counts across all repeat runs; 'rank' is based on the aggregated count");
		Logger.info("Tests for each method are all those seen over the input files (union)");
		
		MergeProfilerFiles m = new MergeProfilerFiles(args);
		try {
			m.mergeProfiles();
		} catch (IOException e) {
			Logger.error("IOException processing files.");
			Logger.error(e);
		}
	}
	
	public MergeProfilerFiles(String[] args) {
		Args.parseOrExit(this, args);
	}
	
	
	public void mergeProfiles() throws IOException {

		// figure out the filenames
		List<File> inputs = new ArrayList<>();
		
		for (String pattern : inputFiles.split(",")) {
			File[] files = new File(dir).listFiles((FileFilter)new WildcardFileFilter(pattern));
			inputs.addAll(Arrays.asList(files));
		}
		Collections.sort(inputs);
		
		if (n < 0) {
			n = inputs.size();
		} else {
			n = Math.min(n, inputs.size());
		}
		
		Logger.info("Found " + inputs.size() + " profiler files. Keeping hot methods appearing in at least " + n + " files.");
		
		Map<String, ProfiledMethod> methods = new HashMap<String, ProfiledMethod>();
		String project = "";
		
		for (File input : inputs) {
			CSVReader inCSV = new CSVReader(new FileReader(input));
				
			inCSV.skip(1);
			for (String[] s : inCSV) {
				// cols are: Project,Rank,Method,Count,Tests
				project = s[0];
				ProfiledMethod pm = methods.get(s[2]);
				if (pm == null) {
					pm = new ProfiledMethod(s[2]);
					methods.put(s[2], pm);
				}
				pm.counts.add(Integer.parseInt(s[3]));
				String[] tests = s[4].split(",");
				pm.tests.addAll(Arrays.asList(tests));
			}
		}
			
		List<ProfiledMethod> sortedMethods = new ArrayList<>();
		for (ProfiledMethod pm : methods.values()) {
			if (pm.counts.size() == n) {
				sortedMethods.add(pm);
			}
		}
		
		Collections.sort(sortedMethods, new Comparator<ProfiledMethod>() {
			@Override
			public int compare(ProfiledMethod arg0, ProfiledMethod arg1) {
				return Integer.compare(arg1.getTotalCount(), arg0.getTotalCount());
			}
		});
		
		ICSVWriter writer = new CSVWriterBuilder(new FileWriter(outputFile)).build();
        writer.writeNext(new String[] {"Project","Rank","Method","Count","Tests"});
        
        int i = 1;
		for (ProfiledMethod pm : sortedMethods) {
			writer.writeNext(new String[] {
					project, 
					Integer.toString(i++),
					pm.methodSignature,
					Integer.toString(pm.getTotalCount()),
					pm.getCSVTests()}
			);
		}
		writer.close();
        
		
		Logger.info("All done.");
	}

	
	private static class ProfiledMethod {
		List<Integer> counts = new ArrayList<>();
		Set<String> tests = new TreeSet<>();
		String methodSignature;
		public ProfiledMethod(String methodSignature) {
			this.methodSignature = methodSignature;
		}
		
		public int getTotalCount() {
			int sum = 0;
			for (Integer i : counts) {
				sum += i;
			}
			return sum;
		}
		
		public String getCSVTests() {
			String rval = "";
			for (String s : tests) {
				if (!rval.isEmpty()) {
					rval += ",";
				}
				rval += s;
			}
			return rval;
		}
	}
}
