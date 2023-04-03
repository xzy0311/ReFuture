package refuture.sootUtil;

import java.util.ArrayList;
import java.util.List;

import soot.Hierarchy;
import soot.Scene;
import soot.SootClass;

// TODO: Auto-generated Javadoc
/**
 * 通过传入类的名称，得到类层次结构。可以通过方法获取所有的子类，其他用到再添加。
 */

public class ClassHierarchy {
	

	/**
	 * 得到指定类或者接口的所有的application子类，不包括它们自身。
	 *
	 * @param className 要得到子类的类的名字
	 * @return the sub classesfor
	 */
	public static List<SootClass> getSubClassesfor(String className) {
		 Hierarchy hierarchy = Scene.v().getActiveHierarchy();
	     SootClass sootClass = Scene.v().getSootClass(className);
//	     sootClass.setApplicationClass();

	     if (sootClass.isInterface()){
		     return hierarchy.getImplementersOf(sootClass);
	     }else {

	    	 return hierarchy.getSubclassesOf(sootClass);
	     }

	}
	//得到输入的Executor和Future的所有的子类，作为初始条件检查的一部分。
	public static List<String> getSubClassInJDK(String name) {
		List<SootClass> subClassInJDK = getSubClassesfor(name);
		List<String> classesName = new ArrayList<String>();
		for(SootClass sc:subClassInJDK) {
			classesName.add(sc.getName());
//			System.out.println("[ClassHierarchy debug]"+sc.getName());
		}
		return classesName;
	}
	
	public static List<String> initialCheckForClassHierarchy() {
	
		List<String> additionalExecutorClass =ExecutorSubclass.getAdditionalExecutorClass();
		return additionalExecutorClass;
	}

	

	
}
