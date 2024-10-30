package org.gradle.api.experimental.jvm;

import org.gradle.api.Plugin;
import org.gradle.api.experimental.buildinit.StaticProjectGenerator;
import org.gradle.api.experimental.buildinit.StaticProjectSpec;
import org.gradle.api.experimental.java.StandaloneJavaApplicationPlugin;
import org.gradle.api.experimental.java.StandaloneJavaLibraryPlugin;
import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.plugins.software.RegistersSoftwareTypes;
import org.gradle.buildinit.specs.internal.BuildInitSpecRegistry;

import javax.inject.Inject;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
@RegistersSoftwareTypes({
        StandaloneJavaApplicationPlugin.class,
        StandaloneJavaLibraryPlugin.class,
        StandaloneJvmLibraryPlugin.class,
        StandaloneJvmApplicationPlugin.class
})
public abstract class JvmEcosystemPlugin implements Plugin<Settings> {
    @Override
    public void apply(Settings target) {
        getBuildInitSpecRegistry().register(StaticProjectGenerator.class, List.of(
                new StaticProjectSpec("java-application", "Declarative Java Application Project")
        ));
        target.getPlugins().apply(JvmEcosystemConventionsPlugin.class);
    }

    @Inject
    protected abstract BuildInitSpecRegistry getBuildInitSpecRegistry();
}
