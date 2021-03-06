package editor.util;

import editor.FileWatcher;
import editor.GosuPanel;
import editor.LabFrame;
import editor.run.FqnRunConfig;
import editor.run.ProgramRunConfigFactory;
import editor.run.ProgramRunConfigParameters;
import editor.settings.ISettings;
import editor.settings.Settings;
import gw.lang.reflect.IType;
import gw.lang.reflect.json.IJsonIO;
import editor.run.IRunConfig;
import editor.tabpane.ITab;
import editor.tabpane.TabPane;
import gw.lang.reflect.Expando;
import gw.lang.reflect.ReflectUtil;
import gw.lang.reflect.module.IProject;

import java.util.Map;
import java.util.TreeMap;
import javax.script.Bindings;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 */
public class Experiment implements IProject
{
  private String _name;
  private List<String> _sourcePath;
  private File _experimentDir;
  private List<String> _openFiles;
  private String _activeFile;
  private IRunConfig _mruRunConfig;
  private List<IRunConfig> _runConfigs;
  private ISettings _mruSettings;
  private Map<String, ISettings> _settings;
  private GosuPanel _gosuPanel;

  public Experiment( String name, File dir, GosuPanel gosuPanel )
  {
    _name = name;
    _gosuPanel = gosuPanel;
    _sourcePath = Arrays.asList( new File( dir, getRelativeGosuSourcePath() ).getAbsolutePath() );
    _experimentDir = dir;
    //noinspection ResultOfMethodCallIgnored
    _experimentDir.mkdirs();
    _openFiles = Collections.emptyList();
    _runConfigs = Collections.emptyList();
    _settings = Settings.makeDefaultSettings( this );
  }

  public Experiment( File dir, GosuPanel gosuPanel )
  {
    _name = dir.getName();
    _gosuPanel = gosuPanel;
    _sourcePath = Collections.emptyList();
    _experimentDir = dir;
    _openFiles = Collections.emptyList();
    _runConfigs = Collections.emptyList();
    _settings = Settings.makeDefaultSettings( this );
    load();
    FileWatcher.instance( this );
  }

  public String getName()
  {
    return _name;
  }

  @Override
  public Object getNativeProject()
  {
    return this;
  }

  @Override
  public boolean isDisposed()
  {
    return false;
  }

  @Override
  public boolean isHeadless()
  {
    return false;
  }

  @Override
  public boolean isShadowMode()
  {
    return false;
  }

  public void setName( String name )
  {
    _name = name;
  }

  public List<String> getSourcePath()
  {
    return _sourcePath;
  }
  public void setSourcePath( List<String> classpath )
  {
    _sourcePath = classpath;
  }

  public File getExperimentDir()
  {
    return _experimentDir;
  }
  @SuppressWarnings("UnusedDeclaration")
  public void setExperimentDir( File experimentDir )
  {
    _experimentDir = experimentDir;
  }

  public List<String> getOpenFiles()
  {
    return _openFiles;
  }

  public String getActiveFile()
  {
    return _activeFile;
  }

  public GosuPanel getGosuPanel()
  {
    return _gosuPanel;
  }

  private File getExperimentFile()
  {
    File experimentDir = getExperimentDir();
    //noinspection ResultOfMethodCallIgnored
    experimentDir.mkdirs();
    File experiment = LabFrame.findExperimentFile( experimentDir );
    if( experiment == null )
    {
      experiment = new File( experimentDir.getAbsolutePath() + File.separator + experimentDir.getName() + ".prj" );
    }
    return experiment;
  }

  public File getOrMakeExperimentFile()
  {
    File experiment = getExperimentFile();
    if( !experiment.exists() )
    {
      save();
    }
    return experiment;
  }

  public void save()
  {
    TabPane tabPane = getGosuPanel().getEditorTabPane();

    File userFile = getExperimentFile();

    Expando bindings = new Expando();

    bindings.put( "Title", "Gosu Experiment" );
    bindings.put( "Version", LabFrame.VERSION );
    bindings.put( "Name", getName() );

    if( tabPane != null )
    {
      ITab selectedTab = tabPane.getSelectedTab();
      if( selectedTab != null )
      {
        bindings.put( "ActiveTab", makeExperimentRelativePathWithSlashes( (File)tabPane.getSelectedTab().getContentPane().getClientProperty( "_file" ) ) );
        bindings.put( "Tabs", Arrays.stream( tabPane.getTabs() ).map( e -> {
          File file = (File)e.getContentPane().getClientProperty( "_file" );
          return makeExperimentRelativePathWithSlashes( file );
        } ).collect( Collectors.toList() ) );
      }
    }

    bindings.put( "SourcePath", getSourcePath().stream().map( path -> {
      String relativePath = makeExperimentRelativePathWithSlashes( new File( path ) );
      path = relativePath == null ? path : relativePath;
      //noinspection ResultOfMethodCallIgnored
      new File( path ).mkdirs();
      return path;
    } ).collect( Collectors.toList() ) );

    IJsonIO.writeList( "RunConfigs", _runConfigs, bindings );
    bindings.put( "MruRunConfig", getMruRunConfig() == null ? null : getMruRunConfig().getName() );

    IJsonIO.writeList( "Settings", new ArrayList<>( _settings.values() ), bindings );
    bindings.put( "MruSettings", getMruSettings() == null ? null : getMruSettings().getName() );

    try( FileWriter fw = new FileWriter( userFile ) )
    {
      String json = (String)ReflectUtil.invokeMethod( bindings, "toJson" );
      fw.write( json );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  public String makeExperimentRelativePath( File file )
  {
    String absExperimentDir = getExperimentDir().getAbsolutePath();
    String absFile = file.getAbsolutePath();
    if( !absFile.startsWith( absExperimentDir + File.separator ) )
    {
      return absFile;
    }
    return absFile.substring( absExperimentDir.length() + 1 );
  }
  public String makeExperimentRelativePathWithSlashes( File file )
  {
    return makeExperimentRelativePath( file ).replace( '\\', '/' );
  }

  private void load()
  {
    try
    {
      System.setProperty( "user.dir", getExperimentDir().getAbsolutePath() );
      Bindings bindings = (Bindings)ReflectUtil.getProperty( getOrMakeExperimentFile().toURI().toURL(), "JsonContent" );

      setName( (String)bindings.getOrDefault( "Name", getExperimentDir().getName() ) );

      //noinspection unchecked
      List<String> sourcePath = (List<String>)bindings.getOrDefault( "SourcePath", Collections.emptyList() );
      if( sourcePath.isEmpty() )
      {
        File srcPath = new File( getExperimentDir(), getRelativeGosuSourcePath() );
        sourcePath.add( srcPath.getAbsolutePath() );
        _sourcePath = sourcePath;
      }
      else
      {
        _sourcePath = sourcePath.stream().map( e -> new File( e ).getAbsolutePath() ).collect( Collectors.toList() );
      }

      //noinspection unchecked
      List<String> tabs = (List<String>)bindings.getOrDefault( "Tabs", Collections.emptyList() );
      List<String> openFiles = new ArrayList<>();
      for( String strTab : tabs )
      {
        File file = new File( strTab ).getAbsoluteFile();
        if( file.isFile() )
        {
          openFiles.add( file.getAbsolutePath() );
        }
      }
      _openFiles = openFiles;

      _activeFile = (String)bindings.get( "ActiveTab" );
      if( _activeFile != null && !_activeFile.isEmpty() )
      {
        _activeFile = new File( _activeFile ).getAbsolutePath();
      }

      //noinspection unchecked
      _runConfigs = IJsonIO.readList( "RunConfigs", bindings );
      _mruRunConfig = findRunConfig( rc -> rc.getName().equals( (String)bindings.get( "MruRunConfig" ) ) );

      _settings = new TreeMap<>();
      List<ISettings> settingList = IJsonIO.readList( "Settings", bindings );
      settingList.forEach( setting -> _settings.put( setting.getPath(), setting ) );
      _settings = Settings.mergeSettings( _settings, this );

      _mruSettings = findSettings( settings -> settings.getName().equals( (String)bindings.get( "MruSettings" ) ) );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  public static String getRelativeGosuSourcePath()
  {
    return "src" + File.separator + "main" + File.separator + "gosu";
  }

  public IRunConfig getOrCreateRunConfig( IType type )
  {
    String fqn = type.getName();
    IRunConfig rc = findRunConfig( runConfig -> runConfig instanceof FqnRunConfig &&
                                                ((FqnRunConfig)runConfig).getFqn().equals( fqn ) );
    return rc == null
           ? EditorUtilities.isRunnable( type )
             ? ProgramRunConfigFactory.instance().newRunConfig( makeProgramParams( type.getRelativeName(), type.getName() ) )
             : null
           : rc;
  }

  private ProgramRunConfigParameters makeProgramParams( String name, String fqn )
  {
    ProgramRunConfigParameters params = ProgramRunConfigFactory.instance().makeParameters();
    params.setName( name );
    params.setFqn( fqn );
    return params;
  }

  public IRunConfig findRunConfig( Predicate<IRunConfig> matcher )
  {
    if( matcher == null )
    {
      return null;
    }

    for( IRunConfig runConfig: _runConfigs )
    {
      if( matcher.test( runConfig ) )
      {
        return runConfig;
      }
    }
    return null;
  }

  public IRunConfig getMruRunConfig()
  {
    return _mruRunConfig;
  }
  public void setMruRunConfig( IRunConfig runConfig )
  {
    _mruRunConfig = runConfig;
  }

  public List<IRunConfig> getRunConfigs()
  {
    return _runConfigs;
  }

  public void addRunConfig( IRunConfig runConfig )
  {
    if( _runConfigs.isEmpty() )
    {
      _runConfigs = new ArrayList<>();
    }

    int index = _runConfigs.indexOf( runConfig );
    if( index >= 0 )
    {
      runConfig = _runConfigs.get( index );
      _runConfigs.remove( index );
    }
    _runConfigs.add( 0, runConfig );

    setMruRunConfig( runConfig );

    save();
  }

  public boolean removeRunConfig( IRunConfig runConfig )
  {
    if( _runConfigs.isEmpty() )
    {
      return false;
    }

    return _runConfigs.remove( runConfig );
  }

  public Map<String, ISettings> getSettings()
  {
    return _settings;
  }

  public ISettings getMruSettings()
  {
    return _mruSettings;
  }
  public void setMruSettings( ISettings settings )
  {
    _mruSettings = settings;
  }
  
  public ISettings findSettings( Predicate<ISettings> matcher )
  {
    if( matcher == null )
    {
      return null;
    }

    for( ISettings settings: _settings.values() )
    {
      if( matcher.test( settings ) )
      {
        return settings;
      }
    }
    return null;
  }  
}
