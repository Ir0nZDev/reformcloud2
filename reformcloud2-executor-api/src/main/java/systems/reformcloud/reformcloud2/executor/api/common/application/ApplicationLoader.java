package systems.reformcloud.reformcloud2.executor.api.common.application;

import systems.reformcloud.reformcloud2.executor.api.common.application.api.Application;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Represents a loader for applications
 */
public interface ApplicationLoader {

    /**
     * Detects all applications
     */
    void detectApplications();

    /**
     * Installs all applications
     */
    void installApplications();

    /**
     * Loads all applications
     */
    void loadApplications();

    /**
     * Enables all applications
     */
    void enableApplications();

    /**
     * Disables all applications
     */
    void disableApplications();

    /**
     * Fetches all updates for all applications &amp; downloads them
     */
    void fetchAllUpdates();

    /**
     * Fetches the updates for a specific addon
     *
     * @param application The name of the application which should get checked
     */
    void fetchUpdates(@Nonnull String application);

    /**
     * Installs an specific application
     *
     * @param application The application which should get installed
     * @return If the cloud can find the application and install it {@code true} else {@code false}
     */
    boolean doSpecificApplicationInstall(@Nonnull InstallableApplication application);

    /**
     * Unloads a specific application
     *
     * @param loadedApplication The application which should get unloaded
     * @return If the application was loaded and got unloaded
     */
    boolean doSpecificApplicationUninstall(@Nonnull LoadedApplication loadedApplication);

    /**
     * Finds the {@link LoadedApplication} by the name and unloads it
     *
     * @param application The name oof the application which should get unloaded
     * @return If the application was loaded and got unloaded
     * @see #getApplication(String)
     * @see #doSpecificApplicationUninstall(LoadedApplication)
     */
    boolean doSpecificApplicationUninstall(@Nonnull String application);

    /**
     * Get a specific application
     *
     * @param name The name of the application
     * @return The loaded application or {@code null} if the application is unknown
     */
    @Nullable
    LoadedApplication getApplication(@Nonnull String name);

    /**
     * The name of a loaded application
     *
     * @param loadedApplication The application from which the name is needed
     * @return The name of the application
     * @see LoadedApplication#getName()
     */
    @Nonnull
    String getApplicationName(@Nonnull LoadedApplication loadedApplication);

    /**
     * @return All currently loaded applications in the runtime
     */
    @Nonnull
    List<LoadedApplication> getApplications();

    /**
     * Registers an {@link ApplicationHandler}
     *
     * @param applicationHandler The handler which should get registered
     */
    void addApplicationHandler(@Nonnull ApplicationHandler applicationHandler);

    @Nullable
    Application getInternalApplication(@Nonnull String name);
}
