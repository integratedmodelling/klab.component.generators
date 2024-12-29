package org.integratedmodelling.generators.adapters;

import org.apache.commons.math3.distribution.*;
import org.integratedmodelling.common.utils.Utils;
import org.integratedmodelling.generators.utils.RandomShapes;
import org.integratedmodelling.klab.api.data.Data;
import org.integratedmodelling.klab.api.data.Version;
import org.integratedmodelling.klab.api.exceptions.KlabIllegalArgumentException;
import org.integratedmodelling.klab.api.exceptions.KlabUnimplementedException;
import org.integratedmodelling.klab.api.geometry.Geometry;
import org.integratedmodelling.klab.api.knowledge.Artifact;
import org.integratedmodelling.klab.api.knowledge.Observable;
import org.integratedmodelling.klab.api.knowledge.Urn;
import org.integratedmodelling.klab.api.knowledge.observation.scale.Scale;
import org.integratedmodelling.klab.api.services.resources.adapters.ResourceAdapter;
import org.integratedmodelling.klab.api.services.runtime.Notification;

import java.util.*;


/**
 * Handles "klab:random:...." URNs. Produces various types of random data, objects, or events. The namespace
 * (third field of the URN) selects the type of object:
 * <p>
 * <dl>
 * <dt>data</dt>
 * <dd>produces numbers for states with the distribution set in the fourth field
 * (resource ID). Range, distribution and sequence vary according to parameters
 * <code>min</code>, <code>max</code>, <code>mean</code>, <code>std</code>,
 * <code>variance</code>, <code>alpha</code>, <code>beta</code>,
 * <code>seed</code>; defaults to normalized distributions.</dd>
 * <dt>events</dt>
 * <dd>produces random events with the shape and duration defined in parameters.
 * The fourth field (resource ID) can be <code>polygons</code>,
 * <code>points</code> or <code>lines</code>. By default, will produce
 * non-overlapping shapes occupying a 10x10 grid in the context and with a
 * probability of 10% at each tick and a duration between 0.1 and 2x the event
 * frequency. Non-spatial events will be supported later.</dd>
 * <dt>objects</dt>
 * <dd>produces random objects with the shape defined in parameters. The fourth
 * field (resource ID) can be <code>polygons</code>, <code>points</code> or
 * <code>lines</code>. By default, will produce non-overlapping shapes in a
 * 10x10 grid with a 50% probability per cell. Non-spatial objects will be
 * supported later.</dd> *
 * </dl>
 *
 * @author Ferd
 */
@ResourceAdapter(name = "random", universal = true, version = Version.CURRENT)
public class RandomGeneratorAdapter {

    private static final String POISSON = "poisson";
    private static final String PASCAL = "pascal";
    private static final String HYPERGEOMETRIC = "hypergeometric";
    private static final String GEOMETRIC = "geometric";
    private static final String BINOMIAL = "binomial";
    private static final String LAPLACE = "laplace";
    private static final String EXPONENTIAL = "exponential";
    private static final String F = "f";
    private static final String T = "t";
    private static final String NAKAGAMI = "nakagami";
    private static final String BETA = "beta";
    private static final String CAUCHY = "cauchy";
    private static final String TRIANGULAR = "triangular";
    private static final String GAMMA = "gamma";
    private static final String PARETO = "pareto";
    private static final String WEIBULL = "weibull";
    private static final String LEVY = "levy";
    private static final String GAUSSIAN = "gaussian";
    private static final String LOGNORMAL = "lognormal";
    private static final String LOGISTIC = "logistic";
    private static final String UNIFORM = "uniform";

    private static final String FRACTION = "fraction";
    private static final String YDIVS = "ydivs";
    private static final String XDIVS = "xdivs";
    private static final String STD = "std";
    private static final String VERTICES = "vertices";
    private static final String GRID = "grid";
    private static final String DURATION = "duration";
    private static final String START = "start";

    public static final String POLYGONS = "polygons";
    public static final String LINES = "lines";
    public static final String POINTS = "points";

    public static final String OBJECTS = "objects";
    public static final String EVENTS = "events";
    public static final String DATA = "data";
    public static final String NAME = "random";

    public static final String P0 = "p0";
    public static final String P1 = "p1";
    public static final String P2 = "p2";
    public static final String P3 = "p3";

    public static String[] namespace_ids = new String[]{DATA, EVENTS, OBJECTS};
    public static String[] shape_ids = new String[]{LINES, POINTS, POLYGONS};
    public static String[] distribution_ids = new String[]{POISSON, PASCAL, HYPERGEOMETRIC, GEOMETRIC,
                                                           BINOMIAL, GAUSSIAN,
                                                           LAPLACE, EXPONENTIAL, F, T, NAKAGAMI, BETA,
                                                           CAUCHY, TRIANGULAR, GAMMA, PARETO, WEIBULL, LEVY,
                                                           LOGNORMAL, LOGISTIC, UNIFORM};
    public static String[] object_attribute_ids = new String[]{FRACTION, XDIVS, YDIVS, VERTICES, STD, GRID,
                                                               P0, P1, P2, P3, DURATION, START};

    private Map<String, Object> distributions = Collections.synchronizedMap(new HashMap<>());
    private RandomShapes shapeGenerator = new RandomShapes();

    public RandomGeneratorAdapter() {
        Arrays.sort(namespace_ids);
        Arrays.sort(shape_ids);
        Arrays.sort(distribution_ids);
        Arrays.sort(object_attribute_ids);
    }

    @ResourceAdapter.Encoder
    public void encode(Urn urn, Data.Builder builder, Geometry geometry, Observable observable) {
        switch (urn.getResourceId()) {
            case DATA -> makeData(urn, builder, geometry);
            case EVENTS -> makeEvents(urn, builder, geometry);
            case OBJECTS -> makeObjects(urn, builder, geometry, observable);
            default -> builder.notification(Notification.error("Random generator adapter: cannot establish " +
                    "what to do with " + urn));
        }
    }

    private void makeObjects(Urn urn, Data.Builder builder, Geometry geometry, Observable observable) {
        
        int vertices = urn.getParameters().containsKey(VERTICES) ?
                       Integer.parseInt(urn.getParameters().get(VERTICES))
                                                                 : 5;
        int xdivs = urn.getParameters().containsKey(XDIVS) ?
                    Integer.parseInt(urn.getParameters().get(XDIVS)) : 10;
        int ydivs = urn.getParameters().containsKey(YDIVS) ?
                    Integer.parseInt(urn.getParameters().get(YDIVS)) : 10;
        double probability = urn.getParameters().containsKey(FRACTION)
                             ? Double.parseDouble(urn.getParameters().get(FRACTION))
                             : 0.5;
        String artifactName = urn.getResourceId().substring(0, urn.getResourceId().length() - 1);

        var scale = Scale.create(geometry);

        if (scale.getSpace() != null) {

            var envelope = scale.getSpace().getEnvelope();
            var objects = builder.objectCollection();

            int n = 0;
            for (var shape : switch (urn.getResourceId()) {
                case POINTS -> shapeGenerator.create(envelope, xdivs, ydivs, probability, 1);
                case LINES -> shapeGenerator.create(envelope, xdivs, ydivs, probability, 2);
                case POLYGONS -> shapeGenerator.create(envelope, xdivs, ydivs, probability, vertices);
                default -> throw new KlabIllegalArgumentException("random adapter: unrecognized shape");
            }) {
                /*
                 * TODO honor any filters on the shapes - area, width, length, whatever
                 */

                var obuilder =
                        objects.add().name(artifactName + "_" + (++n)).geometry(shape.as(Geometry.class));

                if (!urn.getParameters().isEmpty()) {
                    for (String attribute : urn.getParameters().keySet()) {
                        if (Arrays.binarySearch(object_attribute_ids, attribute) < 0) {
                            Object value = getAttributeValue(urn.getParameters().get
                                    (attribute));
                            if (value != null) {
                                //  obuilder.withMetadata(attribute, value);
                            }
                        }
                    }
                }

            }
        }
    }

    private void makeEvents(Urn urn, Data.Builder builder, Geometry geometry) {
    }

    private void makeData(Urn urn, Data.Builder builder, Geometry geometry) {
    }

    private synchronized Object getAttributeValue(String string) {
        if (Utils.Numbers.encodesDouble(string) || Utils.Numbers.encodesLong(string)) {
            return Double.parseDouble(string);
        }
        String[] tokens = Utils.Strings.parseAsFunctionCall(string);
        if (tokens.length > 0 && Arrays.binarySearch(distribution_ids, tokens[0]) >= 0) {
            return sampleDistribution(tokens);
        }

        return null;
    }

    private Double sampleDistribution(String[] tokens) {
        Object distribution = getDistribution(tokens);
        if (distribution instanceof RealDistribution) {
            return ((RealDistribution) distribution).sample();
        } else if (distribution instanceof IntegerDistribution) {
            return (double) ((IntegerDistribution) distribution).sample();
        }
        return null;
    }


    private Object getDistribution(Urn urn) {

        List<String> tokens = new ArrayList<>();
        tokens.add(urn.getResourceId());
        for (int i = 0; ; i++) {
            if (!urn.getParameters().containsKey("p" + i)) {
                break;
            }
            tokens.add(urn.getParameters().get("p" + i));
        }

        return getDistribution(tokens.toArray(new String[tokens.size()]));
    }

    public synchronized Object getDistribution(String[] tokens) {

        String signature = Arrays.toString(tokens);
        if (distributions.containsKey(signature)) {
            return distributions.get(signature);
        }

        List<Double> params = new ArrayList<>();
        for (int i = 1; i < tokens.length; i++) {
            try {
                params.add(Double.parseDouble(tokens[i]));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        Object ret = null;

        switch (tokens[0]) {
            case UNIFORM:
                if (params.isEmpty()) {
                    ret = new UniformRealDistribution();
                } else if (params.size() == 2) {
                    ret = new UniformRealDistribution(params.get(0), params.get(1));
                }
                break;
            //            case LOGISTIC:
            //                if (params.size() == 2) {
            //                    ret = new LogisticDistribution(params.get(0), params.get(1));
            //                }
            //                break;
            case LOGNORMAL:
                if (params.isEmpty()) {
                    ret = new LogNormalDistribution();
                } else if (params.size() == 2) {
                    ret = new LogNormalDistribution(params.get(0), params.get(1));
                }
                break;
            case GAUSSIAN:
                if (params.isEmpty()) {
                    ret = new NormalDistribution();
                } else if (params.size() == 2) {
                    ret = new NormalDistribution(params.get(0), params.get(1));
                }
                break;
            //            case LEVY:
            //                if (params.size() == 2) {
            //                    ret = new LevyDistribution(params.get(0), params.get(1));
            //                }
            //                break;
            case WEIBULL:
                if (params.size() == 2) {
                    ret = new WeibullDistribution(params.get(0), params.get(1));
                } else if (params.size() == 3) {
                    ret = new WeibullDistribution(params.get(0), params.get(1), params.get(2));
                }
                break;
            //            case PARETO:
            //                if (params.size() == 0) {
            //                    ret = new ParetoDistribution();
            //                } else if (params.size() == 2) {
            //                    ret = new ParetoDistribution(params.get(0), params.get(1));
            //                } else if (params.size() == 3) {
            //                    ret = new ParetoDistribution(params.get(0), params.get(1), params.get(2));
            //                }
            //                break;
            //            case GAMMA:
            //                if (params.size() == 0) {
            //                    ret = new ParetoDistribution();
            //                } else if (params.size() == 2) {
            //                    ret = new ParetoDistribution(params.get(0), params.get(1));
            //                } else if (params.size() == 3) {
            //                    ret = new ParetoDistribution(params.get(0), params.get(1), params.get(2));
            //                }
            //                break;
            case TRIANGULAR:
                if (params.size() == 3) {
                    ret = new TriangularDistribution(params.get(0), params.get(1), params.get(2));
                }
                break;
            case CAUCHY:
                if (params.isEmpty()) {
                    ret = new CauchyDistribution();
                } else if (params.size() == 2) {
                    ret = new CauchyDistribution(params.get(0), params.get(1));
                } else if (params.size() == 3) {
                    ret = new CauchyDistribution(params.get(0), params.get(1), params.get(2));
                }
                break;
            case BETA:
                if (params.size() == 2) {
                    ret = new BetaDistribution(params.get(0), params.get(1));
                } else if (params.size() == 3) {
                    ret = new BetaDistribution(params.get(0), params.get(1), params.get(2));
                }
                break;
            //            case NAKAGAMI:
            //                if (params.size() == 2) {
            //                    ret = new NakagamiDistribution(params.get(0), params.get(1));
            //                } else if (params.size() == 3) {
            //                    ret = new NakagamiDistribution(params.get(0), params.get(1), params.get(2));
            //                }
            //                break;
            case T:
                if (params.size() == 1) {
                    ret = new TDistribution(params.get(0));
                } else if (params.size() == 2) {
                    ret = new TDistribution(params.get(0), params.get(1));
                }
                break;
            case F:
                if (params.size() == 2) {
                    ret = new FDistribution(params.get(0), params.get(1));
                } else if (params.size() == 3) {
                    ret = new FDistribution(params.get(0), params.get(1), params.get(2));
                }
                break;
            case EXPONENTIAL:
                if (params.size() == 1) {
                    ret = new ExponentialDistribution(params.get(0));
                } else if (params.size() == 2) {
                    ret = new ExponentialDistribution(params.get(0), params.get(1));
                }
                break;
            //            case LAPLACE:
            //                if (params.size() == 2) {
            //                    ret = new LaplaceDistribution(params.get(0), params.get(1));
            //                }
            //                break;
            case BINOMIAL:
                if (params.size() == 2) {
                    ret = new BinomialDistribution(params.get(0).intValue(), params.get(1));
                }
                break;
            //            case GEOMETRIC:
            //                if (params.size() == 1) {
            //                    ret = new GeometricDistribution(params.get(0));
            //                }
            //                break;
            case HYPERGEOMETRIC:
                if (params.size() == 3) {
                    ret = new HypergeometricDistribution(params.get(0).intValue(), params.get(1).intValue(),
                            params.get(2).intValue());
                }
                break;
            case PASCAL:
                if (params.size() == 2) {
                    ret = new PascalDistribution(params.get(0).intValue(), params.get(1));
                }
                break;
            case POISSON:
                if (params.isEmpty()) {
                    ret = new PoissonDistribution(1.0);
                } else if (params.size() == 1) {
                    ret = new PoissonDistribution(params.get(0));
                } else if (params.size() == 2) {
                    ret = new PoissonDistribution(params.get(0), params.get(1));
                }
                break;
        }

        if (ret == null) {
            throw new IllegalArgumentException(
                    "random adapter: distribution " + tokens[0] + " called with wrong parameters or unknown");
        }

        distributions.put(signature, ret);

        return ret;
    }


    /**
     * If there's no type param in the resource adapter annotation. This may take a URN and/or a full Resource
     * according to what is needed to establish the type.
     *
     * @param resourceUrn
     * @return
     */
    @ResourceAdapter.Type
    public Artifact.Type getType(Urn resourceUrn) {
        return switch (resourceUrn.getResourceId()) {
            case DATA -> Artifact.Type.NUMBER;
            case EVENTS -> Artifact.Type.EVENT;
            case OBJECTS -> Artifact.Type.OBJECT;
            default -> throw new KlabUnimplementedException("random adapter: can't handle URN " + resourceUrn);
        };
    }


}
