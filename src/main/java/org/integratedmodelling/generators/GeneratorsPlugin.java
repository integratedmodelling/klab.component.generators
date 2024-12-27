package org.integratedmodelling.generators;

import org.integratedmodelling.klab.extension.KlabComponent;
import org.pf4j.PluginWrapper;

public class GeneratorsPlugin extends KlabComponent {

    /**
     * Constructor to be used by plugin manager for plugin instantiation. In k.LAB the plugin class is
     * mandatory: plugins must extend {@link KlabComponent} and provide a constructor with this exact
     * signature to be successfully loaded by the manager.
     *
     * @param wrapper
     */
    public GeneratorsPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }
}
