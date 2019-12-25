package my.web.proxy;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum AnonimityType {
	// Target server does not know our real IP address neither we are using
	// a proxy.
	ELITE(1), //
	// Target server does not know our real IP address however it may know
	// we are using a proxy.
	ANONYMOUS(2), //
	// Target server knows our real IP address.
	TRANSPARENT(3), //
	UNKNOWN(4);

	@Getter
	private int level;

	public boolean isGreaterThan(AnonimityType otherAT) {
		return this.getLevel() < otherAT.getLevel();
	}

	public boolean isLowerThan(AnonimityType otherAT) {
		return this.getLevel() > otherAT.getLevel();
	}
}