package refuture.refactoring;

import refuture.astvisitor.AllVisiter;
import refuture.sootUtil.Cancel;
import refuture.sootUtil.CastAnalysis;
import refuture.sootUtil.CollectionEntrypoint;
import refuture.sootUtil.ExecutorSubclass;
import refuture.sootUtil.Instanceof;
import refuture.sootUtil.SootConfig;

public class InitAllStaticfield {
	public static boolean init() {
		boolean status =Future2Completable.initStaticField();
		ExecutorSubclass.initStaticField();
		Cancel.initStaticField();
		SootConfig.sootConfigStaticInitial();
		CollectionEntrypoint.initStaticField();
		Instanceof.initStaticField();
		CastAnalysis.initStaticField();
		AllVisiter.reSet();
		return status;
	}

}
