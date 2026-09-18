package org.integratedmodelling.generators.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.integratedmodelling.common.knowledge.ConceptImpl;
import org.integratedmodelling.common.knowledge.ObservableImpl;
import org.integratedmodelling.common.lang.ServiceCallImpl;
import org.integratedmodelling.klab.api.data.Data;
import org.integratedmodelling.klab.api.geometry.Geometry;
import org.integratedmodelling.klab.api.knowledge.SemanticType;
import org.integratedmodelling.klab.api.knowledge.observation.Observation;
import org.integratedmodelling.klab.api.knowledge.observation.impl.ObservationImpl;
import org.integratedmodelling.klab.api.knowledge.observation.scale.Scale;
import org.integratedmodelling.klab.api.scope.ContextScope;
import org.integratedmodelling.klab.api.services.RuntimeService;
import org.integratedmodelling.klab.configuration.ServiceConfiguration;
import org.junit.jupiter.api.*;

class RandomRelationshipsTest {
  @BeforeAll static void setup() { ServiceConfiguration.injectInstantiators(); }

  record Emitted(long source, long target, String geometry, String identity) {}

  static ObservationImpl observation(long id, boolean relationship, boolean collective, boolean bond) {
    var concept = new ConceptImpl(); concept.setUrn(relationship ? "test:Link" : "test:Endpoint");
    concept.setName(relationship ? "Link" : "Endpoint"); concept.setReferenceName(relationship ? "link" : "endpoint");
    var types = EnumSet.of(SemanticType.COUNTABLE, relationship ? SemanticType.RELATIONSHIP : SemanticType.SUBJECT);
    if (bond) types.add(SemanticType.BIDIRECTIONAL);
    concept.setType(types); concept.setCollective(collective);
    var observable = new ObservableImpl(); observable.setSemantics(concept); observable.setUrn(concept.getUrn());
    var observation = new ObservationImpl(); observation.setId(id); observation.setUrn("test:instance" + id);
    observation.setObservable(observable); return observation;
  }

  private List<Emitted> generate(double percentage, long seed, String shape, boolean bond, boolean overlapping) {
    var source = observation(-1, false, true, false);
    var target = observation(-2, false, true, false);
    var relationship = observation(-3, true, true, bond);
    var sources = new ArrayList<Observation>(); var targets = new ArrayList<Observation>();
    for (int i = 1; i <= 10; i++) {
      sources.add(observation(i, false, false, false));
      targets.add(observation(overlapping ? i : i + 10, false, false, false));
    }
    var scope = mock(ContextScope.class); var runtime = mock(RuntimeService.class);
    when(scope.getService(RuntimeService.class)).thenReturn(runtime);
    when(runtime.getMembers(source, scope)).thenReturn(sources);
    when(runtime.getMembers(target, scope)).thenReturn(targets);
    var call = new ServiceCallImpl();
    call.getParameters().put("percentage", percentage); call.getParameters().put("seed", seed);
    call.getParameters().put("shape", shape);
    Geometry geometry = Scale.create(org.integratedmodelling.klab.runtime.scale.space.ShapeImpl.create(
        "EPSG:4326 POLYGON ((-5 40, 5 40, 5 45, -5 45, -5 40))"));
    var builder = mock(Data.Builder.class);
    var emitted = new ArrayList<Emitted>();
    when(builder.relationship(anyString(), any(), any(), any(), any(), any())).thenAnswer(args -> {
      Observation from = args.getArgument(4), to = args.getArgument(5);
      Geometry generated = args.getArgument(2);
      var space = Scale.create(generated).getSpace();
      assertNotNull(space); assertFalse(space.isEmpty());
      assertTrue(space.getEnvelope().getMinX() >= -5 && space.getEnvelope().getMaxX() <= 5);
      assertTrue(space.getEnvelope().getMinY() >= 40 && space.getEnvelope().getMaxY() <= 45);
      assertFalse(((org.integratedmodelling.klab.api.knowledge.Observable) args.getArgument(1)).getSemantics().isCollective());
      emitted.add(new Emitted(from.getId(), to.getId(), generated.encode(), args.getArgument(3).toString()));
      return builder;
    });
    RandomRelationships.generate(builder, geometry, relationship.getObservable(), call, scope, source, target);
    return emitted;
  }

  @Test void percentagesSelectTheExpectedNumberOfSources() {
    assertEquals(0, generate(0, 42, "lines", false, false).size());
    assertEquals(3, generate(30, 42, "lines", false, false).size());
    assertEquals(10, generate(100, 42, "lines", false, false).size());
  }

  @Test void seedsReproducePairsAndGeometryAcrossAllShapeKinds() {
    for (var shape : List.of("points", "lines", "polygons")) {
      assertEquals(generate(50, 42, shape, false, false), generate(50, 42, shape, false, false));
      assertNotEquals(generate(50, 42, shape, false, false), generate(50, 43, shape, false, false));
    }
  }

  @Test void overlappingBondInputsProduceNeitherSelfLinksNorReversedDuplicates() {
    var pairs = new HashSet<Set<Long>>();
    for (var result : generate(100, 42, "lines", true, true)) {
      assertNotEquals(result.source(), result.target());
      assertTrue(pairs.add(Set.of(result.source(), result.target())));
    }
  }

  @Test void invalidPercentagesAreRejected() {
    for (double percentage : new double[] {-1, 101, Double.NaN, Double.POSITIVE_INFINITY})
      assertThrows(IllegalArgumentException.class, () -> generate(percentage, 42, "lines", false, false));
  }

  @Test void samplingDeduplicatesAndRoundsSmallPools() {
    var endpoint = observation(1, false, false, false);
    assertEquals(1, RandomRelationships.sample(List.of(endpoint, endpoint), 100, new Random(42)).size());
    assertEquals(0, RandomRelationships.sample(List.of(endpoint), 20, new Random(42)).size());
  }
}
