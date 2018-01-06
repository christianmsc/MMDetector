package mmd.refactorings;

import java.util.ArrayList;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInstanceMethodProcessor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.CreateChangeOperation;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;

import mmd.persistence.MethodTargets;
import mmd.utils.CSVUtils;
import mmd.utils.SingletonNullProgressMonitor;

@SuppressWarnings("restriction")
public class MoveMethod {

	private MethodTargets methodTargets = null;

	public boolean ckeckIfMethodCanBeMoved(IMethod method) throws OperationCanceledException, CoreException {

		boolean temUmValido = false;
		ArrayList<String> validTargets;

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

				validTargets = new ArrayList<String>();

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
							validTargets.add(candidate.getType().getQualifiedName());
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

			if (temUmValido) {
				methodTargets = new MethodTargets(method, validTargets);
			}

			return temUmValido;

		} catch (Exception e) {
			return false;
		}
	}

	public MethodTargets canMethodGoAndCome(MethodTargets m) {

		try {
			System.out.println("---------------------------------------------------");
			System.out.println("Analisando metodo "+m.getMethod().getElementName()+" na classe "+m.getMethod().getDeclaringType().getElementName());
			
			//booleano que so verifica se pra algum target deu certo ir e voltar
			boolean peloMenosUmFoi = false;
			
			//Array que vai armazenar os targets que derem pro metodo ir e voltar
			ArrayList<String> validTargets = null;

			// Array que ira armazenar os targets ja encontrados para o metodo
			ArrayList<IVariableBinding> arrayTargets = new ArrayList<IVariableBinding>();

			MoveInstanceMethodProcessor processor = new MoveInstanceMethodProcessor(m.getMethod(),
					JavaPreferencesSettings.getCodeGenerationSettings(m.getMethod().getJavaProject()));

			processor.checkInitialConditions(SingletonNullProgressMonitor.getNullProgressMonitor());

			IVariableBinding[] targets = processor.getPossibleTargets();

			// laco que popula a arrayTargets
			for (IVariableBinding target : targets) {
				for (String targetDetected : m.getTargets()) {
					if (targetDetected.compareTo(target.getType().getQualifiedName()) == 0) {
						arrayTargets.add(target);
					}
				}
			}

			// laco para cada target que ira verificar se vai e volta
			for (IVariableBinding candidate : arrayTargets) {
				
				System.out.print("Indo para "+candidate.getType().getName()+"... ");
				
				processor = new MoveInstanceMethodProcessor(m.getMethod(),
						JavaPreferencesSettings.getCodeGenerationSettings(m.getMethod().getJavaProject()));

				processor.checkInitialConditions(SingletonNullProgressMonitor.getNullProgressMonitor());

				processor.setTarget(candidate);
				processor.setInlineDelegator(true);
				processor.setRemoveDelegator(true);
				processor.setDeprecateDelegates(false);

				Refactoring refactoring = new MoveRefactoring(processor);
				refactoring.checkInitialConditions(SingletonNullProgressMonitor.getNullProgressMonitor());

				final CreateChangeOperation create = new CreateChangeOperation(
						new CheckConditionsOperation(refactoring, CheckConditionsOperation.ALL_CONDITIONS),
						RefactoringStatus.FATAL);

				PerformChangeOperation perform = new PerformChangeOperation(create);

				// ate aqui foram os preperativos para mover o metodo, e agora ele sera movido
				ResourcesPlugin.getWorkspace().run(perform, SingletonNullProgressMonitor.getNullProgressMonitor());
				System.out.println("OK");

				// agora, procura-se pelo metodo movido na classe nova
				IMethod[] methods = m.getMethod().getJavaProject().findType(candidate.getType().getQualifiedName())
						.getMethods();

				for (IMethod methodMoved : methods) {
					
					if (methodMoved.getElementName().compareTo(m.getMethod().getElementName()) == 0) {
						
						System.out.println("Agora o metodo "+methodMoved.getElementName()+" ta na classe "+methodMoved.getDeclaringType().getElementName());
						
						//agora que achou o metodo, ve as novas classes que ele pode ser movido
						processor = new MoveInstanceMethodProcessor(methodMoved,
								JavaPreferencesSettings.getCodeGenerationSettings(methodMoved.getJavaProject()));

						processor.checkInitialConditions(SingletonNullProgressMonitor.getNullProgressMonitor());

						IVariableBinding[] newTargets = processor.getPossibleTargets();

						System.out.print("O metodo consegue voltar para "+m.getMethod().getDeclaringType().getElementName()+"? ");
						for (IVariableBinding target : newTargets) {
							
							//se um dos targets for a classe antiga, ve se da pra mover pra la
							if (target.getType().getQualifiedName()
									.compareTo(m.getMethod().getDeclaringType().getFullyQualifiedName()) == 0) {
								
								System.out.print("Aparentemente sim, vamos ver... ");

								processor.setTarget(target);
								processor.setInlineDelegator(true);
								processor.setRemoveDelegator(true);
								processor.setDeprecateDelegates(false);

								Refactoring ref = new MoveRefactoring(processor);
								RefactoringStatus status = null;

								status = ref.checkAllConditions(new NullProgressMonitor());
								
								//se da pra mover, aleluia! salva isso!
								if (status.isOK()) {
									System.out.println("Sim =)");
									if(validTargets == null){
										validTargets = new ArrayList<String>();
									}
									
									validTargets.add(candidate.getType().getQualifiedName());
									peloMenosUmFoi = true;
									
								}else{
									System.out.println("Nem deu =P");
								}
							}else{
								System.out.println();
								System.out.println("Da naum =(");
							}
						}
						
						break;
					}
				}
				
				//volta com o metodo para a classe original
				System.out.println("Voltando metodo para "+m.getMethod().getDeclaringType().getElementName());
				Change undoChange = perform.getUndoChange();
				undoChange.perform(SingletonNullProgressMonitor.getNullProgressMonitor());

			}
			
			if(peloMenosUmFoi){
				return new MethodTargets(m.getMethod(), validTargets);
			}else{
				return null;
			}

		} catch (Exception e) {
			return null;
		}
	}
	
	public MethodTargets getMethodTargets() {
		return methodTargets;
	}
}
