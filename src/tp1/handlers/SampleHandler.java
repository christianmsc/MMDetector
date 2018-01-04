package tp1.handlers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.internal.resources.Project;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import mmd.ast.DependencyVisitor;
import mmd.refactorings.MoveMethod;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TreeSelection;

@SuppressWarnings("restriction")
public class SampleHandler extends AbstractHandler {

	private ArrayList<IMethod> allMethods;
	public static ArrayList<IMethod> detectedMethods;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {

			allMethods = new ArrayList<IMethod>();
			detectedMethods = new ArrayList<IMethod>();

			hideView();

			IProject iProject = getProjectFromWorkspace(event);

			getClassesMethods(iProject);

			MoveMethod m = new MoveMethod();

			for (IMethod method : allMethods) {

				if (m.ckeckIfMethodCanBeMoved(method)) {

					detectedMethods.add(method);
				}

			}

			verifyGoldset();

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

	private IProject getProjectFromWorkspace(ExecutionEvent event) {

		TreeSelection selection = (TreeSelection) HandlerUtil.getCurrentSelection(event);

		if (selection == null || selection.getFirstElement() == null) {
			MessageDialog.openInformation(HandlerUtil.getActiveShell(event), "Information", "Please select a project");
			return null;
		}

		JavaProject jp;
		Project p;

		try {
			jp = (JavaProject) selection.getFirstElement();
			return jp.getProject();
		} catch (ClassCastException e) {
			p = (Project) selection.getFirstElement();
			return p.getProject();
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

	private void verifyGoldset() {
		ArrayList<String[]> goldset = readGoldsetFile();
		ArrayList<String[]> refactorings = readRefactoringsFile();
		int count = 0;

		for (String[] refactoring : refactorings) {
			int i = 0;
			for (String[] methodGoldset : goldset) {
				i++;
				if (methodGoldset[0].compareTo(refactoring[0]) == 0
						&& methodGoldset[1].compareTo(refactoring[1]) == 0) {
					count++;
					System.out.println(i);
				}
			}
		}

		System.out.println(count);
	}

	private ArrayList<String[]> readRefactoringsFile() {

		ArrayList<String[]> refactorings = new ArrayList<String[]>();
		String csvFile = System.getProperty("user.home") + "/refactorings.csv";
		String line = "";
		String cvsSplitBy = ",";

		try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
			while ((line = br.readLine()) != null) {

				// use comma as separator
				refactorings.add(line.split(cvsSplitBy));
				// String[] country = line.split(cvsSplitBy);
				// System.out.println("Country [code= " + country[4] + " ,
				// name=" + country[5] + "]");

			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		return refactorings;
	}

	private ArrayList<String[]> readGoldsetFile() {

		ArrayList<String[]> goldset = new ArrayList<String[]>();
		String csvFile = System.getProperty("user.home") + "/goldset.csv";
		String line = "";
		String cvsSplitBy = ",";

		try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
			while ((line = br.readLine()) != null) {

				// use comma as separator
				goldset.add(line.split(cvsSplitBy));
				// String[] country = line.split(cvsSplitBy);
				// System.out.println("Country [code= " + country[4] + " ,
				// name=" + country[5] + "]");

			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		return goldset;
	}
}