package com.envestnet.aaaplugin.core.ast;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.envestnet.aaaplugin.util.config.PackageSupportConfigReader;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class MethodInvocationAnalyzer {

    private PackageSupportConfigReader configReader;

    public MethodInvocationAnalyzer(String configFilePath) {
        this.configReader = new PackageSupportConfigReader(configFilePath);
    }

    public boolean isMock(MethodInvocation mi) {
        return matchRule(mi, "mock");
    }

    public boolean isAssert(MethodInvocation mi) {
        return matchRule(mi, "asserts");
    }

    public boolean isJunitExpectedException(MethodInvocation mi) {
        return matchRule(mi, "junitExpectedException");
    }

    private boolean matchRule(MethodInvocation mi, String sectionKey) {
        IMethodBinding binding = mi.resolveMethodBinding();
        if (binding == null) return false;  // Early exit if method binding could not be resolved

        String qualifiedClassName = binding.getDeclaringClass().getQualifiedName();
        String methodName = binding.getMethodDeclaration().getName();
        return configReader.matchesRule(qualifiedClassName, methodName, sectionKey);
    }

    // these four methods will not be affected by the yaml file
    public boolean isThirdParty(MethodInvocation mi) {
        if (mi.resolveMethodBinding().getJavaElement().getPath().makeAbsolute().toString().endsWith(".jar")) {
            return true;
        }
        return false;
    }

    public boolean isGetter(MethodInvocation mi) {
        return (mi.getName().toString().contains("get") || mi.getName().toString().contains("is")) && parameterQuantity(mi) == 0;
    }

    public boolean isSetter(MethodInvocation mi) {
        IMethodBinding binding = mi.resolveMethodBinding();
        return mi.getName().toString().contains("set") && parameterQuantity(mi) != 0 && Objects.equals(binding.getReturnType().toString(), "void");
    }

    public int parameterQuantity(MethodInvocation mi) {
        IMethodBinding binding = mi.resolveMethodBinding();
        return binding.getParameterTypes().length;
    }

}
