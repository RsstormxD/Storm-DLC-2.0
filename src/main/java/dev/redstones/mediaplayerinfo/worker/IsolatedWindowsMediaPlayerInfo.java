package dev.redstones.mediaplayerinfo.worker;

import dev.redstones.mediaplayerinfo.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Owns a single hidden JVM so a native decoder failure cannot crash Minecraft. */
public final class IsolatedWindowsMediaPlayerInfo implements MediaPlayerInfo,AutoCloseable {
    private final ExecutorService io=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"storm-media-ipc");t.setDaemon(true);return t;});
    private volatile Worker worker;
    private volatile boolean closed,polled;
    public boolean hasReadSessions(){return polled;}
    private volatile long retryAfter;
    private record Worker(Process process,DataInputStream input,DataOutputStream output,Path directory){}
    @FunctionalInterface private interface Request<T>{T run(Worker worker)throws Exception;}
    private Worker connect()throws Exception{
        if(closed)throw new IOException("Media bridge stopped");
        if(worker!=null && worker.process.isAlive())return worker;
        dispose();
        Path directory=Files.createTempDirectory("storm-media-");Path jar=directory.resolve("worker.jar");
        try(var resource=getClass().getResourceAsStream("/stormdlc/media-worker.jar")){
            if(resource==null)throw new IOException("Missing bundled media worker");Files.copy(resource,jar);
        }
        Path java=Path.of(System.getProperty("java.home"),"bin","javaw.exe");
        Process process=new ProcessBuilder(java.toString(),"-Xmx64m","-Djava.awt.headless=true","-Dstorm.media.parent="+ProcessHandle.current().pid(),"-Dstorm.media.nativeDir="+directory,"-jar",jar.toString())
            .directory(directory.toFile()).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        Worker next=new Worker(process,new DataInputStream(new BufferedInputStream(process.getInputStream())),new DataOutputStream(new BufferedOutputStream(process.getOutputStream())),directory);
        worker=next;
        if(closed){dispose();throw new IOException("Media bridge stopped");}
        if(next.input.readInt()!=MediaWorker.MAGIC)throw new IOException("Invalid media worker handshake");
        return next;
    }
    private <T>T request(Request<T> action,T fallback){
        if(closed || System.currentTimeMillis()<retryAfter)return fallback;
        Future<T> pending=io.submit(()->action.run(connect()));
        try{return pending.get(6,TimeUnit.SECONDS);}
        catch(Exception e){pending.cancel(true);retryAfter=System.currentTimeMillis()+5000;dispose();if(e instanceof InterruptedException)Thread.currentThread().interrupt();return fallback;}
    }
    private static void begin(Worker w,String command,String owner)throws IOException{
        w.output.writeUTF(command);if(owner!=null)w.output.writeUTF(owner);w.output.flush();
        if(!w.input.readBoolean())throw new IOException("Native media error: "+w.input.readUTF());
    }
    @Override public List<IMediaSession> getMediaSessions(){
        return request(w->{
            begin(w,"LIST",null);int count=w.input.readInt();if(count<0 || count>128)throw new IOException("Invalid session count");
            List<IMediaSession> result=new ArrayList<>();
            for(int i=0;i<count;i++){
                String owner=w.input.readUTF(),title=w.input.readUTF(),artist=w.input.readUTF();int length=w.input.readInt();
                if(length<0 || length>4194304)throw new IOException("Invalid artwork length");byte[] art=new byte[length];w.input.readFully(art);
                MediaInfo info=new MediaInfo(title,artist,art,w.input.readLong(),w.input.readLong(),w.input.readBoolean());result.add(new Session(owner,info));
            }polled=true;return result;
        },List.of());
    }
    private void command(String owner,String command){
        if(closed)return;
        // Never wait for a native media command on the Minecraft render thread.
        CompletableFuture.runAsync(()->request(w->{begin(w,command,owner);return true;},false));
    }
    private final class Session implements IMediaSession {
        private final String owner;private final MediaInfo media;
        Session(String owner,MediaInfo media){this.owner=owner;this.media=media;}
        public String getOwner(){return owner;}public MediaInfo getMedia(){return media;}
        public void play(){command(owner,"PLAY");}public void pause(){command(owner,"PAUSE");}public void playPause(){command(owner,"TOGGLE");}
        public void stop(){command(owner,"STOP_MEDIA");}public void next(){command(owner,"NEXT");}public void previous(){command(owner,"PREVIOUS");}
        public void swapCycle(){command(owner,"CYCLE");}public int getCycleType(){return 0;}
    }
    private synchronized void dispose(){
        Worker previous=worker;worker=null;if(previous==null)return;
        previous.process.destroyForcibly();
        try{previous.process.waitFor(750,TimeUnit.MILLISECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}
        try{previous.input.close();}catch(IOException ignored){}try{previous.output.close();}catch(IOException ignored){}
        for(String file:List.of("MediaPlayerInfo.dll","worker.jar"))try{Files.deleteIfExists(previous.directory.resolve(file));}catch(IOException ignored){}
        try{Files.deleteIfExists(previous.directory);}catch(IOException ignored){}
    }
    @Override public void close(){closed=true;io.shutdownNow();dispose();}
}
