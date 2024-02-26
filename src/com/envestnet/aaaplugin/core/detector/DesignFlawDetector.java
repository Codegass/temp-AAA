package com.envestnet.aaaplugin.core.detector;

import org.eclipse.jdt.core.dom.*;
import com.envestnet.aaaplugin.core.vistor.ObscureAssertVisitor;
import com.envestnet.aaaplugin.core.vistor.SuppressedExceptionVisitor;

import java.util.List;

public class DesignFlawDetector {

    private List<Integer> lineNumbers;

    public boolean detectObscureAssert(MethodDeclaration method) {
        ObscureAssertVisitor visitor = new ObscureAssertVisitor();
        method.accept(visitor);
        lineNumbers = visitor.getRecordedLineNumbers();
        return visitor.hasObscureAssert();
    }

    public boolean detectSuppressedException(MethodDeclaration method) {
        SuppressedExceptionVisitor visitor = new SuppressedExceptionVisitor();
        method.accept(visitor);
        lineNumbers = visitor.getSuppressedExceptionLineNumbers();
        return visitor.hasSuppressedException();
    }

    public List<Integer> getLineNumbers() {
        return lineNumbers;
    }
}
