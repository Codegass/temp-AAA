package com.envestnet.aaaplugin.core.vistor;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;


public class GenericMethodVisitor extends ASTVisitor {
	MethodDeclaration method = null;
	private IMethodBinding binding;
	private ITypeBinding[] itbs;

	public GenericMethodVisitor(IMethodBinding binding) {
		this.binding = binding;
		itbs = binding.getParameterTypes();
		//System.out.println(binding.getName());
		/*
		 * for (ITypeBinding itb : itbs) { System.out.println("binding itb name: " +
		 * itb.getBinaryName()); System.out.println("binding generic?: " +
		 * itb.isGenericType()); }
		 */
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		if (match(node)) method = node;
		return super.visit(node);
	}

	public MethodDeclaration getMethod() {
		return method;
	}

	/*
	 * binding may be a copy of a generic method with substitutions for the method's
	 * type parameters
	 *
	 */
	private boolean match(MethodDeclaration md) {
		String name = md.getName().toString();
		if (!name.equals(binding.getName())) return false;
		List actualParameters = md.parameters();
		ITypeBinding[] itbs2 = new ITypeBinding[actualParameters.size()];
		for (int i = 0; i < itbs2.length; i++) {
			itbs2[i] = ((SingleVariableDeclaration) actualParameters.get(i)).getType().resolveBinding();
		}
		if (!allParametersMatch(itbs, itbs2)) return false;
    	
		return true;
	}

	private boolean allParametersMatch(ITypeBinding[] itbs1, ITypeBinding[] itbs2) {
		if (itbs1.length != itbs2.length) return false;

		for (int i = 0; i < itbs1.length; i++) {
			if (!singleParameterMatch(itbs1[i], itbs2[i])) return false;
		}

		return true;
	}

	private boolean singleParameterMatch(ITypeBinding itb1, ITypeBinding itb2) {
		if (itb2.getModifiers() == 0) return true; // itb2 == ?, T
		// ? T extends org.xxx.xxx not checked
		// To do:

		if (itb1.isEqualTo(itb2)) return true;
		if (!itb1.getBinaryName().equals(itb2.getBinaryName())) {
			return false;
		}

		if (!allParametersMatch(itb1.getTypeArguments(), itb2.getTypeArguments())) return false;

		return true;
	}
}
