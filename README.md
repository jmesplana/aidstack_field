# Aidstack Field

A mobile disaster response and field reporting application built for Android. Designed for humanitarian organizations, emergency responders, and disaster relief teams working in challenging environments with limited connectivity.

## Overview

Aidstack Field enables field teams to document damage assessments, medical needs, supply requests, hazards, and other critical information during disaster response operations. The app works offline-first, ensuring teams can continue operations even when connectivity is limited or unavailable.

## Key Features

### Field Reporting
- **Quick Report Creation**: Create detailed field reports with GPS coordinates, photos, severity levels, and categorization
- **9 Report Categories**: Damage Assessment, Medical Need, Supply Request, Hazard Alert, Evacuation Point, Shelter, Water Source, Infrastructure, and Other
- **5 Severity Levels**: Critical, High, Medium, Low, and Info with color-coded indicators
- **Photo Documentation**: Attach multiple photos with automatic compression to optimize storage and bandwidth

### Status Tracking & Activity Log
- **Status Management**: Track report lifecycle through 6 states: New → Assessed → In Progress → Needs Supplies → Resolved → Closed
- **Activity Timeline**: Comprehensive log of all updates, status changes, and actions taken on each report
- **Collaborative Notes**: Team members can add notes and photos to ongoing reports, maintaining a complete history

### Offline Capabilities
- **Offline Map Caching**: Pre-download map tiles for specific regions before deployment
- **Pre-configured Regions**: Quick access to disaster-prone areas including Jamaica, Haiti, Dominican Republic, Puerto Rico, Cuba, Philippines, Indonesia, Bangladesh, Nepal, and Myanmar
- **Custom Region Download**: Download maps for any location with configurable quality levels (Low/Medium/High)
- **Offline Indicators**: Visual indicators show network status and unsynced report counts

### Location & Navigation
- **Real-time GPS Tracking**: Track your current location, altitude, speed, and bearing
- **Interactive Map View**: View all reports and nearby disasters on an OpenStreetMap-based interface
- **Turn-by-Turn Navigation**: Launch external navigation apps to get directions to report locations
- **Report Clustering**: Easily identify areas with multiple reports

### Data Management
- **CSV Export**: Export all field reports to QGIS-compatible CSV format for GIS analysis and mapping
- **QGIS Integration**: CSV includes WKT (Well-Known Text) geometry format and proper coordinate fields for direct import
- **Report Sharing**: Share individual reports or location data via messaging apps, email, or other channels
- **Statistics Dashboard**: View summary statistics of field operations
- **Photo Compression**: Automatic image compression reduces storage requirements by up to 70%

### Disaster Awareness
- **GDACS Integration**: Real-time feed of global disasters from the Global Disaster Alert and Coordination System
- **Nearby Disasters**: Automatic detection of disasters near your location
- **Disaster Filtering**: Filter by disaster type (earthquakes, cyclones, floods, volcanoes, droughts, wildfires)
- **Distance Calculation**: See how far you are from active disaster zones

## Use Case: Hurricane Response in Jamaica

### Scenario
Hurricane Melissa has made landfall in Jamaica, causing widespread damage across multiple parishes. Infrastructure is damaged, communications are spotty, and coordination between response teams is challenging. Aidstack Field is deployed to support the response effort.

### Pre-Deployment (72 hours before landfall)
1. **Map Preparation**: Response coordinators use the Offline Maps feature to download high-quality map tiles for all of Jamaica and surrounding areas
2. **Team Briefing**: Field teams are trained on the app and categorization system
3. **Device Distribution**: Smartphones with Aidstack Field pre-installed are distributed to assessment teams

### Day 1: Initial Assessment
- **Damage Assessment Teams** deploy to affected areas as soon as it's safe
- Teams create reports using the **Damage Assessment** category, marking severity levels from Critical to Low
- Photos of damaged buildings, roads, and infrastructure are attached (automatically compressed to save storage)
- GPS coordinates are automatically captured for each report
- Reports are saved locally due to lack of cell coverage

### Day 2: Medical & Supply Coordination
- **Medical Teams** use the app to mark locations with **Medical Need** (Critical severity for life-threatening situations)
- **Logistics Teams** identify accessible roads using the **Infrastructure** category
- Field coordinators mark **Shelter** locations and **Water Source** points for displaced populations
- When teams reach areas with connectivity, reports automatically sync
- Exported CSV files are shared via satellite internet to the Emergency Operations Center

### Days 3-7: Ongoing Response
- **Status Tracking** helps coordinate follow-up actions:
  - Critical medical needs marked as "Assessed" once evaluated by medical staff
  - Supply requests transition to "In Progress" when logistics teams are dispatched
  - Reports marked "Resolved" when assistance is delivered
- **Activity Log** maintains record of all actions:
  - "Team deployed with medical supplies - 50 patients treated"
  - "Water purification tablets distributed - 200 families served"
  - "Road cleared, access restored"
- Multiple teams add notes to the same report, creating a comprehensive history
- **Navigation feature** guides new teams to previously reported locations
- Map view helps identify underserved areas (gaps in report coverage)

### Week 2: Recovery Phase
- Reports marked "Closed" as areas recover
- CSV exports provide comprehensive documentation for disaster assessments
- Data shared with government agencies, NGOs, and donors
- Lessons learned inform future disaster preparedness

## Technical Specifications

- **Platform**: Android (API 24+)
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material Design 3
- **Database**: Room (SQLite)
- **Maps**: OSMDroid (OpenStreetMap)
- **Image Loading**: Coil
- **Location Services**: Google Play Services Fused Location Provider
- **Networking**: Retrofit 2 with Moshi JSON serialization

## Installation

1. Clone this repository
2. Open the project in Android Studio
3. Sync Gradle dependencies
4. Build and run on an Android device or emulator

## Configuration

### Offline Maps
1. Open Settings from the top-right menu
2. Select "Offline Maps"
3. Choose a pre-configured region or enter custom coordinates
4. Select quality level (Low: zoom 12, Medium: zoom 14, High: zoom 16)
5. Download maps before deploying to the field

### Units
Toggle between Metric and Imperial units in Settings for speed, altitude, and distance measurements.

### QGIS Import
To import field reports into QGIS for spatial analysis:

1. Export reports as CSV from the Reports tab (Share icon → Export as CSV)
2. Share/save the CSV file to your computer
3. In QGIS, go to Layer → Add Layer → Add Delimited Text Layer
4. Select the exported CSV file
5. Choose "Point coordinates" as Geometry Definition
6. Set X field to "Longitude" and Y field to "Latitude"
7. Set Geometry CRS to "EPSG:4326 - WGS 84"
8. Click "Add" to import the layer

The CSV includes all report details (category, severity, status, description) as attribute fields that can be used for styling, filtering, and analysis in QGIS. The WKT column provides an alternative geometry import method.

### Export Options for Coordinators

The app provides three export options designed for efficient coordination workflow:

**1. Full Export (Current State)**
- Exports all reports with their latest status and information
- One row per report showing current conditions
- Best for: Initial deployment, creating baseline maps, generating situation reports
- Updates timestamp to track when data was last exported

**2. Export Changes Only (Delta)**
- Only exports reports modified since the last export
- Shows count of changed reports in the menu (e.g., "Export Changes Only (12)")
- Best for: Daily updates, bandwidth-limited environments, incremental coordination
- Automatically appears after first export when reports have been updated

**3. Complete Package (+ Timeline)**
- Exports TWO CSV files: Current State + Activity Timeline
- Current State CSV: Latest information for all reports
- Activity Timeline CSV: Full history of all status changes, notes, and actions
- Best for: Comprehensive documentation, accountability tracking, post-disaster analysis

**Coordinator Workflow Example:**

Day 1 (Initial Assessment):
- Field teams create 50 reports
- Coordinator exports "Full Export"
- Imports to QGIS, creates situation map

Day 2 (Response Operations):
- Teams update 10 reports (status changes, add notes)
- Field teams create 15 new reports
- Coordinator exports "Export Changes Only (25)"
- Re-imports to QGIS using same layer (QGIS updates existing reports by ID, adds new ones)

End of Week (Documentation):
- Coordinator exports "Complete Package"
- Activity Timeline CSV shows full evolution of each incident
- Used for after-action reports, donor documentation, lessons learned

**Key Benefits:**
- Unique report IDs ensure proper updates (no duplicates)
- Delta export reduces bandwidth usage by up to 80%
- Activity timeline preserves full audit trail
- Timestamps enable temporal analysis in QGIS

## Permissions

- **Location (Fine & Coarse)**: Required for GPS tracking and map functionality
- **Storage**: Required for saving photos and exported data

## Data Privacy

All data is stored locally on the device. No data is transmitted to external servers unless explicitly shared by the user via the export/share functionality.

## Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.

## License

MIT License - See LICENSE file for details

## Support

For issues, questions, or feature requests, please open an issue on GitHub.

---

Built for humanitarian response teams working in the world's most challenging environments.
