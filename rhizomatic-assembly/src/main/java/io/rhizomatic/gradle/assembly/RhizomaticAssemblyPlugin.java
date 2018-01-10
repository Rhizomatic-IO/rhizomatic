package io.rhizomatic.gradle.assembly;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Assembles a Rhizomatic runtime image.
 */
@SuppressWarnings("unused")
public class RhizomaticAssemblyPlugin implements Plugin<Project> {

    public void apply(@NotNull Project project) {
        Set<Task> javaTasks = project.getTasksByName("compileJava", false);
        if (javaTasks.isEmpty()) {
            throw new GradleException("Java task not found for project");
        }

        AssembleTask task = project.getTasks().create("rhizomaticAssembly", AssembleTask.class);
        task.setDescription("Assembles a Rhizomatic runtime image");
        task.setGroup(LifecycleBasePlugin.BUILD_GROUP);

        task.dependsOn(javaTasks.iterator().next());
    }

}

