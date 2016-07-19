package org.fogbowcloud.app.jdfcompiler.semantic;

public class RemoteBlock extends JDLCommand{

	private String content;

	public RemoteBlock(String content) {
		super();
		this.setBlockType(JDLCommandType.REMOTE);
		this.content = content;
	}
	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
}
