package org.triplea.map.game.notes;

import com.google.common.base.Preconditions;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.experimental.UtilityClass;
import org.triplea.io.FileUtils;
import org.triplea.java.StringUtils;

@UtilityClass
public class GameNotes {
  /**
   * Dada la ruta a un archivo XML, lee las notas de juego correspondientes esperadas y devuelve el
   * contenido de ese archivo. Devuelve una cadena vacía si el archivo de notas del juego no existe o si
   * hay errores al leer el archivo.
   * @param xmlGameFile Ruta al archivo game-xml cuyas notas de juego cargaremos.
   */
  public static String loadGameNotes(final Path xmlGameFile) {
    Preconditions.checkArgument(
        Files.exists(xmlGameFile),
        "Error, expected file did not exist: " + xmlGameFile.toAbsolutePath());
    Preconditions.checkArgument(
        !Files.isDirectory(xmlGameFile),
        "Error, expected file was not a file: " + xmlGameFile.toAbsolutePath());

    final String notesFileName = createExpectedNotesFileName(xmlGameFile);
    final Path notesFile = xmlGameFile.resolveSibling(notesFileName);

    if (!Files.exists(notesFile)) {
      GameNotesMigrator.extractGameNotes(xmlGameFile);
    }
    return Files.exists(notesFile) ? FileUtils.readContents(notesFile).orElse("") : "";
  }

  /**
   * Dado un archivo game-xml, devuelve el nombre esperado del
   * archivo complementario que debe contener el 
   * correspondiente juego (html).
   */
  static String createExpectedNotesFileName(final Path gameXmlFile) {
    Preconditions.checkArgument(
        gameXmlFile.getFileName().toString().endsWith(".xml"),
        "Required a '.xml' file, got instead: " + gameXmlFile.toAbsolutePath());
    return StringUtils.truncateEnding(gameXmlFile.getFileName().toString(), ".xml") + ".notes.html";
  }

  /** Para un archivo xml de juego determinado, comprueba si existe un archivo html de notas complementarias. */
  static boolean gameNotesFileExistsForGameXmlFile(final Path gameXmlFile) {
    Preconditions.checkArgument(
        gameXmlFile.getFileName().toString().endsWith(".xml"),
        "Required a '.xml' file, got instead: " + gameXmlFile.toAbsolutePath());

    final String expectedNotesFile = createExpectedNotesFileName(gameXmlFile);
    return Files.exists(gameXmlFile.resolveSibling(expectedNotesFile));
  }
}
