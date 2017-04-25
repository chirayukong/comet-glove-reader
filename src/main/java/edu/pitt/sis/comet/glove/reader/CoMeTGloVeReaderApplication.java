/**
 * 
 */
package edu.pitt.sis.comet.glove.reader;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import edu.pitt.sis.comet.commons.dao.ConnectDB;

/**
 *
 * Apr 23, 2017 5:57:52 PM
 *
 * @author Chirayu Kong Wongchokprasitti, PhD (chw20@pitt.edu)
 *
 */
@SpringBootApplication
public class CoMeTGloVeReaderApplication implements CommandLineRunner {

	private final String glovePath;

	private final String outputPath;

	@Autowired
	public CoMeTGloVeReaderApplication(
			@Value("${comet.glove.path}") String glovePath,
			@Value("${comet.glove.output}") String outputPath) {
		this.glovePath = glovePath;
		this.outputPath = outputPath;
	}

	private void addWordToList(String[] word, List<String> words, int start,
			int end) {
		if (start == end) {
			if (!words.contains(word[start])) {
				words.add(word[start]);
			}
		} else if (start < end) {
			int mid = (start + end) / 2;
			addWordToList(word, words, start, mid);
			addWordToList(word, words, mid + 1, end);
		}
	}

	private void generateFilteredGloVe(List<String> words) {
		Path glove = Paths.get(glovePath);
		Path file = Paths.get(outputPath);
		try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(
				file, StandardOpenOption.CREATE))) {
			Charset charset = Charset.forName("UTF-8");
			try (BufferedReader reader = Files
					.newBufferedReader(glove, charset)) {

				String line = null;

				while ((line = reader.readLine()) != null) {
					String[] token = line.split("\\s+");
					String term = token[0].trim();

					if (words.contains(term)) {
						byte data[] = line.getBytes();
						try {
							out.write(data, 0, data.length);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run(String... arg0) throws Exception {
		ConnectDB connectDB = new ConnectDB();
		String sql = "SELECT title, detail FROM clean_col";
		ResultSet rs = connectDB.getResultSet(sql);
		List<String> words = Collections.synchronizedList(new ArrayList<>());
		while (rs.next()) {
			String title = rs.getString("title");
			String detail = rs.getString("detail");

			String content = title + " " + detail;
			String[] s = content.split("\\s+");
			if (s != null) {
				addWordToList(s, words, 0, s.length - 1);
			}
		}
		generateFilteredGloVe(words);
	}

	public static void main(String[] args) {
		SpringApplication.run(CoMeTGloVeReaderApplication.class);
	}

}
