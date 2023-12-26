package refuture.refactoring;

import refuture.sootUtil.Cancel;
import refuture.sootUtil.CollectionEntrypoint;
import refuture.sootUtil.ExecutorSubclass;
import refuture.sootUtil.SootConfig;

public class InitAllStaticfield {
	public static boolean init() {
		boolean status =Future2Completable.initStaticField();
		ExecutorSubclass.initStaticField();
		Cancel.initStaticField();
		SootConfig.sootConfigStaticInitial();
		CollectionEntrypoint.initStaticField();
		boolean status1 = ForTask.initStaticField();
		return status&&status1;
	}

}
