package refuture.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import refuture.refactoring.FutureTaskRefactoring;
import refuture.refactoring.FutureTaskRefactoringWizard;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;

public class SampleHandler extends AbstractHandler {
	IJavaElement select;
	Boolean firststart;
	{
		firststart = true;
	}
	private ISelectionListener selectionListener = new ISelectionListener() {
	    @Override
	    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
	        // 处理选择变化事件
			if (selection.isEmpty())
				select = null;
			else if (selection instanceof IStructuredSelection) {
				IStructuredSelection strut = ((IStructuredSelection) selection);
				if (strut.size() != 1)
					select = null;
				if (strut.getFirstElement() instanceof IJavaElement)
					{select = (IJavaElement) strut.getFirstElement();
					}else {
						select = null;
					}
			}else
				select = null;
//			action.setEnabled(true);
			SampleHandler.this.setBaseEnabled(select != null);
		}
	};
	

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		Shell shell = window.getShell();
		ISelectionService selectionService = window.getSelectionService();
		selectionService.addSelectionListener(selectionListener);
		if(select == null) {
			if(firststart) {
				MessageDialog.openInformation(
						shell,
						"refuture",
						"Refuture initial complete");
				firststart = false;
			}else {
				MessageDialog.openInformation(
						shell,
						"refuture",
						"Select is null");
			}

		}else {
			FutureTaskRefactoring refactor = new FutureTaskRefactoring(select);
			FutureTaskRefactoringWizard wizard = new FutureTaskRefactoringWizard(refactor);
			RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
			try {
				op.run(null, "Refactoring Future");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		return null;
	}
}
