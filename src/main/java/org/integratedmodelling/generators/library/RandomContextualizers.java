package org.integratedmodelling.generators.library;

import java.util.Random;
import org.integratedmodelling.klab.api.knowledge.Artifact;
import org.integratedmodelling.klab.api.knowledge.Concept;
import org.integratedmodelling.klab.api.knowledge.Observable;
import org.integratedmodelling.klab.api.knowledge.SemanticType;
import org.integratedmodelling.klab.api.knowledge.SemanticRole;
import org.integratedmodelling.klab.api.lang.ServiceCall;
import org.integratedmodelling.klab.api.scope.Scope;
import org.integratedmodelling.klab.api.services.Reasoner;
import org.integratedmodelling.klab.api.services.runtime.extension.KlabFunction;
import org.integratedmodelling.klab.api.services.runtime.extension.Library;

@Library(
    name = "klab.generators.random",
    description =
        """
                   Contextualizers that generate consistent random values for various use cases.""")
public class RandomContextualizers {

  static Random random = new Random();

  @KlabFunction(
      name = "categories",
      description =
          """
                        Generate random concepts among the concrete closure of an abstract observable""",
      geometry = "*",
      type = Artifact.Type.CONCEPT)
  public static Concept generateConcept(Observable observable, ServiceCall call, Scope scope) {

    // The runtime supplies the full directive; this classifier chooses the predicate family.
    var predicate = observable.builder(scope).without(SemanticRole.INHERENT).buildConcept();

    var concreteChildren =
        scope.getService(Reasoner.class).closure(predicate).stream()
            .filter(c -> !c.is(SemanticType.ABSTRACT))
            .toList();
    if (concreteChildren.size() == 0) {
      return null;
    }
    if (concreteChildren.size() == 1) {
      return concreteChildren.get(0);
    }

    int idx = random.nextInt(concreteChildren.size());
    return concreteChildren.get(idx);
  }
}
