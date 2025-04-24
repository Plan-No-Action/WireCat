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
├── model/             # Shared POJOs
├── core-capture/      # Packet capture engine
├── filter-inspection/ # Filters and inspection logic
├── export-admin/      # Exporters and admin tools
├── ui-desktop/        # JavaFX GUI
├── docs/              # UMLs, SRS, diagrams, sprint notes
├── .github/           # GitHub Actions CI
└── pom.xml            # Multi-module Maven build
```

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

