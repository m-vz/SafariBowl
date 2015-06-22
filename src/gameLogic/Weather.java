package gameLogic;

public enum Weather {
	SWELTERING_HEAT("Sweltering Heat"),
	VERY_SUNNY("Very Sunny"),
	NICE("Nice"),
	POURING_RAIN("Pouring Rain"),
	BLIZZARD("Blizzard");
	String niceString;

	Weather(String niceString) {
		this.niceString = niceString;
	}

	public String toNiceString() {
		return niceString;
	}
}
