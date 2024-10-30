package org.gradle.api.experimental.android;

import org.gradle.api.Plugin;
import org.gradle.api.experimental.android.application.StandaloneAndroidApplicationPlugin;
import org.gradle.api.experimental.android.library.StandaloneAndroidLibraryPlugin;
import org.gradle.api.experimental.buildinit.StaticProjectGenerator;
import org.gradle.api.experimental.buildinit.StaticProjectSpec;
import org.gradle.api.experimental.jvm.JvmEcosystemConventionsPlugin;
import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.plugins.software.RegistersSoftwareTypes;
import org.gradle.buildinit.specs.internal.BuildInitSpecRegistry;

import javax.inject.Inject;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
@RegistersSoftwareTypes({StandaloneAndroidApplicationPlugin.class, StandaloneAndroidLibraryPlugin.class})
public abstract class AndroidEcosystemPlugin implements Plugin<Settings> {
    @Override
    public void apply(Settings target) {
        target.getPlugins().apply("org.gradle.experimental.android-ecosystem-init");
        target.getPlugins().apply(JvmEcosystemConventionsPlugin.class);
        target.getDependencyResolutionManagement().getRepositories().google();
    }

    @Inject
    protected abstract BuildInitSpecRegistry getBuildInitSpecRegistry();
}
