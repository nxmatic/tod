import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.psi.PsiElement;

/**
 * @author Rodolfo Toledo
 */
public class TODGoTo extends AnAction
{

    @Override
    public void update(AnActionEvent theEvent)
    {
        Presentation thePresentation = theEvent.getPresentation();

        PsiElement thePsiElement = DataKeys.PSI_ELEMENT.getData(theEvent.getDataContext());
        thePresentation.setEnabled(thePsiElement != null);

        System.out.println("thePsiElement = " + thePsiElement);
    }

    public void actionPerformed(AnActionEvent anactionevent)
    {
        System.out.println("TODGoTo.actionPerformed");
    }
}
