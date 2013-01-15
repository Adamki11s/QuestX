package couk.adamki11s.npcs;

import net.minecraft.server.v1_4_6.Entity;
import net.minecraft.server.v1_4_6.EntityLiving;
import net.minecraft.server.v1_4_6.Packet;
import net.minecraft.server.v1_4_6.Packet5EntityEquipment;
import net.minecraft.server.v1_4_6.WorldServer;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.bukkit.craftbukkit.v1_4_6.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_4_6.inventory.CraftItemStack;

import com.topcat.npclib.entity.HumanNPC;
import com.topcat.npclib.entity.NPC;

//import net.minecraft.server.EntityLiving;

import couk.adamki11s.ai.RandomMovement;
import couk.adamki11s.data.ItemStackDrop;
import couk.adamki11s.dialogue.Conversation;
import couk.adamki11s.events.ConversationRegister;
import couk.adamki11s.io.FileLocator;

public class SimpleNPC {

	final String name;
	final ChatColor nameColour;
	final Location rootLocation;
	final boolean moveable, attackable, aggressive;
	final int minPauseTicks, maxPauseTicks, maxVariation, respawnTicks, maxHealth;
	final ItemStackDrop inventory;

	int waitedSpawnTicks = 0;

	RandomMovement randMovement;

	Conversation c;

	final NPCHandler handle;

	HumanNPC npc;
	boolean isSpawned = false, underAttack = false;

	int health;

	public SimpleNPC(NPCHandler handle, String name, ChatColor nameColour, Location rootLocation, boolean moveable, boolean attackable, boolean aggressive, int minPauseTicks,
			int maxPauseTicks, int maxVariation, int health, int respawnTicks, ItemStackDrop inventory) {
		UniqueNameRegister.addNPCName(name);
		this.name = name;
		this.nameColour = nameColour;
		this.rootLocation = rootLocation;
		this.moveable = moveable;
		this.attackable = attackable;
		this.aggressive = aggressive;
		this.minPauseTicks = minPauseTicks;
		this.maxPauseTicks = maxPauseTicks;
		this.maxVariation = maxVariation;
		this.maxHealth = health;
		this.respawnTicks = respawnTicks;
		this.handle = handle;
		this.inventory = inventory;

		handle.registerNPC(this);
	}

	public int getMaxHealth() {
		return maxHealth;
	}

	public ItemStackDrop getInventory() {
		return inventory;
	}

	public int getWaitedSpawnTicks() {
		return waitedSpawnTicks;
	}

	public RandomMovement getRandMovement() {
		return randMovement;
	}

	public NPCHandler getHandle() {
		return handle;
	}

	public HumanNPC getNpc() {
		return npc;
	}

	public boolean isSpawned() {
		return isSpawned;
	}

	public boolean isConversing() {
		if (this.c == null) {
			return false;
		} else {
			return this.c.isConversing();
		}
	}

	public void setBoots(ItemStack item) {
		this.npc.getInventory().setBoots(item);
		this.updateArmor(1, item);
	}

	public void setLegs(ItemStack item) {
		this.npc.getInventory().setLeggings(item);
		this.updateArmor(2, item);
	}

	public void setChestplate(ItemStack item) {
		this.npc.getInventory().setChestplate(item);
		this.updateArmor(3, item);
	}

	public void setHelmet(ItemStack item) {
		this.npc.getInventory().setHelmet(item);
		this.updateArmor(4, item);
	}

	Player aggressor;

	public Player getAggressor() {
		return this.aggressor;
	}

	public void unAggro() {
		this.aggressor = null;
		this.underAttack = false;
	}

	public void damageNPC(Player p, int damage) {
		// set under attack and change AI
		// + drop loot on death
		// AI state = run/fight depending on character and on player he is
		// fighting
		health -= damage;
		this.aggressor = p;
		this.underAttack = true;
		if(this.isConversing()){
			ConversationRegister.endPlayerNPCConversation(c.getConvoData().getPlayer());
		}
		if (health <= 0) {
			p.sendMessage("You killed NPC '" + this.getName() + "'. NPC will respawn in " + this.respawnTicks / 20 + " seconds.");
			for (ItemStack i : this.inventory.getDrops()) {
				p.getWorld().dropItemNaturally(this.npc.getBukkitEntity().getLocation(), i);
			}
			this.despawnNPC();
		}
	}

	public void updateArmor(int slot, org.bukkit.inventory.ItemStack itm) {
		net.minecraft.server.v1_4_6.ItemStack i = CraftItemStack.asNMSCopy(itm);
		Packet p = new Packet5EntityEquipment(this.npc.getEntity().id, slot, i);
		((WorldServer) this.npc.getEntity().world).tracker.a(this.npc.getEntity(), p);
	}

	public HumanNPC getHumanNPC() {
		return this.npc;
	}

	public void updateWaitedSpawnTicks(int ticks) {
		if (!this.isNPCSpawned()) {
			this.waitedSpawnTicks += ticks;
			if (this.waitedSpawnTicks >= this.respawnTicks) {
				this.spawnNPC();
			}
		}
	}

	public void interact(Player p) {
		if (!this.isConversing() && !this.isUnderAttack()) {
			if (!FileLocator.doesNPCDlgFileExist(this.getName())) {
				p.sendMessage(ChatColor.AQUA + "[QuestX] " + ChatColor.RED + "No dialogue.dlg file found or it is empty!");
			} else {
				c = new Conversation(p, this);
				c.loadConversation();
				c.startConversation();
				System.out.println("Conversing = " + this.isConversing());
			}
		}
	}

	public boolean doesNPCIDMatch(String id) {
		return ((HumanNPC) this.handle.getNPCManager().getNPC(id)).getName().equalsIgnoreCase(this.npc.getName());
	}

	public void spawnNPC() {
		if (!isSpawned) {

			this.health = this.maxHealth;
			this.waitedSpawnTicks = 0;
			System.out.println("Spawning NPC " + this.getName());
			this.npc = (HumanNPC) this.handle.getNPCManager().spawnHumanNPC(this.name, this.rootLocation);
			isSpawned = true;
			this.setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
			this.setLegs(new ItemStack(Material.DIAMOND_LEGGINGS));
			if (moveable) {
				this.randMovement = new RandomMovement(this, this.rootLocation, this.minPauseTicks, this.maxPauseTicks, this.maxVariation);
			}
		}
	}

	public boolean isNPCSpawned() {
		return this.isSpawned;
	}

	public boolean isUnderAttack() {
		return this.underAttack;
	}

	public void despawnNPC() {
		if (isSpawned) {
			this.isSpawned = false;
			this.handle.getNPCManager().despawnHumanByName(this.name);
			this.randMovement = null;
		}
	}

	public void destroyNPCObject() {
		this.despawnNPC();
		UniqueNameRegister.removeName(name);
		this.handle.removeNPC(this);
	}

	public void moveTick() {
		this.randMovement.move();
	}

	public void moveTo(Location l) {
		this.npc.walkTo(l);
	}

	public void lookAt(Location l) {
		this.npc.lookAtPoint(l);
	}

	public int getRespawnTicks() {
		return this.respawnTicks;
	}

	public int getHealth() {
		return health;
	}

	public void setHealth(int health) {
		this.health = health;
	}

	public String getName() {
		return name;
	}

	public ChatColor getNameColour() {
		return nameColour;
	}

	public Location getRootLocation() {
		return rootLocation;
	}

	public boolean isMoveable() {
		return moveable;
	}

	public boolean isAttackable() {
		return attackable;
	}

	public boolean isAggressive() {
		return aggressive;
	}

	public int getMinPauseTicks() {
		return minPauseTicks;
	}

	public int getMaxPauseTicks() {
		return maxPauseTicks;
	}

	public int getMaxVariation() {
		return maxVariation;
	}

}
