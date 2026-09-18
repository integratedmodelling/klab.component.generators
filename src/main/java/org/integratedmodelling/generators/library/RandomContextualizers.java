package org.integratedmodelling.generators.library;

import java.util.Random;
import org.integratedmodelling.generators.utils.RandomRelationships;
import org.integratedmodelling.klab.api.data.Data;
import org.integratedmodelling.klab.api.geometry.Geometry;
import org.integratedmodelling.klab.api.knowledge.Artifact;
import org.integratedmodelling.klab.api.knowledge.Concept;
import org.integratedmodelling.klab.api.knowledge.Observable;
import org.integratedmodelling.klab.api.knowledge.SemanticType;
import org.integratedmodelling.klab.api.knowledge.SemanticRole;
import org.integratedmodelling.klab.api.lang.ServiceCall;
import org.integratedmodelling.klab.api.knowledge.observation.Observation;
import org.integratedmodelling.klab.api.scope.ContextScope;
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
      name = "relationships",
      description =
          "Select a percentage of each endpoint input and generate random relationship or bond observations.",
      geometry = "*",
      type = Artifact.Type.RELATIONSHIP,
      parameters = {
        @KlabFunction.Argument(
            name = "percentage",
            type = Artifact.Type.NUMBER,
            optional = true,
            description = "Percentage of each endpoint pool to sample (0..100, default 20)."),
        @KlabFunction.Argument(
            name = "seed",
            type = Artifact.Type.NUMBER,
            optional = true,
            description = "Optional random seed for repeatable endpoint selection and geometry."),
        @KlabFunction.Argument(
            name = "shape",
            type = Artifact.Type.TEXT,
            optional = true,
            description = "Random geometry: lines (default), points, or polygons."),
        @KlabFunction.Argument(
            name = "vertices",
            type = Artifact.Type.NUMBER,
            optional = true,
            description = "Number of random vertices for polygon hulls (default 5, minimum 3).")
      })
  public static void generateRelationships(
      Data.Builder builder,
      Geometry geometry,
      Observable observable,
      ServiceCall call,
      ContextScope scope,
      @KlabFunction.Input(
              name = "source",
              type = {Artifact.Type.OBJECT, Artifact.Type.EVENT, Artifact.Type.RELATIONSHIP})
          Observation source,
      @KlabFunction.Input(
              name = "target",
              type = {Artifact.Type.OBJECT, Artifact.Type.EVENT, Artifact.Type.RELATIONSHIP})
          Observation target) {
    RandomRelationships.generate(builder, geometry, observable, call, scope, source, target);
  }

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
