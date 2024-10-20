package org.integratedmodelling.generators;

import org.integratedmodelling.klab.extension.KlabComponent;
import org.pf4j.PluginWrapper;

public class GeneratorsPlugin extends KlabComponent {
    /**
     * Constructor to be used by plugin manager for plugin instantiation. Your plugins have to provide constructor
     * with this exact signature to be successfully loaded by manager.
     *
     * @param wrapper
     */
    public GeneratorsPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }
}
