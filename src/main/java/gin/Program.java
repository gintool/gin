package gin;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Program {

    public String getFilename() {
        return filename;
    }

    private String filename;

    private CompilationUnit compilationUnit;
    private int statementCount;
    private int numberOfBlocks;
    private int[] blockSizes;

    private static final boolean DEBUG = false;

    public Program(String filename) {
        this.filename = filename;
        File programFile = new File(filename);
        try {
            compilationUnit = JavaParser.parse(programFile);
        } catch (IOException e) {
            System.err.println("Exception reading program source: " + e);
            System.exit(-1);
        }
        countStatements();
        countBlocks();
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

    public int getBlockSize(int block) {
        return blockSizes[block];
    }

    private void countStatements() {
        List<Statement> list = compilationUnit.getNodesByType(Statement.class);
        statementCount = list.size();
        if (DEBUG) {
            list.stream().forEach(f -> System.out.println("[" + list.indexOf(f) + "] " + f.toString()));
        }
    }

    private void countBlocks() {
        List<BlockStmt> list = compilationUnit.getNodesByType(BlockStmt.class);
        numberOfBlocks = list.size();
        blockSizes = new int[numberOfBlocks];
        int counter = 0;
        for (BlockStmt b: list) {
            blockSizes[counter] = b.getStatements().size();
            counter++;
        }
        if (DEBUG) {
            list.stream().forEach(f -> System.out.println("[B" + list.indexOf(f) + "] " + f.toString()));
        }
    }
}
