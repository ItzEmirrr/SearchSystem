import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import org.elasticsearch.client.RestClient;

import java.nio.file.Paths;

public class IndexFiles {
    public static void main(String[] args) throws Exception {
        RestClient restClient = RestClient.builder(
                new org.apache.http.HttpHost("localhost", 9200)
        ).build();

        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper()
        );

        ElasticsearchClient client = new ElasticsearchClient(transport);

        Fb2Indexer indexer = new Fb2Indexer(client);

        // Путь
        indexer.indexDirectory(Paths.get("D:\\Users\\Admin\\Documents\\Search\\files2"));
//        indexer.deleteIndex();
    }
}
