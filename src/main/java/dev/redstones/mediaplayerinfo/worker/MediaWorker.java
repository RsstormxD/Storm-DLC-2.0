package dev.redstones.mediaplayerinfo.worker;

import dev.redstones.mediaplayerinfo.*;
import dev.redstones.mediaplayerinfo.impl.win.WindowsMediaPlayerInfo;
import java.io.*;
import java.util.List;

/** Standalone child JVM. No Minecraft classes and no account credentials. */
public final class MediaWorker {
    static final int MAGIC=0x53564d31;
    public static void main(String[] args){
        long parent=Long.getLong("storm.media.parent",-1L);
        if(parent>0){Thread guard=new Thread(()->{
            while(true){try{Thread.sleep(2000);}catch(InterruptedException e){return;}
                if(ProcessHandle.of(parent).map(ProcessHandle::isAlive).orElse(false))continue;
                Runtime.getRuntime().halt(0);
            }
        },"media-parent-watch");guard.setDaemon(true);guard.start();}

        try(var input=new DataInputStream(new BufferedInputStream(System.in));var output=new DataOutputStream(new BufferedOutputStream(System.out))){
            var nativeApi=new WindowsMediaPlayerInfo();
            output.writeInt(MAGIC);output.flush();
            while(true){
                String command=input.readUTF();
                if(command.equals("STOP"))break;
                String owner=command.equals("LIST")?"":input.readUTF();
                try{
                    List<IMediaSession> sessions=nativeApi.getMediaSessions();
                    var buffer=new ByteArrayOutputStream();var payload=new DataOutputStream(buffer);
                    if(command.equals("LIST")){
                        payload.writeInt(sessions.size());
                        for(var session:sessions){
                            var media=session.getMedia();payload.writeUTF(session.getOwner());payload.writeUTF(media.getTitle());payload.writeUTF(media.getArtist());
                            byte[] art=media.getArtworkPng();if(art==null || art.length>4194304)art=new byte[0];payload.writeInt(art.length);payload.write(art);
                            payload.writeLong(media.getPosition());payload.writeLong(media.getDuration());payload.writeBoolean(media.isPlaying());
                        }
                    }else{
                        var session=sessions.stream().filter(s->s.getOwner().equals(owner)).findFirst().orElse(null);
                        if(session!=null)switch(command){
                            case "PLAY"->session.play();case "PAUSE"->session.pause();case "TOGGLE"->session.playPause();case "NEXT"->session.next();case "PREVIOUS"->session.previous();case "STOP_MEDIA"->session.stop();case "CYCLE"->session.swapCycle();default->throw new IOException("Unknown media command");
                        }
                    }
                    output.writeBoolean(true);output.write(buffer.toByteArray());
                }catch(Throwable error){output.writeBoolean(false);output.writeUTF(error.getClass().getSimpleName());}
                output.flush();
            }
        }catch(Throwable ignored){
        }finally{
            // Windows COM teardown in the supplied DLL is unsafe. Only this helper
            // process exits here; Minecraft and its world saves are unaffected.
            Runtime.getRuntime().halt(0);
        }
    }
}
