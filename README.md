# k.LAB Component Generators

[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](http://www.gnu.org/licenses/agpl-3.0)
[![Java Version](https://img.shields.io/badge/Java-21-orange)](https://openjdk.java.net/projects/jdk/21/)
[![Maven Central](https://img.shields.io/badge/Maven-1.0--SNAPSHOT-green)](https://search.maven.org/)

A k.LAB plugin component that provides autonomous observation generation capabilities for testing and simulation purposes. This component generates realistic-looking geographic terrains and spatial features using fractal algorithms and procedural generation techniques.

## Overview

The **k.LAB Component Generators** is a plugin for the k.LAB semantic modeling platform that specializes in generating synthetic geospatial data for stress-testing, simulation, and modeling scenarios. It provides contextualizers that can create realistic terrain patterns, elevation models, and random geometric shapes within specified spatial extents.

## Features

### Terrain Generation
- **Fractal Terrain Generation**: Uses the diamond-square algorithm to create realistic elevation surfaces
- **Configurable Parameters**: Adjust detail level, roughness, and value ranges
- **Memory-Efficient**: Optimized for small to medium-sized spatial grids
- **Realistic Patterns**: Suitable for simulating elevation, slope, and other terrain characteristics

### Random Shape Generation
- **Geometric Primitives**: Generate points, lines, and polygons
- **Spatial Distribution**: Non-overlapping shapes within defined envelopes
- **Configurable Density**: Control frequency and vertex count of generated shapes
- **Convex Hull Algorithm**: Ensures valid polygon generation

## Installation

This component is part of the k.LAB services ecosystem. Add it as a dependency to your k.LAB project:

```xml
<dependency>
    <groupId>org.integratedmodelling</groupId>
    <artifactId>klab.component.generators</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## Usage

### Terrain Contextualizer

The `terrain` function generates fractal surfaces suitable for elevation modeling:

```k
// Basic terrain generation with default parameters
elevation = terrain();

// Customized terrain with specific range and characteristics
elevation = terrain(
    range: [0, 3000],     // Elevation range in meters
    detail: 10,           // Higher detail level
    roughness: 0.7        // More rugged terrain
);
```

**Parameters:**
- `range` (optional): Min-max range of generated values (default: 0-4000)
- `detail` (optional): Amount of detail in the structure (default: 8)
- `roughness` (optional): Terrain roughness factor (default: 0.55)

**Requirements:**
- Must be used with S2 (spatial 2D) geometries
- Recommended for small to medium grid sizes due to memory usage

### Random relationship and bond contextualizer

Use `klab.generators.random.relationships(fraction = 20, seed = 42)` as the
implementation of a collective relationship/bond model. The Tier-0 CONNECTION
strategy resolves and binds its `source` and `target` collective inputs.

The generator samples `round(pool size * fraction / 100)` distinct members from
each input pool, then connects each selected source to one randomly chosen,
different selected target. Targets may be reused. Self-connections are excluded;
bonds also exclude reversed duplicates. Empty samples produce no observations.

Parameters:

- `fraction`: 0 through 100, default 20; applies to endpoint sampling, not all possible pairs.
- `seed`: optional integer; repeats endpoint selection, pairing and identities for the same inputs.

Relationship geometry is derived from the paired endpoint observations after
transforming both shapes to the current observation projection:

- Two point geometries are joined by a straight line from source to target.
- For line geometries, the closest pair among their start and end points is
  joined. The same rule handles point/line pairs and multipart point or line
  geometries.
- If either endpoint is a polygon or multipolygon, the result is the smallest
  convex hull covering both complete endpoint geometries.

The generated spatial shape replaces the space in the current scale, so other
dimensions such as time are retained. Every endpoint must have a non-empty
spatial point, line or polygon shape; generation fails if an endpoint has no
usable spatial geometry. Every output is an individual relationship observation
with an identity and two participants. Runtime stores it in a cohort and
acknowledges it using `ContextScope.between(...)`; this generator does not
resolve its own outputs.

### `klab:random:...` URN adapter

The universal `random` resource adapter accepts URNs in this form:

```text
klab:random:<namespace>:<resource-id>#<key>=<value>&...
```

The encoder dispatches on `<namespace>`, which may be `data`, `objects` or
`events`.

#### Numeric data

`klab:random:data:<distribution>` fills every position in the requested numeric
storage with an independent sample. Distribution arguments are positional URN
parameters named `p0`, `p1`, and so on; numbering must be contiguous. For
example:

```text
klab:random:data:gaussian#p0=10&p1=2
klab:random:data:poisson#p0=4
klab:random:data:uniform#p0=-1&p1=1
```

The implemented distributions and accepted argument counts are:

| Resource ID | Arguments (`p0`, `p1`, ...) |
| --- | --- |
| `uniform` | none, or lower bound and upper bound |
| `lognormal` | none, or scale and shape |
| `gaussian` | none, or mean and standard deviation |
| `weibull` | shape and scale, optionally inverse-CDF accuracy |
| `triangular` | lower bound, mode and upper bound |
| `cauchy` | none, or median and scale, optionally inverse-CDF accuracy |
| `beta` | alpha and beta, optionally inverse-CDF accuracy |
| `t` | degrees of freedom, optionally inverse-CDF accuracy |
| `f` | numerator and denominator degrees of freedom, optionally inverse-CDF accuracy |
| `exponential` | mean, optionally inverse-CDF accuracy |
| `binomial` | number of trials and success probability |
| `hypergeometric` | population size, number of successes and sample size |
| `pascal` | number of successes and success probability |
| `poisson` | none (mean 1), mean, or mean and convergence epsilon |

Unknown distributions, nonnumeric arguments, and unsupported argument counts
raise an error. Distribution instances are cached by name and arguments. The
adapter does not accept a seed, so repeated calls are not reproducible.

#### Spatial objects

`klab:random:objects:<shape>` creates individual objects within the requested
spatial envelope. `<shape>` is `points`, `lines` or `polygons`:

```text
klab:random:objects:points#fraction=0.1&xdivs=20&ydivs=20
klab:random:objects:polygons#fraction=0.3&vertices=8
```

The envelope is divided into an approximate grid and at most one shape is
generated per cell, so generated objects do not overlap. Supported generation
parameters are:

- `fraction`: probability of generating a shape in each cell, default `0.2`.
  This is a probability from 0 to 1, unlike the relationship contextualizer's
  percentage.
- `xdivs`, `ydivs`: approximate grid divisions, each defaulting to `10`.
- `vertices`: points use one vertex and lines use two; for polygons this sets
  the convex-hull sample count and defaults to `5`.

If either grid dimension is `1`, the current implementation creates one shape
over the entire envelope and does not apply `fraction`. No objects are emitted
for a non-spatial geometry. Parameter values are parsed directly and are not
range-validated.

Additional, non-reserved parameters become object metadata when their value is
either numeric or a supported distribution call such as `gaussian(10,2)`; one
sample is stored per generated object. The reserved names `fraction`, `xdivs`,
`ydivs`, `vertices`, `std`, `grid`, `p0` through `p3`, `duration`, and `start`
are not copied to metadata. Of these, only `fraction`, `xdivs`, `ydivs`, and
`vertices` currently affect object generation.

#### Events and current limitations

The `events` namespace is dispatched but is not implemented, so
`klab:random:events:...` currently emits no events. An unknown namespace adds an
error notification to the data builder. The adapter's type-inference hook also
currently compares the resource ID with `data`, `events`, and `objects` instead
of comparing the namespace; consequently it cannot infer a type from the
four-part URNs documented above and throws an unimplemented-operation error if
that hook is invoked.

## Technical Details

### Architecture
- **Plugin Framework**: Built on PF4J plugin architecture with k.LAB conventions
- **k.LAB Integration**: Extends `KlabComponent`; packaged by `klab.product` Maven plugin
- **Java 21**: Leverages modern Java features and performance improvements

### Algorithms
- **Diamond-Square Algorithm**: Classical fractal terrain generation (Fournier et al. 1982)
- **Convex Hull Generation**: JTS Topology Suite for valid polygon creation
- **Normal Distribution**: Apache Commons Math for statistical shape distribution

### Performance Considerations
- Terrain generation operates in RAM for optimal performance
- Recommended for geometries with reasonable grid sizes
- Memory usage scales quadratically with grid dimensions

## Dependencies

- **k.LAB Core Services**: Core k.LAB platform functionality
- **Apache Commons Math**: Statistical distributions and mathematical operations
- **GeoTools**: Geospatial data processing and geometry operations
- **JTS Topology Suite**: Computational geometry algorithms

## Development

### Building the Project

```bash
mvn clean install
```

### Running Tests

```bash
mvn test
```

## License

This project is licensed under the GNU Affero General Public License (AGPL) version 3.0. See the [license text](http://www.gnu.org/licenses/agpl-3.0.en.html) for details.

## Contributors

- **Ferdinando Villa** - Lead Developer
  - Email: ferdinando.villa@bc3research.org
  - Organization: Basque Centre for Climate Change (BC3); IKERBASQUE

## Organization

**Integrated Modelling Partnership**  
Website: [integratedmodelling.org](http://www.integratedmodelling.org)

## Repository

- **Source Code**: [GitHub Repository](https://github.com/integratedmodelling/klab.component.generators)
- **Issue Tracking**: Use GitHub Issues for bug reports and feature requests

## Support

For questions, issues, or contributions, please visit the [k.LAB community resources](http://www.integratedmodelling.org) or create an issue in the GitHub repository.

---

*This component is part of the k.LAB semantic modeling platform for integrated assessment and environmental modeling.*


