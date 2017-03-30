package gin;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Program {

    private String filename;
    private CompilationUnit compilationUnit;
    private int statementCount;
    private int numberOfBlocks;
    private int[] blockSizes;

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
        return this.statementCount;
    }

    public int getBlockCount() { return this.numberOfBlocks; }

    public int getBlockSize(int block) { return this.blockSizes[block]; }

    private void countStatements() {
        List<Statement> list = compilationUnit.getNodesByType(Statement.class);
        statementCount = list.size();
        list.stream().forEach(f -> System.out.println("[" + list.indexOf(f) + "] " + f.toString()));
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
        list.stream().forEach(f -> System.out.println("[B" + list.indexOf(f) + "] " + f.toString()));
    }
}
