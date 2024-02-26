package com.envestnet.aaaplugin.core.vistor;

import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;

public class SuppressedExceptionVisitor extends ASTVisitor {
    private boolean suppressedException = false;
    private List<Integer> suppressedExceptionLineNumbers = new ArrayList<>();

    @Override
    public boolean visit(TryStatement node) {
        List<?> catchClauses = node.catchClauses();
        for (Object obj : catchClauses) {
            CatchClause catchClause = (CatchClause) obj;
            Block catchBlock = catchClause.getBody();

            // Check if the catch block is empty or contains only a print statement
            if (catchBlock.statements().isEmpty() || containsOnlyPrintOrFailStatement(catchBlock)) {
                // Exclude cases with JUnit fail() call
                if (!containsFailCall(catchBlock)) {
                    recordSuppressedException(node);
                }
            }
        }
        return super.visit(node);
    }

    private void recordSuppressedException(TryStatement node) {
        suppressedException = true;
        int lineNumber = ((CompilationUnit) node.getRoot()).getLineNumber(node.getStartPosition());
        suppressedExceptionLineNumbers.add(lineNumber);
    }

    private boolean containsOnlyPrintOrFailStatement(Block catchBlock) {
        if (catchBlock.statements().size() == 1) {
            Statement statement = (Statement) catchBlock.statements().get(0);
            if (statement instanceof ExpressionStatement) {
                Expression expression = ((ExpressionStatement) statement).getExpression();
                if (expression instanceof MethodInvocation) {
                    MethodInvocation methodInvocation = (MethodInvocation) expression;
                    IMethodBinding binding = methodInvocation.resolveMethodBinding();
                    if (binding != null && (isPrintMethod(binding) || isFailMethod(binding))) {
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
            return (className.equals("org.junit.Assert") || className.equals("org.junit.jupiter.api.Assertions")) && methodBinding.getName().equals("fail");
        }
        return false;
    }

    private boolean containsFailCall(Block catchBlock) {
        // This method is similar to containsOnlyPrintOrFailStatement but focuses on the fail() call
        return containsOnlyPrintOrFailStatement(catchBlock); // Reuse logic since it's already checking for fail()
    }

    public List<Integer> getSuppressedExceptionLineNumbers() {
        return suppressedExceptionLineNumbers;
    }

    public boolean hasSuppressedException() {
        return suppressedException;
    }
}
