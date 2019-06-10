package gin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.pmw.tinylog.Logger;

import com.github.javaparser.JavaParser;
import com.github.javaparser.Position;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.comments.Comment;

/**
 * A SourceFile designed for supporting line-level edits
 * 
 * In practice SourceFile can be viewed as immutable. The only way it can be changed
 * is via the insert/delete line/statement/node or replaceNode methods, which
 * create and return a new SourceFile as part of their signature
 */
public class SourceFileLine extends SourceFile {

    /**the raw lines in the source*/
    private SortedMap<LineID, String> lines;

    /** IDs of lines in the target methods */
    private List<LineID> lineIDsInTargetMethod;
    
    /** IDs of empty lines */
    private List<LineID> lineIDsEmpty;
    
    /** IDs of comment lines */
    private List<LineID> lineIDsComments;

    
    public SourceFileLine(String filename, List<String> targetMethodNames) {

        super(filename, targetMethodNames);
        
        this.lines = readLines(filename);
        populateIDLists();

    }

    public SourceFileLine(File file, String method) {
        this(file.getPath(), Arrays.asList(method));
    }

    /**
     * create a copy of a source file
     * 
     * Note: avoid clone. http://techarticles-wasim.blogspot.com/2011/11/java-clone-method-why-to-avoid-no.html
     *
     * this is only called by methods within this class that are going to make a change
     * 
     * @param sf the sourceFile to copy
     */
    private SourceFileLine(SourceFileLine sf) {
        
        super(sf.filename, sf.targetMethods);
        
        // copy the lines
        this.lines = new TreeMap<>(sf.lines);
        this.lineIDsInTargetMethod = new ArrayList<>(sf.lineIDsInTargetMethod); // instead, copy the lines
        this.lineIDsEmpty = new ArrayList<>(sf.lineIDsEmpty);
        this.lineIDsComments = new ArrayList<>(sf.lineIDsComments);
        
    }
    

    @Override
    public SourceFile copyOf() {
        return new SourceFileLine(this);
    }
    
    /*============== the following are setup methods - reading files, building ID lists etc ==============*/
    
    private SortedMap<LineID, String> readLines(String filename) {
        SortedMap<LineID, String> lines = new TreeMap<>();
        try {
            // readAllLines doesn't guarantee a modifiable list, so we copy to ArrayList
            List<String> rawLines = Files.readAllLines(Paths.get(filename), Charset.defaultCharset());
           
            for (int i = 0; i < rawLines.size(); i++) {
                lines.put(new LineID(true, i + 1, 0), rawLines.get(i));
            }
        } catch (IOException e) {
            Logger.error("Exception reading program source: " + e);
            System.exit(-1);
        }
        
        return lines;
    }

    /**
     * This updates the lists of IDs for things like the locations
     * of the target methods and blank lines / comments
     */
    private void populateIDLists() {

        // A JavaParser CU is used to find the lines for the methods
        CompilationUnit compilationUnit = JavaParser.parse(this.getSource().toString());
        
        this.lineIDsInTargetMethod = new ArrayList<>();

        if (this.targetMethods == null || targetMethods.size() == 0) {
            if (this.lines != null) {
                this.lineIDsInTargetMethod.addAll(this.lines.keySet());
            }
        } else {
            List<Node> targetMethodRootNodes = getTargetMethodRootNodesFromCU(compilationUnit, this.targetMethods);

            for (Node node : targetMethodRootNodes) {
                // javaparser line numbers start at 1, now list also starts at 1 so no need to subtract 1
                int targetMethodStartLine = node.getRange().get().begin.line;
                int targetMethodEndLine = node.getRange().get().end.line;

                if (this.lines != null) {
                    for (int i = 1; i <= lines.size(); i++) {
                        if ((i >= targetMethodStartLine) && (i <= targetMethodEndLine)) {
                            this.lineIDsInTargetMethod.add(new LineID(true, i, 0));
                        }
                    }
                }
            }
        }
        
        // work out where the empty lines are
        // in theory this is just those of zero length or containing only whitespace
        // so we runTests for lines with anything that's not whitespace
        this.lineIDsEmpty = new ArrayList<>();
        for (Entry<LineID, String> e : lines.entrySet()) {
            if (!e.getValue().matches("^.*\\S+.*$")) {
                this.lineIDsEmpty.add(e.getKey());
            }
        }
        
        // where are the lines that only contain comments?
        // first we look for all comments and tag their lines
        // then we look for everything else and remove those lines
        // where something else either starts or finishes on a comment line
        // (this should catch anything where there's an inline comment)
        // some info on comments here https://github.com/javaparser/javaparser/wiki/Comments-(Features)
        SortedSet<Integer> possibleCommentLines = new TreeSet<>();
        List<Comment> allComments = compilationUnit.getAllContainedComments();
        for (Comment comment : allComments) {
            for (int i = comment.getBegin().get().line; i <= comment.getEnd().get().line; i++) {
                possibleCommentLines.add(i);
            }
        }
        for (Node n : compilationUnit.getChildNodesByType(Node.class)) {
            if (!(n instanceof Comment)) {
                // if this node begins or end on any line
                // that also has a comment, we want to keep those
                // lines, so remove them from possibleCommentLines
                
                // it's rare that this won't have a range but some things don't (e.g. in lambda expressions)
                // we just ignore those
                Optional<Position> beginOpt = n.getBegin();
                if (beginOpt.isPresent()) {
                    int begin = beginOpt.get().line;
                    possibleCommentLines.remove(begin);
                }
                
                Optional<Position> endOpt = n.getEnd();
                if (endOpt.isPresent()) {
                    int end = n.getEnd().get().line;
                    possibleCommentLines.remove(end);
                }
            }
        }
        
        this.lineIDsComments = new ArrayList<>();
        for (Integer i : possibleCommentLines) {
            this.lineIDsComments.add(new LineID(true, i, 0));
        }
    }
    
    /*============== the following are getter methods ==============*/
    
    @Override
    public String getSource() {
        StringBuffer buf = new StringBuffer();
        for (String line : lines.values()) {
            buf.append(line);
            buf.append(System.lineSeparator());
        }
        return buf.toString();
    }

    /*============== the following are line editing methods ==============*/
    
    /**
     * @return copy of line from source with given line number, or null if line has been deleted
     * @param lineNumber to copy
     * */
    public String getLine(int lineNumber) {
        return lines.get(new LineID(true, lineNumber, 0));
    }

    /**
     * return a copy of this sourceFile, with the specified line removed,
     * or this sourceFile if the line couldn't be removed
     * @param lineNumber to remove
     * @return a modified copy of this {@link SourceFileLine}
     */
    public SourceFileLine removeLine(int lineNumber) {
        LineID toDelete = new LineID(true, lineNumber, 0);
        
        // line already deleted? don't bother proceeding.
        if (!lines.containsKey(toDelete)) {
            return this;
        } else {
            // make a copy of this sourceFile
            SourceFileLine sf = new SourceFileLine(this);
            sf.lines.remove(toDelete);
            return sf;
        }
    }

    /**
     * return a copy of this sourceFile, with the specified line inserted,
     * or this sourceFile if the line couldn't be inserted for some reason
     * @param lineNumber to insert after
     * @param line to insert
     * @return a modified copy of this {@link SourceFileLine}
     */
    public SourceFileLine insertLine(int lineNumber, String line) {
        if (lineNumber >= 1) {
            SourceFileLine sf = new SourceFileLine(this);
            sf.lines.put(getNextInsertPoint(lineNumber), line);
            return sf;
        } else {
            return this;
        }
    }

    
    /*============== the following are methods to get IDs and counts to assist in making edits ==============*/
    
    public List<Integer> getAllLineIDs() {
        List<Integer> lineIDs = new ArrayList<>(lines.size());
        for (LineID id : lines.keySet()) {
            lineIDs.add(id.lineNumber);
        }
        return Collections.unmodifiableList(lineIDs);
    }
    
    /**
     * make a new LineID to follow the specified original line, and any other
     * lines inserted after it, but before the next original line
     */
    private LineID getNextInsertPoint(int originalLineNumber) {
        // find the lines after the specified line, but before a hypothetical
        // (as we might be at end anyway) next line
        LineID currentLine = new LineID(true, originalLineNumber, 0);
        LineID nextLine = new LineID(true, originalLineNumber + 1, 0);
        
        Set<LineID> insertedLines = this.lines.tailMap(currentLine).headMap(nextLine).keySet();
        int nextOffset = 0;
        for (LineID l : insertedLines) {
            nextOffset = Math.max(nextOffset, l.offset + 1);
        }
        
        return new LineID(false, originalLineNumber, nextOffset);
    }

    public List<Integer> getLineIDsInTargetMethod() {
        List<Integer> lineIDs = new ArrayList<>(lineIDsInTargetMethod.size());
        for (LineID id : lineIDsInTargetMethod) {
            lineIDs.add(id.lineNumber);
        }
        return Collections.unmodifiableList(lineIDs);
    }
    
    /**@return a list of line numbers corresponding to empty lines (i.e. those of zero length or containing only whitespace)*/
    public List<Integer> getLineIDsEmpty() {
        List<Integer> lineIDs = new ArrayList<>(lineIDsEmpty.size());
        for (LineID id : lineIDsEmpty) {
            lineIDs.add(id.lineNumber);
        }
        return Collections.unmodifiableList(lineIDs);
    }
    
    /**@return a list of line numbers corresponding to lines that are purely comments*/
    public List<Integer> getLineIDsOnlyComments() {
        List<Integer> lineIDs = new ArrayList<>(lineIDsComments.size());
        for (LineID id : lineIDsComments) {
            lineIDs.add(id.lineNumber);
        }
        return Collections.unmodifiableList(lineIDs);
    }

    /**
     * @return a list of line numbers corresponding to lines that aren't empty or purely comments
     * either getAllLineIDs() or getLineIDsInTargetMethod()
     * with the IDs returned by getLineIDsEmpty() and getLineIDsOnlyComments() removed
     * @param inTargetMethod limit line numbers to target method if true, all line numbers otherwise
     * */
    public List<Integer> getLineIDsNonEmptyOrComments(boolean inTargetMethod) {
        SortedSet<Integer> allLineIDs = new TreeSet<>();
        allLineIDs.addAll(inTargetMethod ? getLineIDsInTargetMethod() : getAllLineIDs());
        
        allLineIDs.removeAll(getLineIDsEmpty());
        allLineIDs.removeAll(getLineIDsOnlyComments());
        
        return Collections.unmodifiableList(new ArrayList<>(allLineIDs));
    }

 
    /*============== the following are some helper methods and classes ==============*/
     
    /**
     * used to keep the list of lines in the correct order
     */
    private static final class LineID implements Comparable<LineID> {
        private final boolean isOriginalLine;
        private final int lineNumber; // original code line number
        private final int offset; // this is used to sort inserted lines following the lineNumber they were inserted after 
        
        public LineID(boolean isOriginalLine, int lineNumber, int offset) {
            this.isOriginalLine = isOriginalLine;
            this.lineNumber = lineNumber;
            this.offset = offset;
        }
        
        @Override
        public int compareTo(LineID that) {
            if (isOriginalLine || (this.lineNumber != that.lineNumber)) {
                return Integer.compare(this.lineNumber, that.lineNumber);
            } else {
                if (this.isOriginalLine) {
                    return -1;
                } else if (that.isOriginalLine) {
                    return 1;
                } else {
                    return Integer.compare(this.offset, that.offset);
                }
            }
        }
        
        @Override
        public String toString() {
            return (this.isOriginalLine ? "O" : "I") + this.lineNumber + (this.isOriginalLine ? "" : "." + this.offset);
        }
    }
}
