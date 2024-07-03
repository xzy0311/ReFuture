package refuture.refactoring;

import org.eclipse.jdt.core.dom.ASTNode;

import soot.SootMethod;

// 这个类用来错误时输出当前处理的方法调用节点所在的类和方法以及行号
public class RefutureException extends RuntimeException {
    public RefutureException(ASTNode expNode) {
        super(AnalysisUtils.invocNodeInfo(expNode));
    }
    public RefutureException(String info) {
        super(info);
    }
    public RefutureException(ASTNode expNode,String info) {
        super(AnalysisUtils.invocNodeInfo(expNode)+"|"+info);
    }
    public RefutureException(SootMethod sm,String stmt) {
    	super("所在方法签名："+sm.getSignature()+stmt);
    }

}
