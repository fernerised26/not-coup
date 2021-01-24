package game.systems;

import java.util.Random;

public class CrowdfundRandomizer {
	
	private static Random RAND = new Random();

	public static String getRandomCampaign() {
		int randomInt = RAND.nextInt(10);
		switch(randomInt) {
			case 0:
				return "customizable cybernetic pets";
			case 1:
				return "a stress relieving fish tank";
			case 2:
				return "a poop-whenever clothing line";
			case 3:
				return "holistic self-care products";
			case 4:
				return "communal sleeping bags";
			case 5:
				return "zero-calorie survival foods";
			case 6:
				return "a massive open world video game";
			case 7:
				return "a surreptitious snacking device";
			case 8:
				return "artisanal chamber pots";
			case 9:
				return "exclusive branded socks";
			default:
				return "internet-connected bidets";
		}
	}
}
