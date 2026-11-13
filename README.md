# Metro Prague

Metro Prague is a public transport app for Prague, designed to make timetables and departures easy
to find and use. The repository contains independent native platform apps and shared product and
API documentation.

## Platforms

| Platform | Technology | Project | Status |
| --- | --- | --- | --- |
| Android | Kotlin, Jetpack Compose, Material 3 Expressive | `android/` | Active development |
| iOS | Swift, SwiftUI | `ios/` | Planned |

## Monorepo Layout

```text
MetroPrague/
├── android/       # Android app, Gradle project, setup instructions
├── ios/           # iOS app (planned)
├── docs/
│   ├── product/   # shared feature behavior, parity, decisions
│   ├── api/       # shared API research
│   └── ai/        # agent roles, plans, reports, migration guide
├── fixtures/      # shared platform-neutral test inputs
└── .github/       # repository workflows
```

See [platform feature status](docs/product/parity.md) for implementation progress across platforms.

## Planned Features

### Timetables and Search

- Browse and search metro lines and their schedules.
- Browse and search tram lines and their schedules.
- View departure and arrival times for individual stations and stops.
- Show real-time updates when a suitable public transport API is available.

## Navigation and UX

The app will use a bottom navigation bar with three primary sections:

1. **Timetables/Search** for finding lines, stations, stops, and schedules.
2. **To be defined**, potentially a map, route planner, or nearby stops view.
3. **To be defined**, potentially favorites, service alerts, or settings.

Additional UX goals include:

- Material You styling with Material 3 Expressive components.
- Light and dark theme support.
- Accessible controls, descriptions, and touch targets.
- A responsive, native user interface on each platform.

## Offline and Personalization

- Cache timetable data for offline use where the data source permits it.
- Save frequently used lines, stations, or stops as favorites.
- Display service disruptions and schedule changes when such data is available.

## Data and Privacy

Golemio provides timetable and real-time data for the Android app. Android development setup and
data behavior are described in [the Android README](android/README.md). Shared API findings are in the
[Golemio API reference](docs/api/golemio.md). Never commit API keys or other secrets; keep local
platform configuration gitignored.

## Status

Metro Prague is under active development. Planned functionality and navigation sections may change
as transport data sources and product requirements are defined.

## License

This project is licensed under the Apache License, Version 2.0.
