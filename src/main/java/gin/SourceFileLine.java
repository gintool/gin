package gin;

import com.github.javaparser.Position;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.comments.Comment;
import org.pmw.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;

/**
 * A SourceFile designed for supporting line-level edits
 * <p>
 * In practice SourceFile can be viewed as immutable. The only way it can be changed
 * is via the insert/delete line/statement/node or replaceNode methods, which
 * create and return a new SourceFile as part of their signature
 */
public class SourceFileLine extends SourceFile {

    @Serial
    private static final long serialVersionUID = -4661893718115291631L;

    /**
     * the raw lines in the source
     */
    private final SortedMap<LineID, String> lines;

    /**
     * IDs of lines in the target methods
     */
    private List<LineID> lineIDsInTargetMethod;

    /**
     * IDs of empty lines
     */
    private List<LineID> lineIDsEmpty;

    /**
     * IDs of comment lines
     */
    private List<LineID> lineIDsComments;


    public SourceFileLine(String filename, List<String> targetMethodNames) {

        super(filename, targetMethodNames);

        this.lines = readLines(filename);
        populateIDLists();

    }

    public SourceFileLine(File file, String method) {
        this(file.getPath(), Collections.singletonList(method));
    }

    /**
     * create a copy of a source file
     * <p>
     * Note: avoid clon<a href="e.">http://techarticles-wasim.blogspot.com/2011/11/java-clone-method-why-to-avoid-no.h</a>tml
     * <p>
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
        CompilationUnit compilationUnit = StaticJavaParser.parse(this.getSource());

        this.lineIDsInTargetMethod = new ArrayList<>();

        if (this.targetMethods == null || targetMethods.isEmpty()) {
            if (this.lines != null) {
                this.lineIDsInTargetMethod.addAll(this.lines.keySet());
            }
        } else {
            List<Node> targetMethodRootNodes = getTargetMethodRootNodesFromCU(compilationUnit, this.targetMethods);

            // put in a set to start with to avoid duplicates
            SortedSet<LineID> ids = new TreeSet<>();

            for (Node node : targetMethodRootNodes) {
                // javaparser line numbers start at 1, now list also starts at 1 so no need to subtract 1
                int targetMethodStartLine = node.getRange().get().begin.line;
                int targetMethodEndLine = node.getRange().get().end.line;

                if (this.lines != null) {
                    for (int i = 1; i <= lines.size(); i++) {
                        if ((i >= targetMethodStartLine) && (i <= targetMethodEndLine)) {
                            ids.add(new LineID(true, i, 0));
                        }
                    }
                }
            }

            this.lineIDsInTargetMethod.addAll(ids);
        }

        // work out where the empty lines are
        // in theory this is just those of zero length or containing only whitespace
        // so we runTests for lines with anything that's not whitespace
        this.lineIDsEmpty = new ArrayList<>();
        if (this.lines != null) {
            for (Entry<LineID, String> e : lines.entrySet()) {
                if (!e.getValue().matches("^.*\\S+.*$")) {
                    this.lineIDsEmpty.add(e.getKey());
                }
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
        StringBuilder buf = new StringBuilder();
        for (String line : lines.values()) {
            buf.append(line);
            buf.append(System.lineSeparator());
        }
        return buf.toString();
    }

    /*============== the following are line editing methods ==============*/

    /**
     * @param lineNumber to copy
     * @return copy of line from source with given line number, or null if line has been deleted
     */
    public String getLine(int lineNumber) {
        return lines.get(new LineID(true, lineNumber, 0));
    }

    /**
     * return a copy of this sourceFile, with the specified line removed,
     * or this sourceFile if the line couldn't be removed
     *
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
     *
     * @param lineNumber to insert after
     * @param line       to insert
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

    /**
     * @return a list of line numbers corresponding to empty lines (i.e. those of zero length or containing only whitespace)
     */
    public List<Integer> getLineIDsEmpty() {
        List<Integer> lineIDs = new ArrayList<>(lineIDsEmpty.size());
        for (LineID id : lineIDsEmpty) {
            lineIDs.add(id.lineNumber);
        }
        return Collections.unmodifiableList(lineIDs);
    }

    /**
     * @return a list of line numbers corresponding to lines that are purely comments
     */
    public List<Integer> getLineIDsOnlyComments() {
        List<Integer> lineIDs = new ArrayList<>(lineIDsComments.size());
        for (LineID id : lineIDsComments) {
            lineIDs.add(id.lineNumber);
        }
        return Collections.unmodifiableList(lineIDs);
    }

    /**
     * @param inTargetMethod limit line numbers to target method if true, all line numbers otherwise
     * @return a list of line numbers corresponding to lines that aren't empty or purely comments
     * either getAllLineIDs() or getLineIDsInTargetMethod()
     * with the IDs returned by getLineIDsEmpty() and getLineIDsOnlyComments() removed
     */
    public List<Integer> getLineIDsNonEmptyOrComments(boolean inTargetMethod) {
        SortedSet<Integer> allLineIDs = new TreeSet<>(inTargetMethod ? getLineIDsInTargetMethod() : getAllLineIDs());

        getLineIDsEmpty().forEach(allLineIDs::remove);
        getLineIDsOnlyComments().forEach(allLineIDs::remove);
        // Unmodifiable list return
        return List.copyOf(allLineIDs);
    }


    /*============== the following are some helper methods and classes ==============*/

    /**
     * used to keep the list of lines in the correct order
     *
     * @param lineNumber original code line number
     * @param offset     this is used to sort inserted lines following the lineNumber they were inserted after
     */
    private record LineID(boolean isOriginalLine, int lineNumber, int offset) implements Comparable<LineID> {

        @Override
        public int compareTo(LineID that) {
            if (isOriginalLine || (this.lineNumber != that.lineNumber)) {
                return Integer.compare(this.lineNumber, that.lineNumber);
            } else {
                if (that.isOriginalLine) {
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
