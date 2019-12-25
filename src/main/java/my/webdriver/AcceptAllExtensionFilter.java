package my.webdriver;

public enum AcceptAllExtensionFilter implements ExtensionFilter {
	INSTANCE;

	@Override
	public boolean accept(String extensionPath) {
		return true;
	}
}
