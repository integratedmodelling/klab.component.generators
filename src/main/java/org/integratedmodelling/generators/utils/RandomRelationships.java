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
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

/** Synthetic connection generator with random endpoint sampling and geometry-aware connections. */
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
    var projection = scale.getSpace().getEnvelope().getProjection();
    var individual = Observable.promote(observable.getSemantics().singular());
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
      var fromShape = endpointShape(from, projection);
      var toShape = endpointShape(to, projection);
      var jts = relationshipGeometry(fromShape.getJTSGeometry(), toShape.getJTSGeometry());
      var generated =
          scale.with(ShapeImpl.create(jts, projection)).as(Geometry.class);
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

  private static ShapeImpl endpointShape(
      Observation endpoint,
      org.integratedmodelling.klab.api.knowledge.observation.scale.space.Projection projection) {
    if (endpoint.getGeometry() == null) {
      throw new IllegalArgumentException("Relationship endpoint has no geometry: " + endpoint.getUrn());
    }
    var space = Scale.create(endpoint.getGeometry()).getSpace();
    if (!(space instanceof ShapeImpl shape) || shape.isEmpty()) {
      throw new IllegalArgumentException(
          "Relationship endpoint has no usable spatial shape: " + endpoint.getUrn());
    }
    return shape.transform(projection);
  }

  static org.locationtech.jts.geom.Geometry relationshipGeometry(
      org.locationtech.jts.geom.Geometry source,
      org.locationtech.jts.geom.Geometry target) {
    if (source == null || source.isEmpty() || target == null || target.isEmpty()) {
      throw new IllegalArgumentException("Relationship endpoints must have non-empty geometry");
    }
    if (source.getDimension() == 2 || target.getDimension() == 2) {
      return source.getFactory().createGeometryCollection(
              new org.locationtech.jts.geom.Geometry[] {source, target})
          .convexHull();
    }

    var sourceEndpoints = endpoints(source);
    var targetEndpoints = endpoints(target);
    Coordinate closestSource = null;
    Coordinate closestTarget = null;
    double closestDistance = Double.POSITIVE_INFINITY;
    for (var sourceEndpoint : sourceEndpoints) {
      for (var targetEndpoint : targetEndpoints) {
        double distance = sourceEndpoint.distance(targetEndpoint);
        if (distance < closestDistance) {
          closestDistance = distance;
          closestSource = sourceEndpoint;
          closestTarget = targetEndpoint;
        }
      }
    }
    return source.getFactory().createLineString(
        new Coordinate[] {new Coordinate(closestSource), new Coordinate(closestTarget)});
  }

  private static List<Coordinate> endpoints(org.locationtech.jts.geom.Geometry geometry) {
    var ret = new ArrayList<Coordinate>();
    for (int i = 0; i < geometry.getNumGeometries(); i++) {
      var component = geometry.getGeometryN(i);
      if (component instanceof Point point) {
        ret.add(point.getCoordinate());
      } else if (component instanceof LineString line) {
        ret.add(line.getCoordinateN(0));
        ret.add(line.getCoordinateN(line.getNumPoints() - 1));
      } else {
        throw new IllegalArgumentException(
            "Unsupported relationship endpoint geometry: " + component.getGeometryType());
      }
    }
    return ret;
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
