package me.DMan16.AxChunks;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.Aldreda.AxUtils.AxUtils;
import me.Aldreda.AxUtils.Utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class AxChunks extends JavaPlugin implements Listener {
	static StateFlag KeepChunkFlag;
	
	public static StateFlag getKeepChunkFlag() {
		return KeepChunkFlag;
	}
	
	public void onLoad() {
		if (AxUtils.getWorldGuardManager() == null) Bukkit.getPluginManager().disablePlugin(this);
		else KeepChunkFlag = AxUtils.getWorldGuardManager().newStateFlag("always-loaded",false);
	}
	
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this,this);
		for (World world : getServer().getWorlds()) onWorldLoad(new WorldLoadEvent(world));
		if (AxUtils.getCitizensManager() != null) new CitizensListener(this);
		Utils.chatColorsLogPlugin("&fAxChunks &aloaded!");
	}
	
	static void loadChunk(Chunk chunk) {
		chunk.load();
		chunk.setForceLoaded(true);
	}
	
	static boolean isKeepRegion(List<ProtectedRegion> regions) {
		for (ProtectedRegion region : regions) {
			State state = region.getFlag(KeepChunkFlag);
			if (state == State.ALLOW) return true;
			else if (state == State.DENY) return false;
		}
		return false;
	}
	
	static List<Chunk> getChunks(ProtectedRegion region, World world) {
		BlockVector3 min = region.getMinimumPoint();
		BlockVector3 max = region.getMaximumPoint();
		int minX = (int) Math.floor(Math.min(min.getBlockX(),max.getBlockX()) / 16.0) * 16;
		int maxX = Math.max(min.getBlockX(),max.getBlockX());
		int minZ = (int) Math.floor(Math.min(min.getBlockZ(),max.getBlockZ()) / 16.0) * 16;
		int maxZ = Math.max(min.getBlockZ(),max.getBlockZ());
		List<Chunk> chunks = new ArrayList<Chunk>();
		for (int x = minX; x <= maxX; x += 16) for (int z = minZ; z <= maxZ; z += 16) chunks.add(world.getChunkAt(x,z));
		return chunks;
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onChunkUnload(ChunkUnloadEvent event) {
		if (KeepChunkFlag == null) return;
		Chunk chunk = event.getChunk();
		int x = chunk.getX() * 16;
		int z = chunk.getZ() * 16;
		BlockVector3 min = BlockVector3.at(x,event.getWorld().getMinHeight(),z);
		BlockVector3 max = BlockVector3.at(x + 16 - 1,event.getWorld().getMaxHeight(),z + 16 - 1);
		ProtectedRegion region = new ProtectedCuboidRegion("AxChunksChunkUnloadEventTemp",min,max);
		List<ProtectedRegion> intersecting = region.getIntersectingRegions(AxUtils.getWorldGuardManager().getRegions(event.getWorld()).values());
		if (isKeepRegion(intersecting)) new BukkitRunnable() {
			public void run() {
				loadChunk(chunk);
			}
		}.runTask(this);
	}
	
	/*@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onWorldUnload(WorldUnloadEvent event) {
		if (event.isCancelled()) return;
		if (KeepChunkFlag != null) for (ProtectedRegion region : AxUtils.getWorldGuardManager().getRegions(event.getWorld()).values()) if (region.getFlag(KeepChunkFlag) == State.ALLOW) {
			event.setCancelled(true);
			break;
		}
	}*/
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onWorldLoad(WorldLoadEvent event) {
		if (KeepChunkFlag == null) return;
		List<ProtectedRegion> regions = new ArrayList<ProtectedRegion>(AxUtils.getWorldGuardManager().getRegions(event.getWorld()).values());
		while (!regions.isEmpty()) {
			ProtectedRegion region = regions.get(0);
			List<ProtectedRegion> intersecting = new ArrayList<ProtectedRegion>(region.getIntersectingRegions(regions));
			intersecting.add(region);
			if (isKeepRegion(intersecting)) {
				List<Chunk> chunks = getChunks(region,event.getWorld());
				chunks.forEach(AxChunks::loadChunk);
			}
			regions.removeAll(intersecting);
		}
	}
}