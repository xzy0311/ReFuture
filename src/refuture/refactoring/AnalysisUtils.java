package refuture.refactoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.Change;

// TODO: Auto-generated Javadoc
/**
 * The Class AnalysisUtils.
 * 提供分析方法的工具包类，它的方法都是静态的。
 */
public class AnalysisUtils {

	/** The Constant IMPORT_ExecutorService. */
	public static final List<String> IMPORT_ExecutorService = Arrays.asList("java.util.concurrent.ExecutorService");
	
	/** The Constant IMPORT_Future. */
	public static final List<String> IMPORT_Future = Arrays.asList("java.util.concurrent.Future");
	
	/** The Constant IMPORT_FutureTask. */
	public static final List<String> IMPORT_FutureTask = Arrays.asList("java.util.concurrent.FutureTask");
	
	/** The Constant IMPORT_CompletableFuture. */
	public static final List<String> IMPORT_CompletableFuture = Arrays.asList("java.util.concurrent.CompletableFuture");
	
	/** The Constant IMPORT_Runnable. */
	public static final List<String> IMPORT_Runnable = Arrays.asList("java.lang.Runnable");
	
	/** The Constant IMPORT_Callable. */
	public static final List<String> IMPORT_Callable = Arrays.asList("java.util.concurrent.Callable");
	
	/** The Constant IMPORT_ConCurrent. */
	public static final List<String> IMPORT_ConCurrent = Arrays.asList(
			"java.util.concurrent.ExecutorService"
			,"java.util.concurrent.Future"
			,"java.util.concurrent.FutureTask"
			,"java.util.concurrent.CompletableFuture"
			,"java.lang.Runnable"
			,"java.util.concurrent.Callable");

	/**
	 * Collect from select.
	 *
	 * @param select the selectelement
	 * @return 传入的对象中包含的java文件列表。
	 */
	public static List<ICompilationUnit> collectFromSelect(IProject select) {
		List<ICompilationUnit> allJavaFiles = new ArrayList<ICompilationUnit>();
		//得到选中的元素中的JAVA项目。

		IJavaProject project = JavaCore.create(select);
		
		try {
			//遍历项目的下一级，找到java源代码文件夹。
			for (IJavaElement element:project.getChildren()) {
				//目前来说，我见过的java项目结构，java源代码都是放入src开头，且最后不是resources结尾的包中。
				if(element.toString().startsWith("src")&&!element.getElementName().equals("resources")) {
					//找到包
					IPackageFragmentRoot packageRoot = (IPackageFragmentRoot) element;
					for (IJavaElement ele : packageRoot.getChildren()) {
						if (ele instanceof IPackageFragment) {
							IPackageFragment packageFragment = (IPackageFragment) ele;
							
							//一个CompilationUnit代表一个java文件。
							for (ICompilationUnit unit : packageFragment.getCompilationUnits()) {
								allJavaFiles.add(unit);
							}
						}
					}
				}
			}
			
		}catch(JavaModelException e) {
			e.printStackTrace();
		}
		

		return allJavaFiles;
	}

	/**
	 * 操作import的方法，将JAVA文件传入，将要查找的import字符串传入.
	 * 此后若有需要更改import的情况，我不再考虑修改，而是直接添加我所需要的Imports,
	 * 详情查看{@ImportRewrite}
	 *
	 * @param javafile  JAVA文件
	 * @param importold 要查找的import字符串
	 * @return true, 如果存在该字符串
	 */
	public static boolean searchImport(ICompilationUnit javafile, List<String> importold) {

		boolean flag = false;
		try {
			CompilationUnit astRootNode = getASTRootNode(javafile);

			List<ImportDeclaration> oldImports = new ArrayList<ImportDeclaration>();
			//得到所有的import.
			AnalysisUtils.getImportNodes(astRootNode, oldImports, false);
			for(String oldString :importold)
			{
				for(ImportDeclaration oldImport:oldImports)
				{
					if(oldImport.getName().toString().equals(oldString)) {
						flag = true;
						return flag;
					}
				}

			}
			
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return flag;
	}
	
	
	/**
	 * Gets the AST Root node,CompilationUnit类型是整个java文件在AST
	 * 树上的表示，它的根节点就是一个CompilationUnit类型。.
	 *
	 * @param javafile the javafile
	 * @return the AST Root node
	 * @throws JavaModelException the java model exception
	 */
	public static CompilationUnit getASTRootNode(ICompilationUnit javafile) throws JavaModelException {

		String stringSource = javafile.getSource();
		Document document = new Document(stringSource);

		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setSource(javafile);
		CompilationUnit astRootNode = (CompilationUnit) parser.createAST(null);
//		astRoot.getRoot();//可能不需要再获取root节点了，默认可能返回的RootNode。
		return astRootNode;
	}
	
	

	/**
	 * 从给定的ASTNode（第一个形参），进行【查找】遍历，若找到对应类型的Node结构，就将它的结果放入第二个参数里。
	 *
	 *尝试用泛型，但是出问题了，不知如何解决暂时不用了。
	 * @param <T>           the generic type 要查找的特定的Node结构类型
	 * @param astNode       the ast node 查找的节点（包含子树）
	 * @param results       the results 结果节点列表
	 * @param visitChildren the visit children 是否查找子树
	 */
//	public static <T> void getSpecificNodes(ASTNode astNode,final List<T> results,boolean visitChildren) {
//		astNode.accept(new ASTVisitor() {
//			public boolean visit(T node) {
//				System.out.println(node);
//				results.add(node);
//				return visitChildren;
//			}
//		});
//	}
//	
	public static void getImportNodes(ASTNode astNode,final List<ImportDeclaration> results,boolean visitChildren) {
		astNode.accept(new ASTVisitor() {
			public boolean visit(ImportDeclaration node) {
				System.out.println(node.getName());
				results.add(node);
				return visitChildren;
			}
		});
	}

	
	
	
	
	

}
