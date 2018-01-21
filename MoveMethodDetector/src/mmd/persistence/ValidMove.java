package mmd.persistence;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInstanceMethodProcessor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.CreateChangeOperation;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;

import mmd.utils.CSVUtils;
import mmd.utils.SingletonNullProgressMonitor;

@SuppressWarnings("restriction")
public class ValidMove {

	private IMethod method;
	private String target;
	private boolean error;

	public ValidMove(IMethod method, String target, boolean daErro) {
		this.method = method;
		this.target = target;
		this.error = daErro;
	}

	public IMethod getMethod() {
		return method;
	}

	public String getTarget() {
		return target;
	}

	public boolean isMoveWithError() {
		return error;
	}

	public void moveMethod(IMethod method) {
		try {

			MoveInstanceMethodProcessor processor = new MoveInstanceMethodProcessor(method,
					JavaPreferencesSettings.getCodeGenerationSettings(method.getJavaProject()));

			processor.checkInitialConditions(SingletonNullProgressMonitor.getNullProgressMonitor());

			IVariableBinding[] targets = processor.getPossibleTargets();

			for (IVariableBinding target : targets) {
				if (target.getType().getQualifiedName().compareTo(getTarget()) == 0) {
					processor.setTarget(target);
					processor.setInlineDelegator(true);
					processor.setRemoveDelegator(true);
					processor.setDeprecateDelegates(false);

					Refactoring refactoring = new MoveRefactoring(processor);
					refactoring.checkInitialConditions(SingletonNullProgressMonitor.getNullProgressMonitor());

					final CreateChangeOperation create = new CreateChangeOperation(
							new CheckConditionsOperation(refactoring, CheckConditionsOperation.ALL_CONDITIONS),
							RefactoringStatus.FATAL);

					PerformChangeOperation perform = new PerformChangeOperation(create);

					ResourcesPlugin.getWorkspace().run(perform, SingletonNullProgressMonitor.getNullProgressMonitor());

					CSVUtils.writeInGoldset(target.getType().getQualifiedName() + "::" + getMethod().getElementName(),
							getMethod().getDeclaringType().getFullyQualifiedName());

					break;
				}
			}
		} catch (Exception e) {
			return;
		}
	}
}