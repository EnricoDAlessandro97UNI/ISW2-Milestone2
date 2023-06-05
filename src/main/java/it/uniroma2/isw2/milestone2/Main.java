package it.uniroma2.isw2.milestone2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
	
	private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
	
	private static final String PROJECT_NAME = "SYNCOPE";    // or BOOKKEEPER
	private static final String OUTPUTS_FOLDER = "outputs/"; // Directory dei risultati
	
	private static void initOutputsFolder() throws IOException {
		Path path = Paths.get(OUTPUTS_FOLDER);
		if (!Files.exists(path)) // Se non esiste, crea la cartella dei risultati
			Files.createDirectory(path);
		
		// Controlla l'esistenza dei file di output
		Path[] filesPaths = {
				Paths.get(String.format("%s%s_analysis.csv", OUTPUTS_FOLDER, PROJECT_NAME))
		};
		
		for (Path filePath : filesPaths) {
			if (Files.exists(filePath)) // Se il file specifico già esiste lo cancella e lo ricrea
				Files.delete(filePath);
			Files.createFile(filePath);
		}
	}

	public static void main(String[] args) {
		
		/* Needed for SMOTE in presence of NaN */
		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
		
		try {
			initOutputsFolder();
			WekaAnalyser wekaAnalyser = new WekaAnalyser(PROJECT_NAME, OUTPUTS_FOLDER);
			wekaAnalyser.analyse();
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, String.format("Errore inizializzazione dei file di output: %s", e.getMessage()));
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, String.format("Errore nell'analisi da parte di Weka: %s", e.getMessage()));
		} 
	}
	
}
