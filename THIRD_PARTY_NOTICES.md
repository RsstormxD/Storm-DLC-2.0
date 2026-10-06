# Third-party notices

Storm DLC 2.0 combines the two archives supplied for this project.

* Astolfo Visuals 2.0: SRS. The supplied metadata declares GPL-3.0-only.
  The supplied archive contains recovered/decompiled Java sources and original assets.
  Package names and resource namespaces are retained for compatibility and attribution.
* Song Island 1.0.1: mimitokox_, https://github.com/Mimitokox/song-island-minecraft.
  The supplied LICENSE contains the GNU Affero GPL version 3 (preserved as
  LICENSE-Song-Island and LICENSE). Its fabric.mod.json says CC0-1.0; this port
  preserves the actual supplied license text and does not treat that metadata as a waiver.
* MediaPlayerInfo Java bridge and DLL: supplied Song Island archive, under
  dev.redstones.mediaplayerinfo. Upstream: https://github.com/Redstonecrafter0/MediaPlayerInfo
  The native library runs in an isolated helper JVM because the supplied DLL
  can crash during COM teardown. Main-game media access uses bounded IPC.
* MRE font atlases and shaders: preserved from Astolfo Visuals. The missing Java
  rendering library was replaced by a compatibility implementation in this project.
* JNA 5.15.0: bundled through Gradle; upstream notices are retained in its nested JAR.
* Minecraft, Fabric, Spotify and other names remain the property of their owners.
  This is an unofficial client-side integration.
* Cursor animations: bat.gif and duck.gif supplied by the user from the Excellent
  texture folder. Their original GIF files, transparency and frame timings are preserved.
* Menu wallpaper: the user's Windows desktop image, originally named
  wallpaperflare.com_wallpaper.jpg. The supplied image is bundled unchanged as
  a retained legacy background; the client can also read the current Windows wallpaper.
* Menu themes 1–3: the three JPEG images supplied by the user on 2026-10-06,
  originally named 1234dsza.jpg and the two liquid-marbling-paint-texture-background
  images (including the copy with the suffix (1)). Bundled unchanged as
  assets/stormdlc/menu/theme-1.jpg, theme-2.jpg and theme-3.jpg.
  These user-supplied images are not claimed as original Storm artwork.

The user-supplied Xyeta combat, movement, math and aura sources were inspected as
references for sensitivity quantization, aim height, movement and slot restoration.
Storm's integration uses its own Mojmap module lifecycle, friend filters, rendering
and interaction services. No Xyeta runtime, GRU model or external client dependency
is included.

Changes: Yarn mapping recovery, Minecraft 1.21.4 backport of Song Island rendering
and input APIs, Storm branding, settings integration, world-space GUI and media panels,
shared media polling, configuration persistence and normal shutdown without force-killing Minecraft.

Visual ideas in version 1.5 were independently adapted after inspecting the supplied
Rockstar Client 1.21.11 sources: Ambience color isolation and night tint, ViewModel
uniform hand scale, and Beautifully screen opening animations. These features use
Storm DLC 2.0 settings and Minecraft 1.21.4 hooks. No Rockstar source files, assets,
combat automation, movement cheats or dependencies are bundled in this adaptation.

Version 1.6 extends this visual adaptation with cloud/star tint controls (Ambience),
particle burst modes and physics controls (Kill Effects), procedural ring controls
(JumpCircleEspFeature), and an independent custom swing editor (Swing Animation).
Self Aura adapts the cosmetic flame idea to the local player only, with normal
particle occlusion. Ambient Particles and expanded hat controls are new Storm
implementations. Previously bundled Radiant and Sunset sky shaders are now exposed
in the sky selector. No Rockstar ESP, combat automation or movement modules were
ported. No additional Rockstar assets or dependencies are included.
