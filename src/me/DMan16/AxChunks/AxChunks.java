package me.DMan16.AxChunks;

import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.Aldreda.AxUtils.AxUtils;
import me.Aldreda.AxUtils.Utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class AxChunks extends JavaPlugin implements Listener {
	StateFlag KeepChunkFlag;
	
	public void onLoad() {
		if (AxUtils.getWorldGuardManager() == null) Bukkit.getPluginManager().disablePlugin(this);
		else this.KeepChunkFlag = AxUtils.getWorldGuardManager().newStateFlag("KeepChunkLoadedFlag",false);
	}
	
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this,this);
		if (this.KeepChunkFlag != null) for (World world : getServer().getWorlds())
			for (ProtectedRegion region : AxUtils.getWorldGuardManager().getRegions(world).values()) if (region.getFlag(KeepChunkFlag) == State.ALLOW) {
				Chunk chunk = new Location(world,region.getMinimumPoint().getX(),region.getMinimumPoint().getY(),region.getMinimumPoint().getZ()).getChunk();
				if (region.contains(chunk.getX(),0,chunk.getZ())) loadChunk(chunk);
			}
		Utils.chatColorsLogPlugin("&fAxChunks &aloaded!");
	}
	
	private boolean loadChunk(Chunk chunk) {
		try {
			chunk.load();
			chunk.setForceLoaded(true);
			return true;
		} catch (Exception e) {}
		return false;
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onChunkUnload(ChunkUnloadEvent event) {
		if (this.KeepChunkFlag != null) try {
			for (ProtectedRegion region : AxUtils.getWorldGuardManager().sortRegionsByPriority(AxUtils.getWorldGuardManager().getRegionSet(new Location(event.getChunk().getWorld(),
					event.getChunk().getX(),0,event.getChunk().getZ())))) if (region.getFlag(KeepChunkFlag) == State.ALLOW) {
				loadChunk(event.getChunk());
				break;
			}
		} catch (Exception e) {}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onWorldUnload(WorldUnloadEvent event) {
		if (event.isCancelled()) return;
		if (this.KeepChunkFlag != null) for (ProtectedRegion region : AxUtils.getWorldGuardManager().getRegions(event.getWorld()).values()) if (region.getFlag(KeepChunkFlag) == State.ALLOW) {
			event.setCancelled(true);
			break;
		}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onWorldLoad(WorldLoadEvent event) {
		if (this.KeepChunkFlag != null) for (ProtectedRegion region : AxUtils.getWorldGuardManager().getRegions(event.getWorld()).values()) if (region.getFlag(KeepChunkFlag) == State.ALLOW) {
			Chunk chunk = new Location(event.getWorld(),region.getMinimumPoint().getX(),region.getMinimumPoint().getY(),region.getMinimumPoint().getZ()).getChunk();
			if (region.contains(chunk.getX(),0,chunk.getZ())) loadChunk(chunk);
		}
	}
}