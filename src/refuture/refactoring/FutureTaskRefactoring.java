package refuture.refactoring;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.TextEdit;

/**
 * 此类是重构的动作类 重构的预览也是通过此类完成
 */
public class FutureTaskRefactoring extends Refactoring {
	// 所有的重构变化
	List<Change> fChangeManager = new ArrayList<Change>();
	// 所有需要修改的JavaElement
	List<IJavaElement> compilationUnits = new ArrayList<IJavaElement>();
	// @Test ' s parameter
	boolean needTimeout = false;
	String timeoutValue = "500";

	/**
	 * 重构的构造方法
	 * 
	 * @param element
	 */
	public FutureTaskRefactoring(IJavaElement element) {
		// while (element.getElementType() > IJavaElement.COMPILATION_UNIT) {
		// element = element.getParent();
		// if (element == null)
		// return;
		// }
		// if (element.getElementType() == IJavaElement.COMPILATION_UNIT) {
		// if (!element.isReadOnly())
		// compilationUnits.add(element);
		// }
		// if (element.getElementType() < IJavaElement.COMPILATION_UNIT)
		// findAllCompilationUnits(element.getJavaProject());

		findAllCompilationUnits(element.getJavaProject());
	}

	/**
	 * 重构的后置条件，用于检查用户输入参数后是否满足某个条件
	 */
	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		collectChanges();
		if (fChangeManager.size() == 0)
			return RefactoringStatus.createFatalErrorStatus("No testing methods found!");
		else
			return RefactoringStatus.createInfoStatus("Final condition has been checked");
	}

	/**
	 * 重构初始条件，用于检查重构开始前应满足的条件
	 */
	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		return RefactoringStatus.createInfoStatus("Initial Condition is OK!");
	}

	/**
	 * 重构的代码变化 如果代码变化多于一处，则通过CompositeChange来完成
	 */
	@Override
	public Change createChange(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		Change[] changes = new Change[fChangeManager.size()];
		System.arraycopy(fChangeManager.toArray(), 0, changes, 0, fChangeManager.size());
		CompositeChange change = new CompositeChange("Add @Test annotation", changes);
		return change;
	}

	@Override
	public String getName() {
		return "annotation_xzy";
	}

	/**
	 * iterate the project to find in all IPackageFragment
	 * 
	 * @param project
	 */
	private void findAllCompilationUnits(IJavaProject project) {
		try {
			for (IJavaElement element : project.getChildren()) { // IPackageFragmentRoot
//				if (element.getElementName().equals("src")) { 
					IPackageFragmentRoot root = (IPackageFragmentRoot) element;
					for (IJavaElement ele : root.getChildren()) {
						if (ele instanceof IPackageFragment) {
							IPackageFragment fragment = (IPackageFragment) ele;
							for (ICompilationUnit unit : fragment.getCompilationUnits()) {
								compilationUnits.add(unit);
							}
						}
					}
//				}
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

	/**
	 * create all the changes
	 * 
	 * @throws JavaModelException
	 */
	private void collectChanges() throws JavaModelException {

		for (IJavaElement element : compilationUnits) {

			// create a document
			ICompilationUnit cu = (ICompilationUnit) element;
			String source = cu.getSource();
			Document document = new Document(source);

			// creation of DOM/AST from a ICompilationUnit
			ASTParser parser = ASTParser.newParser(AST.JLS8);
			parser.setSource(cu);
			CompilationUnit astRoot = (CompilationUnit) parser.createAST(null);

			// start record of the modifications
			astRoot.recordModifications();

			// find all methods that started with "test"
			List<MethodDeclaration> methods = new ArrayList<MethodDeclaration>();
			getMethods(astRoot.getRoot(), methods);
			for (MethodDeclaration md : methods) {
				collectChanges(astRoot, md);
			}

			TextEdit edits = astRoot.rewrite(document, cu.getJavaProject().getOptions(true));
			TextFileChange change = new TextFileChange("", (IFile) cu.getResource());
			change.setEdit(edits);
			fChangeManager.add(change);
		}
	}

	/**
	 * get all the methods in the compilationUnit
	 * 
	 * @param cuu
	 * @param methods
	 */
	private void getMethods(ASTNode cuu, final List methods) {
		cuu.accept(new ASTVisitor() {
			public boolean visit(MethodDeclaration node) {
				methods.add(node);
				return false;
			}
		});
	}

	/**
	 * create the change of method in a compilation
	 * 
	 * @param root
	 * @param method
	 * @return
	 */
	private boolean collectChanges(CompilationUnit root, MethodDeclaration method) {
		if (method.getName().getFullyQualifiedName().startsWith("test")) {
			AST ast = method.getAST();
			if (needTimeout) {
				NormalAnnotation na = ast.newNormalAnnotation();
				na.setTypeName(ast.newSimpleName("Test"));
				MemberValuePair pair = ast.newMemberValuePair();
				pair.setName(ast.newSimpleName("timeout"));
				pair.setValue(ast.newNumberLiteral(timeoutValue));
				na.values().add(pair);
				method.modifiers().add(0, na);
			} else {
				MarkerAnnotation na = ast.newMarkerAnnotation();
				na.setTypeName(ast.newSimpleName("Test"));
				method.modifiers().add(0, na);
			}
			return true;
		}
		return false;
	}

	public void setNeedTimeout(boolean n) {
		needTimeout = n;
	}

	public void setTimeout(String value) {
		timeoutValue = value;
	}
}
