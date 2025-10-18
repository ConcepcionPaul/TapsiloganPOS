# Tapsilogan

A simple **OOP-based Point of Sale (POS) System** built with **Java** and **Maven**, designed for a Tapsilogan restaurant.  

---

## ⚙️ Requirements

- [Java JDK 17+](https://adoptium.net/) (or compatible)
- [Apache Maven](https://maven.apache.org/) 3.8+
- Windows is supported out of the box via JavaFX dependencies with `win` classifiers in `pom.xml`.
  - On macOS/Linux, update the JavaFX dependency classifiers (`mac`, `linux`) or remove classifiers and use modular JavaFX.

---

## 🚀 How to Run

- Run all commands from the project root: `Concepcion_oop/`
- Ensure data files exist in the project root (see Data Files below).

### 1) Clone
```bash
git clone <repo-url>
cd Concepcion_oop
```

### 2) Build
```bash
mvn clean package
```

### 3) Run (JavaFX GUI)
Preferred (JavaFX Maven Plugin):
```bash
mvn javafx:run
```

Alternative (Exec Plugin):
```bash
mvn exec:java -Dexec.mainClass="JavaFXMain"
```

---

## 📁 Data Files
The app reads/writes JSON files from the current working directory (`System.getProperty("user.dir")`). Keep these in the project root (`Concepcion_oop/`):

- `menu.json`
- `inventory.json`
- `sales.json`

These paths are defined in `TapsiloganPOS/src/main/java/DataPaths.java`.

---

## 🔐 Default Credentials
Located at `TapsiloganPOS/src/main/resources/credentials.json`:

- **admin** / `admin123` (role: admin)
- **cashier** / `cashier123` (role: cashier)

---

## ❗ Troubleshooting

- **JavaFX errors or blank window**: Verify JDK 17 and run with Maven (`mvn -v` to check). Prefer `mvn javafx:run`.
- **Resources not found (FXML/CSS)**: Always run from project root so resources under `TapsiloganPOS/src/main/resources/` resolve correctly.
- **Data not loading/saving**: Ensure JSON files exist in the root as listed above and that you have write permissions.
- **Non-Windows OS**: Adjust JavaFX dependency classifiers in `pom.xml`.

---

## 💡 Suggestions

- **Packaging**: Add an assembly or jpackage step to produce a runnable app per-OS (fat jar or native image).
- **Cross-platform JavaFX**: Parameterize the JavaFX platform or use OS profiles in `pom.xml`.
- **Data location**: Consider moving defaults under `resources` and copying to a user data directory on first run.
- **Credentials**: Replace JSON credentials with a hashed store; add a simple user management UI.
- **Tests/CI**: Add unit tests and a GitHub Actions workflow for build and basic checks.

---
