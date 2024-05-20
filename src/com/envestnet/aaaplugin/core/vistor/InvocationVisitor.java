package com.envestnet.aaaplugin.core.vistor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NormalAnnotation;

import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ExpressionStatement;


public class InvocationVisitor extends ASTVisitor {
	List<ASTNode> methods = new ArrayList<>();
	List<ASTNode> order = new ArrayList<>();

	@Override
	public boolean visit(MethodInvocation node) {
		methods.add(node);

		if (!order.contains(node)) {
			order.addAll(getPotential(node));
		}

		return true;
	}


	private List<ASTNode> getPotential(ASTNode node) {
		List<ASTNode> invocations = new ArrayList<>();
		Expression caller = null;
		List<ASTNode> arguments = null;
		if (node instanceof MethodInvocation) {
			MethodInvocation mi = (MethodInvocation) node;
			caller = mi.getExpression();
			arguments = mi.arguments();
		} else if (node instanceof ClassInstanceCreation) {
			ClassInstanceCreation cic = (ClassInstanceCreation) node;
			caller = cic.getExpression();
			arguments = cic.arguments();
		} else if (node instanceof ExpressionMethodReference) {
			ExpressionMethodReference emr = (ExpressionMethodReference) node;
			caller = emr.getExpression();
		} else if (node instanceof LambdaExpression) { // Lambda Handling
			LambdaExpression le = (LambdaExpression) node;
			ASTNode body = le.getBody();
			if (body instanceof Block) {
				Block block = (Block) le.getBody();
				for (Statement stmt : (List<Statement>) block.statements()) {
					if (stmt instanceof ExpressionStatement) {
						Expression expr = ((ExpressionStatement) stmt).getExpression();
						invocations.addAll(getPotential(expr));
					}
				}
			} else if (body instanceof Expression) {
				Expression exprBody = (Expression) body;
				invocations.addAll(getPotential(exprBody));
			}
		} else if (node instanceof InfixExpression) { // Infix Handling
			InfixExpression ie = (InfixExpression) node;
			Expression leftOperand = ie.getLeftOperand();
			Expression rightOperand = ie.getRightOperand();

			invocations.addAll(getPotential(leftOperand));
			invocations.addAll(getPotential(rightOperand));
		}

		if (caller != null && (
				caller.getNodeType() == ASTNode.METHOD_INVOCATION ||
						caller.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION ||
						caller.getNodeType() == ASTNode.EXPRESSION_METHOD_REFERENCE ||
						caller.getNodeType() == ASTNode.LAMBDA_EXPRESSION ||
						caller.getNodeType() == ASTNode.INFIX_EXPRESSION
		)) {
			invocations.addAll(getPotential(caller));
		}

		if (arguments != null) {
			for (ASTNode arg : arguments) {
				if (arg.getNodeType() == ASTNode.METHOD_INVOCATION ||
						arg.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION ||
						arg.getNodeType() == ASTNode.EXPRESSION_METHOD_REFERENCE
				) {
					invocations.addAll(getPotential(arg));
				}
			}
		}

		invocations.add(node);

		return invocations;
	}

	//
	private void sortMethods() {
		Collections.sort(methods, Comparator.comparing(n -> order.indexOf(n)));
	}


	@Override
	public boolean visit(ExpressionMethodReference node) {
		methods.add(node);

		if (!order.contains(node)) {
			order.addAll(getPotential(node));
		}

		return true;
	}

	@Override
	public boolean visit(ClassInstanceCreation node) {
		methods.add(node);

		if (!order.contains(node)) {
			order.addAll(getPotential(node));
		}

		return true;
	}

	@Override
	public boolean visit(NormalAnnotation node) {
		// if format @Test(expected=..)
		if (node.getTypeName().toString().equals("Test")) {
			List<MemberValuePair> values = node.values();
			for (MemberValuePair v : values) {
				if (v.getName().toString().contains("expected")) {
					methods.add(node);
				}
			}
		}

		return true;
	}

	public List<ASTNode> getMethods() {
		sortMethods();
		return methods;
	}


}
