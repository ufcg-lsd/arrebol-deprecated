package org.fogbowcloud.app.jdfcompiler.semantic;

public class RemoteBlock extends Block{

	private String content;

	public RemoteBlock(String content) {
		super();
		this.setBlockType(BlockType.REMOTE);
		this.content = content;
	}
	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
}
