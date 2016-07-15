package org.fogbowcloud.app.jdfcompiler.semantic;

public class Block {
	public enum BlockType {
		IO, REMOTE
	}

	private BlockType blockType;
	
	public Block() {
	}

	public BlockType getBlockType() {
		return blockType;
	}

	public void setBlockType(BlockType blockType) {
		this.blockType = blockType;
	}
}
