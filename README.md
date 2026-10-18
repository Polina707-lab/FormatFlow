# FormatFlow

**FormatFlow** is a modular file conversion application built with Java, OSGi, and Eclipse SWT/JFace.

The project demonstrates a plugin-based architecture where file converters can be developed and integrated as independent OSGi bundles.

## Technologies

- Java
- OSGi
- Eclipse PDE
- SWT/JFace (planned)

## Project Structure

- `formatflow.api` — shared interfaces for file converters.
- `formatflow.converter.csvjson` — CSV-to-JSON converter implementation.

## Project Status

Under active development.

## Planned Features

- File conversion between supported formats.
- Modular converter architecture using OSGi services.
- Desktop graphical interface with SWT/JFace.
- Extensible converter system.
- Automated tests.