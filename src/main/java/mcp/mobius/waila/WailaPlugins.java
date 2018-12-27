package mcp.mobius.waila;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import info.tehnut.pluginloader.LoaderCreator;
import info.tehnut.pluginloader.PluginLoaderBuilder;
import info.tehnut.pluginloader.ValidationStrategy;
import mcp.mobius.waila.addons.core.PluginCore;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.impl.WailaRegistrar;
import mcp.mobius.waila.api.impl.config.PluginConfig;
import net.fabricmc.api.loader.Loader;
import net.fabricmc.loader.language.LanguageAdapter;

import java.util.List;
import java.util.Map;

public class WailaPlugins implements LoaderCreator {

    public static final Map<String, IWailaPlugin> PLUGINS = Maps.newHashMap();

    @Override
    public void createLoaders() {
        new PluginLoaderBuilder(Waila.MODID)
                .withValidator(ValidationStrategy.instanceOf(IWailaPlugin.class))
                .withInitializer((clazz, container) -> {
                    if (PLUGINS.containsKey(container.getInfo().getId()))
                        return; // No plugin overrides allowed

                    if (container.getInfo().getData() != null && container.getInfo().getData().isJsonObject()) {
                        JsonObject json = container.getInfo().getData().getAsJsonObject();
                        if (json.has("required") && !Loader.getInstance().isModLoaded(json.getAsJsonPrimitive("required").getAsString()))
                            return;
                    }

                    try {
                        IWailaPlugin plugin = (IWailaPlugin) container.getOwner().getAdapter().createInstance(clazz, new LanguageAdapter.Options());
                        PLUGINS.put(container.getInfo().getId(), plugin);
                        Waila.LOGGER.info("Discovered plugin {} at {}", container.getInfo().getId(), plugin.getClass().getCanonicalName());
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                })
                .withPostCall(() -> {
                    Waila.LOGGER.info("Registering plugin at {}", PluginCore.class.getCanonicalName());
                    PLUGINS.remove("waila_core").register(WailaRegistrar.INSTANCE); // Handle and clear the core plugin so it's registered first
                    List<IWailaPlugin> sorted = Lists.newArrayList(PLUGINS.values());
                    sorted.sort((o1, o2) -> {
                        if (o1.getClass().getCanonicalName().startsWith("mcp.mobius.waila"))
                            return -1; // Put waila plugins at the top

                        return o1.getClass().getCanonicalName().compareToIgnoreCase(o2.getClass().getCanonicalName());
                    });

                    sorted.forEach(p -> {
                        Waila.LOGGER.info("Registering plugin at {}", p.getClass().getCanonicalName());
                        p.register(WailaRegistrar.INSTANCE);
                    });
                    PluginConfig.INSTANCE.reload();
                })
                .build();
    }
}