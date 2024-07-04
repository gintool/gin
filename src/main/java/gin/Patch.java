package gin;

import gin.edit.Edit;
import gin.edit.Edit.EditType;
import gin.edit.NoEdit;
import gin.edit.line.LineEdit;
import gin.edit.statement.StatementEdit;
import gin.edit.llm.LLMMaskedStatement;
import gin.edit.llm.LLMReplaceStatement;
import org.apache.commons.io.FileUtils;
import org.pmw.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Represents a patch, a potential set of changes to a sourcefile.
 */
public class Patch implements Serializable, Cloneable {

    @Serial
    private static final long serialVersionUID = 1645891147232089192L;
    protected LinkedList<Edit> edits = new LinkedList<>();
    protected SourceFile sourceFile;
    /**
     * if, overall the patch was valid; so all edits were applied successfully, and
     * JP was able to parse tree as a string.
     * false if JP couldn't parse as string (we don't know which edit caused the problem),
     * or if any individual edit failed
     */
    boolean lastApplyWasValid;

    // we need both of the following:
    /**
     * identifies individual edits that were applied successfully (true) or failed due to JP (false)
     */
    List<Boolean> editsValidOnLastApply;
    private Class<?> superClassOfEdits;

    public Patch(SourceFile sourceFile) {
        this.sourceFile = sourceFile;
        this.superClassOfEdits = null;
        this.lastApplyWasValid = false;
        this.editsValidOnLastApply = Collections.emptyList();
    }

    @Override
    public Patch clone() {
        Patch clonePatch = new Patch(this.sourceFile);
        clonePatch.edits = new LinkedList<>(this.edits);
        clonePatch.superClassOfEdits = this.superClassOfEdits;
        return clonePatch;
    }

    public int size() {
        return this.edits.size();
    }

    public List<Edit> getEdits() {
        return this.edits;
    }

    /**
     * @return the sourcefile object that this patch was created against
     */
    public SourceFile getSourceFile() {
        return sourceFile;
    }

    /**
     * add an edit to this patch.
     *
     * @param edit - the edit to be added
     * @throws IllegalArgumentException if the edit is of a different type
     *                                  (i.e. line/statement) to those already in the patch
     */
    public void add(Edit edit) {
        // do not add an empty Edit
        if (!edit.getClass().toString().equals(NoEdit.class.toString())) {
            if ((superClassOfEdits == null) || superClassOfEdits.isAssignableFrom(edit.getClass())) {
                this.edits.add(edit);

                if (superClassOfEdits == null) {
                    if (StatementEdit.class.isAssignableFrom(edit.getClass())) {
                        superClassOfEdits = StatementEdit.class;
                    } else {
                        superClassOfEdits = LineEdit.class;
                    }
                }
            } else {
                throw new IllegalArgumentException("Mixed line/statement edits not supported.");
            }
        }
    }

    public void remove(int index) {
        this.edits.remove(index);
    }

    /**
     * Apply this patch to the source file.
     * We loop over the edits, applying each one in turn.
     * If any edit fails, we act as if nothing happened and move on to the next one,
     * so it is possible for a patch to return an unaltered copy of the original source code
     * <p>
     * There are three places where edits might not be valid and get ignored:
     * <ul>
     * <li>1 - if the Edit is well behaved but detects an error, it will return null
     * <li>2 - if the Edit throws an exception during application
     * <li>3 - if the Edit leads to a sourceFile that can't be written out as a string
     * </ul>
     * In all these situations, we set a flag "lastApplyWasInvalid" to true so it
     * can be reported via lastApplyWasInvalid(); and we act as if the edit didn't happen at all
     * (move to next edit for 1 and 2; or return original unaltered source for 3)
     * <p>
     * 
     * @param metadata to use when applying the edits; could use, e.g., an error code or filter on how to apply it
     *
     * @return text of patched sourcecode; if there were problems, we just get the same sourcefile back
     */
    public String apply(Object metadata) {

        SourceFile patchedSourceFile = sourceFile.copyOf();
        lastApplyWasValid = true;
        editsValidOnLastApply = new ArrayList<>();

        for (Edit edit : edits) {
            try {
                SourceFile patchedByThisEdit = edit.apply(patchedSourceFile, metadata);
                if (patchedByThisEdit == null) {
                    lastApplyWasValid = false;
                    editsValidOnLastApply.add(false);
                } else {
                    patchedSourceFile = patchedByThisEdit; // only if the edit actually worked do we update the source
                    editsValidOnLastApply.add(true);
                }
            } catch (Exception e) {
                lastApplyWasValid = false;
                editsValidOnLastApply.add(false);
                // any unexpected problem applying the edit means 
                // we just don't apply it
                
                e.printStackTrace();
            }
        }

        try {
            return patchedSourceFile.getSource();
        } catch (ClassCastException e) {
            // sometimes happens if an edit has violated JavaParser's expectations
            // - see https://github.com/drdrwhite/ginfork/issues/104
            // if we get here, the whole patch is invalid
            this.lastApplyWasValid = false;
            return sourceFile.getSource();
        }

    }
    
    /**apply with no metadata*/
    public String apply() {
    	return this.apply(null);
    }

    /**
     * add a random edit to this patch of a specific class
     *
     * @param rng               - for random number generation
     * @param allowableEditType - list of allowable edit types
     */
    public void addRandomEditOfClass(Random rng, Class<? extends Edit> allowableEditType) {
        addRandomEditOfClasses(rng, Collections.singletonList(allowableEditType));
    }

    /**
     * add a random edit to this patch, one of a specific list of classes
     *
     * @param rng                - for random number generation
     * @param allowableEditTypes - list of allowable edit types
     */
    public void addRandomEditOfClasses(Random rng, List<Class<? extends Edit>> allowableEditTypes) {
        this.add(randomEdit(rng, allowableEditTypes));
    }

    /**
     * add a random edit to this patch of a specific type (family of classes)
     *
     * @param rng               - for random number generation
     * @param allowableEditType - list of allowable edit types
     */
    public void addRandomEdit(Random rng, EditType allowableEditType) {
        addRandomEditOfClasses(rng, Edit.getEditClassesOfType(allowableEditType));
    }

    /**
     * add a random edit to this patch, one of a list of specific types (families of classes)
     *
     * @param rng                - for random number generation
     * @param allowableEditTypes - list of allowable edit types
     */
    public void addRandomEdit(Random rng, List<EditType> allowableEditTypes) {
        addRandomEditOfClasses(rng, Edit.getEditClassesOfTypes(allowableEditTypes));
    }

    private Edit randomEdit(Random rng, List<Class<? extends Edit>> allowableEditTypes) {
        // generate a random edit. target methods are accounted for here
        // by pulling the appropriate line/statement IDs from sourceFile

        if (allowableEditTypes.isEmpty()) {
            Logger.error("No edit types were specified.");
            System.exit(-1);
        }

        Edit edit = null;

        // decide what edit we're doing to make
        Logger.info("Choosing an edit type from: " + allowableEditTypes);
        Class<? extends Edit> editType = allowableEditTypes.get(rng.nextInt(allowableEditTypes.size()));

        // make one
        try {
            edit = editType.getDeclaredConstructor(SourceFile.class, Random.class).newInstance(sourceFile, rng);
        } catch (NoSuchMethodException e) {
            // we get here if the edit author forgot to add a (SourceFile,Random) constructor
            // leave edit null, it'll be filled below.
            // BUT we should issue a warning...
            Logger.warn("(SourceFile,Random) constructor not found for edit class " + editType);
        } catch (Exception e) {
            // e.g. NoApplicableNodesException
            // we get here if the chosen edit couldn't be created for the given source file
            // leave edit null, it'll be filled below.
        }

        if (edit == null) {
            edit = new NoEdit();
        }

        return edit;

    }
    
    public void writePatchedSourceToFile(String filename) {
    	writePatchedSourceToFile(filename, null);
    }

    public void writePatchedSourceToFile(String filename, Object metadata) {

        // Apply this patch
        String patchedSourceFile = this.apply(metadata);

        try {
            FileUtils.writeStringToFile(new File(filename), patchedSourceFile, Charset.defaultCharset());
        } catch (IOException e) {
            Logger.error("Exception writing source code of patched program to: " + filename);
            Logger.trace(e);
            System.exit(-1);
        }

    }

    public boolean isOnlyLineEdits() {
        boolean rval = true;
        for (Edit e : edits) {
            rval &= e.getEditType() == EditType.LINE;
        }

        return rval;
    }

    public boolean isOnlyStatementEdits() {
        boolean rval = true;
        for (Edit e : edits) {
            rval &= e.getEditType() != EditType.LINE;
        }

        return rval;
    }

    public boolean lastApplyWasValid() {
        return lastApplyWasValid;
    }

    public List<Boolean> getEditsInvalidOnLastApply() {
        return editsValidOnLastApply;
    }

    @Override
    public String toString() {
        StringBuilder description = new StringBuilder("| ");
        for (Edit edit : edits) {
            description.append(edit.toString()).append(" | ");
        }
        return description.toString().trim();
    }


}

