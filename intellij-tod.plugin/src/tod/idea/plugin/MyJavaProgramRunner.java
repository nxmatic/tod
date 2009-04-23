package tod.idea.plugin;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.jetbrains.annotations.NotNull;

import tod.core.config.TODConfig;
import tod.core.database.structure.SourceRange;
import tod.core.session.ISession;
import tod.core.session.SessionUtils;
import tod.core.session.TODSessionManager;
import tod.gui.MinerUI;
import tod.gui.SwingDialogUtils;
import zz.utils.properties.IProperty;
import zz.utils.properties.PropertyListener;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.ConfigurationInfoProvider;
import com.intellij.execution.configurations.ConfigurationPerRunnerSettings;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.runners.JavaProgramRunner;
import com.intellij.execution.runners.RunnerInfo;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.peer.PeerFactory;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.content.Content;

/**
 * @author Rodolfo Toledo
 */
public class MyJavaProgramRunner implements JavaProgramRunner
{

    private final Application itsApplication;

    private ToolWindow itsToolWindow;

    MinerUI theMinerUI;

    public MyJavaProgramRunner(Application aProject)
    {
        itsApplication = aProject;
    }

    public JDOMExternalizable createConfigurationData(ConfigurationInfoProvider settingsProvider)
    {
        //JDOMExternalizable data = defaultRunner.createConfigurationData(settingsProvider);
        //System.out.println("MyJavaProgramRunner.createConfigurationData: " + data);
        //return data;
        return null;
    }

    public void patch(JavaParameters javaParameters, final RunnerSettings settings, boolean beforeExecution) throws ExecutionException
    {
        System.out.println("TODApplicationComponent$MyJavaProgramRunner.patch: " + beforeExecution);

        ParametersList vmParametersList = javaParameters.getVMParametersList();
        vmParametersList.add("-noverify");
        vmParametersList.add("-Xbootclasspath/p:E:\\Proyectos\\TOD-Plugin\\tod\\TOD\\lib\\tod-agent.jar");
        vmParametersList.add("-Dagent-cache-path=E:\\tmp\\");
        vmParametersList.add("-Dagent-verbose=3");
        vmParametersList.add("-Dcollector-host=localhost");
        vmParametersList.add("-Dcollector-port=8058");
        vmParametersList.add("-agentpath:E:/Proyectos/TOD-Plugin/bci-agent.dll");

        //DBProcessManager.getDefault().start();
        theMinerUI = new MinerUI()
        {

        	{
        		TODSessionManager.getInstance().pCurrentSession().addHardListener(new PropertyListener<ISession>()
        				{
        					public void propertyChanged(
        							IProperty<ISession> aProperty, 
        							ISession aOldValue, 
        							final ISession aNewValue)
        					{
        						SwingUtilities.invokeLater(new Runnable()
        						{
        							public void run()
        							{
        								setSession(aNewValue);
        							}
        						});
        					}
        				});

        		setSession(TODSessionManager.getInstance().pCurrentSession().get());
        	}
        	
            public void gotoSource(SourceRange aSourceRange)
            {
                navigate(aSourceRange.sourceFile, aSourceRange.startLine,
                         settings.getRunProfile().getModules()[0].getProject());
            }
            
    		public <T> T showDialog(DialogType<T> aDialog)
    		{
    			return SwingDialogUtils.showDialog(this, aDialog);
    		}

        };
        
        TODConfig theConfig = new TODConfig(); //TODO: retrieve TOD config from launch configuration
        IntelliJProgramLaunch theLaunch = new IntelliJProgramLaunch(); // TODO: see if information is needed 
        TODSessionManager.getInstance().getSession(theMinerUI, theConfig, theLaunch);

    }

    public void checkConfiguration(RunnerSettings settings, ConfigurationPerRunnerSettings perRunnerSettings)
            throws RuntimeConfigurationException
    {
        System.out.println("MyJavaProgramRunner.checkConfiguration");
    }

    public void onProcessStarted(RunnerSettings settings, ExecutionResult executionResult)
    {
        System.out.println("TODApplicationComponent$MyJavaProgramRunner.onProcessStarted");
        if (itsToolWindow == null)
        {
            final Project theProject = settings.getRunProfile().getModules()[0].getProject();
            ToolWindowManager theManager = ToolWindowManager.getInstance(theProject);
            itsToolWindow = theManager.registerToolWindow("TOD Navigator", true, ToolWindowAnchor.BOTTOM);
            PeerFactory theFactory = PeerFactory.getInstance();

            Content theContent = theFactory.getContentFactory().createContent(theMinerUI, "", false);
            itsToolWindow.getContentManager().addContent(theContent);
            itsToolWindow.show(null);

            navigate("java.lang.Object", 15, theProject);
        }
    }

    private void navigate(String aClassName, int lineNo, Project aProject)
    {
        GlobalSearchScope scope = GlobalSearchScope.allScope(aProject);
        PsiClass theClass = PsiManager.getInstance(aProject).findClass(aClassName, scope);

        if (theClass == null)
        {
            JOptionPane.showMessageDialog(null, "Could not find " + aClassName, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        theClass = (PsiClass) theClass.getNavigationElement();
        if (theClass.canNavigateToSource())
        {
            theClass.navigate(true);

            FileEditorManager fileEditorManager = FileEditorManager.getInstance(aProject);
            Editor editor = fileEditorManager.getSelectedTextEditor();
            if (editor != null)
            {
                Document theDocument = editor.getDocument();
                int offsetStart = theDocument.getLineStartOffset(lineNo - 1);
                //int offsetEnd = editor.getDocument().getLineEndOffset(dest.lineNo);
                editor.getCaretModel().moveToOffset(offsetStart);
                //editor.getSelectionModel().setSelection(offsetStart, offsetEnd);
            }
        }
        else
        {
            JOptionPane.showMessageDialog(null, "can not navigate " + aClassName, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public AnAction[] createActions(ExecutionResult executionResult)
    {
        return new AnAction[]{
        };
        /*AnAction[] actions = defaultRunner.createActions(executionResult);
        for(AnAction action : actions){
            System.out.println("action = " + action);
        }
        return actions;*/
    }

    public RunnerInfo getInfo()
    {
        return new RunnerInfo("TOD", "Debug selected configuration using TOD",
                              IconLoader.getIcon("/general/add.png"), IconLoader.getIcon("/general/add.png"),
                              "TOD", "tod-help-id");
    }

    public SettingsEditor getSettingsEditor(RunConfiguration configuration)
    {
        return new MySettingsEditor();
    }

    private static class MySettingsEditor extends SettingsEditor
    {

        private JPanel panel;

        public MySettingsEditor()
        {
            super();
            setUp();
        }

        private void setUp()
        {
            panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.add(new JLabel("skjdlaks"));

            /*
            TODConfig.Item[] items = TODConfig.ITEMS;
            for(TODConfig.Item item : items){
                if(item instanceof TODConfig.BooleanItem){
                    panel.add(new JCheckBox(item.getName()));
                }
            }
            */
        }

        protected void resetEditorFrom(Object s)
        {
            System.out.println("MyJavaProgramRunner$MySettingsEditor.resetEditorFrom: " + s);
        }

        protected void applyEditorTo(Object s) throws ConfigurationException
        {
            System.out.println("MyJavaProgramRunner$MySettingsEditor.applyEditorTo: " + s);
        }

        @NotNull
        @Override
        public JComponent createEditor()
        {
            System.out.println("MyJavaProgramRunner$MySettingsEditor.createEditor");
            return panel;
        }

        protected void disposeEditor()
        {
        }
    }

}