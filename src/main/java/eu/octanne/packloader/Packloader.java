package eu.octanne.packloader;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import net.minecraft.server.v1_16_R3.PacketPlayInResourcePackStatus;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class Packloader extends JavaPlugin implements Listener {
    String urlHash = "http://cdn.octanne.eu/texture-pack/WarshipHash.php";
    String urlPack = "http://cdn.octanne.eu/texture-pack/WarShip.zip";

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        saveDefaultConfig();
        this.urlHash = getConfig().getString("urlHash", this.urlHash);
        this.urlPack = getConfig().getString("urlPack", this.urlPack);
    }

    public void injectPlayer(final Player player) {
        ChannelDuplexHandler channelDuplexHandler = new ChannelDuplexHandler()
        {
            @Override
            public void channelRead(ChannelHandlerContext channelHandlerContext, Object packet) throws Exception
            {
                if (packet instanceof PacketPlayInResourcePackStatus) {
                    PacketPlayInResourcePackStatus packetPlayInResourcePackStatus = (PacketPlayInResourcePackStatus)packet;
                    switch(packetPlayInResourcePackStatus.status) {
                        case ACCEPTED:
                            player.sendMessage("§aTéléchargement du texture pack en cours ...");
                            break;
                        case DECLINED:
                            player.sendMessage("§cTexture pack refusé, le pack est obligatoire pour jouer sur ce serveur !");
                            break;
                        case FAILED_DOWNLOAD:
                            player.sendMessage("§cTexture pack non chargé, une erreur s'est produite veuillez prévenir un administrateur !");
                            break;
                        case SUCCESSFULLY_LOADED:
                            player.sendMessage("§aTexture pack chargé avec succès !");
                            break;
                    }
                }
                super.channelRead(channelHandlerContext, packet);
            }

            @Override            
            public void write(ChannelHandlerContext channelHandlerContext, Object packet, ChannelPromise channelPromise) throws Exception {
                super.write(channelHandlerContext, packet, channelPromise);
            }
        };

        
        ChannelPipeline pipeline = (((CraftPlayer)player).getHandle()).playerConnection.networkManager.channel.pipeline();
        pipeline.addBefore("packet_handler", player.getName() + "-packloader", channelDuplexHandler);
    }
    
    private void removePlayer(Player player) {
        Channel channel = (((CraftPlayer)player).getHandle()).playerConnection.networkManager.channel;
        channel.eventLoop().submit(() -> {
            channel.pipeline().remove(player.getName() + "-packloader");
            return null;
        });
    }
    
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        injectPlayer(e.getPlayer());
        e.getPlayer().setResourcePack(this.urlPack, actualHash());
    }
    
    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        removePlayer(e.getPlayer());
    }
    
    @EventHandler
    public void onKick(PlayerKickEvent e) {
        removePlayer(e.getPlayer());
    }
    
    public String actualHash() {
        HttpClient client = HttpClient.newHttpClient();
        
        HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(this.urlHash)).build();
        
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String hash = response.body();
            getLogger().log(Level.INFO, "Hash : " + hash);
            return hash;
        } catch (IOException|InterruptedException e) {
            e.printStackTrace();
            return "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        } 
    }
}
