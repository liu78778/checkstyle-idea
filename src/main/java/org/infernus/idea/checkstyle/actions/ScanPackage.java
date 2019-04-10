package org.infernus.idea.checkstyle.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.toolwindow.CheckStyleToolWindowPanel;

/**
 * Action to execute a CheckStyle scan on the current module.
 */
public class ScanPackage extends BaseAction {

    @Override
    public final void actionPerformed(final AnActionEvent event) {
        try {
            final Project project = DataKeys.PROJECT.getData(event.getDataContext());
            if (project == null) {
                return;
            }

            final ToolWindow toolWindow = ToolWindowManager.getInstance(
                    project).getToolWindow(CheckStyleToolWindowPanel.ID_TOOLWINDOW);

            final VirtualFile[] directoryFiles = getDirectory(event, project);

            if (directoryFiles.length == 0) {
                setProgressText(toolWindow, "plugin.status.in-progress.no-file");
                return;
            }

            toolWindow.activate(() -> {
                try {
                    setProgressText(toolWindow, "plugin.status.in-progress.module");

                    Runnable scanAction = new ScanSourceRootsAction(project, directoryFiles,
                            getSelectedOverride(toolWindow));

                    ApplicationManager.getApplication().runReadAction(scanAction);
                } catch (Throwable e) {
                    CheckStylePlugin.processErrorAndLog("Current Package scan", e);
                }
            });

        } catch (Throwable e) {
            CheckStylePlugin.processErrorAndLog("Current Package scan", e);
        }
    }

    @Override
    public final void update(final AnActionEvent event) {
        super.update(event);

        try {
            final Presentation presentation = event.getPresentation();

            final Project project = DataKeys.PROJECT.getData(event.getDataContext());
            if (project == null) { // check if we're loading...
                presentation.setEnabled(false);
                return;
            }
        } catch (Throwable e) {
            CheckStylePlugin.processErrorAndLog("Current Package button update", e);
        }
    }

    private static VirtualFile[] getDirectory(AnActionEvent e, Project project ) {

        VirtualFile[] selectedFiles = DataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());
        VirtualFile[] directoryFiles = new VirtualFile[1];

        if (selectedFiles == null || selectedFiles.length != 1) {
            return directoryFiles;
        }
        VirtualFile directory = selectedFiles[0];
        if (!directory.isDirectory()) {
            directory = directory.getParent();
            if (directory == null) {
                return directoryFiles;
            }
            if (!directory.isDirectory()) {
                return directoryFiles;
            }
        }
        final PsiDirectory psiDirectory = PsiManager.getInstance(project).findDirectory(directory);
        if (psiDirectory == null) {
            return directoryFiles;
        }
        if (!PsiDirectoryFactory.getInstance(project).isPackage(psiDirectory)) {
            return directoryFiles;
        }

        directoryFiles[0] = directory;

        return directoryFiles;
    }
}
