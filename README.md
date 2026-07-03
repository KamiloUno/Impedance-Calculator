# Impedance Calculator

A comprehensive Java application for calculating equivalent impedance in electrical circuits. The tool enables engineers and students to analyze complex circuits containing resistors, capacitors, and inductors in various configurations.

## Key Features

**Component Types**
- Resistors (fixed impedance)
- Capacitors (frequency-dependent impedance)
- Inductors (frequency-dependent impedance)

**Circuit Configuration**
- Series connections for additive impedance calculations
- Parallel connections for reciprocal impedance calculations
- Mixed topologies by combining series and parallel sections
- Build circuits interactively or through command-line expressions

**User Interfaces**
- **Graphical Interface (Swing)**: Intuitive visual circuit builder with tree-based component organization and real-time calculation results
- **Console Interface**: Interactive command-line mode for batch processing and scripting
- Both interfaces feature validation and error handling for robust operation

**Calculation Capabilities**
- Real and imaginary impedance components
- Impedance magnitude computation
- Frequency-dependent reactance for capacitors and inductors
- Support for wide frequency ranges (from millihertz to gigahertz)

**Developer Features**
- Clean MVC architecture separating model, view, and controller concerns
- Complex number arithmetic library for precise calculations
- Comprehensive input parsing and validation
- Accessibility features in GUI for inclusive user experience
- Full test coverage with JUnit 5 parameterized tests

## Technical Stack

- **Language**: Java 21
- **Build System**: Maven
- **GUI Framework**: Swing
- **Testing**: JUnit 5
- **Code Generation**: Project Lombok
- **Architecture**: Model-View-Controller pattern

## Console Interface
<img width="512" height="315" alt="image" src="https://github.com/user-attachments/assets/325a9d64-e0d1-4cf3-bd0a-5f5986971ca3" />

## Graphical Interface
<img width="780" height="760" alt="image" src="https://github.com/user-attachments/assets/9fa1a8d2-658f-4e5a-be08-ef1da8c16264" />


