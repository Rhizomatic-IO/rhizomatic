package io.rhizomatic.gradle.assembly;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

/**
 * Assembles a Rhizomatic runtime image.
 */
@SuppressWarnings("unused")
public class RhizomaticAssemblyPlugin implements Plugin<Project> {

    public void apply(Project project) {
        var javaTasks = project.getTasksByName("compileJava", false);
        if (javaTasks.isEmpty()) {
            throw new GradleException("Java task not found for project");
        }

        var task = project.getTasks().create("rhizomaticAssembly", AssembleTask.class);
        task.setDescription("Assembles a Rhizomatic runtime image");
        task.setGroup(LifecycleBasePlugin.BUILD_GROUP);

        for (var javaTask : javaTasks) {
            System.out.println(javaTask.getName());
        }
        task.dependsOn(javaTasks.iterator().next());
    }

}

