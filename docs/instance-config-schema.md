# Instance Configuration Schema

The `instance-config.yaml` defines all department-specific configuration for the monitor.

---

## Top-Level Structure

```yaml
departmentName: string          # Required – Name of the fire department
commandContact: string          # Optional – Contact info displayed during alarms (e.g. "ELW 15/11-4")
persons: []                     # Required – List of leadership personnel
vehicles: []                    # Required – List of vehicles/units
defaultOrder: []                # Required – Default vehicle display order (list of vehicle IDs)
statuses: {}                    # Required – FMS status definitions (key = status number)
ruleGroups: []                  # Required – Alarm rule groups with keyword matching
```

---

## `divera`

Divera 24/7 API credentials for this tenant.

```yaml
divera:
  accessKey: string    # Required – Divera 24/7 API access key for this department
  baseUrl: string       # Optional – overrides the global default Divera base URL
```

---

## `persons` / `vehicles` (Unit)

Each entry in `persons` and `vehicles` follows the same schema:

```yaml
- id: string                    # Required – Unique identifier (referenced in rules)
  name: string                  # Required – Full display name
  shortName: string             # Required – Abbreviated name for compact display
  type: string                  # Required – Description of the unit type
  ric: string                   # Required – Radio identification code (RIC)
  diveraId: number              # Required – Divera 24/7 vehicle/person ID
```

### Example

```yaml
vehicles:
  - id: hlf20
    name: HLF20
    shortName: HLF
    type: Hilfeleistungslöschfahrzeug
    ric: "15/48-4"
    diveraId: 4714

persons:
  - id: ortsbm
    name: OrtsBm
    shortName: OBM
    type: Ortsbrandmeister
    ric: "15/03-4"
    diveraId: 55884
```

---

## `defaultOrder`

List of vehicle `id` values that defines the default display order when no alarm-specific order applies.

```yaml
defaultOrder:
  - elw1
  - hlf20
  - tlf16
  - rw1
  - sw2000
  - mtw1
  - rtb1
```

---

## `statuses`

Map of FMS status codes to display labels and colors. The key is the numeric FMS status as a string.

```yaml
statuses:
  "1":
    label: "BEREIT/FUNK"        # Required – Display label
    color: "#6ddd6d"            # Required – Hex color for frontend display
  "2":
    label: "BEREIT/WACHE"
    color: "#1e9e4a"
  "3":
    label: "ANFAHRT"
    color: "#e8820c"
  "4":
    label: "EINSATZSTELLE"
    color: "#dd2222"
  "5":
    label: "SPRECHWUNSCH"
    color: "#2e8fdf"
  "6":
    label: "NICHT BEREIT"
    color: "#5a5a5a"
```

---

## `ruleGroups`

List of alarm categories. Each group contains rules that are matched against the alarm title.

```yaml
ruleGroups:
  - category: string            # Required – Short category code (e.g. "F", "H", "CBRN", "S")
    label: string               # Required – Display label for the category
    color: string               # Required – Hex color for alarm display
    rules: []                   # Required – List of rules within this group
```

### Rule

```yaml
rules:
  - label: string               # Required – Display name of the alarm type
    keywords: [string]          # Required – Keywords to match against alarm title
    vehicleOrder: [string]      # Required – Ordered list of vehicle IDs to alert
    remainingOrder: [string]    # Optional – Custom order for non-alerted vehicles
                                #            (falls back to defaultOrder if omitted)
    matchMode: string           # Optional – Matching strategy (default: CONTAINS)
                                #            Values: CONTAINS, EXACT, STARTS_WITH, REGEX
    hint: string                # Optional – Additional info displayed during alarm
                                #            (e.g. equipment notes, tactical hints)
```

### Match Modes

| Mode | Behavior |
|---|---|
| `CONTAINS` | Keyword found anywhere in alarm title (default, case-sensitive) |
| `EXACT` | Alarm title must exactly match the keyword (case-insensitive) |
| `STARTS_WITH` | Alarm title must start with the keyword (case-sensitive) |
| `REGEX` | Keyword is interpreted as a Java regex pattern |

### Keyword Matching Priority

When multiple rules match, the rule with the **longest matching keyword** wins. This ensures that more specific alarm codes (e.g. `F012`) take priority over general ones (e.g. `F01`).

### Example

```yaml
ruleGroups:
  - category: F
    label: Brand
    color: "#b30000"
    rules:
      - label: "PKW-Brand"
        keywords: ["F011", "F 011"]
        vehicleOrder: [elw1, hlf20, tlf16, sw2000]

      - label: "Brandnachschau"
        keywords: ["F012", "F 012"]
        vehicleOrder: [elw1, hlf20]
        hint: "Atemschutz bereitstellen, Riegelstellung prüfen"

      - label: "Kleinbrand"
        keywords: ["F01", "F 01"]
        vehicleOrder: [elw1, hlf20, tlf16]
        remainingOrder: [sw2000, rw1, mtw1, rtb1]

  - category: H
    label: Hilfeleistung
    color: "#1a72ff"
    rules:
      - label: "Person im Wasser"
        keywords: ["H071", "H 071"]
        vehicleOrder: [elw1, hlf20, mtw1, rtb1, rw1, tlf16]
        matchMode: CONTAINS

  - category: S
    label: Unterstützung
    color: "#ddaa00"
    rules:
      - label: "Unterstützung Rettungsdienst"
        keywords: ["S011", "S 011"]
        vehicleOrder: [elw1, hlf20]
```

---

## Full Minimal Example

```yaml
departmentName: Feuerwehr Musterstadt
commandContact: "ELW 15/11-4"

persons: []

vehicles:
  - id: hlf20
    name: HLF20
    shortName: HLF
    type: Hilfeleistungslöschfahrzeug
    ric: "15/48-4"
    diveraId: 4714

defaultOrder:
  - hlf20

statuses:
  "1":
    label: "BEREIT"
    color: "#00ff00"
  "6":
    label: "NICHT BEREIT"
    color: "#5a5a5a"

ruleGroups:
  - category: F
    label: Brand
    color: "#ff0000"
    rules:
      - label: Kleinbrand
        keywords: ["F01"]
        vehicleOrder: [hlf20]
```