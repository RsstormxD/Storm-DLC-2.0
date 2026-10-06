package xyz.angames.astolfoclient.client.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.commands.SharedSuggestionProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.AstolfoclientClient;

@Environment(EnvType.CLIENT)
@Mixin(CommandSuggestions.class)
public abstract class ChatInputSuggestorMixin {
   @Shadow
   private EditBox input;
   @Shadow
   private CompletableFuture<Suggestions> pendingSuggestions;
   @Shadow
   private ParseResults<SharedSuggestionProvider> currentParse;

   @Shadow
   public abstract void updateUsageInfo();

   @Inject(method = "updateCommandInfo", at = @At("HEAD"), cancellable = true)
   public void onRefresh(CallbackInfo ci) {
      String text = this.input.getValue();
      if (text.startsWith("$")) {
         int cursor = this.input.getCursorPosition();
         int lastSpace = text.lastIndexOf(32, cursor - 1);
         int start = lastSpace == -1 ? 1 : lastSpace + 1;
         List<String> suggestionsList = AstolfoclientClient.commandManager.getSuggestions(text);
         SuggestionsBuilder builder = new SuggestionsBuilder(text, start);

         for (String s : suggestionsList) {
            builder.suggest(s);
         }

         this.pendingSuggestions = builder.buildFuture();
         Minecraft client = Minecraft.getInstance();
         if (client.player != null && client.player.connection != null) {
            CommandDispatcher<SharedSuggestionProvider> dummyDispatcher = new CommandDispatcher();
            StringReader reader = new StringReader(text);
            reader.setCursor(text.length());
            CommandContextBuilder<SharedSuggestionProvider> contextBuilder = new CommandContextBuilder(
               dummyDispatcher, client.player.connection.getSuggestionsProvider(), dummyDispatcher.getRoot(), 0
            );
            this.currentParse = new ParseResults(contextBuilder, reader, Collections.emptyMap());
         }

         this.updateUsageInfo();
         ci.cancel();
      }
   }
}
