package refuture.refactoring;

import refuture.sootUtil.ExecutorSubclass;

public class InitAllStaticfield {
	public static boolean init() {
		boolean status =Future2Completable.initStaticField();
		ExecutorSubclass.initStaticField();
		return status;
	}

}
