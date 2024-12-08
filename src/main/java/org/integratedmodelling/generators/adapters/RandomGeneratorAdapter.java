package org.integratedmodelling.generators.adapters;

import org.integratedmodelling.klab.api.data.Version;
import org.integratedmodelling.klab.api.knowledge.Artifact;
import org.integratedmodelling.klab.api.knowledge.Resource;
import org.integratedmodelling.klab.api.knowledge.Urn;
import org.integratedmodelling.klab.api.services.resources.adapters.ResourceAdapter;

@ResourceAdapter(name = "random", universal = true, version = Version.CURRENT)
public class RandomGeneratorAdapter {

    @ResourceAdapter.Encoder
    public void encode(Urn urn, Resource.Builder builder) {
        switch (urn.getNamespace()) {
            default -> builder.addError("Cannot establish what to do with " + urn);
        }
    }

    /**
     * If there's no type param in the resource adapter annotation. This may take a URN
     * and/or a full Resource according to what is needed to establish the type.
     * @param resourceUrn
     * @return
     */
    @ResourceAdapter.Type
    public Artifact.Type getType(Urn resourceUrn) {
        // TODO objects
        return Artifact.Type.NUMBER;
    }


}
