package io.rhizomatic.gradle.assembly;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.JavaCompile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static io.rhizomatic.gradle.assembly.IOHelper.cleanDirectory;
import static io.rhizomatic.gradle.assembly.IOHelper.copyDirectory;
import static io.rhizomatic.gradle.assembly.IOHelper.copyFile;
import static java.util.Arrays.stream;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * Assembles the Rhizomatic runtime image.
 */
public class AssembleTask extends DefaultTask {
    public static final String RHIZOMATIC_BOOTSTRAP_APP = "rhizomatic-bootstrap-app";
    public static final String RHIZOMATIC_GROUP = "io.rhizomatic";
    private String appGroup = "";  // the group name for application modules
    private boolean appCopy = true; // true if application modules should be copied
    private String bootstrapModule; // the bootstrap module name (the bootstrap module is determined using the appGroup and bootstrapModule values.
    private String bootstrapName;  // the file name to copy the bootstrap module to (exclusive of its extension)
    // the files to copy the webapp dir. They may be in the form key:value, in which case the key will be the module name and the  value will be the webapp context name; otherwise
    // the context name will be derived from the module name
    private String[] _webapps = new String[0];
    private boolean includeSourceDir = true;  // true if the src directories of the project containing the plugin configuration should be included
    private boolean useArchives = false;  // true if the app module archives are used instead of exploded format
    private boolean reload = false;
    private String[] _patchModules = new String[0];

    @Input
    public String getAppGroup() {
        return appGroup;
    }

    public void setAppGroup(String appGroup) {
        this.appGroup = appGroup;
    }

    @Input
    public boolean isAppCopy() {
        return appCopy;
    }

    public void setAppCopy(boolean appCopy) {
        this.appCopy = appCopy;
    }

    @Input
    public String getBootstrapModule() {
        return bootstrapModule;
    }

    public void setBootstrapModule(String bootstrapModule) {
        this.bootstrapModule = bootstrapModule;
    }

    @Input
    public String getBootstrapName() {
        return bootstrapName;
    }

    public void setBootstrapName(String bootstrapName) {
        this.bootstrapName = bootstrapName;
    }

    @Input
    public boolean isIncludeSourceDir() {
        return includeSourceDir;
    }

    public void setIncludeSourceDir(boolean includeSourceDir) {
        this.includeSourceDir = includeSourceDir;
    }

    @Input
    public boolean isUseArchives() {
        return useArchives;
    }

    public void setUseArchives(boolean useArchives) {
        this.useArchives = useArchives;
    }

    @Input
    public boolean isReload() {
        return reload;
    }

    public void setReload(boolean reload) {
        this.reload = reload;
    }

    @Input
    public String[] getPatchModules() {
        return _patchModules;
    }

    public void setPatchModules(String[] patchModules) {
        this._patchModules = (String[]) patchModules;
    }

    @Input
    public String[] getWebapps() {
        return _webapps;
    }

    public void setWebapps(String[] webapps) {
        this._webapps = webapps;
    }

    @TaskAction
    public void assemble() {
        var logger = Logging.getLogger("rhizomatic-assembly");

        var project = getProject();

        var transitiveDependencies = resolveDependencies(project);

        logger.info("Assembling Rhizomatic runtime image");

        createRuntimeImage(project, transitiveDependencies);
    }

    /**
     * Transitively resolves all runtime dependencies using breadth-first traversal. BFS will use the version numbers of dependencies that are "closest" to the root (first level)
     * dependencies if there are transitive duplicates.
     */
    private Map<String, ResolvedDependency> resolveDependencies(Project project) {
        var configuration = project.getConfigurations().getByName("runtimeClasspath");

        var resolvedConfiguration = configuration.getResolvedConfiguration();
        var directDependencies = new HashMap<String, ResolvedDependency>();

        // resolve breadth-first
        resolvedConfiguration.getFirstLevelModuleDependencies().forEach(d -> directDependencies.put(getKey(d), d));

        var transitiveDependencies = new HashMap<>(directDependencies);
        directDependencies.values().forEach(d -> calculateTransitive(d, transitiveDependencies));
        return transitiveDependencies;
    }

    /**
     * Recursively calculates transitive dependencies.
     */
    private void calculateTransitive(ResolvedDependency dependency, Map<String, ResolvedDependency> dependencies) {
        // breadth-first search guarantees nearest transitive dependencies are used
        var currentLevel =
                dependency.getChildren().stream().filter(child -> !dependencies.containsKey(getKey(child))).collect(toMap(this::getKey, identity()));
        dependencies.putAll(currentLevel);
        currentLevel.values().forEach(child -> calculateTransitive(child, dependencies));
    }

    /**
     * Creates the runtime image from the list of transitive dependencies. The dependencies are composed of app modules, library modules, Rhizomatic system modules, and and a
     * bootstrap module.
     */
    private void createRuntimeImage(Project project, Map<String, ResolvedDependency> transitiveDependencies) {
        if (getAppGroup().trim().length() == 0) {
            Logging.getLogger("rhizomatic-assembly").info("No application module group specified. Application modules will not be copied.");
        }
        var imageDir = new File(project.getBuildDir(), "image");
        if (imageDir.exists()) {
            // remove previously created image
            cleanDirectory(imageDir);
        }

        var systemDir = new File(imageDir, "system");
        systemDir.mkdirs();

        var librariesDir = new File(imageDir, "libraries");
        librariesDir.mkdirs();

        var appDir = new File(imageDir, "app");
        appDir.mkdirs();

        var webappDir = new File(imageDir, "webapp");
        var webappNames = new HashMap<String, String>();
        for (var webapp : _webapps) {
            if (webapp.contains(":")) {
                var tokens = webapp.split(":");  // context name is specified after the ':'
                webappNames.put(tokens[0], tokens[1]);
            } else {
                webappNames.put(webapp, webapp); // context and module name are the same
            }
        }
        if (!webappNames.isEmpty()) {
            webappDir.mkdirs();
        }

        var patchLibrariesDir = new File(imageDir, "plibraries");
        patchLibrariesDir.mkdirs();

        var configuration = project.getConfigurations().getByName("compileClasspath");
        var projectDependencies = new HashMap<String, ProjectDependency>();

        configuration.getAllDependencies().forEach(dependency -> {
            if (dependency instanceof ProjectDependency) {
                if (RHIZOMATIC_GROUP.equals(dependency.getGroup())) {
                    // do not include dependencies
                    return;
                }
                projectDependencies.put(dependency.getGroup() + ":" + dependency.getName(), (ProjectDependency) dependency);
            }
        });
        // Copy runtime image: Rhizomatic modules to /system, application modules to /app; otherwise to /libraries
        for (var dependency : transitiveDependencies.values()) {
            if (RHIZOMATIC_GROUP.equals(dependency.getModuleGroup())) {
                // Rhizomatic module
                if (RHIZOMATIC_BOOTSTRAP_APP.equals(dependency.getModuleName())) {
                    // use RZ boot app
                    var name = getBootstrapName();
                    for (var artifact : dependency.getModuleArtifacts()) {
                        if (name == null) {
                            name = artifact.getFile().getName();
                        } else {
                            name = name + ".jar";
                        }
                        copyFile(artifact.getFile(), new File(imageDir, name));
                    }

                } else {
                    copy(dependency, systemDir);
                }
            } else if (getAppGroup().equals(dependency.getModuleGroup())) {
                if (!appCopy) {
                    continue;
                    // only copy app modules if enabled
                }
                if (getBootstrapModule() != null && getBootstrapModule().equals(dependency.getModuleName())) {
                    // bootstrap module
                    for (var artifact : dependency.getModuleArtifacts()) {
                        File target;
                        var name = getBootstrapName();
                        if (name == null) {
                            target = new File(imageDir, artifact.getFile().getName());
                        } else {
                            target = name.endsWith(".jar") ? new File(imageDir, name) : new File(imageDir, name + ".jar");
                        }
                        copyFile(artifact.getFile(), target);
                    }
                } else if (webappNames.containsKey(dependency.getModuleName())) {
                    // Copy web app dist directory - only support exploded format
                    var projectDependency = projectDependencies.get(getKey(dependency));
                    var sourceDistDir = new File(projectDependency.getDependencyProject().getProjectDir(), "dist");
                    var destDir = new File(webappDir, webappNames.get(dependency.getModuleName()));
                    copyDirectory(sourceDistDir, destDir);
                } else {
                    // app module
                    if (useArchives) {
                        // using archives, copy them
                        copy(dependency, appDir);
                    } else {
                        // using exploded format, get the other subproject and copy its compiled output
                        var projectDependency = projectDependencies.get(getKey(dependency));
                        if (projectDependency != null) {
                            Project dependencyProject = projectDependency.getDependencyProject();
                            var compileTask = (JavaCompile) dependencyProject.getTasks().getByName("compileJava");
                            var classesDir = compileTask.getDestinationDir();

                            var buildDir = dependencyProject.getBuildDir();
                            var resourcesDir = new File(buildDir, "resources" + File.separator + "main");
                            if (classesDir.exists()) {
                                copyDirectory(classesDir, new File(appDir, dependencyProject.getName()));
                            }
                            if (resourcesDir.exists()) {
                                copyDirectory(resourcesDir, new File(appDir, dependencyProject.getName()));
                            }
                        }
                    }
                }
            } else {
                if (stream(_patchModules).anyMatch(name -> dependency.getModuleName().equals(name))) {
                    // patch library module
                    copy(dependency, patchLibrariesDir);
                } else {
                    // library module
                    copy(dependency, librariesDir);
                }
            }
        }

        // copy src/main/resources contents if configured
        if (isIncludeSourceDir()) {
            var projectDir = project.getProjectDir();
            var resourcesDir = new File(projectDir, "src" + File.separator + "main" + File.separator + "resources");
            if (resourcesDir.isDirectory()) {
                var files = resourcesDir.listFiles();
                if (files != null) {
                    for (var entry : files) {
                        if (entry.isFile()) {
                            copyFile(entry, new File(imageDir, entry.getName()));
                        } else {
                            copyDirectory(entry, new File(imageDir, entry.getName()));
                        }
                    }
                }
            }
        }
    }

    private void copy(ResolvedDependency dependency, File target) {
        for (var artifact : dependency.getModuleArtifacts()) {
            copyFile(artifact.getFile(), new File(target, artifact.getFile().getName()));
        }
    }

    private String getKey(ResolvedDependency dependency) {
        return dependency.getModuleGroup() + ":" + dependency.getModuleName();
    }

}

