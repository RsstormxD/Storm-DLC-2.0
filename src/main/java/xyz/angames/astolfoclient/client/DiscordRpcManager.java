package xyz.angames.astolfoclient.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.gui.ClickGuiScreen;
import xyz.angames.astolfoclient.client.util.DiscordAvatarManager;

@Environment(EnvType.CLIENT)
public class DiscordRpcManager {
   public static final String CLIENT_ID="1552749614816305223";
   public static volatile String discordUsername="Discord offline";
   private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("StormDLC/Discord");
   private volatile boolean running,ready,dirty=true;
   private volatile int generation;
   private volatile String presenceDetails="Storm DLC 2.0", presenceState="Minecraft 1.21.4";
   private volatile boolean showElapsed=true,showLogo=true,spinningLogo=true;
   public boolean isRunning(){return running;}
   public String status(){return !running?"disabled":ready?(acknowledged?"connected":"publishing"):"waiting for Discord";}
   public void setPresence(String details,String state,boolean elapsed,boolean logo,boolean spinning){
      details=details.length()>120?details.substring(0,120):details;
      state=state.length()>120?state.substring(0,120):state;
      if(!details.equals(presenceDetails)||!state.equals(presenceState)||elapsed!=showElapsed||logo!=showLogo||spinning!=spinningLogo){
         presenceDetails=details;presenceState=state;showElapsed=elapsed;showLogo=logo;spinningLogo=spinning;dirty=true;
      }
   }
   private volatile boolean acknowledged;
   public boolean hasAcknowledgedActivity(){return acknowledged;}
   private volatile IPCChannel ipcChannel;
   private Thread rpcThread;
   private java.util.concurrent.ScheduledExecutorService updater;
   private long startTimestamp;
   private volatile boolean useAsset=true;
   private String imageKey="https://cdn.discordapp.com/app-icons/1552749614816305223/e0887420b1fc42f6019d339ae7455dfe.png";
   // Discord animates externally hosted GIFs; uploaded application assets are static.
   private String animatedImageKey="https://iili.io/n0L5JPS.gif";

   public synchronized void start(){
      if(running)return;
      final int run=++generation;acknowledged=false;dirty=true;
      startTimestamp=System.currentTimeMillis()/1000;running=true;
      try{
         var path=dev.stormdlc.config.ClientPaths.configDirectory().resolve("discord-rpc.json");
         JsonObject settings=java.nio.file.Files.exists(path)
            ?JsonParser.parseString(java.nio.file.Files.readString(path)).getAsJsonObject():new JsonObject();
         if(settings.has("largeImage") && !settings.get("largeImage").isJsonNull())imageKey=settings.get("largeImage").getAsString();
         if(settings.has("animatedLargeImage") && !settings.get("animatedLargeImage").isJsonNull()
            && !settings.get("animatedLargeImage").getAsString().isBlank())animatedImageKey=settings.get("animatedLargeImage").getAsString();
         settings.addProperty("applicationId",CLIENT_ID);
         settings.addProperty("largeImage",imageKey);
         settings.addProperty("animatedLargeImage",animatedImageKey);
         java.nio.file.Files.createDirectories(path.getParent());
         java.nio.file.Files.writeString(path,settings.toString());
      }catch(Exception e){LOG.warn("Cannot read Discord image setting");}
      updater=java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"storm-rpc-updater");t.setDaemon(true);return t;});
      updater.scheduleWithFixedDelay(()->{if(dirty && ready)publish();},1,15,java.util.concurrent.TimeUnit.SECONDS);
      rpcThread=new Thread(()->{
         while(running && generation==run){
            try{
               IPCChannel channel=connectIPC();
               if(channel==null){Thread.sleep(5000);continue;}
               if(!running || generation!=run){channel.close();break;}
               ipcChannel=channel;JsonObject hello=new JsonObject();hello.addProperty("v",1);hello.addProperty("client_id",CLIENT_ID);writeFrame(channel,0,hello.toString());
               while(running && generation==run && ipcChannel==channel){
                  Frame frame=readFrame(channel);
                  if(frame.op==2)throw new IOException("Discord closed IPC");
                  if(frame.op==3){writeFrame(channel,4,frame.json);continue;}
                  if(frame.op!=1)continue;
                  JsonObject root=JsonParser.parseString(frame.json).getAsJsonObject();
                  String event=root.has("evt") && !root.get("evt").isJsonNull()?root.get("evt").getAsString():"";
                  if(event.equals("READY")){
                     ready=true;dirty=true;useAsset=true;
                     JsonObject data=root.getAsJsonObject("data");
                     if(data!=null && data.has("user")){
                        JsonObject user=data.getAsJsonObject("user");String name=user.get("username").getAsString();String id=user.get("id").getAsString();String avatar=user.has("avatar") && !user.get("avatar").isJsonNull()?user.get("avatar").getAsString():"";
                        discordUsername=name;
                        net.minecraft.client.Minecraft.getInstance().execute(()->{ClickGuiScreen.setDiscordUser(name,id,avatar);DiscordAvatarManager.update(name,id,avatar);});
                     }
                     LOG.info("Discord IPC ready for application {}",CLIENT_ID);publish();
                  }else if(event.equals("ERROR")){
                     LOG.warn("Discord rejected activity: {}",root.get("data"));
                     if(useAsset){useAsset=false;dirty=true;}
                  }else if(root.has("cmd") && root.get("cmd").getAsString().equals("SET_ACTIVITY")){acknowledged=true;LOG.info("Discord activity accepted");}
               }
            }catch(InterruptedException e){break;}
            catch(Exception e){if(running)LOG.debug("Discord reconnect: {}",e.toString());}
            finally{if(generation==run)closeChannel();}
            if(running && generation==run)try{Thread.sleep(5000);}catch(InterruptedException e){break;}
         }
      },"storm-discord-rpc");rpcThread.setDaemon(true);rpcThread.start();
   }
   public synchronized void stop(){
      generation++;running=false;
      IPCChannel channel=ipcChannel;
      if(ready && channel!=null)try{
         JsonObject args=new JsonObject();args.addProperty("pid",ProcessHandle.current().pid());args.add("activity",com.google.gson.JsonNull.INSTANCE);
         JsonObject payload=new JsonObject();payload.addProperty("cmd","SET_ACTIVITY");payload.add("args",args);payload.addProperty("nonce",UUID.randomUUID().toString());writeFrame(channel,1,payload.toString());
      }catch(Exception ignored){}
      acknowledged=false;if(updater!=null)updater.shutdownNow();closeChannel();if(rpcThread!=null)rpcThread.interrupt();
   }
   private void closeChannel(){
      ready=false;acknowledged=false;IPCChannel old=ipcChannel;ipcChannel=null;
      if(old!=null)try{old.close();}catch(IOException ignored){}
   }
   public void update(){dirty=true;}
   private synchronized void publish(){
      IPCChannel channel=ipcChannel;if(!running || !ready || channel==null)return;
      try{
         JsonObject activity=new JsonObject();activity.addProperty("name","Storm DLC 2.0");activity.addProperty("state",presenceState);activity.addProperty("details",presenceDetails);
         JsonObject timestamps=new JsonObject();timestamps.addProperty("start",startTimestamp);if(showElapsed)activity.add("timestamps",timestamps);
         String selectedImage=spinningLogo?animatedImageKey:imageKey;
         if(showLogo && useAsset && !selectedImage.isBlank()){JsonObject assets=new JsonObject();assets.addProperty("large_image",selectedImage);assets.addProperty("large_text","Storm DLC 2.0");activity.add("assets",assets);}
         JsonObject args=new JsonObject();args.addProperty("pid",ProcessHandle.current().pid());args.add("activity",activity);
         JsonObject payload=new JsonObject();payload.addProperty("cmd","SET_ACTIVITY");payload.add("args",args);payload.addProperty("nonce",UUID.randomUUID().toString());
         writeFrame(channel,1,payload.toString());dirty=false;
      }catch(Exception e){closeChannel();}
   }
   private static void writeFrame(IPCChannel channel,int op,String json)throws IOException{
      byte[] bytes=json.getBytes(StandardCharsets.UTF_8);ByteBuffer frame=ByteBuffer.allocate(8+bytes.length).order(ByteOrder.LITTLE_ENDIAN).putInt(op).putInt(bytes.length).put(bytes);
      synchronized(channel){channel.write(frame.array());}
   }
   private record Frame(int op,String json){}
   private static Frame readFrame(IPCChannel channel)throws IOException{
      byte[] header=new byte[8];channel.readFully(header);ByteBuffer buffer=ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);int op=buffer.getInt(),length=buffer.getInt();
      if(op<0 || op>4 || length<0 || length>65536)throw new IOException("Invalid Discord IPC frame");
      byte[] body=new byte[length];channel.readFully(body);return new Frame(op,new String(body,StandardCharsets.UTF_8));
   }

   private DiscordRpcManager.IPCChannel connectIPC() {
      boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");

      for (int i = 0; i < 10; i++) {
         try {
            if (isWindows) {
               String pipePath = "\\\\.\\pipe\\discord-ipc-" + i;
               return new DiscordRpcManager.WindowsPipeChannel(pipePath);
            }

            String[] envDirs = new String[]{System.getenv("XDG_RUNTIME_DIR"), System.getenv("TMPDIR"), System.getenv("TMP"), System.getenv("TEMP"), "/tmp"};

            for (String dir : envDirs) {
               if (dir != null && !dir.trim().isEmpty()) {
                  File socketFile = new File(dir, "discord-ipc-" + i);
                  if (socketFile.exists()) {
                     return new DiscordRpcManager.UnixDomainChannel(socketFile.getAbsolutePath());
                  }
               }
            }
         } catch (Exception var9) {
         }
      }

      return null;
   }

   @Environment(EnvType.CLIENT)
   private interface IPCChannel extends Closeable {
      void write(byte[] var1) throws IOException;

      void readFully(byte[] var1) throws IOException;
   }

   @Environment(EnvType.CLIENT)
   private static class UnixDomainChannel implements DiscordRpcManager.IPCChannel {
      private final SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);

      public UnixDomainChannel(String socketPath) throws IOException {
         this.channel.connect(UnixDomainSocketAddress.of(socketPath));
      }

      @Override
      public void write(byte[] data) throws IOException {
         ByteBuffer buf = ByteBuffer.wrap(data);

         while (buf.hasRemaining()) {
            this.channel.write(buf);
         }
      }

      @Override
      public void readFully(byte[] dst) throws IOException {
         ByteBuffer buf = ByteBuffer.wrap(dst);

         while (buf.hasRemaining()) {
            int read = this.channel.read(buf);
            if (read < 0) {
               throw new IOException("End of stream reached");
            }
         }
      }

      @Override
      public void close() throws IOException {
         this.channel.close();
      }
   }

   private interface WinPipe extends com.sun.jna.win32.StdCallLibrary {
      WinPipe API=com.sun.jna.Native.load("kernel32",WinPipe.class);
      com.sun.jna.Pointer CreateFileW(com.sun.jna.WString name,int access,int share,com.sun.jna.Pointer security,int creation,int flags,com.sun.jna.Pointer template);
      boolean PeekNamedPipe(com.sun.jna.Pointer pipe,com.sun.jna.Pointer buffer,int size,com.sun.jna.ptr.IntByReference read,com.sun.jna.ptr.IntByReference available,com.sun.jna.ptr.IntByReference left);
      boolean ReadFile(com.sun.jna.Pointer pipe,byte[] buffer,int length,com.sun.jna.ptr.IntByReference read,com.sun.jna.Pointer overlapped);
      boolean WriteFile(com.sun.jna.Pointer pipe,byte[] buffer,int length,com.sun.jna.ptr.IntByReference written,com.sun.jna.Pointer overlapped);
      boolean CancelIoEx(com.sun.jna.Pointer pipe,com.sun.jna.Pointer overlapped);
      boolean CloseHandle(com.sun.jna.Pointer handle);
   }
   private static final class WindowsPipeChannel implements IPCChannel {
      private final com.sun.jna.Pointer handle;
      private volatile boolean closed;
      WindowsPipeChannel(String path)throws IOException{
         handle=WinPipe.API.CreateFileW(new com.sun.jna.WString(path),0xC0000000,0,null,3,0,null);
         if(handle==null || com.sun.jna.Pointer.nativeValue(handle)==-1L)throw new IOException("Discord pipe unavailable");
      }
      public void write(byte[] bytes)throws IOException{
         var written=new com.sun.jna.ptr.IntByReference();
         if(closed || !WinPipe.API.WriteFile(handle,bytes,bytes.length,written,null) || written.getValue()!=bytes.length)throw new IOException("Discord pipe write failed");
      }
      public void readFully(byte[] dst)throws IOException{
         int offset=0;
         while(offset<dst.length){
            var available=new com.sun.jna.ptr.IntByReference();
            if(closed || !WinPipe.API.PeekNamedPipe(handle,null,0,null,available,null))throw new IOException("Discord pipe closed");
            if(available.getValue()==0){try{Thread.sleep(20);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException("Interrupted",e);}continue;}
            byte[] chunk=new byte[Math.min(dst.length-offset,available.getValue())];var read=new com.sun.jna.ptr.IntByReference();
            if(!WinPipe.API.ReadFile(handle,chunk,chunk.length,read,null) || read.getValue()==0)throw new IOException("Discord pipe read failed");
            System.arraycopy(chunk,0,dst,offset,read.getValue());offset+=read.getValue();
         }
      }
      public synchronized void close(){if(closed)return;closed=true;WinPipe.API.CancelIoEx(handle,null);WinPipe.API.CloseHandle(handle);}
   }
}
