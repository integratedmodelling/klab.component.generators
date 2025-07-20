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

## Technical Details

### Architecture
- **Plugin Framework**: Built on PF4J plugin architecture
- **k.LAB Integration**: Extends `KlabComponent` for seamless integration
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
