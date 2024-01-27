package refuture.refactoring;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;

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


}
