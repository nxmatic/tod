package tod.idea.plugin;

import com.intellij.execution.ExecutionRegistry;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.components.ApplicationComponent;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import tod.impl.dbgrid.DBProcessManager;

/**
 * @author Rodolfo Toledo
 */
public class TODApplicationComponent implements ApplicationComponent /*, Configurable*/
{

    private Application application;

    private MyJavaProgramRunner runner;

    public TODApplicationComponent(Application application)
    {
        this.application = application;

        DBProcessManager.cp =
                "E:\\Proyectos\\TOD-Plugin\\tod\\TOD\\build\\tod-debugger.jar;" +
                        "E:\\Proyectos\\TOD-Plugin\\tod\\TOD-dbgrid\\build\\tod-dbgrid.jar;" +
                        "E:\\Proyectos\\TOD-Plugin\\tod\\zz.utils\\build\\zz.utils.jar;" +
                        "E:\\Proyectos\\TOD-Plugin\\tod\\TOD\\lib\\asm-all-3.1.jar;" +
                        "E:\\Proyectos\\TOD-Plugin\\tod\\TOD\\lib\\reflex-core.jar;" +
                        "E:\\Proyectos\\TOD-Plugin\\tod\\TOD\\lib\\tod-agent.jar;" +
                        "E:\\Proyectos\\TOD-Plugin\\tod\\TOD-dbgrid\\lib\\pom.jar";

        DBProcessManager.lib = "E:\\Proyectos\\TOD-Plugin\\";
    }

    @NonNls
    @NotNull
    public String getComponentName()
    {
        return "tod";
    }

    public void initComponent()
    {
        runner = new MyJavaProgramRunner(application);
        ExecutionRegistry.getInstance().registerRunner(runner);
    }

    public void disposeComponent()
    {
        ExecutionRegistry.getInstance().unregisterRunner(runner);
    }
}
