package com.adamki11s.npcs.tasks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;

import com.adamki11s.bundle.LocaleBundle;
import com.adamki11s.exceptions.InvalidKillTrackerException;
import com.adamki11s.questx.QuestX;

public class EntityKillTracker {

	// HashSet<EntityType> types = new HashSet<EntityType>();
	HashMap<EntityType, Integer> required = new HashMap<EntityType, Integer>();
	HashMap<EntityType, Integer> current = new HashMap<EntityType, Integer>();

	public EntityKillTracker(String in) throws InvalidKillTrackerException {
		this.parseInput(in);
	}

	// format entity_type:kills,
	void parseInput(String in) throws InvalidKillTrackerException {
		String[] ents = in.split("#");
		for (String parse : ents) {
			String[] components = parse.split(",");
			EntityType e = EntityType.valueOf(components[0]);
			if (e == null) {
				throw new InvalidKillTrackerException(in, "Could not parse entity type, check you have entered a valid entity");
			}
			// types.add(e);
			int k;
			try {
				k = Integer.parseInt(components[1]);
			} catch (NumberFormatException nfe) {
				throw new InvalidKillTrackerException(in, "Could not parse number of entities to kill, check the value is greater than or equal to 0 and is a whole number");
			}
			this.required.put(e, k);
			this.current.put(e, 0);
			QuestX.logDebug("Loaded EntityType : " + e.toString() + ", amount = " + k);
		}
	}

	public void trackKill(EntityType e) {
		if (this.required.get(e) != null) {
			int cur = this.current.get(e) + 1;
			this.current.put(e, cur);
		}
	}

	public boolean areRequiredEntitiesKilled() {
		for (Map.Entry<EntityType, Integer> entry : this.required.entrySet()) {
			EntityType e = entry.getKey();
			int reqKills = entry.getValue();
			if (this.current.get(e) < reqKills) {
				return false;
			}
		}
		return true;
	}

	public String sendEntitiesToKill() {
		StringBuilder buff = new StringBuilder();
		buff.append(ChatColor.RED).append(LocaleBundle.getString("kill"));
		for (Map.Entry<EntityType, Integer> entry : this.required.entrySet()) {
			EntityType e = entry.getKey();
			int reqKills = entry.getValue();
			int kc;
			if ((kc = this.current.get(e)) < reqKills) {
				buff.append(e.toString()).append(" : ").append(reqKills - kc).append(", ");
			}
		}
		return buff.toString();
	}

}
