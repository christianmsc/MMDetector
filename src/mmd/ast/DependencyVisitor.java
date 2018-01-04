package mmd.ast;

import java.util.ArrayList;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class DependencyVisitor extends ASTVisitor {

	private CompilationUnit fullClass;
	private ArrayList<IMethod> arrayMethod;

	public DependencyVisitor(ICompilationUnit unit) throws JavaModelException {

		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit);
		parser.setCompilerOptions(JavaCore.getOptions());
		parser.setProject(unit.getJavaProject());
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);

		this.fullClass = (CompilationUnit) parser.createAST(null);// parse
		this.fullClass.accept(this);
	}

	@Override
	public boolean visit(MethodDeclaration node) {

		IMethod imeth = (IMethod) node.resolveBinding().getJavaElement();

		if (arrayMethod == null) {
			arrayMethod = new ArrayList<IMethod>();
		}
		arrayMethod.add(imeth);
		return true;
	}

	public ArrayList<IMethod> getArrayMethod() {
		return arrayMethod;
	}

}
