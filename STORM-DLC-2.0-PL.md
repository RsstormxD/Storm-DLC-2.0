# Storm DLC 2.0 — Fabric / Minecraft 1.21.4

Wersja moda: 2.0.1. Nazwa pozostaje Storm DLC 2.0.
Plik: `build/libs/storm-dlc-2.0.1+1.21.4.jar`.

Nazwa Storm DLC 2.0 jest używana w GUI, HUD, tytule okna, Discord RPC,
metadanych, dokumentacji i projekcie Gradle. Przestrzeń nazw zasobów i pakietów
integracji to `stormdlc`, a ustawienia są zapisywane w `config/stormdlc`.
Przy pierwszym uruchomieniu dotychczasowy folder ustawień jest przenoszony,
jeśli nie ma jeszcze nowego folderu. Nowe konfiguracje nie są nadpisywane.

Discord RPC → **Spinning logo** wybiera rzeczywistą animację niebieskiego S:
60 klatek, pełny obrót w 3 sekundy, zapętlony GIF. Wyłączenie opcji wybiera
obraz statyczny. **Show Storm logo** włącza lub wyłącza sam obraz.
RPC nie dopisuje już napisu o obracaniu logo.

Discord wymaga publicznego adresu animacji. Domyślny GIF jest dostępny pod
`https://iili.io/n0L5JPS.gif` i dołączony do źródeł oraz JAR-a jako
`assets/stormdlc/discord-logo-spinning.gif`. W pliku
`config/stormdlc/discord-rpc.json` pole `animatedLargeImage` pozwala użyć
własnego publicznego GIF-a lub animowanego WebP. Pole `largeImage` określa
statyczny obraz. Po zmianie adresu użyj **Reconnect to Discord**.

Po ustawieniu repozytorium klient sprawdza GitHub Releases w tle przy uruchomieniu
i co 30 minut. Nowsza stabilna wersja z JAR-em dla Minecrafta 1.21.4
wyświetla powiadomienie oraz prywatną wiadomość na czacie z linkiem **Pobierz**.
Starsze JAR-y z poprzednich zmian nie mają tego mechanizmu: zainstaluj bieżący JAR.
Adres repozytorium ustawia `updates_repository` w `gradle.properties`.
GitHub Actions uzupełnia go automatycznie podczas publikowania wydania.

Budowanie bez testów: `gradlew.bat assemble -x test -x compileTestJava -x processTestResources` (Java 21, Mojmap).
Nowe wydanie opublikujesz przez zmianę `mod_version` w `gradle.properties`
na gałęzi `main` lub wysłanie tagu, np. `v2.0.2+1.21.4`.
Workflow `.github/workflows/release.yml` skompiluje i opublikuje JAR-y.

System Combat i Friends opisuje [STORM-DLC-MODULES-2.0-PL.md](STORM-DLC-MODULES-2.0-PL.md).
Animowane ładowanie, trzy motywy menu, profil Discord i ruch KillAura:
[STORM-DLC-UI-2.0-PL.md](STORM-DLC-UI-2.0-PL.md).
