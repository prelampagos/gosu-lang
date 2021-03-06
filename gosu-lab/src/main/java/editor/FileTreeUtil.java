package editor;

import java.util.Arrays;
import javax.swing.tree.TreeModel;
import java.io.File;

/**
 */
public class FileTreeUtil
{
  public static FileTree find( String fqn )
  {
    TreeModel model = getExperimentView().getTree().getModel();
    FileTree root = (FileTree)model.getRoot();
    return root.find( fqn );
  }

  public static FileTree find( File file )
  {
    return FileTreeUtil.getRoot().find( file );
  }

  public static ExperimentView getExperimentView()
  {
    return getGosuPanel() == null ? null : getGosuPanel().getExperimentView();
  }

  public static GosuPanel getGosuPanel()
  {
    return LabFrame.instance().getGosuPanel();
  }

  public static FileTree getRoot()
  {
    ExperimentView experimentView = getExperimentView();
    if( experimentView == null )
    {
      return null;
    }
    TreeModel model = experimentView.getTree().getModel();
    return (FileTree)model.getRoot();
  }

  public static FileTree makeExternalFileTree( File file, String fqn )
  {
    return new ExternalFileTree( file, fqn );
  }

  public static boolean isSupportedTextFile( FileTree ft )
  {
    String[] binaryExt = { ".jar", ".zip", ".tar", ".gz", ".hprof", ".png", ".gif", ".jpg", ".bmp", ".exe", ".dll", ".so",  };
    String fileName = ft.getFileOrDir().getName().toLowerCase();
    return !Arrays.stream( binaryExt ).anyMatch( fileName::endsWith );
  }
}
