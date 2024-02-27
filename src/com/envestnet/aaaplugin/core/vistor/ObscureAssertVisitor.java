package com.envestnet.aaaplugin.core.vistor;

import org.eclipse.jdt.core.dom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;
import java.io.IOException;

public class ObscureAssertVisitor extends ASTVisitor {
    private boolean obscureAssertFound = false;
    private int currentComplexity = 0;
    private int maxComplexityInCurrentBlock = 0;

    private Stack<List<Integer>> complexityStack = new Stack<>();
    private List<Integer> recordedLineNumbers = new ArrayList<>();

    private static final Logger LOGGER = Logger.getLogger(ObscureAssertVisitor.class.getName());

    static {
        try {
            FileHandler fileHandler = new FileHandler("ObscureAssertVisitor.log", true); // Append mode is true.
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(Level.INFO);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "File logger not working.", e);
        }
    }

    @Override
    public void preVisit(ASTNode node) {
        if (isControlFlowStatement(node) || node instanceof SwitchCase) { // Handle SwitchCase as a separate control flow
            System.out.println("enter control flow statement: " + node.toString());
            currentComplexity++; // Increment complexity for control flow structures
            maxComplexityInCurrentBlock = Math.max(maxComplexityInCurrentBlock, currentComplexity);
            complexityStack.push(new ArrayList<>());
            recordLineNumber(node);
        }
    }

    @Override
    public void postVisit(ASTNode node) {
        if (isControlFlowStatement(node) || node instanceof SwitchCase) { // Handle SwitchCase in postVisit as well
            currentComplexity--;
            List<Integer> complexityLines = complexityStack.pop();
            if (currentComplexity < 2 && !obscureAssertFound) {
                recordedLineNumbers.removeAll(complexityLines);
            } else {
                for (int lineNumber : complexityLines) {
                    insertInSortedOrder(recordedLineNumbers, lineNumber);
                }
            }
            if (currentComplexity == 0) {
                maxComplexityInCurrentBlock = 0;
            }
        }
    }

    private void insertInSortedOrder(List<Integer> list, int lineNumber) {
        int position = 0;
        while (position < list.size() && list.get(position) < lineNumber) {
            position++;
        }
        list.add(position, lineNumber);
    }

    @Override
    public boolean visit(MethodInvocation node) {
        if (maxComplexityInCurrentBlock >= 2 && isJUnitAssertion(node)) {
            obscureAssertFound = true;
        }
        return super.visit(node);
    }

    private boolean isControlFlowStatement(ASTNode node) {
        return node instanceof IfStatement || node instanceof ForStatement ||
                node instanceof WhileStatement || node instanceof SwitchStatement; // Already considers SwitchStatement
    }

    private boolean isJUnitAssertion(MethodInvocation node) {
        IMethodBinding binding = node.resolveMethodBinding();
        if (binding == null) return false;
        ITypeBinding declaringClass = binding.getDeclaringClass();
        if (declaringClass != null) {
            String className = declaringClass.getQualifiedName();
            return className.startsWith("org.junit") ||
                    className.startsWith("org.hamcrest") ||
                    className.startsWith("org.testng") ||
                    className.startsWith("org.assertj");
        }
        return false;
    }

    private void recordLineNumber(ASTNode node) {
        int lineNumber = ((CompilationUnit) node.getRoot()).getLineNumber(node.getStartPosition());
        if (!complexityStack.isEmpty()) {
            complexityStack.peek().add(lineNumber);
        } else {
            recordedLineNumbers.add(lineNumber);
        }
    }

    public boolean hasObscureAssert() {
        return obscureAssertFound;
    }

    public List<Integer> getRecordedLineNumbers() {
        return recordedLineNumbers;
    }
}
