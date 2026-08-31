package dev.iconfy.idea.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.JLabel

/**
 * Faz 0 placeholder: proves the plugin loads and the "Iconfy" tool window registers. The Compose +
 * Jewel icon browser replaces this content in Faz 2.
 */
class IconfyToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val content = ContentFactory.getInstance()
            .createContent(JLabel("  Iconfy — icon browser coming soon"), "", false)
        toolWindow.contentManager.addContent(content)
    }
}
