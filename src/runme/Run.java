package runme;

import java.io.IOException;

import controllers.RunManager;
import inputParsers.ConfigParser;
import inputParsers.InstructionParser;

public class Run {

	private static String TAG = "ConfigParser";

	public static void main(String[] args) {
		ConfigParser cparser = ConfigParser.getInstance();
		InstructionParser iParser = InstructionParser.getInstance();
		try {
			cparser.readConfigFile("C:\\Users\\tchet\\workspace\\ScoreBoardWithCache\\Config.txt");
			iParser.readInstFile("C:\\Users\\tchet\\workspace\\ScoreBoardWithCache\\Inst.txt");
			RunManager rManager = RunManager.getInstance();
		} catch (IOException e) {
			System.out.println(TAG+e.getMessage());
		}
	}

}
