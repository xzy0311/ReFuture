package refuture.refactoring;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

public class FutureTaskRefactoringWizard extends RefactoringWizard {
	
	UserInputWizardPage page;
//	AnnotationRefactoringWizardPage page;

	public FutureTaskRefactoringWizard(Refactoring refactoring) {
		super(refactoring, WIZARD_BASED_USER_INTERFACE);

	}

	@Override
	protected void addUserInputPages() {
		page = new FutureTaskRefactoringWizardPage("refactor FutureTask");
//		addPage(page);
	}

}
