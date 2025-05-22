# 🐾 WireCat – Network Packet Analysis Toolkit

**WireCat** is a modern, cross-platform network traffic capture and inspection tool built in JavaFX. Designed for cybersecurity students and professionals, it enables **real-time packet analysis**, deep inspection, and flexible export capabilities — all wrapped in a modular, intuitive interface.

> 🛱️ Capture. 🔍 Filter. 📊 Analyze.  
> Built by students, for the next generation of analysts.

---

## 📦 Features

- **Live Packet Capture** from selected interfaces
- **Hex & ASCII Inspection View** for deep analysis
- **Protocol Filtering** (TCP, UDP, ICMP, ARP, ...)
- **Real-Time Statistics Dashboard**
- **Export Support** (`.pcap`, `.txt`, `.csv`)
- **Admin Tools**: Configuration, diagnostics, and update utility
- **Modular Java Architecture (MVC)** for scalability
- **Multi-platform**: Runs on Linux, macOS, and Windows

---

## 🧱 Project Structure

```
WireCat/
├── .github/                    # GitHub Actions CI workflows
│   └── workflows/              # CI/CD pipelines
│
├── core-capture/               # Main module for capturing and analyzing packets
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/wirecat/core_capture/
│   │       │       ├── filter/         # Packet filtering logic (by IP, port, protocol, etc.)
│   │       │       ├── model/          # Data models (CapturedPacket, PacketDetail, Conversation, etc.)
│   │       │       ├── service/        # Core services (CaptureService, AIAnalysisService)
│   │       │       ├── ui/panel/       # JavaFX UI panels (InspectorPanel, ConversationPanel)
│   │       │       └── util/           # Utility classes and helpers
│   │       └── resources/
│   │           ├── css/                # JavaFX CSS for styling
│   │           │   └── components/     # Component-specific styles
│   │           └── icons/              # Icons used across the UI
│   └── target/                         # Build output (compiled classes, etc.)
│       ├── classes/
│       ├── generated-sources/
│       └── maven-status/
│
├── sample/                     # Sample .pcap files or datasets for testing
└── pom.xml                     # Multi-module Maven build file

```
## 📂 Key Packages Overview

| Package          | Purpose                                             |
| ---------------- | --------------------------------------------------- |
| `filter`         | Defines and applies filters for captured packets    |
| `model`          | Contains core data classes (packets, conversations) |
| `service`        | Handles live capture, AI integration, export logic  |
| `ui.panel`       | JavaFX UI panels (viewer, inspector, tracker)       |
| `util`           | Utilities and support classes                       |
| `css/components` | Visual styles for UI elements                       |
| `icons`          | Icon assets used in the frontend                    |


---

## 🚀 Quick Start

### 🛠 Prerequisites

- **Java 17+**
- **Maven**
- (Optional) **Wireshark/tcpdump** for comparison/testing
- On Linux, use `sudo` or `setcap` for interface access (`sudo setcap cap_net_raw=eip $(which java)`)

### 🔧 Build & Run

```bash
# Clone the repo
git clone https://github.com/Plan-No-Action/WireCat.git
cd WireCat

# Build the project
mvn clean install

# Run the JavaFX UI
cd ui-desktop
mvn javafx:run
```

---

## 📊 Use Cases

- Real-time traffic monitoring in university labs
- Packet analysis training for CTFs and blue team prep
- Lightweight alternative to Wireshark
- Export `.pcap` for offline forensics and training

---

## 🧐 Technology Stack

| Component      | Tech                     |
|----------------|--------------------------|
| Language       | Java (JDK 17+)           |
| GUI            | JavaFX                   |
| Packet Capture | Pcap4J / JNetPcap        |
| Build Tool     | Maven                    |
| Testing        | JUnit                    |
---

