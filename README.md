# Storm DLC 2.0 — Minecraft 1.21.4

[Pobierz najnowszy JAR](https://github.com/RsstormxD/Storm-DLC-2.0/releases/latest)
· [Repozytorium](https://github.com/RsstormxD/Storm-DLC-2.0)

Połączenie Astolfo Visuals 2.0 i Song Island 1.0.1, z portem Song Island z 1.21.11.

## Instalacja

Minecraft Java 1.21.4, Java 21, Fabric Loader 0.16.10 lub nowszy oraz Fabric API
0.119.4+1.21.4. Plik storm-dlc-2.0.0+1.21.4.jar umieść w folderze mods.
Usuń poprzednie osobne mody Astolfo Visuals i Song Island z tego profilu.
Usuń też poprzedni JAR klienta, zanim dodasz nowe wydanie Storm DLC 2.0.
Obsługa Spotify/sesji multimedialnych wymaga Windows x64. Biblioteka DLL znajduje
się wewnątrz moda i jest ładowana automatycznie; nie kopiuj jej do folderu systemowego.

## Obsługa

* INSERT lub prawy SHIFT: zwykłe GUI Storm DLC 2.0.
* Render → Interface: moduły Dynamic Island, Cursor, Main Menu, Interface, GUI 3D i Spotify 3D.
* GUI 3D i Spotify 3D są domyślnie wyłączone. Włącz je w normalnym GUI.
* G: odblokuj kursor do obsługi paneli w świecie. ESC/G: wróć do gry.
* GUI 3D: lewy przycisk wybiera moduł, prawy go przełącza. Po prawej są jego ustawienia.
  Kółko przewija listę, suwaki obsługują przeciąganie. Zwykłe GUI nadal jest dostępne.
* Spotify 3D: okładka, tytuł, wykonawca, postęp, poprzedni utwór, pauza/wznów i następny.
  Uruchom Spotify w Windows i odtwórz muzykę. Mod steruje odtwarzaczem systemowym;
  nie wymaga logowania do Spotify w Minecraft i sam nie pobiera muzyki.
* Place in front ustawia panel ponownie przed kamerą. Rozmiar, odległość, przesunięcie
  i kąt są regulowane osobno dla obu modułów. Panele zostają w miejscu w bieżącym świecie.
* Dynamic Island: profil gracza ze skinem i statystykami, panel Music z karaoke,
  sterowanie muzyką i reakcje na moduły, utwory, obrażenia oraz lot.
  Panel rozwiń kliknięciem po otwarciu czatu i wybierz Profile lub Music.
  Starsze konfiguracje Song Island są nadal obsługiwane.
* Interface → Notifications: angielskie powiadomienia o włączeniu i wyłączeniu
  modułów. Island Profile, Island Lyrics i Island Reactions sterują zawartością wyspy.
* Cursor: animowana kaczka lub nietoperz pod kursorem na ekranach menu,
  ClickGUI, czatu i ekwipunku. Wybierz Style: Duck/Bat; rozmiar, przesunięcie,
  tempo animacji, Smooth Follow i Click Pulse mają osobne ustawienia.
* Main Menu: napis StormDLC, zegar i aktualna tapeta Windows. Przycisk Wallpaper
  w menu głównym pozwala wybrać własny PNG/JPG, dołączoną tapetę, zegar i przyciemnienie.
  Dynamic Island jest widoczna także w menu; opcja Show in menus steruje tym widokiem.
* Combat: KillAura, AutoTrap i AutoWeb ze wspólnymi filtrami i sortowaniem celów.
  Domyślnie wybierają graczy; moby i zwierzęta włączasz osobno. KillAura ma
  domyślny zasięg ataku 3,6 bloku, ciche rotacje z GCD i wygładzaniem modelu,
  opcjonalną predykcję elytry oraz Target Strafe. Tryby rotacji: MineStar V1,
  MineStar V2, Polar, AC V2 i HVH; Rotation speed i Stable Target regulują zachowanie.
* Friends → Friends → Manage Friends: edytor nicków i UUID. Lista jest zapisywana
  automatycznie i wyklucza przyjaciół z działań Combat. Dostępne są także komendy
  `$friend add <nick/UUID>`, `$friend remove <nick/UUID>`, `$friend list` i `$friend clear`.
* AutoTrap używa obsydianu lub płaczącego obsydianu z hotbara, AutoWeb używa pajęczyn.
  Wymagają dostępnego miejsca, widocznej powierzchni podparcia i normalnego zasięgu
  interakcji. Mają Silent Rotations, Use Aura Target i opcjonalne Inventory Swap.
  Po każdej próbie przywracają wybrany slot. AutoTrap buduje dach z podparciem;
  AutoWeb ma osobne ustawienia nóg/głowy i opcjonalne przewidywanie ruchu.
* Konfiguracja: config/stormdlc. Ustawienia menu są zapisywane przy jego zamykaniu
  i wyłączaniu gry; pozycja Dynamic Island ma osobny zapis.
* Discord RPC → Spinning logo: rzeczywisty, zapętlony obrót logo S. Wyłączenie tej
  opcji wybiera obraz statyczny. Discord desktop musi działać z włączonym
  udostępnianiem aktywności. Szczegóły konfiguracji: STORM-DLC-2.0-PL.md.

Teksty piosenek pochodzą z usług używanych przez oryginalny Song Island.
Ich dostępność i synchronizacja zależą od utworu i zewnętrznych usług.

## Aktualizacje

Mod sprawdza publiczne wydania `RsstormxD/Storm-DLC-2.0` przy uruchomieniu i co
30 minut. Nowsze stabilne wydanie dla tej samej wersji Minecrafta wyświetla
powiadomienie oraz wiadomość na czacie z przyciskiem **Pobierz**.
Wiadomość jest widoczna tylko u Ciebie. Sprawdzanie odbywa się w tle;
brak połączenia z GitHubem nie blokuje gry. Instalację nowego JAR-a wykonujesz ręcznie.

## Budowanie i publikowanie

Java 21, oficjalne mapowania Mojang (Mojmap), Loom 1.13.6 i Gradle 8.14.
Windows: `gradlew.bat assemble -x test -x compileTestJava -x processTestResources`.
Linux: `./gradlew assemble -x test -x compileTestJava -x processTestResources`.
Wynik: `build/libs/storm-dlc-2.0.0+1.21.4.jar`.
Repozytorium aktualizacji określa `updates_repository` w `gradle.properties`;
można je też przekazać jako `-Pupdates_repository=konto/repo`.
Pusta wartość wyłącza sprawdzanie aktualizacji.

GitHub Actions buduje JAR-y bez uruchamiania testów i dołącza je do Releases
po zmianie `mod_version` na gałęzi `main`, wysłaniu tagu, np. `v2.0.2+1.21.4`,
lub opublikowaniu wydania z takim tagiem. Pierwsze przesłanie projektu na
`main` również tworzy wydanie. Workflow można też uruchomić ręcznie z zakładki Actions.
Workflow automatycznie wpisuje adres bieżącego repozytorium do moda.
Powiadomienie pojawia się dopiero, gdy wydanie ma gotowy plik
`storm-dlc-2.0.2+1.21.4.jar`. Wersje robocze i prerelease są pomijane.

Źródła i informacje o autorach/licencjach: THIRD_PARTY_NOTICES.md, LICENSE,
LICENSE-Song-Island. Nie dołączono plików gry Minecraft.
Architektura i ustawienia modułów: [STORM-DLC-MODULES-2.0-PL.md](STORM-DLC-MODULES-2.0-PL.md).
Dynamic Island, Cursor i powiadomienia: [STORM-DLC-ISLAND-2.0-PL.md](STORM-DLC-ISLAND-2.0-PL.md).
