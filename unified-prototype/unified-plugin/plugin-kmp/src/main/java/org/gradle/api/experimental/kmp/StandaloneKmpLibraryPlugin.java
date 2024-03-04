package org.gradle.api.experimental.kmp;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.DependencyScopeConfiguration;
import org.gradle.api.experimental.common.LibraryDependencies;
import org.gradle.api.provider.Property;
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension;
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet;

/**
 * Creates a declarative {@link KmpLibrary} DSL model, applies the official KMP plugin,
 * and links the declarative model to the official plugin.
 */
public class StandaloneKmpLibraryPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        KmpLibrary dslModel = createDslModel(project);

        // Register an afterEvaluate listener before we apply the Android plugin to ensure we can
        // run actions before Android does.
        project.afterEvaluate(p -> linkDslModelToPlugin(p, dslModel));

        // Apply the official KMP plugin.
        project.getPlugins().apply("org.jetbrains.kotlin.multiplatform");

        linkDslModelToPluginLazy(project, dslModel);
    }

    private KmpLibrary createDslModel(Project project) {
        KmpLibrary dslModel = project.getExtensions().create("kmpLibrary", AbstractKmpLibrary.class);

        // In order for function extraction from the DependencyCollector on the library deps to work, configurations must exist
        // Matching the names of the getters on LibraryDependencies
        DependencyScopeConfiguration api = project.getConfigurations().dependencyScope("api").get();
        DependencyScopeConfiguration implementation = project.getConfigurations().dependencyScope("implementation").get();
        DependencyScopeConfiguration compileOnly = project.getConfigurations().dependencyScope("compileOnly").get();
        DependencyScopeConfiguration runtimeOnly = project.getConfigurations().dependencyScope("runtimeOnly").get();

        // Wire the dependency collector to these configurations
        project.getConfigurations().getByName("api").fromDependencyCollector(dslModel.getDependencies().getApi());
        project.getConfigurations().getByName("implementation").fromDependencyCollector(dslModel.getDependencies().getImplementation());
        project.getConfigurations().getByName("compileOnly").fromDependencyCollector(dslModel.getDependencies().getCompileOnly());
        project.getConfigurations().getByName("runtimeOnly").fromDependencyCollector(dslModel.getDependencies().getRuntimeOnly());

        return dslModel;
    }

    /**
     * Performs linking actions that must occur within an afterEvaluate block.
     */
    private void linkDslModelToPlugin(Project project, KmpLibrary dslModel) {
        KotlinMultiplatformExtension kotlin = project.getExtensions().getByType(KotlinMultiplatformExtension.class);

        // Link common properties
        kotlin.getSourceSets().configureEach(sourceSet -> {
            sourceSet.languageSettings(languageSettings -> {
                ifPresent(dslModel.getLanguageVersion(), languageSettings::setLanguageVersion);
                ifPresent(dslModel.getLanguageVersion(), languageSettings::setApiVersion);
            });
        });
        dslModel.getTargets().withType(KmpJsTarget.class).all(target -> {
            kotlin.js(target.getName(), kotlinTarget -> {
                if (target.getEnvironment().get() == KmpJsTarget.JsEnvironment.NODE) {
                    kotlinTarget.nodejs();
                } else {
                    kotlinTarget.browser();
                }
            });
        });
    }

    /**
     * Performs linking actions that do not need to occur within an afterEvaluate block.
     */
    private void linkDslModelToPluginLazy(Project project, KmpLibrary dslModel) {
        KotlinMultiplatformExtension kotlin = project.getExtensions().getByType(KotlinMultiplatformExtension.class);

        // Link common dependencies
        linkSourceSetToDependencies(project, kotlin.getSourceSets().getByName("commonMain"), dslModel.getDependencies());

        // Link JVM targets
        dslModel.getTargets().withType(KmpJvmTarget.class).all(target -> {
            kotlin.jvm(target.getName(), kotlinTarget -> {
                linkSourceSetToDependencies(
                    project,
                    kotlinTarget.getCompilations().getByName("main").getDefaultSourceSet(),
                    target.getDependencies()
                );
                kotlinTarget.getCompilations().configureEach(compilation -> {
                    compilation.getCompilerOptions().getOptions().getJvmTarget().set(target.getJvmTarget());
                });
            });
        });

        // Link JS targets
        dslModel.getTargets().withType(KmpJsTarget.class).all(target -> {
            kotlin.js(target.getName(), kotlinTarget -> {
                linkSourceSetToDependencies(
                    project,
                    kotlinTarget.getCompilations().getByName("main").getDefaultSourceSet(),
                    target.getDependencies()
                );
            });
        });
    }

    private static void linkSourceSetToDependencies(Project project, KotlinSourceSet sourceSet, LibraryDependencies libraryDependencies) {
        project.getConfigurations().getByName(sourceSet.getImplementationConfigurationName())
            .getDependencies().addAllLater(libraryDependencies.getImplementation().getDependencies());
        project.getConfigurations().getByName(sourceSet.getApiConfigurationName())
            .getDependencies().addAllLater(libraryDependencies.getApi().getDependencies());
        project.getConfigurations().getByName(sourceSet.getCompileOnlyConfigurationName())
            .getDependencies().addAllLater(libraryDependencies.getCompileOnly().getDependencies());
        project.getConfigurations().getByName(sourceSet.getRuntimeOnlyConfigurationName())
            .getDependencies().addAllLater(libraryDependencies.getRuntimeOnly().getDependencies());
    }

    private static <T> void ifPresent(Property<T> property, Action<T> action) {
        if (property.isPresent()) {
            action.execute(property.get());
        }
    }
}
