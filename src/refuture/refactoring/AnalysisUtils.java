package refuture.refactoring;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
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

	/** The projectpath. */
	private static String PROJECTOUTPATH;
	
	/** The projectpath. */
	private static String PROJECTPATH;
	
	/**
	 * Collect from select.
	 *
	 * @param project the project
	 * @return 传入的对象中包含的java文件列表。
	 */
	public static List<ICompilationUnit> collectFromSelect(IJavaProject project) {
		List<ICompilationUnit> allJavaFiles = new ArrayList<ICompilationUnit>();

//得到输出的class在的文件夹，方便后继使用soot分析。
		try {
			
			
			PROJECTOUTPATH = project.getOutputLocation().toOSString();
			
			PROJECTPATH = project.getProject().getLocation().toOSString();
			
			int lastIndex = PROJECTPATH.lastIndexOf("/");
			String RUNTIMEPATH = PROJECTPATH.substring(0, lastIndex);
			PROJECTOUTPATH = RUNTIMEPATH+PROJECTOUTPATH;
		}catch(JavaModelException ex){
			System.out.println(ex);
		}
		//得到选中的元素中的JAVA项目。
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
		System.out.println("[xzy处理前]"+methodSootName);
		//最后对methodsubsignature进行处理，将形式参数去除,将多余的空格去除，使用正则表达式。
//		String patternY = "<(\\w+)>"; // 匹配形如<asdf>的命令y
//		String patternX = "(?<=<)\\w+(?=>)"; // 匹配形如<asdf>中的命令x
//		String replacement = "java.lang.Object"; // 替换为java.lang.Object
//		Pattern patternYObj = Pattern.compile(patternY);
//		Matcher matcherY = patternYObj.matcher(methodSootName);
//		while (matcherY.find()) {
//		    String commandY = matcherY.group();
//		    Pattern patternXObj = Pattern.compile(patternX);
//		    Matcher matcherX = patternXObj.matcher(commandY);
//		    if (matcherX.find()) {
//		        String commandX = matcherX.group();
//		        methodSootName = methodSootName.replaceAll(commandY, "").replaceAll("(?<!\\S)" + commandX + "(?!\\S)", replacement);
//		    }
//		}
//		methodSootName = methodSootName.replaceAll("\\s+", " ").trim();
		//去除额外的<>和因此产生的空格。
	    String result = methodSootName.replaceAll("<[^<>]*>", "");
	    do {
	    	methodSootName = result;
		    result = methodSootName.replaceAll("<[^<>]*>", "");
	    }while(!result.equals(methodSootName));

		methodSootName = methodSootName.replaceAll("\\s{2,}", " ");
		System.out.println("[xzy处理后]"+methodSootName);
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
	
	

	public static String getProjectPath() {
		return PROJECTPATH;
	}


	
	public static String getSootClassPath() {
		return PROJECTOUTPATH;
	}

}
