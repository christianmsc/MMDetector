package tp1.handlers;

import java.util.ArrayList;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.internal.resources.Project;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import mmd.ast.DependencyVisitor;
import mmd.persistence.MethodTargets;
import mmd.refactorings.MoveMethod;
import mmd.utils.SingletonNullProgressMonitor;

@SuppressWarnings("restriction")
public class SampleHandler extends AbstractHandler {

	private ArrayList<IMethod> allMethods;
	private ArrayList<MethodTargets> methodsTargets;
	public static ArrayList<MethodTargets> newMethodsTargets;
	public static IJavaProject projectOriginal;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {

			allMethods = new ArrayList<IMethod>();
			methodsTargets = new ArrayList<MethodTargets>();
			newMethodsTargets = new ArrayList<MethodTargets>();
			MethodTargets mt;

			hideView();

			projectOriginal = getProjectFromWorkspace(event);
			
			// Faz uma copia do projeto
			System.out.print("Copiando projeto " + projectOriginal.getElementName() + "... ");
			IJavaProject projectCopy = cloneProject(projectOriginal.getProject());
			System.out.println("OK");

			getClassesMethods(projectCopy.getProject());

			MoveMethod mv = new MoveMethod();

			for (IMethod method : allMethods) {

				if (mv.ckeckIfMethodCanBeMoved(method)) {

					methodsTargets.add(mv.getMethodTargets());
				}

			}
			
			System.out.println("Métodos que podem ser movidos: "+methodsTargets.size());

			allMethods = null;

			for (MethodTargets m : methodsTargets) {
				mt = mv.canMethodGoAndCome(m);
				if (mt != null) {
					newMethodsTargets.add(mt);
				}
			}
			
			System.out.println("Métodos que podem ir e voltar: "+newMethodsTargets.size());

			methodsTargets = null;

			//Deleta a copia do projeto
			projectCopy.getProject().delete(true, SingletonNullProgressMonitor.getNullProgressMonitor());
			
			openView();
			

		} catch (OperationCanceledException | CoreException e) {

			e.printStackTrace();
		}
		
		

		return null;

	}

	private void getClassesMethods(final IProject project) throws CoreException {
		project.accept(new IResourceVisitor() {

			@Override
			public boolean visit(IResource resource) throws JavaModelException {
				if (resource instanceof IFile && resource.getName().endsWith(".java")) {
					ICompilationUnit unit = ((ICompilationUnit) JavaCore.create((IFile) resource));

					DependencyVisitor dp = new DependencyVisitor(unit);
					if (dp.getArrayMethod() != null) {
						allMethods.addAll(dp.getArrayMethod());
					}

				}
				return true;
			}
		});
	}

	private IJavaProject getProjectFromWorkspace(ExecutionEvent event) {

		TreeSelection selection = (TreeSelection) HandlerUtil.getCurrentSelection(event);

		if (selection == null || selection.getFirstElement() == null) {
			MessageDialog.openInformation(HandlerUtil.getActiveShell(event), "Information", "Please select a project");
			return null;
		}

		JavaProject jp;
		Project p;

		try {
			jp = (JavaProject) selection.getFirstElement();
			return JavaCore.create(jp.getProject());
		} catch (ClassCastException e) {
			p = (Project) selection.getFirstElement();
			return JavaCore.create(p.getProject());
		}
	}

	private void hideView() {
		IWorkbenchPage wp = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

		// Acha a view :
		IViewPart myView = wp.findView("tp1.views.SampleView");

		// Esconde a view :
		wp.hideView(myView);
	}

	private void openView() {
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("tp1.views.SampleView");
		} catch (PartInitException e) {
			e.printStackTrace();
		}
	}
	
	public static IJavaProject cloneProject(IProject iProject) {
		try {
			IProgressMonitor m = new NullProgressMonitor();
			IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
			IProjectDescription projectDescription;

			projectDescription = iProject.getDescription();

			String cloneName = iProject.getName() + "Temp";
			// create clone project in workspace
			IProjectDescription cloneDescription = workspaceRoot.getWorkspace().newProjectDescription(cloneName);
			// copy project files
			iProject.copy(cloneDescription, true, m);
			IProject clone = workspaceRoot.getProject(cloneName);

			cloneDescription.setNatureIds(projectDescription.getNatureIds());
			cloneDescription.setReferencedProjects(projectDescription.getReferencedProjects());
			cloneDescription.setDynamicReferences(projectDescription.getDynamicReferences());
			cloneDescription.setBuildSpec(projectDescription.getBuildSpec());
			cloneDescription.setReferencedProjects(projectDescription.getReferencedProjects());
			clone.setDescription(cloneDescription, null);
			clone.open(m);
			return JavaCore.create(clone);

		} catch (CoreException e) {

			e.printStackTrace();
			return null;
		}
	}

}