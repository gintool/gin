package gin;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class SourceFile {

    private CompilationUnit compilationUnit;
    private int statementCount;
    private int numberOfBlocks;
    private int[] numberOfInsertionPointsInBlock;
    private String filename;

    public SourceFile(String filename) {
        this.filename = filename;
        try {
            this.compilationUnit = JavaParser.parse(new File(filename));
        } catch (IOException e) {
            System.err.println("Exception reading program source: " + e);
            System.exit(-1);
        }
        countStatements();
        countBlocks();
    }

    public SourceFile(CompilationUnit compilationUnit) {
        this.compilationUnit = compilationUnit;
        countStatements();
        countBlocks();
    }

    public String getSource() {
        return this.compilationUnit.toString();
    }

    public String getFilename() {
        return this.filename;
    }

    public CompilationUnit getCompilationUnit() {
        return compilationUnit;
    }

    public int getStatementCount() {
        return statementCount;
    }

    public int getNumberOfBlocks() {
        return numberOfBlocks;
    }

    public int getNumberOfInsertionPointsInBlock(int block) {
        return numberOfInsertionPointsInBlock[block];
    }

    private void countStatements() {
        List<Statement> list = compilationUnit.getChildNodesByType(Statement.class);
        statementCount = list.size();
    }

    private void countBlocks() {
        List<BlockStmt> list = compilationUnit.getChildNodesByType(BlockStmt.class);
        numberOfBlocks = list.size();
        numberOfInsertionPointsInBlock = new int[numberOfBlocks];
        int counter = 0;
        for (BlockStmt b: list) {
            numberOfInsertionPointsInBlock[counter] = b.getStatements().size();
            counter++;
        }
    }

    public String statementList() {
        List<Statement> list = compilationUnit.getChildNodesByType(Statement.class);
        statementCount = list.size();
        int counter = 0;
        String output = "";
        for (Statement statement: list) {
            output +=  "[" + counter + "] " + statement.toString() + "\n"; // can't use indexof as may appear > once
            counter++;
        }
        return output;
    }

    public String blockList() {
        List<BlockStmt> list = compilationUnit.getChildNodesByType(BlockStmt.class);
        numberOfBlocks = list.size();
        int counter = 0;
        String output = "";
        for (BlockStmt block: list) {
            output +=  "[" + counter + "] " + block.toString() + "\n"; // can't use indexof as may appear > once
            counter++;
        }
        return output;
    }

    public String toString() {
        return this.getSource();
    }

}
