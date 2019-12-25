package my.web.proxy;

public enum Protocol {
	HTTP, HTTPS;

	enum Support {
		OK, KO, UNKNOWN;
	}

	@Override
	public String toString() {
		return name().toLowerCase();
	}

	public static boolean recognize(String urlProtocol) {
		for (Protocol prot : values()) {
			if (prot.name().equalsIgnoreCase(urlProtocol)) {
				return true;
			}
		}

		return false;
	}

	public static Protocol from(String protocol) {
		return valueOf(protocol.toUpperCase());
	}
}
