package com.adamki11s.sync.io.serializable;

import java.io.Serializable;

import org.bukkit.enchantments.Enchantment;

public class SyncEnchantment implements Serializable {

	private static final long serialVersionUID = 1400673419716003438L;
	
	private int id, level;

	/**
	 * A serializable implementation of Bukkit's Enchantment class.
	 * @param e (Enchantment)
	 */
	public SyncEnchantment(Enchantment e){
		this.id = e.getId();
		this.level = e.getMaxLevel();
	}

	public int getId() {
		return id;
	}

	public int getLevel() {
		return level;
	}

}
