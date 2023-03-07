package max.demo.marketanalysis.infra.oanda.v20.candles;

import lombok.Builder;
import lombok.With;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.function.ThrowingBiFunction;

import java.io.IOException;
import java.io.RandomAccessFile;

@Slf4j
@UtilityClass
public class ReadFileUtil {

  @With
  @Builder
  public record FirstAndLastLine(String first, String last) {
    public static FirstAndLastLine empty() {
      return new FirstAndLastLine("", "");
    }
  }

  public static FirstAndLastLine getFirstAndLastLineFromFile(String fileName) {
    try (RandomAccessFile file = new RandomAccessFile(fileName, "r")) {
      var fileLength = file.length();
      if (fileLength == 0) {
        return FirstAndLastLine.empty();
      }

      return FirstAndLastLine
          .builder()
          .first(readLineFromEnd(file, findEndOfFirstLine(file, fileLength)))
          .last(readLineFromEnd(file, findEndOfLastLine(file, fileLength)))
          .build();
    } catch (IOException e) {
      log.error("Error while reading file: {}", fileName);
      return FirstAndLastLine.empty();
    }
  }

  public static String readFirstLineFromFile(String fileName) {
    return readLineFromEndHelper(fileName, ReadFileUtil::findEndOfFirstLine);
  }

  /**
   * Reads last line from a large file by starting from the last byte. Assumes each line contains a newline char (\n).
   */
  public static String readLastLineFromFile(String fileName) {
    return readLineFromEndHelper(fileName, ReadFileUtil::findEndOfLastLine);
  }

  /**
   * @param fileName          path to the file to read (as a String)
   * @param filePointerGetter function that finds the end of the line to read
   * @return the line that was read
   */
  @NotNull
  private static String readLineFromEndHelper(String fileName, ThrowingBiFunction<RandomAccessFile, Long, Long> filePointerGetter) {
    var line = "";
    try (RandomAccessFile file = new RandomAccessFile(fileName, "r")) {
      var fileLength = file.length();
      if (fileLength == 0) {
        return line;
      }

      var filePointer = filePointerGetter.apply(file, fileLength);
      line = readLineFromEnd(file, filePointer);
    } catch (IOException e) {
      log.error("Error while reading file: {}", fileName);
    }
    return line;
  }

  /**
   * Finds the end of the first line or the end of file if there is no new line character.
   */
  private static long findEndOfFirstLine(RandomAccessFile file, long fileLength) throws IOException {
    long filePointer = -1;
    int currentByte = -1; // must not be a new line char

    while (filePointer + 1 < fileLength && !isNewLineChar(currentByte)) {
      file.seek(++filePointer);
      currentByte = file.readByte();
    }
    return filePointer;
  }

  /**
   * Read backwards until a new line character is found or the start of the file is reached.
   */
  private static long findEndOfLastLine(RandomAccessFile file, long fileLength) throws IOException {
    long filePointer = fileLength - 1;
    file.seek(filePointer);
    int currentByte = file.readByte();

    while (filePointer > 0 && !isNewLineChar(currentByte)) {
      filePointer--;
      file.seek(filePointer);
      currentByte = file.readByte();
    }
    return filePointer;
  }

  /**
   * Reads a line from its end (backward) until a new line character is found or the start of the file is reached.
   *
   * @param filePointer the assumed end of the line
   */
  @NotNull
  private static String readLineFromEnd(RandomAccessFile file, long filePointer) throws IOException {
    int currentByte;
    StringBuilder sb = new StringBuilder();

    while (filePointer > 0) {
      filePointer--;
      file.seek(filePointer);
      currentByte = file.readByte();
      if (isNewLineChar(currentByte)) {
        break;
      }
      sb.append((char) currentByte);
    }

    return sb.reverse().toString() // \n\rolleh --> hello\r\n
        .replace("\n", "") // --> hello\r
        .replace("\r", ""); // --> hello
  }

  /**
   * 10 = LF = \n
   * <br />
   * 13 = CR = \r
   */
  private static boolean isNewLineChar(int currentByte) {
    return currentByte == 10 || currentByte == 13;
  }

}
