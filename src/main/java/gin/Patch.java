package gin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
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
import gin.edit.modifynode.ReorderLogicalExpression;
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

    public Patch(SourceFile sourceFile) {
        this.sourceFile = sourceFile;
        this.superClassOfEdits = null;
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
     * @return text of patched sourcecode; if there were problems, we just get the same sourcefile back
     */
    public String apply() {

        SourceFile patchedSourceFile = sourceFile.copyOf();
        
        for (Edit edit: edits) {
            try {
                patchedSourceFile = edit.apply(patchedSourceFile);
            } catch(Exception e) {
                // any problem applying the edit means 
                // we just don't apply it
            }
        }

        try {
            return patchedSourceFile.getSource();
        } catch (ClassCastException e) {
            // sometimes happens if an edit has violated JavaParser's expectations
            // - see https://github.com/drdrwhite/ginfork/issues/104
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

    @Override
    public String toString() {
        String description = "| ";
        for (Edit edit: edits) {
            description += edit.toString() + " | ";
        }
        return description.trim();
    }


}

