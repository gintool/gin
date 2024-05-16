package gin.util.analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pmw.tinylog.Logger;

import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.CSVReaderHeaderAwareBuilder;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import com.opencsv.RFC4180Parser;
import com.opencsv.RFC4180ParserBuilder;
import com.opencsv.exceptions.CsvValidationException;
import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;


/**
 * RS output is one line per test
 * This aggregates to one line per edit; with a count of test passes, total tests run, and total run time
 */
public class AggregateRandomSamplerOutput {

	@Argument(alias = "f", description = "Input: a file output by RandomSampler", required = true)
    protected File inputFile;
	
	public static void main(String[] args) {
		AggregateRandomSamplerOutput a = new AggregateRandomSamplerOutput(args);
		a.process();
	}
	
	public AggregateRandomSamplerOutput(String[] args) {
		Args.parseOrExit(this, args);
	}
	
	public void process() {	
		// columns are: "PatchIndex","PatchSize","Patch","MethodIndex","TestIndex","UnitTest","RepNumber","PatchValid","PatchCompiled","TestPassed","TestExecutionTime(ns)","TestCPUTime(ns)","TestTimedOut","TestExceptionType","TestExceptionMessage","AssertionExpectedValue","AssertionActualValue","NoOp","EditsValid"
		// columns to keep: "PatchIndex","PatchSize","Patch","MethodIndex","PatchValid","PatchCompiled","NoOp","EditsValid"
		// columns to aggregate: "TestPassed" (count),"TestExecutionTime(ns)" (sum),"TestCPUTime(ns)" (sum),"TestTimedOut" (count)
		// columns to add: "AllTestsPassed" (true/false), "TestCount" (int)
		// columns to drop: "TestIndex","UnitTest","RepNumber","TestExceptionType","TestExceptionMessage","AssertionExpectedValue","AssertionActualValue"
		
		try {
			String outputFilename = inputFile + "_aggregated.csv";
            Logger.info("Reading from: " + inputFile);
            Logger.info("Writing to: " + outputFilename);
			
            RFC4180Parser parser = new RFC4180ParserBuilder().build();
            CSVReaderHeaderAware reader = (CSVReaderHeaderAware)(new CSVReaderHeaderAwareBuilder(new FileReader(inputFile)).withCSVParser(parser).build());
            ICSVWriter writer = new CSVWriterBuilder(new FileWriter(outputFilename)).build();
            writer.writeNext(new String[] {"PatchIndex","PatchSize","Patch","MethodIndex","PatchValid","PatchCompiled","TestPassedCount","AllTestsPassed","TestCount","TestExecutionTimeTotal(ns)","TestCPUTimeTotal(ns)","TestTimedOutCount","NoOp","EditsValid"});
            
            Map<String, String> data = reader.readMap();
            
            Map<String,String> outputRow = new HashMap<>();

            int testPassedCount = 0, testTimedOutCount = 0, testCount = 0;
            long testExecutionTimeTotal = 0, testCPUTimeTotal = 0; 
            String currentPatchIndex = data.get("PatchIndex");
            
            fileLoop:
            while (true) {

            	// write out row and reset values if we're at the end of a block of results for an edit
            	if (data == null || !data.get("PatchIndex").equals(currentPatchIndex)) {
            		writer.writeNext(new String[] {
						outputRow.get("PatchIndex"),
						outputRow.get("PatchSize"),
						outputRow.get("Patch"),
						outputRow.get("MethodIndex"),
						outputRow.get("PatchValid"),
						outputRow.get("PatchCompiled"),
						Integer.toString(testPassedCount),  
						Boolean.toString(testPassedCount == testCount),    // all tests passed
						Integer.toString(testCount), // test count
						Long.toString(testExecutionTimeTotal),
						Long.toString(testCPUTimeTotal),
						Integer.toString(testTimedOutCount),
						outputRow.get("NoOp"),
						outputRow.get("EditsValid")
            		});
            
            		outputRow.clear();
            		testPassedCount = 0;
            		testTimedOutCount = 0;
            		testCount = 0;
                    testExecutionTimeTotal = 0;
                    testCPUTimeTotal = 0; 
                    
                    if (data == null) {
                    	break fileLoop;
                    }
            	}
            
            	currentPatchIndex = data.get("PatchIndex");
            	
                // copy values that we'll be keeping
            	outputRow.put("PatchIndex", data.get("PatchIndex"));
            	outputRow.put("PatchSize", data.get("PatchSize"));
            	outputRow.put("Patch", data.get("Patch"));
            	outputRow.put("MethodIndex", data.get("MethodIndex"));
            	outputRow.put("PatchValid", data.get("PatchValid"));
            	outputRow.put("PatchCompiled", data.get("PatchCompiled"));
            	outputRow.put("NoOp", data.get("NoOp"));
            	outputRow.put("EditsValid", data.get("EditsValid"));

            	// add to aggregate values
            	testPassedCount += data.get("TestPassed").equals("true") ? 1 : 0;
            	testTimedOutCount += data.get("TestTimedOut").equals("true") ? 1 : 0;
            	testCount++;
            	try {testExecutionTimeTotal += Long.parseLong(data.get("TestExecutionTime(ns)")); } catch (NumberFormatException e) {}
            	try {testCPUTimeTotal += Long.parseLong(data.get("TestCPUTime(ns)")); } catch (NumberFormatException e) {}
            	
                data = reader.readMap();
                
            }
            reader.close();
            writer.close();
            
    		Logger.info("All done. Results written to " + outputFilename);
		} catch (CsvValidationException | FileNotFoundException e) {
            Logger.error(e.getMessage());
            Logger.trace(e);
        } catch (IOException e) {
            Logger.error("Error reading method file: " + inputFile);
            Logger.trace(e);
        }
	}

}
