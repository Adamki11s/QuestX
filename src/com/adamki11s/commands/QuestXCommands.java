package com.adamki11s.commands;

import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.adamki11s.ai.RandomMovement;
import com.adamki11s.data.ItemStackDrop;
import com.adamki11s.data.ItemStackProbability;
import com.adamki11s.display.QuestDisplay;
import com.adamki11s.npcs.NPCHandler;
import com.adamki11s.npcs.SimpleNPC;
import com.adamki11s.npcs.UniqueNameRegister;
import com.adamki11s.npcs.io.CreateNPC;
import com.adamki11s.npcs.loading.FixedLoadingTable;
import com.adamki11s.npcs.tasks.Fireworks;
import com.adamki11s.quests.QuestManager;
import com.adamki11s.quests.setup.QuestSetup;
import com.adamki11s.quests.setup.QuestUnpacker;
import com.adamki11s.questx.QuestX;
import com.adamki11s.reputation.Reputation;
import com.topcat.npclib.entity.HumanNPC;

public class QuestXCommands implements CommandExecutor {

	QuestX plugin;
	NPCHandler handle;

	public QuestXCommands(QuestX main) {
		this.plugin = main;
		this.handle = main.getNPCHandler();
	}

	HumanNPC test;
	RandomMovement rm;

	Reputation r = new Reputation("Adamki11s", 100);

	HashMap<String, QuestSetup> setups = new HashMap<String, QuestSetup>();

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (label.equalsIgnoreCase("questx") || label.equalsIgnoreCase("q")) {
			if (!(sender instanceof Player)) {
				System.out.println("QuestX Commands must be issued in-game.");
				return true;
			} else {
				Player p = (Player) sender;

				ItemStack[] gear = new ItemStack[] { null, null, null, null, new ItemStack(Material.WOOD_AXE) };

				if(args.length == 2 && args[0].equalsIgnoreCase("qinfo")){
					if(args[1].equalsIgnoreCase("current")){
						QuestDisplay.displayCurrentQuestInfo(p);
					}
					return true;
				}
				
				if (args.length == 2 && args[0].equalsIgnoreCase("unpack")) {
					String qName = args[1];
					QuestUnpacker upack = new QuestUnpacker(qName);
					boolean suc = upack.unpackQuest();
					if (suc) {
						QuestX.logChat(p, "Unpack successfull");
						QuestX.logChat(p, "/QuestX setup <questname> " + ChatColor.GREEN + " to setup this quest");
					} else {
						QuestX.logChat(p, "Error while unpacking");
					}
					return true;
				}

				if (args.length == 2 && args[0].equalsIgnoreCase("setup")) {
					String qName = args[1];
					if(setups.containsKey(p.getName())){
						QuestX.logChat(p, "You are already setting this quest up!");
						return true;
					}
					if (!setups.containsKey(p.getName())) {
						if (QuestManager.doesQuestExist(qName)) {
							if (!QuestManager.hasQuestBeenSetup(qName)) {
								QuestSetup qs = new QuestSetup(qName, handle);
								if (!qs.canSetup()) {
									QuestX.logChat(p, "Failed to start setup, reason : " + qs.getFailSetupReason());
								} else {
									QuestX.logChat(p, "Setup successful! /questx next " + ChatColor.GREEN + "To select the next spawn location");
									qs.sendInitialMessage(p);
									this.setups.put(p.getName(), qs);
								}
							} else {
								QuestX.logChat(p, "This quest has already been setup");
							}
						} else {
							QuestX.logChat(p, "A quest by that name does not exist");
						}
					}
					return true;
				}

				if (args.length == 1 && args[0].equalsIgnoreCase("next")) {
					if (setups.containsKey(p.getName())) {
						QuestSetup qs = this.setups.get(p.getName());
						if (!qs.isSetupComplete()) {
							qs.setupSpawn(p);
							if (qs.isSetupComplete()) {
								qs.removeFromList();
								this.setups.remove(p.getName());
								QuestX.logChat(p, "Quest setup successfully!");
							}
						}
					} else {
						QuestX.logChat(p, "You aren't setting up a quest!");
					}
					return true;
				}

				if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
					if (UniqueNameRegister.isNameUnique(args[1])) {
						CreateNPC create = new CreateNPC(args[1], ChatColor.RED);

						// Format id:data:quantity:chance(out of 10,000)/
						String invDrops = "1,0,5,6000#2,0,3,3000", gr = "0,0,0,0,0";
						create.setProperties(true, true, false, true, (20 * 5), (20 * 20), 15, (20 * 30), 30, 2, 2, invDrops, gr);
						create.createNPCFiles();
					} else {
						QuestX.logChat(p, "NPC with this name already exists");
					}
					return true;
				}

				if (args.length == 2 && args[0].equalsIgnoreCase("find")) {
					String npcName = args[1];
					SimpleNPC npc = this.handle.getSimpleNPCByName(npcName);
					if (npc == null) {
						QuestX.logChat(p, "NPC with this name is not spawned");
						return true;
					} else {
						Fireworks f = new Fireworks(npc.getHumanNPC().getBukkitEntity().getLocation(), 6, 20);
						f.fireLocatorBeacons();
						QuestX.logChat(p, "Launching locator beacons!");
						return true;
					}
				}

				if (args.length == 2 && args[0].equalsIgnoreCase("tele")) {
					String npcName = args[1];
					SimpleNPC npc = this.handle.getSimpleNPCByName(npcName);
					if (npc == null) {
						QuestX.logChat(p, "NPC with this name is not spawned");
						return true;
					} else {
						p.teleport(npc.getHumanNPC().getBukkitEntity().getLocation());
						return true;
					}
				}

				if (args.length == 2 && args[0].equalsIgnoreCase("setfixedspawn")) {
					String npcName = args[1];
					boolean suc = FixedLoadingTable.addFixedNPCSpawn(p, npcName, p.getLocation(), handle);
					if (suc) {
						this.handle.getSimpleNPCByName(npcName).spawnNPC();
					}
					return true;
				}

				if (args.length == 2 && args[0].equalsIgnoreCase("stressspawn")) {
					int max = Integer.parseInt(args[1]);
					for (int i = 0; i < max; i++) { // 1/10 chance of dropping
						SimpleNPC snpc = new SimpleNPC(this.handle, ("a" + i), ChatColor.BLUE, true, true, false, 60, 200, 20, 100, 200, new ItemStackDrop(
								new ItemStackProbability[] { new ItemStackProbability(new ItemStack(Material.GOLD_AXE, 1), 6000) }), gear, 1, 1.5);
						snpc.spawnNPC();
						// QuestX.logChat(p, "NPC Spawned!");
					}
					return true;
				}

				if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
					String npcName = args[1];
					if (!UniqueNameRegister.isNameUnique(npcName)) {
						QuestX.logChat(p, ChatColor.RED + "Name is not unique!");
						return true;
					} else {
						SimpleNPC snpc = new SimpleNPC(this.handle, npcName, ChatColor.BLUE, true, true, false, 60, 200, 10, 100, 200, new ItemStackDrop(
								new ItemStackProbability[] { new ItemStackProbability(new ItemStack(Material.GOLD_AXE, 1), 6000) }), gear, 1, 1.5);
						snpc.spawnNPC();

						QuestX.logChat(p, "NPC Spawned!");
						return true;
					}

				}
			}
		}
		return true;
	}
}
