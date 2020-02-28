package gin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.pmw.tinylog.Logger;

import gin.edit.Edit;
import gin.edit.Edit.EditType;
import gin.edit.NoEdit;
import gin.edit.line.CopyLine;
import gin.edit.line.DeleteLine;
import gin.edit.line.LineEdit;
import gin.edit.line.ReplaceLine;
import gin.edit.line.SwapLine;
import gin.edit.matched.MatchedCopyStatement;
import gin.edit.matched.MatchedDeleteStatement;
import gin.edit.matched.MatchedReplaceStatement;
import gin.edit.matched.MatchedSwapStatement;
import gin.edit.modifynode.BinaryOperatorReplacement;
import gin.edit.modifynode.NoApplicableNodesException;
import gin.edit.modifynode.UnaryOperatorReplacement;
import gin.edit.statement.CopyStatement;
import gin.edit.statement.DeleteStatement;
import gin.edit.statement.ReplaceStatement;
import gin.edit.statement.StatementEdit;
import gin.edit.statement.SwapStatement;

/**
 * Represents a patch, a potential set of changes to a sourcefile.
 */
public class Patch {

    protected LinkedList<Edit> edits = new LinkedList<>();
    protected SourceFile sourceFile;
    private Class<?> superClassOfEdits;
    
    // we need both of the following:
    
    /**
     * if, overall the patch was valid; so all edits were applied successfully, and
     * JP was able to parse tree as a string.
     * false if JP couldn't parse as string (we don't know which edit caused the problem),
     * or if any individual edit failed
     */
    boolean lastApplyWasValid;
    
    /**
     * identifies individual edits that were applied successfully (true) or failed due to JP (false)
     */
    List<Boolean> editsValidOnLastApply; 

    public Patch(SourceFile sourceFile) {
        this.sourceFile = sourceFile;
        this.superClassOfEdits = null;
        this.lastApplyWasValid = false;
        this.editsValidOnLastApply = Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public Patch clone() {
        Patch clonePatch = new Patch(this.sourceFile);
        clonePatch.edits = (LinkedList<Edit>)(this.edits.clone());
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
     *         (i.e. line/statement) to those already in the patch
     */
    public void add(Edit edit) {
        if ( (edit.getClass().toString()).equals(NoEdit.class.toString()) ) {
            // do not add an empty Edit
        } else {
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
     * 
     * 
     * @return text of patched sourcecode; if there were problems, we just get the same sourcefile back
     */
    public String apply() {

        SourceFile patchedSourceFile = sourceFile.copyOf();
        lastApplyWasValid = true;
        editsValidOnLastApply = new ArrayList<>();
        
        for (Edit edit: edits) {
            try {
                SourceFile patchedByThisEdit = edit.apply(patchedSourceFile);
                if (patchedByThisEdit == null) {
                    lastApplyWasValid = false;
                    editsValidOnLastApply.add(false);
                } else {
                    patchedSourceFile = patchedByThisEdit; // only if the edit actually worked do we update the source
                    editsValidOnLastApply.add(true);
                }
            } catch(Exception e) {
                lastApplyWasValid = false;
                editsValidOnLastApply.add(false);
                // any unexpected problem applying the edit means 
                // we just don't apply it
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

    public void addRandomEdit(Random rng, EditType allowableEditType) {
        addRandomEdit(rng, new LinkedList<EditType>(Arrays.asList(allowableEditType)));
    }

    public void addRandomEdit(Random rng, List<EditType> allowableEditTypes) {
        this.add(randomEdit(rng, allowableEditTypes));
    }

    private Edit randomEdit(Random rng, List<EditType> allowableEditTypes) {
        // generate a random edit. target methods are accounted for here
        // by pulling the appropriate line/statement IDs from sourceFile
        
        if (allowableEditTypes.isEmpty()) {
            Logger.error("No edit types were specified.");
            System.exit(-1);
        }
        
        Edit edit = null;

        // decide what edit we're doing to make
        // first, choose an overall type (line,statement,substatement)
        // then choose a particular kind of edit within that type (copy/delete/move etc.)
        EditType editType = allowableEditTypes.get(rng.nextInt(allowableEditTypes.size())); 
        
        try {
            switch (editType) {
                case LINE:
                    int editSubType = rng.nextInt(4);
                    switch (editSubType) {
                        case (0): // delete line
                            edit = new DeleteLine(sourceFile, rng);
                            break;
                        case (1): // copy line
                            edit = new CopyLine(sourceFile, rng);
                            break;
    /* SB: omitted for experiments
                        case (2): // move line
                            edit = new MoveLine(sourceFile, rng);
                            break;
    */
                        case (2): // replace line
                            edit = new ReplaceLine(sourceFile, rng);
                            break;
                        case (3): // swap line
                            edit = new SwapLine(sourceFile, rng);
                            break;
                    }
                    break;
                case STATEMENT:
                    editSubType = rng.nextInt(4);
                    switch (editSubType) {
                        case (0): // delete statement
                            edit = new DeleteStatement(sourceFile, rng);
                            break;
                        case (1): // copy statement
                            edit = new CopyStatement(sourceFile, rng);
                            break;
    /*
                        case (2): // move statement
                            edit = new MoveStatement(sourceFile, rng);
                            break;
    */
                        case (2): // replace statement
                            edit = new ReplaceStatement(sourceFile, rng);
                            break;
                        case (3): // swap statement
                            edit = new SwapStatement(sourceFile, rng);
                            break;
                    }
                    break;
                case MODIFY_STATEMENT:
                    editSubType = rng.nextInt(2);
                    // will also need a random choice between different modifications...
                    // see https://github.com/gintool/gin/issues/13#issuecomment-342489660
                    switch (editSubType) {
                    case (0): // BinaryOperator
                        edit = new BinaryOperatorReplacement(sourceFile, rng);
                        break;
                    case (1): // UnaryOperator
                        edit = new UnaryOperatorReplacement(sourceFile, rng);
                        break;
    /*
                    case (2): // ReorderLogicalExpression
                        edit = new ReorderLogicalExpression(sourceFile, rng); // not yet ready
                        break;
    */
                    }
                    
                    break;
                case MATCHED_STATEMENT:
                    editSubType = rng.nextInt(4);
                    switch (editSubType) {
                        case (0): // delete statement
                            edit = new MatchedDeleteStatement(sourceFile, rng);
                            break;
                        case (1): // copy statement
                            edit = new MatchedCopyStatement(sourceFile, rng);
                            break;
    /*
                        case (2): // move statement
                            edit = new MatchedMoveStatement(sourceFile, rng); // doesn't exist currently! maybe can't exist.
                            break;
    */
                        case (2): // replace statement
                            edit = new MatchedReplaceStatement(sourceFile, rng);
                            break;
                        case (3): // swap statement
                            edit = new MatchedSwapStatement(sourceFile, rng);
                            break;
                    }
                    break;
            }
        } catch (NoApplicableNodesException e) {
            // we get here if the chosen edit couldn't be created for the given source file
            // leave edit null, it'll be filled below.
        }
        
        if (edit == null) {
            edit = new NoEdit();
        }

        return edit;

    }

    public void writePatchedSourceToFile(String filename) {

        // Apply this patch
        String patchedSourceFile = this.apply();

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
        String description = "| ";
        for (Edit edit: edits) {
            description += edit.toString() + " | ";
        }
        return description.trim();
    }


}

