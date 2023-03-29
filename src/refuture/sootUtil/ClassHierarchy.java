package refuture.sootUtil;

import java.util.List;

import soot.Hierarchy;
import soot.Scene;
import soot.SootClass;

// TODO: Auto-generated Javadoc
/**
 * 通过传入类的名称，得到类层次结构。可以通过方法获取所有的子类，其他用到再添加。
 */

public class ClassHierarchy {

/** The hierarchy. */
private static Hierarchy hierarchy = Scene.v().getActiveHierarchy();
	

	/**
	 * 得到指定类或者接口的所有的application子类，不包括它们自身。
	 *
	 * @param className 要得到子类的类的名字
	 * @return the sub classesfor
	 */
	public static List<SootClass> getSubClassesfor(String className) {
		
	     SootClass sootClass = Scene.v().getSootClass(className);
//	     sootClass.setApplicationClass();

	     if (sootClass.isInterface()){
		     for (SootClass c : hierarchy.getImplementersOf(sootClass)) {
		         System.out.println(c.getName());
		     }
		     return hierarchy.getImplementersOf(sootClass);
	     }else {
	    	 for(SootClass c:hierarchy.getSubclassesOf(sootClass)) {
	    		 System.out.println(c.getName());
	    	 }

	    	 return hierarchy.getSubclassesOf(sootClass);
	     }

	     

	}
}
