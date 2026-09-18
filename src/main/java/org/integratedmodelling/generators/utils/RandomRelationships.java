package org.integratedmodelling.generators.utils;

import java.nio.charset.StandardCharsets;
import java.util.*;
import org.integratedmodelling.klab.api.data.Data;
import org.integratedmodelling.klab.api.geometry.Geometry;
import org.integratedmodelling.klab.api.knowledge.Observable;
import org.integratedmodelling.klab.api.knowledge.SemanticType;
import org.integratedmodelling.klab.api.knowledge.Urn;
import org.integratedmodelling.klab.api.knowledge.observation.Observation;
import org.integratedmodelling.klab.api.knowledge.observation.scale.Scale;
import org.integratedmodelling.klab.api.lang.ServiceCall;
import org.integratedmodelling.klab.api.scope.ContextScope;
import org.integratedmodelling.klab.api.services.RuntimeService;
import org.integratedmodelling.klab.runtime.scale.space.ShapeImpl;
import org.locationtech.jts.algorithm.ConvexHull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

/**
 * Synthetic connection generator: endpoint sampling, pair selection, and geometry share one RNG.
 */
public final class RandomRelationships {
  private RandomRelationships() {}

  public static void generate(
      Data.Builder builder,
      Geometry geometry,
      Observable observable,
      ServiceCall call,
      ContextScope scope,
      Observation source,
      Observation target) {
    if (!observable.is(SemanticType.RELATIONSHIP) || !observable.getSemantics().isCollective())
      throw new IllegalArgumentException(
          "Random relationships require a collective relationship or bond");
    double percentage = call.getParameters().get("fraction", 20.0);
    if (!Double.isFinite(percentage) || percentage < 0 || percentage > 100)
      throw new IllegalArgumentException("Endpoint fraction must be between 0 and 100");
    String shape = call.getParameters().get("shape", "lines");
    if (!Set.of("lines", "points", "polygons").contains(shape))
      throw new IllegalArgumentException("Unknown random relationship shape: " + shape);
    int vertices = call.getParameters().get("vertices", 5);
    if (vertices < 3 || vertices > 10000)
      throw new IllegalArgumentException("Polygon vertices must be between 3 and 10000");
    if (source == null || target == null)
      throw new IllegalArgumentException("Random relationships require source and target inputs");
    long seed = call.getParameters().get("seed", new Random().nextLong());
    var random = new Random(seed);
    if (percentage == 0) return;
    var runtime = scope.getService(RuntimeService.class);
    var sources = sample(runtime.getMembers(source, scope), percentage, random);
    var targets = sample(runtime.getMembers(target, scope), percentage, random);
    if (sources.isEmpty() || targets.isEmpty()) return;
    var scale = Scale.create(geometry);
    if (scale.getSpace() == null)
      throw new IllegalArgumentException(
          "Random relationship geometry requires a spatial observation context");
    var envelope = scale.getSpace().getEnvelope();
    if (!(envelope.getMaxX() > envelope.getMinX()) || !(envelope.getMaxY() > envelope.getMinY()))
      throw new IllegalArgumentException(
          "Random relationship geometry requires a non-degenerate spatial envelope");
    var individual = Observable.promote(observable.getSemantics().singular());
    var factory = new GeometryFactory();
    var pairs = new HashSet<String>();
    for (var from : sources) {
      var candidates = targets.stream().filter(to -> to.getId() != from.getId()).toList();
      if (candidates.isEmpty()) continue;
      var to = candidates.get(random.nextInt(candidates.size()));
      String first = from.getUrn(), second = to.getUrn();
      if (observable.is(SemanticType.BIDIRECTIONAL) && first.compareTo(second) > 0) {
        var swap = first;
        first = second;
        second = swap;
      }
      String pair = first + "\n" + second;
      if (!pairs.add(pair)) continue;
      int count = shape.equals("points") ? 1 : shape.equals("lines") ? 3 : vertices;
      var coordinates = new Coordinate[count];
      for (int i = 0; i < count; i++)
        coordinates[i] =
            new Coordinate(
                envelope.getMinX()
                    + random.nextDouble() * (envelope.getMaxX() - envelope.getMinX()),
                envelope.getMinY()
                    + random.nextDouble() * (envelope.getMaxY() - envelope.getMinY()));
      var jts =
          switch (shape) {
            case "points" -> factory.createPoint(coordinates[0]);
            case "lines" -> factory.createLineString(coordinates);
            default -> new ConvexHull(coordinates, factory).getConvexHull();
          };
      var generated =
          scale.with(ShapeImpl.create(jts, envelope.getProjection())).as(Geometry.class);
      var identity =
          UUID.nameUUIDFromBytes(
              (observable.getUrn() + "\n" + seed + "\n" + pair).getBytes(StandardCharsets.UTF_8));
      builder.relationship(
          "relationship_" + pairs.size(),
          individual,
          generated,
          Urn.of("random.connections:" + identity),
          from,
          to);
    }
  }

  static List<Observation> sample(List<Observation> pool, double percentage, Random random) {
    var unique = new TreeMap<String, Observation>();
    for (var member : pool) {
      if (member == null
          || member.isEmpty()
          || member.getObservable().getSemantics().isCollective()
          || !SemanticType.isEnumerableSubstantial(member.getObservable().getSemantics().getType()))
        throw new IllegalArgumentException("Endpoint inputs must contain individual substantials");
      unique.put(member.getUrn(), member);
    }
    var shuffled = new ArrayList<>(unique.values());
    Collections.shuffle(shuffled, random);
    return new ArrayList<>(
        shuffled.subList(0, (int) Math.round(shuffled.size() * percentage / 100.0)));
  }
}
