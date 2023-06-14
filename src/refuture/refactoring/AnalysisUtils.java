package refuture.refactoring;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.Change;

// TODO: Auto-generated Javadoc
/**
 * The Class AnalysisUtils.
 * 提供分析方法的工具包类，它的方法都是静态的。
 */
public class AnalysisUtils {

	/** The projectpath. */
	private static List<String> PROJECTOUTPATH;
	
	/** The projectpath. */
	private static String PROJECTPATH;
	
	
	
	/**
	 * Collect from select,并得到项目的路径。
	 *
	 * @param project the project
	 * @return 传入的对象中包含的java文件列表。
	 */
	public static List<ICompilationUnit> collectFromSelect(IJavaProject project) {
		List<ICompilationUnit> allJavaFiles = new ArrayList<ICompilationUnit>();
//		ExportReferencedLibraries.export(project);//导出依赖的jar到项目下的refuture-lib
		boolean testFlag = true;//测试标志，是否将test-classes替换classes从而得到测试代码生成的class文件路径。可能只适合maven 项目。
		
		//得到输出的class在的文件夹，方便后继使用soot分析。
		try {
			String projectoutpath = project.getOutputLocation().toOSString();
			PROJECTPATH = project.getProject().getLocation().toOSString();
			int lastIndex = PROJECTPATH.lastIndexOf("/");
			String RUNTIMEPATH = PROJECTPATH.substring(0, lastIndex);
			PROJECTOUTPATH = new ArrayList<String>();
			PROJECTOUTPATH.add(RUNTIMEPATH+projectoutpath);
		}catch(JavaModelException ex){
			System.out.println(ex);
		}
		//得到选中的元素中的JAVA项目。
		try {
			//遍历项目的下一级，找到java源代码文件夹。
			for (IJavaElement element:project.getChildren()) {
				if(testFlag&&element.toString().startsWith("test")) {
					String projectOutPath = PROJECTOUTPATH.get(0);
					String porjectTestOutPath = projectOutPath.replace("classes", "test-classes");
					PROJECTOUTPATH.add(porjectTestOutPath);
				}
				//目前来说，我见过的java项目结构，java源代码都是放入src开头，且最后不是resources结尾的包中。
				boolean javaFolder = element.toString().startsWith("src")&&!element.getElementName().equals("resources")||element.toString().startsWith("test");
//				boolean javaFolder = element.toString().startsWith("java");
				
				if(javaFolder) {
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
	 * 得到 node所属的方法的Soot中名称，在方法体外和构造函数，则返回“void {@code<init>}()”,否则返回方法签名.
	 *方法的返回值若是引用类型，需要写全名。
	 * @param node node必须保证，是类里面的语句节点，否则陷入无限循环。
	 * @return the method name
	 */
	public static String getSootMethodName(ASTNode node) {
		String methodSootName ="void <init>()";

		while(!(node instanceof TypeDeclaration) ) {
			if(node instanceof MethodDeclaration) {
				MethodDeclaration mdNode = (MethodDeclaration)node;
				IMethodBinding imb =mdNode.resolveBinding();
				Type type = mdNode.getReturnType2();
				String methodReturnTypeName;
				if(type == null) {//构造函数为null,但是需要判断是否有参数
					if(getmethodParameters(mdNode).isEmpty())
						{break;}else {
							methodSootName ="void <init>("+getmethodParameters(mdNode)+")";
							break;
						}
				}
				if(type.resolveBinding().isTypeVariable()) {
					methodReturnTypeName = "java.lang.Object";
				}else {
					methodReturnTypeName = imb.getReturnType().getQualifiedName().toString();
				}
				String methodSimpleName = mdNode.getName().toString();
				String methodParameters = getmethodParameters(mdNode);
				methodSootName = methodReturnTypeName+" "+methodSimpleName+"("+methodParameters+")";
				break;
			}
			node = node.getParent();
			if(node == node.getParent()) {
				System.out.println("[getMethodName]：传入的ASTNode有问题");
				throw new ExceptionInInitializerError("[getMethodName]：传入的ASTNode有问题");
			}
		}
//		System.out.println("[AnalysisUtils.getSootMethodName处理前]"+methodSootName);
		//去除额外的<>和因此产生的空格。
	    String result = methodSootName.replaceAll("<[^<>]*>", "");
	    do {
	    	methodSootName = result;
		    result = methodSootName.replaceAll("<[^<>]*>", "");
	    }while(!result.equals(methodSootName));

		methodSootName = methodSootName.replaceAll("\\s{2,}", " ");
//		System.out.println("[AnalysisUtils.getSootMethodName处理前]"+methodSootName);
		return methodSootName;
		
	}
	
	/**
	 * 通过MethodDeclaration,得到参数的类型全名。
	 * 需要开启绑定。
	 *
	 * @param mdNode the md node
	 * @return the method parameters
	 */
	private static String getmethodParameters(MethodDeclaration mdNode){
		List<ASTNode> parameterList = mdNode.parameters();
		String parameterString = new String();
		if(parameterList.isEmpty()) {
			return "";
		}else {
			for(ASTNode astnode:parameterList) {
				SingleVariableDeclaration param = (SingleVariableDeclaration) astnode;
				 ITypeBinding typeBinding = param.getType().resolveBinding();
				 String typefullName;
				 if(typeBinding.isTypeVariable()) {
					 typefullName = "java.lang.Object";
				 }else if(typeBinding.isPrimitive()||typeBinding.isArray()){
					 typefullName = typeBinding.getQualifiedName();
				 }else {
					 typefullName = typeBinding.getBinaryName();
				 }
                if(parameterString.isEmpty()) {
                	parameterString = typefullName;
                }else {
                	parameterString = parameterString+","+typefullName;
                }
			}
			return parameterString;
		}
	}
	
	/**
	 * 得到节点所属的方法块的方法定义节点
	 *
	 * @param node the node
	 * @return the method declaration 4 node
	 */
	public static MethodDeclaration getMethodDeclaration4node(ASTNode node) {

		while(!(node instanceof TypeDeclaration) ) {
			if(node instanceof MethodDeclaration) {
				break;
			}
			node = node.getParent();
			if(node == node.getParent()) {
				System.out.println("[getMethodName]：传入的ASTNode有问题");
				throw new ExceptionInInitializerError("[getMethodName]：传入的ASTNode有问题");
			}
		}
		return (MethodDeclaration) node;
	}
	
	/**
	 * Checks if is declaration or assignment.
	 *
	 * @param invocationNode the invocation node
	 * @return true, if is declar or assign
	 */
	public static boolean isDeclarOrAssign(MethodInvocation invocationNode) {
		ASTNode parentNode= invocationNode.getParent();
		
		if(parentNode instanceof Assignment || parentNode instanceof VariableDeclarationStatement) {
			return true;
		}
		return false;
	}
	
	public static String getProjectPath() {
		return PROJECTPATH;
	}

	public static List<String> getSootClassPath() {
		return PROJECTOUTPATH;
	}
	public static void throwNull() {
		throw new NullPointerException();
	}
}
