import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.w3c.dom.*;

public class Fb2Indexer {

    private final ElasticsearchClient client;

    public Fb2Indexer(ElasticsearchClient client) {
        this.client = client;
    }

    public void indexDirectory(Path directory) throws Exception {
        List<File> fb2Files = listFb2Files(directory.toFile());

        for (File file : fb2Files) {
            if (isRussian(file)) {
                System.out.println("Индексируем файл: " + file.getAbsolutePath());
                String title = extractTitle(file);
                List<String> sentences = extractSentences(file);

                for (String sentence : sentences) {
                    indexSentence(title, file.getAbsolutePath(), sentence);
                }
            } else {
                System.out.println("Пропускаем файл (не русский): " + file.getAbsolutePath());
            }
        }
    }

    private List<File> listFb2Files(File dir) throws Exception {
        try (Stream<Path> paths = Files.walk(dir.toPath())) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".fb2"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        }
    }

    private boolean isRussian(File file) throws Exception {
        Document doc = parseXml(file);
        NodeList fictionBooks = doc.getElementsByTagName("FictionBook");
        if (fictionBooks.getLength() == 0) return false;

        Element fictionBook = (Element) fictionBooks.item(0);
        String lang = fictionBook.getAttribute("lang");

        if (lang != null && lang.equalsIgnoreCase("ru")) {
            return true;
        }

        NodeList langNodes = doc.getElementsByTagName("lang");
        if (langNodes.getLength() > 0) {
            String langText = langNodes.item(0).getTextContent();
            return langText != null && langText.equalsIgnoreCase("ru");
        }

        return false;
    }

    private String extractTitle(File file) throws Exception {
        Document doc = parseXml(file);
        NodeList titleNodes = doc.getElementsByTagName("book-title");
        if (titleNodes.getLength() > 0) {
            return titleNodes.item(0).getTextContent();
        }
        return "Без названия";
    }

    private List<String> extractSentences(File file) throws Exception {
        String textContent = extractTextContent(file);
        return splitSentences(textContent);
    }

    private String extractTextContent(File file) throws Exception {
        Document doc = parseXml(file);
        NodeList bodyNodes = doc.getElementsByTagName("body");
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < bodyNodes.getLength(); i++) {
            Node body = bodyNodes.item(i);
            sb.append(body.getTextContent()).append(" ");
        }

        return sb.toString();
    }

    private List<String> splitSentences(String text) {
        return Arrays.stream(text.split("(?<=[.!?])\\s+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private Document parseXml(File file) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        dbf.setValidating(false);
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(file);
    }

    private void indexSentence(String title, String filePath, String sentence) {
        try {
            Map<String, Object> doc = new HashMap<>();
            doc.put("title", title);
            doc.put("file_path", filePath);
            doc.put("sentence", sentence);

            IndexRequest<Map<String, Object>> request = IndexRequest.of(i -> i
                    .index("books")
                    .document(doc)
            );

            IndexResponse response = client.index(request);

            // Можешь убрать для ускорения
            System.out.println("Индексировано, id=" + response.id());

        } catch (ElasticsearchException | java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
