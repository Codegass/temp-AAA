package com.envestnet.aaaplugin.core.vistor;

import org.eclipse.jdt.core.dom.*;

import java.beans.Expression;
//import java.beans.Expression;
import java.util.ArrayList;
import java.util.List;

public class SuppressedExceptionVisitor extends ASTVisitor {
    private boolean suppressedException = false;
    private List<Integer> suppressedExceptionLineNumbers = new ArrayList<>();

    @Override
    public boolean visit(TryStatement node) {
        // Check if the try block contains a fail call
        if (!containsFailCall(node.getBody())) {
            List<?> catchClauses = node.catchClauses();
            for (Object obj : catchClauses) {
                CatchClause catchClause = (CatchClause) obj;
                Block catchBlock = catchClause.getBody();
                // Check if the catch block is empty or contains print statement(s)
                if (catchBlock.statements().isEmpty() || containsPrintStatement(catchBlock)) {
                    recordSuppressedException(node, catchClause);
                }
            }
        }
        return super.visit(node);
    }

    private void recordSuppressedException(TryStatement tryStatement, CatchClause catchClause) {
        suppressedException = true;
        CompilationUnit cu = (CompilationUnit) tryStatement.getRoot();
        int tryLineNumber = cu.getLineNumber(tryStatement.getStartPosition());
        int catchLineNumber = cu.getLineNumber(catchClause.getStartPosition());
        // You might want to store these in a more structured way depending on your needs
        suppressedExceptionLineNumbers.add(tryLineNumber);
        suppressedExceptionLineNumbers.add(catchLineNumber);
    }
    
    private boolean containsPrintStatement(Block catchBlock) {
        for (Object obj : catchBlock.statements()) {
            Statement statement = (Statement) obj;
            if (statement instanceof ExpressionStatement) {
                Expression expression = ((ExpressionStatement) statement).getExpression();
                if (expression instanceof MethodInvocation) {
                    MethodInvocation methodInvocation = (MethodInvocation) expression;
                    IMethodBinding binding = methodInvocation.resolveMethodBinding();
                    if (binding != null && isPrintMethod(binding)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isPrintMethod(IMethodBinding methodBinding) {
        // Check if the method is a print statement (like System.out.print or System.out.println)
        ITypeBinding declaringClass = methodBinding.getDeclaringClass();
        if (declaringClass != null) {
            String className = declaringClass.getQualifiedName();
            String methodName = methodBinding.getName();
            return (className.equals("java.io.PrintStream") && (methodName.equals("print") || methodName.equals("println")));
        }
        return false;
    }

    private boolean isFailMethod(IMethodBinding methodBinding) {
        ITypeBinding declaringClass = methodBinding.getDeclaringClass();
        if (declaringClass != null) {
            String className = declaringClass.getQualifiedName();
            // Check for JUnit 4 and 5 fail() method
            return (className.equals("org.junit.Assert") || className.contains("junit.framework.TestCase") || className.equals("org.junit.jupiter.api.Assertions")) && methodBinding.getName().equals("fail");
        }
        return false;
    }

    private boolean containsFailCall(Block block) {
        for (Object obj : block.statements()) {
            Statement statement = (Statement) obj;
            if (statement instanceof ExpressionStatement) {
                Expression expression = ((ExpressionStatement) statement).getExpression();
                if (expression instanceof MethodInvocation) {
                    MethodInvocation methodInvocation = (MethodInvocation) expression;
                    IMethodBinding binding = methodInvocation.resolveMethodBinding();
                    if (binding != null && isFailMethod(binding)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public List<Integer> getSuppressedExceptionLineNumbers() {
        return suppressedExceptionLineNumbers;
    }

    public boolean hasSuppressedException() {
        return suppressedException;
    }
}
