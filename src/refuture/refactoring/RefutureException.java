package refuture.refactoring;

import org.eclipse.jdt.core.dom.MethodInvocation;

// 这个类用来错误时输出当前处理的方法调用节点所在的类和方法以及行号
public class RefutureException extends RuntimeException {
    public RefutureException(MethodInvocation invocationNode) {
        super(AnalysisUtils.invocNodeInfo(invocationNode));
    }
    public RefutureException(String info) {
        super(info);
    }
    public RefutureException(MethodInvocation invocationNode,String info) {
        super(AnalysisUtils.invocNodeInfo(invocationNode)+"|"+info);
    }


}
