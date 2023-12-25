package refuture.sootUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import refuture.refactoring.AnalysisUtils;
import refuture.refactoring.RefutureException;
import soot.AmbiguousMethodException;
import soot.Body;
import soot.G;
import soot.Local;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.ValueBox;
import soot.jimple.Stmt;
import soot.jimple.internal.ImmediateBox;
import soot.jimple.internal.JimpleLocalBox;
import soot.toolkits.scalar.LocalDefs;


/*
 * 这个工具类是用来适配AST的，在AST上进行交互时，需要的方法。
 */
public class AdaptAst {

	/**
	 * 从方法调用的ASTNode得到对应的Soot的Jimple Stmt.
	 *
	 * @param miv the miv
	 * @return the jimple invoc stmt
	 */
	public static Stmt getJimpleInvocStmt(MethodInvocation miv) {
		CompilationUnit cu = (CompilationUnit)miv.getRoot();
		int lineNumber = cu.getLineNumber(miv.getStartPosition());//行号
		String ivcMethodName = miv.getName().toString();//调用的方法的名称，只包含名称
		SootClass sc = getSootClass4InvocNode(miv);
//		if(sc == null) {
//			return null;
//		}
		SootMethod sm = getSootMethod4invocNode(miv);
		if(!AnalysisUtils.skipMethodName.isEmpty()) {
			if (AnalysisUtils.skipMethodName.contains(sm.getSignature())) {
				return null;
			}
		}
		
		AnalysisUtils.debugPrint("[AdaptAST.getJimpleInvocStmt]:包含submit/execute方法调用的类："+sc.getName()+"方法名"+sm.getSignature());

		Body body =sm.retrieveActiveBody();
        Iterator<Unit> i=body.getUnits().snapshotIterator();
        boolean notLocalFlag = false;// if method is localmethod,its jimple name is stemp name not execute or submit.
        int jimpleLineNumber =0;
        int containTargetNum = 0;
        Stmt TempStmt = null;
        while(i.hasNext())
        {
            Stmt stmt=(Stmt) i.next();
            if(stmt.toString().contains(ivcMethodName)) {
            	notLocalFlag =true;
            	containTargetNum ++;
            	if(containTargetNum == 1) {
            		TempStmt = stmt;
            	}
            	jimpleLineNumber = stmt.getJavaSourceStartLineNumber();
            	if(jimpleLineNumber==lineNumber||lineNumber - jimpleLineNumber == 1) {
            		return stmt;
            	}
            }
            
        }
//		throw new IllegalStateException("[getJimpleInvocStmt]获取调用节点对应的Stmt出错");
        if(containTargetNum == 1) {
        	return TempStmt;
        }
        if(notLocalFlag) {
        	System.out.println("@error[AdaptAST.getJimpleInvocStmt]:获取调用节点对应的Stmt出错，找到有调用名称的语句，但是行号不对应且不是唯一一个这个方法调用在这个方法中{MethodSig:"
        +sm.getSignature()+"ASTLineNumber:"+lineNumber+"JimLineNumber:"+jimpleLineNumber);
        }
		//从这里开始分出lambda的处理
		if(AdaptAst.invocInLambda(miv)>0) {
			return getJimpleInvocStmtInLambda(miv);
		}
		throw new RefutureException(miv,"排查错误原因。");
//        return null;
		//这里后期需要修改为返回null,可以增加程序健壮性。不过走到这里，肯定有程序的源代码，所以应该要有它的class文件的，
		//也就是说正常情况下不应该出错。
	}
	private static Stmt getJimpleInvocStmtInLambda(MethodInvocation miv) {
		CompilationUnit cu = (CompilationUnit)miv.getRoot();
        //在主类的方法中得到想要的方法调用的Stmt.
		SootMethod mainMethod = getSootRealFunction4InLambda(miv);
//        for(SootMethod mainMethod : mainClassMethodList) {
    	Iterator<Unit> mainMethodIterator = mainMethod.retrieveActiveBody().getUnits().snapshotIterator();
    	while(mainMethodIterator.hasNext()) {
    		Stmt stmt3=(Stmt) mainMethodIterator.next();
    		if(stmt3.containsInvokeExpr()&&stmt3.toString().contains(miv.getName().toString())
    				&&stmt3.getJavaSourceStartLineNumber() == cu.getLineNumber(miv.getStartPosition())){
    			return stmt3;
    		}
    	}
//        }
        throw new RefutureException(miv);
	}
	//若MIV存在于Lambda表达式,则得到实际MIV所在的SootMethod,也就是存在于主SootClass中的实际Lambda表达式内容的方法的SootMethod.
	public static SootMethod getSootRealFunction4InLambda(MethodInvocation miv) {
		int deep = invocInLambda(miv);
		//得到通过AST得到的方法名称，类名称，调用的行号。
		//得到lambda外的函数调用的名称
		//12.19修改,得到SootMethod下的第一层lambda外的methodInvocation.并获得总共的层数.若外层不是方法调用,而是变量定义语句,则应该会报错,报错的时候再解决.
		List<Expression> InvocLambdaMethodList = AdaptAst.getInvocLambdaMethod2Delcaration(miv,deep);
		
		SootClass sc = getSootClass4InvocNode(miv);
		SootMethod sm =getSootMethod4invocNode(miv);
		SootMethod mainMethod = sm;
		//得到lambda的SootClass,通过调用Lambda的MIV所在的Body和Stmt.
		for(int i = 0 ;i < InvocLambdaMethodList.size();i++) {
			Expression invocLambdaMethod = InvocLambdaMethodList.get(i);
			mainMethod = getSootRealFunction4Lambda(mainMethod,invocLambdaMethod,sc.getName());
		}
		return mainMethod;
	}
	// 得到方法调用及其外部的SootMethod, 以及主类的SootClass.getName().得到存在于主SootClass中的实际Lambda表达式内容的方法的SootMethod.
	private static SootMethod getSootRealFunction4Lambda(SootMethod sm,Expression invocLambdaMethod,String mainClassName) {
		SootClass lambdaClass = getInvocSootLambdaOnBody(sm,invocLambdaMethod);
    	SootMethod method = getSootFunction4Lambda(lambdaClass);
    	//新的情况，在lambda表达式中，并没有直接的调用这个函数。而是又调用存在于主类的一个lambda方法，并且里面包含行号。
		//在关键的方法中，筛选调用了主类定义的方法的语句，通过字符串匹配，得到方法签名，然后进入该方法进行寻找。
    	Iterator<Unit> lambdaIterator = method.retrieveActiveBody().getUnits().snapshotIterator();
    	List<SootMethod> mainClassMethodList = new ArrayList<SootMethod>();
    	while(lambdaIterator.hasNext()) {
    		Stmt stmt2=(Stmt) lambdaIterator.next();
    		if(stmt2.containsInvokeExpr()) {
    			String unitString = stmt2.toString();
                int firstLessThanIndex = unitString.indexOf("<");  
                int firstColonIndex = unitString.indexOf(":", firstLessThanIndex);  
                unitString = unitString.substring(firstLessThanIndex + 1, firstColonIndex);
                if(unitString.equals(mainClassName)) {
                	mainClassMethodList.add(stmt2.getInvokeExpr().getMethod());
                }
    		}
    	}
        if(mainClassMethodList.isEmpty()) {
        	throw new IllegalStateException();
        }
        if(mainClassMethodList.size()>1) {
        	throw new RefutureException("得到的主类中的lambda调用方法大于1;"+mainClassMethodList.toString());
        }
		return mainClassMethodList.get(0);
	}
	// 得到调用Lambda的MIV,及其所在的SootMethod中实际调用的Lambda的SootClass.
	private static SootClass getInvocSootLambdaOnBody(SootMethod sm,Expression invocLambdaMethod){
		//这里得到了对应的JimpleIR中的调用lambda表达式的stmt。
		CompilationUnit cu = (CompilationUnit)invocLambdaMethod.getRoot();
		int invocLambdaMethodLineNumber = cu.getLineNumber(invocLambdaMethod.getStartPosition());
		Stmt bMIVstmt = null;		
		Local lambdaLocal = null;
		int taskNumber = 1;
		//12.25日添加，支持new 调用构造方法时传入的lambda，这时使用lambda实现的参数不能够在arguments中找到。
		if(invocLambdaMethod instanceof ClassInstanceCreation) {
			ClassInstanceCreation invocLambdaClassInstanceCreation = (ClassInstanceCreation)invocLambdaMethod;
			bMIVstmt=getStmt4StringOnBody(sm, "void <init>", invocLambdaMethodLineNumber);
			for(Object ob :invocLambdaClassInstanceCreation.arguments()) {
				if(ob instanceof LambdaExpression) {
					break;
				}
				taskNumber++;
			}
		}else {
			MethodInvocation invocLambdaMethodInvocation = (MethodInvocation)invocLambdaMethod;
			bMIVstmt=getStmt4StringOnBody(sm, invocLambdaMethodInvocation.getName().toString(), invocLambdaMethodLineNumber);
			// 寻找第几个参数是Lambda表达式,一般来说不会同时有两个异步任务对象，我就只记录参数中的第一个任务类型的位置
			for(Object ob :invocLambdaMethodInvocation.arguments()) {
				if(ob instanceof LambdaExpression) {
					break;
				}
				taskNumber++;
			}
		}
		
		List<ValueBox> boxList=bMIVstmt.getUseBoxes();
		int count = 1;
		for(ValueBox valueBox : boxList) {
			if(valueBox instanceof ImmediateBox) {
				if(valueBox.getValue() instanceof Local) {
					lambdaLocal = (Local)valueBox.getValue();
				}
				if(count == taskNumber) {
					break;
				}
				count +=1;
			}
		}
		
		LocalDefs ld = G.v().soot_toolkits_scalar_LocalDefsFactory().newLocalDefs(sm.retrieveActiveBody());
		List<Unit> units = ld.getDefsOfAt(lambdaLocal, bMIVstmt);
		if(units.size() !=1) {
			throw new RefutureException("不应该不是1");
		}
		Unit unit = units.get(0);
		String unitString = unit.toString();
        int firstLessThanIndex = unitString.indexOf("<");  
        int firstColonIndex = unitString.indexOf(":", firstLessThanIndex);  
        unitString = unitString.substring(firstLessThanIndex + 1, firstColonIndex);
        return Scene.v().getSootClass(unitString);
	}

	// 给定SootMethod 和 要匹配的字符串及行号,若匹配成功,则返回Stmt.
	public static Stmt getStmt4StringOnBody(SootMethod sm,String mivName,int ASTLineNume) {
		Body body =sm.retrieveActiveBody();
        Iterator<Unit> it=body.getUnits().snapshotIterator();
        int countInvoc = 0;
        Iterator<Unit> tmpit = body.getUnits().snapshotIterator();
        while(tmpit.hasNext())//防止判断行号从api中读取的有些误差,若只有一个调用则不需要判断行号
        {
            Stmt stmt=(Stmt) tmpit.next();
            if(stmt.toString().contains(mivName)) {
            	countInvoc ++;
            }
        }
        while(it.hasNext())
        {
            Stmt stmt=(Stmt) it.next();
            if(stmt.toString().contains(mivName)&&((countInvoc == 1)||(stmt.getJavaSourceStartLineNumber()==ASTLineNume))) {
            	return stmt;
            }
        }
		throw new RefutureException("从SootMethd的Body中,没有找到包含文本对应的Stmt");
	}
	
	// 得到一个Lambda SootClass的三个方法中,函数式接口实现方法的SootMethod.
	public static SootMethod getSootFunction4Lambda(SootClass sootLambdaClass) {
		List<SootMethod> lambdaSootMethodList = sootLambdaClass.getMethods();
        for(SootMethod method : lambdaSootMethodList) {
        	if(method.getName().toString().equals("<init>")||method.getName().toString().equals("bootstrap$")) {
        		continue;
        	}
        	return method;
        }
		throw new RefutureException("未获得SootLambdaClass的sootMethod");
	}
	//得到存在链式调用Lambda表达式嵌套的情况下,调用Labmda表达式的方法由MethodDeclaration到MIV的所有调用Lambda的MethodInvocation列表
	//因为JDT对于构造方法调用，不是MethodInvocation，所有这里加入构造方法调用支持。
	private static List<Expression> getInvocLambdaMethod2Delcaration(MethodInvocation miv,int deep) {
		List<Expression> methodInvocList = new ArrayList<>() ;
		ASTNode node = miv;
		for(;deep>0;deep--) {
			do {
				node = node.getParent();
			}while(!(node instanceof LambdaExpression));
			//不考虑其他可能,报错再说,只要需要变量/对象的地方,就有可能需要LambdaExpression.
			while(!(node instanceof MethodInvocation)&&!(node instanceof ClassInstanceCreation)) {
				node = node.getParent();
				if (node instanceof MethodDeclaration) {
					throw new RefutureException(miv);
				}
			}
			methodInvocList.add((Expression) node);
		}
		Collections.reverse(methodInvocList);
		return methodInvocList;
	}
	//返回当前MIV是否在Lambda表达式中,在的话若存在嵌套方法调用Lambda,则计算距离MethodDelaration深度.返回值大于0代表存在.
	public static int invocInLambda(MethodInvocation miv) {
		int deep = 0;
		ASTNode node = (ASTNode) miv;
		MethodDeclaration method = AnalysisUtils.getMethodDeclaration4node(miv);
		if(method != null) {
			while(!node.equals(method)) {
				if(node instanceof LambdaExpression) {
					deep++;
				}
				node = node.getParent();
			}
		}else {
			TypeDeclaration type = AnalysisUtils.getTypeDeclaration4node(miv);
			while(!node.equals(type)) {
				if(node instanceof LambdaExpression) {
					deep++;
				}
				node = node.getParent();
			}
		}
		return deep;
	}

	public static SootClass getSootClass4InvocNode(MethodInvocation incovNode) {
		ITypeBinding itb;
		String typeFullName;
		ASTNode astNode = AnalysisUtils.getMethodDeclaration4node(incovNode);
		if(astNode == null) {//说明没有在方法内部
			TypeDeclaration td = AnalysisUtils.getTypeDeclaration4node(incovNode);
			if(td == null) {throw new RefutureException(incovNode,"这个文件主类型可能为枚举类型");}
			itb = td.resolveBinding();
		}else if(astNode.getParent() instanceof AnonymousClassDeclaration) {
			AnonymousClassDeclaration ad = (AnonymousClassDeclaration)astNode.getParent();
			itb = ad.resolveBinding();
		}else {
			TypeDeclaration td=(TypeDeclaration)astNode.getParent();//MethodDeclaration 节点的父节点就是TypeDeclaration
			itb = td.resolveBinding();//得到FullName,必须是用绑定。
		}
		if(itb.isNested()) {
			typeFullName = itb.getBinaryName();
		}else {
			typeFullName = itb.getQualifiedName();
		}
		// maybe null
		if(typeFullName == null){
			System.out.println("@error[AdaptAst.getSootClass4InvocNode]:因为获取绑定信息中的名称出错,未获取成功,所以得不到类型名");
			throw new RefutureException(incovNode,"@error[AdaptAst.getSootClass4InvocNode]:因为获取绑定信息中的名称出错,未获取成功,所以得不到类型名");
//			return null;
			}
		SootClass sc = Scene.v().getSootClass(typeFullName);
		if(sc.isPhantom()) {
//			return null;
			throw new RefutureException(incovNode,"@error[AdaptAst.getSootClass4InvocNode]:调用了虚幻类，请检查soot ClassPath,虚幻类类名为:"+typeFullName);
		}
		return sc;
	}
	public static Set<String> getMayTypesName4ReceiverObject(Stmt invocStmt) {
		List<ValueBox> lvbs = invocStmt.getUseBoxes();
		Iterator<ValueBox> it =lvbs.iterator();
    	while(it.hasNext()) {
    		Object o = it.next();
    		if (o instanceof JimpleLocalBox) {
    			//Soot会在JInvocStmt里放入InvocExprBox,里面有JInterfaceInvokeExpr,里面有argBoxes和baseBox,分别存放ImmediateBox,JimpleLocalBox。
    			JimpleLocalBox jlb = (JimpleLocalBox) o;
    			Local local = (Local)jlb.getValue();
    			PointsToAnalysis pa = Scene.v().getPointsToAnalysis();
    			PointsToSet ptset = pa.reachingObjects(local);
    			Set<Type> typeSet = ptset.possibleTypes();
    			if(typeSet == null || typeSet.size()==0) {
    				break;
    			}
    			Set<String> typeSetStrings = new HashSet<>();
    			for (Type obj : typeSet) {
    				typeSetStrings.add(obj.toString()); // 将每个对象转换为字符串类型并添加到 Set<String> 中
    			}
    			return typeSetStrings;
    		}
    	}
		return null;
	}
	public static SootMethod getSootMethod4invocNode(MethodInvocation invocationNode) {
		SootMethod sm;
		SootClass sc = getSootClass4InvocNode(invocationNode);
		try{
			sm= sc.getMethodByName(AnalysisUtils.getSimpleMethodNameofSoot(invocationNode));
		}catch(AmbiguousMethodException e) {
			sm = sc.getMethod(AnalysisUtils.getMethodNameNArgusofSoot(invocationNode));
		}
		return sm;
	}
}
