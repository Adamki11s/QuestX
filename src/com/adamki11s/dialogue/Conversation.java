package com.adamki11s.dialogue;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.adamki11s.dialogue.triggers.Trigger;
import com.adamki11s.dialogue.triggers.TriggerType;
import com.adamki11s.events.ConversationRegister;
import com.adamki11s.exceptions.InvalidDialogueException;
import com.adamki11s.exceptions.MissingTaskPropertyException;
import com.adamki11s.io.FileLocator;
import com.adamki11s.npcs.NPCHandler;
import com.adamki11s.npcs.SimpleNPC;
import com.adamki11s.npcs.tasks.TaskLoader;
import com.adamki11s.npcs.tasks.TaskManager;
import com.adamki11s.npcs.tasks.TaskRegister;
import com.adamki11s.quests.QuestLoader;
import com.adamki11s.quests.QuestManager;
import com.adamki11s.quests.QuestTask;
import com.adamki11s.questx.QuestX;

public class Conversation {

	ConversationData convoData;
	DialogueSet[] dialogue;
	String currentNode = "1";
	boolean conversing = false, indexSelected = false, parseSuccess = false;;

	final NPCHandler handle = new NPCHandler((JavaPlugin) QuestX.p, null);

	public void respond(String s) {
		int i = 1;
		try {
			i = Integer.parseInt(s);
			this.selectSpeechOption(i);
		} catch (NumberFormatException ex) {
			QuestX.logChat(this.convoData.getPlayer(), "Invalid option! Select an index. Number only. Eg '1'");
		}
	}

	public Conversation(String pName, SimpleNPC npc) {
		this.convoData = new ConversationData(pName, npc);
	}

	public void loadConversation() {
		// Find file
		// Invoke DLG Parser
		DLGParser parse = new DLGParser(this, FileLocator.getNPCDlgFile(this.convoData.getSimpleNpc().getName()), this.convoData.getSimpleNpc().getName());
		try {
			this.dialogue = parse.parse();
			parseSuccess = true;
		} catch (InvalidDialogueException e) {
			parseSuccess = false;
			QuestX.logError("-----REASON-----");
			e.printErrorReason();
			/*
			 * QuestX.logError("-----STACK-----"); e.printStackTrace();
			 * QuestX.logError("-----STACK END-----");
			 */
		}
	}

	public boolean wasParseSuccessful() {
		return this.parseSuccess;
	}

	public void startConversation() {
		this.conversing = true;
		this.displaySpeechOptions();
		ConversationRegister.playersConversing.add(this);
	}

	public void endConversation() {
		Player p = this.getConvoData().getPlayer();
		if (p != null) {
			QuestX.logChat(p, ChatColor.AQUA + "[QuestX] " + ChatColor.RED + " Conversation ended.");
		}
		this.conversing = false;
		ConversationRegister.playersConversing.remove(this);
	}

	public boolean isConversing() {
		return this.conversing;
	}

	public void displaySpeechOptions() {
		DialogueSet d = this.getDialogeSetFromNode(currentNode);
		DialogueItem[] items = d.getItems();
		Player p = this.convoData.getPlayer();
		int count = 1;
		for (DialogueItem di : items) {
			if (di.doesPlayerHaveRequiredRepLevel(p.getName())) {
				QuestX.logChat(p, "[#" + count + "] " + di.getSay());
			} else {
				QuestX.logChat(p, "[#" + count + "] Unavailable");
			}
			count += 1;
		}
	}

	public void selectSpeechOption(int index) {
		DialogueSet d = this.getDialogeSetFromNode(currentNode);
		DialogueItem[] items = d.getItems();
		QuestX.logMSG("Items length = " + items.length);
		if (index > items.length || index < 1) {
			QuestX.logChat(this.getConvoData().getPlayer(), "Invalid chat option!");
			this.displaySpeechOptions();
			return;
		}
		DialogueItem selected = items[index - 1];
		Player p = this.convoData.getPlayer();
		if (selected.doesPlayerHaveRequiredRepLevel(p.getName())) {
			Trigger selTrigger = selected.getTrigger();

			if (selTrigger.getTriggerType() != TriggerType.QUEST) {
				DialogueResponse dr = d.getResponse();
				String response = dr.getResponses()[index - 1];
				QuestX.logChat(p, "[" + this.convoData.getSimpleNpc().getName() + "] " + response);
			}

			System.out.println("Current node = " + this.currentNode);
			this.currentNode = this.currentNode + index;
			System.out.println("Current node = " + this.currentNode);
			System.out.println("Trigger type = " + selTrigger.getTriggerType().toString());
			if (selTrigger.getTriggerType() == TriggerType.END) {
				this.endConversation();
				return;
			} else if (selTrigger.getTriggerType() == TriggerType.TASK) {
				System.out.println("INSIDE TRIGGER code");
				System.out.println("Does player have task = " + TaskRegister.doesPlayerHaveTask(p.getName()));
				boolean alreadyDone = TaskRegister.hasPlayerCompletedTask(this.getConvoData().getSimpleNpc().getName(), p.getName());
				if (alreadyDone) {
					QuestX.logChat(p, "You have already completed this task!");
					this.endConversation();
					return;
				}
				if (TaskRegister.doesPlayerHaveTask(p.getName())) {
					System.out.println("In has task code");
					QuestX.logChat(p, ChatColor.RED + "You already have a task assigned!");
					QuestX.logChat(p, ChatColor.WHITE + "/questx task cancel" + ChatColor.RED + " to cancel current task.");
					this.endConversation();
					return;
				} else {
					System.out.println("In has NOT task code");
					TaskLoader tl = new TaskLoader(FileLocator.getNPCTaskFile(this.getConvoData().getSimpleNpc().getName()), this.getConvoData().getSimpleNpc().getName());
					QuestX.logDebug("Loading task...");
					try {
						tl.load();
						QuestX.logDebug("Task Loaded!");
						TaskManager manage = new TaskManager(p.getName(), tl);
						TaskRegister.registerTask(manage);
						QuestX.logChat(p, ChatColor.ITALIC + tl.getTaskName() + ChatColor.RESET + ChatColor.GREEN + " task started!");
						QuestX.logChat(p, "Task description : " + tl.getTaskDescription());
						QuestX.logDebug("Not recieving msgs?");
					} catch (MissingTaskPropertyException e) {
						e.printErrorReason();
						QuestX.logChat(p, "Task failed to load, task file is incorrectly formatted. Check the server log for details.");
					}

					this.endConversation();
					return;
				}
			} else if (selTrigger.getTriggerType() == TriggerType.QUEST) {
				QuestX.logDebug("Inside quest code");
				SimpleNPC npc = this.getConvoData().getSimpleNpc();
				if (npc.doesLinkToQuest()) {
					QuestX.logDebug("NPC links to quest = " + npc.getQuestName());
					String qName = npc.getQuestName();

					if (QuestManager.hasQuestBeenSetup(qName)) {

						if (!QuestManager.doesPlayerHaveQuest(p.getName())) {
							// start a quest
							QuestX.logDebug("Player does not have quest!");
							if (!QuestManager.isQuestLoaded(qName)) {
								QuestManager.loadQuest(qName);
							}
							QuestX.logDebug("QUEST LOADED ############");
							QuestLoader ql = QuestManager.getQuestLoader(qName);
							ql.loadAndCheckPlayerProgress(p.getName());
							if (ql.isQuestComplete(p.getName())) {
								QuestX.logChat(p, "You have already completed this quest!");
							} else {
								QuestManager.setCurrentPlayerQuest(p.getName(), qName);
								QuestX.logDebug(ql.getStartText() + "<<<<<< START TEXT");
								QuestX.logChat(p, ql.getStartText());
								QuestTask t = QuestManager.getCurrentQuestTask(p.getName());
								if (t != null) {
									QuestX.logDebug("Task in non-null");
									// t.sendWhatIsLeftToDo(p);
								} else {
									QuestX.logDebug("Task is null!");
								}
							}
							this.endConversation();
						} else {
							QuestX.logDebug("Player has quest!");
							// player already has quest
						}

					} else {
						// quest has not been setup
						QuestX.logChat(p, "This quest has not yet been setup. /q setup " + qName);
					}
				} else {
					QuestX.logDebug("NPC has no link to a quest");
				}
				this.endConversation();
			} else {
				this.displaySpeechOptions();
			}
		} else {
			QuestX.logChat(p, "You must have at least " + items[index - 1].getRequriedRep().getMinRep() + " reputation.");
		}
	}

	boolean canNPCCompleteQuestNode(String quest, String player) {
		SimpleNPC n = this.getConvoData().getSimpleNpc();
		int currentNode = QuestManager.getQuestLoader(quest).getCurrentQuestNode(player);
		return (n.getCompleteQuestNodes().contains(currentNode));
	}

	DialogueSet getDialogeSetFromNode(String node) {
		for (DialogueSet dSet : dialogue) {
			if (dSet.getDialogueID().equalsIgnoreCase(node)) {
				return dSet;
			}
		}
		return null;
	}

	public ConversationData getConvoData() {
		return this.convoData;
	}

}
