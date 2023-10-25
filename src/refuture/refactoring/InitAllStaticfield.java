package refuture.refactoring;

import refuture.sootUtil.Cancel;
import refuture.sootUtil.ExecutorSubclass;

public class InitAllStaticfield {
	public static boolean init() {
		boolean status =Future2Completable.initStaticField();
		ExecutorSubclass.initStaticField();
		Cancel.initStaticField();
		boolean status1 = ForTask.initStaticField();
		return status&&status1;
	}

}
