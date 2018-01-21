package tp1.views;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import mmd.persistence.ValidMove;
import tp1.handlers.SampleHandler;

public class SampleView extends ViewPart {

	public static final String ID = "tp1.views.SampleView";

	private TableViewer viewer;
	private Action doubleClickAction;

	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout(4, false);
		parent.setLayout(layout);
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);

		String[] titles = { "Metodo", "Classe Original", "Classe Nova", "Da Erro Ao Mover?" };
		int[] bounds = { 200, 200, 200, 150 };

		TableViewerColumn col = createTableViewerColumn(titles[0], bounds[0], 0);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				ValidMove m = (ValidMove) element;
				return m.getMethod().getElementName();
			}
		});

		col = createTableViewerColumn(titles[1], bounds[1], 1);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				ValidMove m = (ValidMove) element;
				return m.getMethod().getDeclaringType().getFullyQualifiedName();
			}
		});

		col = createTableViewerColumn(titles[2], bounds[2], 2);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				ValidMove m = (ValidMove) element;
				return m.getTarget();
			}
		});

		col = createTableViewerColumn(titles[3], bounds[3], 3);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				ValidMove m = (ValidMove) element;
				if (m.isMoveWithError()) {
					return "Sim";
				} else {
					return "Nao";
				}
			}
		});

		viewer.refresh();

		final Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		viewer.setContentProvider(ArrayContentProvider.getInstance());

		viewer.setInput(SampleHandler.newMethodsTargets);
		getSite().setSelectionProvider(viewer);

		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalSpan = 2;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		viewer.getControl().setLayoutData(gridData);

		hookDoubleClickAction();

	}

	public void setFocus() {
		viewer.getControl().setFocus();
	}

	private TableViewerColumn createTableViewerColumn(String title, int bound, final int colNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setWidth(bound);
		column.setResizable(true);
		column.setMoveable(true);
		return viewerColumn;
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});

		doubleClickAction = new Action() {
			public void run() {
				try {

					IMethod methodSelected = null;

					IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();

					ValidMove methodTarget = (ValidMove) selection.getFirstElement();

					IMethod[] methods;

					methods = SampleHandler.projectOriginal
							.findType(methodTarget.getMethod().getDeclaringType().getFullyQualifiedName()).getMethods();

					for (IMethod method : methods) {

						if (method.getElementName().compareTo(methodTarget.getMethod().getElementName()) == 0) {
							if (method.getNumberOfParameters() == methodTarget.getMethod().getNumberOfParameters()) {
								String[] parametersMethod = method.getParameterTypes();
								String[] parametersMethodTarget = methodTarget.getMethod().getParameterTypes();
								boolean todosBatem = true;
								for (int i = 0; i < method.getNumberOfParameters(); i++) {
									if (parametersMethod[i].compareTo(parametersMethodTarget[i]) != 0) {
										todosBatem = false;
									}
								}

								if (todosBatem) {
									methodSelected = method;
									break;

								}

							}

						}
					}

					methodTarget.moveMethod(methodSelected);

					SampleHandler.newMethodsTargets.remove(methodTarget);

					for (ValidMove validMove : SampleHandler.newMethodsTargets) {
						if (validMove.getMethod().equals(methodSelected)) {
							SampleHandler.newMethodsTargets.remove(validMove);
						}
					}

					hideView();

					openView();

				} catch (JavaModelException e) {
					e.printStackTrace();
				}

			}
		};
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

}