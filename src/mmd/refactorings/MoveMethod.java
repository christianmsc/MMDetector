package mmd.refactorings;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;

import mmd.utils.CSVUtils;
import mmd.utils.SingletonNullProgressMonitor;

import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInstanceMethodProcessor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

@SuppressWarnings("restriction")
public class MoveMethod {

	public boolean ckeckIfMethodCanBeMoved(IMethod method) throws OperationCanceledException, CoreException {

		boolean temUmValido = false;

		try {

			System.out.print("Tentando method " + method.getElementName() + "... ");
			if (method.isConstructor()) {
				System.out.println("Eh construtor!");
				return false;
			}

			MoveInstanceMethodProcessor processor = new MoveInstanceMethodProcessor(method,
					JavaPreferencesSettings.getCodeGenerationSettings(method.getJavaProject()));

			processor.checkInitialConditions(SingletonNullProgressMonitor.getNullProgressMonitor());

			IVariableBinding[] targets = processor.getPossibleTargets();

			if (targets.length == 0 || targets == null) {
				System.out.println("Nao da pra mover pra nenhum lugar");
				return false;
			}

			else {

				System.out.println();

				for (int i = 0; i < targets.length; i++) {

					IVariableBinding candidate = targets[i];
					System.out.print("Destino: " + candidate.getType().getName() + ": ");

					if (candidate.getType().isEnum() || candidate.getType().isInterface()) {
						System.out.println("É enumerado ou interface");
						continue;
					}

					processor = new MoveInstanceMethodProcessor(method,
							JavaPreferencesSettings.getCodeGenerationSettings(method.getJavaProject()));

					processor.checkInitialConditions(SingletonNullProgressMonitor.getNullProgressMonitor());

					processor.setTarget(candidate);
					processor.setInlineDelegator(true);
					processor.setRemoveDelegator(true);
					processor.setDeprecateDelegates(false);

					Refactoring ref = new MoveRefactoring(processor);
					RefactoringStatus status = null;

					status = ref.checkAllConditions(new NullProgressMonitor());

					if (status.isOK()) {

						System.out.println("OK!");

						Change undoChange = ref.createChange(new NullProgressMonitor());
						System.out.print("Undo: ");

						if (undoChange != null) {
							System.out.println("OK!");
							CSVUtils.writeValidMoveMethod(
									method.getDeclaringType().getFullyQualifiedName() + "::" + method.getElementName(),
									candidate.getType().getQualifiedName());
							temUmValido = true;
						}

						else {
							System.out.println("Falhou");
						}

					} else {
						System.out.println("Falhou");
					}

				}
			}

			return temUmValido;

		} catch (Exception e) {
			return false;
		}
	}
}
