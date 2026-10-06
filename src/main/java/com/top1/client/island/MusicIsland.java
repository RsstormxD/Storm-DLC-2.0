package com.top1.client.island;

import com.top1.client.island.font.Font;
import com.top1.client.island.font.Fonts;
import com.top1.client.island.render.IslandRender;
import com.top1.client.music.LyricsFetcher;
import com.top1.client.music.MusicTracker;
import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import java.util.List;
import dev.stormdlc.hud.ClientFeedback;
import dev.stormdlc.hud.IslandProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public class MusicIsland {
	private static final float PILL_HEIGHT = 32.0F;
    private static final float PILL_MIN_WIDTH = 176.0F;
	private static final float PILL_MAX_WIDTH = 208.0F;
	private static final float WORLD_PANEL_WIDTH = 164.0F;
	private static final float LYRIC_STEP = 9.0F;
	private static final int WHITE = 0xFFFFFFFF;
    private int accent() {
        var type = rendering3D ? xyz.angames.astolfoclient.client.module.modules.render.Spotify3DModule.class
                              : xyz.angames.astolfoclient.client.module.modules.render.SongIslandModule.class;
        return xyz.angames.astolfoclient.client.config.VisualColors.resolve(
            xyz.angames.astolfoclient.client.module.ModuleManager.getModule(type), WHITE, 0L);
    }
	private static final float TEXT_TAIL = 10.0F;
	private static final float TEXT_LEAD = 6.0F;
	private static final float SETTINGS_HEIGHT = 108.0F;
    private static final float TAB_HEIGHT = 22.0F;
    private boolean profileSelected = true;
    private final Anim reactionPresence = new Anim(260), reactionChange = new Anim(240);
    private ClientFeedback.Notice displayedReaction, previousReaction;
    private long feedbackEpoch = Long.MIN_VALUE;
    private boolean mediaAvailable;
    private String lastMediaKey = "";
    private Boolean lastPlaying;
    private java.util.UUID mediaPlayer;
    public boolean rendering3D = false;
    public boolean showLyrics = true;

	private final MusicTracker tracker;
	private final IslandSize size = new IslandSize(PILL_MIN_WIDTH, PILL_HEIGHT);
	private boolean expanded;
	private final Anim extendAnim = new Anim(300L);
	private final Anim widthAnim = new Anim(620L);
	private final Anim heightAnim = new Anim(620L);
	private final Anim pausingAnim = new Anim(400L);
	private final float[] waveValues = new float[4];
	private long waveFrame;
	private final Anim lyricScroll = new Anim(420L);
	private final Anim pillScroll = new Anim(520L);
	private final Anim expandedScroll = new Anim(520L);
	private final WordScales pillScales = new WordScales();
	private final WordScales expandedScales = new WordScales();
	private boolean settingsOpen;
	private final Anim settingsAnim = new Anim(320L);
	private float manualScroll;
	private long manualScrollAt;
	private String currentLine = "";
	private String previousLine = "";
	private long lineChanged = -10000L;

	public MusicIsland(MusicTracker tracker) {
		this.tracker = tracker;
        widthAnim.snap(PILL_MIN_WIDTH);
        heightAnim.snap(PILL_HEIGHT);
	}

	public void render() {
        Minecraft mc = Minecraft.getInstance();
        if (rendering3D && mc.player == null || mc.player != null && mc.options.hideGui) return;
        var module = xyz.angames.astolfoclient.client.module.modules.render.SongIslandModule.INSTANCE;
        if (!rendering3D && mc.player == null && module != null && !module.showMenus.get()) return;
        boolean interactive = isInteractive(mc) || mc.screen instanceof DragScreen;
        if (!rendering3D && !interactive) { expanded = false; settingsOpen = false; }
        if (rendering3D) expanded = true;
        IMediaSession session = tracker.getSession();
        MediaInfo media = session == null ? null : session.getMedia();
        mediaAvailable = media != null;
        if (!rendering3D) updateMediaReaction(mc, session, media);
        ClientFeedback.Notice reaction = rendering3D ? null : ClientFeedback.reaction();
        if (feedbackEpoch != ClientFeedback.epoch()) {
            displayedReaction = previousReaction = null;
            reactionPresence.snap(0);
            reactionChange.snap(1);
            feedbackEpoch = ClientFeedback.epoch();
        }
        if (reaction != displayedReaction) {
            previousReaction = displayedReaction != null ? displayedReaction : previousReaction;
            displayedReaction = reaction;
            reactionChange.snap(0);
        }
        reactionPresence.update(reaction == null ? 0 : 1);
        reactionChange.update(1);
        float reactionAlpha = net.minecraft.util.Mth.clamp(reactionPresence.get(), 0, 1);
        float reactionBlend = net.minecraft.util.Mth.clamp(reactionChange.get(), 0, 1);
        ClientFeedback.Notice reactionContent = displayedReaction == null ? previousReaction : displayedReaction;
        extendAnim.update(expanded ? 1.0F : 0.0F);
        float ext = rendering3D ? 1.0F : net.minecraft.util.Mth.clamp(extendAnim.get(), 0, 1);
        float surfaceExt = net.minecraft.util.Mth.lerp(reactionAlpha, ext, .25F);
        float desiredWidth = net.minecraft.util.Mth.lerp(reactionAlpha, targetWidth(session), reactionContent == null ? targetWidth(session) : reactionWidth(reactionContent));
        float desiredHeight = net.minecraft.util.Mth.lerp(reactionAlpha, targetHeight(), 42);
        widthAnim.update(desiredWidth);
        heightAnim.update(desiredHeight);
        size.width = rendering3D ? panelWidth() : widthAnim.get();
        size.height = rendering3D ? targetHeight() : heightAnim.get();
        float x = islandX(mc), y = islandY(mc);
        float radius = rendering3D ? 18 : Math.min(size.height / 2, 16 + 6 * surfaceExt);
        IslandRender.drawRoundedRect(x - 2, y + 2, size.width + 4, size.height + 2,
            radius + 2, radius + 2, radius + 2, radius + 2, 0x34000000);
        int border = reactionContent == null ? 0x38FFFFFF : blendColor(0x38FFFFFF, color(reactionContent.tone().color(), 100), reactionAlpha);
        IslandRender.drawRoundedRect(x - 0.6F, y - 0.6F, size.width + 1.2F, size.height + 1.2F,
            radius + 0.6F, radius + 0.6F, radius + 0.6F, radius + 0.6F, border);
        IslandRender.drawRoundedRect(x, y, size.width, size.height,
            radius, radius, radius, radius, 0xFA050507);
        if (reactionAlpha < .998F && media != null) {
            IslandRender.pushOpacity(1 - reactionAlpha);
            try { drawWaves(x, y, media, ext); } finally { IslandRender.popOpacity(); }
        }
        IslandRender.pushScissor(x, y, size.width, size.height);
        try {
            if (reactionAlpha > .002F && reactionContent != null) {
                if (displayedReaction != null && previousReaction != null && reactionBlend < .998F) {
                    IslandRender.pushOpacity(reactionAlpha * (1 - reactionBlend));
                    try { drawReaction(x, y - 4 * reactionBlend, previousReaction); }
                    finally { IslandRender.popOpacity(); }
                }
                IslandRender.pushOpacity(reactionAlpha * (displayedReaction == null ? 1 : reactionBlend));
                try { drawReaction(x, y + (displayedReaction == null ? 0 : 4 * (1 - reactionBlend)), reactionContent); }
                finally { IslandRender.popOpacity(); }
            }
            if (reactionAlpha < .998F) {
                IslandRender.pushOpacity(1 - reactionAlpha);
                try {
                if (!rendering3D && ext > 0.01F && profileEnabled()) drawTabs(x, y, ext);
                float contentY = y + topInset() * ext;
                boolean profile = !rendering3D && profileEnabled() && (profileSelected || media == null);
                if (!rendering3D && ext < 0.995F && media == null && profileEnabled())
                    IslandProfile.drawPill(mc, x, y, size.width, 255 * (1 - ext));
                if (profile && ext > 0.005F)
                    IslandProfile.drawExpanded(mc, x, contentY, panelWidth(), 255 * ext);
                if (media != null) {
                    if (!profile || ext < 0.995F) drawCover(x, profile ? y : contentY, profile ? 0 : ext, profile ? 255 * (1 - ext) : 255);
                    if (ext < 0.995F) drawPill(x, y, media, ext);
                    if (!profile && ext > 0.005F) drawExpanded(x, contentY, session, media, ext);
                } else if (!profileEnabled() || rendering3D) {
                    Font font = Fonts.medium(7);
                    IslandRender.drawCenteredText(font, "Play music to see lyrics",
                        x + size.width / 2, y + (size.height - 7) / 2, 0x99FFFFFF);
                }
                } finally { IslandRender.popOpacity(); }
            }
        } finally { IslandRender.popScissor(); }
        boolean musicSettings = reactionAlpha < .01F && (!profileEnabled() || !profileSelected) && media != null;
        settingsAnim.update(settingsOpen && ext > 0.5F && musicSettings ? 1.0F : 0.0F);
        if (settingsAnim.get() > 0.01F)
            drawSettings(x, settingsY(mc, y), 255 * ext, settingsAnim.get(), settingsAbove(mc, y));
    }

    private boolean profileEnabled() {
        var settings = ClientFeedback.settings();
        return !rendering3D && (settings == null || settings.islandProfile.get());
    }

    private boolean lyricsEnabled() {
        var settings = ClientFeedback.settings();
        return rendering3D ? showLyrics : settings == null || settings.islandLyrics.get();
    }

    private float topInset() { return profileEnabled() ? TAB_HEIGHT : 0; }

    private void drawTabs(float x, float y, float ext) {
        float alpha = 255 * ext;
        boolean selected = profileSelected || !mediaAvailable;
        for (int index = 0; index < 2; index++) {
            float tabWidth = (size.width - 24) / 2;
            float tabX = x + 10 + index * (tabWidth + 4);
            boolean active = selected == (index == 0);
            if (active) IslandRender.drawRoundedRect(tabX, y + 4, tabWidth, 15, 7, 7, 7, 7, color(0xff24242a, alpha));
            IslandRender.drawCenteredText(Fonts.medium(7), index == 0 ? "Profile" : "Music",
                tabX + tabWidth / 2, y + 8, color(active ? 0xffc3ddff : 0xff8292ac, alpha));
        }
    }

    private float reactionWidth(ClientFeedback.Notice notice) {
        return Math.min(panelWidth(), Math.max(PILL_MIN_WIDTH, Math.max(Fonts.medium(8).width(notice.title()),
            Fonts.regular(6.5F).width(notice.message())) + 48));
    }

    private void drawReaction(float x, float y, ClientFeedback.Notice notice) {
        float alpha = 255;
        float pulse = (float) (Math.sin(notice.age() * 0.014) * Math.exp(-notice.age() / 500.0) * 1.5);
        float circle = 10 + pulse;
        IslandRender.drawRoundedRect(x + 13 - pulse / 2, y + 16 - pulse / 2, circle, circle,
            circle / 2, circle / 2, circle / 2, circle / 2, color(notice.tone().color(), alpha));
        IslandRender.drawWindowedText(Fonts.medium(8), notice.title(), x + 32, y + 10,
            color(0xfff3f7ff, alpha), x + 31, x + size.width - 12, 5, 0);
        IslandRender.drawWindowedText(Fonts.regular(6.5F), notice.message(), x + 32, y + 25,
            color(0xffa8a8b2, alpha), x + 31, x + size.width - 12, 5, 0);
        IslandRender.drawRoundedRect(x + 9, y + size.height - 3, (size.width - 18) * notice.remaining(), 1,
            0.5F, 0.5F, 0.5F, 0.5F, color(notice.tone().color(), alpha * 0.65F));
    }

    private void updateMediaReaction(Minecraft mc, IMediaSession session, MediaInfo media) {
        java.util.UUID identity = mc.player == null ? mc.getUser().getProfileId() : mc.player.getUUID();
        if (!identity.equals(mediaPlayer)) {
            mediaPlayer = identity;
            lastMediaKey = "";
            lastPlaying = null;
        }
        if (media == null) { lastMediaKey = ""; lastPlaying = null; return; }
        String key = session.getOwner() + "|" + media.getTitle() + "|" + media.getArtist();
        if (!key.equals(lastMediaKey))
            ClientFeedback.react("music:track", "Now playing", media.getTitle() + " - " + media.getArtist(),
                ClientFeedback.Tone.INFO, 2600, 40);
        else if (lastPlaying != null && lastPlaying != media.isPlaying())
            ClientFeedback.react("music:playback", media.isPlaying() ? "Music resumed" : "Music paused",
                media.getTitle(), ClientFeedback.Tone.INFO, 1700, 30);
        lastMediaKey = key;
        lastPlaying = media.isPlaying();
    }

    public void reset() {
        displayedReaction = previousReaction = null;
        reactionPresence.snap(0);
        reactionChange.snap(1);
        expanded = settingsOpen = false;
        profileSelected = true;
        mediaAvailable = false;
        lastMediaKey = currentLine = previousLine = "";
        mediaPlayer = null;
        lastPlaying = null;
        manualScroll = 0;
        manualScrollAt = 0;
        lyricScroll.snap(0);
        pillScroll.snap(0);
        expandedScroll.snap(0);
    }

	private float islandX(Minecraft mc) {
        if(rendering3D) return 4.0F;
		float sw = mc.getWindow().getGuiScaledWidth();
        boolean menu = mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen;
        float center = !menu && IslandSettings.isMoved() ? IslandSettings.posX : sw / 2.0F;
        center = net.minecraft.util.Mth.clamp(center, size.width / 2 + 2, sw - size.width / 2 - 2);
		return center - size.width / 2.0F;
	}

    private static int blendColor(int from, int to, float t) {
        int result = 0;
        for (int shift = 0; shift <= 24; shift += 8)
            result |= Math.round(net.minecraft.util.Mth.lerp(t, from >>> shift & 255, to >>> shift & 255)) << shift;
        return result;
    }

	private float islandY(Minecraft mc) {
        if(rendering3D) return 4.0F;
        boolean menu = mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen;
        float base = !menu && IslandSettings.isMoved() ? IslandSettings.posY : 8.0F;
		float maxY = mc.getWindow().getGuiScaledHeight() - size.height - 2.0F;
		return Math.max(2.0F, Math.min(base, maxY));
	}

	public float centerX() {
		Minecraft mc = Minecraft.getInstance();
        return islandX(mc) + size.width / 2;
	}

	public float topY() {
		return islandY(Minecraft.getInstance());
	}

	public float width() {
		return size.width;
	}

	public float surfaceHeight(){return targetHeight()+8+(settingsOpen?112:0);}

	public float height() {
		return size.height;
	}

	public boolean containsPoint(double mouseX, double mouseY) {
		float x = centerX() - size.width / 2.0F;
		return hovered(x, topY(), size.width, size.height, mouseX, mouseY);
	}

    private void drawCover(float x, float y, float ext, float alpha) {
		float margin = 8.0F + 4.0F * ext;
		float imageSize = 18.0F + 8.0F * ext;
		float imageY = y + (ext > 0.01F ? margin : (PILL_HEIGHT - imageSize) / 2.0F);
		ResourceLocation cover = tracker.getImage();
		ResourceLocation image = cover != null ? cover : IslandRender.id("icons/music/no_image.png");
		float r = 5.0F + ext;
        IslandRender.drawRoundedTexture(image, x + margin, imageY, imageSize, imageSize, r, r, r, r, color(WHITE, alpha));
	}

	private void drawPill(float x, float y, MediaInfo media, float ext) {
		Font font = Fonts.medium(8.0F);
        Font bigFont = Fonts.medium(8.8F);
        String line = pillLine(media);
        float x1 = x + 34.0F;
        float x2 = x + size.width - 28.0F;
        float textY = y + 8.0F;
		int alpha = (int) (255.0F * (1.0F - ext));
		if(alpha <= 2) return;
        String subtitle = (media.isPlaying() ? "NOW PLAYING" : "PAUSED") + "  ·  "
            + java.util.Objects.toString(media.getArtist(), "Unknown artist");
        IslandRender.drawWindowedText(Fonts.regular(5.5F), subtitle, x1, y + 21,
            color(0xff9999a4, alpha), x1 - 1, x2, 4, 0);

		float slide = Math.min(1.0F, (System.currentTimeMillis() - lineChanged) / 420.0F);
		slide = 1.0F - (1.0F - slide) * (1.0F - slide) * (1.0F - slide);
		if(slide < 1.0F && !previousLine.isEmpty()){
			drawGhostedLine(font, previousLine, x1, textY - slide * 10.0F,
				x1, x2, alpha * (1.0F - slide), slide);
			drawKaraokeLine(font, bigFont, line, x1, textY + (1.0F - slide) * 10.0F,
				x1, x2, alpha * slide, media, pillScroll, pillScales);
		}else{
			drawKaraokeLine(font, bigFont, line, x1, textY,
				x1, x2, alpha, media, pillScroll, pillScales);
		}
	}

	private void drawKaraokeLine(Font font, Font bigFont, String line, float x, float y,
		float x1, float x2, float alpha, MediaInfo media, Anim scrollAnim, WordScales scales) {
		if(line.isEmpty()) return;
		List<LyricsFetcher.LyricLine> lines = tracker.getLyrics();
        boolean synced = lyricsEnabled() && !lines.isEmpty() && lines.get(0).time() >= 0.0F;
		float position = tracker.smoothPosition();
		int index = synced ? LyricsFetcher.currentIndex(lines, position) : -1;

		if(!synced || index < 0){
			float window = x2 - x1;
			float lineWidth = font.width(line);
			float offset = marquee(lineWidth, window);
			boolean overflow = lineWidth > window;
			float left = overflow ? Math.max(0.0F, Math.min(7.0F, offset * 1.6F)) : 0.0F;
			float right = overflow
				? Math.max(0.0F, Math.min(7.0F, (lineWidth - window + 2.0F - offset) * 1.6F))
				: 0.0F;
			IslandRender.drawWindowedText(font, line, x - offset, y, color(accent(), alpha),
				x1 - 1.0F, x2, right, left);
			return;
		}

		float progress = Karaoke.lineProgress(lines, index, position);
		float sung = Karaoke.sungChars(line, progress);
		List<Karaoke.Word> words = scales.words(line);
		if(words.isEmpty()) return;
		int curWord = Karaoke.activeWord(words, sung);
		scales.update(curWord);

		float baseSize = font.getSize();
		float bigSize = bigFont.getSize();
		float window = x2 - x1;
		float lineWidth = font.width(line);
		float target = 0.0F;
		if(lineWidth > window - TEXT_TAIL){
			float wordStartX = font.width(line.substring(0, words.get(curWord).start()));
			float wordWidth = font.width(line.substring(words.get(curWord).start(), words.get(curWord).end()));
			target = wordStartX + wordWidth / 2.0F - window / 2.0F + TEXT_LEAD;
			target = Math.max(0.0F, Math.min(lineWidth - window + TEXT_TAIL, target));
		}
		float entry = scales.entry();
		if(entry < 1.0F) scrollAnim.snap(target);
		scrollAnim.update(target);
		float scroll = scrollAnim.get();
		boolean scrolling = lineWidth > window - TEXT_TAIL;
		float fadeLeft = scrolling ? Math.max(0.0F, Math.min(7.0F, scroll * 1.6F)) : 0.0F;
		float fadeRight = scrolling
			? Math.max(0.0F, Math.min(7.0F, (lineWidth - window + TEXT_TAIL - scroll) * 1.6F))
			: 0.0F;
		float blur = Math.min(3.5F, Math.abs(scrollAnim.delta()) * 1.4F);
		float drawX = x - scroll;

		alpha *= entry;
		y += (1.0F - entry) * 5.0F;
		int litColor = color(accent(), alpha);
		int grayColor = color(accent(), alpha * 0.32F);

		float shift = 0.0F;
		for(int i = 0; i < words.size(); i++){
			Karaoke.Word word = words.get(i);
			String text = line.substring(word.start(), word.end());
			float wordX = drawX + font.width(line.substring(0, word.start()));
			float scale = scales.scale(i);
			float size = baseSize + (bigSize - baseSize) * scale;
			Font wordFont = Fonts.medium(size);
			float baseWidth = font.width(text);
			float bigWidth = wordFont.width(text);
			float renderX = wordX + shift;
			shift += bigWidth - baseWidth;
			float renderY = y - scale * 0.5F;
			int[] colors = Karaoke.colors(text, sung - word.start(), litColor, grayColor);

			float ghostPower = Math.min(1.0F, blur / 2.5F);
			if(ghostPower > 0.02F){
				int[] ghost = Karaoke.colors(text, sung - word.start(),
					Karaoke.withAlpha(litColor, 0.22F * ghostPower),
					Karaoke.withAlpha(grayColor, 0.22F * ghostPower));
				IslandRender.drawKaraokeText(wordFont, text, renderX + blur, renderY, ghost,
					x1 - 1.0F, x2, fadeRight, fadeLeft);
				IslandRender.drawKaraokeText(wordFont, text, renderX + blur * 0.5F, renderY,
					Karaoke.colors(text, sung - word.start(),
						Karaoke.withAlpha(litColor, 0.35F * ghostPower),
						Karaoke.withAlpha(grayColor, 0.35F * ghostPower)),
					x1 - 1.0F, x2, fadeRight, fadeLeft);
			}
			IslandRender.drawKaraokeText(wordFont, text, renderX, renderY, colors,
				x1 - 1.0F, x2, fadeRight, fadeLeft);
		}
	}

	private static float marquee(float contentWidth, float window) {
		float overflow = contentWidth - window + 2.0F;
		if(overflow <= 0.0F) return 0.0F;
		float travel = 2400.0F + overflow * 90.0F;
		float hold = 1600.0F;
		float cycle = (travel + hold) * 2.0F;
		float t = System.currentTimeMillis() % (long) cycle;
		float phase;
		if(t < hold) phase = 0.0F;
		else if(t < hold + travel) phase = (t - hold) / travel;
		else if(t < hold * 2.0F + travel) phase = 1.0F;
		else phase = 1.0F - (t - hold * 2.0F - travel) / travel;
		phase = phase * phase * (3.0F - 2.0F * phase);
		return overflow * phase;
	}

	private void drawGhostedLine(Font font, String line, float x, float y,
		float x1, float x2, float alpha, float slide) {
		if(line.isEmpty() || alpha <= 2.0F) return;
		float travel = (1.0F - slide) * 6.0F;
		if(travel > 0.4F){
			IslandRender.drawWindowedText(font, line, x, y - travel, color(accent(), alpha * 0.25F),
				x1 - 1.0F, x2, 7.0F, 0.0F);
		}
		IslandRender.drawWindowedText(font, line, x, y, color(accent(), alpha),
			x1 - 1.0F, x2, 7.0F, 0.0F);
	}

	private void drawExpanded(float x, float y, IMediaSession session, MediaInfo media, float ext) {
		Font title = Fonts.medium(7.0F);
		Font regular = Fonts.regular(6.5F);
		Font small = Fonts.regular(5.0F);
        String titleText = java.util.Objects.toString(media.getTitle(), "Unknown track");
        String artistText = java.util.Objects.toString(media.getArtist(), "Unknown artist");
		int alpha = (int) (255.0F * ext);
		float textX = x + 46.0F;
		float textWidth = panelWidth() - 58.0F;
		float height = targetHeight() - topInset();

        boolean titleOverflow = title.width(titleText) > textWidth;
        boolean artistOverflow = regular.width(artistText) > textWidth;
        IslandRender.drawWindowedText(title, titleText, textX, y + 13.0F,
			color(accent(), alpha), textX - 1.0F, textX + textWidth, titleOverflow ? 8.0F : 0.0F, 0.0F);
        IslandRender.drawWindowedText(regular, artistText, textX, y + 23.0F,
			color(accent(), alpha * 0.65F), textX - 1.0F, textX + textWidth, artistOverflow ? 8.0F : 0.0F, 0.0F);

		float barWidth = panelWidth() - 48.0F;
		float barX = x + (panelWidth() - barWidth) / 2.0F;
		float barY = y + 42.0F;
		IslandRender.drawText(small, MusicTracker.formatTime((long) tracker.smoothPosition()),
			barX, barY + 5.0F, color(accent(), alpha * 0.7F));
		String duration = MusicTracker.formatTime(media.getDuration());
		IslandRender.drawText(small, duration,
			barX + barWidth - small.width(duration), barY + 5.0F, color(accent(), alpha * 0.7F));
		IslandRender.drawRoundedRect(barX, barY, barWidth, 3.0F, 1.5F, 1.5F, 1.5F, 1.5F, color(accent(), alpha * 0.22F));
		float progress = media.getDuration() > 0
			? Math.min(1.0F, tracker.smoothPosition() / (float) media.getDuration()) : 0.0F;
		if(progress > 0.0F){
			IslandRender.drawRoundedRect(barX, barY, barWidth * progress, 3.0F,
				1.5F, 1.5F, 1.5F, 1.5F, color(accent(), alpha * 0.85F));
		}

		if(lyricsEnabled()) drawLyrics(x, y, media, ext, height);
        if (lyricsEnabled() && tracker.getLyrics().isEmpty()) IslandRender.drawCenteredText(Fonts.regular(5.5F),
            "Lyrics unavailable for this track", x + panelWidth() / 2, y + 57, color(0xff8292ac, alpha));

		float controlY = y + height - 24.0F;
		float mid = x + panelWidth() / 2.0F;
		pausingAnim.update(media.isPlaying() ? 1.0F : 0.0F);
		float pause = pausingAnim.get();
		IslandRender.drawTexture(IslandRender.id("icons/music/previous.png"), mid - 40.0F, controlY, 16.0F, 16.0F, color(accent(), alpha));
		IslandRender.drawTexture(IslandRender.id("icons/music/play.png"), mid - 8.0F, controlY, 16.0F, 16.0F,
			color(accent(), alpha * (1.0F - pause)));
		IslandRender.drawTexture(IslandRender.id("icons/music/pause.png"), mid - 8.0F, controlY, 16.0F, 16.0F,
			color(accent(), alpha * pause));
		IslandRender.drawTexture(IslandRender.id("icons/music/next.png"), mid + 24.0F, controlY, 16.0F, 16.0F, color(accent(), alpha));

		String owner = ownerIcon(session);
		if(owner != null){
			IslandRender.drawTexture(IslandRender.id("icons/media/" + owner + ".png"),
				x + 14.0F, y + height - 20.0F, 8.0F, 8.0F, color(accent(), alpha * 0.8F));
		}

		float dotsX = x + panelWidth() - 20.0F;
		float dotsY = y + height - 15.0F;
		int dotsColor = color(accent(), alpha * (settingsOpen ? 0.9F : 0.4F));
		for(int i = 0; i < 3; i++){
			IslandRender.drawRoundedRect(dotsX + i * 4.0F, dotsY, 2.4F, 2.4F,
				1.2F, 1.2F, 1.2F, 1.2F, dotsColor);
		}

	}

	private boolean settingsAbove(Minecraft mc, float islandTop) {
		return !rendering3D && islandTop + size.height + 4.0F + SETTINGS_HEIGHT > mc.getWindow().getGuiScaledHeight();
	}

	private float settingsY(Minecraft mc, float islandTop) {
		return settingsAbove(mc, islandTop)
			? islandTop - SETTINGS_HEIGHT - 4.0F
			: islandTop + size.height + 4.0F;
	}

	private void drawSettings(float x, float y, float alpha, float open, boolean above) {
		float width = panelWidth();
		float full = SETTINGS_HEIGHT;
		float height = full * (0.55F + 0.45F * open);
		float bgAlpha = alpha * open;
		float slide = (1.0F - open) * 6.0F;
		y += above ? slide : -slide;
		if(above) y += full - height;

		IslandRender.drawSquircle(x - 1.0F, y - 1.0F, width + 2.0F, height + 2.0F, 6.0F,
			11.0F, 11.0F, 11.0F, 11.0F, color(accent(), bgAlpha * 0.1F));
		IslandRender.drawBlur(x, y, width, height, 45.0F, 4.0F, 10.0F, 10.0F, 10.0F, 10.0F,
			color(accent(), 255.0F * open));
		IslandRender.drawSquircle(x, y, width, height, 6.0F, 10.0F, 10.0F, 10.0F, 10.0F,
			color(0xFF101010, bgAlpha * 0.85F));

		if(open < 0.35F) return;
		float content = (open - 0.35F) / 0.65F;
		float a2 = bgAlpha * content;

		Font header = Fonts.medium(7.0F);
		Font item = Fonts.regular(6.5F);
		IslandRender.drawText(header, "Settings", x + 12.0F, y + 8.0F, color(accent(), a2));

		IslandRender.drawText(item, "Lyric delay", x + 12.0F, y + 21.0F, color(accent(), a2 * 0.55F));
		String offset = String.format("%+.1fs", tracker.getLyricsOffset());
		IslandRender.drawText(item, offset, x + width - 12.0F - item.width(offset), y + 21.0F,
			color(accent(), a2 * 0.8F));

		IslandRender.drawText(item, "Manual Map Music", x + 12.0F, y + 33.0F, color(accent(), a2 * 0.55F));
		boolean mappable = !tracker.getRawLyrics().isEmpty();
		String mapState = mappable ? "OPEN" : "NO LYRICS";
		IslandRender.drawText(item, mapState, x + width - 12.0F - item.width(mapState), y + 33.0F,
			color(mappable ? 0xFF8FC7E3 : 0xFFE38F8F, a2 * 0.9F));

		IslandRender.drawText(item, "[BETA] Text in 3d", x + 12.0F, y + 45.0F, color(accent(), a2 * 0.55F));
		String worldState = IslandSettings.worldText ? "ON" : "OFF";
		IslandRender.drawText(item, worldState, x + width - 12.0F - item.width(worldState), y + 45.0F,
			color(IslandSettings.worldText ? 0xFF9BE38F : 0xFFFFFFFF, a2 * 0.9F));

		IslandRender.drawText(item, "Hide bossbar", x + 12.0F, y + 57.0F, color(accent(), a2 * 0.55F));
		String bossState = IslandSettings.hideBossbar ? "ON" : "OFF";
		IslandRender.drawText(item, bossState, x + width - 12.0F - item.width(bossState), y + 57.0F,
			color(IslandSettings.hideBossbar ? 0xFF9BE38F : 0xFFFFFFFF, a2 * 0.9F));

		IslandRender.drawText(item, "Drag", x + 12.0F, y + 69.0F, color(accent(), a2 * 0.55F));
		boolean moved = IslandSettings.isMoved();
		String dragState = moved ? "RESET" : "DRAG";
		IslandRender.drawText(item, dragState, x + width - 12.0F - item.width(dragState), y + 69.0F,
			color(moved ? 0xFFE36F6F : 0xFFFFFFFF, a2 * 0.9F));

		Font small = Fonts.regular(5.5F);
		String version = "Storm DLC v" + com.top1.client.VersionCheck.VERSION
			+ (com.top1.client.VersionCheck.isOutdated() ? "  (update available)" : "");
		IslandRender.drawText(item, version, x + 12.0F, y + 84.0F,
			color(com.top1.client.VersionCheck.isOutdated() ? 0xFFE3D18F : WHITE, a2 * 0.45F));
		IslandRender.drawWindowedText(small, dev.stormdlc.update.ClientUpdates.repositoryUrl(),
			x + 12.0F, y + 94.0F, color(accent(), a2 * 0.28F),
			x + 11.0F, x + width - 10.0F, 6.0F, 0.0F);
	}

	private void drawLyrics(float x, float y, MediaInfo media, float ext, float height) {
		List<LyricsFetcher.LyricLine> lines = tracker.getLyrics();
		if(lines.isEmpty()) return;
		Font active = Fonts.medium(6.5F);
		Font activeBig = Fonts.medium(7.2F);
		Font idle = Fonts.regular(6.5F);
		float position = tracker.smoothPosition();
		int index = LyricsFetcher.currentIndex(lines, position);
		boolean synced = lines.get(0).time() >= 0.0F;

		float ax = x + 12.0F;
		float aw = panelWidth() - 24.0F;
		float ay = y + 56.0F;
		float ah = height - 56.0F - 28.0F;
		if(ah <= LYRIC_STEP) return;

		float center = ah / 2.0F - LYRIC_STEP / 2.0F;
		float autoScroll = synced ? Math.max(0, index) * LYRIC_STEP - center : -center + LYRIC_STEP;
		float maxScroll = Math.max(0.0F, lines.size() * LYRIC_STEP - ah);
		if(System.currentTimeMillis() - manualScrollAt < 4000L){
			autoScroll = Math.max(-center, Math.min(maxScroll, manualScroll));
		}else{
			manualScroll = autoScroll;
		}
		lyricScroll.update(autoScroll);
		float scroll = lyricScroll.get();

		IslandRender.pushScissor(ax - 2.0F, ay, aw + 4.0F, ah);
		for(int i = 0; i < lines.size(); i++){
			float lineY = ay + i * LYRIC_STEP - scroll;
			if(lineY < ay - LYRIC_STEP || lineY > ay + ah) continue;
			float fromTop = lineY - ay;
			float edge = 1.0F;
			if(fromTop < LYRIC_STEP) edge = Math.max(0.0F, (fromTop + LYRIC_STEP) / (LYRIC_STEP * 2.0F));
			float fromBottom = ay + ah - lineY;
			if(fromBottom < LYRIC_STEP * 2.0F) edge = Math.min(edge, Math.max(0.0F, fromBottom / (LYRIC_STEP * 2.0F)));
			String text = lines.get(i).text();
			float alpha = 255.0F * ext * edge;
			if(i == index && synced){
				drawKaraokeLine(active, activeBig, text, ax, lineY,
					ax, ax + aw, alpha, media, expandedScroll, expandedScales);
			}else{
				boolean overflow = idle.width(text) > aw;
				IslandRender.drawWindowedText(idle, text, ax, lineY,
					color(accent(), alpha * 0.38F), ax - 1.0F, ax + aw,
					overflow ? 7.0F : 0.0F, 0.0F);
			}
		}
		IslandRender.popScissor();
	}

	private void drawWaves(float x, float y, MediaInfo media, float ext) {
		long now = System.nanoTime();
		float dt = waveFrame == 0L ? 0.016F : Math.min(0.1F, (now - waveFrame) / 1.0E9F);
		waveFrame = now;

		float time = now / 1.0E9F;
		float barsX = rendering3D ? x + size.width + 6.0F : x + size.width - 21.0F;
		float centerY = y + (rendering3D ? 9.5F : PILL_HEIGHT / 2.0F);
		int barColor = xyz.angames.astolfoclient.client.config.VisualColors.resolve(
            xyz.angames.astolfoclient.client.module.ModuleManager.getModule(rendering3D ? xyz.angames.astolfoclient.client.module.modules.render.Spotify3DModule.class : xyz.angames.astolfoclient.client.module.modules.render.SongIslandModule.class), tracker.getMediaColor(), 0L);
		if (!rendering3D) barColor = color(barColor, 255 * (1 - ext));
		float peak = tracker.getPeak();
		float level = peak > 0.0F ? Math.min(1.0F, (float) Math.pow(peak, 0.55F) * 1.4F) : 0.6F;

		for(int i = 0; i < waveValues.length; i++){
			float target;
			if(!media.isPlaying()){
				target = 2.0F;
			}else{
				float slow = (float) Math.sin(time * 3.3F + i * 1.3F);
				float fast = (float) Math.sin(time * 7.9F + i * 2.4F);
				float shape = Math.abs(slow * 0.6F + fast * 0.4F);
				target = 2.5F + shape * (3.5F + level * 6.0F);
			}
			float rate = target > waveValues[i] ? 18.0F : 7.0F;
			waveValues[i] += (target - waveValues[i]) * Math.min(1.0F, dt * rate);
			float value = Math.max(1.6F, waveValues[i]);
			IslandRender.drawRoundedRect(barsX + i * 3.0F, centerY - value / 2.0F, 1.6F, value,
				0.8F, 0.8F, 0.8F, 0.8F, barColor);
		}
	}

	private String pillLine(MediaInfo media) {
		List<LyricsFetcher.LyricLine> lines = tracker.getLyrics();
		String line = media.getTitle() + "  |  " + media.getArtist();
		if(lyricsEnabled() && !lines.isEmpty() && lines.get(0).time() >= 0.0F){
			float position = tracker.smoothPosition();
			int index = LyricsFetcher.currentIndex(lines, position);
			if(index >= 0 && !inGap(lines, index, position)) line = lines.get(index).text();
		}
		if(!line.equals(currentLine)){
			previousLine = currentLine;
			currentLine = line;
			lineChanged = System.currentTimeMillis();
		}
		return line;
	}

	private static boolean inGap(List<LyricsFetcher.LyricLine> lines, int index, float position) {
		float next = index + 1 < lines.size() ? lines.get(index + 1).time() : Float.MAX_VALUE;
		float since = position - lines.get(index).time();
		float until = next - position;
		return since > 6.0F && until > 3.0F;
	}

	private float targetWidth(IMediaSession session) {
        if (expanded) return panelWidth();
        MediaInfo media = session == null ? null : session.getMedia();
        if (media != null) pillLine(media);
        float width = media == null ? PILL_MIN_WIDTH : PILL_MAX_WIDTH;
        return Math.min(width, Minecraft.getInstance().getWindow().getGuiScaledWidth() - 16.0F);
    }

    private float targetHeight() {
        if (!expanded) return PILL_HEIGHT;
        if (profileEnabled() && (profileSelected || !mediaAvailable)) return 122;
        return (tracker.getLyrics().isEmpty() || !lyricsEnabled() ? 92 : 128) + topInset();
    }

	private static String ownerIcon(IMediaSession session) {
		String owner = session.getOwner().toLowerCase();
		if(owner.contains("spotify")) return "spotify";
		if(owner.contains("chrome")) return "youtube";
		if(owner.contains("edge")) return "edge";
		if(owner.contains("yandex")) return "yandex_music"; 
		if(owner.contains("soundcloud")) return "soundcloud";
		if(owner.contains("telegram")) return "telegram";
		return null;
	}

	private static int color(int argb, float alpha) {
		int a = (int) Math.max(0.0F, Math.min(255.0F, alpha));
		return (a << 24) | (argb & 0xFFFFFF);
	}

	private float panelWidth() {
        return rendering3D ? WORLD_PANEL_WIDTH : Math.min(224.0F, Minecraft.getInstance().getWindow().getGuiScaledWidth() - 16.0F);
    }

    private static boolean isInteractive(Minecraft mc) {
        return mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen
            || mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen;
    }

	public boolean handleScroll(double mouseX, double mouseY, double amount) {
		Minecraft mc = Minecraft.getInstance();
		if(!expanded || (!rendering3D && (mc.screen == null || !isInteractive(mc)))
            || (profileEnabled() && (profileSelected || !mediaAvailable)) || (!rendering3D && ClientFeedback.reaction() != null)) return false;
		float x = islandX(mc);
		float islandTop = islandY(mc);
		if(settingsOpen && hovered(x, settingsY(mc, islandTop), size.width, SETTINGS_HEIGHT, mouseX, mouseY)){
			tracker.adjustLyricsOffset((float) amount * 0.1F);
			return true;
		}
		if(tracker.getLyrics().isEmpty()) return false;
		if(!hovered(x, islandTop, size.width, size.height, mouseX, mouseY)) return false;
		manualScroll = lyricScroll.get() - (float) amount * LYRIC_STEP;
		manualScrollAt = System.currentTimeMillis();
		return true;
	}

	public boolean handleClick(double mouseX, double mouseY, int button) {
		Minecraft mc = Minecraft.getInstance();
		if(button != 0 || rendering3D && mc.player == null) return false;
        if(!rendering3D && (!isInteractive(mc) || mc.mouseHandler.isMouseGrabbed())) return false;
		float x = islandX(mc);
		float y = islandY(mc);
		boolean inside = hovered(x, y, size.width, size.height, mouseX, mouseY);
		if (!rendering3D && ClientFeedback.reaction() != null) {
            if (inside) ClientFeedback.dismissReaction();
            return inside;
        }
        float panelTop = settingsY(mc, y);
		if(expanded && settingsOpen
			&& hovered(x, panelTop, size.width, SETTINGS_HEIGHT, mouseX, mouseY)) {
			float panelY = panelTop;
			if(hovered(x, panelY + 29.0F, size.width, 14.0F, mouseX, mouseY)) openMapper();
			else if(hovered(x, panelY + 43.0F, size.width, 14.0F, mouseX, mouseY)){
				IslandSettings.worldText = !IslandSettings.worldText;
				IslandSettings.save();
			}
			else if(hovered(x, panelY + 55.0F, size.width, 14.0F, mouseX, mouseY)){
				IslandSettings.hideBossbar = !IslandSettings.hideBossbar;
				IslandSettings.save();
			}
			else if(hovered(x, panelY + 67.0F, size.width, 14.0F, mouseX, mouseY)){
				if(IslandSettings.isMoved()) IslandSettings.resetPosition();
				else{
					settingsOpen = false;
					expanded = false;
					mc.setScreen(new DragScreen(this));
				}
			}
			return true;
		}
		if(!expanded){
			if(inside && (tracker.hasSession() || profileEnabled())){
				expanded = true;
				return true;
			}
			return false;
		}
		if(!inside){
			expanded = false;
			settingsOpen = false;
			return false;
		}
        if (profileEnabled() && hovered(x, y, size.width, TAB_HEIGHT, mouseX, mouseY)) {
            profileSelected = mouseX < x + size.width / 2;
            settingsOpen = false;
            return true;
        }
        if (profileEnabled() && (profileSelected || !mediaAvailable)) return true;
		IMediaSession session = tracker.getSession();
		if(session == null) return true;
		float dotsX = x + size.width - 24.0F;
		float dotsY = y + size.height - 20.0F;
		if(hovered(dotsX, dotsY, 20.0F, 14.0F, mouseX, mouseY)){
			settingsOpen = !settingsOpen;
			return true;
		}
		float mid = x + size.width / 2.0F;
		float controlY = y + size.height - 24.0F;
		if(hovered(mid - 40.0F, controlY, 16.0F, 16.0F, mouseX, mouseY)) session.previous();
		else if(hovered(mid - 8.0F, controlY, 16.0F, 16.0F, mouseX, mouseY)) session.playPause();
		else if(hovered(mid + 24.0F, controlY, 16.0F, 16.0F, mouseX, mouseY)) session.next();
		return true;
	}

	public boolean isWorldTextEnabled() {
		return IslandSettings.worldText;
	}

	public boolean shouldHideBossbar() {
		return IslandSettings.hideBossbar && tracker.hasSession();
	}

	private void openMapper() {
		MapperScreen currentScreen = MapperScreen.create(tracker);
		if(currentScreen == null) return;
		expanded = false;
		settingsOpen = false;
		Minecraft.getInstance().setScreen(currentScreen);
	}

	private static boolean hovered(float x, float y, float w, float h, double mx, double my) {
		return mx >= x && mx <= x + w && my >= y && my <= y + h;
	}

	private static final class IslandSize {
		float width;
		float height;

		IslandSize(float width, float height) {
			this.width = width;
			this.height = height;
		}
	}
}
