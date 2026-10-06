# Storm DLC 2.0 — Combat i Friends, Fabric 1.21.4 / Mojmap

Projekt używa Java 21 i oficjalnych mapowań Mojang. Nazwa pozostaje Storm DLC 2.0,
wersja metadanych bieżącego JAR-a to `2.0.1+1.21.4`.

## Obsługa

Kategorie **Combat** i **Friends** są dostępne w nowoczesnym i klasycznym ClickGUI,
również w panelu GUI 3D. Ustawienia otwierasz tak jak w istniejących modułach.
**Friends → Friends → Manage Friends** otwiera edytor nazw i UUID, z dodawaniem
gracza spod celownika, usuwaniem, stronicowaniem i czyszczeniem listy.

Lista w `config/stormdlc/friends.json` działa niezależnie od przełączników modułów.
Nazwy są normalizowane przez `Locale.ROOT`; sprawdzane są zarówno nick, jak i UUID.
Dodanie gracza z celownika zapisuje oba identyfikatory. Usunięcie identyfikatora
gracza obecnego w świecie usuwa oba wpisy. Dla gracza offline usuń zapisany nick
oraz UUID z listy. Komendy: `$friend add <nick/UUID>`, `$friend remove <nick/UUID>`,
`$friend list`, `$friend clear`.

## Ustawienia i zachowanie

| Ustawienie | Domyślnie | Działanie |
| --- | --- | --- |
| Target Players / Mobs / Animals | true / false / false | Wspólne filtry Combat, zawsze z pominięciem przyjaciół |
| Search Range | 8,0 | Odległość od oczu do najbliższego punktu AABB celu |
| Sort Mode | DISTANCE | DISTANCE, HEALTH z absorpcją, ARMOR; stabilne rozstrzyganie remisów |
| Stable Target | true | Histereza ogranicza przeskakiwanie między celami o zbliżonych wynikach |
| Attack Range | 3,6 | KillAura; kontrola zasięgu bieżącego AABB i promienia ataku |
| Target ESP | true | Obrys wybranego celu |
| Rotation Mode | MINESTAR_V1 | MineStar V1, MineStar V2, Polar, AC V2 i HVH; każdy korzysta z GCD |
| Rotation speed | 0,45 | Tempo obrotu wszystkich trybów poza HVH; stan zachowuje ruchy poniżej kroku myszy |
| Visual Rotations | true | Wizualne obroty modelu bez zmiany yaw/pitch lokalnej kamery |
| Elytra Predict / Prediction Ticks | false / 1,5 | Ekstrapolacja dla lotu celu albo lokalnego gracza; zakres 0–5 ticków |
| True Position ESP | false | Zielony obrys przewidywanego AABB |
| Movement | Follow Target | Follow Target, Target Strafe lub Off; sterowanie kierunkiem niezależne od kamery |
| Follow Distance / Movement Prediction | 2,3 / 1,5 | Dystans zatrzymania i krótkie wyprzedzenie ruchu celu |
| Follow Auto Jump / Manual Movement Override | true / true | Skoki na pojedyncze bloki i ręczne przejęcie sterowania klawiszami |
| Auto Sprint / Criticals Sync | false / false | Opcjonalne sterowanie sprintem i oczekiwanie na fazę opadania |
| Strafe Distance / Smart Criticals / No Eat Attack | 2,8 / true / true | Odległość orbity, krytyki przy skoku i pomijanie ataków podczas używania przedmiotów |
| Elytra Boost / Min Speed | false / 0,85 | Opcjonalne użycie fajerwerku z przywróceniem slotu |
| Place Range / Place Delay / Blocks Per Tick | 4,5 / 2 / 1 | AutoTrap/AutoWeb; zasięg ogranicza też atrybut interakcji gracza |
| Silent Rotations / Use Aura Target / Inventory Swap | true / true / false | Rotacja przy stawianiu, priorytet celu KillAura i tymczasowa podmiana przedmiotów |

Każdy atak wymaga `getAttackStrengthScale(0.5f) >= 0.92f`, ważnego celu,
widoczności i zgodności promienia z bieżącą skrzynką. Pakiet rotacji jest
wysyłany przed zwykłym `gameMode.attack`, po czym następuje swing i pakiet
przywracający kąty kamery. Kod nie wywołuje `setYRot` ani `setXRot` na lokalnym
graczu. W trybie wizualnym mixin czasowo zmienia kąty modelu, wykonuje pobranie
stanu renderowania i przywraca pola w `finally`.

Predykcja korzysta z `getDeltaMovement()`, pomiaru przemieszczenia, oporu powietrza,
grawitacji i ograniczonej poprawki na ping. Sprawdza kolizje bloków w każdym kroku.
To oszacowanie pozycji klienta, a nie dostęp do przyszłego stanu serwera; nie może
zagwarantować przyjęcia każdego ataku przez serwer.

AutoTrap układa ściany wokół AABB i opcjonalny dach. Wybierz obsydian lub płaczący
obsydian w **Allowed Blocks**. AutoWeb układa pajęczyny przy nogach i opcjonalnie
na wysokości głowy. Blok musi być w hotbarze lub ekwipunku przy włączonym
**Inventory Swap**; pozycja musi być pusta, załadowana,
wewnątrz granicy świata i wysokości budowania. Interakcja wymaga widocznej,
osiągalnej powierzchni podparcia, prawidłowego `BlockPlaceContext` i dozwolonej
kolizji bloku. Pajęczyna może zajmować przestrzeń celu; pozycje zajęte przez
przyjaciół są pomijane. Wspólny limit wynosi 4 próby interakcji na tick, z
krótkim buforem powtórzeń. Slot jest synchronizowany i przywracany w `finally`.
Interakcja czasowo włącza secondary use, aby stawiać na blokach interaktywnych;
stan kucania i odpowiednie pakiety są również przywracane w `finally`.

## Architektura źródeł

| Klasa | Odpowiedzialność |
| --- | --- |
| `Module` / `ModuleManager` | Cykl życia na wątku klienta, dispatch, zarządzanie zasobami i reset kontekstu |
| `FriendsManager` / `FriendManager` | Reaktywny `Set<String>`, atomowy zapis i zgodność istniejących komend |
| `FriendsScreen` / `FriendsModule` | Edytor danych i integracja z kategorią Friends |
| `TargetingModule` / `TargetSelector` | Filtry, priorytet, sortowanie i ponowna walidacja celu |
| `SilentRotations` / `VisualRotations` / `VisualRotationMixin` | Niezależne kąty ataku i bezpieczne rotacje modelu |
| `RotationMath` / `RotationPackets` | Kwantyzacja myszy, wygładzanie i przywracanie kierunku w pakietach |
| `TrajectoryPredictor` / `TargetBoxRenderer` | Predykcja 3D i obrysy AABB |
| `MovementControl` / `GroundPathPlanner` | Predykcja podążania, ograniczone A*, kontrola kolizji i własność wymuszonych klawiszy |
| `HotbarSlots` | Przywracanie slotu i synchronizacja interakcji |
| `BlockGrid` / `BlockPlacementService` / `BlockPlacementModule` | Siatka pozycji, walidacja i ograniczanie interakcji |
| `KillAuraModule` / `AutoTrapModule` / `AutoWebModule` | Konkretny cykl działania modułów Combat |

Listenery Fabric są rejestrowane centralnie. Moduły nie rejestrują ponownie
eventów po każdym włączeniu. Subskrypcje przyjaciół należą do zakresu modułu
i są zamykane przy wyłączeniu, także po błędzie sprzątania. Zmiana świata,
respawn, śmierć, brak gracza lub otwarte GUI zerują stan działania. Nie ma
warunku ograniczającego Combat do singleplayera.

Konfiguracja zapisuje również zagnieżdżone ustawienia. Przy ładowaniu najpierw
wyłącza moduł, ustawia wartości i dopiero potem przywraca stan włączenia.
Starsze `MineStar V3` przechodzi na `MINESTAR_V2`, a `Elytra Mode` na `Elytra Boost`.
Włączone starsze `Target Lock` lub `Target Strafe` wybiera `Movement: Target Strafe`.
Nowa instalacja domyślnie wybiera `Movement: Follow Target`. Lista Friends,
filtry celów i czyszczenie stanu obowiązują również podczas podążania.
Ruch korzysta z normalnych klawiszy, uwzględnia kierunek kamery, widoczność celu,
kolizje, podłoże, niebezpieczne bloki i trasę w załadowanych chunkach.
Opis menu i ruchu: [STORM-DLC-UI-2.0-PL.md](STORM-DLC-UI-2.0-PL.md).
Powiadomienia, wyspa i Cursor: [STORM-DLC-ISLAND-2.0-PL.md](STORM-DLC-ISLAND-2.0-PL.md).

Budowanie bez testów:
`gradlew.bat assemble -x test -x compileTestJava -x processTestResources`.
JAR: `build/libs/storm-dlc-2.0.1+1.21.4.jar`.
