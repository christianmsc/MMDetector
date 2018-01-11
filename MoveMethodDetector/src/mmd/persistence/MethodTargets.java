package mmd.persistence;

import java.util.ArrayList;

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
public class MethodTargets {

	IMethod method;
	ArrayList<String> targets;

	public MethodTargets(IMethod method, ArrayList<String> targets) {
		this.method = method;
		this.targets = targets;
	}

	public IMethod getMethod() {
		return method;
	}

	public ArrayList<String> getTargets() {
		return targets;
	}

	public void moveMethod(IMethod method) {
		try {

			MoveInstanceMethodProcessor processor = new MoveInstanceMethodProcessor(method,
					JavaPreferencesSettings.getCodeGenerationSettings(method.getJavaProject()));

			processor.checkInitialConditions(SingletonNullProgressMonitor.getNullProgressMonitor());

			IVariableBinding[] targets = processor.getPossibleTargets();

			for (IVariableBinding target : targets) {
				if (target.getType().getQualifiedName().compareTo(getTargets().get(0)) == 0) {
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

					CSVUtils.writeInGoldset(getMethod().getDeclaringType().getFullyQualifiedName() + "::"
							+ getMethod().getElementName(), target.getType().getQualifiedName());
					
					CSVUtils.writeInGoldset(target.getType().getQualifiedName()+ "::"
							+ getMethod().getElementName(),getMethod().getDeclaringType().getFullyQualifiedName()  );

					break;
				}
			}
		} catch (Exception e) {
			return;
		}
	}
}